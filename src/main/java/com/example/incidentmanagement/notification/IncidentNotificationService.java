package com.example.incidentmanagement.notification;

import com.example.incidentmanagement.exception.IncidentNotFoundException;
import com.example.incidentmanagement.model.Incident;
import com.example.incidentmanagement.model.IncidentAnalysis;
import com.example.incidentmanagement.model.Severity;
import com.example.incidentmanagement.repository.IncidentAnalysisRepository;
import com.example.incidentmanagement.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bridges incident/analysis data to the mail-sending concern. This is the single place that
 * knows both "when should we notify" (severity gating) and "where does email content come
 * from" (Incident + IncidentAnalysis), so that logic exists exactly once and is shared by both
 * the automatic post-analysis trigger and the manual resend endpoint.
 */
@Service
@RequiredArgsConstructor
public class IncidentNotificationService {

    private static final String NO_SUMMARY = "AI analysis not available for this incident.";
    private static final String NO_RECOMMENDED_ACTION = "No recommended action available.";

    private final IncidentRepository incidentRepository;
    private final IncidentAnalysisRepository incidentAnalysisRepository;
    private final NotificationService notificationService;

    // Called after AI analysis finishes (success or failure). Only HIGH/CRITICAL incidents
    // are notified automatically; summary/recommendedAction may already carry a fallback
    // string from the caller if analysis itself failed.
    public void notifyIfSevere(String title, Severity severity, String summary,
                                String recommendedAction, LocalDateTime timestamp) {
        if (severity != Severity.HIGH && severity != Severity.CRITICAL) {
            return;
        }
        notificationService.sendIncidentNotificationAsync(title, severity, summary, recommendedAction, timestamp);
    }

    // Manual resend: bypasses the severity gate entirely, since a human is explicitly asking
    // for this incident to be (re-)notified regardless of its severity.
    public void resendNotification(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + incidentId));

        Optional<IncidentAnalysis> analysis = incidentAnalysisRepository.findByIncidentId(incidentId);
        String summary = analysis.map(IncidentAnalysis::getSummary)
                .filter(s -> s != null && !s.isBlank())
                .orElse(NO_SUMMARY);
        String recommendedAction = analysis.map(IncidentAnalysis::getRecommendedAction)
                .filter(s -> s != null && !s.isBlank())
                .orElse(NO_RECOMMENDED_ACTION);

        notificationService.sendIncidentNotificationAsync(
                incident.getTitle(), incident.getSeverity(), summary, recommendedAction, incident.getCreatedAt());
    }
}
