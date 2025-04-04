package com.smartface.event.mapper;

import com.smartface.event.EventEntity;
import com.smartface.keycloak.grpc.EventRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;

@Mapper(componentModel = "cdi")
public interface EventMapper {

    @Mapping(target = "id", source = "eventId")
    @Mapping(target = "time", source = "timestamp", qualifiedByName = "toInstant")
    @Mapping(target = "type", source = "eventType")
    @Mapping(target = "realmId", source = "realmId")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "details", source = "details")
    EventEntity toEntity(EventRequest request);

    @Named("toInstant")
    default Instant toInstant(long timestamp) {
        return Instant.ofEpochMilli(timestamp);
    }
} 