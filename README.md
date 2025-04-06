# Keycloak Event Service

This project implements a system to capture and process Keycloak events using Kafka and store them in a PostgreSQL database. The system consists of two main components:

## Project Structure

```
.
├── keycloak-kafka-event-listener/    # Keycloak event listener implementation
├── keycloak-event-service/           # Event service that consumes and processes events
│   ├── k8s/                         # Kubernetes deployment configurations
│   └── scripts/                     # Utility scripts
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
1. Start the services:
```bash
docker compose up -d
```

2. The event service will automatically:
- Connect to Redpanda (Kafka)
- Create necessary database tables
- Start consuming events

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

## Utility Scripts

The `scripts/` directory contains:
- `setup-keycloak.sh`: Automates Keycloak configuration including:
  - Realm creation
  - Client setup
  - User creation
  - Event listener configuration

## Troubleshooting

If you encounter connection issues:
1. Verify Redpanda is running and accessible
2. Check the event service logs
3. Ensure all services are on the same Docker network
4. For Kubernetes deployments, check pod status and logs

## Development

- The project uses Java with Quarkus framework
- Kafka integration is handled by SmallRye Reactive Messaging
- Database operations use JPA/Hibernate 