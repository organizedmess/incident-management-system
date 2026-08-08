package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public record IncidentStatusUpdateRequest(
        @NotNull(message = "Status is required") IncidentStatus status
) {
}
