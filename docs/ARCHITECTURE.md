# API Sentinel — Architecture & Technical Specification

## 1. System Overview

**API Sentinel** is an enterprise-grade API monitoring and reliability engineering platform built as a modular monolith. It enables developers and DevOps engineers to register HTTP/HTTPS endpoints, continuously track latency and availability via an automated background monitoring engine, detect anomalies, manage incident lifecycles, and receive actionable alerts.

```mermaid
flowchart TD
    subgraph Client["Frontend (React 18 + TypeScript + Vite)"]
        UI[Tailwind UI & Recharts]
        State[Auth & API Services]
    end

    subgraph Backend["Spring Boot 3.3 Modular Monolith (Java 21)"]
        Sec[Spring Security + JWT Filter]
        API[REST API Controllers]
        
        subgraph Core["Core Business Services"]
            AuthSvc[Auth & User Service]
            EndpointSvc[Monitored Endpoint Service]
            IncidentSvc[Incident Lifecycle Manager]
            AlertSvc[Notification & Alert Service]
            AnalyticsSvc[Dashboard & Metrics Engine]
        end
        
        subgraph Engine["Automated Monitoring Engine"]
            Scheduler[Spring Task Scheduler]
            Executor[Bounded Worker Executor Pool]
            SSRF[SSRF & DNS Safety Validator]
            HTTPClient[Safe HTTP Execution Engine]
            Policy[Health Policy Evaluator]
        end
    end

    subgraph DataTier["Data Persistence"]
        Flyway[Flyway Migration Engine]
        DB[(PostgreSQL / H2 Compatibility)]
    end

    UI -->|HTTPS / REST API| Sec
    Sec --> API
    API --> Core
    Scheduler -->|Every Tick| Executor
    Executor --> SSRF
    SSRF -->|Validated Request| HTTPClient
    HTTPClient -->|Target API| ExternalEndpoints[(Monitored APIs)]
    HTTPClient --> Policy
    Policy --> IncidentSvc
    IncidentSvc --> AlertSvc
    Core --> DB
    Policy --> DB
```

---

## 2. Security Architecture & SSRF Prevention

Because API Sentinel allows users to register arbitrary URLs for background HTTP probing, **Server-Side Request Forgery (SSRF)** is the primary threat vector. The platform implements multi-layer defense-in-depth:

### 2.1 Multi-Layer SSRF Protections
1. **Scheme Whitelisting**: Only `http` and `https` schemes are accepted. Protocols like `file://`, `gopher://`, `ftp://`, `ldap://`, or cloud-specific protocols are rejected during validation.
2. **IP & Subnet Blacklisting**:
   - IPv4 Loopback (`127.0.0.0/8`)
   - IPv6 Loopback (`::1/128`, `::/128`)
   - RFC 1918 Private Networks:
     - `10.0.0.0/8`
     - `172.16.0.0/12`
     - `192.168.0.0/16`
   - Link-Local addresses: `169.254.0.0/16` (including cloud metadata: `169.254.169.254`), `fe80::/10`
   - Unique Local IPv6: `fc00::/7`
   - Carrier-Grade NAT: `100.64.0.0/10`
   - Broadcast / Multicast / Reserved: `224.0.0.0/4`, `240.0.0.0/4`, `0.0.0.0/8`
   - Well-known cloud metadata endpoints (e.g., AWS, GCP, Azure, Oracle, Alibaba `100.100.100.200`).
3. **DNS Resolution & Rebinding Protection**:
   - Hostnames are resolved to IP addresses via `InetAddress.getAllByName(host)`.
   - Every resolved IP address is verified against the blacklist prior to initiating socket connection.
   - Redirects: Automatic HTTP redirects (`followRedirects = false`) are disabled by default. If redirect following is configured, each hop's destination URL is re-validated through the full SSRF pipeline before following.
4. **Request & Resource Boundaries**:
   - Connection timeout: Configurable, capped at maximum 10 seconds.
   - Read / Response timeout: Configurable, capped at maximum 15 seconds.
   - Response Body Limit: Truncated at 100 KB to prevent memory exhaustion / decompression bombs.
   - Header & Credential Stripping: Check logs never store Authorization headers, cookies, API tokens, or internal credentials.
5. **Multi-Tenant Data Isolation**:
   - Every protected API checks current authenticated principal ID against the endpoint's `userId`.
   - Cross-user data access is rejected with `403 Forbidden` or `404 Not Found`.

---

## 3. Database Schema (PostgreSQL + Flyway)

The schema is strictly normalized and indexed for high-volume health check time-series queries.

```mermaid
erDiagram
    USERS ||--o{ MONITORED_ENDPOINTS : owns
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ USER_SETTINGS : configures
    MONITORED_ENDPOINTS ||--o{ HEALTH_CHECK_RESULTS : records
    MONITORED_ENDPOINTS ||--o{ INCIDENTS : generates
    INCIDENTS ||--o{ INCIDENT_EVENTS : logs
    INCIDENTS ||--o{ NOTIFICATIONS : triggers

    USERS {
        bigserial id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar role
        timestamp created_at
        timestamp updated_at
    }

    MONITORED_ENDPOINTS {
        bigserial id PK
        bigint user_id FK
        varchar name
        varchar url
        varchar http_method
        varchar expected_status_codes
        integer check_interval_seconds
        integer timeout_millis
        integer latency_threshold_millis
        boolean is_active
        integer consecutive_failures_threshold
        varchar current_status
        timestamp last_checked_at
        timestamp created_at
        timestamp updated_at
    }

    HEALTH_CHECK_RESULTS {
        bigserial id PK
        bigint endpoint_id FK
        timestamp checked_at
        integer response_time_ms
        integer http_status_code
        boolean is_healthy
        varchar error_type
        text error_message
        integer response_bytes
    }

    INCIDENTS {
        bigserial id PK
        bigint endpoint_id FK
        varchar status
        timestamp started_at
        timestamp acknowledged_at
        timestamp resolved_at
        varchar root_cause
        text error_summary
        integer failure_count
    }

    INCIDENT_EVENTS {
        bigserial id PK
        bigint incident_id FK
        varchar event_type
        text description
        timestamp created_at
    }

    NOTIFICATIONS {
        bigserial id PK
        bigint user_id FK
        bigint incident_id FK
        varchar channel
        varchar status
        varchar title
        text message
        boolean is_read
        timestamp created_at
    }

    USER_SETTINGS {
        bigserial id PK
        bigint user_id FK,UK
        boolean email_notifications_enabled
        varchar notification_email
        integer default_check_interval
        integer default_failure_threshold
    }
```

### Critical Indexes
- `idx_endpoints_user_id`: Fast filtering of user endpoints.
- `idx_endpoints_scheduling`: `(is_active, last_checked_at)` for rapid polling of due checks.
- `idx_checks_endpoint_time`: `(endpoint_id, checked_at DESC)` for latency graphs and uptime percentile calculations.
- `idx_incidents_endpoint_status`: `(endpoint_id, status)` for active incident evaluation and deduplication.

---

## 4. Automated Monitoring Engine Architecture

The monitoring engine is strictly isolated from inbound REST API request threads:

```mermaid
sequenceDiagram
    autonumber
    participant S as Scheduler (@Scheduled)
    participant Q as Bounded ThreadPool
    participant W as HealthCheckWorker
    participant SSRF as SSRF Validator
    participant HTTP as Safe HttpClient
    participant Ext as Target Server
    participant Pol as Health Policy Evaluator
    participant DB as Database (JPA / Flyway)
    participant Inc as Incident Manager

    S->>DB: Fetch endpoints where is_active=true AND due for check
    S->>Q: Dispatch endpoint task (if not already in-flight)
    Q->>W: Run check task
    W->>SSRF: Validate URL & resolved IP address
    alt SSRF / Prohibited IP
        SSRF-->>W: ProhibitedDestinationException
        W->>DB: Save HealthCheckResult (is_healthy=false, SSRF_BLOCKED)
        W->>Pol: Evaluate failure
    else URL Validated
        W->>HTTP: Execute HTTP Request (with timeouts)
        HTTP->>Ext: GET / HEAD request
        Ext-->>HTTP: HTTP Status & Headers & Truncated Body
        HTTP-->>W: ExecutionResult (status, latencyMs, bytes)
        W->>DB: Save HealthCheckResult
        W->>Pol: Evaluate (Status in Expected, Latency <= Threshold)
    end
    Pol->>Inc: State Transition (Consecutive Failures/Recovery)
    Inc->>DB: Open / Update / Resolve Incident
```

---

## 5. Analytics & Dashboard Metrics Calculation

1. **Uptime Percentage**:
   $$\text{Availability } (\%) = \frac{\text{Count of Healthy Checks within Window}}{\text{Total Completed Checks within Window}} \times 100$$
   Window options: 1 Hour, 24 Hours, 7 Days, 30 Days.
2. **Latency Percentiles (p50, p95, p99)**:
   - Uses SQL `PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY response_time_ms)` in PostgreSQL, or Java in-memory sorting of windowed samples for universal dialect compatibility.
   - Distinguishes between average latency and tail percentiles.
3. **Incident MTTR (Mean Time to Resolution)**:
   - Evaluated as $\frac{\sum (\text{resolved\_at} - \text{started\_at})}{\text{Count of Resolved Incidents}}$.

---

## 6. Implementation Phases

- **Phase 1**: Environment verification, toolchain setup, architecture specification, directory scaffolding.
- **Phase 2**: Backend foundation: Spring Boot 3.3, Flyway migrations, JPA entities, JWT authentication, Security filters, User and Endpoint REST APIs, unit/integration tests.
- **Phase 3**: Monitoring engine: SSRF validator, safe HTTP client, bounded thread pool scheduler, manual check triggers, automated checks persistence.
- **Phase 4**: Incident lifecycle management, failure threshold tracking, recovery detection, notification dispatch, metrics and percentiles engine.
- **Phase 5**: Frontend: React 18, Vite, TypeScript, Tailwind CSS, Recharts, dark observability design, interactive dashboard, endpoint management, incident timeline, real API integration.
- **Phase 6**: End-to-end integration, CORS configuration, system validation tests, complete workflow verification.
- **Phase 7**: Reliability hardening, SMTP email notification handling, OpenAPI/Swagger 3 documentation.
- **Phase 8**: Production Docker & Docker Compose setup, comprehensive README, test suite execution, deployment guide.
