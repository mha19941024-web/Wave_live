-- Wave Live
-- Accounts + Music + Visual Effects

CREATE TABLE IF NOT EXISTS account_credentials (
  user_id TEXT PRIMARY KEY,
  password_hash TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,

  FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS music_tracks (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  artist TEXT NOT NULL DEFAULT '',
  audio_url TEXT NOT NULL,
  cover_url TEXT,
  duration_seconds INTEGER NOT NULL DEFAULT 0,
  active INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_music_tracks_active
ON music_tracks(active, created_at DESC);

CREATE TABLE IF NOT EXISTS visual_effects (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  value TEXT NOT NULL,
  active INTEGER NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_visual_effects_active
ON visual_effects(active, sort_order);

-- الفلاتر والمؤثرات الأساسية داخل Wave.
-- التطبيق ينفذها محليًا باستخدام ColorMatrix/Compose overlays.

INSERT OR IGNORE INTO visual_effects
(id, name, type, value, active, sort_order)
VALUES
('normal', 'Normal', 'filter', 'normal', 1, 10),
('vivid', 'Vivid', 'filter', 'vivid', 1, 20),
('warm', 'Warm', 'filter', 'warm', 1, 30),
('cool', 'Cool', 'filter', 'cool', 1, 40),
('mono', 'Mono', 'filter', 'mono', 1, 50),
('dream', 'Dream', 'filter', 'dream', 1, 60),
('wave_glow', 'Wave Glow', 'overlay', 'purple_glow', 1, 70),
('sparkle', 'Sparkle', 'overlay', 'sparkle', 1, 80),
('soft_skin', 'Soft Skin', 'filter', 'soft_skin', 1, 90),
('cinematic', 'Cinematic', 'filter', 'cinematic', 1, 100);
