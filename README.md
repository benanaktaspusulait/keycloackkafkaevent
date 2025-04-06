# Keycloak Event Service

This project implements a system to capture and process Keycloak events using a database-first approach with Kafka as a secondary channel. The system ensures event persistence even when Kafka is unavailable.

## Project Structure

```
.
├── keycloak-kafka-event-listener/    # Keycloak event listener implementation
├── keycloak-event-service/           # Event service that consumes and processes events
│   ├── k8s/                         # Kubernetes deployment configurations
│   ├── src/                         # Source code
│   └── pom.xml                      # Maven configuration
├── docker-compose.yml                # Docker configuration for all services
├── create_tables.sql                 # Database schema
└── testcase-1.pdf                    # Project documentation
```

## Components

### 1. Keycloak Kafka Event Listener
- Implements a Keycloak event listener that:
  - First stores events in PostgreSQL database
  - Then publishes events to a Kafka topic
  - Ensures event persistence even if Kafka is unavailable
- Located in `keycloak-kafka-event-listener/`

### 2. Event Service
- Consumes events from Kafka using SmallRye Reactive Messaging
- Processes and stores events in PostgreSQL
- Implements gRPC endpoints for event retrieval
- Located in `keycloak-event-service/`

### 3. Infrastructure
- Uses Docker Compose for container orchestration
- Includes services:
  - Redpanda (Kafka-compatible message broker)
  - PostgreSQL (Database)
  - Keycloak (Identity and Access Management)
  - Event Service (Event processing)

## Event Processing Flow

1. **Event Generation**:
   - Keycloak generates events (login, logout, etc.)
   - Event listener captures these events

2. **Database Storage**:
   - Events are immediately stored in PostgreSQL
   - Ensures data persistence regardless of Kafka status

3. **Kafka Publishing**:
   - After successful database storage, events are published to Kafka
   - Event service consumes from Kafka for additional processing

4. **Event Service Processing**:
   - Consumes events from Kafka
   - Provides gRPC endpoints for event retrieval
   - Implements additional business logic if needed

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 17 or later
- Maven

### Development Environment Setup

1. Build the event listener:
```bash
cd keycloak-kafka-event-listener
mvn clean package
```

2. Start all services:
```bash
docker-compose up --build
```

3. Access services:
- Keycloak: http://localhost:8080
- Event Service: http://localhost:8082
- PostgreSQL: localhost:5432
- Redpanda (Kafka): localhost:9092

### Testing the System

1. **Verify Database Setup**:
```bash
docker exec -it smartface-postgres-1 psql -U postgres -d keycloak_events -c "\dt"
```

2. **Check Event Listener Logs**:
```bash
docker logs -f smartface-keycloak-1
```

3. **Monitor Event Service**:
```bash
docker logs -f smartface-event-service-1
```

4. **Test Event Generation**:
- Log in to Keycloak admin console (http://localhost:8080)
- Create a test user
- Perform login/logout actions
- Verify events in database and Kafka

### Production Deployment (Kubernetes)
The project includes Kubernetes configurations in `keycloak-event-service/k8s/`:

1. Create namespace:
```bash
kubectl apply -f k8s/namespace.yaml
```

2. Create secrets:
```bash
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/secret.yaml
```

3. Deploy the service:
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

## Troubleshooting

### Common Issues

1. **Database Connection Issues**:
```bash
docker exec -it smartface-postgres-1 psql -U postgres -d keycloak_events -c "SELECT COUNT(*) FROM keycloak_events;"
```

2. **Kafka Connection Issues**:
```bash
docker exec -it smartface-redpanda-1 rpk topic list
```

3. **Event Listener Issues**:
```bash
docker logs -f smartface-keycloak-1 | grep "KafkaEventListenerProvider"
```

4. **Event Service Issues**:
```bash
docker logs -f smartface-event-service-1
```

### Health Checks

1. **Keycloak Health**:
```bash
curl http://localhost:8080/health/ready
```

2. **Event Service Health**:
```bash
curl http://localhost:8082/q/health/ready
```

3. **Database Health**:
```bash
docker exec -it smartface-postgres-1 pg_isready -U postgres
```

## Development

- The project uses Java with Quarkus framework
- Kafka integration is handled by SmallRye Reactive Messaging
- Database operations use JPA/Hibernate
- Event processing is implemented using reactive streams 