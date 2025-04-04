package com.smartface.keycloak.grpc;

import com.smartface.keycloak.entity.KeycloakEvent;
import com.smartface.keycloak.kafka.EventProducer;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventProducer eventProducer;

    @Override
    public Uni<EventResponse> sendEvent(EventRequest request) {
        try {
            log.debug("Processing event request: {}", request.getEventId());
            KeycloakEvent event = new KeycloakEvent();
            event.setEventId(request.getEventId());
            event.setEventType(request.getEventType());
            event.setRealmId(request.getRealmId());
            event.setClientId(request.getClientId());
            event.setUserId(request.getUserId());
            event.setSessionId(request.getSessionId());
            event.setIpAddress(request.getIpAddress());
            event.setEventTime(Instant.ofEpochMilli(request.getTimestamp()));
            event.setDetails(new HashMap<>(request.getDetailsMap()));

            event.persist();
            eventProducer.send(event);

            log.debug("Successfully processed event: {}", event.getEventId());
            return Uni.createFrom().item(() ->
                EventResponse.newBuilder()
                    .setEventId(event.getEventId())
                    .setStatus("SUCCESS")
                    .setMessage("Event processed successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to process event: {}", request.getEventId(), e);
            return Uni.createFrom().item(() ->
                EventResponse.newBuilder()
                    .setEventId(request.getEventId())
                    .setStatus("ERROR")
                    .setMessage("Failed to process event: " + e.getMessage())
                    .build()
            );
        }
    }

    @Override
    public Multi<EventResponse> streamEvents(EventFilter filter) {
        log.debug("Streaming events for filter: {}", filter);
        return Multi.createFrom().emitter(emitter -> {
            KeycloakEvent.stream("realm_id = ?1 and event_time between ?2 and ?3",
                    filter.getRealmId(),
                    Instant.ofEpochMilli(filter.getFromTimestamp()),
                    Instant.ofEpochMilli(filter.getToTimestamp()))
                .forEach(event -> {
                    EventResponse response = EventResponse.newBuilder()
                        .setEventId(((KeycloakEvent) event).getEventId())
                        .setStatus("SUCCESS")
                        .setMessage("Event retrieved")
                        .build();
                    emitter.emit(response);
                });
            emitter.complete();
        });
    }
} 