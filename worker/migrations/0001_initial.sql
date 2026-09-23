PRAGMA foreign_keys = ON;

-- =========================================================
-- WAVE LIVE DATABASE
-- Compatible with worker/src/index.ts
-- =========================================================

-- =========================================================
-- USERS
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT NOT NULL DEFAULT '',
    coins INTEGER NOT NULL DEFAULT 0,
    followers INTEGER NOT NULL DEFAULT 0,
    following INTEGER NOT NULL DEFAULT 0,
    verified INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_username
ON users(username);

-- =========================================================
-- ACCOUNT CREDENTIALS
-- =========================================================

CREATE TABLE IF NOT EXISTS account_credentials (
    user_id TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================================================
-- SESSIONS
-- =========================================================

CREATE TABLE IF NOT EXISTS sessions (
    token TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id
ON sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_sessions_expires_at
ON sessions(expires_at);

-- =========================================================
-- VIDEOS
-- =========================================================

CREATE TABLE IF NOT EXISTS videos (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    stream_id TEXT,
    caption TEXT NOT NULL DEFAULT '',
    music_name TEXT,
    likes INTEGER NOT NULL DEFAULT 0,
    comments INTEGER NOT NULL DEFAULT 0,
    shares INTEGER NOT NULL DEFAULT 0,
    views INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_videos_user_id
ON videos(user_id);

CREATE INDEX IF NOT EXISTS idx_videos_created_at
ON videos(created_at);

-- =========================================================
-- LIKES
-- =========================================================

CREATE TABLE IF NOT EXISTS likes (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    video_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE(user_id, video_id),
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    FOREIGN KEY(video_id)
        REFERENCES videos(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_likes_video_id
ON likes(video_id);

CREATE INDEX IF NOT EXISTS idx_likes_user_id
ON likes(user_id);

-- =========================================================
-- COMMENTS
-- =========================================================

CREATE TABLE IF NOT EXISTS comments (
    id TEXT PRIMARY KEY,
    video_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    text TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY(video_id)
        REFERENCES videos(id)
        ON DELETE CASCADE,
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comments_video_id
ON comments(video_id);

CREATE INDEX IF NOT EXISTS idx_comments_user_id
ON comments(user_id);

-- =========================================================
-- FOLLOWS
-- =========================================================

CREATE TABLE IF NOT EXISTS follows (
    follower_id TEXT NOT NULL,
    following_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY(follower_id, following_id),
    FOREIGN KEY(follower_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    FOREIGN KEY(following_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_follows_follower_id
ON follows(follower_id);

CREATE INDEX IF NOT EXISTS idx_follows_following_id
ON follows(following_id);

-- =========================================================
-- LIVE STREAMS
-- =========================================================

CREATE TABLE IF NOT EXISTS live_streams (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    stream_url TEXT,
    playback_url TEXT,
    rtmps_url TEXT,
    stream_key TEXT,
    viewer_count INTEGER NOT NULL DEFAULT 0,
    likes INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active',
    started_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_live_streams_user_id
ON live_streams(user_id);

CREATE INDEX IF NOT EXISTS idx_live_streams_status
ON live_streams(status);

CREATE INDEX IF NOT EXISTS idx_live_streams_started_at
ON live_streams(started_at);

-- =========================================================
-- GIFTS
-- =========================================================

CREATE TABLE IF NOT EXISTS gifts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price INTEGER NOT NULL,
    icon TEXT,
    image_url TEXT,
    animation_url TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_gifts_enabled
ON gifts(enabled);

CREATE INDEX IF NOT EXISTS idx_gifts_price
ON gifts(price);

-- =========================================================
-- GIFT TRANSACTIONS
-- IMPORTANT:
-- Column names MUST match worker/src/index.ts
-- =========================================================

CREATE TABLE IF NOT EXISTS gift_transactions (
    id TEXT PRIMARY KEY,
    live_id TEXT NOT NULL,
    sender_user_id TEXT NOT NULL,
    receiver_user_id TEXT NOT NULL,
    gift_id TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    total_coins INTEGER NOT NULL,
    created_at TEXT NOT NULL,

    FOREIGN KEY(live_id)
        REFERENCES live_streams(id)
        ON DELETE CASCADE,

    FOREIGN KEY(sender_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY(receiver_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY(gift_id)
        REFERENCES gifts(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_live_id
ON gift_transactions(live_id);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_sender
ON gift_transactions(sender_user_id);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_receiver
ON gift_transactions(receiver_user_id);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_created
ON gift_transactions(created_at);

-- =========================================================
-- WALLET DEPOSITS
-- =========================================================

CREATE TABLE IF NOT EXISTS wallet_deposits (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    wallet_number TEXT NOT NULL,
    transaction_reference TEXT,
    coins INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,

    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wallet_deposits_user_id
ON wallet_deposits(user_id);

CREATE INDEX IF NOT EXISTS idx_wallet_deposits_status
ON wallet_deposits(status);

CREATE INDEX IF NOT EXISTS idx_wallet_deposits_created
ON wallet_deposits(created_at);

-- =========================================================
-- WALLET TRANSACTIONS
-- =========================================================

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    amount INTEGER NOT NULL DEFAULT 0,
    coins INTEGER NOT NULL DEFAULT 0,
    reference TEXT,
    created_at TEXT NOT NULL,

    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user
ON wallet_transactions(user_id);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_created
ON wallet_transactions(created_at);

-- =========================================================
-- MUSIC
-- =========================================================

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
ON music_tracks(active);

CREATE INDEX IF NOT EXISTS idx_music_tracks_created
ON music_tracks(created_at);

-- =========================================================
-- VISUAL EFFECTS
-- =========================================================

CREATE TABLE IF NOT EXISTS visual_effects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    value TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_visual_effects_active
ON visual_effects(active);

CREATE INDEX IF NOT EXISTS idx_visual_effects_sort
ON visual_effects(sort_order);

-- =========================================================
-- REPORTS
-- =========================================================

CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY,
    reporter_user_id TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL,

    FOREIGN KEY(reporter_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reports_reporter
ON reports(reporter_user_id);

CREATE INDEX IF NOT EXISTS idx_reports_target
ON reports(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_reports_status
ON reports(status);

-- =========================================================
-- BLOCKS
-- =========================================================

CREATE TABLE IF NOT EXISTS blocks (
    blocker_id TEXT NOT NULL,
    blocked_id TEXT NOT NULL,
    created_at TEXT NOT NULL,

    PRIMARY KEY(blocker_id, blocked_id),

    FOREIGN KEY(blocker_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY(blocked_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_blocks_blocker
ON blocks(blocker_id);

CREATE INDEX IF NOT EXISTS idx_blocks_blocked
ON blocks(blocked_id);

-- =========================================================
-- DEFAULT GIFTS
-- =========================================================

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url,
    enabled,
    created_at
)
VALUES
(
    'rose',
    'Rose',
    5,
    '🌹',
    NULL,
    NULL,
    1,
    datetime('now')
);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url,
    enabled,
    created_at
)
VALUES
(
    'heart',
    'Heart',
    10,
    '❤️',
    NULL,
    NULL,
    1,
    datetime('now')
);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url,
    enabled,
    created_at
)
VALUES
(
    'crown',
    'Wave Crown',
    100,
    '👑',
    NULL,
    NULL,
    1,
    datetime('now')
);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url,
    enabled,
    created_at
)
VALUES
(
    'diamond',
    'Diamond',
    500,
    '💎',
    NULL,
    NULL,
    1,
    datetime('now')
);

-- =========================================================
-- DEFAULT MUSIC
-- No copyrighted audio is inserted here.
-- Add only music you have the right to distribute.
-- =========================================================

-- =========================================================
-- DEFAULT VISUAL EFFECTS
-- =========================================================

INSERT OR IGNORE INTO visual_effects
(
    id,
    name,
    type,
    value,
    active,
    sort_order
)
VALUES
(
    'none',
    'Normal',
    'filter',
    'none',
    1,
    0
);

INSERT OR IGNORE INTO visual_effects
(
    id,
    name,
    type,
    value,
    active,
    sort_order
)
VALUES
(
    'beauty',
    'Beauty',
    'filter',
    'beauty',
    1,
    10
);

INSERT OR IGNORE INTO visual_effects
(
    id,
    name,
    type,
    value,
    active,
    sort_order
)
VALUES
(
    'warm',
    'Warm',
    'filter',
    'warm',
    1,
    20
);

INSERT OR IGNORE INTO visual_effects
(
    id,
    name,
    type,
    value,
    active,
    sort_order
)
VALUES
(
    'cool',
    'Cool',
    'filter',
    'cool',
    1,
    30
);

-- =========================================================
-- END OF WAVE LIVE SCHEMA
-- =========================================================
