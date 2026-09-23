ALTER TABLE live_streams
ADD COLUMN ended_at TEXT;

CREATE INDEX IF NOT EXISTS idx_live_streams_ended_at
ON live_streams(ended_at);
