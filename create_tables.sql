-- Create the events table
CREATE TABLE IF NOT EXISTS keycloak_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    time TIMESTAMP NOT NULL DEFAULT NOW(),
    type VARCHAR(255) NOT NULL,
    realm_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    ip_address VARCHAR(255),
    error VARCHAR(255),
    details JSONB
);

-- Create the outbox table
CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    realm_id VARCHAR(255),
    client_id VARCHAR(255),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    ip_address VARCHAR(255),
    error VARCHAR(255),
    details JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP
);

-- Create the event details table
CREATE TABLE IF NOT EXISTS event_details (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    CONSTRAINT fk_event_details_event FOREIGN KEY (event_id) REFERENCES keycloak_events(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_events_time ON keycloak_events(time);
CREATE INDEX IF NOT EXISTS idx_events_type ON keycloak_events(type);
CREATE INDEX IF NOT EXISTS idx_events_realm_id ON keycloak_events(realm_id);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON keycloak_events(user_id);
CREATE INDEX IF NOT EXISTS idx_event_details_event_id ON event_details(event_id);
CREATE INDEX IF NOT EXISTS idx_outbox_status ON event_outbox(status);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON event_outbox(created_at); 