-- Add indexes for common query patterns to improve performance

-- Material search (e.g., find all DIAMOND_SWORD events)
CREATE INDEX IF NOT EXISTS idx_material ON item_events(material);

-- Event type filtering (e.g., find all PICKUP events)
CREATE INDEX IF NOT EXISTS idx_event_type ON item_events(event_type);

-- Location-based queries (e.g., find events in a specific world/region)
CREATE INDEX IF NOT EXISTS idx_world ON item_events(world);

-- Composite index for player + material queries
CREATE INDEX IF NOT EXISTS idx_player_material ON item_events(player_uuid, material);

-- Timestamp range queries (already have idx_time, but add for specific ranges)
CREATE INDEX IF NOT EXISTS idx_timestamp_range ON item_events(timestamp);
