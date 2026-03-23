-- Migration script for creating gateway_routes table
-- This table stores Spring Cloud Gateway route definitions persistently

-- Create the gateway_routes table
CREATE TABLE IF NOT EXISTS gateway_routes (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    uri VARCHAR(500) NOT NULL,
    predicates TEXT,
    filters TEXT,
    metadata TEXT,
    route_order INTEGER,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comments for documentation
COMMENT ON TABLE gateway_routes IS 'Stores Spring Cloud Gateway route definitions for persistence';
COMMENT ON COLUMN gateway_routes.id IS 'Unique route identifier';
COMMENT ON COLUMN gateway_routes.uri IS 'Target URI for the route';
COMMENT ON COLUMN gateway_routes.predicates IS 'Route predicates in JSON format';
COMMENT ON COLUMN gateway_routes.filters IS 'Route filters in JSON format';
COMMENT ON COLUMN gateway_routes.metadata IS 'Route metadata in JSON format';
COMMENT ON COLUMN gateway_routes.route_order IS 'Route execution order (lower values execute first)';
COMMENT ON COLUMN gateway_routes.enabled IS 'Whether the route is active';
COMMENT ON COLUMN gateway_routes.created_at IS 'Route creation timestamp';
COMMENT ON COLUMN gateway_routes.updated_at IS 'Route last update timestamp';

