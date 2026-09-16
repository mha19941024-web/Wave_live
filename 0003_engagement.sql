CREATE TABLE IF NOT EXISTS video_likes (
  video_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  PRIMARY KEY (video_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_video_likes_user ON video_likes(user_id);
