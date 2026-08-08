package com.example.incidentmanagement.dto;

import java.time.LocalDate;

/**
 * Shared shape for /analytics/daily (all-time, sparse) and /analytics/trends
 * (fixed 30-day window, zero-filled) -- both are just "incident count per day."
 */
public record DailyCountResponse(LocalDate day, long count) {
}
