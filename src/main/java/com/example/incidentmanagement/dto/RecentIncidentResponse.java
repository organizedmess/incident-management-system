package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.IncidentStatus;
import com.example.incidentmanagement.model.Severity;

import java.time.LocalDateTime;

/**
 * Deliberately lighter than IncidentResponse: skips description entirely so the
 * /analytics/recent widget query never fetches that TEXT column from the DB.
 */
public record RecentIncidentResponse(
        Long id,
        String title,
        Severity severity,
        IncidentStatus status,
        LocalDateTime createdAt
) {
}
