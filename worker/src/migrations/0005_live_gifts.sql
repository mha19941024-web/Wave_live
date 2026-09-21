CREATE TABLE IF NOT EXISTS live_sessions (
  id TEXT PRIMARY KEY,
  host_user_id TEXT NOT NULL,
  title TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'created',
  cloudflare_input_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  ended_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_live_sessions_status
ON live_sessions(status, created_at DESC);


CREATE TABLE IF NOT EXISTS gift_catalog (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  icon TEXT NOT NULL,
  price_coins INTEGER NOT NULL CHECK(price_coins > 0),
  sort_order INTEGER NOT NULL DEFAULT 0,
  active INTEGER NOT NULL DEFAULT 1
);


CREATE TABLE IF NOT EXISTS wallets (
  user_id TEXT PRIMARY KEY,
  coins INTEGER NOT NULL DEFAULT 0 CHECK(coins >= 0),
  updated_at TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS gift_transactions (
  id TEXT PRIMARY KEY,
  live_id TEXT NOT NULL,
  sender_user_id TEXT NOT NULL,
  receiver_user_id TEXT NOT NULL,
  gift_id TEXT NOT NULL,
  quantity INTEGER NOT NULL CHECK(quantity > 0),
  coins_total INTEGER NOT NULL CHECK(coins_total > 0),
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_live
ON gift_transactions(live_id, created_at DESC);


INSERT OR IGNORE INTO gift_catalog
(id, name, icon, price_coins, sort_order, active)
VALUES
('rose', 'Rose', '🌹', 5, 10, 1),
('heart', 'Heart', '💖', 10, 20, 1),
('crown', 'Wave Crown', '👑', 500, 30, 1),
('diamond', 'Wave Diamond', '💎', 250, 40, 1),
('rocket', 'Wave Rocket', '🚀', 500, 50, 1),
('lion', 'Lion', '🦁', 1000, 60, 1),
('star', 'Super Star', '🌟', 1000, 70, 1);
