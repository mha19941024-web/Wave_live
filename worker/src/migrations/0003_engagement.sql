-- Wave Live - Engagement indexes
-- The likes table is already created by 0001_init.sql.
-- Keep one canonical likes table and add indexes only.

CREATE INDEX IF NOT EXISTS idx_likes_user
ON likes(user_id);

CREATE INDEX IF NOT EXISTS idx_likes_video_created
ON likes(video_id, created_at DESC);
