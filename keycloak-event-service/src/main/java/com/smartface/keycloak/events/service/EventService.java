package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import java.util.List;

public interface EventService {
    void processEvent(String eventId, String eventType, String realmId, String clientId, 
                     String userId, String sessionId, String ipAddress, String error, String details);
    
    List<EventOutbox> findPendingEvents();
    
    void updateEventStatus(String eventId, EventStatus status, String error);
} 