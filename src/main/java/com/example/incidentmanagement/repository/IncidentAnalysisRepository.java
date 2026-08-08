package com.example.incidentmanagement.repository;

import com.example.incidentmanagement.model.IncidentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, Long> {

    Optional<IncidentAnalysis> findByIncidentId(Long incidentId);

    // AVG ignores NULLs on its own (FAILED rows have priorityScore = null), so the explicit
    // status filter is a defensive statement of intent rather than a strict necessity. Returns
    // null (not 0) when there are zero COMPLETED rows -- handled by AnalyticsService.
    @Query("""
            SELECT AVG(a.priorityScore)
            FROM IncidentAnalysis a
            WHERE a.status = com.example.incidentmanagement.model.AnalysisStatus.COMPLETED
            """)
    Double getAveragePriorityScore();
}
