package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.IncidentStatus;
import com.example.incidentmanagement.model.Severity;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        Severity severity,
        IncidentStatus status,
        String source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
