package com.example.incidentmanagement.analytics;

import com.example.incidentmanagement.dto.AnalyticsOverviewResponse;
import com.example.incidentmanagement.dto.DailyCountResponse;
import com.example.incidentmanagement.dto.RecentIncidentResponse;
import com.example.incidentmanagement.dto.SeverityCountResponse;
import com.example.incidentmanagement.dto.StatusCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    @GetMapping("/severity")
    public ResponseEntity<List<SeverityCountResponse>> getSeverityBreakdown() {
        return ResponseEntity.ok(analyticsService.getSeverityBreakdown());
    }

    @GetMapping("/status")
    public ResponseEntity<List<StatusCountResponse>> getStatusBreakdown() {
        return ResponseEntity.ok(analyticsService.getStatusBreakdown());
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyCountResponse>> getDailyCounts() {
        return ResponseEntity.ok(analyticsService.getDailyCounts());
    }

    @GetMapping("/trends")
    public ResponseEntity<List<DailyCountResponse>> getTrends() {
        return ResponseEntity.ok(analyticsService.getTrends());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentIncidentResponse>> getRecentIncidents() {
        return ResponseEntity.ok(analyticsService.getRecentIncidents());
    }
}
