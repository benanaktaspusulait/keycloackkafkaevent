#!/bin/bash

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
until curl -s -f -o /dev/null http://localhost:8081/health/ready; do
    echo "Waiting for Keycloak..."
    sleep 5
done

# Get access token
echo "Getting access token..."
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8081/realms/master/protocol/openid-connect/token \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=admin" \
    -d "password=admin" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" | jq -r '.access_token')

# Create realm
echo "Creating realm..."
curl -s -X POST http://localhost:8081/admin/realms \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "realm": "smartface",
        "enabled": true,
        "displayName": "SmartFace",
        "eventsEnabled": true,
        "eventsListeners": ["kafka_event_listener"]
    }'

# Create client
echo "Creating client..."
curl -s -X POST http://localhost:8081/admin/realms/smartface/clients \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "clientId": "event-service",
        "enabled": true,
        "publicClient": false,
        "redirectUris": ["http://localhost:8080/*"],
        "webOrigins": ["+"],
        "protocol": "openid-connect",
        "attributes": {
            "access.token.lifespan": "3600"
        }
    }'

# Create user
echo "Creating user..."
curl -s -X POST http://localhost:8081/admin/realms/smartface/users \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "test-user",
        "enabled": true,
        "credentials": [{
            "type": "password",
            "value": "test-password",
            "temporary": false
        }]
    }'

echo "Keycloak setup completed!" 