package com.example.incidentmanagement.analytics;

import com.example.incidentmanagement.dto.AnalyticsOverviewResponse;
import com.example.incidentmanagement.dto.DailyCountResponse;
import com.example.incidentmanagement.dto.IncidentCounts;
import com.example.incidentmanagement.dto.RecentIncidentResponse;
import com.example.incidentmanagement.dto.SeverityCountResponse;
import com.example.incidentmanagement.dto.StatusCountResponse;
import com.example.incidentmanagement.model.IncidentStatus;
import com.example.incidentmanagement.model.Severity;
import com.example.incidentmanagement.repository.DailyIncidentCountProjection;
import com.example.incidentmanagement.repository.IncidentAnalysisRepository;
import com.example.incidentmanagement.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read-only aggregation layer for the dashboard. Every method here either delegates a single
 * DB-side aggregate query to the repository, or takes that query's (possibly sparse) result and
 * fills in the zero-count gaps a chart needs -- it never loads full Incident entities to compute
 * a count in Java.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int RECENT_INCIDENTS_LIMIT = 10;
    private static final int TREND_WINDOW_DAYS = 30;

    private final IncidentRepository incidentRepository;
    private final IncidentAnalysisRepository incidentAnalysisRepository;

    public AnalyticsOverviewResponse getOverview() {
        IncidentCounts counts = incidentRepository.getIncidentCounts();
        Double rawAverage = incidentAnalysisRepository.getAveragePriorityScore();
        double averagePriorityScore = rawAverage != null ? Math.round(rawAverage * 100.0) / 100.0 : 0.0;

        return new AnalyticsOverviewResponse(
                counts.total(),
                counts.open(),
                counts.resolved(),
                counts.critical(),
                averagePriorityScore
        );
    }

    // Fills in every Severity with 0 if the DB result is missing it, so a pie/bar chart always
    // renders all categories instead of only the ones that happen to have data.
    public List<SeverityCountResponse> getSeverityBreakdown() {
        Map<Severity, Long> counts = incidentRepository.countGroupedBySeverity().stream()
                .collect(Collectors.toMap(SeverityCountResponse::severity, SeverityCountResponse::count));

        return Arrays.stream(Severity.values())
                .map(severity -> new SeverityCountResponse(severity, counts.getOrDefault(severity, 0L)))
                .toList();
    }

    public List<StatusCountResponse> getStatusBreakdown() {
        Map<IncidentStatus, Long> counts = incidentRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(StatusCountResponse::status, StatusCountResponse::count));

        return Arrays.stream(IncidentStatus.values())
                .map(status -> new StatusCountResponse(status, counts.getOrDefault(status, 0L)))
                .toList();
    }

    // All-time, sparse: only days that actually have incidents are returned. No fixed window,
    // so zero-filling isn't meaningful here the way it is for the fixed 30-day trend below.
    public List<DailyCountResponse> getDailyCounts() {
        return incidentRepository.countGroupedByDay().stream()
                .map(p -> new DailyCountResponse(p.getDay(), p.getCount()))
                .toList();
    }

    // Fixed 30-day window, zero-filled for every day (including days with no incidents) so the
    // response is a complete, evenly-spaced time series a line/area chart can plot directly
    // without the frontend having to fill gaps itself.
    public List<DailyCountResponse> getTrends() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(TREND_WINDOW_DAYS - 1L);

        Map<LocalDate, Long> counts = incidentRepository.countGroupedByDaySince(startDate.atStartOfDay()).stream()
                .collect(Collectors.toMap(DailyIncidentCountProjection::getDay, DailyIncidentCountProjection::getCount));

        return Stream.iterate(startDate, day -> day.plusDays(1))
                .limit(TREND_WINDOW_DAYS)
                .map(day -> new DailyCountResponse(day, counts.getOrDefault(day, 0L)))
                .toList();
    }

    public List<RecentIncidentResponse> getRecentIncidents() {
        return incidentRepository.findRecentIncidents(PageRequest.of(0, RECENT_INCIDENTS_LIMIT));
    }
}
