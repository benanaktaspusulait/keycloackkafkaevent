-- Create the events table
CREATE TABLE IF NOT EXISTS keycloak_events (
    id VARCHAR(255) PRIMARY KEY,
    time TIMESTAMP ,
    event_type VARCHAR(255) NOT NULL,
    realm_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    ip_address VARCHAR(255),
    error VARCHAR(255),
    event_time TIMESTAMP NOT NULL DEFAULT NOW(),
    details JSONB
);

-- Create the event details table
CREATE TABLE IF NOT EXISTS event_details (
    event_id VARCHAR(255) REFERENCES keycloak_events(id),
    detail_key VARCHAR(255) NOT NULL,
    detail_value TEXT,
    PRIMARY KEY (event_id, detail_key)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_events_time ON keycloak_events(time);
CREATE INDEX IF NOT EXISTS idx_events_type ON keycloak_events(type);
CREATE INDEX IF NOT EXISTS idx_events_realm_id ON keycloak_events(realm_id);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON keycloak_events(user_id);
CREATE INDEX IF NOT EXISTS idx_event_details_event_id ON event_details(event_id); 