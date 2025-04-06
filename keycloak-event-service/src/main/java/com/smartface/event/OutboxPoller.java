package com.smartface.event;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Slf4j
@ApplicationScoped
public class OutboxPoller {

    @Inject
    DataSource dataSource;

    @Inject
    @Channel("keycloak-events-out")
    Emitter<String> kafkaEmitter;

    @ConfigProperty(name = "mp.messaging.outgoing.keycloak-events-out.topic")
    String topic;

    @Scheduled(every = "5s")
    @Transactional
    void pollOutbox() {
        try (Connection conn = dataSource.getConnection()) {
            // First, get pending events
            List<OutboxEvent> events = getPendingEvents(conn);
            
            for (OutboxEvent event : events) {
                try {
                    // Send to Kafka using Emitter
                    CompletionStage<Void> sendFuture = kafkaEmitter.send(event.payload);
                    
                    sendFuture.whenComplete((success, failure) -> {
                        try (Connection updateConn = dataSource.getConnection()) {
                            if (failure != null) {
                                // Update retry count and error
                                updateFailedEvent(updateConn, event.id, failure.getMessage());
                                log.error("Failed to send event {} to Kafka: {}", event.id, failure.getMessage());
                            } else {
                                // Mark as published
                                markEventAsPublished(updateConn, event.id);
                                log.debug("Successfully published event {} to Kafka", event.id);
                            }
                        } catch (Exception e) {
                            log.error("Error updating event status: {}", e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log.error("Error processing event {}: {}", event.id, e.getMessage());
                    updateFailedEvent(conn, event.id, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in outbox polling: {}", e.getMessage());
        }
    }

    private List<OutboxEvent> getPendingEvents(Connection conn) throws Exception {
        List<OutboxEvent> events = new ArrayList<>();
        String sql = "SELECT id, event_id, payload FROM event_outbox WHERE status = 'PENDING' AND (retry_count < 3 OR retry_count IS NULL) ORDER BY created_at LIMIT 10";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OutboxEvent event = new OutboxEvent();
                    event.id = rs.getString("id");
                    event.eventId = rs.getString("event_id");
                    event.payload = rs.getString("payload");
                    events.add(event);
                }
            }
        }
        return events;
    }

    private void markEventAsPublished(Connection conn, String id) throws Exception {
        String sql = "UPDATE event_outbox SET status = 'PUBLISHED', published_at = ?, retry_count = COALESCE(retry_count, 0) WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(Instant.now()));
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }

    private void updateFailedEvent(Connection conn, String id, String error) throws Exception {
        String sql = "UPDATE event_outbox SET status = 'FAILED', last_error = ?, retry_count = COALESCE(retry_count, 0) + 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, error);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }

    private static class OutboxEvent {
        String id;
        String eventId;
        String payload;
    }
} 