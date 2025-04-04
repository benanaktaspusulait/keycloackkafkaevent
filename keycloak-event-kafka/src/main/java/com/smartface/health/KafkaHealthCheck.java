package com.smartface.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.util.Properties;

@Readiness
@ApplicationScoped
public class KafkaHealthCheck implements HealthCheck {

    private final String bootstrapServers;

    public KafkaHealthCheck() {
        this.bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
    }

    @Override
    public HealthCheckResponse call() {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            
            try (AdminClient adminClient = AdminClient.create(props)) {
                adminClient.listTopics().names().get();
                return HealthCheckResponse.up("Kafka connection");
            }
        } catch (Exception e) {
            return HealthCheckResponse.down("Kafka connection");
        }
    }
} 