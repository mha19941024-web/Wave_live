-- Wave Live
-- Accounts + Music + Visual Effects

CREATE TABLE IF NOT EXISTS account_credentials (
  user_id TEXT PRIMARY KEY,
  password_hash TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
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

-- مكتبة Wave الأساسية.
-- روابط الصوت هنا يجب أن تكون لملفات تملك Wave حق استخدامها.
INSERT OR IGNORE INTO music_tracks
(
  id,
  title,
  artist,
  audio_url,
  duration_seconds,
  active,
  created_at
)
VALUES
(
  'wave_intro',
  'Wave Intro',
  'Wave Music',
  'https://worker-jolly-band-100e.mha19941024.workers.dev/music/wave-intro.mp3',
  30,
  1,
  datetime('now')
);

-- فلاتر بصرية آمنة يمكن تنفيذها داخل التطبيق
-- باستخدام ColorMatrix/Compose overlays.
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
('sparkle', 'Sparkle', 'overlay', 'sparkle', 1, 80);
