package com.smartface.event;

import com.smartface.keycloak.grpc.EventFilter;
import com.smartface.keycloak.grpc.EventRequest;
import com.smartface.keycloak.grpc.EventResponse;
import com.smartface.keycloak.grpc.EventService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Properties;

@Slf4j
@GrpcService
@Singleton
public class EventServiceImpl implements EventService {
    @Inject
    EventRepository eventRepository;

    private KafkaProducer<String, String> producer;

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
                EventEntity event = toEntity(request);

                // Save event to database
                eventRepository.persist(event);
                log.debug("Successfully saved event with id: {}", event.getId());

                // Send event to Kafka
                ProducerRecord<String, String> record = new ProducerRecord<>("keycloak_events", event.getId(), event.toString());
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
                    .setEventId(event.getId())
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
                // Query events based on filter
                eventRepository.findByRealmAndTimeRange(
                    filter.getRealmId(),
                    Instant.ofEpochMilli(filter.getFromTimestamp()),
                    Instant.ofEpochMilli(filter.getToTimestamp())
                )
                .forEach(event -> {
                    try {
                        EventResponse response = EventResponse.newBuilder()
                            .setEventId(event.getId())
                            .setStatus("SUCCESS")
                            .setMessage("Event retrieved")
                            .build();
                        emitter.emit(response);
                    } catch (Exception e) {
                        log.error("Error transforming event to response", e);
                        emitter.fail(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Error setting up event stream", e);
                emitter.fail(e);
            }
        });
    }

    public EventEntity toEntity(EventRequest request) {
        if (request == null) {
            return null;
        }

        EventEntity eventEntity = new EventEntity();

        eventEntity.setId(request.getEventId());
        eventEntity.setTime(Instant.ofEpochMilli(request.getTimestamp()));
        eventEntity.setType(request.getEventType());
        eventEntity.setRealmId(request.getRealmId());
        eventEntity.setClientId(request.getClientId());
        eventEntity.setUserId(request.getUserId());
        eventEntity.setSessionId(request.getSessionId());
        eventEntity.setIpAddress(request.getIpAddress());
        eventEntity.setDetails(request.getDetailsMap());
        eventEntity.setError(request.getError());

        return eventEntity;
    }
}