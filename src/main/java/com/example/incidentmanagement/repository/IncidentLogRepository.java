package com.example.incidentmanagement.repository;

import com.example.incidentmanagement.model.IncidentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentLogRepository extends JpaRepository<IncidentLog, Long> {
}
