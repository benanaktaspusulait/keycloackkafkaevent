package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.grpc.EventRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@QuarkusTest
class EventServiceImplTest {

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


        EventRequest eventRequest = EventRequest.newBuilder()
                .setEventId(eventId)
                .setEventType(eventType)
                .setDetails(details)
                .build();
        // When
        eventService.processEvent(eventRequest);

        // Then
        ArgumentCaptor<EventOutbox> eventCaptor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxRepository).persist(eventCaptor.capture());

        EventOutbox capturedEvent = eventCaptor.getValue();
        assertEquals(eventId, capturedEvent.getEventId());
        assertEquals(eventType, capturedEvent.getEventType());
        assertEquals(details, capturedEvent.getDetails());
        assertEquals(EventStatus.PENDING, capturedEvent.getStatus());
    }

    @Test
    void testFindPendingEvents() {
        // Given
        EventOutbox event1 = new EventOutbox();
        event1.setEventId ("event1");
        event1.setStatus (EventStatus.PENDING);

        EventOutbox event2 = new EventOutbox();
        event2.setEventId ("event2");
        event2.setStatus (EventStatus.PENDING);

        List<EventOutbox> pendingEvents = Arrays.asList(event1, event2);
        when(eventOutboxRepository.findPendingEvents()).thenReturn(pendingEvents);

        // When
        List<EventOutbox> result = eventService.findPendingEvents();

        // Then
        assertEquals(2, result.size());
        assertEquals("event1", result.get(0).getEventId());
        assertEquals("event2", result.get(1).getEventId());
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