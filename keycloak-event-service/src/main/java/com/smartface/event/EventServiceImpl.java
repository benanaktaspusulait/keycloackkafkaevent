package com.smartface.event;

import com.smartface.event.mapper.EventMapper;
import com.smartface.keycloak.grpc.EventFilter;
import com.smartface.keycloak.grpc.EventRequest;
import com.smartface.keycloak.grpc.EventResponse;
import com.smartface.keycloak.grpc.EventService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Instant;

@Slf4j
@GrpcService
@ApplicationScoped
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    
    @Channel("keycloak-events")
    Emitter<String> eventEmitter;

    @Override
    public Uni<EventResponse> sendEvent(EventRequest request) {
        log.debug("Processing event request: {}", request.getEventId());
        
        return Uni.createFrom().item(() -> {
            try {
                EventEntity event = eventMapper.toEntity(request);

                // Save event to database
                eventRepository.persist(event);
                log.debug("Successfully saved event with id: {}", event.getId());

                // Emit event to Kafka
                eventEmitter.send(event.toString())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to emit event to Kafka", ex);
                        } else {
                            log.debug("Successfully emitted event to Kafka");
                        }
                    });

                // Build and return response
                return EventResponse.newBuilder()
                    .setEventId(event.getId())
                    .setStatus("SUCCESS")
                    .setMessage("Event processed successfully")
                    .build();
            } catch (Exception e) {
                log.error("Error processing event request", e);
                return EventResponse.newBuilder()
                    .setEventId(request.getEventId())
                    .setStatus("ERROR")
                    .setMessage("Failed to process event: " + e.getMessage())
                    .build();
            }
        });
    }

    @Override
    public Multi<EventResponse> streamEvents(EventFilter filter) {
        log.debug("Streaming events with filter: {}", filter);
        
        return Multi.createFrom().emitter(emitter -> {
            try {
                // Query events based on filter
                eventRepository.findByRealmAndTimeRange(
                    filter.getRealmId(),
                    Instant.ofEpochMilli(filter.getFromTimestamp()),
                    Instant.ofEpochMilli(filter.getToTimestamp())
                )
                .forEach(event -> {
                    try {
                        EventResponse response = EventResponse.newBuilder()
                            .setEventId(event.getId())
                            .setStatus("SUCCESS")
                            .setMessage("Event retrieved")
                            .build();
                        emitter.emit(response);
                    } catch (Exception e) {
                        log.error("Error transforming event to response", e);
                        emitter.fail(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Error setting up event stream", e);
                emitter.fail(e);
            }
        });
    }
}