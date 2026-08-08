package com.example.incidentmanagement.ai;

import com.example.incidentmanagement.dto.IncidentAnalysisResponse;
import com.example.incidentmanagement.exception.IncidentNotFoundException;
import com.example.incidentmanagement.model.AnalysisStatus;
import com.example.incidentmanagement.model.IncidentAnalysis;
import com.example.incidentmanagement.model.Severity;
import com.example.incidentmanagement.notification.IncidentNotificationService;
import com.example.incidentmanagement.repository.IncidentAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bridges the AI module to the rest of the app. This is the only class the business layer
 * (IncidentService) talks to -- it never sees GeminiService, Gemini's API, or JSON parsing.
 *
 * analyzeAsync runs off the request thread so incident creation is never slowed down by
 * Gemini, and its own catch-all guarantees an IncidentAnalysis row is always written -- with
 * a real result on success, or a recorded error on failure -- so a failing AI call can never
 * surface as a broken request or an unhandled exception. Once the analysis row is written, it
 * hands off to IncidentNotificationService, which decides whether the severity warrants an
 * email -- this class doesn't know or care what "notify" means.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentAnalysisService {

    private static final String NO_SUMMARY = "AI analysis unavailable.";
    private static final String NO_RECOMMENDED_ACTION = "No recommended action available.";

    private final GeminiService geminiService;
    private final IncidentAnalysisRepository incidentAnalysisRepository;
    private final IncidentNotificationService incidentNotificationService;

    @Async
    public void analyzeAsync(Long incidentId, String title, String description, Severity severity) {
        IncidentAnalysis analysis;
        try {
            GeminiAnalysisResult result = geminiService.analyze(title, description, severity);
            analysis = IncidentAnalysis.builder()
                    .incidentId(incidentId)
                    .summary(result.summary())
                    .rootCause(result.rootCause())
                    .recommendedAction(result.recommendedAction())
                    .priorityScore(result.priorityScore())
                    .status(AnalysisStatus.COMPLETED)
                    .build();
        } catch (Exception ex) {
            // Deliberately catches any Exception, not just GeminiAnalysisException: whatever
            // goes wrong here must end in a stored error row, never an escaping exception.
            log.error("AI analysis failed for incident {}: {}", incidentId, ex.getMessage(), ex);
            analysis = IncidentAnalysis.builder()
                    .incidentId(incidentId)
                    .status(AnalysisStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .build();
        }
        incidentAnalysisRepository.save(analysis);

        // A HIGH/CRITICAL incident is notified regardless of whether AI analysis itself
        // succeeded -- a human still needs to know, just with fallback text if there's no
        // real summary/recommendedAction yet.
        incidentNotificationService.notifyIfSevere(
                title,
                severity,
                analysis.getSummary() != null ? analysis.getSummary() : NO_SUMMARY,
                analysis.getRecommendedAction() != null ? analysis.getRecommendedAction() : NO_RECOMMENDED_ACTION,
                analysis.getCreatedAt());
    }

    public IncidentAnalysisResponse getAnalysis(Long incidentId) {
        IncidentAnalysis analysis = incidentAnalysisRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(
                        "No analysis found for incident: " + incidentId));
        return toResponse(analysis);
    }

    private IncidentAnalysisResponse toResponse(IncidentAnalysis analysis) {
        return new IncidentAnalysisResponse(
                analysis.getId(),
                analysis.getIncidentId(),
                analysis.getSummary(),
                analysis.getRootCause(),
                analysis.getRecommendedAction(),
                analysis.getPriorityScore(),
                analysis.getStatus(),
                analysis.getErrorMessage(),
                analysis.getCreatedAt()
        );
    }
}
