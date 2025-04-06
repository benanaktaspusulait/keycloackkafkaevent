-- Create the events table
CREATE TABLE IF NOT EXISTS events (
    id VARCHAR(255) PRIMARY KEY,
    time TIMESTAMP NOT NULL,
    type VARCHAR(255) NOT NULL,
    realm_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    ip_address VARCHAR(255),
    error VARCHAR(255),
    details JSONB
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_events_time ON events(time);
CREATE INDEX IF NOT EXISTS idx_events_type ON events(type);
CREATE INDEX IF NOT EXISTS idx_events_realm_id ON events(realm_id);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id); 