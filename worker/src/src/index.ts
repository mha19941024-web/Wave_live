export interface Env {
  DB: D1Database;
  ENVIRONMENT?: string;
  SESSION_DAYS?: string;
  CORS_ORIGIN?: string;
}

const DEFAULT_GIFTS = [
  {
    id: "rose",
    name: "Rose",
    price: 5,
    icon: "🌹",
    image_url: null,
    animation_url: null,
  },
  {
    id: "heart",
    name: "Heart",
    price: 10,
    icon: "❤️",
    image_url: null,
    animation_url: null,
  },
  {
    id: "diamond",
    name: "Diamond",
    price: 250,
    icon: "💎",
    image_url: null,
    animation_url: null,
  },
  {
    id: "wave_crown",
    name: "Wave Crown",
    price: 500,
    icon: "👑",
    image_url: null,
    animation_url: null,
  },
  {
    id: "rocket",
    name: "Rocket",
    price: 500,
    icon: "🚀",
    image_url: null,
    animation_url: null,
  },
  {
    id: "super_star",
    name: "Super Star",
    price: 1000,
    icon: "⭐",
    image_url: null,
    animation_url: null,
  },
];

const WALLET_NUMBERS = [
  "01284306120",
  "01144210918",
];

const CORS_HEADERS_BASE = {
  "Access-Control-Allow-Headers":
    "Content-Type, Authorization",
  "Access-Control-Allow-Methods":
    "GET, POST, PATCH, DELETE, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function corsHeaders(env: Env): HeadersInit {
  return {
    ...CORS_HEADERS_BASE,
    "Access-Control-Allow-Origin":
      env.CORS_ORIGIN || "*",
  };
}

function json(
  data: unknown,
  status = 200,
  env?: Env
): Response {
  const headers = new Headers(
    corsHeaders(env || ({} as Env))
  );

  return new Response(
    JSON.stringify(data),
    {
      status,
      headers,
    }
  );
}

function now(): string {
  return new Date().toISOString();
}

function id(prefix: string): string {
  return `${prefix}_${crypto.randomUUID()}`;
}

function randomToken(): string {
  return `${crypto.randomUUID()}${crypto.randomUUID()}`
    .replaceAll("-", "");
}

function getSessionDays(env: Env): number {
  const value = Number(env.SESSION_DAYS || "30");

  if (!Number.isFinite(value) || value <= 0) {
    return 30;
  }

  return Math.min(Math.floor(value), 365);
}

async function readJson(
  request: Request
): Promise<Record<string, any>> {
  try {
    const body = await request.json();

    if (
      body &&
      typeof body === "object" &&
      !Array.isArray(body)
    ) {
      return body as Record<string, any>;
    }

    return {};
  } catch {
    return {};
  }
}

function cleanString(
  value: unknown,
  maxLength = 500
): string {
  if (typeof value !== "string") {
    return "";
  }

  return value.trim().slice(0, maxLength);
}

function getBearerToken(
  request: Request
): string | null {
  const header =
    request.headers.get("Authorization") || "";

  if (!header.toLowerCase().startsWith("bearer ")) {
    return null;
  }

  const token = header
    .slice(7)
    .trim();

  return token || null;
}

async function getUser(
  request: Request,
  env: Env
): Promise<any | null> {
  const token = getBearerToken(request);

  if (!token) {
    return null;
  }

  const result = await env.DB.prepare(`
    SELECT
      u.*
    FROM sessions s
    INNER JOIN users u
      ON u.id = s.user_id
    WHERE s.token = ?
      AND s.expires_at > ?
    LIMIT 1
  `)
    .bind(token, now())
    .first();

  return result || null;
}

function publicUser(user: any): any {
  return {
    id: user.id,
    username: user.username,
    displayName: user.display_name,
    avatar: user.avatar_url || null,
    bio: user.bio || null,
    coins: Number(user.coins || 0),
    followers: Number(user.followers || 0),
    following: Number(user.following || 0),
    verified: Boolean(Number(user.verified || 0)),
  };
}

function publicVideo(row: any): any {
  return {
    id: row.id,
    userId: row.user_id,
    username: row.username || "",
    displayName: row.display_name || "",
    avatar: row.avatar_url || null,
    videoUrl: row.video_url || "",
    thumbnailUrl: row.thumbnail_url || null,
    caption: row.caption || "",
    musicName: row.music_name || null,
    likes: Number(row.likes || 0),
    comments: Number(row.comments || 0),
    shares: Number(row.shares || 0),
    views: Number(row.views || 0),
    liked: Boolean(Number(row.liked || 0)),
    createdAt: row.created_at || null,
  };
}

function publicLive(row: any): any {
  return {
    id: row.id,
    userId: row.user_id,
    username: row.username || "",
    displayName: row.display_name || "",
    avatar: row.avatar_url || null,
    title: row.title || "",
    streamUrl: row.stream_url || null,
    playbackUrl: row.playback_url || null,
    rtmpsUrl: row.rtmps_url || null,
    streamKey: row.stream_key || null,
    viewerCount: Number(row.viewer_count || 0),
    likes: Number(row.likes || 0),
    status: row.status || "active",
    startedAt: row.started_at || null,
  };
}

async function ensureGifts(
  env: Env
): Promise<void> {
  const existing = await env.DB
    .prepare("SELECT COUNT(*) AS count FROM gifts")
    .first<{ count: number }>();

  if (Number(existing?.count || 0) > 0) {
    return;
  }

  const timestamp = now();

  for (const gift of DEFAULT_GIFTS) {
    await env.DB.prepare(`
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
      VALUES (?, ?, ?, ?, ?, ?, 1, ?)
    `)
      .bind(
        gift.id,
        gift.name,
        gift.price,
        gift.icon,
        gift.image_url,
        gift.animation_url,
        timestamp
      )
      .run();
  }
}

async function requireUser(
  request: Request,
  env: Env
): Promise<
  | { user: any }
  | { response: Response }
> {
  const user = await getUser(request, env);

  if (!user) {
    return {
      response: json(
        {
          success: false,
          message: "Authentication required",
        },
        401,
        env
      ),
    };
  }

  return { user };
}

async function createSession(
  env: Env
): Promise<any> {
  const timestamp = now();

  const username =
    `wave_${crypto.randomUUID().slice(0, 8)}`;

  const userId = id("usr");
  const token = randomToken();

  const expires =
    new Date(
      Date.now() +
      getSessionDays(env) *
      24 *
      60 *
      60 *
      1000
    ).toISOString();

  await env.DB.prepare(`
    INSERT INTO users
    (
      id,
      username,
      display_name,
      avatar_url,
      bio,
      coins,
      followers,
      following,
      verified,
      created_at,
      updated_at
    )
    VALUES (?, ?, ?, ?, ?, 0, 0, 0, 0, ?, ?)
  `)
    .bind(
      userId,
      username,
      "Wave User",
      null,
      "",
      timestamp,
      timestamp
    )
    .run();

  await env.DB.prepare(`
    INSERT INTO sessions
    (
      token,
      user_id,
      expires_at,
      created_at
    )
    VALUES (?, ?, ?, ?)
  `)
    .bind(
      token,
      userId,
      expires,
      timestamp
    )
    .run();

  return {
    token,
    user: {
      id: userId,
      username,
      displayName: "Wave User",
      avatar: null,
      bio: "",
      coins: 0,
      followers: 0,
      following: 0,
      verified: false,
    },
    expiresAt: expires,
  };
}

async function handleHealth(
  env: Env
): Promise<Response> {
  let database = "ok";

  try {
    await env.DB
      .prepare("SELECT 1")
      .first();
  } catch {
    database = "error";
  }

  return json(
    {
      success: database === "ok",
      message: "Wave Server is working!",
      environment:
        env.ENVIRONMENT || "production",
      database,
      time: now(),
    },
    database === "ok" ? 200 : 503,
    env
  );
}

async function handleSession(
  env: Env
): Promise<Response> {
  try {
    const session =
      await createSession(env);

    return json(
      {
        success: true,
        ...session,
      },
      201,
      env
    );
  } catch (error) {
    return json(
      {
        success: false,
        message: "Unable to create session",
        error:
          error instanceof Error
            ? error.message
            : String(error),
      },
      500,
      env
    );
  }
}

async function handleMe(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  return json(
    {
      success: true,
      user: publicUser(auth.user),
      wallet: {
        coins: Number(auth.user.coins || 0),
      },
    },
    200,
    env
  );
}

async function handleFeed(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const url = new URL(request.url);

  let limit =
    Number(url.searchParams.get("limit") || "20");

  let cursor =
    Number(url.searchParams.get("cursor") || "0");

  if (!Number.isFinite(limit)) {
    limit = 20;
  }

  if (!Number.isFinite(cursor)) {
    cursor = 0;
  }

  limit = Math.max(
    1,
    Math.min(Math.floor(limit), 50)
  );

  cursor = Math.max(
    0,
    Math.floor(cursor)
  );

  const rows =
    await env.DB.prepare(`
      SELECT
        v.*,
        u.username,
        u.display_name,
        u.avatar_url,
        CASE
          WHEN l.id IS NULL THEN 0
          ELSE 1
        END AS liked
      FROM videos v
      INNER JOIN users u
        ON u.id = v.user_id
      LEFT JOIN likes l
        ON l.video_id = v.id
        AND l.user_id = ?
      ORDER BY v.created_at DESC
      LIMIT ? OFFSET ?
    `)
      .bind(
        auth.user.id,
        limit,
        cursor
      )
      .all();

  return json(
    {
      success: true,
      items: (rows.results || [])
        .map(publicVideo),
      cursor: cursor + limit,
      hasMore:
        (rows.results || []).length === limit,
    },
    200,
    env
  );
}

async function handleCreateVideo(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const body =
    await readJson(request);

  const videoUrl =
    cleanString(
      body.url || body.videoUrl,
      2000
    );

  const caption =
    cleanString(body.caption, 1000);

  const thumbnailUrl =
    cleanString(
      body.thumbnailUrl,
      2000
    ) || null;

  const streamId =
    cleanString(
      body.streamId,
      300
    ) || null;

  const musicName =
    cleanString(
      body.musicName,
      200
    ) || null;

  if (!videoUrl && !streamId) {
    return json(
      {
        success: false,
        message:
          "Video URL or stream ID is required",
      },
      400,
      env
    );
  }

  const videoId = id("vid");
  const timestamp = now();

  await env.DB.prepare(`
    INSERT INTO videos
    (
      id,
      user_id,
      video_url,
      thumbnail_url,
      stream_id,
      caption,
      music_name,
      likes,
      comments,
      shares,
      views,
      created_at
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, ?)
  `)
    .bind(
      videoId,
      auth.user.id,
      videoUrl || "",
      thumbnailUrl,
      streamId,
      caption,
      musicName,
      timestamp
    )
    .run();

  return json(
    {
      success: true,
      id: videoId,
      message: "Video created",
    },
    201,
    env
  );
}

async function handleLikeVideo(
  request: Request,
  env: Env,
  videoId: string,
  unlike = false
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const video =
    await env.DB.prepare(
      "SELECT id FROM videos WHERE id = ?"
    )
      .bind(videoId)
      .first();

  if (!video) {
    return json(
      {
        success: false,
        message: "Video not found",
      },
      404,
      env
    );
  }

  if (unlike) {
    const result =
      await env.DB.prepare(`
        DELETE FROM likes
        WHERE user_id = ?
          AND video_id = ?
      `)
        .bind(
          auth.user.id,
          videoId
        )
        .run();

    if (result.meta.changes > 0) {
      await env.DB.prepare(`
        UPDATE videos
        SET likes =
          CASE
            WHEN likes > 0 THEN likes - 1
            ELSE 0
          END
        WHERE id = ?
      `)
        .bind(videoId)
        .run();
    }
  } else {
    const result =
      await env.DB.prepare(`
        INSERT OR IGNORE INTO likes
        (
          id,
          user_id,
          video_id,
          created_at
        )
        VALUES (?, ?, ?, ?)
      `)
        .bind(
          id("like"),
          auth.user.id,
          videoId,
          now()
        )
        .run();

    if (result.meta.changes > 0) {
      await env.DB.prepare(`
        UPDATE videos
        SET likes = likes + 1
        WHERE id = ?
      `)
        .bind(videoId)
        .run();
    }
  }

  const updated =
    await env.DB.prepare(
      "SELECT likes FROM videos WHERE id = ?"
    )
      .bind(videoId)
      .first<{ likes: number }>();

  return json(
    {
      success: true,
      liked: !unlike,
      likes: Number(updated?.likes || 0),
    },
    200,
    env
  );
}

async function handleComments(
  request: Request,
  env: Env,
  videoId: string
): Promise<Response> {
  const method = request.method;

  if (method === "GET") {
    const rows =
      await env.DB.prepare(`
        SELECT
          c.*,
          u.username,
          u.display_name,
          u.avatar_url
        FROM comments c
        INNER JOIN users u
          ON u.id = c.user_id
        WHERE c.video_id = ?
        ORDER BY c.created_at DESC
        LIMIT 50
      `)
        .bind(videoId)
        .all();

    return json(
      {
        success: true,
        items: (rows.results || []).map(
          (row: any) => ({
            id: row.id,
            videoId: row.video_id,
            userId: row.user_id,
            username: row.username || "",
            displayName:
              row.display_name || "",
            avatar:
              row.avatar_url || null,
            text: row.text || "",
            createdAt:
              row.created_at || null,
          })
        ),
      },
      200,
      env
    );
  }

  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const body =
    await readJson(request);

  const textValue =
    cleanString(body.text, 1000);

  if (!textValue) {
    return json(
      {
        success: false,
        message: "Comment text is required",
      },
      400,
      env
    );
  }

  const video =
    await env.DB.prepare(
      "SELECT id FROM videos WHERE id = ?"
    )
      .bind(videoId)
      .first();

  if (!video) {
    return json(
      {
        success: false,
        message: "Video not found",
      },
      404,
      env
    );
  }

  const commentId = id("com");
  const timestamp = now();

  await env.DB.prepare(`
    INSERT INTO comments
    (
      id,
      video_id,
      user_id,
      text,
      created_at
    )
    VALUES (?, ?, ?, ?, ?)
  `)
    .bind(
      commentId,
      videoId,
      auth.user.id,
      textValue,
      timestamp
    )
    .run();

  await env.DB.prepare(`
    UPDATE videos
    SET comments = comments + 1
    WHERE id = ?
  `)
    .bind(videoId)
    .run();

  return json(
    {
      success: true,
      id: commentId,
      message: "Comment added",
    },
    201,
    env
  );
}

async function handleFollow(
  request: Request,
  env: Env,
  target: string,
  unfollow = false
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const targetUser =
    await env.DB.prepare(`
      SELECT *
      FROM users
      WHERE id = ?
         OR username = ?
      LIMIT 1
    `)
      .bind(target, target)
      .first();

  if (!targetUser) {
    return json(
      {
        success: false,
        message: "User not found",
      },
      404,
      env
    );
  }

  if (targetUser.id === auth.user.id) {
    return json(
      {
        success: false,
        message: "You cannot follow yourself",
      },
      400,
      env
    );
  }

  if (unfollow) {
    const result =
      await env.DB.prepare(`
        DELETE FROM follows
        WHERE follower_id = ?
          AND following_id = ?
      `)
        .bind(
          auth.user.id,
          targetUser.id
        )
        .run();

    if (result.meta.changes > 0) {
      await env.DB.batch([
        env.DB.prepare(`
          UPDATE users
          SET following =
            CASE
              WHEN following > 0
              THEN following - 1
              ELSE 0
            END
          WHERE id = ?
        `).bind(auth.user.id),

        env.DB.prepare(`
          UPDATE users
          SET followers =
            CASE
              WHEN followers > 0
              THEN followers - 1
              ELSE 0
            END
          WHERE id = ?
        `).bind(targetUser.id),
      ]);
    }
  } else {
    const result =
      await env.DB.prepare(`
        INSERT OR IGNORE INTO follows
        (
          follower_id,
          following_id,
          created_at
        )
        VALUES (?, ?, ?)
      `)
        .bind(
          auth.user.id,
          targetUser.id,
          now()
        )
        .run();

    if (result.meta.changes > 0) {
      await env.DB.batch([
        env.DB.prepare(`
          UPDATE users
          SET following = following + 1
          WHERE id = ?
        `).bind(auth.user.id),

        env.DB.prepare(`
          UPDATE users
          SET followers = followers + 1
          WHERE id = ?
        `).bind(targetUser.id),
      ]);
    }
  }

  return json(
    {
      success: true,
      following: !unfollow,
    },
    200,
    env
  );
}

async function handleProfile(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const body =
    await readJson(request);

  const displayName =
    cleanString(
      body.displayName,
      100
    );

  const bio =
    cleanString(
      body.bio,
      500
    );

  const avatarUrl =
    cleanString(
      body.avatarUrl || body.avatar,
      2000
    );

  await env.DB.prepare(`
    UPDATE users
    SET
      display_name =
        CASE
          WHEN ? <> '' THEN ?
          ELSE display_name
        END,
      bio =
        CASE
          WHEN ? <> '' THEN ?
          ELSE bio
        END,
      avatar_url =
        CASE
          WHEN ? <> '' THEN ?
          ELSE avatar_url
        END,
      updated_at = ?
    WHERE id = ?
  `)
    .bind(
      displayName,
      displayName,
      bio,
      bio,
      avatarUrl,
      avatarUrl,
      now(),
      auth.user.id
    )
    .run();

  const updated =
    await env.DB.prepare(
      "SELECT * FROM users WHERE id = ?"
    )
      .bind(auth.user.id)
      .first();

  return json(
    {
      success: true,
      user: publicUser(updated),
    },
    200,
    env
  );
}

async function handleGifts(
  env: Env
): Promise<Response> {
  await ensureGifts(env);

  const rows =
    await env.DB.prepare(`
      SELECT
        id,
        name,
        price,
        icon,
        image_url,
        animation_url,
        enabled
      FROM gifts
      WHERE enabled = 1
      ORDER BY price ASC
    `)
      .all();

  return json(
    {
      success: true,
      items: (rows.results || []).map(
        (row: any) => ({
          id: row.id,
          name: row.name,
          price: Number(row.price || 0),
          icon: row.icon || "",
          imageUrl:
            row.image_url || null,
          animationUrl:
            row.animation_url || null,
          enabled:
            Boolean(Number(row.enabled || 0)),
        })
      ),
    },
    200,
    env
  );
}

async function handleCreateLive(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const body =
    await readJson(request);

  const title =
    cleanString(
      body.title,
      200
    ) || "Wave Live";

  const liveId = id("live");
  const timestamp = now();

  /*
   * Cloudflare Stream / RTMPS credentials
   * are intentionally not fabricated here.
   *
   * Real stream credentials should be created
   * by the configured video provider and stored
   * in the corresponding columns.
   */

  await env.DB.prepare(`
    INSERT INTO live_streams
    (
      id,
      user_id,
      title,
      stream_url,
      playback_url,
      rtmps_url,
      stream_key,
      viewer_count,
      likes,
      status,
      started_at,
      created_at
    )
    VALUES (?, ?, ?, NULL, NULL, NULL, NULL, 0, 0, 'active', ?, ?)
  `)
    .bind(
      liveId,
      auth.user.id,
      title,
      timestamp,
      timestamp
    )
    .run();

  const live =
    await env.DB.prepare(`
      SELECT
        l.*,
        u.username,
        u.display_name,
        u.avatar_url
      FROM live_streams l
      INNER JOIN users u
        ON u.id = l.user_id
      WHERE l.id = ?
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  return json(
    {
      success: true,
      message:
        "Live session created. Stream credentials require video-provider configuration.",
      live: publicLive(live),
    },
    201,
    env
  );
}

async function handleGetLive(
  env: Env,
  liveId: string
): Promise<Response> {
  const live =
    await env.DB.prepare(`
      SELECT
        l.*,
        u.username,
        u.display_name,
        u.avatar_url
      FROM live_streams l
      INNER JOIN users u
        ON u.id = l.user_id
      WHERE l.id = ?
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  if (!live) {
    return json(
      {
        success: false,
        message: "Live not found",
      },
      404,
      env
    );
  }

  return json(
    {
      success: true,
      live: publicLive(live),
    },
    200,
    env
  );
}

async function handleUpdateLive(
  request: Request,
  env: Env,
  liveId: string
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const live =
    await env.DB.prepare(`
      SELECT *
      FROM live_streams
      WHERE id = ?
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  if (!live) {
    return json(
      {
        success: false,
        message: "Live not found",
      },
      404,
      env
    );
  }

  if (live.user_id !== auth.user.id) {
    return json(
      {
        success: false,
        message: "Not allowed",
      },
      403,
      env
    );
  }

  const body =
    await readJson(request);

  const status =
    cleanString(
      body.status,
      30
    );

  const title =
    cleanString(
      body.title,
      200
    );

  const allowedStatuses = [
    "active",
    "ended",
    "paused",
  ];

  const nextStatus =
    allowedStatuses.includes(status)
      ? status
      : String(live.status || "active");

  await env.DB.prepare(`
    UPDATE live_streams
    SET
      status = ?,
      title =
        CASE
          WHEN ? <> '' THEN ?
          ELSE title
        END
    WHERE id = ?
  `)
    .bind(
      nextStatus,
      title,
      title,
      liveId
    )
    .run();

  return handleGetLive(
    env,
    liveId
  );
}

async function handleSendGift(
  request: Request,
  env: Env,
  liveId: string
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  await ensureGifts(env);

  const body =
    await readJson(request);

  const giftId =
    cleanString(
      body.giftId,
      100
    );

  const quantity =
    Math.max(
      1,
      Math.min(
        Number(body.quantity || 1),
        100
      )
    );

  if (!giftId) {
    return json(
      {
        success: false,
        message: "Gift ID is required",
      },
      400,
      env
    );
  }

  const live =
    await env.DB.prepare(`
      SELECT *
      FROM live_streams
      WHERE id = ?
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  if (!live) {
    return json(
      {
        success: false,
        message: "Live not found",
      },
      404,
      env
    );
  }

  const gift =
    await env.DB.prepare(`
      SELECT *
      FROM gifts
      WHERE id = ?
        AND enabled = 1
      LIMIT 1
    `)
      .bind(giftId)
      .first();

  if (!gift) {
    return json(
      {
        success: false,
        message: "Gift not found",
      },
      404,
      env
    );
  }

  const price =
    Number(gift.price || 0);

  if (price <= 0) {
    return json(
      {
        success: false,
        message: "Invalid gift price",
      },
      400,
      env
    );
  }

  const totalCoins =
    price * quantity;

  const receiverUserId =
    cleanString(
      body.receiverUserId,
      200
    ) || live.user_id;

  if (!receiverUserId) {
    return json(
      {
        success: false,
        message: "Receiver not found",
      },
      400,
      env
    );
  }

  const receiver =
    await env.DB.prepare(
      "SELECT id FROM users WHERE id = ?"
    )
      .bind(receiverUserId)
      .first();

  if (!receiver) {
    return json(
      {
        success: false,
        message: "Receiver user not found",
      },
      404,
      env
    );
  }

  /*
   * Atomic coin deduction.
   * This prevents spending more coins than the
   * current balance when multiple requests arrive.
   */
  const debit =
    await env.DB.prepare(`
      UPDATE users
      SET coins = coins - ?
      WHERE id = ?
        AND coins >= ?
    `)
      .bind(
        totalCoins,
        auth.user.id,
        totalCoins
      )
      .run();

  if (debit.meta.changes !== 1) {
    const current =
      await env.DB.prepare(
        "SELECT coins FROM users WHERE id = ?"
      )
        .bind(auth.user.id)
        .first<{ coins: number }>();

    return json(
      {
        success: false,
        message: "Insufficient coins",
        remainingCoins:
          Number(current?.coins || 0),
      },
      400,
      env
    );
  }

  /*
   * Receiver gets the coins.
   * For production revenue accounting, this can later
   * be extended with creator balance / settlement rules.
   */
  await env.DB.prepare(`
    UPDATE users
    SET coins = coins + ?
    WHERE id = ?
  `)
    .bind(
      totalCoins,
      receiverUserId
    )
    .run();

  const transactionId =
    id("gift_tx");

  await env.DB.prepare(`
    INSERT INTO gift_transactions
    (
      id,
      live_id,
      sender_user_id,
      receiver_user_id,
      gift_id,
      quantity,
      total_coins,
      created_at
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `)
    .bind(
      transactionId,
      liveId,
      auth.user.id,
      receiverUserId,
      giftId,
      quantity,
      totalCoins,
      now()
    )
    .run();

  const updated =
    await env.DB.prepare(
      "SELECT coins FROM users WHERE id = ?"
    )
      .bind(auth.user.id)
      .first<{ coins: number }>();

  return json(
    {
      success: true,
      message: "Gift sent",
      transactionId,
      remainingCoins:
        Number(updated?.coins || 0),
      gift: {
        id: gift.id,
        name: gift.name,
        price,
        icon: gift.icon || "",
        quantity,
        totalCoins,
      },
    },
    200,
    env
  );
}

async function handleDirectUpload(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  /*
   * A real direct-upload URL must come from the
   * configured video storage/provider.
   *
   * We intentionally do not generate a fake upload URL.
   */
  return json(
    {
      success: false,
      message:
        "Direct upload is not configured yet. Connect the production video storage/provider first.",
    },
    501,
    env
  );
}

async function handleWallet(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  return json(
    {
      success: true,
      coins:
        Number(auth.user.coins || 0),
      walletNumbers:
        WALLET_NUMBERS,
      paymentMethods: [
        "wallet",
      ],
    },
    200,
    env
  );
}

async function handleWalletDeposit(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const body =
    await readJson(request);

  const amount =
    Number(body.amount || 0);

  const walletNumber =
    cleanString(
      body.walletNumber,
      30
    );

  const reference =
    cleanString(
      body.transactionReference ||
      body.reference,
      200
    ) || null;

  if (
    !Number.isFinite(amount) ||
    amount <= 0
  ) {
    return json(
      {
        success: false,
        message: "Invalid deposit amount",
      },
      400,
      env
    );
  }

  if (
    !WALLET_NUMBERS.includes(
      walletNumber
    )
  ) {
    return json(
      {
        success: false,
        message:
          "Unsupported wallet number",
      },
      400,
      env
    );
  }

  /*
   * Coins are NOT added here.
   * The deposit stays pending until payment
   * verification/approval is implemented.
   */
  const depositId =
    id("dep");

  const timestamp = now();

  await env.DB.prepare(`
    INSERT INTO wallet_deposits
    (
      id,
      user_id,
      amount,
      wallet_number,
      transaction_reference,
      coins,
      status,
      created_at,
      updated_at
    )
    VALUES (?, ?, ?, ?, ?, 0, 'pending', ?, ?)
  `)
    .bind(
      depositId,
      auth.user.id,
      Math.floor(amount),
      walletNumber,
      reference,
      timestamp,
      timestamp
    )
    .run();

  return json(
    {
      success: true,
      pending: true,
      depositId,
      coins:
        Number(auth.user.coins || 0),
      message:
        "Deposit submitted and is waiting for verification.",
    },
    201,
    env
  );
}

async function handleWalletDeposits(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const rows =
    await env.DB.prepare(`
      SELECT
        id,
        amount,
        wallet_number,
        transaction_reference,
        coins,
        status,
        created_at,
        updated_at
      FROM wallet_deposits
      WHERE user_id = ?
      ORDER BY created_at DESC
      LIMIT 50
    `)
      .bind(auth.user.id)
      .all();

  return json(
    {
      success: true,
      items: (rows.results || []).map(
        (row: any) => ({
          id: row.id,
          amount:
            Number(row.amount || 0),
          walletNumber:
            row.wallet_number || "",
          transactionReference:
            row.transaction_reference ||
            null,
          coins:
            Number(row.coins || 0),
          status:
            row.status || "pending",
          createdAt:
            row.created_at || null,
          updatedAt:
            row.updated_at || null,
        })
      ),
    },
    200,
    env
  );
}

async function handleReport(
  request: Request,
  env: Env
): Promise<Response> {
  const auth =
    await requireUser(request, env);

  if ("response" in auth) {
    return auth.response;
  }

  const body =
    await readJson(request);

  const targetType =
    cleanString(
      body.targetType,
      50
    );

  const targetId =
    cleanString(
      body.targetId,
      200
    );

  const reason =
    cleanString(
      body.reason,
      500
    );

  if (
    !targetType ||
    !targetId ||
    !reason
  ) {
    return json(
      {
        success: false,
        message:
          "targetType, targetId and reason are required",
      },
      400,
      env
    );
  }

  const reportId =
    id("report");

  await env.DB.prepare(`
    INSERT INTO reports
    (
      id,
      reporter_user_id,
      target_type,
      target_id,
      reason,
      status,
      created_at
    )
    VALUES (?, ?, ?, ?, ?, 'pending', ?)
  `)
    .bind(
      reportId,
      auth.user.id,
      targetType,
      targetId,
      reason,
      now()
    )
    .run();

  return json(
    {
      success: true,
      reportId,
      message:
        "Report submitted",
    },
    201,
    env
  );
}

export default {
  async fetch(
    request: Request,
    env: Env
  ): Promise<Response> {
    try {
      if (request.method === "OPTIONS") {
        return new Response(
          null,
          {
            status: 204,
            headers:
              corsHeaders(env),
          }
        );
      }

      const url =
        new URL(request.url);

      const path =
        url.pathname.replace(
          /\/+$/,
          ""
        ) || "/";

      const method =
        request.method.toUpperCase();

      /*
       * Health
       */
      if (
        path === "/health" ||
        path === "/"
      ) {
        return handleHealth(env);
      }

      /*
       * Anonymous session
       */
      if (
        path === "/api/session" &&
        method === "POST"
      ) {
        return handleSession(env);
      }

      /*
       * Current user
       */
      if (
        path === "/api/me" &&
        method === "GET"
      ) {
        return handleMe(
          request,
          env
        );
      }

      /*
       * Feed
       */
      if (
        path === "/api/feed" &&
        method === "GET"
      ) {
        return handleFeed(
          request,
          env
        );
      }

      /*
       * Videos
       */
      if (
        path === "/api/videos" &&
        method === "POST"
      ) {
        return handleCreateVideo(
          request,
          env
        );
      }

      /*
       * Likes
       */
      const likeMatch =
        path.match(
          /^\/api\/videos\/([^/]+)\/like$/
        );

      if (
        likeMatch &&
        method === "POST"
      ) {
        return handleLikeVideo(
          request,
          env,
          decodeURIComponent(
            likeMatch[1]
          ),
          false
        );
      }

      const unlikeMatch =
        path.match(
          /^\/api\/videos\/([^/]+)\/like$/
        );

      if (
        unlikeMatch &&
        method === "DELETE"
      ) {
        return handleLikeVideo(
          request,
          env,
          decodeURIComponent(
            unlikeMatch[1]
          ),
          true
        );
      }

      /*
       * Comments
       */
      const commentsMatch =
        path.match(
          /^\/api\/videos\/([^/]+)\/comments$/
        );

      if (
        commentsMatch &&
        (method === "GET" ||
          method === "POST")
      ) {
        return handleComments(
          request,
          env,
          decodeURIComponent(
            commentsMatch[1]
          )
        );
      }

      /*
       * Follow / unfollow
       */
      const followMatch =
        path.match(
          /^\/api\/users\/([^/]+)\/follow$/
        );

      if (
        followMatch &&
        method === "POST"
      ) {
        return handleFollow(
          request,
          env,
          decodeURIComponent(
            followMatch[1]
          ),
          false
        );
      }

      if (
        followMatch &&
        method === "DELETE"
      ) {
        return handleFollow(
          request,
          env,
          decodeURIComponent(
            followMatch[1]
          ),
          true
        );
      }

      /*
       * Profile
       */
      if (
        path === "/api/profile" &&
        method === "PATCH"
      ) {
        return handleProfile(
          request,
          env
        );
      }

      /*
       * Gifts
       */
      if (
        path === "/api/gifts" &&
        method === "GET"
      ) {
        return handleGifts(env);
      }

      /*
       * Create Live
       */
      if (
        path === "/api/live/create" &&
        method === "POST"
      ) {
        return handleCreateLive(
          request,
          env
        );
      }

      /*
       * Get / update Live
       */
      const liveMatch =
        path.match(
          /^\/api\/live\/([^/]+)$/
        );

      if (
        liveMatch &&
        (method === "GET" ||
          method === "PATCH")
      ) {
        const liveId =
          decodeURIComponent(
            liveMatch[1]
          );

        if (method === "GET") {
          return handleGetLive(
            env,
            liveId
          );
        }

        return handleUpdateLive(
          request,
          env,
          liveId
        );
      }

      /*
       * Live gifts
       */
      const liveGiftMatch =
        path.match(
          /^\/api\/live\/([^/]+)\/gifts$/
        );

      if (
        liveGiftMatch &&
        method === "POST"
      ) {
        return handleSendGift(
          request,
          env,
          decodeURIComponent(
            liveGiftMatch[1]
          )
        );
      }

      /*
       * Direct upload
       */
      if (
        path === "/api/upload/direct" &&
        method === "POST"
      ) {
        return handleDirectUpload(
          request,
          env
        );
      }

      /*
       * Wallet balance / wallet numbers
       */
      if (
        path === "/api/wallet" &&
        method === "GET"
      ) {
        return handleWallet(
          request,
          env
        );
      }

      /*
       * Wallet deposit
       */
      if (
        path === "/api/wallet/deposit" &&
        method === "POST"
      ) {
        return handleWalletDeposit(
          request,
          env
        );
      }

      /*
       * Deposit history
       */
      if (
        path === "/api/wallet/deposits" &&
        method === "GET"
      ) {
        return handleWalletDeposits(
          request,
          env
        );
      }

      /*
       * Reports
       */
      if (
        path === "/api/reports" &&
        method === "POST"
      ) {
        return handleReport(
          request,
          env
        );
      }

      return json(
        {
          success: false,
          message: "Not Found",
          path,
        },
        404,
        env
      );
    } catch (error) {
      return json(
        {
          success: false,
          message: "Internal Server Error",
          error:
            env.ENVIRONMENT === "production"
              ? undefined
              : error instanceof Error
                ? error.message
                : String(error),
        },
        500,
        env
      );
    }
  },
};
