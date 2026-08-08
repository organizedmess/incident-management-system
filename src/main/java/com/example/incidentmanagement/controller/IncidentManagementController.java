package com.example.incidentmanagement.controller;

import com.example.incidentmanagement.ai.IncidentAnalysisService;
import com.example.incidentmanagement.dto.IncidentAnalysisResponse;
import com.example.incidentmanagement.dto.IncidentCreateRequest;
import com.example.incidentmanagement.dto.IncidentResponse;
import com.example.incidentmanagement.dto.IncidentStatusUpdateRequest;
import com.example.incidentmanagement.notification.IncidentNotificationService;
import com.example.incidentmanagement.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentManagementController {

    private final IncidentService incidentService;
    private final IncidentAnalysisService incidentAnalysisService;
    private final IncidentNotificationService incidentNotificationService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody IncidentCreateRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.created(URI.create("/incidents/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody IncidentStatusUpdateRequest request) {
        return ResponseEntity.ok(incidentService.updateStatus(id, request));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<IncidentAnalysisResponse> getAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(incidentAnalysisService.getAnalysis(id));
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<Void> resendNotification(@PathVariable Long id) {
        incidentNotificationService.resendNotification(id);
        return ResponseEntity.accepted().build();
    }
}
