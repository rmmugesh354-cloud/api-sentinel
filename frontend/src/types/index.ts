export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  fullName: string;
  role: string;
}

export type HttpMethod = 'GET' | 'HEAD' | 'POST';

export type EndpointStatus = 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'UNKNOWN' | 'PAUSED';

export interface MonitoredEndpoint {
  id: number;
  userId: number;
  name: string;
  url: string;
  httpMethod: HttpMethod;
  expectedStatusCodes: string;
  checkIntervalSeconds: number;
  timeoutMillis: number;
  latencyThresholdMillis: number;
  isActive: boolean;
  consecutiveFailuresThreshold: number;
  consecutiveFailuresCount: number;
  consecutiveSuccessesCount: number;
  currentStatus: EndpointStatus;
  lastCheckedAt: string | null;
  nextCheckAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface EndpointRequest {
  name: string;
  url: string;
  httpMethod: HttpMethod;
  expectedStatusCodes: string;
  checkIntervalSeconds: number;
  timeoutMillis: number;
  latencyThresholdMillis: number;
  consecutiveFailuresThreshold: number;
}

export interface HealthCheckResult {
  id: number;
  endpointId: number;
  checkedAt: string;
  responseTimeMs: number;
  httpStatusCode: number | null;
  isHealthy: boolean;
  errorReason: string | null;
  errorDetails: string | null;
  responseSizeBytes: number;
}

export type IncidentStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';

export type IncidentEventType = 'INCIDENT_OPENED' | 'INCIDENT_ACKNOWLEDGED' | 'INCIDENT_RESOLVED' | 'FAILURE_RECORDED';

export interface IncidentEvent {
  id: number;
  incidentId: number;
  eventType: IncidentEventType;
  description: string;
  createdAt: string;
}

export interface Incident {
  id: number;
  endpointId: number;
  endpointName: string;
  endpointUrl: string;
  status: IncidentStatus;
  startedAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  durationSeconds: number | null;
  rootCause: string;
  errorSummary: string | null;
  failureCount: number;
  events: IncidentEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface Notification {
  id: number;
  userId: number;
  incidentId: number | null;
  channel: 'IN_APP' | 'EMAIL';
  status: 'SENT' | 'FAILED' | 'PENDING';
  title: string;
  message: string;
  isRead: boolean;
  errorDetails?: string | null;
  createdAt: string;
}

export interface DashboardSummary {
  totalEndpoints: number;
  healthyEndpoints: number;
  degradedEndpoints: number;
  unhealthyEndpoints: number;
  pausedEndpoints: number;
  openIncidents: number;
  overallAvailabilityPercentage: number;
  recentFailures: HealthCheckResult[];
  recentIncidents: Incident[];
}

export interface LatencyPoint {
  timestamp: string;
  latencyMs: number;
  isHealthy: boolean;
  statusCode: number | null;
}

export interface LatencyMetrics {
  p50Ms: number;
  p95Ms: number;
  p99Ms: number;
  averageMs: number;
  minMs: number;
  maxMs: number;
  sampleCount: number;
  timeSeries: LatencyPoint[];
}

export interface AvailabilityMetrics {
  window: string;
  availabilityPercentage: number;
  totalChecks: number;
  successfulChecks: number;
  failedChecks: number;
  calculationDescription: string;
}

export interface UserSettings {
  id: number;
  userId: number;
  emailNotificationsEnabled: boolean;
  notificationEmail: string | null;
  defaultCheckIntervalSeconds: number;
  defaultConsecutiveFailures: number;
}
