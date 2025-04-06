package com.smartface.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartface.keycloak.grpc.EventFilter;
import com.smartface.keycloak.grpc.EventRequest;
import com.smartface.keycloak.grpc.EventResponse;
import com.smartface.keycloak.grpc.EventService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.Properties;

@Slf4j
@GrpcService
@Singleton
public class EventServiceImpl implements EventService {
    private KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @PostConstruct
    void initialize() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        this.producer = new KafkaProducer<>(props);
        log.info("Kafka producer initialized with bootstrap servers: {}", bootstrapServers);
    }

    @PreDestroy
    void cleanup() {
        if (producer != null) {
            producer.close();
            log.info("Kafka producer closed");
        }
    }

    @Override
    public Uni<EventResponse> sendEvent(EventRequest request) {
        log.debug("Processing event request: {}", request.getEventId());
        
        return Uni.createFrom().item(() -> {
            try {
                // Convert event to JSON
                Map<String, Object> eventMap = Map.of(
                    "id", request.getEventId(),
                    "time", request.getTimestamp(),
                    "type", request.getEventType(),
                    "realmId", request.getRealmId(),
                    "clientId", request.getClientId(),
                    "userId", request.getUserId(),
                    "sessionId", request.getSessionId(),
                    "ipAddress", request.getIpAddress(),
                    "details", request.getDetailsMap(),
                    "error", request.getError()
                );
                String eventJson = objectMapper.writeValueAsString(eventMap);

                // Send event to Kafka
                ProducerRecord<String, String> record = new ProducerRecord<>("keycloak_events", request.getEventId(), eventJson);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send event to Kafka", exception);
                    } else {
                        log.debug("Successfully sent event to Kafka: topic={}, partition={}, offset={}", 
                            metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });

                // Build and return response
                return EventResponse.newBuilder()
                    .setEventId(request.getEventId())
                    .setStatus("SUCCESS")
                    .setMessage("Event processed successfully")
                    .build();
            } catch (Exception e) {
                log.error("Error processing event request", e);
                return EventResponse.newBuilder()
                    .setEventId(request.getEventId())
                    .setStatus("ERROR")
                    .setMessage("Failed to process event: " + e.getMessage())
                    .build();
            }
        });
    }

    @Override
    public Multi<EventResponse> streamEvents(EventFilter filter) {
        log.debug("Streaming events with filter: {}", filter);
        
        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("Error setting up event stream", e);
                emitter.fail(e);
            }
        });
    }
}