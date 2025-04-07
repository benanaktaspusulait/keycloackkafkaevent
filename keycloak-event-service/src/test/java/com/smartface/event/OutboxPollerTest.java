package com.smartface.event;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@QuarkusTest
public class OutboxPollerTest {

    @InjectMock
    DataSource dataSource;

    @Inject
    OutboxPoller outboxPoller;

    @Inject
    @Channel("keycloak-events-out")
    Emitter<String> emitter;

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws Exception {
        // Setup mock JDBC objects
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @Test
    void testPollOutboxWithEvents() throws Exception {
        // Setup mock result set with two events
        when(mockResultSet.next())
            .thenReturn(true)  // First event
            .thenReturn(true)  // Second event
            .thenReturn(false); // No more events

        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();
        String payload1 = "{\"id\":\"" + eventId1 + "\",\"type\":\"LOGIN\"}";
        String payload2 = "{\"id\":\"" + eventId2 + "\",\"type\":\"LOGOUT\"}";

        when(mockResultSet.getString("id"))
            .thenReturn(eventId1)
            .thenReturn(eventId2);
        when(mockResultSet.getString("event_id"))
            .thenReturn(eventId1)
            .thenReturn(eventId2);
        when(mockResultSet.getString("payload"))
            .thenReturn(payload1)
            .thenReturn(payload2);

        // Execute the poller
        outboxPoller.pollOutbox();

        // Verify that the events were marked as published
        verify(mockConnection, times(2)).prepareStatement(contains("UPDATE event_outbox SET status = 'PUBLISHED'"));
    }

    @Test
    void testPollOutboxWithNoEvents() throws Exception {
        // Setup mock result set with no events
        when(mockResultSet.next()).thenReturn(false);

        // Execute the poller
        outboxPoller.pollOutbox();

        // Verify that no updates were made
        verify(mockConnection, never()).prepareStatement(contains("UPDATE event_outbox SET status"));
    }

    @Test
    void testPollOutboxWithError() throws Exception {
        // Setup mock result set with one event that will fail
        when(mockResultSet.next())
            .thenReturn(true)
            .thenReturn(false);

        String eventId = UUID.randomUUID().toString();
        String payload = "{\"id\":\"" + eventId + "\",\"type\":\"LOGIN\"}";

        when(mockResultSet.getString("id")).thenReturn(eventId);
        when(mockResultSet.getString("event_id")).thenReturn(eventId);
        when(mockResultSet.getString("payload")).thenReturn(payload);

        // Make the emitter throw an error
        doThrow(new RuntimeException("Test error")).when(emitter).send(Message.of(payload));

        // Execute the poller
        outboxPoller.pollOutbox();

        // Verify that the event was marked as failed
        verify(mockConnection).prepareStatement(contains("UPDATE event_outbox SET status = 'FAILED'"));
    }
} 