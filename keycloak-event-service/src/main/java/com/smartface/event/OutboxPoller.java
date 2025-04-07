package com.smartface.event;

import com.smartface.event.entity.EventStatus;
import com.smartface.event.entity.OutboxEvent;
import com.smartface.event.repository.OutboxEventRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Slf4j
@ApplicationScoped
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final Emitter<String> kafkaEmitter;
    private final String topic;

    @Inject
    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                       @Channel("keycloak-events-out") Emitter<String> kafkaEmitter,
                       @ConfigProperty(name = "mp.messaging.outgoing.keycloak-events-out.topic") String topic) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEmitter = kafkaEmitter;
        this.topic = topic;
    }

    @Scheduled(every = "5s")
    @Transactional
    void pollOutbox() {
        try {
            // Get pending events
            List<OutboxEvent> events = outboxEventRepository.findPendingEvents();
            
            for (OutboxEvent event : events) {
                try {
                    // Send to Kafka using Emitter
                    CompletionStage<Void> sendFuture = kafkaEmitter.send(event.getPayload());
                    
                    sendFuture.whenComplete((success, failure) -> {
                        if (failure != null) {
                            // Update retry count and error
                            outboxEventRepository.updateStatus(event.id, EventStatus.FAILED, failure.getMessage());
                            log.error("Failed to send event {} to Kafka: {}", event.id, failure.getMessage());
                        } else {
                            // Mark as published
                            outboxEventRepository.updateStatus(event.id, EventStatus.PUBLISHED, null);
                            log.debug("Successfully published event {} to Kafka", event.id);
                        }
                    });
                } catch (Exception e) {
                    log.error("Error processing event {}: {}", event.id, e.getMessage());
                    outboxEventRepository.updateStatus(event.id, EventStatus.FAILED, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in outbox polling: {}", e.getMessage());
        }
    }
} 