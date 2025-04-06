# Keycloak Event Service

This project implements a system to capture and process Keycloak events using Kafka and store them in a PostgreSQL database. The system consists of two main components:

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
- Implements a Keycloak event listener that captures authentication and user events
- Publishes events to a Kafka topic
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

## Configuration

The system is configured through:
- `application.properties` in the event service
- `docker-compose.yml` for container configuration
- `create_tables.sql` for database schema

## Getting Started

### Development Environment (Docker Compose)
1. Build and start the services:
```bash
docker-compose up --build
```

2. The system will automatically:
- Initialize the PostgreSQL database with required tables
- Start Keycloak with the event listener
- Start the event service to consume and process events

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

The Kubernetes deployment includes:
- Horizontal scaling (2 replicas)
- Resource limits and requests
- Health checks (liveness and readiness probes)
- TLS configuration for gRPC
- Secure secret management

## Database Schema

The `create_tables.sql` file defines the database schema:
- `keycloak_events` table for storing events
- Indexes for optimized query performance
- JSONB field for flexible event details storage

## Troubleshooting

If you encounter issues:
1. Check service logs:
```bash
docker-compose logs -f [service-name]
```

2. Verify database tables:
```bash
docker exec -it smartface-postgres-1 psql -U postgres -d keycloak_events -c "\dt"
```

3. Common issues:
- Ensure all services are running and healthy
- Check database connection settings
- Verify Kafka topic configuration
- Monitor event service logs for processing errors

## Development

- The project uses Java with Quarkus framework
- Kafka integration is handled by SmallRye Reactive Messaging
- Database operations use JPA/Hibernate
- Event processing is implemented using reactive streams 