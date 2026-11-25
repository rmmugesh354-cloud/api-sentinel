package com.sentinel.apisentinel.check;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HealthCheckResultRepository extends JpaRepository<HealthCheckResult, Long> {

    Page<HealthCheckResult> findAllByEndpointIdOrderByCheckedAtDesc(Long endpointId, Pageable pageable);

    List<HealthCheckResult> findTop50ByEndpointIdOrderByCheckedAtDesc(Long endpointId);

    List<HealthCheckResult> findAllByEndpointIdAndCheckedAtAfterOrderByCheckedAtAsc(Long endpointId, Instant since);

    long countByEndpointIdAndCheckedAtAfter(Long endpointId, Instant since);

    long countByEndpointIdAndCheckedAtAfterAndIsHealthyTrue(Long endpointId, Instant since);

    @Query("SELECT COUNT(h) FROM HealthCheckResult h WHERE h.endpoint.user.id = :userId AND h.checkedAt >= :since")
    long countUserChecksSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("SELECT COUNT(h) FROM HealthCheckResult h WHERE h.endpoint.user.id = :userId AND h.checkedAt >= :since AND h.isHealthy = true")
    long countUserHealthyChecksSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("SELECT h.responseTimeMs FROM HealthCheckResult h WHERE h.endpoint.user.id = :userId AND h.checkedAt >= :since ORDER BY h.responseTimeMs ASC")
    List<Integer> findUserResponseTimesSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("SELECT h.responseTimeMs FROM HealthCheckResult h WHERE h.endpoint.id = :endpointId AND h.checkedAt >= :since ORDER BY h.responseTimeMs ASC")
    List<Integer> findEndpointResponseTimesSince(@Param("endpointId") Long endpointId, @Param("since") Instant since);

    @Query("SELECT h FROM HealthCheckResult h WHERE h.endpoint.user.id = :userId AND h.isHealthy = false ORDER BY h.checkedAt DESC")
    List<HealthCheckResult> findRecentUserFailures(@Param("userId") Long userId, Pageable pageable);
}
