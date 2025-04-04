package com.smartface.keycloak.grpc;

import com.smartface.keycloak.entity.KeycloakEvent;
import com.smartface.keycloak.kafka.EventProducer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(EventServiceImplTest.TestProfile.class)
class EventServiceImplTest {

    @Inject
    EventServiceImpl eventService;

    @InjectMock
    EventProducer eventProducer;

    public static class TestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "test";
        }
    }

    private EventRequest createSampleRequest() {
        Map<String, String> details = new HashMap<>();
        details.put("key1", "value1");
        details.put("key2", "value2");

        return EventRequest.newBuilder()
                .setEventId("test-event-1")
                .setEventType("LOGIN")
                .setRealmId("test-realm")
                .setClientId("test-client")
                .setUserId("test-user")
                .setSessionId("test-session")
                .setIpAddress("127.0.0.1")
                .setTimestamp(Instant.now().toEpochMilli())
                .putAllDetails(details)
                .build();
    }

    @Test
    void testSendEventSuccess() {
        EventRequest request = createSampleRequest();
        doNothing().when(eventProducer).send(any(KeycloakEvent.class));

        UniAssertSubscriber<EventResponse> subscriber = eventService.sendEvent(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        EventResponse response = subscriber.getItem();
        
        assertNotNull(response);
        assertEquals(request.getEventId(), response.getEventId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Event processed successfully", response.getMessage());
        
        verify(eventProducer, times(1)).send(any(KeycloakEvent.class));
    }

    @Test
    void testSendEventFailure() {
        EventRequest request = createSampleRequest();
        doThrow(new RuntimeException("Test error")).when(eventProducer).send(any(KeycloakEvent.class));

        UniAssertSubscriber<EventResponse> subscriber = eventService.sendEvent(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        EventResponse response = subscriber.getItem();
        
        assertNotNull(response);
        assertEquals(request.getEventId(), response.getEventId());
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Test error"));
    }

    @Test
    void testStreamEvents() {
        EventFilter filter = EventFilter.newBuilder()
                .setRealmId("test-realm")
                .setFromTimestamp(Instant.now().minusSeconds(3600).toEpochMilli())
                .setToTimestamp(Instant.now().toEpochMilli())
                .build();

        eventService.streamEvents(filter)
                .subscribe().with(
                    item -> {
                        assertNotNull(item);
                        assertNotNull(item.getEventId());
                        assertNotNull(item.getStatus());
                        assertNotNull(item.getMessage());
                    },
                    failure -> fail("Stream should not fail"),
                    () -> {} // onComplete
                );
    }
} 