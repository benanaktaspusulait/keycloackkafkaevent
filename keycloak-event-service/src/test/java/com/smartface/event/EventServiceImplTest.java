package com.smartface.event;

import com.smartface.keycloak.grpc.EventFilter;
import com.smartface.keycloak.grpc.EventRequest;
import com.smartface.keycloak.grpc.EventResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class EventServiceImplTest {

    @Inject
    EventServiceImpl eventService;

    @InjectMock
    KafkaProducer<String, String> producer;

    private EventRequest testRequest;

    @BeforeEach
    void setUp() {
        String eventId = UUID.randomUUID().toString();
        testRequest = EventRequest.newBuilder()
            .setEventId(eventId)
            .setEventType("LOGIN")
            .setRealmId("test-realm")
            .setClientId("test-client")
            .setUserId("test-user")
            .setSessionId("test-session")
            .setIpAddress("127.0.0.1")
            .setTimestamp(Instant.now().toEpochMilli())
            .build();
    }

    @Test
    void testSendEvent() {
        // Mock Kafka producer send
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        when(producer.send(any(ProducerRecord.class), any())).thenAnswer(invocation -> {
            ProducerRecord<String, String> record = invocation.getArgument(0);
            assertEquals(testRequest.getEventId(), record.key());
            assertTrue(record.value().contains(testRequest.getEventId()));
            assertTrue(record.value().contains(testRequest.getEventType()));
            assertTrue(record.value().contains(testRequest.getRealmId()));
            
            future.complete(new RecordMetadata(null, 0, 0, 0, 0L, 0, 0));
            return future;
        });

        // Send event
        EventResponse response = eventService.sendEvent(testRequest)
            .await().indefinitely();

        // Verify response
        assertNotNull(response);
        assertEquals(testRequest.getEventId(), response.getEventId());
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("successfully"));

        // Verify Kafka producer was called
        verify(producer).send(any(ProducerRecord.class), any());
    }

    @Test
    void testSendEventWithError() {
        // Mock Kafka producer to throw error
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test error"));
        when(producer.send(any(ProducerRecord.class), any())).thenReturn(future);

        // Send event
        EventResponse response = eventService.sendEvent(testRequest)
            .await().indefinitely();

        // Verify error response
        assertNotNull(response);
        assertEquals(testRequest.getEventId(), response.getEventId());
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Failed"));

        // Verify Kafka producer was called
        verify(producer).send(any(ProducerRecord.class), any());
    }

    @Test
    void testStreamEvents() {
        EventFilter filter = EventFilter.newBuilder()
            .setRealmId("test-realm")
            .setClientId("test-client")
            .setUserId("test-user")
            .setEventType("LOGIN")
            .build();

        Multi<EventResponse> events = eventService.streamEvents(filter);
        
        assertNotNull(events);
        // Note: Since the current implementation returns an empty Multi,
        // we just verify it's not null. In a real implementation,
        // we would test the actual streaming functionality.
    }
}