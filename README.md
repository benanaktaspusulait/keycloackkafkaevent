# SmartFace Keycloak Event System

This project implements a robust event handling system for Keycloak using the Outbox Pattern. It ensures reliable event delivery from Keycloak to Kafka while maintaining data consistency.

## Architecture

The system consists of the following components:

1. **Keycloak Event Listener**
   - Captures events from Keycloak
   - Stores events in PostgreSQL
   - Uses the Outbox Pattern for reliable Kafka publishing

2. **Outbox Poller**
   - Runs as part of the Keycloak event listener
   - Polls the outbox table for pending events
   - Publishes events to Kafka
   - Handles retries and error tracking

3. **Event Service**
   - Consumes events from Kafka
   - Provides gRPC interface for event queries
   - Implements event filtering and streaming

## Database Schema

### keycloak_events
- Stores all Keycloak events
- Primary table for event data
- Includes timestamps, event types, and related metadata

### event_outbox
- Implements the Outbox Pattern
- Tracks event publishing status
- Handles retries and error tracking
- Ensures exactly-once delivery

### event_details
- Stores additional event details
- Supports flexible event data storage

## Setup and Running

1. **Prerequisites**
   - Docker and Docker Compose
   - Java 17 or later
   - Maven

2. **Environment Variables**
   ```bash
   KC_EVENTS_LISTENER_KAFKA_BOOTSTRAP_SERVERS=redpanda:29092
   KC_EVENTS_LISTENER_KAFKA_TOPIC=keycloak_events
   KC_EVENTS_LISTENER_KAFKA_CLIENT_ID=keycloak-event-listener
   KC_EVENTS_LISTENER_DB_URL=jdbc:postgresql://postgres:5432/keycloak
   KC_EVENTS_LISTENER_DB_USER=keycloak
   KC_EVENTS_LISTENER_DB_PASSWORD=password
   ```

3. **Building and Running**
   ```bash
   # Build the event listener
   cd keycloak-kafka-event-listener
   mvn clean package

   # Start all services
   docker compose up -d --build
   ```

4. **Verifying the Setup**
   - Check Keycloak logs: `docker logs smartface-keycloak-1`
   - Check event service logs: `docker logs smartface-event-service-1`
   - Monitor Kafka topics: `docker exec -it smartface-redpanda-1 rpk topic list`

## Event Flow

1. Keycloak generates an event
2. Event listener:
   - Stores event in `keycloak_events` table
   - Creates entry in `event_outbox` table
3. Outbox poller:
   - Reads pending events from outbox
   - Publishes to Kafka
   - Updates event status
4. Event service:
   - Consumes events from Kafka
   - Provides gRPC interface for queries

## Error Handling

- Failed events are retried up to 3 times
- Error details are stored in the outbox table
- System maintains consistency through transactions
- Failed events can be manually reviewed and retried

## Monitoring

- Check event status in `event_outbox` table
- Monitor Kafka consumer lag
- Review error logs in Keycloak and event service
- Track event processing metrics

## Development

1. **Building the Event Listener**
   ```bash
   cd keycloak-kafka-event-listener
   mvn clean package
   ```

2. **Building the Event Service**
   ```bash
   cd keycloak-event-service
   mvn clean package
   ```

3. **Testing**
   - Unit tests: `mvn test`
   - Integration tests: `mvn verify`
   - Manual testing: Use Keycloak admin console to trigger events

## Troubleshooting

1. **Events not appearing in Kafka**
   - Check Keycloak logs for event listener errors
   - Verify outbox table for pending events
   - Check Kafka connectivity

2. **Database Connection Issues**
   - Verify PostgreSQL is running
   - Check connection string and credentials
   - Review database logs

3. **Kafka Publishing Failures**
   - Check Redpanda status
   - Verify topic exists
   - Review outbox table for failed events

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 