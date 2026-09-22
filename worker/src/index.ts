export interface Env {
  DB: D1Database;
  ENVIRONMENT: string;
  SESSION_DAYS: string;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods":
    "GET, POST, PATCH, DELETE, OPTIONS",
  "Access-Control-Allow-Headers":
    "Content-Type, Authorization",
  "Content-Type":
    "application/json; charset=utf-8",
};

const WALLET_NUMBERS = [
  "01284306120",
  "01144210918",
];

const MAX_USERNAME_LENGTH = 24;
const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_LENGTH = 128;
const PBKDF2_ITERATIONS = 120000;

/* ------------------------------------------------ */
/* Types                                            */
/* ------------------------------------------------ */

type AnyObject = Record<string, any>;

interface GiftDefinition {
  id: string;
  name: string;
  price: number;
  icon: string;
  sortOrder: number;
}

const GIFT_DEFINITIONS: GiftDefinition[] = [
  {
    id: "rose",
    name: "Wave Rose",
    price: 5,
    icon: "🌹",
    sortOrder: 10,
  },
  {
    id: "heart",
    name: "Wave Heart",
    price: 10,
    icon: "💖",
    sortOrder: 20,
  },
  {
    id: "fire",
    name: "Wave Fire",
    price: 25,
    icon: "🔥",
    sortOrder: 30,
  },
  {
    id: "star",
    name: "Super Star",
    price: 50,
    icon: "🌟",
    sortOrder: 40,
  },
  {
    id: "diamond",
    name: "Wave Diamond",
    price: 100,
    icon: "💎",
    sortOrder: 50,
  },
  {
    id: "rocket",
    name: "Wave Rocket",
    price: 250,
    icon: "🚀",
    sortOrder: 60,
  },
  {
    id: "crown",
    name: "Wave Crown",
    price: 500,
    icon: "👑",
    sortOrder: 70,
  },
  {
    id: "royal_crown",
    name: "Royal Wave Crown",
    price: 1000,
    icon: "👑",
    sortOrder: 80,
  },
  {
    id: "lion",
    name: "Golden Lion",
    price: 1500,
    icon: "🦁",
    sortOrder: 90,
  },
];

/* ------------------------------------------------ */
/* Response helpers                                 */
/* ------------------------------------------------ */

function json(
  data: unknown,
  status = 200
): Response {
  return new Response(
    JSON.stringify(data),
    {
      status,
      headers: CORS_HEADERS,
    }
  );
}

function ok(
  data: AnyObject = {}
): Response {
  return json({
    success: true,
    ...data,
  });
}

function fail(
  message: string,
  status = 400,
  extra: AnyObject = {}
): Response {
  return json(
    {
      success: false,
      message,
      ...extra,
    },
    status
  );
}

/* ------------------------------------------------ */
/* General helpers                                  */
/* ------------------------------------------------ */

function makeId(
  prefix: string
): string {
  return (
    prefix +
    "_" +
    crypto
      .randomUUID()
      .replaceAll("-", "")
  );
}

function cleanString(
  value: unknown,
  max = 500
): string {
  return String(value ?? "")
    .trim()
    .slice(0, max);
}

function normalizeUsername(
  value: unknown
): string {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .slice(0, MAX_USERNAME_LENGTH);
}

function getToken(
  request: Request
): string {
  const header =
    request.headers.get(
      "Authorization"
    ) || "";

  if (
    !header.startsWith("Bearer ")
  ) {
    return "";
  }

  return header
    .slice(7)
    .trim();
}

async function readJson(
  request: Request
): Promise<AnyObject> {
  try {
    const value =
      await request.json();

    if (
      value &&
      typeof value === "object"
    ) {
      return value as AnyObject;
    }

    return {};
  } catch {
    return {};
  }
}

function integer(
  value: unknown,
  fallback = 0
): number {
  const n = Number(value);

  if (!Number.isFinite(n)) {
    return fallback;
  }

  return Math.floor(n);
}

function clamp(
  value: number,
  min: number,
  max: number
): number {
  return Math.min(
    max,
    Math.max(min, value)
  );
}

function safeLimit(
  value: unknown,
  fallback = 20
): number {
  return clamp(
    integer(value, fallback),
    1,
    50
  );
}

/* ------------------------------------------------ */
/* Password hashing                                 */
/* ------------------------------------------------ */

function bytesToBase64(
  bytes: Uint8Array
): string {
  let binary = "";

  for (const byte of bytes) {
    binary += String.fromCharCode(
      byte
    );
  }

  return btoa(binary);
}

function base64ToBytes(
  value: string
): Uint8Array {
  const binary =
    atob(value);

  const bytes =
    new Uint8Array(
      binary.length
    );

  for (
    let i = 0;
    i < binary.length;
    i++
  ) {
    bytes[i] =
      binary.charCodeAt(i);
  }

  return bytes;
}

function constantTimeEqual(
  a: Uint8Array,
  b: Uint8Array
): boolean {
  if (a.length !== b.length) {
    return false;
  }

  let result = 0;

  for (
    let i = 0;
    i < a.length;
    i++
  ) {
    result |=
      a[i] ^ b[i];
  }

  return result === 0;
}

async function hashPassword(
  password: string
): Promise<string> {
  const salt =
    crypto.getRandomValues(
      new Uint8Array(16)
    );

  const keyMaterial =
    await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(
        password
      ),
      "PBKDF2",
      false,
      ["deriveBits"]
    );

  const derived =
    await crypto.subtle.deriveBits(
      {
        name: "PBKDF2",
        salt,
        iterations:
          PBKDF2_ITERATIONS,
        hash: "SHA-256",
      },
      keyMaterial,
      256
    );

  return [
    "pbkdf2",
    String(PBKDF2_ITERATIONS),
    bytesToBase64(salt),
    bytesToBase64(
      new Uint8Array(
        derived
      )
    ),
  ].join("$");
}

async function verifyPassword(
  password: string,
  stored: string
): Promise<boolean> {
  const parts =
    stored.split("$");

  if (
    parts.length !== 4 ||
    parts[0] !== "pbkdf2"
  ) {
    return false;
  }

  const iterations =
    Number(parts[1]);

  if (
    !Number.isInteger(
      iterations
    ) ||
    iterations < 10000 ||
    iterations > 1000000
  ) {
    return false;
  }

  try {
    const salt =
      base64ToBytes(parts[2]);

    const expected =
      base64ToBytes(parts[3]);

    const keyMaterial =
      await crypto.subtle.importKey(
        "raw",
        new TextEncoder().encode(
          password
        ),
        "PBKDF2",
        false,
        ["deriveBits"]
      );

    const derived =
      await crypto.subtle.deriveBits(
        {
          name: "PBKDF2",
          salt,
          iterations,
          hash: "SHA-256",
        },
        keyMaterial,
        256
      );

    return constantTimeEqual(
      expected,
      new Uint8Array(
        derived
      )
    );
  } catch {
    return false;
  }
}

/* ------------------------------------------------ */
/* Authentication                                   */
/* ------------------------------------------------ */

async function createSession(
  env: Env,
  userId: string
): Promise<{
  token: string;
  expiresAt: string;
}> {
  const token =
    crypto.randomUUID() +
    "." +
    crypto.randomUUID();

  const days = clamp(
    integer(
      env.SESSION_DAYS,
      30
    ),
    1,
    365
  );

  const session =
    await env.DB.prepare(`
      INSERT INTO sessions (
        token,
        user_id,
        expires_at,
        created_at
      )
      VALUES (
        ?,
        ?,
        datetime(
          'now',
          '+' || ? || ' days'
        ),
        datetime('now')
      )
      RETURNING
        token,
        expires_at
    `)
      .bind(
        token,
        userId,
        days
      )
      .first<{
        token: string;
        expires_at: string;
      }>();

  if (!session) {
    throw new Error(
      "Could not create session"
    );
  }

  return {
    token: session.token,
    expiresAt:
      session.expires_at,
  };
}

async function getCurrentUser(
  env: Env,
  request: Request
): Promise<any | null> {
  const token =
    getToken(request);

  if (!token) {
    return null;
  }

  return await env.DB.prepare(`
    SELECT
      u.id,
      u.username,
      u.display_name,
      u.avatar_url,
      u.bio,
      u.coins,
      u.followers,
      u.following,
      u.verified,
      u.created_at,
      u.updated_at
    FROM sessions s
    INNER JOIN users u
      ON u.id = s.user_id
    WHERE
      s.token = ?
      AND s.expires_at > datetime('now')
    LIMIT 1
  `)
    .bind(token)
    .first();
}

async function requireUser(
  env: Env,
  request: Request
): Promise<any> {
  const user =
    await getCurrentUser(
      env,
      request
    );

  if (!user) {
    throw new ApiError(
      "Unauthorized",
      401
    );
  }

  return user;
}

function publicUser(
  user: any
): AnyObject {
  return {
    id: String(user.id),
    username:
      String(user.username || ""),
    displayName:
      String(
        user.display_name ||
        user.username ||
        ""
      ),
    avatar:
      user.avatar_url ||
      null,
    bio:
      user.bio || "",
    coins:
      Number(user.coins || 0),
    followers:
      Number(
        user.followers || 0
      ),
    following:
      Number(
        user.following || 0
      ),
    verified:
      Number(
        user.verified || 0
      ) === 1,
    createdAt:
      user.created_at ||
      null,
  };
}

/* ------------------------------------------------ */
/* API error                                        */
/* ------------------------------------------------ */

class ApiError extends Error {
  status: number;
  extra: AnyObject;

  constructor(
    message: string,
    status = 400,
    extra: AnyObject = {}
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.extra = extra;
  }
}

/* ------------------------------------------------ */
/* Runtime database preparation                    */
/* ------------------------------------------------ */

async function prepareRuntime(
  env: Env
): Promise<void> {
  /*
   * The real schema is managed by migrations.
   * This function only makes safe seed/upsert operations.
   */

  const giftStatements =
    GIFT_DEFINITIONS.map(
      (gift) =>
        env.DB.prepare(`
          INSERT OR IGNORE INTO gifts (
            id,
            name,
            price,
            icon,
            image_url,
            animation_url,
            enabled,
            created_at
          )
          VALUES (
            ?,
            ?,
            ?,
            ?,
            NULL,
            NULL,
            1,
            datetime('now')
          )
        `).bind(
          gift.id,
          gift.name,
          gift.price,
          gift.icon
        )
    );

  /*
   * The gift transfer trigger is created once.
   *
   * The trigger makes the actual coin transfer part
   * of the same INSERT transaction as the gift record.
   */
  const trigger =
    env.DB.prepare(`
      CREATE TRIGGER IF NOT EXISTS
      wave_gift_transfer_guard
      BEFORE INSERT ON gift_transactions
      BEGIN

        SELECT RAISE(
          ABORT,
          'INVALID_GIFT'
        )
        WHERE
          NEW.quantity <= 0
          OR NEW.total_coins <= 0;

        SELECT RAISE(
          ABORT,
          'INVALID_SENDER'
        )
        WHERE NOT EXISTS (
          SELECT 1
          FROM users
          WHERE id =
            NEW.sender_user_id
        );

        SELECT RAISE(
          ABORT,
          'INVALID_RECEIVER'
        )
        WHERE NOT EXISTS (
          SELECT 1
          FROM users
          WHERE id =
            NEW.receiver_user_id
        );

        SELECT RAISE(
          ABORT,
          'INSUFFICIENT_COINS'
        )
        WHERE (
          SELECT coins
          FROM users
          WHERE id =
            NEW.sender_user_id
        ) < NEW.total_coins;

        UPDATE users
        SET coins =
          coins - NEW.total_coins
        WHERE id =
          NEW.sender_user_id;

        UPDATE users
        SET coins =
          coins + NEW.total_coins
        WHERE id =
          NEW.receiver_user_id;

      END
    `);

  await env.DB.batch([
    ...giftStatements,
    trigger,
  ]);
}

/* ------------------------------------------------ */
/* Gift helpers                                     */
/* ------------------------------------------------ */

async function getGift(
  env: Env,
  giftId: string
): Promise<any | null> {
  return await env.DB.prepare(`
    SELECT
      id,
      name,
      price,
      icon,
      image_url,
      animation_url,
      enabled
    FROM gifts
    WHERE
      id = ?
      AND enabled = 1
    LIMIT 1
  `)
    .bind(giftId)
    .first();
}

function mapGift(
  gift: any
): AnyObject {
  return {
    id:
      String(gift.id),
    name:
      String(gift.name),
    price:
      Number(gift.price || 0),
    icon:
      gift.icon || "",
    imageUrl:
      gift.image_url ||
      null,
    animationUrl:
      gift.animation_url ||
      null,
  };
}

/* ------------------------------------------------ */
/* Live helpers                                     */
/* ------------------------------------------------ */

function mapLive(
  live: any
): AnyObject {
  return {
    id:
      String(live.id),
    userId:
      String(live.user_id),
    username:
      live.username || "",
    displayName:
      live.display_name ||
      live.username ||
      "",
    avatar:
      live.avatar_url ||
      null,
    title:
      live.title || "",
    streamUrl:
      live.stream_url ||
      null,
    playbackUrl:
      live.playback_url ||
      null,
    rtmpsUrl:
      live.rtmps_url ||
      null,
    streamKey:
      live.stream_key ||
      null,
    viewerCount:
      Number(
        live.viewer_count || 0
      ),
    likes:
      Number(
        live.likes || 0
      ),
    status:
      live.status || "active",
    startedAt:
      live.started_at ||
      live.created_at ||
      null,
    createdAt:
      live.created_at ||
      null,
  };
}

/* ------------------------------------------------ */
/* User/profile helpers                             */
/* ------------------------------------------------ */

async function getUserById(
  env: Env,
  userId: string
): Promise<any | null> {
  return await env.DB.prepare(`
    SELECT *
    FROM users
    WHERE id = ?
    LIMIT 1
  `)
    .bind(userId)
    .first();
}

/* ------------------------------------------------ */
/* Health                                           */
/* ------------------------------------------------ */

async function handleHealth(
  env: Env
): Promise<Response> {
  await env.DB.prepare(
    "SELECT 1 AS ok"
  ).first();

  return ok({
    status: "ok",
    service:
      "Wave Live API",
    environment:
      env.ENVIRONMENT ||
      "production",
    database: "connected",
    version: "2.0.0",
  });
}

/* ------------------------------------------------ */
/* Register                                         */
/* ------------------------------------------------ */

async function handleRegister(
  env: Env,
  request: Request
): Promise<Response> {
  const data =
    await readJson(request);

  const username =
    normalizeUsername(
      data.username
    );

  const password =
    String(
      data.password || ""
    );

  const displayName =
    cleanString(
      data.displayName ||
        data.display_name ||
        username,
      80
    );

  if (
    !/^[a-z0-9_]{3,24}$/.test(
      username
    )
  ) {
    return fail(
      "Username must contain 3-24 lowercase letters, numbers or underscores",
      400
    );
  }

  if (
    password.length <
      MIN_PASSWORD_LENGTH ||
    password.length >
      MAX_PASSWORD_LENGTH
  ) {
    return fail(
      `Password must be between ${MIN_PASSWORD_LENGTH} and ${MAX_PASSWORD_LENGTH} characters`,
      400
    );
  }

  const existing =
    await env.DB.prepare(`
      SELECT id
      FROM users
      WHERE username = ?
      LIMIT 1
    `)
      .bind(username)
      .first();

  if (existing) {
    return fail(
      "Username already exists",
      409
    );
  }

  const userId =
    makeId("usr");

  const passwordHash =
    await hashPassword(
      password
    );

  const now =
    new Date().toISOString();

  await env.DB.batch([
    env.DB.prepare(`
      INSERT INTO users (
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
      VALUES (
        ?,
        ?,
        ?,
        NULL,
        '',
        0,
        0,
        0,
        0,
        ?,
        ?
      )
    `).bind(
      userId,
      username,
      displayName ||
        username,
      now,
      now
    ),

    env.DB.prepare(`
      INSERT INTO account_credentials (
        user_id,
        password_hash,
        created_at,
        updated_at
      )
      VALUES (
        ?,
        ?,
        ?,
        ?
      )
    `).bind(
      userId,
      passwordHash,
      now,
      now
    ),
  ]);

  const session =
    await createSession(
      env,
      userId
    );

  const user =
    await getUserById(
      env,
      userId
    );

  return ok({
    token:
      session.token,
    expiresAt:
      session.expiresAt,
    user:
      publicUser(user),
  });
}

/* ------------------------------------------------ */
/* Login                                            */
/* ------------------------------------------------ */

async function handleLogin(
  env: Env,
  request: Request
): Promise<Response> {
  const data =
    await readJson(request);

  const username =
    normalizeUsername(
      data.username
    );

  const password =
    String(
      data.password || ""
    );

  if (!username || !password) {
    return fail(
      "Username and password are required",
      400
    );
  }

  const user =
    await env.DB.prepare(`
      SELECT *
      FROM users
      WHERE username = ?
      LIMIT 1
    `)
      .bind(username)
      .first();

  if (!user) {
    return fail(
      "Invalid username or password",
      401
    );
  }

  const credentials =
    await env.DB.prepare(`
      SELECT password_hash
      FROM account_credentials
      WHERE user_id = ?
      LIMIT 1
    `)
      .bind(user.id)
      .first<{
        password_hash: string;
      }>();

  if (!credentials) {
    return fail(
      "Account credentials are not configured",
      401
    );
  }

  const valid =
    await verifyPassword(
      password,
      credentials.password_hash
    );

  if (!valid) {
    return fail(
      "Invalid username or password",
      401
    );
  }

  const session =
    await createSession(
      env,
      String(user.id)
    );

  return ok({
    token:
      session.token,
    expiresAt:
      session.expiresAt,
    user:
      publicUser(user),
  });
}

/* ------------------------------------------------ */
/* Logout                                           */
/* ------------------------------------------------ */

async function handleLogout(
  env: Env,
  request: Request
): Promise<Response> {
  const token =
    getToken(request);

  if (token) {
    await env.DB.prepare(`
      DELETE FROM sessions
      WHERE token = ?
    `)
      .bind(token)
      .run();
  }

  return ok();
}

/* ------------------------------------------------ */
/* Me                                               */
/* ------------------------------------------------ */

async function handleMe(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  return ok({
    user:
      publicUser(user),
  });
}

/* ------------------------------------------------ */
/* Feed                                             */
/* ------------------------------------------------ */

async function handleFeed(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await getCurrentUser(
      env,
      request
    );

  const url =
    new URL(request.url);

  const limit =
    safeLimit(
      url.searchParams.get(
        "limit"
      ),
      20
    );

  const offset =
    clamp(
      integer(
        url.searchParams.get(
          "offset"
        ),
        0
      ),
      0,
      1000000
    );

  const rows =
    await env.DB.prepare(`
      SELECT
        v.id,
        v.user_id,
        v.video_url,
        v.thumbnail_url,
        v.stream_id,
        v.caption,
        v.music_name,
        v.likes,
        v.comments,
        v.shares,
        v.views,
        v.created_at,
        u.username,
        u.display_name,
        u.avatar_url,
        u.verified
      FROM videos v
      INNER JOIN users u
        ON u.id = v.user_id
      ORDER BY
        v.created_at DESC
      LIMIT ?
      OFFSET ?
    `)
      .bind(
        limit,
        offset
      )
      .all();

  const items: AnyObject[] =
    [];

  for (
    const row of
      rows.results || []
  ) {
    let liked = false;

    if (user) {
      const result =
        await env.DB.prepare(`
          SELECT 1
          FROM likes
          WHERE
            user_id = ?
            AND video_id = ?
          LIMIT 1
        `)
          .bind(
            user.id,
            row.id
          )
          .first();

      liked =
        Boolean(result);
    }

    items.push({
      id:
        row.id,
      userId:
        row.user_id,
      username:
        row.username,
      displayName:
        row.display_name ||
        row.username,
      avatar:
        row.avatar_url ||
        null,
      verified:
        Number(
          row.verified || 0
        ) === 1,
      videoUrl:
        row.video_url,
      thumbnailUrl:
        row.thumbnail_url ||
        null,
      streamId:
        row.stream_id ||
        null,
      caption:
        row.caption ||
        "",
      musicName:
        row.music_name ||
        null,
      likes:
        Number(
          row.likes || 0
        ),
      comments:
        Number(
          row.comments || 0
        ),
      shares:
        Number(
          row.shares || 0
        ),
      views:
        Number(
          row.views || 0
        ),
      liked,
      createdAt:
        row.created_at,
    });
  }

  return ok({
    items,
    nextOffset:
      items.length === limit
        ? offset + limit
        : null,
  });
}

/* ------------------------------------------------ */
/* Create video                                     */
/* ------------------------------------------------ */

async function handleCreateVideo(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const data =
    await readJson(request);

  const videoUrl =
    cleanString(
      data.videoUrl ||
        data.video_url,
      2000
    );

  if (!videoUrl) {
    return fail(
      "videoUrl is required",
      400
    );
  }

  const videoId =
    makeId("vid");

  const now =
    new Date().toISOString();

  await env.DB.prepare(`
    INSERT INTO videos (
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
    VALUES (
      ?,
      ?,
      ?,
      ?,
      ?,
      ?,
      ?,
      0,
      0,
      0,
      0,
      ?
    )
  `)
    .bind(
      videoId,
      user.id,
      videoUrl,
      cleanString(
        data.thumbnailUrl ||
          data.thumbnail_url,
        2000
      ) || null,
      cleanString(
        data.streamId ||
          data.stream_id,
        200
      ) || null,
      cleanString(
        data.caption,
        500
      ),
      cleanString(
        data.musicName ||
          data.music_name,
        200
      ) || null,
      now
    )
    .run();

  return ok({
    video: {
      id: videoId,
      userId:
        user.id,
      videoUrl,
    },
  });
}

/* ------------------------------------------------ */
/* Like/unlike                                      */
/* ------------------------------------------------ */

async function handleLike(
  env: Env,
  request: Request,
  videoId: string
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const video =
    await env.DB.prepare(`
      SELECT id
      FROM videos
      WHERE id = ?
      LIMIT 1
    `)
      .bind(videoId)
      .first();

  if (!video) {
    return fail(
      "Video not found",
      404
    );
  }

  const existing =
    await env.DB.prepare(`
      SELECT id
      FROM likes
      WHERE
        user_id = ?
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
        WHERE
          user_id = ?
          AND video_id = ?
      `).bind(
        user.id,
        videoId
      ),

      env.DB.prepare(`
        UPDATE videos
        SET likes =
          CASE
            WHEN likes > 0
              THEN likes - 1
            ELSE 0
          END
        WHERE id = ?
      `).bind(videoId),
    ]);

    return ok({
      liked: false,
    });
  }

  await env.DB.batch([
    env.DB.prepare(`
      INSERT INTO likes (
        id,
        user_id,
        video_id,
        created_at
      )
      VALUES (
        ?,
        ?,
        ?,
        datetime('now')
      )
    `).bind(
      makeId("like"),
      user.id,
      videoId
    ),

    env.DB.prepare(`
      UPDATE videos
      SET likes =
        likes + 1
      WHERE id = ?
    `).bind(videoId),
  ]);

  return ok({
    liked: true,
  });
}

/* ------------------------------------------------ */
/* Views                                            */
/* ------------------------------------------------ */

async function handleVideoView(
  env: Env,
  request: Request,
  videoId: string
): Promise<Response> {
  const video =
    await env.DB.prepare(`
      SELECT id, views
      FROM videos
      WHERE id = ?
      LIMIT 1
    `)
      .bind(videoId)
      .first();

  if (!video) {
    return fail(
      "Video not found",
      404
    );
  }

  await env.DB.prepare(`
    UPDATE videos
    SET views =
      views + 1
    WHERE id = ?
  `)
    .bind(videoId)
    .run();

  return ok({
    views:
      Number(
        video.views || 0
      ) + 1,
  });
}

/* ------------------------------------------------ */
/* Comments                                         */
/* ------------------------------------------------ */

async function handleComments(
  env: Env,
  request: Request,
  videoId: string
): Promise<Response> {
  const url =
    new URL(request.url);

  const limit =
    safeLimit(
      url.searchParams.get(
        "limit"
      ),
      50
    );

  const rows =
    await env.DB.prepare(`
      SELECT
        c.id,
        c.video_id,
        c.user_id,
        c.text,
        c.created_at,
        u.username,
        u.display_name,
        u.avatar_url,
        u.verified
      FROM comments c
      INNER JOIN users u
        ON u.id = c.user_id
      WHERE c.video_id = ?
      ORDER BY
        c.created_at DESC
      LIMIT ?
    `)
      .bind(
        videoId,
        limit
      )
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) => ({
            id:
              row.id,
            videoId:
              row.video_id,
            userId:
              row.user_id,
            username:
              row.username,
            displayName:
              row.display_name ||
              row.username,
            avatar:
              row.avatar_url ||
              null,
            verified:
              Number(
                row.verified || 0
              ) === 1,
            text:
              row.text,
            createdAt:
              row.created_at,
          })
        ),
  });
}

async function handleCreateComment(
  env: Env,
  request: Request,
  videoId: string
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const video =
    await env.DB.prepare(`
      SELECT id
      FROM videos
      WHERE id = ?
      LIMIT 1
    `)
      .bind(videoId)
      .first();

  if (!video) {
    return fail(
      "Video not found",
      404
    );
  }

  const data =
    await readJson(request);

  const text =
    cleanString(
      data.text,
      500
    );

  if (!text) {
    return fail(
      "Comment cannot be empty",
      400
    );
  }

  const commentId =
    makeId("comment");

  await env.DB.batch([
    env.DB.prepare(`
      INSERT INTO comments (
        id,
        video_id,
        user_id,
        text,
        created_at
      )
      VALUES (
        ?,
        ?,
        ?,
        ?,
        datetime('now')
      )
    `).bind(
      commentId,
      videoId,
      user.id,
      text
    ),

    env.DB.prepare(`
      UPDATE videos
      SET comments =
        comments + 1
      WHERE id = ?
    `).bind(videoId),
  ]);

  return ok({
    comment: {
      id:
        commentId,
      videoId,
      userId:
        user.id,
      text,
    },
  });
}

/* ------------------------------------------------ */
/* Profiles                                         */
/* ------------------------------------------------ */

async function handleProfile(
  env: Env,
  request: Request,
  userId: string
): Promise<Response> {
  const profile =
    await env.DB.prepare(`
      SELECT *
      FROM users
      WHERE id = ?
      LIMIT 1
    `)
      .bind(userId)
      .first();

  if (!profile) {
    return fail(
      "User not found",
      404
    );
  }

  const currentUser =
    await getCurrentUser(
      env,
      request
    );

  let following = false;

  if (currentUser) {
    const row =
      await env.DB.prepare(`
        SELECT 1
        FROM follows
        WHERE
          follower_id = ?
          AND following_id = ?
        LIMIT 1
      `)
        .bind(
          currentUser.id,
          userId
        )
        .first();

    following =
      Boolean(row);
  }

  return ok({
    user: {
      ...publicUser(
        profile
      ),
      following,
    },
  });
}

/* ------------------------------------------------ */
/* Follow                                           */
/* ------------------------------------------------ */

async function handleFollow(
  env: Env,
  request: Request,
  targetUserId: string
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  if (
    String(user.id) ===
    String(targetUserId)
  ) {
    return fail(
      "You cannot follow yourself",
      400
    );
  }

  const target =
    await getUserById(
      env,
      targetUserId
    );

  if (!target) {
    return fail(
      "User not found",
      404
    );
  }

  const existing =
    await env.DB.prepare(`
      SELECT 1
      FROM follows
      WHERE
        follower_id = ?
        AND following_id = ?
      LIMIT 1
    `)
      .bind(
        user.id,
        targetUserId
      )
      .first();

  if (existing) {
    await env.DB.batch([
      env.DB.prepare(`
        DELETE FROM follows
        WHERE
          follower_id = ?
          AND following_id = ?
      `).bind(
        user.id,
        targetUserId
      ),

      env.DB.prepare(`
        UPDATE users
        SET following =
          CASE
            WHEN following > 0
              THEN following - 1
            ELSE 0
          END,
          updated_at = datetime('now')
        WHERE id = ?
      `).bind(user.id),

      env.DB.prepare(`
        UPDATE users
        SET followers =
          CASE
            WHEN followers > 0
              THEN followers - 1
            ELSE 0
          END,
          updated_at = datetime('now')
        WHERE id = ?
      `).bind(targetUserId),
    ]);

    return ok({
      following: false,
    });
  }

  await env.DB.batch([
    env.DB.prepare(`
      INSERT INTO follows (
        follower_id,
        following_id,
        created_at
      )
      VALUES (
        ?,
        ?,
        datetime('now')
      )
    `).bind(
      user.id,
      targetUserId
    ),

    env.DB.prepare(`
      UPDATE users
      SET following =
        following + 1,
        updated_at = datetime('now')
      WHERE id = ?
    `).bind(user.id),

    env.DB.prepare(`
      UPDATE users
      SET followers =
        followers + 1,
        updated_at = datetime('now')
      WHERE id = ?
    `).bind(targetUserId),
  ]);

  return ok({
    following: true,
  });
}

/* ------------------------------------------------ */
/* Live list                                        */
/* ------------------------------------------------ */

async function handleLiveList(
  env: Env
): Promise<Response> {
  const rows =
    await env.DB.prepare(`
      SELECT
        l.*,
        u.username,
        u.display_name,
        u.avatar_url,
        u.verified
      FROM live_streams l
      INNER JOIN users u
        ON u.id = l.user_id
      WHERE
        l.status = 'active'
      ORDER BY
        COALESCE(
          l.started_at,
          l.created_at
        ) DESC
      LIMIT 100
    `)
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) =>
            mapLive(row)
        ),
  });
}

/* ------------------------------------------------ */
/* Create live                                      */
/* ------------------------------------------------ */

async function handleCreateLive(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const data =
    await readJson(request);

  const title =
    cleanString(
      data.title,
      200
    );

  if (!title) {
    return fail(
      "Live title is required",
      400
    );
  }

  /*
   * These values may be supplied by the
   * actual streaming provider later.
   *
   * The Worker does not invent a fake
   * RTMPS/playback endpoint.
   */
  const streamUrl =
    cleanString(
      data.streamUrl ||
        data.stream_url,
      2000
    ) || null;

  const playbackUrl =
    cleanString(
      data.playbackUrl ||
        data.playback_url,
      2000
    ) || null;

  const rtmpsUrl =
    cleanString(
      data.rtmpsUrl ||
        data.rtmps_url,
      2000
    ) || null;

  const streamKey =
    cleanString(
      data.streamKey ||
        data.stream_key,
      500
    ) || null;

  const liveId =
    makeId("live");

  const now =
    new Date().toISOString();

  await env.DB.prepare(`
    INSERT INTO live_streams (
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
    VALUES (
      ?,
      ?,
      ?,
      ?,
      ?,
      ?,
      ?,
      0,
      0,
      'active',
      ?,
      ?
    )
  `)
    .bind(
      liveId,
      user.id,
      title,
      streamUrl,
      playbackUrl,
      rtmpsUrl,
      streamKey,
      now,
      now
    )
    .run();

  const live =
    await env.DB.prepare(`
      SELECT
        l.*,
        u.username,
        u.display_name,
        u.avatar_url,
        u.verified
      FROM live_streams l
      INNER JOIN users u
        ON u.id = l.user_id
      WHERE l.id = ?
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  return ok({
    live:
      mapLive(live),
  });
}

/* ------------------------------------------------ */
/* Get live                                         */
/* ------------------------------------------------ */

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
        u.avatar_url,
        u.verified
      FROM live_streams l
      INNER JOIN users u
        ON u.id = l.user_id
      WHERE l.id = ?
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  if (!live) {
    return fail(
      "Live not found",
      404
    );
  }

  return ok({
    live:
      mapLive(live),
  });
}

/* ------------------------------------------------ */
/* Update live                                      */
/* ------------------------------------------------ */

async function handleUpdateLive(
  env: Env,
  request: Request,
  liveId: string
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

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
    return fail(
      "Live not found",
      404
    );
  }

  if (
    String(live.user_id) !==
    String(user.id)
  ) {
    return fail(
      "Forbidden",
      403
    );
  }

  const data =
    await readJson(request);

  const updates: string[] =
    [];

  const values: unknown[] =
    [];

  if (
    data.title !== undefined
  ) {
    const title =
      cleanString(
        data.title,
        200
      );

    if (!title) {
      return fail(
        "Title cannot be empty",
        400
      );
    }

    updates.push(
      "title = ?"
    );

    values.push(title);
  }

  if (
    data.streamUrl !==
      undefined ||
    data.stream_url !==
      undefined
  ) {
    updates.push(
      "stream_url = ?"
    );

    values.push(
      cleanString(
        data.streamUrl ||
          data.stream_url,
        2000
      ) || null
    );
  }

  if (
    data.playbackUrl !==
      undefined ||
    data.playback_url !==
      undefined
  ) {
    updates.push(
      "playback_url = ?"
    );

    values.push(
      cleanString(
        data.playbackUrl ||
          data.playback_url,
        2000
      ) || null
    );
  }

  if (
    data.rtmpsUrl !==
      undefined ||
    data.rtmps_url !==
      undefined
  ) {
    updates.push(
      "rtmps_url = ?"
    );

    values.push(
      cleanString(
        data.rtmpsUrl ||
          data.rtmps_url,
        2000
      ) || null
    );
  }

  if (
    data.streamKey !==
      undefined ||
    data.stream_key !==
      undefined
  ) {
    updates.push(
      "stream_key = ?"
    );

    values.push(
      cleanString(
        data.streamKey ||
          data.stream_key,
        500
      ) || null
    );
  }

  if (
    data.status !==
    undefined
  ) {
    const status =
      String(
        data.status
      );

    if (
      ![
        "active",
        "ended",
      ].includes(status)
    ) {
      return fail(
        "Invalid live status",
        400
      );
    }

    updates.push(
      "status = ?"
    );

    values.push(status);

    if (
      status === "ended"
    ) {
      updates.push(
        "ended_at = datetime('now')"
      );
    }
  }

  if (
    data.viewerCount !==
      undefined ||
    data.viewer_count !==
      undefined
  ) {
    updates.push(
      "viewer_count = ?"
    );

    values.push(
      clamp(
        integer(
          data.viewerCount ??
            data.viewer_count,
          0
        ),
        0,
        100000000
      )
    );
  }

  if (
    data.likes !==
      undefined
  ) {
    updates.push(
      "likes = ?"
    );

    values.push(
      clamp(
        integer(
          data.likes,
          0
        ),
        0,
        100000000
      )
    );
  }

  if (
    updates.length === 0
  ) {
    return handleGetLive(
      env,
      liveId
    );
  }

  values.push(
    liveId
  );

  await env.DB.prepare(`
    UPDATE live_streams
    SET ${updates.join(", ")}
    WHERE id = ?
  `)
    .bind(
      ...values
    )
    .run();

  return handleGetLive(
    env,
    liveId
  );
}

/* ------------------------------------------------ */
/* Gifts                                            */
/* ------------------------------------------------ */

async function handleGifts(
  env: Env
): Promise<Response> {
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
      ORDER BY
        price ASC,
        name ASC
    `)
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (gift: any) =>
            mapGift(gift)
        ),
  });
}

/* ------------------------------------------------ */
/* Send gift                                        */
/* ------------------------------------------------ */

async function handleSendGift(
  env: Env,
  request: Request,
  liveId: string
): Promise<Response> {
  const sender =
    await requireUser(
      env,
      request
    );

  const data =
    await readJson(request);

  const giftId =
    cleanString(
      data.giftId ||
        data.gift_id,
      100
    );

  const quantity =
    clamp(
      integer(
        data.quantity,
        1
      ),
      1,
      100
    );

  const live =
    await env.DB.prepare(`
      SELECT *
      FROM live_streams
      WHERE
        id = ?
        AND status = 'active'
      LIMIT 1
    `)
      .bind(liveId)
      .first();

  if (!live) {
    return fail(
      "Active live not found",
      404
    );
  }

  const receiverId =
    cleanString(
      data.receiverUserId ||
        data.receiver_user_id ||
        live.user_id,
      200
    );

  if (
    String(sender.id) ===
    String(receiverId)
  ) {
    return fail(
      "You cannot send a gift to yourself",
      400
    );
  }

  const gift =
    await getGift(
      env,
      giftId
    );

  if (!gift) {
    return fail(
      "Gift not found",
      404
    );
  }

  const price =
    Number(
      gift.price || 0
    );

  const totalCoins =
    price * quantity;

  if (
    !Number.isSafeInteger(
      totalCoins
    ) ||
    totalCoins <= 0
  ) {
    return fail(
      "Invalid gift amount",
      400
    );
  }

  const receiver =
    await getUserById(
      env,
      receiverId
    );

  if (!receiver) {
    return fail(
      "Receiver not found",
      404
    );
  }

  const transactionId =
    makeId("gift");

  /*
   * The trigger performs:
   * 1. validation
   * 2. sender debit
   * 3. receiver credit
   *
   * All happen inside the INSERT transaction.
   */
  try {
    await env.DB.prepare(`
      INSERT INTO gift_transactions (
        id,
        live_id,
        sender_user_id,
        receiver_user_id,
        gift_id,
        quantity,
        total_coins,
        created_at
      )
      VALUES (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        datetime('now')
      )
    `)
      .bind(
        transactionId,
        liveId,
        sender.id,
        receiverId,
        gift.id,
        quantity,
        totalCoins
      )
      .run();
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : String(error);

    if (
      message.includes(
        "INSUFFICIENT_COINS"
      )
    ) {
      const current =
        await getUserById(
          env,
          String(sender.id)
        );

      return fail(
        "Insufficient coins",
        400,
        {
          requiredCoins:
            totalCoins,
          currentCoins:
            Number(
              current?.coins || 0
            ),
        }
      );
    }

    if (
      message.includes(
        "INVALID_RECEIVER"
      )
    ) {
      return fail(
        "Receiver not found",
        404
      );
    }

    throw error;
  }

  const updatedSender =
    await getUserById(
      env,
      String(sender.id)
    );

  return ok({
    transactionId,
    remainingCoins:
      Number(
        updatedSender?.coins ||
          0
      ),
    gift: {
      id:
        gift.id,
      name:
        gift.name,
      icon:
        gift.icon || "",
      imageUrl:
        gift.image_url ||
        null,
      animationUrl:
        gift.animation_url ||
        null,
      quantity,
      totalCoins,
    },
  });
}

/* ------------------------------------------------ */
/* Gift history                                     */
/* ------------------------------------------------ */

async function handleGiftHistory(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const rows =
    await env.DB.prepare(`
      SELECT
        gt.id,
        gt.live_id,
        gt.sender_user_id,
        gt.receiver_user_id,
        gt.gift_id,
        gt.quantity,
        gt.total_coins,
        gt.created_at,
        g.name AS gift_name,
        g.icon AS gift_icon
      FROM gift_transactions gt
      INNER JOIN gifts g
        ON g.id = gt.gift_id
      WHERE
        gt.sender_user_id = ?
        OR gt.receiver_user_id = ?
      ORDER BY
        gt.created_at DESC
      LIMIT 100
    `)
      .bind(
        user.id,
        user.id
      )
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) => ({
            id:
              row.id,
            liveId:
              row.live_id,
            senderUserId:
              row.sender_user_id,
            receiverUserId:
              row.receiver_user_id,
            giftId:
              row.gift_id,
            giftName:
              row.gift_name,
            giftIcon:
              row.gift_icon,
            quantity:
              Number(
                row.quantity || 0
              ),
            totalCoins:
              Number(
                row.total_coins ||
                  0
              ),
            createdAt:
              row.created_at,
          })
        ),
  });
}

/* ------------------------------------------------ */
/* Wallet                                           */
/* ------------------------------------------------ */

async function handleWallet(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const fresh =
    await getUserById(
      env,
      String(user.id)
    );

  return ok({
    coins:
      Number(
        fresh?.coins || 0
      ),
    walletNumbers:
      WALLET_NUMBERS,
    paymentMethods: [
      "wallet",
    ],
  });
}

/* ------------------------------------------------ */
/* Deposit                                          */
/* ------------------------------------------------ */

async function handleCreateDeposit(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const data =
    await readJson(request);

  const amount =
    integer(
      data.amount,
      0
    );

  const walletNumber =
    cleanString(
      data.walletNumber ||
        data.wallet_number,
      50
    );

  const reference =
    cleanString(
      data.transactionReference ||
        data.transaction_reference,
      200
    );

  if (
    amount <= 0 ||
    amount > 1000000
  ) {
    return fail(
      "Invalid deposit amount",
      400
    );
  }

  if (
    !WALLET_NUMBERS.includes(
      walletNumber
    )
  ) {
    return fail(
      "Unsupported wallet number",
      400
    );
  }

  if (!reference) {
    return fail(
      "Transaction reference is required",
      400
    );
  }

  /*
   * The conversion is intentionally kept 1:1
   * until the official pricing/admin approval
   * system is connected.
   *
   * Coins are NOT credited here.
   */
  const coins =
    amount;

  const depositId =
    makeId("dep");

  const now =
    new Date().toISOString();

  await env.DB.prepare(`
    INSERT INTO wallet_deposits (
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
    VALUES (
      ?,
      ?,
      ?,
      ?,
      ?,
      ?,
      'pending',
      ?,
      ?
    )
  `)
    .bind(
      depositId,
      user.id,
      amount,
      walletNumber,
      reference,
      coins,
      now,
      now
    )
    .run();

  return ok({
    depositId,
    amount,
    coins,
    walletNumber,
    transactionReference:
      reference,
    status:
      "pending",
    message:
      "Deposit submitted for verification",
  });
}

/* ------------------------------------------------ */
/* Deposit history                                  */
/* ------------------------------------------------ */

async function handleDeposits(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

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
      ORDER BY
        created_at DESC
      LIMIT 100
    `)
      .bind(user.id)
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) => ({
            id:
              row.id,
            amount:
              Number(
                row.amount || 0
              ),
            walletNumber:
              row.wallet_number,
            transactionReference:
              row.transaction_reference ||
              null,
            coins:
              Number(
                row.coins || 0
              ),
            status:
              row.status,
            createdAt:
              row.created_at,
            updatedAt:
              row.updated_at,
          })
        ),
  });
}

/* ------------------------------------------------ */
/* Wallet transactions                              */
/* ------------------------------------------------ */

async function handleWalletTransactions(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const rows =
    await env.DB.prepare(`
      SELECT
        id,
        type,
        amount,
        coins,
        reference,
        created_at
      FROM wallet_transactions
      WHERE user_id = ?
      ORDER BY
        created_at DESC
      LIMIT 100
    `)
      .bind(user.id)
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) => ({
            id:
              row.id,
            type:
              row.type,
            amount:
              Number(
                row.amount || 0
              ),
            coins:
              Number(
                row.coins || 0
              ),
            reference:
              row.reference ||
              null,
            createdAt:
              row.created_at,
          })
        ),
  });
}

/* ------------------------------------------------ */
/* Music                                            */
/* ------------------------------------------------ */

async function handleMusic(
  env: Env
): Promise<Response> {
  const rows =
    await env.DB.prepare(`
      SELECT
        id,
        title,
        artist,
        audio_url,
        cover_url,
        duration_seconds,
        active,
        created_at
      FROM music_tracks
      WHERE active = 1
      ORDER BY
        created_at DESC
      LIMIT 200
    `)
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) => ({
            id:
              row.id,
            title:
              row.title,
            artist:
              row.artist ||
              "",
            audioUrl:
              row.audio_url,
            coverUrl:
              row.cover_url ||
              null,
            durationSeconds:
              Number(
                row.duration_seconds ||
                  0
              ),
          })
        ),
  });
}

/* ------------------------------------------------ */
/* Effects                                          */
/* ------------------------------------------------ */

async function handleEffects(
  env: Env
): Promise<Response> {
  const rows =
    await env.DB.prepare(`
      SELECT
        id,
        name,
        type,
        value,
        active,
        sort_order
      FROM visual_effects
      WHERE active = 1
      ORDER BY
        sort_order ASC
    `)
      .all();

  return ok({
    items:
      (rows.results || [])
        .map(
          (row: any) => ({
            id:
              row.id,
            name:
              row.name,
            type:
              row.type,
            value:
              row.value,
          })
        ),
  });
}

/* ------------------------------------------------ */
/* Reports                                          */
/* ------------------------------------------------ */

async function handleCreateReport(
  env: Env,
  request: Request
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  const data =
    await readJson(request);

  const targetType =
    cleanString(
      data.targetType ||
        data.target_type,
      50
    );

  const targetId =
    cleanString(
      data.targetId ||
        data.target_id,
      200
    );

  const reason =
    cleanString(
      data.reason,
      200
    );

  if (
    !targetType ||
    !targetId ||
    !reason
  ) {
    return fail(
      "targetType, targetId and reason are required",
      400
    );
  }

  const reportId =
    makeId("report");

  await env.DB.prepare(`
    INSERT INTO reports (
      id,
      reporter_user_id,
      target_type,
      target_id,
      reason,
      status,
      created_at
    )
    VALUES (
      ?,
      ?,
      ?,
      ?,
      ?,
      'pending',
      datetime('now')
    )
  `)
    .bind(
      reportId,
      user.id,
      targetType,
      targetId,
      reason
    )
    .run();

  return ok({
    reportId,
    status:
      "pending",
  });
}

/* ------------------------------------------------ */
/* Blocks                                           */
/* ------------------------------------------------ */

async function handleBlock(
  env: Env,
  request: Request,
  targetUserId: string
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  if (
    String(user.id) ===
    String(targetUserId)
  ) {
    return fail(
      "You cannot block yourself",
      400
    );
  }

  const target =
    await getUserById(
      env,
      targetUserId
    );

  if (!target) {
    return fail(
      "User not found",
      404
    );
  }

  await env.DB.prepare(`
    INSERT OR IGNORE INTO blocks (
      blocker_id,
      blocked_id,
      created_at
    )
    VALUES (
      ?,
      ?,
      datetime('now')
    )
  `)
    .bind(
      user.id,
      targetUserId
    )
    .run();

  /*
   * Remove follow relationships as well.
   */
  await env.DB.batch([
    env.DB.prepare(`
      DELETE FROM follows
      WHERE
        follower_id = ?
        AND following_id = ?
    `).bind(
      user.id,
      targetUserId
    ),

    env.DB.prepare(`
      DELETE FROM follows
      WHERE
        follower_id = ?
        AND following_id = ?
    `).bind(
      targetUserId,
      user.id
    ),
  ]);

  return ok({
    blocked: true,
  });
}

async function handleUnblock(
  env: Env,
  request: Request,
  targetUserId: string
): Promise<Response> {
  const user =
    await requireUser(
      env,
      request
    );

  await env.DB.prepare(`
    DELETE FROM blocks
    WHERE
      blocker_id = ?
      AND blocked_id = ?
  `)
    .bind(
      user.id,
      targetUserId
    )
    .run();

  return ok({
    blocked: false,
  });
}

/* ------------------------------------------------ */
/* Router                                           */
/* ------------------------------------------------ */

async function handleRequest(
  request: Request,
  env: Env
): Promise<Response> {
  const url =
    new URL(request.url);

  const method =
    request.method;

  const path =
    url.pathname
      .replace(
        /\/+/g,
        "/"
      )
      .replace(
        /\/$/,
        ""
      ) || "/";

  /*
   * Do not silently create a different schema here.
   * The six migrations are the canonical database schema.
   */
  if (
    path === "/health" &&
    method === "GET"
  ) {
    return await handleHealth(
      env
    );
  }

  /*
   * Seed gifts and install the safe transfer trigger.
   */
  await prepareRuntime(env);

  /* Authentication */

  if (
    path ===
      "/api/auth/register" &&
    method === "POST"
  ) {
    return await handleRegister(
      env,
      request
    );
  }

  if (
    path ===
      "/api/auth/login" &&
    method === "POST"
  ) {
    return await handleLogin(
      env,
      request
    );
  }

  if (
    path ===
      "/api/auth/logout" &&
    method === "POST"
  ) {
    return await handleLogout(
      env,
      request
    );
  }

  if (
    path === "/api/me" &&
    method === "GET"
  ) {
    return await handleMe(
      env,
      request
    );
  }

  /* Feed */

  if (
    path === "/api/feed" &&
    method === "GET"
  ) {
    return await handleFeed(
      env,
      request
    );
  }

  if (
    path === "/api/videos" &&
    method === "POST"
  ) {
    return await handleCreateVideo(
      env,
      request
    );
  }

  let match =
    path.match(
      /^\/api\/videos\/([^/]+)\/like$/
    );

  if (
    match &&
    method === "POST"
  ) {
    return await handleLike(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  match =
    path.match(
      /^\/api\/videos\/([^/]+)\/view$/
    );

  if (
    match &&
    method === "POST"
  ) {
    return await handleVideoView(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  match =
    path.match(
      /^\/api\/videos\/([^/]+)\/comments$/
    );

  if (
    match &&
    method === "GET"
  ) {
    return await handleComments(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  if (
    match &&
    method === "POST"
  ) {
    return await handleCreateComment(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  /* Profiles */

  match =
    path.match(
      /^\/api\/users\/([^/]+)$/
    );

  if (
    match &&
    method === "GET"
  ) {
    return await handleProfile(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  match =
    path.match(
      /^\/api\/users\/([^/]+)\/follow$/
    );

  if (
    match &&
    method === "POST"
  ) {
    return await handleFollow(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  /* Live */

  if (
    path === "/api/live" &&
    method === "GET"
  ) {
    return await handleLiveList(
      env
    );
  }

  if (
    path === "/api/live/create" &&
    method === "POST"
  ) {
    return await handleCreateLive(
      env,
      request
    );
  }

  match =
    path.match(
      /^\/api\/live\/([^/]+)$/
    );

  if (
    match &&
    method === "GET"
  ) {
    return await handleGetLive(
      env,
      decodeURIComponent(
        match[1]
      )
    );
  }

  if (
    match &&
    method === "PATCH"
  ) {
    return await handleUpdateLive(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  /* Gifts */

  if (
    path === "/api/gifts" &&
    method === "GET"
  ) {
    return await handleGifts(
      env
    );
  }

  match =
    path.match(
      /^\/api\/live\/([^/]+)\/gifts$/
    );

  if (
    match &&
    method === "POST"
  ) {
    return await handleSendGift(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  if (
    path ===
      "/api/gifts/history" &&
    method === "GET"
  ) {
    return await handleGiftHistory(
      env,
      request
    );
  }

  /* Wallet */

  if (
    path === "/api/wallet" &&
    method === "GET"
  ) {
    return await handleWallet(
      env,
      request
    );
  }

  if (
    path ===
      "/api/wallet/deposit" &&
    method === "POST"
  ) {
    return await handleCreateDeposit(
      env,
      request
    );
  }

  if (
    path ===
      "/api/wallet/deposits" &&
    method === "GET"
  ) {
    return await handleDeposits(
      env,
      request
    );
  }

  if (
    path ===
      "/api/wallet/transactions" &&
    method === "GET"
  ) {
    return await handleWalletTransactions(
      env,
      request
    );
  }

  /* Music */

  if (
    path === "/api/music" &&
    method === "GET"
  ) {
    return await handleMusic(
      env
    );
  }

  /* Effects */

  if (
    path === "/api/effects" &&
    method === "GET"
  ) {
    return await handleEffects(
      env
    );
  }

  /* Reports */

  if (
    path === "/api/reports" &&
    method === "POST"
  ) {
    return await handleCreateReport(
      env,
      request
    );
  }

  /* Blocks */

  match =
    path.match(
      /^\/api\/users\/([^/]+)\/block$/
    );

  if (
    match &&
    method === "POST"
  ) {
    return await handleBlock(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  if (
    match &&
    method === "DELETE"
  ) {
    return await handleUnblock(
      env,
      request,
      decodeURIComponent(
        match[1]
      )
    );
  }

  return fail(
    "Not found",
    404
  );
}

/* ------------------------------------------------ */
/* Worker entry                                     */
/* ------------------------------------------------ */

export default {
  async fetch(
    request: Request,
    env: Env
  ): Promise<Response> {
    if (
      request.method ===
      "OPTIONS"
    ) {
      return new Response(
        null,
        {
          status: 204,
          headers:
            CORS_HEADERS,
        }
      );
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

      if (
        error instanceof
        ApiError
      ) {
        return fail(
          error.message,
          error.status,
          error.extra
        );
      }

      const message =
        error instanceof Error
          ? error.message
          : "Internal server error";

      return fail(
        message,
        500
      );
    }
  },
};
