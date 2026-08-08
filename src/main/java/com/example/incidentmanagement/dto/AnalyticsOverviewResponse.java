package com.example.incidentmanagement.dto;

public record AnalyticsOverviewResponse(
        long totalIncidents,
        long openIncidents,
        long resolvedIncidents,
        long criticalIncidents,
        double averagePriorityScore
) {
}
