-- Wave Live - Social indexes
-- The core social tables are created in 0001_init.sql.
-- This migration only adds indexes that improve social queries.

CREATE INDEX IF NOT EXISTS idx_comments_video_created
ON comments(video_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_comments_user
ON comments(user_id);

CREATE INDEX IF NOT EXISTS idx_follows_follower
ON follows(follower_id);

CREATE INDEX IF NOT EXISTS idx_follows_following_created
ON follows(following_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_sessions_user_expires
ON sessions(user_id, expires_at);
