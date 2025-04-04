package com.smartface.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class EventConsumer {
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, String> consumer;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @PostConstruct
    void start() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "keycloak-event-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("keycloak_events"));

        executorService.submit(this::pollMessages);
    }

    private void pollMessages() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        log.info("Received event: key={}, value={}", record.key(), record.value());
                        
                        // Parse the event JSON
                        EventEntity event = objectMapper.readValue(record.value(), EventEntity.class);
                        event.setTime(Instant.now());
                        
                        // Persist the event
                        eventRepository.persist(event);
                        
                        log.info("Event persisted successfully: {}", event.getId());
                    } catch (Exception e) {
                        log.error("Error processing event: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in consumer polling loop", e);
        } finally {
            try {
                consumer.close();
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
    }

    @PreDestroy
    void stop() {
        running.set(false);
        try {
            executorService.shutdown();
            consumer.wakeup();
        } catch (Exception e) {
            log.error("Error stopping consumer", e);
        }
    }
} 