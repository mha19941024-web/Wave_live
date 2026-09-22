export interface Env {
  DB: D1Database;
  ENVIRONMENT: string;
  SESSION_DAYS: string;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PATCH, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Content-Type": "application/json; charset=utf-8",
};

const GIFTS = [
  {
    id: "rose",
    name: "Rose",
    price: 5,
    icon: "🌹",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "heart",
    name: "Heart",
    price: 20,
    icon: "❤️",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "diamond",
    name: "Diamond",
    price: 100,
    icon: "💎",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "fire",
    name: "Fire",
    price: 250,
    icon: "🔥",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "wave_crown",
    name: "Wave Crown",
    price: 500,
    icon: "👑",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "royal_wave_crown",
    name: "Royal Wave Crown",
    price: 1000,
    icon: "👑",
    imageUrl: null,
    animationUrl: null,
  },
];

const EFFECTS = [
  {
    id: "natural",
    name: "Natural",
    type: "filter",
    value: "none",
  },
  {
    id: "bright",
    name: "Bright",
    type: "filter",
    value: "brightness(1.12)",
  },
  {
    id: "warm",
    name: "Warm",
    type: "filter",
    value: "sepia(0.12) saturate(1.15)",
  },
  {
    id: "cool",
    name: "Cool",
    type: "filter",
    value: "hue-rotate(8deg) saturate(1.1)",
  },
  {
    id: "vintage",
    name: "Vintage",
    type: "filter",
    value: "sepia(0.3) contrast(1.05)",
  },
];

const MUSIC = [
  {
    id: "wave_free_01",
    title: "Wave Beat",
    artist: "Wave Music",
    audio_url: "",
    cover_url: null,
    duration_seconds: 0,
  },
  {
    id: "wave_free_02",
    title: "Night Wave",
    artist: "Wave Music",
    audio_url: "",
    cover_url: null,
    duration_seconds: 0,
  },
  {
    id: "wave_free_03",
    title: "Summer Wave",
    artist: "Wave Music",
    audio_url: "",
    cover_url: null,
    duration_seconds: 0,
  },
];

function response(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: CORS_HEADERS,
  });
}

function makeId(prefix: string): string {
  return `${prefix}_${crypto.randomUUID().replaceAll("-", "")}`;
}

async function readJson(request: Request): Promise<any> {
  try {
    return await request.json();
  } catch {
    return {};
  }
}

function getToken(request: Request): string {
  const authorization = request.headers.get("Authorization") || "";

  if (!authorization.startsWith("Bearer ")) {
    return "";
  }

  return authorization.substring(7).trim();
}

async function ensureSchema(env: Env): Promise<void> {
  await env.DB.batch([
    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        display_name TEXT,
        avatar_url TEXT,
        bio TEXT,
        coins INTEGER NOT NULL DEFAULT 0,
        followers INTEGER NOT NULL DEFAULT 0,
        following INTEGER NOT NULL DEFAULT 0,
        verified INTEGER NOT NULL DEFAULT 0,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS sessions (
        token TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        expires_at TEXT NOT NULL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS videos (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        video_url TEXT NOT NULL,
        thumbnail_url TEXT,
        caption TEXT,
        music_name TEXT,
        likes INTEGER NOT NULL DEFAULT 0,
        comments INTEGER NOT NULL DEFAULT 0,
        shares INTEGER NOT NULL DEFAULT 0,
        views INTEGER NOT NULL DEFAULT 0,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS likes (
        user_id TEXT NOT NULL,
        video_id TEXT NOT NULL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (user_id, video_id)
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS lives (
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
        started_at TEXT DEFAULT CURRENT_TIMESTAMP,
        ended_at TEXT
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS gift_transactions (
        id TEXT PRIMARY KEY,
        live_id TEXT NOT NULL,
        sender_user_id TEXT NOT NULL,
        receiver_user_id TEXT,
        gift_id TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        total_coins INTEGER NOT NULL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS deposits (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        amount INTEGER NOT NULL,
        wallet_number TEXT NOT NULL,
        transaction_reference TEXT,
        coins INTEGER NOT NULL,
        status TEXT NOT NULL DEFAULT 'pending',
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS follows (
        follower_id TEXT NOT NULL,
        following_id TEXT NOT NULL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (follower_id, following_id)
      )
    `),

    env.DB.prepare(`
      CREATE TABLE IF NOT EXISTS reports (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        target_type TEXT NOT NULL,
        target_id TEXT NOT NULL,
        reason TEXT NOT NULL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `),
  ]);
}

async function hashPassword(password: string): Promise<string> {
  const data = new TextEncoder().encode(password);

  const digest = await crypto.subtle.digest(
    "SHA-256",
    data
  );

  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function publicUser(user: any): any {
  return {
    id: String(user.id),
    username: String(user.username || ""),
    displayName: String(
      user.display_name ||
      user.displayName ||
      user.username ||
      ""
    ),
    avatar: user.avatar_url || user.avatar || null,
    bio: user.bio || null,
    coins: Number(user.coins || 0),
    followers: Number(user.followers || 0),
    following: Number(user.following || 0),
    verified: Number(user.verified || 0) === 1,
  };
}

async function getCurrentUser(
  env: Env,
  request: Request
): Promise<any | null> {
  const token = getToken(request);

  if (!token) {
    return null;
  }

  const user = await env.DB.prepare(`
    SELECT
      u.*
    FROM sessions s
    INNER JOIN users u
      ON u.id = s.user_id
    WHERE s.token = ?
      AND s.expires_at > datetime('now')
    LIMIT 1
  `)
    .bind(token)
    .first();

  return user || null;
}

async function requireUser(
  env: Env,
  request: Request
): Promise<any | null> {
  return await getCurrentUser(env, request);
}

async function createSession(
  env: Env,
  userId: string
): Promise<string> {
  const token =
    `${crypto.randomUUID()}.${crypto.randomUUID()}`;

  const days = Number(env.SESSION_DAYS || 30);

  const safeDays =
    Number.isFinite(days) && days > 0
      ? Math.floor(days)
      : 30;

  await env.DB.prepare(`
    INSERT INTO sessions (
      token,
      user_id,
      expires_at
    )
    VALUES (
      ?,
      ?,
      datetime('now', '+' || ? || ' days')
    )
  `)
    .bind(token, userId, safeDays)
    .run();

  return token;
}

function normalizeUsername(value: unknown): string {
  return String(value || "")
    .trim()
    .toLowerCase();
}

function findGift(id: string): any | null {
  return GIFTS.find((gift) => gift.id === id) || null;
}

function mapLive(live: any): any {
  return {
    id: live.id,
    userId: live.user_id,
    username: live.username || "",
    displayName:
      live.display_name ||
      live.username ||
      "",
    avatar: live.avatar_url || null,
    title: live.title,
    streamUrl: live.stream_url || null,
    playbackUrl: live.playback_url || null,
    rtmpsUrl: live.rtmps_url || null,
    streamKey: live.stream_key || null,
    viewerCount: Number(live.viewer_count || 0),
    likes: Number(live.likes || 0),
    status: live.status,
    startedAt: live.started_at,
  };
}

async function handleRequest(
  request: Request,
  env: Env
): Promise<Response> {
  await ensureSchema(env);

  const url = new URL(request.url);
  const method = request.method;
  const path =
    url.pathname.replace(/\/+/g, "/").replace(/\/$/, "") ||
    "/";

  if (
    method === "GET" &&
    path === "/health"
  ) {
    return response({
      success: true,
      status: "ok",
      service: "Wave Live API",
      environment: env.ENVIRONMENT || "production",
    });
  }

  if (
    method === "POST" &&
    path === "/api/auth/register"
  ) {
    const data = await readJson(request);

    const username = normalizeUsername(data.username);
    const password = String(data.password || "");
    const displayName =
      String(
        data.displayName ||
        data.display_name ||
        username
      ).trim();

    if (!/^[a-z0-9_]{3,24}$/.test(username)) {
      return response(
        {
          success: false,
          message:
            "Username must contain 3-24 lowercase letters, numbers or underscores",
        },
        400
      );
    }

    if (
      password.length < 8 ||
      password.length > 128
    ) {
      return response(
        {
          success: false,
          message:
            "Password must be between 8 and 128 characters",
        },
        400
      );
    }

    const existing =
      await env.DB.prepare(
        "SELECT id FROM users WHERE username = ? LIMIT 1"
      )
        .bind(username)
        .first();

    if (existing) {
      return response(
        {
          success: false,
          message: "Username already exists",
        },
        409
      );
    }

    const userId = makeId("usr");
    const passwordHash =
      await hashPassword(password);

    await env.DB.prepare(`
      INSERT INTO users (
        id,
        username,
        password_hash,
        display_name
      )
      VALUES (?, ?, ?, ?)
    `)
      .bind(
        userId,
        username,
        passwordHash,
        displayName || username
      )
      .run();

    const token =
      await createSession(env, userId);

    const user =
      await env.DB.prepare(
        "SELECT * FROM users WHERE id = ? LIMIT 1"
      )
        .bind(userId)
        .first();

    return response({
      success: true,
      token,
      user: publicUser(user),
    });
  }

  if (
    method === "POST" &&
    path === "/api/auth/login"
  ) {
    const data = await readJson(request);

    const username =
      normalizeUsername(data.username);

    const password =
      String(data.password || "");

    const user =
      await env.DB.prepare(
        "SELECT * FROM users WHERE username = ? LIMIT 1"
      )
        .bind(username)
        .first();

    if (!user) {
      return response(
        {
          success: false,
          message: "Invalid username or password",
        },
        401
      );
    }

    const passwordHash =
      await hashPassword(password);

    if (
      String(user.password_hash) !==
      passwordHash
    ) {
      return response(
        {
          success: false,
          message: "Invalid username or password",
        },
        401
      );
    }

    const token =
      await createSession(
        env,
        String(user.id)
      );

    return response({
      success: true,
      token,
      user: publicUser(user),
    });
  }

  if (
    method === "POST" &&
    path === "/api/auth/logout"
  ) {
    const token = getToken(request);

    if (token) {
      await env.DB.prepare(
        "DELETE FROM sessions WHERE token = ?"
      )
        .bind(token)
        .run();
    }

    return response({
      success: true,
    });
  }

  if (
    method === "GET" &&
    path === "/api/me"
  ) {
    const user =
      await requireUser(env, request);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    return response({
      success: true,
      user: publicUser(user),
    });
  }

  if (
    method === "GET" &&
    path === "/api/gifts"
  ) {
    return response({
      success: true,
      items: GIFTS,
    });
  }

  if (
    method === "GET" &&
    path === "/api/effects"
  ) {
    return response({
      success: true,
      items: EFFECTS,
    });
  }

  if (
    method === "GET" &&
    path === "/api/music"
  ) {
    return response({
      success: true,
      items: MUSIC,
    });
  }

  if (
    method === "GET" &&
    path === "/api/feed"
  ) {
    const user =
      await getCurrentUser(env, request);

    const requestedLimit =
      Number(
        url.searchParams.get("limit") || "20"
      );

    const limit = Math.min(
      50,
      Math.max(
        1,
        Number.isFinite(requestedLimit)
          ? Math.floor(requestedLimit)
          : 20
      )
    );

    const requestedCursor =
      Number(
        url.searchParams.get("cursor") || "0"
      );

    const cursor = Math.max(
      0,
      Number.isFinite(requestedCursor)
        ? Math.floor(requestedCursor)
        : 0
    );

    const result =
      await env.DB.prepare(`
        SELECT
          v.*,
          u.username,
          u.display_name,
          u.avatar_url,
          u.verified
        FROM videos v
        INNER JOIN users u
          ON u.id = v.user_id
        ORDER BY v.created_at DESC
        LIMIT ?
        OFFSET ?
      `)
        .bind(limit, cursor)
        .all();

    const items: any[] = [];

    for (const row of result.results || []) {
      let liked = false;

      if (user) {
        const like =
          await env.DB.prepare(`
            SELECT 1
            FROM likes
            WHERE user_id = ?
              AND video_id = ?
            LIMIT 1
          `)
            .bind(
              user.id,
              row.id
            )
            .first();

        liked = Boolean(like);
      }

      items.push({
        id: row.id,
        userId: row.user_id,
        username: row.username,
        displayName:
          row.display_name ||
          row.username,
        avatar:
          row.avatar_url || null,
        videoUrl: row.video_url,
        thumbnailUrl:
          row.thumbnail_url || null,
        caption:
          row.caption || "",
        musicName:
          row.music_name || null,
        likes:
          Number(row.likes || 0),
        comments:
          Number(row.comments || 0),
        shares:
          Number(row.shares || 0),
        views:
          Number(row.views || 0),
        liked,
        createdAt:
          row.created_at,
      });
    }

    return response({
      success: true,
      items,
      nextCursor:
        items.length === limit
          ? cursor + limit
          : null,
    });
  }

  if (
    method === "POST" &&
    path === "/api/videos"
  ) {
    const user =
      await requireUser(env, request);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const data = await readJson(request);

    const videoUrl = String(
      data.videoUrl ||
      data.video_url ||
      ""
    ).trim();

    if (!videoUrl) {
      return response(
        {
          success: false,
          message: "videoUrl is required",
        },
        400
      );
    }

    const videoId =
      makeId("vid");

    await env.DB.prepare(`
      INSERT INTO videos (
        id,
        user_id,
        video_url,
        thumbnail_url,
        caption,
        music_name
      )
      VALUES (?, ?, ?, ?, ?, ?)
    `)
      .bind(
        videoId,
        user.id,
        videoUrl,
        data.thumbnailUrl ||
          data.thumbnail_url ||
          null,
        data.caption || "",
        data.musicName ||
          data.music_name ||
          null
      )
      .run();

    return response({
      success: true,
      video: {
        id: videoId,
        videoUrl,
      },
    });
  }

  const likeMatch =
    path.match(
      /^\/api\/videos\/([^/]+)\/like$/
    );

  if (
    method === "POST" &&
    likeMatch
  ) {
    const user =
      await requireUser(env, request);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const videoId =
      decodeURIComponent(
        likeMatch[1]
      );

    const video =
      await env.DB.prepare(
        "SELECT id FROM videos WHERE id = ? LIMIT 1"
      )
        .bind(videoId)
        .first();

    if (!video) {
      return response(
        {
          success: false,
          message: "Video not found",
        },
        404
      );
    }

    const existing =
      await env.DB.prepare(`
        SELECT 1
        FROM likes
        WHERE user_id = ?
          AND video_id = ?
        LIMIT 1
      `)
        .bind(
          user.id,
          videoId
        )
        .first();

    if (existing) {
      await env.DB.batch([
        env.DB.prepare(`
          DELETE FROM likes
          WHERE user_id = ?
            AND video_id = ?
        `).bind(
          user.id,
          videoId
        ),

        env.DB.prepare(`
          UPDATE videos
          SET likes = MAX(0, likes - 1)
          WHERE id = ?
        `).bind(videoId),
      ]);

      return response({
        success: true,
        liked: false,
      });
    }

    await env.DB.batch([
      env.DB.prepare(`
        INSERT INTO likes (
          user_id,
          video_id
        )
        VALUES (?, ?)
      `).bind(
        user.id,
        videoId
      ),

      env.DB.prepare(`
        UPDATE videos
        SET likes = likes + 1
        WHERE id = ?
      `).bind(videoId),
    ]);

    return response({
      success: true,
      liked: true,
    });
  }

  if (
    method === "GET" &&
    path === "/api/live"
  ) {
    const result =
      await env.DB.prepare(`
        SELECT
          l.*,
          u.username,
          u.display_name,
          u.avatar_url
        FROM lives l
        INNER JOIN users u
          ON u.id = l.user_id
        WHERE l.status = 'active'
        ORDER BY l.started_at DESC
      `)
        .all();

    return response({
      success: true,
      items:
        (result.results || []).map(
          (live: any) =>
            mapLive(live)
        ),
    });
  }

  if (
    method === "POST" &&
    path === "/api/live/create"
  ) {
    const user =
      await requireUser(env, request);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const data = await readJson(request);

    const title =
      String(data.title || "").trim();

    if (!title) {
      return response(
        {
          success: false,
          message:
            "Live title is required",
        },
        400
      );
    }

    const liveId =
      makeId("live");

    await env.DB.prepare(`
      INSERT INTO lives (
        id,
        user_id,
        title,
        status
      )
      VALUES (?, ?, ?, 'active')
    `)
      .bind(
        liveId,
        user.id,
        title
      )
      .run();

    const live =
      await env.DB.prepare(`
        SELECT
          l.*,
          u.username,
          u.display_name,
          u.avatar_url
        FROM lives l
        INNER JOIN users u
          ON u.id = l.user_id
        WHERE l.id = ?
        LIMIT 1
      `)
        .bind(liveId)
        .first();

    return response({
      success: true,
      live: mapLive(live),
    });
  }

  const liveMatch =
    path.match(
      /^\/api\/live\/([^/]+)$/
    );

  if (liveMatch) {
    const liveId =
      decodeURIComponent(
        liveMatch[1]
      );

    if (
      method === "GET"
    ) {
      const live =
        await env.DB.prepare(`
          SELECT
            l.*,
            u.username,
            u.display_name,
            u.avatar_url
          FROM lives l
          INNER JOIN users u
            ON u.id = l.user_id
          WHERE l.id = ?
          LIMIT 1
        `)
          .bind(liveId)
          .first();

      if (!live) {
        return response(
          {
            success: false,
            message: "Live not found",
          },
          404
        );
      }

      return response({
        success: true,
        live: mapLive(live),
      });
    }

    if (
      method === "PATCH"
    ) {
      const user =
        await requireUser(
          env,
          request
        );

      if (!user) {
        return response(
          {
            success: false,
            message: "Unauthorized",
          },
          401
        );
      }

      const live =
        await env.DB.prepare(
          "SELECT * FROM lives WHERE id = ? LIMIT 1"
        )
          .bind(liveId)
          .first();

      if (!live) {
        return response(
          {
            success: false,
            message: "Live not found",
          },
          404
        );
      }

      if (
        String(live.user_id) !==
        String(user.id)
      ) {
        return response(
          {
            success: false,
            message: "Forbidden",
          },
          403
        );
      }

      const data =
        await readJson(request);

      if (
        data.title !== undefined
      ) {
        const title =
          String(data.title).trim();

        if (!title) {
          return response(
            {
              success: false,
              message:
                "Title cannot be empty",
            },
            400
          );
        }

        await env.DB.prepare(`
          UPDATE lives
          SET title = ?
          WHERE id = ?
        `)
          .bind(
            title,
            liveId
          )
          .run();
      }

      if (
        data.status !== undefined
      ) {
        const allowedStatuses = [
          "active",
          "ended",
          "paused",
        ];

        const status =
          String(data.status);

        if (
          !allowedStatuses.includes(
            status
          )
        ) {
          return response(
            {
              success: false,
              message:
                "Invalid live status",
            },
            400
          );
        }

        if (status === "ended") {
          await env.DB.prepare(`
            UPDATE lives
            SET
              status = ?,
              ended_at = CURRENT_TIMESTAMP
            WHERE id = ?
          `)
            .bind(
              status,
              liveId
            )
            .run();
        } else {
          await env.DB.prepare(`
            UPDATE lives
            SET status = ?
            WHERE id = ?
          `)
            .bind(
              status,
              liveId
            )
            .run();
        }
      }

      const updated =
        await env.DB.prepare(`
          SELECT
            l.*,
            u.username,
            u.display_name,
            u.avatar_url
          FROM lives l
          INNER JOIN users u
            ON u.id = l.user_id
          WHERE l.id = ?
          LIMIT 1
        `)
          .bind(liveId)
          .first();

      return response({
        success: true,
        live: mapLive(updated),
      });
    }
  }

  const giftMatch =
    path.match(
      /^\/api\/live\/([^/]+)\/gifts$/
    );

  if (
    method === "POST" &&
    giftMatch
  ) {
    const user =
      await requireUser(
        env,
        request
      );

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const liveId =
      decodeURIComponent(
        giftMatch[1]
      );

    const data =
      await readJson(request);

    const giftId =
      String(
        data.giftId ||
        data.gift_id ||
        ""
      );

    const gift =
      findGift(giftId);

    if (!gift) {
      return response(
        {
          success: false,
          message: "Gift not found",
        },
        404
      );
    }

    const requestedQuantity =
      Number(
        data.quantity || 1
      );

    const quantity = Math.min(
      100,
      Math.max(
        1,
        Number.isFinite(
          requestedQuantity
        )
          ? Math.floor(
              requestedQuantity
            )
          : 1
      )
    );

    const live =
      await env.DB.prepare(`
        SELECT *
        FROM lives
        WHERE id = ?
          AND status = 'active'
        LIMIT 1
      `)
        .bind(liveId)
        .first();

    if (!live) {
      return response(
        {
          success: false,
          message:
            "Active live not found",
        },
        404
      );
    }

    const totalCoins =
      gift.price * quantity;

    const senderCoins =
      Number(user.coins || 0);

    if (
      senderCoins <
      totalCoins
    ) {
      return response(
        {
          success: false,
          message:
            "Insufficient coins",
          requiredCoins:
            totalCoins,
          currentCoins:
            senderCoins,
        },
        400
      );
    }

    const receiverId =
      String(
        data.receiverUserId ||
        data.receiver_user_id ||
        live.user_id
      );

    const receiver =
      await env.DB.prepare(
        "SELECT id FROM users WHERE id = ? LIMIT 1"
      )
        .bind(receiverId)
        .first();

    if (!receiver) {
      return response(
        {
          success: false,
          message:
            "Receiver not found",
        },
        404
      );
    }

    const transactionId =
      makeId("gift");

    await env.DB.batch([
      env.DB.prepare(`
        UPDATE users
        SET coins = coins - ?
        WHERE id = ?
          AND coins >= ?
      `).bind(
        totalCoins,
        user.id,
        totalCoins
      ),

      env.DB.prepare(`
        UPDATE users
        SET coins = coins + ?
        WHERE id = ?
      `).bind(
        totalCoins,
        receiverId
      ),

      env.DB.prepare(`
        INSERT INTO gift_transactions (
          id,
          live_id,
          sender_user_id,
          receiver_user_id,
          gift_id,
          quantity,
          total_coins
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `).bind(
        transactionId,
        liveId,
        user.id,
        receiverId,
        gift.id,
        quantity,
        totalCoins
      ),
    ]);

    const updatedSender =
      await env.DB.prepare(
        "SELECT coins FROM users WHERE id = ? LIMIT 1"
      )
        .bind(user.id)
        .first();

    return response({
      success: true,
      transactionId,
      remainingCoins:
        Number(
          updatedSender?.coins || 0
        ),
      gift: {
        id: gift.id,
        name: gift.name,
        quantity,
        totalCoins,
      },
    });
  }

  if (
    method === "GET" &&
    path === "/api/wallet"
  ) {
    const user =
      await requireUser(
        env,
        request
      );

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    return response({
      success: true,
      coins:
        Number(user.coins || 0),
      walletNumbers: [
        "01284306120",
        "01144210918",
      ],
      paymentMethods: [
        "wallet",
      ],
    });
  }

  if (
    method === "POST" &&
    path === "/api/wallet/deposit"
  ) {
    const user =
      await requireUser(
        env,
        request
      );

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const data =
      await readJson(request);

    const amount =
      Math.floor(
        Number(
          data.amount || 0
        )
      );

    const walletNumber =
      String(
        data.walletNumber ||
        data.wallet_number ||
        ""
      ).trim();

    const transactionReference =
      String(
        data.transactionReference ||
        data.transaction_reference ||
        ""
      ).trim();

    if (
      amount <= 0 ||
      !walletNumber
    ) {
      return response(
        {
          success: false,
          message:
            "Invalid deposit information",
        },
        400
      );
    }

    const depositId =
      makeId("dep");

    const requestedCoins =
      amount;

    await env.DB.prepare(`
      INSERT INTO deposits (
        id,
        user_id,
        amount,
        wallet_number,
        transaction_reference,
        coins,
        status
      )
      VALUES (?, ?, ?, ?, ?, ?, 'pending')
    `)
      .bind(
        depositId,
        user.id,
        amount,
        walletNumber,
        transactionReference ||
          null,
        requestedCoins
      )
      .run();

    return response({
      success: true,
      depositId,
      amount,
      walletNumber,
      coins: requestedCoins,
      status: "pending",
      message:
        "Deposit submitted for verification",
    });
  }

  if (
    method === "GET" &&
    path === "/api/wallet/deposits"
  ) {
    const user =
      await requireUser(
        env,
        request
      );

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const result =
      await env.DB.prepare(`
        SELECT *
        FROM deposits
        WHERE user_id = ?
        ORDER BY created_at DESC
      `)
        .bind(user.id)
        .all();

    return response({
      success: true,
      items:
        (result.results || []).map(
          (deposit: any) => ({
            id: deposit.id,
            amount:
              Number(
                deposit.amount || 0
              ),
            wallet_number:
              deposit.wallet_number,
            transaction_reference:
              deposit.transaction_reference ||
              null,
            coins:
              Number(
                deposit.coins || 0
              ),
            status:
              deposit.status,
            created_at:
              deposit.created_at,
            updated_at:
              deposit.updated_at,
          })
        ),
    });
  }

  if (
    method === "POST" &&
    path === "/api/reports"
  ) {
    const user =
      await requireUser(
        env,
        request
      );

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401
      );
    }

    const data =
      await readJson(request);

    const targetType =
      String(
        data.targetType ||
        data.target_type ||
        ""
      ).trim();

    const targetId =
      String(
        data.targetId ||
        data.target_id ||
        ""
      ).trim();

    const reason =
      String(
        data.reason || ""
      ).trim();

    if (
      !targetType ||
      !targetId ||
      !reason
    ) {
      return response(
        {
          success: false,
          message:
            "targetType, targetId and reason are required",
        },
        400
      );
    }

    const reportId =
      makeId("report");

    await env.DB.prepare(`
      INSERT INTO reports (
        id,
        user_id,
        target_type,
        target_id,
        reason
      )
      VALUES (?, ?, ?, ?, ?)
    `)
      .bind(
        reportId,
        user.id,
        targetType,
        targetId,
        reason
      )
      .run();

    return response({
      success: true,
      reportId,
    });
  }

  return response(
    {
      success: false,
      message: "Not found",
    },
    404
  );
}

export default {
  async fetch(
    request: Request,
    env: Env
  ): Promise<Response> {
    if (
      request.method === "OPTIONS"
    ) {
      return new Response(null, {
        status: 204,
        headers: CORS_HEADERS,
      });
    }

    try {
      return await handleRequest(
        request,
        env
      );
    } catch (error) {
      console.error(
        "Wave API error:",
        error
      );

      return response(
        {
          success: false,
          message:
            error instanceof Error
              ? error.message
              : "Internal server error",
        },
        500
      );
    }
  },
};
