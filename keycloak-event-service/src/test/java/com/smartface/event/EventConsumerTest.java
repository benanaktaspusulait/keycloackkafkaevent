package com.smartface.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

@QuarkusTest
public class EventConsumerTest {

    @InjectMock
    EventRepository eventRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    EventConsumer eventConsumer;

    @BeforeEach
    void setUp() {
        eventConsumer = new EventConsumer(eventRepository, objectMapper);
    }

    @Test
    void testConsumeEvent() throws Exception {
        String eventJson = """
            {
                "id": "%s",
                "eventType": "LOGIN",
                "realmId": "test-realm",
                "clientId": "test-client",
                "userId": "test-user",
                "sessionId": "test-session",
                "ipAddress": "127.0.0.1",
                "timestamp": "%s"
            }
            """.formatted(UUID.randomUUID(), Instant.now());

        eventConsumer.consume(eventJson);

        Mockito.verify(eventRepository).persist(Mockito.any(EventEntity.class));
    }

    @Test
    void testConsumeEventWithMissingFields() throws Exception {
        String eventJson = """
            {
                "id": "%s",
                "eventType": "LOGIN",
                "realmId": "test-realm"
            }
            """.formatted(UUID.randomUUID());

        eventConsumer.consume(eventJson);

        Mockito.verify(eventRepository).persist(Mockito.any(EventEntity.class));
    }

    @Test
    void testConsumeEventWithInvalidJson() {
        String eventJson = "invalid json";

        eventConsumer.consume(eventJson);

        Mockito.verify(eventRepository, Mockito.never()).persist(Mockito.any(EventEntity.class));
    }
} 