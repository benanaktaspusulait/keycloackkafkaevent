package com.smartface.keycloak.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

public class KafkaEventListenerProvider implements EventListenerProvider {
    private final String bootstrapServers;
    private final String topic;
    private final String clientId;
    private final ObjectMapper objectMapper;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final KafkaProducer<String, String> producer;

    public KafkaEventListenerProvider(String bootstrapServers, String topic, String clientId) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.clientId = clientId;
        this.objectMapper = new ObjectMapper();
        
        // Database configuration
        this.dbUrl = System.getenv("KC_EVENTS_LISTENER_DB_URL");
        this.dbUser = System.getenv("KC_EVENTS_LISTENER_DB_USER");
        this.dbPassword = System.getenv("KC_EVENTS_LISTENER_DB_PASSWORD");
        
        // Create the Kafka producer
        this.producer = createProducer();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private void storeEventInDatabase(Event event) throws SQLException {
        String sql = "INSERT INTO keycloak_events (id, time, type, realm_id, client_id, user_id, session_id, ip_address, error, details) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getId());
            pstmt.setTimestamp(2, new java.sql.Timestamp(event.getTime()));
            pstmt.setString(3, event.getType().name());
            pstmt.setString(4, event.getRealmId());
            pstmt.setString(5, event.getClientId());
            pstmt.setString(6, event.getUserId());
            pstmt.setString(7, event.getSessionId());
            pstmt.setString(8, event.getIpAddress());
            pstmt.setString(9, event.getError());
            
            // Convert details to JSON string
            String detailsJson = "{}";
            if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                try {
                    detailsJson = objectMapper.writeValueAsString(event.getDetails());
                } catch (Exception e) {
                    System.err.println("Failed to serialize event details: " + e.getMessage());
                }
            }
            pstmt.setString(10, detailsJson);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to store event in database: " + e.getMessage());
            throw e;
        }
    }

    private void storeAdminEventInDatabase(AdminEvent event) throws SQLException {
        String sql = "INSERT INTO keycloak_events (id, time, type, realm_id, client_id, user_id, details) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getId());
            pstmt.setTimestamp(2, new java.sql.Timestamp(event.getTime()));
            pstmt.setString(3, "ADMIN_" + event.getOperationType().name());
            pstmt.setString(4, event.getRealmId());
            pstmt.setString(5, event.getResourceType().name());
            pstmt.setString(6, event.getAuthDetails().getUserId());
            
            Map<String, String> details = new HashMap<>();
            details.put("resourcePath", event.getResourcePath());
            details.put("representation", event.getRepresentation());
            String detailsJson = "{}";
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (Exception e) {
                System.err.println("Failed to serialize admin event details: " + e.getMessage());
            }
            pstmt.setString(7, detailsJson);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to store admin event in database: " + e.getMessage());
            throw e;
        }
    }

    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new KafkaProducer<>(props);
    }

    @Override
    public void onEvent(Event event) {
        try {
            // First store in database
            storeEventInDatabase(event);
            
            // Then publish to Kafka
            String eventJson = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getId(), eventJson);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Failed to send event to Kafka: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error processing event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        try {
            // First store in database
            storeAdminEventInDatabase(adminEvent);
            
            // Then publish to Kafka
            String eventJson = objectMapper.writeValueAsString(adminEvent);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, adminEvent.getId(), eventJson);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Failed to send admin event to Kafka: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error processing admin event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
            } catch (Exception e) {
                System.err.println("Error closing Kafka producer: " + e.getMessage());
            }
        }
    }
} 