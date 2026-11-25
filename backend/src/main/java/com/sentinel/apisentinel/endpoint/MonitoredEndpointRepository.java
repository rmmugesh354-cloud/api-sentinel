package com.sentinel.apisentinel.endpoint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredEndpointRepository extends JpaRepository<MonitoredEndpoint, Long> {

    Page<MonitoredEndpoint> findAllByUserId(Long userId, Pageable pageable);

    List<MonitoredEndpoint> findAllByUserId(Long userId);

    Optional<MonitoredEndpoint> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndCurrentStatus(Long userId, EndpointStatus currentStatus);

    long countByUserIdAndIsActive(Long userId, boolean isActive);

    @Query("SELECT e FROM MonitoredEndpoint e WHERE e.isActive = true AND e.nextCheckAt <= :now ORDER BY e.nextCheckAt ASC")
    List<MonitoredEndpoint> findDueEndpoints(@Param("now") Instant now, Pageable pageable);
}
