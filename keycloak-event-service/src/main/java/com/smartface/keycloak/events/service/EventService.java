package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.grpc.EventRequest;

import java.util.List;

public interface EventService {
    void sendEvent(EventRequest eventRequest);
    
    List<EventOutbox> findPendingEvents();
    
    void updateEventStatus(String eventId, EventStatus status, String error);
} 