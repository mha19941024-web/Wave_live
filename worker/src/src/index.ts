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
    imageUrl: null,
    animationUrl: null,
    enabled: true,
  },
  {
    id: "heart",
    name: "Heart",
    price: 10,
    icon: "❤️",
    imageUrl: null,
    animationUrl: null,
    enabled: true,
  },
  {
    id: "wave-crown",
    name: "Wave Crown",
    price: 500,
    icon: "👑",
    imageUrl: null,
    animationUrl: null,
    enabled: true,
  },
  {
    id: "diamond",
    name: "Diamond",
    price: 250,
    icon: "💎",
    imageUrl: null,
    animationUrl: null,
    enabled: true,
  },
  {
    id: "rocket",
    name: "Rocket",
    price: 500,
    icon: "🚀",
    imageUrl: null,
    animationUrl: null,
    enabled: true,
  },
  {
    id: "super-star",
    name: "Super Star",
    price: 1000,
    icon: "⭐",
    imageUrl: null,
    animationUrl: null,
    enabled: true,
  },
];

const jsonHeaders = {
  "Content-Type": "application/json; charset=utf-8",
};

function corsHeaders(request: Request, env: Env): Record<string, string> {
  const origin = request.headers.get("Origin") || "*";
  const allowed = env.CORS_ORIGIN || "*";

  return {
    ...jsonHeaders,
    "Access-Control-Allow-Origin": allowed === "*" ? "*" : origin,
    "Access-Control-Allow-Headers":
      "Content-Type, Authorization, X-Requested-With",
    "Access-Control-Allow-Methods":
      "GET, POST, PATCH, DELETE, OPTIONS",
    "Access-Control-Max-Age": "86400",
  };
}

function response(
  body: unknown,
  status = 200,
  request?: Request,
  env?: Env
): Response {
  const headers = corsHeaders(
    request || new Request("https://wave.local"),
    env || ({} as Env)
  );

  return new Response(JSON.stringify(body), {
    status,
    headers,
  });
}

function now(): string {
  return new Date().toISOString();
}

function id(prefix: string): string {
  return `${prefix}_${crypto.randomUUID()}`;
}

function getToken(request: Request): string | null {
  const value = request.headers.get("Authorization");

  if (!value) {
    return null;
  }

  if (!value.startsWith("Bearer ")) {
    return null;
  }

  return value.substring(7).trim() || null;
}

async function getUser(
  request: Request,
  env: Env
): Promise<any | null> {
  const token = getToken(request);

  if (!token) {
    return null;
  }

  const session = await env.DB.prepare(
    `
    SELECT
      s.user_id,
      s.expires_at
    FROM sessions s
    WHERE s.token = ?
    LIMIT 1
    `
  )
    .bind(token)
    .first();

  if (!session) {
    return null;
  }

  const expiresAt = String(session.expires_at || "");

  if (expiresAt && new Date(expiresAt).getTime() < Date.now()) {
    return null;
  }

  return await env.DB.prepare(
    `
    SELECT *
    FROM users
    WHERE id = ?
    LIMIT 1
    `
  )
    .bind(session.user_id)
    .first();
}

function publicUser(user: any, isFollowing = false) {
  if (!user) {
    return null;
  }

  return {
    id: String(user.id || ""),
    username: String(user.username || ""),
    displayName: String(
      user.display_name || user.username || ""
    ),
    avatar: user.avatar_url || null,
    bio: user.bio || null,
    coins: Number(user.coins || 0),
    followers: Number(user.followers || 0),
    following: Number(user.following || 0),
    isFollowing,
    verified: Boolean(user.verified || false),
  };
}

function publicGift(gift: any) {
  return {
    id: String(gift.id || ""),
    name: String(gift.name || ""),
    price: Number(gift.price || 0),
    icon: gift.icon || "",
    imageUrl: gift.image_url || null,
    animationUrl: gift.animation_url || null,
    enabled: Boolean(gift.enabled ?? true),
  };
}

function publicLive(live: any) {
  return {
    id: String(live.id || ""),
    userId: String(live.user_id || ""),
    username: String(live.username || ""),
    displayName: String(live.display_name || ""),
    avatar: live.avatar_url || null,
    title: String(live.title || ""),
    streamUrl: live.stream_url || null,
    playbackUrl: live.playback_url || null,
    rtmpsUrl: live.rtmps_url || null,
    streamKey: live.stream_key || null,
    viewerCount: Number(live.viewer_count || 0),
    likes: Number(live.likes || 0),
    status: String(live.status || "active"),
    startedAt: live.started_at || null,
  };
}

async function ensureGifts(env: Env) {
  try {
    const row = await env.DB.prepare(
      `SELECT COUNT(*) AS count FROM gifts`
    ).first();

    const count = Number(row?.count || 0);

    if (count > 0) {
      return;
    }

    for (const gift of DEFAULT_GIFTS) {
      await env.DB.prepare(
        `
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
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `
      )
        .bind(
          gift.id,
          gift.name,
          gift.price,
          gift.icon,
          gift.imageUrl,
          gift.animationUrl,
          gift.enabled ? 1 : 0,
          now()
        )
        .run();
    }
  } catch {
    // Database migrations are handled separately.
  }
}

async function handleRequest(
  request: Request,
  env: Env
): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;
  const method = request.method.toUpperCase();

  if (method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: corsHeaders(request, env),
    });
  }

  if (path === "/health" || path === "/") {
    return response(
      {
        success: true,
        message: "Wave Server is working!",
        environment: env.ENVIRONMENT || "production",
        time: now(),
      },
      200,
      request,
      env
    );
  }

  /*
   * SESSION
   */
  if (path === "/api/session" && method === "POST") {
    const userId = id("user");
    const token = crypto.randomUUID() + crypto.randomUUID();

    const username = `wave_${userId.substring(5, 13)}`;

    const displayName = "Wave User";

    const days = Math.max(
      1,
      Number(env.SESSION_DAYS || 30)
    );

    const expiresAt = new Date(
      Date.now() + days * 86400000
    ).toISOString();

    try {
      await env.DB.prepare(
        `
        INSERT INTO users
        (
          id,
          username,
          display_name,
          coins,
          followers,
          following,
          verified,
          created_at,
          updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        `
      )
        .bind(
          userId,
          username,
          displayName,
          0,
          0,
          0,
          0,
          now(),
          now()
        )
        .run();

      await env.DB.prepare(
        `
        INSERT INTO sessions
        (
          token,
          user_id,
          expires_at,
          created_at
        )
        VALUES (?, ?, ?, ?)
        `
      )
        .bind(
          token,
          userId,
          expiresAt,
          now()
        )
        .run();

      return response(
        {
          success: true,
          token,
          user: publicUser({
            id: userId,
            username,
            display_name: displayName,
            coins: 0,
            followers: 0,
            following: 0,
            verified: 0,
          }),
        },
        200,
        request,
        env
      );
    } catch (error) {
      return response(
        {
          success: false,
          message: "Unable to create session",
          error: String(error),
        },
        500,
        request,
        env
      );
    }
  }

  /*
   * CURRENT USER
   */
  if (path === "/api/me" && method === "GET") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    return response(
      {
        success: true,
        user: publicUser(user),
        wallet: {
          coins: Number(user.coins || 0),
        },
      },
      200,
      request,
      env
    );
  }

  /*
   * FEED
   */
  if (path === "/api/feed" && method === "GET") {
    const limit = Math.min(
      50,
      Math.max(
        1,
        Number(url.searchParams.get("limit") || 20)
      )
    );

    const cursor = Math.max(
      0,
      Number(url.searchParams.get("cursor") || 0)
    );

    try {
      const result = await env.DB.prepare(
        `
        SELECT
          v.*,
          u.username,
          u.display_name,
          u.avatar_url
        FROM videos v
        LEFT JOIN users u
          ON u.id = v.user_id
        ORDER BY v.created_at DESC
        LIMIT ? OFFSET ?
        `
      )
        .bind(limit, cursor)
        .all();

      const items = (result.results || []).map(
        (video: any) => ({
          id: String(video.id || ""),
          userId: String(video.user_id || ""),
          username: String(video.username || ""),
          displayName: String(video.display_name || ""),
          avatar: video.avatar_url || null,
          videoUrl: String(video.video_url || ""),
          thumbnailUrl: video.thumbnail_url || null,
          caption: String(video.caption || ""),
          musicName: video.music_name || null,
          likes: Number(video.likes || 0),
          comments: Number(video.comments || 0),
          shares: Number(video.shares || 0),
          views: Number(video.views || 0),
          liked: false,
          createdAt: video.created_at || null,
        })
      );

      return response(
        {
          success: true,
          items,
          nextCursor: cursor + items.length,
        },
        200,
        request,
        env
      );
    } catch (error) {
      return response(
        {
          success: false,
          items: [],
          message: String(error),
        },
        500,
        request,
        env
      );
    }
  }

  /*
   * CREATE VIDEO
   */
  if (path === "/api/videos" && method === "POST") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const body: any = await request
      .json()
      .catch(() => ({}));

    const videoUrl = String(body.url || "").trim();
    const caption = String(body.caption || "").trim();
    const streamId = body.streamId
      ? String(body.streamId)
      : null;

    if (!videoUrl && !streamId) {
      return response(
        {
          success: false,
          message: "Video URL is required",
        },
        400,
        request,
        env
      );
    }

    const videoId = id("video");

    await env.DB.prepare(
      `
      INSERT INTO videos
      (
        id,
        user_id,
        video_url,
        stream_id,
        caption,
        likes,
        comments,
        shares,
        views,
        created_at
      )
      VALUES (?, ?, ?, ?, ?, 0, 0, 0, 0, ?)
      `
    )
      .bind(
        videoId,
        user.id,
        videoUrl,
        streamId,
        caption,
        now()
      )
      .run();

    return response(
      {
        success: true,
        id: videoId,
        message: "Video created",
      },
      201,
      request,
      env
    );
  }

  /*
   * LIKE / UNLIKE
   */
  const likeMatch = path.match(
    /^\/api\/videos\/([^/]+)\/like$/
  );

  if (likeMatch && method === "POST") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const videoId = likeMatch[1];

    try {
      await env.DB.prepare(
        `
        INSERT OR IGNORE INTO likes
        (
          id,
          user_id,
          video_id,
          created_at
        )
        VALUES (?, ?, ?, ?)
        `
      )
        .bind(
          id("like"),
          user.id,
          videoId,
          now()
        )
        .run();

      await env.DB.prepare(
        `
        UPDATE videos
        SET likes =
          (
            SELECT COUNT(*)
            FROM likes
            WHERE video_id = ?
          )
        WHERE id = ?
        `
      )
        .bind(videoId, videoId)
        .run();

      return response(
        {
          success: true,
          liked: true,
        },
        200,
        request,
        env
      );
    } catch (error) {
      return response(
        {
          success: false,
          message: String(error),
        },
        500,
        request,
        env
      );
    }
  }

  const unlikeMatch = path.match(
    /^\/api\/videos\/([^/]+)\/like$/
  );

  if (unlikeMatch && method === "DELETE") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const videoId = unlikeMatch[1];

    await env.DB.prepare(
      `
      DELETE FROM likes
      WHERE user_id = ?
      AND video_id = ?
      `
    )
      .bind(user.id, videoId)
      .run();

    await env.DB.prepare(
      `
      UPDATE videos
      SET likes =
        (
          SELECT COUNT(*)
          FROM likes
          WHERE video_id = ?
        )
      WHERE id = ?
      `
    )
      .bind(videoId, videoId)
      .run();

    return response(
      {
        success: true,
        liked: false,
      },
      200,
      request,
      env
    );
  }

  /*
   * COMMENTS
   */
  const commentsMatch = path.match(
    /^\/api\/videos\/([^/]+)\/comments$/
  );

  if (commentsMatch && method === "GET") {
    const videoId = commentsMatch[1];

    const limit = Math.min(
      100,
      Math.max(
        1,
        Number(url.searchParams.get("limit") || 30)
      )
    );

    const result = await env.DB.prepare(
      `
      SELECT
        c.*,
        u.username,
        u.display_name,
        u.avatar_url
      FROM comments c
      LEFT JOIN users u
        ON u.id = c.user_id
      WHERE c.video_id = ?
      ORDER BY c.created_at DESC
      LIMIT ?
      `
    )
      .bind(videoId, limit)
      .all();

    return response(
      {
        success: true,
        items: (result.results || []).map(
          (comment: any) => ({
            id: String(comment.id || ""),
            videoId: String(comment.video_id || ""),
            userId: String(comment.user_id || ""),
            username: String(comment.username || ""),
            displayName: String(
              comment.display_name || ""
            ),
            avatar: comment.avatar_url || null,
            text: String(comment.text || ""),
            createdAt: comment.created_at || null,
          })
        ),
      },
      200,
      request,
      env
    );
  }

  if (commentsMatch && method === "POST") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const videoId = commentsMatch[1];

    const body: any = await request
      .json()
      .catch(() => ({}));

    const text = String(body.text || "").trim();

    if (!text) {
      return response(
        {
          success: false,
          message: "Comment cannot be empty",
        },
        400,
        request,
        env
      );
    }

    const commentId = id("comment");

    await env.DB.prepare(
      `
      INSERT INTO comments
      (
        id,
        video_id,
        user_id,
        text,
        created_at
      )
      VALUES (?, ?, ?, ?, ?)
      `
    )
      .bind(
        commentId,
        videoId,
        user.id,
        text.substring(0, 1000),
        now()
      )
      .run();

    await env.DB.prepare(
      `
      UPDATE videos
      SET comments =
        (
          SELECT COUNT(*)
          FROM comments
          WHERE video_id = ?
        )
      WHERE id = ?
      `
    )
      .bind(videoId, videoId)
      .run();

    return response(
      {
        success: true,
        id: commentId,
        message: "Comment added",
      },
      201,
      request,
      env
    );
  }

  /*
   * FOLLOW
   */
  const followMatch = path.match(
    /^\/api\/users\/([^/]+)\/follow$/
  );

  if (followMatch && method === "POST") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const target = await env.DB.prepare(
      `
      SELECT *
      FROM users
      WHERE id = ?
      OR username = ?
      LIMIT 1
      `
    )
      .bind(
        followMatch[1],
        followMatch[1]
      )
      .first();

    if (!target) {
      return response(
        {
          success: false,
          message: "User not found",
        },
        404,
        request,
        env
      );
    }

    if (target.id === user.id) {
      return response(
        {
          success: false,
          message: "Cannot follow yourself",
        },
        400,
        request,
        env
      );
    }

    await env.DB.prepare(
      `
      INSERT OR IGNORE INTO follows
      (
        follower_id,
        following_id,
        created_at
      )
      VALUES (?, ?, ?)
      `
    )
      .bind(
        user.id,
        target.id,
        now()
      )
      .run();

    await env.DB.prepare(
      `
      UPDATE users
      SET following =
        (
          SELECT COUNT(*)
          FROM follows
          WHERE follower_id = ?
        )
      WHERE id = ?
      `
    )
      .bind(user.id, user.id)
      .run();

    await env.DB.prepare(
      `
      UPDATE users
      SET followers =
        (
          SELECT COUNT(*)
          FROM follows
          WHERE following_id = ?
        )
      WHERE id = ?
      `
    )
      .bind(target.id, target.id)
      .run();

    return response(
      {
        success: true,
        following: true,
      },
      200,
      request,
      env
    );
  }

  if (followMatch && method === "DELETE") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const target = await env.DB.prepare(
      `
      SELECT *
      FROM users
      WHERE id = ?
      OR username = ?
      LIMIT 1
      `
    )
      .bind(
        followMatch[1],
        followMatch[1]
      )
      .first();

    if (!target) {
      return response(
        {
          success: false,
          message: "User not found",
        },
        404,
        request,
        env
      );
    }

    await env.DB.prepare(
      `
      DELETE FROM follows
      WHERE follower_id = ?
      AND following_id = ?
      `
    )
      .bind(user.id, target.id)
      .run();

    return response(
      {
        success: true,
        following: false,
      },
      200,
      request,
      env
    );
  }

  /*
   * PROFILE UPDATE
   */
  if (path === "/api/profile" && method === "PATCH") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const body: any = await request
      .json()
      .catch(() => ({}));

    const displayName =
      body.displayName !== undefined
        ? String(body.displayName).trim()
        : user.display_name;

    const bio =
      body.bio !== undefined
        ? String(body.bio).trim()
        : user.bio;

    const avatarUrl =
      body.avatarUrl !== undefined
        ? String(body.avatarUrl).trim()
        : user.avatar_url;

    await env.DB.prepare(
      `
      UPDATE users
      SET
        display_name = ?,
        bio = ?,
        avatar_url = ?,
        updated_at = ?
      WHERE id = ?
      `
    )
      .bind(
        displayName,
        bio,
        avatarUrl,
        now(),
        user.id
      )
      .run();

    return response(
      {
        success: true,
        user: publicUser({
          ...user,
          display_name: displayName,
          bio,
          avatar_url: avatarUrl,
        }),
      },
      200,
      request,
      env
    );
  }

  /*
   * GIFTS
   */
  if (path === "/api/gifts" && method === "GET") {
    try {
      await ensureGifts(env);

      const result = await env.DB.prepare(
        `
        SELECT *
        FROM gifts
        WHERE enabled = 1
        ORDER BY price ASC
        `
      ).all();

      return response(
        {
          success: true,
          items: (result.results || []).map(
            publicGift
          ),
        },
        200,
        request,
        env
      );
    } catch {
      return response(
        {
          success: true,
          items: DEFAULT_GIFTS,
        },
        200,
        request,
        env
      );
    }
  }

  /*
   * CREATE LIVE
   */
  if (path === "/api/live/create" && method === "POST") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const body: any = await request
      .json()
      .catch(() => ({}));

    const title =
      String(body.title || "Wave Live").trim();

    if (!title) {
      return response(
        {
          success: false,
          message: "Live title is required",
        },
        400,
        request,
        env
      );
    }

    const liveId = id("live");

    const streamKey =
      crypto.randomUUID().replace(/-/g, "") +
      crypto.randomUUID().replace(/-/g, "");

    await env.DB.prepare(
      `
      INSERT INTO live_streams
      (
        id,
        user_id,
        title,
        stream_key,
        viewer_count,
        likes,
        status,
        started_at,
        created_at
      )
      VALUES (?, ?, ?, ?, 0, 0, 'active', ?, ?)
      `
    )
      .bind(
        liveId,
        user.id,
        title,
        streamKey,
        now(),
        now()
      )
      .run();

    return response(
      {
        success: true,
        message: "Live created",
        live: publicLive({
          id: liveId,
          user_id: user.id,
          username: user.username,
          display_name: user.display_name,
          avatar_url: user.avatar_url,
          title,
          stream_key: streamKey,
          viewer_count: 0,
          likes: 0,
          status: "active",
          started_at: now(),
        }),
      },
      201,
      request,
      env
    );
  }

  /*
   * GET LIVE
   */
  const liveMatch = path.match(
    /^\/api\/live\/([^/]+)$/
  );

  if (liveMatch && method === "GET") {
    const live = await env.DB.prepare(
      `
      SELECT
        l.*,
        u.username,
        u.display_name,
        u.avatar_url
      FROM live_streams l
      LEFT JOIN users u
        ON u.id = l.user_id
      WHERE l.id = ?
      LIMIT 1
      `
    )
      .bind(liveMatch[1])
      .first();

    if (!live) {
      return response(
        {
          success: false,
          message: "Live not found",
        },
        404,
        request,
        env
      );
    }

    return response(
      {
        success: true,
        live: publicLive(live),
      },
      200,
      request,
      env
    );
  }

  /*
   * UPDATE LIVE
   */
  if (liveMatch && method === "PATCH") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const live = await env.DB.prepare(
      `
      SELECT *
      FROM live_streams
      WHERE id = ?
      LIMIT 1
      `
    )
      .bind(liveMatch[1])
      .first();

    if (!live || live.user_id !== user.id) {
      return response(
        {
          success: false,
          message: "Live not found",
        },
        404,
        request,
        env
      );
    }

    const body: any = await request
      .json()
      .catch(() => ({}));

    const status =
      body.status !== undefined
        ? String(body.status)
        : live.status;

    const title =
      body.title !== undefined
        ? String(body.title)
        : live.title;

    await env.DB.prepare(
      `
      UPDATE live_streams
      SET
        status = ?,
        title = ?
      WHERE id = ?
      `
    )
      .bind(
        status,
        title,
        liveMatch[1]
      )
      .run();

    return response(
      {
        success: true,
        message: "Live updated",
      },
      200,
      request,
      env
    );
  }

  /*
   * SEND GIFT
   */
  const giftMatch = path.match(
    /^\/api\/live\/([^/]+)\/gifts$/
  );

  if (giftMatch && method === "POST") {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    const liveId = giftMatch[1];

    const body: any = await request
      .json()
      .catch(() => ({}));

    const giftId = String(body.giftId || "");
    const quantity = Math.max(
      1,
      Math.min(
        100,
        Number(body.quantity || 1)
      )
    );

    if (!giftId) {
      return response(
        {
          success: false,
          message: "Gift ID is required",
        },
        400,
        request,
        env
      );
    }

    const gift = await env.DB.prepare(
      `
      SELECT *
      FROM gifts
      WHERE id = ?
      AND enabled = 1
      LIMIT 1
      `
    )
      .bind(giftId)
      .first();

    if (!gift) {
      return response(
        {
          success: false,
          message: "Gift not found",
        },
        404,
        request,
        env
      );
    }

    const live = await env.DB.prepare(
      `
      SELECT *
      FROM live_streams
      WHERE id = ?
      LIMIT 1
      `
    )
      .bind(liveId)
      .first();

    if (!live) {
      return response(
        {
          success: false,
          message: "Live not found",
        },
        404,
        request,
        env
      );
    }

    const total =
      Number(gift.price || 0) * quantity;

    const currentCoins =
      Number(user.coins || 0);

    if (currentCoins < total) {
      return response(
        {
          success: false,
          message: "Not enough Wave Coins",
          remainingCoins: currentCoins,
        },
        400,
        request,
        env
      );
    }

    const receiverUserId =
      body.receiverUserId
        ? String(body.receiverUserId)
        : String(live.user_id);

    const transactionId = id("gift_tx");

    await env.DB.prepare(
      `
      UPDATE users
      SET coins = coins - ?
      WHERE id = ?
      AND coins >= ?
      `
    )
      .bind(
        total,
        user.id,
        total
      )
      .run();

    await env.DB.prepare(
      `
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
      `
    )
      .bind(
        transactionId,
        liveId,
        user.id,
        receiverUserId,
        gift.id,
        quantity,
        total,
        now()
      )
      .run();

    return response(
      {
        success: true,
        message: "Gift sent",
        remainingCoins:
          currentCoins - total,
        gift: publicGift(gift),
      },
      200,
      request,
      env
    );
  }

  /*
   * DIRECT UPLOAD
   *
   * Placeholder endpoint.
   * Actual Cloudflare Stream direct-upload credentials
   * should be added through Worker secrets.
   */
  if (
    path === "/api/upload/direct" &&
    method === "POST"
  ) {
    const user = await getUser(request, env);

    if (!user) {
      return response(
        {
          success: false,
          message: "Unauthorized",
        },
        401,
        request,
        env
      );
    }

    return response(
      {
        success: false,
        message:
          "Cloudflare Stream upload credentials are not configured yet.",
      },
      501,
      request,
      env
    );
  }

  /*
   * FALLBACK
   */
  return response(
    {
      success: false,
      message: "Not Found",
      path,
    },
    404,
    request,
    env
  );
}

export default {
  async fetch(
    request: Request,
    env: Env
  ): Promise<Response> {
    try {
      return await handleRequest(
        request,
        env
      );
    } catch (error) {
      return response(
        {
          success: false,
          message: "Internal Server Error",
          error: String(error),
        },
        500,
        request,
        env
      );
    }
  },
};
