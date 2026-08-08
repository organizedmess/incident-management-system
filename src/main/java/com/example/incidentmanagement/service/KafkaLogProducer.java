package com.example.incidentmanagement.service;

import com.example.incidentmanagement.model.IncidentLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.example.incidentmanagement.kafka.KafkaTopicConfig.INCIDENT_LOGS_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaLogProducer implements IncidentLogProcessor {

    private final KafkaTemplate<String, IncidentLog> incidentLogKafkaTemplate;

    @Override
    public void process(IncidentLog incidentLog) {
        sendIncidentLog(incidentLog);
    }

    public void sendIncidentLog(IncidentLog incidentLog) {
        incidentLogKafkaTemplate.send(INCIDENT_LOGS_TOPIC, incidentLog.getServiceName(), incidentLog)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish incident log for service {}", incidentLog.getServiceName(), ex);
                    } else {
                        log.info("Published incident log for service {} to partition {} offset {}",
                                incidentLog.getServiceName(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
