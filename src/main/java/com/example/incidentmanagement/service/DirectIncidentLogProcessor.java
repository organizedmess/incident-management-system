package com.example.incidentmanagement.service;

import com.example.incidentmanagement.model.IncidentLog;
import com.example.incidentmanagement.repository.IncidentLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "false")
public class DirectIncidentLogProcessor implements IncidentLogProcessor {

    private final IncidentLogRepository incidentLogRepository;
    private final VectorStore vectorStore;

    @Override
    public void process(IncidentLog incidentLog) {
        try {
            IncidentLog saved = incidentLogRepository.save(incidentLog);
            indexInVectorStore(saved);
            log.info("Processed incident log id={} for service={}", saved.getId(), saved.getServiceName());
        } catch (Exception ex) {
            log.error("Failed to process incident log for service {}: {}",
                    incidentLog.getServiceName(), ex.getMessage(), ex);
        }
    }

    private void indexInVectorStore(IncidentLog incidentLog) {
        String content = """
                Service: %s
                Error: %s
                Stack Trace:
                %s""".formatted(
                incidentLog.getServiceName(),
                incidentLog.getErrorMessage(),
                incidentLog.getStackTrace());

        Document document = new Document(content, Map.of(
                "incidentId", incidentLog.getId(),
                "serviceName", incidentLog.getServiceName()
        ));

        vectorStore.add(List.of(document));
    }
}
