package com.example.incidentmanagement.notification;

import com.example.incidentmanagement.model.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Sole point of contact with the mail system. Knows nothing about Incident, IncidentAnalysis,
 * or why a notification is being sent -- it only turns a fixed set of fields into an email.
 * Keeping it this narrow mirrors GeminiService's role in the AI module: a single, isolated,
 * independently-testable side effect.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendIncidentNotificationAsync(String title, Severity severity, String summary,
                                               String recommendedAction, LocalDateTime timestamp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(notificationProperties.recipients().toArray(new String[0]));
            message.setSubject("[%s] Incident Alert: %s".formatted(severity, title));
            message.setText("""
                    Title: %s
                    Severity: %s
                    Summary: %s
                    Recommended Action: %s
                    Timestamp: %s
                    """.formatted(title, severity, summary, recommendedAction, timestamp));

            mailSender.send(message);
            log.info("Sent incident notification email for '{}' to {}", title, notificationProperties.recipients());
        } catch (Exception ex) {
            // Notification is best-effort: a mail failure must never propagate or affect
            // anything else in the request/analysis flow that triggered it.
            log.error("Failed to send incident notification email for '{}': {}", title, ex.getMessage(), ex);
        }
    }
}
