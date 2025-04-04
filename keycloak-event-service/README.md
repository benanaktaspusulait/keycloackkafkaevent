# Keycloak Event Service

A service that listens to Keycloak events and sinks them to PostgreSQL via Kafka Connect.

## Features

- Listens to Keycloak events via gRPC
- Publishes events to Kafka
- Sinks events to PostgreSQL using Kafka Connect
- Health monitoring and metrics
- Kubernetes-native deployment

## Prerequisites

- Kubernetes cluster
- kubectl configured to access the cluster
- Docker or container runtime
- Maven 3.8+
- Java 17+

## Deployment Steps

### 1. Build the Application

```bash
# Build the application
./mvnw clean package

# Build the Docker image
docker build -f src/main/docker/Dockerfile.jvm -t keycloak-event-service:latest .
```

### 2. Prepare PostgreSQL JDBC Driver

```bash
# Download PostgreSQL JDBC driver
wget https://jdbc.postgresql.org/download/postgresql-42.5.1.jar

# Create ConfigMap with the driver
kubectl create configmap postgres-jdbc-driver \
  --from-file=postgresql.jar=postgresql-42.5.1.jar \
  -n keycloak-events
```

### 3. Deploy Infrastructure Components

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy PostgreSQL
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml

# Deploy Kafka and Zookeeper
kubectl apply -f k8s/kafka-deployment.yaml
kubectl apply -f k8s/kafka-service.yaml
kubectl apply -f k8s/zookeeper-deployment.yaml
kubectl apply -f k8s/zookeeper-service.yaml

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n keycloak-events
kubectl wait --for=condition=ready pod -l app=kafka -n keycloak-events
kubectl wait --for=condition=ready pod -l app=zookeeper -n keycloak-events
```

### 4. Deploy Application Components

```bash
# Deploy application configuration
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Deploy the application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml

# Deploy Kafka Connect
kubectl apply -f k8s/kafka-connect-deployment.yaml
kubectl apply -f k8s/kafka-connect-service.yaml
kubectl apply -f k8s/kafka-connect-connector.yaml

# Wait for application to be ready
kubectl wait --for=condition=ready pod -l app=keycloak-event-service -n keycloak-events
kubectl wait --for=condition=ready pod -l app=kafka-connect -n keycloak-events
```

### 5. Create Kafka Connect Connector

```bash
# Create the connector using Kafka Connect REST API
curl -X POST -H "Content-Type: application/json" \
  --data @k8s/kafka-connect-connector.yaml \
  http://kafka-connect:8083/connectors
```

### 6. Verify Deployment

```bash
# Check application logs
kubectl logs -l app=keycloak-event-service -n keycloak-events

# Check Kafka Connect logs
kubectl logs -l app=kafka-connect -n keycloak-events

# Check connector status
curl http://kafka-connect:8083/connectors/keycloak-events-sink/status

# Check PostgreSQL for events
kubectl exec -it $(kubectl get pod -l app=postgres -n keycloak-events -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres -d keycloak_events -c "SELECT * FROM keycloak_events LIMIT 5;"
```

## Accessing the Service

- gRPC Service: `keycloak-event-service.keycloak-events.svc.cluster.local:9000`
- HTTP Health Check: `http://keycloak-event-service.keycloak-events.svc.cluster.local:8080/q/health`
- Metrics: `http://keycloak-event-service.keycloak-events.svc.cluster.local:8080/q/metrics`
- Kafka Connect REST API: `http://kafka-connect.keycloak-events.svc.cluster.local:8083`

## Configuration

### Environment Variables

- `POSTGRES_PASSWORD`: PostgreSQL password
- `KAFKA_SASL_USERNAME`: Kafka SASL username
- `KAFKA_SASL_PASSWORD`: Kafka SASL password

### Application Properties

See `k8s/configmap.yaml` for all configurable properties.

## Monitoring

The service exposes the following endpoints:

- `/q/health`: Health check endpoint
- `/q/health/live`: Liveness probe
- `/q/health/ready`: Readiness probe
- `/q/metrics`: Prometheus metrics

## Troubleshooting

1. Check pod status:
```bash
kubectl get pods -n keycloak-events
```

2. Check pod logs:
```bash
kubectl logs -l app=keycloak-event-service -n keycloak-events
kubectl logs -l app=kafka-connect -n keycloak-events
```

3. Check connector status:
```bash
curl http://kafka-connect:8083/connectors/keycloak-events-sink/status
```

4. Check database connection:
```bash
kubectl exec -it $(kubectl get pod -l app=postgres -n keycloak-events -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres -d keycloak_events
```

## Cleanup

To remove all resources:

```bash
kubectl delete namespace keycloak-events
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details. 