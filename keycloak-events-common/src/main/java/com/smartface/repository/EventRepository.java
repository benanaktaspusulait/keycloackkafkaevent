package com.smartface.repository;


import com.smartface.entity.EventEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class EventRepository implements PanacheRepository<EventEntity> {
} 