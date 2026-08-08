package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.AnalysisStatus;

import java.time.LocalDateTime;

public record IncidentAnalysisResponse(
        Long id,
        Long incidentId,
        String summary,
        String rootCause,
        String recommendedAction,
        Integer priorityScore,
        AnalysisStatus status,
        String errorMessage,
        LocalDateTime createdAt
) {
}
