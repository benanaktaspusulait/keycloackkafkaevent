package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class OutboxPollerTest {

    @InjectMock
    EventOutboxRepository eventOutboxRepository;

    @InjectMock
    @Channel("events")
    Emitter<Record<String, String>> emitter;

    @Inject
    OutboxPoller outboxPoller;

    @BeforeEach
    void setUp() {
        reset(eventOutboxRepository, emitter);
    }

    @Test
    void testPollOutboxWithPendingEvents() {
        // Given
        EventOutbox event1 = new EventOutbox();
        event1.eventId = "event1";
        event1.details = "{\"key\":\"value1\"}";
        event1.status = EventStatus.PENDING;

        EventOutbox event2 = new EventOutbox();
        event2.eventId = "event2";
        event2.details = "{\"key\":\"value2\"}";
        event2.status = EventStatus.PENDING;

        when(eventOutboxRepository.findPendingEvents()).thenReturn(Arrays.asList(event1, event2));
        when(emitter.<Record<String, String>>send(any())).thenReturn(null);

        // When
        outboxPoller.pollOutbox();

        // Then
        verify(emitter, times(2)).<Record<String, String>>send(any());
        verify(eventOutboxRepository).updateStatus("event1", EventStatus.PUBLISHED, null);
        verify(eventOutboxRepository).updateStatus("event2", EventStatus.PUBLISHED, null);
    }

    @Test
    void testPollOutboxWithNoEvents() {
        // Given
        when(eventOutboxRepository.findPendingEvents()).thenReturn(Collections.emptyList());

        // When
        outboxPoller.pollOutbox();

        // Then
        verify(emitter, never()).<Record<String, String>>send(any());
        verify(eventOutboxRepository, never()).updateStatus(any(), any(), any());
    }

    @Test
    void testPollOutboxWithError() {
        // Given
        EventOutbox event = new EventOutbox();
        event.eventId = "event1";
        event.details = "{\"key\":\"value1\"}";
        event.status = EventStatus.PENDING;

        when(eventOutboxRepository.findPendingEvents()).thenReturn(Collections.singletonList(event));
        when(emitter.<Record<String, String>>send(any())).thenThrow(new RuntimeException("Test error"));

        // When
        outboxPoller.pollOutbox();

        // Then
        verify(emitter).<Record<String, String>>send(any());
        verify(eventOutboxRepository).updateStatus("event1", EventStatus.FAILED, "Test error");
    }
} 