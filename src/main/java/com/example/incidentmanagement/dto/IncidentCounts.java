package com.example.incidentmanagement.dto;

/**
 * Internal aggregation carrier for the single-row COUNT query behind /analytics/overview.
 * Not returned over HTTP directly -- AnalyticsService assembles the final
 * AnalyticsOverviewResponse from this plus the separately-queried average priority score.
 */
public record IncidentCounts(long total, long open, long resolved, long critical) {
}
