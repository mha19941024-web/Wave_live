PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    avatar TEXT,
    bio TEXT,
    coins INTEGER NOT NULL DEFAULT 0,
    followers INTEGER NOT NULL DEFAULT 0,
    following INTEGER NOT NULL DEFAULT 0,
    verified INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    token TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id
ON sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_sessions_expires_at
ON sessions(expires_at);

CREATE TABLE IF NOT EXISTS videos (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption TEXT NOT NULL DEFAULT '',
    music_name TEXT,
    likes INTEGER NOT NULL DEFAULT 0,
    comments INTEGER NOT NULL DEFAULT 0,
    shares INTEGER NOT NULL DEFAULT 0,
    views INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_videos_user_id
ON videos(user_id);

CREATE INDEX IF NOT EXISTS idx_videos_created_at
ON videos(created_at);

CREATE TABLE IF NOT EXISTS likes (
    user_id TEXT NOT NULL,
    video_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY(user_id, video_id)
);

CREATE INDEX IF NOT EXISTS idx_likes_video_id
ON likes(video_id);

CREATE TABLE IF NOT EXISTS comments (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    video_id TEXT NOT NULL,
    text TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_video_id
ON comments(video_id);

CREATE TABLE IF NOT EXISTS follows (
    follower_id TEXT NOT NULL,
    following_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY(follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_following_id
ON follows(following_id);

CREATE TABLE IF NOT EXISTS live (
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
    started_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_live_user_id
ON live(user_id);

CREATE INDEX IF NOT EXISTS idx_live_status
ON live(status);

CREATE INDEX IF NOT EXISTS idx_live_started_at
ON live(started_at);

CREATE TABLE IF NOT EXISTS gifts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price INTEGER NOT NULL,
    icon TEXT,
    image_url TEXT,
    animation_url TEXT
);

CREATE TABLE IF NOT EXISTS gift_transactions (
    id TEXT PRIMARY KEY,
    live_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    receiver_id TEXT,
    gift_id TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    total_coins INTEGER NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_live_id
ON gift_transactions(live_id);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_sender_id
ON gift_transactions(sender_id);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_receiver_id
ON gift_transactions(receiver_id);

CREATE TABLE IF NOT EXISTS deposits (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    wallet_number TEXT NOT NULL,
    transaction_reference TEXT,
    coins INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_deposits_user_id
ON deposits(user_id);

CREATE INDEX IF NOT EXISTS idx_deposits_status
ON deposits(status);

CREATE INDEX IF NOT EXISTS idx_deposits_created_at
ON deposits(created_at);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url
)
VALUES
(
    'rose',
    'Rose',
    5,
    '🌹',
    NULL,
    NULL
);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url
)
VALUES
(
    'heart',
    'Heart',
    10,
    '❤️',
    NULL,
    NULL
);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url
)
VALUES
(
    'crown',
    'Wave Crown',
    100,
    '👑',
    NULL,
    NULL
);

INSERT OR IGNORE INTO gifts
(
    id,
    name,
    price,
    icon,
    image_url,
    animation_url
)
VALUES
(
    'diamond',
    'Diamond',
    500,
    '💎',
    NULL,
    NULL
);
