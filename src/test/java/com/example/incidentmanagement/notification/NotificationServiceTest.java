package com.example.incidentmanagement.notification;

import com.example.incidentmanagement.model.Severity;
import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private Resend resend;

    @Mock
    private Emails emailsComponent;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties(List.of("oncall@example.com"));
        notificationService = new NotificationService(resend, properties);
        // @Value fields aren't populated outside a Spring context.
        ReflectionTestUtils.setField(notificationService, "fromAddress", "alerts@example.com");
    }

    @Test
    void sendIncidentNotificationAsync_sendsEmailWithExpectedContent() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 8, 10, 0);
        when(resend.emails()).thenReturn(emailsComponent);
        when(emailsComponent.send(any(CreateEmailOptions.class)))
                .thenReturn(new CreateEmailResponse("email-id-123"));

        notificationService.sendIncidentNotificationAsync(
                "DB down", Severity.CRITICAL, "DB unreachable", "Restart primary", timestamp);

        ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(emailsComponent).send(captor.capture());

        CreateEmailOptions sent = captor.getValue();
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
    void sendIncidentNotificationAsync_resendThrows_doesNotPropagate() throws Exception {
        when(resend.emails()).thenReturn(emailsComponent);
        doThrow(new RuntimeException("Resend API down"))
                .when(emailsComponent).send(any(CreateEmailOptions.class));

        assertThatCode(() -> notificationService.sendIncidentNotificationAsync(
                "DB down", Severity.HIGH, "summary", "action", LocalDateTime.now()))
                .doesNotThrowAnyException();
    }
}
