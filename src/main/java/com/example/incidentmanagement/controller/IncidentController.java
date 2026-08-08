package com.example.incidentmanagement.controller;

import com.example.incidentmanagement.ai.ResolutionAgentService;
import com.example.incidentmanagement.model.IncidentLog;
import com.example.incidentmanagement.model.IncidentRequest;
import com.example.incidentmanagement.service.IncidentLogProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentLogProcessor incidentLogProcessor;
    private final ResolutionAgentService resolutionAgentService;

    @PostMapping
    public ResponseEntity<Void> reportIncident(@RequestBody IncidentRequest request) {
        IncidentLog incidentLog = IncidentLog.builder()
                .serviceName(request.serviceName())
                .errorMessage(request.errorMessage())
                .stackTrace(request.stackTrace())
                .status("OPEN")
                .timestamp(LocalDateTime.now())
                .build();

        incidentLogProcessor.process(incidentLog);

        return ResponseEntity.accepted().build();
    }

    // Manual trigger for the RAG + function-calling agent, for testing the diagnosis
    // pipeline independently of the async Kafka ingestion flow.
    @PostMapping("/{id}/diagnose")
    public ResponseEntity<String> diagnose(@PathVariable Long id) {
        String diagnosis = resolutionAgentService.diagnoseAndResolve(id);
        return ResponseEntity.ok(diagnosis);
    }
}
