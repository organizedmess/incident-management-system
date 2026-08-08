package com.example.incidentmanagement.service;

import com.example.incidentmanagement.model.IncidentLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.example.incidentmanagement.kafka.KafkaTopicConfig.INCIDENT_LOGS_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaLogProducer {

    private final KafkaTemplate<String, IncidentLog> incidentLogKafkaTemplate;

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
