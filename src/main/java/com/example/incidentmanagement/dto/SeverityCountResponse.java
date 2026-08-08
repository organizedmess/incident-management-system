package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.Severity;

public record SeverityCountResponse(Severity severity, long count) {
}
