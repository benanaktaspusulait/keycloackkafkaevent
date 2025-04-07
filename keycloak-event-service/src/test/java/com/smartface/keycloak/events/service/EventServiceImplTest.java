package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class EventServiceImplTest {

    @InjectMock
    EventOutboxRepository eventOutboxRepository;

    @Inject
    EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        reset(eventOutboxRepository);
    }

    @Test
    void testProcessEvent() {
        // Given
        String eventId = "test-event-id";
        String eventType = "USER_EVENT";
        String details = "{\"key\":\"value\"}";

        // When
        eventService.processEvent(eventId, eventType, null, null, null, null, null, null, details);

        // Then
        ArgumentCaptor<EventOutbox> eventCaptor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxRepository).persist(eventCaptor.capture());

        EventOutbox capturedEvent = eventCaptor.getValue();
        assertEquals(eventId, capturedEvent.eventId);
        assertEquals(eventType, capturedEvent.eventType);
        assertEquals(details, capturedEvent.details);
        assertEquals(EventStatus.PENDING, capturedEvent.status);
    }

    @Test
    void testFindPendingEvents() {
        // Given
        EventOutbox event1 = new EventOutbox();
        event1.eventId = "event1";
        event1.status = EventStatus.PENDING;

        EventOutbox event2 = new EventOutbox();
        event2.eventId = "event2";
        event2.status = EventStatus.PENDING;

        List<EventOutbox> pendingEvents = Arrays.asList(event1, event2);
        when(eventOutboxRepository.findPendingEvents()).thenReturn(pendingEvents);

        // When
        List<EventOutbox> result = eventService.findPendingEvents();

        // Then
        assertEquals(2, result.size());
        assertEquals("event1", result.get(0).eventId);
        assertEquals("event2", result.get(1).eventId);
        verify(eventOutboxRepository).findPendingEvents();
    }

    @Test
    void testUpdateEventStatus() {
        // Given
        String eventId = "test-event-id";
        EventStatus newStatus = EventStatus.PUBLISHED;
        String error = null;

        // When
        eventService.updateEventStatus(eventId, newStatus, error);

        // Then
        verify(eventOutboxRepository).updateStatus(eventId, newStatus, error);
    }
}