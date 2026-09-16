CREATE TABLE IF NOT EXISTS videos (
  id TEXT PRIMARY KEY,
  url TEXT NOT NULL,
  user TEXT NOT NULL,
  caption TEXT NOT NULL DEFAULT '',
  likes INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_videos_created_at ON videos(created_at DESC);
