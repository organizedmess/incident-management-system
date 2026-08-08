package com.example.incidentmanagement.repository;

import com.example.incidentmanagement.dto.IncidentCounts;
import com.example.incidentmanagement.dto.RecentIncidentResponse;
import com.example.incidentmanagement.dto.SeverityCountResponse;
import com.example.incidentmanagement.dto.StatusCountResponse;
import com.example.incidentmanagement.model.Incident;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Single-row aggregate: one pass over the table with conditional counts, instead of four
    // separate COUNT queries. COALESCE guards against SUM(...) returning SQL NULL (not 0) when
    // zero rows match a given CASE branch -- IncidentCounts' fields are primitive long, so a
    // NULL binding there would throw.
    @Query("""
            SELECT new com.example.incidentmanagement.dto.IncidentCounts(
                COUNT(i),
                COALESCE(SUM(CASE WHEN i.status = com.example.incidentmanagement.model.IncidentStatus.OPEN THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN i.status = com.example.incidentmanagement.model.IncidentStatus.RESOLVED THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN i.severity = com.example.incidentmanagement.model.Severity.CRITICAL THEN 1L ELSE 0L END), 0L)
            )
            FROM Incident i
            """)
    IncidentCounts getIncidentCounts();

    // GROUP BY pushed to the DB; only ever returns as many rows as there are Severity values
    // (4), never loads full Incident entities.
    @Query("""
            SELECT new com.example.incidentmanagement.dto.SeverityCountResponse(i.severity, COUNT(i))
            FROM Incident i
            GROUP BY i.severity
            """)
    List<SeverityCountResponse> countGroupedBySeverity();

    @Query("""
            SELECT new com.example.incidentmanagement.dto.StatusCountResponse(i.status, COUNT(i))
            FROM Incident i
            GROUP BY i.status
            """)
    List<StatusCountResponse> countGroupedByStatus();

    // Native query + interface projection (see DailyIncidentCountProjection) instead of a JPQL
    // constructor expression: Hibernate 6's HQL constructor-matching for SELECT NEW rejects
    // temporal-extraction function results (tried both FUNCTION('date', ...) and the built-in
    // date() function; both throw "Missing constructor" at query-validation time despite being
    // semantically valid). Native SQL skips that HQL-level check entirely.
    @Query(value = """
            SELECT CAST(created_at AS date) AS day, COUNT(*) AS count
            FROM incidents
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<DailyIncidentCountProjection> countGroupedByDay();

    @Query(value = """
            SELECT CAST(created_at AS date) AS day, COUNT(*) AS count
            FROM incidents
            WHERE created_at >= :since
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<DailyIncidentCountProjection> countGroupedByDaySince(@Param("since") LocalDateTime since);

    // Pageable here is used purely for its LIMIT/OFFSET translation (PageRequest.of(0, 10)),
    // not for returning a Page<> -- the ORDER BY is baked into the query itself.
    @Query("""
            SELECT new com.example.incidentmanagement.dto.RecentIncidentResponse(
                i.id, i.title, i.severity, i.status, i.createdAt)
            FROM Incident i
            ORDER BY i.createdAt DESC
            """)
    List<RecentIncidentResponse> findRecentIncidents(Pageable pageable);
}
