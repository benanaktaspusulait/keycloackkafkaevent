package com.smartface.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.util.Properties;

@Slf4j
public class KafkaEventListenerProvider implements EventListenerProvider {
    private final KafkaProducer<String, String> producer;
    private final String topic;

    public KafkaEventListenerProvider(String bootstrapServers, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void onEvent(Event event) {
        try {
            String eventJson = EventSerializer.serialize(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getUserId(), eventJson);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send user event to Kafka", exception);
                } else {
                    log.debug("Sent user event to Kafka: {} - partition: {}, offset: {}",
                            event.getType(), metadata.partition(), metadata.offset());
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error while processing user event", e);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        try {
            String eventJson = EventSerializer.serialize(adminEvent);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, adminEvent.getResourceType().name(), eventJson);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send admin event to Kafka", exception);
                } else {
                    log.debug("Sent admin event to Kafka: {} {} - partition: {}, offset: {}",
                            adminEvent.getOperationType(), adminEvent.getResourceType(),
                            metadata.partition(), metadata.offset());
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error while processing admin event", e);
        }
    }

    @Override
    public void close() {
        try {
            producer.flush();
            producer.close();
            log.info("Kafka producer closed");
        } catch (Exception e) {
            log.error("Error closing Kafka producer", e);
        }
    }
}
