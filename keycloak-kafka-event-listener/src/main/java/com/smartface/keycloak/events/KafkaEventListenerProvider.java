package com.smartface.keycloak.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.util.Properties;

public class KafkaEventListenerProvider implements EventListenerProvider {
    private final String bootstrapServers;
    private final String topic;
    private final String clientId;
    private final ObjectMapper objectMapper;

    public KafkaEventListenerProvider(String bootstrapServers, String topic, String clientId) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.clientId = clientId;
        this.objectMapper = new ObjectMapper();
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
        try (KafkaProducer<String, String> producer = createProducer()) {
            String eventJson = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getId(), eventJson);
            producer.send(record).get(); // Wait for the send to complete
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        try (KafkaProducer<String, String> producer = createProducer()) {
            String eventJson = objectMapper.writeValueAsString(adminEvent);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, adminEvent.getId(), eventJson);
            producer.send(record).get(); // Wait for the send to complete
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        // Nothing to close since producers are managed per event
    }
} 