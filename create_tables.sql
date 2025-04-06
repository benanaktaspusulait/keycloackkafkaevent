CREATE TABLE IF NOT EXISTS keycloak_events (
    id VARCHAR(255) PRIMARY KEY,
    event_time TIMESTAMP,
    event_type VARCHAR(255),
    realm_id VARCHAR(255),
    client_id VARCHAR(255),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    ip_address VARCHAR(255),
    error VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS event_details (
    event_id VARCHAR(255) REFERENCES keycloak_events(id),
    detail_key VARCHAR(255),
    detail_value VARCHAR(255),
    PRIMARY KEY (event_id, detail_key)
); 