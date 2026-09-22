-- Wave Live - Live streaming and gifts
-- Core live/gift tables are created by 0001_init.sql.
-- This migration only adds the production gift catalog
-- and useful indexes.

CREATE TABLE IF NOT EXISTS gift_catalog (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  icon TEXT NOT NULL,
  price_coins INTEGER NOT NULL CHECK (price_coins > 0),
  sort_order INTEGER NOT NULL DEFAULT 0,
  active INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_gift_catalog_active
ON gift_catalog(active, sort_order);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_live
ON gift_transactions(live_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_sender
ON gift_transactions(sender_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_receiver
ON gift_transactions(receiver_user_id, created_at DESC);

-- Starter Wave gift catalog.
-- Graphic/icon assets can be replaced later without changing the IDs.

INSERT OR IGNORE INTO gift_catalog
(id, name, icon, price_coins, sort_order, active)
VALUES
('rose', 'Wave Rose', '🌹', 5, 10, 1),
('heart', 'Wave Heart', '💖', 10, 20, 1),
('fire', 'Wave Fire', '🔥', 25, 30, 1),
('star', 'Super Star', '🌟', 50, 40, 1),
('diamond', 'Wave Diamond', '💎', 100, 50, 1),
('rocket', 'Wave Rocket', '🚀', 250, 60, 1),
('crown', 'Wave Crown', '👑', 500, 70, 1),
('royal_crown', 'Royal Wave Crown', '👑', 1000, 80, 1),
('lion', 'Golden Lion', '🦁', 1500, 90, 1);
