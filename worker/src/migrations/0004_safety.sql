-- Wave Live - Safety and moderation

CREATE TABLE IF NOT EXISTS blocks (
  blocker_id TEXT NOT NULL,
  blocked_id TEXT NOT NULL,
  created_at TEXT NOT NULL,

  PRIMARY KEY (blocker_id, blocked_id),

  FOREIGN KEY (blocker_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  FOREIGN KEY (blocked_id)
    REFERENCES users(id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_blocks_blocked
ON blocks(blocked_id);

CREATE INDEX IF NOT EXISTS idx_blocks_blocker_created
ON blocks(blocker_id, created_at DESC);

-- reports is already created by 0001_init.sql.
-- Add indexes only.

CREATE INDEX IF NOT EXISTS idx_reports_created
ON reports(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_reporter
ON reports(reporter_id);

CREATE INDEX IF NOT EXISTS idx_reports_reported_user
ON reports(reported_user_id);

CREATE INDEX IF NOT EXISTS idx_reports_video
ON reports(video_id);
