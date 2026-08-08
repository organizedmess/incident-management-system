package com.example.incidentmanagement.notification;

import com.example.incidentmanagement.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties(List.of("oncall@example.com"));
        notificationService = new NotificationService(mailSender, properties);
        // @Value fields aren't populated outside a Spring context.
        ReflectionTestUtils.setField(notificationService, "fromAddress", "alerts@example.com");
    }

    @Test
    void sendIncidentNotificationAsync_sendsEmailWithExpectedContent() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 8, 10, 0);

        notificationService.sendIncidentNotificationAsync(
                "DB down", Severity.CRITICAL, "DB unreachable", "Restart primary", timestamp);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("alerts@example.com");
        assertThat(sent.getTo()).containsExactly("oncall@example.com");
        assertThat(sent.getSubject()).isEqualTo("[CRITICAL] Incident Alert: DB down");
        assertThat(sent.getText())
                .contains("DB down")
                .contains("CRITICAL")
                .contains("DB unreachable")
                .contains("Restart primary")
                .contains(timestamp.toString());
    }

    @Test
    void sendIncidentNotificationAsync_mailSenderThrows_doesNotPropagate() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> notificationService.sendIncidentNotificationAsync(
                "DB down", Severity.HIGH, "summary", "action", LocalDateTime.now()))
                .doesNotThrowAnyException();
    }
}
