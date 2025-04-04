package com.smartface.event;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@ApplicationScoped
public class EventRepository implements PanacheRepository<EventEntity> {
    
    public List<EventEntity> findByRealmAndTimeRange(String realmId, Instant startTime, Instant endTime) {
        log.debug("Finding events for realm: {} between {} and {}", realmId, startTime, endTime);
        return find("realmId = ?1 and time between ?2 and ?3", realmId, startTime, endTime).list();
    }

} 