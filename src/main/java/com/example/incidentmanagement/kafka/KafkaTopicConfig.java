package com.example.incidentmanagement.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    public static final String INCIDENT_LOGS_TOPIC = "incident-logs-topic";

    @Bean
    public NewTopic incidentLogsTopic() {
        return TopicBuilder.name(INCIDENT_LOGS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
