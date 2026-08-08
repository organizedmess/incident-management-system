package com.example.incidentmanagement.dto;

import com.example.incidentmanagement.model.IncidentStatus;

public record StatusCountResponse(IncidentStatus status, long count) {
}
