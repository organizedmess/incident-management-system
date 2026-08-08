package com.example.incidentmanagement.analytics;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAnalysisRepository incidentAnalysisRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getOverview_mapsCountsAndRoundsAverage() {
        when(incidentRepository.getIncidentCounts()).thenReturn(new IncidentCounts(10L, 4L, 5L, 2L));
        when(incidentAnalysisRepository.getAveragePriorityScore()).thenReturn(72.345);

        var overview = analyticsService.getOverview();

        assertThat(overview.totalIncidents()).isEqualTo(10L);
        assertThat(overview.openIncidents()).isEqualTo(4L);
        assertThat(overview.resolvedIncidents()).isEqualTo(5L);
        assertThat(overview.criticalIncidents()).isEqualTo(2L);
        assertThat(overview.averagePriorityScore()).isEqualTo(72.35);
    }

    @Test
    void getOverview_nullAverage_defaultsToZero() {
        when(incidentRepository.getIncidentCounts()).thenReturn(new IncidentCounts(0L, 0L, 0L, 0L));
        when(incidentAnalysisRepository.getAveragePriorityScore()).thenReturn(null);

        var overview = analyticsService.getOverview();

        assertThat(overview.averagePriorityScore()).isEqualTo(0.0);
    }

    @Test
    void getSeverityBreakdown_fillsZeroForMissingSeverities() {
        when(incidentRepository.countGroupedBySeverity())
                .thenReturn(List.of(new SeverityCountResponse(Severity.HIGH, 3L)));

        List<SeverityCountResponse> result = analyticsService.getSeverityBreakdown();

        Map<Severity, Long> byCounts = result.stream()
                .collect(java.util.stream.Collectors.toMap(SeverityCountResponse::severity, SeverityCountResponse::count));

        assertThat(result).hasSize(Severity.values().length);
        assertThat(byCounts.get(Severity.HIGH)).isEqualTo(3L);
        assertThat(byCounts.get(Severity.LOW)).isEqualTo(0L);
        assertThat(byCounts.get(Severity.MEDIUM)).isEqualTo(0L);
        assertThat(byCounts.get(Severity.CRITICAL)).isEqualTo(0L);
    }

    @Test
    void getStatusBreakdown_fillsZeroForMissingStatuses() {
        when(incidentRepository.countGroupedByStatus())
                .thenReturn(List.of(new StatusCountResponse(IncidentStatus.OPEN, 7L)));

        List<StatusCountResponse> result = analyticsService.getStatusBreakdown();

        Map<IncidentStatus, Long> byCounts = result.stream()
                .collect(java.util.stream.Collectors.toMap(StatusCountResponse::status, StatusCountResponse::count));

        assertThat(result).hasSize(IncidentStatus.values().length);
        assertThat(byCounts.get(IncidentStatus.OPEN)).isEqualTo(7L);
        assertThat(byCounts.get(IncidentStatus.ACKNOWLEDGED)).isEqualTo(0L);
        assertThat(byCounts.get(IncidentStatus.RESOLVED)).isEqualTo(0L);
    }

    private record TestDailyProjection(LocalDate day, Long count) implements DailyIncidentCountProjection {
        @Override
        public LocalDate getDay() {
            return day;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }

    @Test
    void getDailyCounts_mapsProjectionToResponse() {
        List<DailyIncidentCountProjection> sparse = List.of(
                new TestDailyProjection(LocalDate.of(2026, 1, 1), 2L),
                new TestDailyProjection(LocalDate.of(2026, 3, 15), 5L)
        );
        when(incidentRepository.countGroupedByDay()).thenReturn(sparse);

        assertThat(analyticsService.getDailyCounts()).containsExactly(
                new DailyCountResponse(LocalDate.of(2026, 1, 1), 2L),
                new DailyCountResponse(LocalDate.of(2026, 3, 15), 5L)
        );
    }

    @Test
    void getTrends_returnsThirtyDaysZeroFilledAroundActualData() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysAgo = today.minusDays(3);
        when(incidentRepository.countGroupedByDaySince(any(LocalDateTime.class)))
                .thenReturn(List.of(new TestDailyProjection(threeDaysAgo, 9L)));

        List<DailyCountResponse> trends = analyticsService.getTrends();

        assertThat(trends).hasSize(30);
        assertThat(trends.get(0).day()).isEqualTo(today.minusDays(29));
        assertThat(trends.get(29).day()).isEqualTo(today);
        Map<LocalDate, Long> byDay = trends.stream()
                .collect(java.util.stream.Collectors.toMap(DailyCountResponse::day, DailyCountResponse::count));
        assertThat(byDay.get(threeDaysAgo)).isEqualTo(9L);
        assertThat(byDay.get(today)).isEqualTo(0L);
    }

    @Test
    void getRecentIncidents_requestsTopTenMostRecent() {
        RecentIncidentResponse incident = new RecentIncidentResponse(
                1L, "Title", Severity.HIGH, IncidentStatus.OPEN, LocalDateTime.now());
        when(incidentRepository.findRecentIncidents(any(Pageable.class))).thenReturn(List.of(incident));

        List<RecentIncidentResponse> result = analyticsService.getRecentIncidents();

        assertThat(result).containsExactly(incident);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(incidentRepository).findRecentIncidents(captor.capture());
        assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 10));
    }
}
