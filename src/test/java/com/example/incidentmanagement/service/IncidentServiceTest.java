package com.example.incidentmanagement.service;

import com.example.incidentmanagement.ai.IncidentAnalysisService;
import com.example.incidentmanagement.dto.IncidentCreateRequest;
import com.example.incidentmanagement.dto.IncidentResponse;
import com.example.incidentmanagement.dto.IncidentStatusUpdateRequest;
import com.example.incidentmanagement.exception.IncidentNotFoundException;
import com.example.incidentmanagement.model.Incident;
import com.example.incidentmanagement.model.IncidentStatus;
import com.example.incidentmanagement.model.Severity;
import com.example.incidentmanagement.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAnalysisService incidentAnalysisService;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, incidentAnalysisService);
    }

    @Test
    void createIncident_savesAndReturnsMappedResponse() {
        IncidentCreateRequest request =
                new IncidentCreateRequest("DB down", "Primary DB unreachable", Severity.CRITICAL, "monitoring");

        Incident saved = Incident.builder()
                .id(1L)
                .title(request.title())
                .description(request.description())
                .severity(request.severity())
                .status(IncidentStatus.OPEN)
                .source(request.source())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("DB down");
        assertThat(response.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(response.status()).isEqualTo(IncidentStatus.OPEN);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("DB down");
        assertThat(captor.getValue().getSource()).isEqualTo("monitoring");

        // Creation must trigger AI analysis, but IncidentService must not know how it works.
        verify(incidentAnalysisService).analyzeAsync(1L, "DB down", "Primary DB unreachable", Severity.CRITICAL);
    }

    @Test
    void getAllIncidents_returnsMappedList() {
        Incident incident = Incident.builder()
                .id(1L).title("t").description("d").severity(Severity.LOW)
                .status(IncidentStatus.OPEN).source("s")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(incidentRepository.findAll()).thenReturn(List.of(incident));

        List<IncidentResponse> result = incidentService.getAllIncidents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getIncidentById_found_returnsMappedResponse() {
        Incident incident = Incident.builder()
                .id(5L).title("t").description("d").severity(Severity.HIGH)
                .status(IncidentStatus.ACKNOWLEDGED).source("s")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(incidentRepository.findById(5L)).thenReturn(Optional.of(incident));

        IncidentResponse response = incidentService.getIncidentById(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
    }

    @Test
    void getIncidentById_notFound_throwsIncidentNotFoundException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentById(99L))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatus_found_updatesAndReturnsMappedResponse() {
        Incident incident = Incident.builder()
                .id(7L).title("t").description("d").severity(Severity.MEDIUM)
                .status(IncidentStatus.OPEN).source("s")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(incidentRepository.findById(7L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse response =
                incidentService.updateStatus(7L, new IncidentStatusUpdateRequest(IncidentStatus.RESOLVED));

        assertThat(response.status()).isEqualTo(IncidentStatus.RESOLVED);
        verify(incidentRepository).save(incident);
    }

    @Test
    void updateStatus_notFound_throwsIncidentNotFoundException() {
        when(incidentRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                incidentService.updateStatus(42L, new IncidentStatusUpdateRequest(IncidentStatus.RESOLVED)))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessageContaining("42");
    }
}
