package com.example.incidentmanagement.service;

import com.example.incidentmanagement.ai.IncidentAnalysisService;
import com.example.incidentmanagement.dto.IncidentCreateRequest;
import com.example.incidentmanagement.dto.IncidentResponse;
import com.example.incidentmanagement.dto.IncidentStatusUpdateRequest;
import com.example.incidentmanagement.exception.IncidentNotFoundException;
import com.example.incidentmanagement.model.Incident;
import com.example.incidentmanagement.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentAnalysisService incidentAnalysisService;

    public IncidentResponse createIncident(IncidentCreateRequest request) {
        Incident incident = Incident.builder()
                .title(request.title())
                .description(request.description())
                .severity(request.severity())
                .source(request.source())
                .build();

        Incident saved = incidentRepository.save(incident);

        // Fire-and-forget: AI analysis runs asynchronously and can never affect this
        // response or fail this request (see IncidentAnalysisService for the guarantee).
        incidentAnalysisService.analyzeAsync(
                saved.getId(), saved.getTitle(), saved.getDescription(), saved.getSeverity());

        return toResponse(saved);
    }

    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public IncidentResponse getIncidentById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    public IncidentResponse updateStatus(Long id, IncidentStatusUpdateRequest request) {
        Incident incident = findByIdOrThrow(id);
        incident.setStatus(request.status());
        return toResponse(incidentRepository.save(incident));
    }

    private Incident findByIdOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + id));
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getSource(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
