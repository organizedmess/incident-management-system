package com.example.incidentmanagement.notification;

import com.example.incidentmanagement.exception.IncidentNotFoundException;
import com.example.incidentmanagement.model.Incident;
import com.example.incidentmanagement.model.IncidentAnalysis;
import com.example.incidentmanagement.model.IncidentStatus;
import com.example.incidentmanagement.model.Severity;
import com.example.incidentmanagement.repository.IncidentAnalysisRepository;
import com.example.incidentmanagement.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentNotificationServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAnalysisRepository incidentAnalysisRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IncidentNotificationService incidentNotificationService;

    @Test
    void notifyIfSevere_highSeverity_sendsNotification() {
        LocalDateTime timestamp = LocalDateTime.now();

        incidentNotificationService.notifyIfSevere("Title", Severity.HIGH, "summary", "action", timestamp);

        verify(notificationService).sendIncidentNotificationAsync("Title", Severity.HIGH, "summary", "action", timestamp);
    }

    @Test
    void notifyIfSevere_criticalSeverity_sendsNotification() {
        LocalDateTime timestamp = LocalDateTime.now();

        incidentNotificationService.notifyIfSevere("Title", Severity.CRITICAL, "summary", "action", timestamp);

        verify(notificationService).sendIncidentNotificationAsync("Title", Severity.CRITICAL, "summary", "action", timestamp);
    }

    @Test
    void notifyIfSevere_lowOrMediumSeverity_doesNotSend() {
        incidentNotificationService.notifyIfSevere("Title", Severity.LOW, "summary", "action", LocalDateTime.now());
        incidentNotificationService.notifyIfSevere("Title", Severity.MEDIUM, "summary", "action", LocalDateTime.now());

        verifyNoInteractions(notificationService);
    }

    @Test
    void resendNotification_incidentWithAnalysis_sendsRealContent() {
        LocalDateTime createdAt = LocalDateTime.now();
        Incident incident = Incident.builder()
                .id(1L).title("DB down").severity(Severity.CRITICAL)
                .status(IncidentStatus.OPEN).description("d").source("s")
                .createdAt(createdAt).updatedAt(createdAt)
                .build();
        IncidentAnalysis analysis = IncidentAnalysis.builder()
                .incidentId(1L).summary("Real summary").recommendedAction("Real action")
                .build();

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentAnalysisRepository.findByIncidentId(1L)).thenReturn(Optional.of(analysis));

        incidentNotificationService.resendNotification(1L);

        verify(notificationService).sendIncidentNotificationAsync(
                "DB down", Severity.CRITICAL, "Real summary", "Real action", createdAt);
    }

    @Test
    void resendNotification_incidentWithoutAnalysis_usesFallbackText() {
        LocalDateTime createdAt = LocalDateTime.now();
        Incident incident = Incident.builder()
                .id(2L).title("API slow").severity(Severity.LOW)
                .status(IncidentStatus.OPEN).description("d").source("s")
                .createdAt(createdAt).updatedAt(createdAt)
                .build();

        when(incidentRepository.findById(2L)).thenReturn(Optional.of(incident));
        when(incidentAnalysisRepository.findByIncidentId(2L)).thenReturn(Optional.empty());

        incidentNotificationService.resendNotification(2L);

        verify(notificationService).sendIncidentNotificationAsync(
                eq("API slow"), eq(Severity.LOW),
                eq("AI analysis not available for this incident."),
                eq("No recommended action available."),
                eq(createdAt));
    }

    @Test
    void resendNotification_incidentNotFound_throwsException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentNotificationService.resendNotification(99L))
                .isInstanceOf(IncidentNotFoundException.class);

        verify(notificationService, never()).sendIncidentNotificationAsync(any(), any(), any(), any(), any());
    }
}
