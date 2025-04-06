package com.smartface.keycloak.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;

public class KafkaEventListenerProvider implements EventListenerProvider {

    private static final Logger LOGGER = Logger.getLogger(KafkaEventListenerProvider.class.getName());

    private final String bootstrapServers;
    private final String topic;
    private final String clientId;
    private final ObjectMapper objectMapper;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public KafkaEventListenerProvider(String bootstrapServers, String topic, String clientId) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.clientId = clientId;
        this.objectMapper = new ObjectMapper();
        
        // Database configuration
        this.dbUrl = System.getenv("KC_EVENTS_LISTENER_DB_URL");
        this.dbUser = System.getenv("KC_EVENTS_LISTENER_DB_USER");
        this.dbPassword = System.getenv("KC_EVENTS_LISTENER_DB_PASSWORD");
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private void storeEventDetails(Connection conn, String eventId, Map<String, String> details) throws SQLException {
        if (details == null || details.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO event_details (event_id, detail_key, detail_value) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                pstmt.setString(1, eventId);
                pstmt.setString(2, entry.getKey());
                pstmt.setString(3, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void storeInOutbox(Connection conn, String eventId, String eventJson) throws SQLException {
        String sql = "INSERT INTO event_outbox (id, event_id, topic, payload, status) " +
                    "VALUES (?, ?, ?, ?::jsonb, 'PENDING')";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, eventId);
            pstmt.setString(3, topic);
            pstmt.setString(4, eventJson);
            pstmt.executeUpdate();
        }
    }

    private void processEvent(Event event) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Store event in main table
                String sql = "INSERT INTO keycloak_events (id, time, type, realm_id, client_id, user_id, session_id, ip_address, error, details) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, event.getId());
                    long eventTime = event.getTime() <= 0 ? System.currentTimeMillis() : event.getTime();
                    pstmt.setTimestamp(2, new java.sql.Timestamp(eventTime));
                    pstmt.setString(3, event.getType().name());
                    pstmt.setString(4, event.getRealmId());
                    pstmt.setString(5, event.getClientId());
                    pstmt.setString(6, event.getUserId());
                    pstmt.setString(7, event.getSessionId());
                    pstmt.setString(8, event.getIpAddress());
                    pstmt.setString(9, event.getError());
                    
                    String detailsJson;
                    try {
                        detailsJson = objectMapper.writeValueAsString(event.getDetails());
                    } catch (JsonProcessingException e) {
                        LOGGER.warning("Failed to serialize event details: " + e.getMessage());
                        detailsJson = "{}";
                    }
                    pstmt.setString(10, detailsJson);
                    
                    pstmt.executeUpdate();
                }

                // Store details in event_details table
                storeEventDetails(conn, event.getId(), event.getDetails());

                // Store in outbox
                String eventJson = objectMapper.writeValueAsString(event);
                storeInOutbox(conn, event.getId(), eventJson);

                conn.commit();
                LOGGER.info("Successfully processed event: " + event.getId());
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Failed to process event: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            LOGGER.severe("Database error while processing event: " + e.getMessage());
            throw new RuntimeException("Failed to process event", e);
        }
    }

    private void processAdminEvent(AdminEvent adminEvent) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Store admin event in main table
                String sql = "INSERT INTO keycloak_events (id, time, type, realm_id, client_id, user_id, details) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, adminEvent.getId());
                    pstmt.setTimestamp(2, new java.sql.Timestamp(adminEvent.getTime()));
                    pstmt.setString(3, "ADMIN_" + adminEvent.getOperationType().name());
                    pstmt.setString(4, adminEvent.getRealmId());
                    pstmt.setString(5, adminEvent.getResourceType().name());
                    pstmt.setString(6, adminEvent.getAuthDetails().getUserId());
                    
                    String representationJson;
                    try {
                        representationJson = objectMapper.writeValueAsString(adminEvent.getRepresentation());
                    } catch (JsonProcessingException e) {
                        LOGGER.warning("Failed to serialize admin event representation: " + e.getMessage());
                        representationJson = "{}";
                    }
                    pstmt.setString(7, representationJson);
                    
                    pstmt.executeUpdate();
                }

                // Store admin event details
                if (adminEvent.getRepresentation() != null) {
                    Map<String, String> details = Map.of(
                        "resourceType", adminEvent.getResourceType().name(),
                        "operationType", adminEvent.getOperationType().name(),
                        "representation", adminEvent.getRepresentation()
                    );
                    storeEventDetails(conn, adminEvent.getId(), details);
                }

                // Store in outbox
                String eventJson = objectMapper.writeValueAsString(adminEvent);
                storeInOutbox(conn, adminEvent.getId(), eventJson);

                conn.commit();
                LOGGER.info("Successfully processed admin event: " + adminEvent.getId());
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Failed to process admin event: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            LOGGER.severe("Database error while processing admin event: " + e.getMessage());
            throw new RuntimeException("Failed to process admin event", e);
        }
    }

    @Override
    public void onEvent(Event event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            LOGGER.severe("Error processing event: " + e.getMessage());
            throw new RuntimeException("Failed to process event", e);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        try {
            processAdminEvent(adminEvent);
        } catch (Exception e) {
            LOGGER.severe("Error processing admin event: " + e.getMessage());
            throw new RuntimeException("Failed to process admin event", e);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
