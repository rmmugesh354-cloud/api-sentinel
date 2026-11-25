package com.sentinel.apisentinel.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByEndpointIdAndStatusIn(Long endpointId, Collection<IncidentStatus> statuses);

    Page<Incident> findAllByEndpointUserId(Long userId, Pageable pageable);

    Page<Incident> findAllByEndpointUserIdAndStatus(Long userId, IncidentStatus status, Pageable pageable);

    Optional<Incident> findByIdAndEndpointUserId(Long id, Long userId);

    long countByEndpointUserIdAndStatus(Long userId, IncidentStatus status);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.endpoint.user.id = :userId AND i.status != 'RESOLVED'")
    long countActiveIncidentsForUser(@Param("userId") Long userId);

    List<Incident> findTop10ByEndpointUserIdOrderByStartedAtDesc(Long userId);
}
