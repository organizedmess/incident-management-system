package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentCreateRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Severity is required") Severity severity,
        @NotBlank(message = "Source is required") String source
) {
}
