package com.example.incidentmanagement.notification;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.incidentmanagement.model.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Sole point of contact with the mail system. Knows nothing about Incident, IncidentAnalysis,
 * or why a notification is being sent -- it only turns a fixed set of fields into an email.
 * Keeping it this narrow mirrors GeminiService's role in the AI module: a single, isolated,
 * independently-testable side effect.
 *
 * <p>Sends via Resend's HTTPS API rather than SMTP because Render's free tier blocks outbound
 * SMTP (port 587), which made JavaMailSender/Gmail unusable in production.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final Resend resend;
    private final NotificationProperties notificationProperties;

    @Value("${resend.from}")
    private String fromAddress;

    @Async
    public void sendIncidentNotificationAsync(String title, Severity severity, String summary,
                                               String recommendedAction, LocalDateTime timestamp) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(notificationProperties.recipients())
                    .subject("[%s] Incident Alert: %s".formatted(severity, title))
                    .text("""
                            Title: %s
                            Severity: %s
                            Summary: %s
                            Recommended Action: %s
                            Timestamp: %s
                            """.formatted(title, severity, summary, recommendedAction, timestamp))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Incident notification email sent successfully for incident: {} (resend id: {})",
                    title, response.getId());
        } catch (Exception ex) {
            // Notification is best-effort: a mail failure must never propagate or affect
            // anything else in the request/analysis flow that triggered it.
            log.error("Failed to send incident notification email for '{}': {}", title, ex.getMessage(), ex);
        }
    }
}
