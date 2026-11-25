package com.sentinel.apisentinel.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentEventRepository extends JpaRepository<IncidentEvent, Long> {
    List<IncidentEvent> findAllByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
