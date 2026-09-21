export interface Env {
  DB?: D1Database;
  ENVIRONMENT?: string;
  SESSION_DAYS?: string;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Allow-Methods": "GET, POST, PATCH, DELETE, OPTIONS",
};

const WALLET_NUMBERS = [
  "01284306120",
  "01144210918",
];

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
    price: 10,
    icon: "❤️",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "crown",
    name: "Wave Crown",
    price: 100,
    icon: "👑",
    imageUrl: null,
    animationUrl: null,
  },
  {
    id: "diamond",
    name: "Diamond",
    price: 500,
    icon: "💎",
    imageUrl: null,
    animationUrl: null,
  },
];

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...CORS_HEADERS,
      "Content-Type": "application/json; charset=utf-8",
    },
  });
}

function makeId(prefix: string): string {
  return `${prefix}_${crypto.randomUUID().replaceAll("-", "")}`;
}

function getToken(request: Request): string {
  const authorization =
    request.headers.get("Authorization") || "";

  if (!authorization.startsWith("Bearer ")) {
    return "";
  }

  return authorization.substring(7).trim();
}

function getExpiresAt(env: Env): string {
  const days = Math.max(
    1,
    Number(env.SESSION_DAYS || "30")
  );

  return new Date(
    Date.now() + days * 24 * 60 * 60 * 1000
  ).toISOString();
}

function userJson(user: any) {
  return {
    id: String(user.id || ""),
    username: String(user.username || ""),
    displayName: String(
      user.display_name ??
        user.displayName ??
        ""
    ),
    avatar: user.avatar ?? null,
    bio: user.bio ?? null,
    coins: Number(user.coins || 0),
    followers: Number(user.followers || 0),
    following: Number(user.following || 0),
    verified:
      Number(user.verified || 0) === 1 ||
      user.verified === true,
  };
}

async function ensureSchema(
  db?: D1Database
): Promise<void> {
  if (!db) {
    return;
  }

  await db.batch([
    db.prepare(`
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
      )
    `),

    db.prepare(`
      CREATE TABLE IF NOT EXISTS sessions (
        token TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        expires_at TEXT NOT NULL,
        created_at TEXT NOT NULL
      )
    `),

    db.prepare(`
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
      )
    `),

    db.prepare(`
      CREATE TABLE IF NOT EXISTS likes (
        user_id TEXT NOT NULL,
        video_id TEXT NOT NULL,
        created_at TEXT NOT NULL,
        PRIMARY KEY (user_id, video_id)
      )
    `),

    db.prepare(`
      CREATE TABLE IF NOT EXISTS comments (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        video_id TEXT NOT NULL,
        text TEXT NOT NULL,
        created_at TEXT NOT NULL
      )
    `),

    db.prepare(`
      CREATE TABLE IF NOT EXISTS follows (
        follower_id TEXT NOT NULL,
        following_id TEXT NOT NULL,
        created_at TEXT NOT NULL,
        PRIMARY KEY (follower_id, following_id)
      )
    `),

    db.prepare(`
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
      )
    `),

    db.prepare(`
      CREATE TABLE IF NOT EXISTS gifts (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        price INTEGER NOT NULL,
        icon TEXT,
        image_url TEXT,
        animation_url TEXT
      )
    `),

    db.prepare(`
      CREATE TABLE IF NOT EXISTS gift_transactions (
        id TEXT PRIMARY KEY,
        live_id TEXT NOT NULL,
        sender_id TEXT NOT NULL,
        receiver_id TEXT,
        gift_id TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        total_coins INTEGER NOT NULL,
        created_at TEXT NOT NULL
      )
    `),

    db.prepare(`
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
      )
    `),
  ]);
}

async function seedGifts(
  db?: D1Database
): Promise<void> {
  if (!db) {
    return;
  }

  for (const gift of GIFTS) {
    await db
      .prepare(`
        INSERT OR IGNORE INTO gifts
        (
          id,
          name,
          price,
          icon,
          image_url,
          animation_url
        )
        VALUES (?, ?, ?, ?, ?, ?)
      `)
      .bind(
        gift.id,
        gift.name,
        gift.price,
        gift.icon,
        gift.imageUrl,
        gift.animationUrl
      )
      .run();
  }
}

async function getCurrentUser(
  request: Request,
  env: Env
): Promise<any | null> {
  const token = getToken(request);

  if (!token) {
    return null;
  }

  if (!env.DB) {
    return {
      id: "demo-user",
      username: "wave_user",
      display_name: "Wave User",
      avatar: null,
      bio: "",
      coins: 1000,
      followers: 0,
      following: 0,
      verified: 0,
    };
  }

  const row =
    await env.DB
      .prepare(`
        SELECT
          u.*
        FROM sessions s
        JOIN users u
          ON u.id = s.user_id
        WHERE
          s.token = ?
          AND s.expires_at > ?
        LIMIT 1
      `)
      .bind(
        token,
        new Date().toISOString()
      )
      .first();

  return row || null;
}

function parseVideo(video: any) {
  if (!video?.id) {
    return null;
  }

  return {
    id: String(video.id),
    userId: String(video.user_id || ""),
    username: String(video.username || ""),
    displayName: String(
      video.display_name || ""
    ),
    avatar: video.avatar ?? null,
    videoUrl: String(
      video.video_url || ""
    ),
    thumbnailUrl:
      video.thumbnail_url ?? null,
    caption: String(
      video.caption || ""
    ),
    musicName:
      video.music_name ?? null,
    likes: Number(video.likes || 0),
    comments: Number(
      video.comments || 0
    ),
    shares: Number(
      video.shares || 0
    ),
    views: Number(
      video.views || 0
    ),
    liked: Boolean(
      video.liked || false
    ),
    createdAt:
      video.created_at ?? null,
  };
}

function parseLive(live: any) {
  if (!live?.id) {
    return null;
  }

  return {
    id: String(live.id),
    userId: String(
      live.user_id || ""
    ),
    username: String(
      live.username || ""
    ),
    displayName: String(
      live.display_name || ""
    ),
    avatar:
      live.avatar ?? null,
    title: String(
      live.title || ""
    ),
    streamUrl:
      live.stream_url ?? null,
    playbackUrl:
      live.playback_url ?? null,
    rtmpsUrl:
      live.rtmps_url ?? null,
    streamKey:
      live.stream_key ?? null,
    viewerCount: Number(
      live.viewer_count || 0
    ),
    likes: Number(
      live.likes || 0
    ),
    status: String(
      live.status || "active"
    ),
    startedAt:
      live.started_at ?? null,
  };
}

async function handleRequest(
  request: Request,
  env: Env
): Promise<Response> {
  if (request.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: CORS_HEADERS,
    });
  }

  const url = new URL(
    request.url
  );

  const path =
    url.pathname.replace(/\/+$/, "") ||
    "/";

  const method =
    request.method.toUpperCase();

  try {
    await ensureSchema(env.DB);
    await seedGifts(env.DB);

    /*
     * HEALTH
     */

    if (
      method === "GET" &&
      (path === "/" ||
        path === "/health")
    ) {
      return json({
        success: true,
        service: "Wave Server",
        status: "ok",
        environment:
          env.ENVIRONMENT ||
          "production",
        database:
          Boolean(env.DB),
      });
    }

    /*
     * SESSION
     */

    if (
      method === "POST" &&
      path === "/api/session"
    ) {
      const now =
        new Date();

      const expiresAt =
        getExpiresAt(env);

      const token =
        makeId("sess");

      const userId =
        makeId("usr");

      if (env.DB) {
        const username =
          "wave_" +
          crypto.randomUUID()
            .slice(0, 8);

        await env.DB
          .prepare(`
            INSERT INTO users
            (
              id,
              username,
              display_name,
              coins,
              created_at
            )
            VALUES (?, ?, ?, ?, ?)
          `)
          .bind(
            userId,
            username,
            "Wave User",
            1000,
            now.toISOString()
          )
          .run();

        await env.DB
          .prepare(`
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
            expiresAt,
            now.toISOString()
          )
          .run();

        const user =
          await env.DB
            .prepare(
              "SELECT * FROM users WHERE id = ?"
            )
            .bind(userId)
            .first();

        return json({
          success: true,
          token,
          expiresAt,
          user: userJson(user),
        });
      }

      return json({
        success: true,
        token,
        expiresAt,
        user: userJson({
          id: "demo-user",
          username: "wave_user",
          display_name:
            "Wave User",
          coins: 1000,
        }),
      });
    }

    /*
     * CURRENT USER
     */

    if (
      method === "GET" &&
      path === "/api/me"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      return json({
        success: true,
        user: userJson(user),
      });
    }

    /*
     * GIFTS
     */

    if (
      method === "GET" &&
      path === "/api/gifts"
    ) {
      return json({
        success: true,
        items: GIFTS,
      });
    }

    /*
     * FEED
     */

    if (
      method === "GET" &&
      path === "/api/feed"
    ) {
      if (!env.DB) {
        return json({
          success: true,
          items: [],
        });
      }

      const limit =
        Math.min(
          Math.max(
            Number(
              url.searchParams.get(
                "limit"
              ) || 20
            ),
            1
          ),
          50
        );

      const rows =
        await env.DB
          .prepare(`
            SELECT
              v.*,
              u.username,
              u.display_name,
              u.avatar
            FROM videos v
            LEFT JOIN users u
              ON u.id = v.user_id
            ORDER BY
              v.created_at DESC
            LIMIT ?
          `)
          .bind(limit)
          .all();

      return json({
        success: true,
        items:
          (rows.results || [])
            .map(parseVideo)
            .filter(Boolean),
      });
    }

    /*
     * CREATE VIDEO
     */

    if (
      method === "POST" &&
      path === "/api/videos"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      const body =
        await request.json<any>();

      const videoUrl =
        String(
          body.videoUrl || ""
        ).trim();

      if (!videoUrl) {
        return json(
          {
            success: false,
            message:
              "videoUrl is required",
          },
          400
        );
      }

      const videoId =
        makeId("vid");

      if (env.DB) {
        await env.DB
          .prepare(`
            INSERT INTO videos
            (
              id,
              user_id,
              video_url,
              thumbnail_url,
              caption,
              music_name,
              created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
          `)
          .bind(
            videoId,
            user.id,
            videoUrl,
            body.thumbnailUrl ||
              null,
            String(
              body.caption || ""
            ),
            body.musicName ||
              null,
            new Date().toISOString()
          )
          .run();
      }

      return json({
        success: true,
        id: videoId,
      });
    }

    /*
     * VIDEO ROUTES
     */

    const videoMatch =
      path.match(
        /^\/api\/videos\/([^/]+)(?:\/(like|comments))?$/
      );

    if (videoMatch) {
      const videoId =
        decodeURIComponent(
          videoMatch[1]
        );

      const action =
        videoMatch[2] ||
        "";

      const user =
        await getCurrentUser(
          request,
          env
        );

      /*
       * LIKE
       */

      if (
        action === "like" &&
        method === "POST"
      ) {
        if (!user) {
          return json(
            {
              success: false,
              message:
                "Unauthorized",
            },
            401
          );
        }

        if (env.DB) {
          await env.DB
            .prepare(`
              INSERT OR IGNORE INTO likes
              (
                user_id,
                video_id,
                created_at
              )
              VALUES (?, ?, ?)
            `)
            .bind(
              user.id,
              videoId,
              new Date().toISOString()
            )
            .run();

          await env.DB
            .prepare(`
              UPDATE videos
              SET likes =
                (
                  SELECT COUNT(*)
                  FROM likes
                  WHERE video_id = ?
                )
              WHERE id = ?
            `)
            .bind(
              videoId,
              videoId
            )
            .run();

          const video =
            await env.DB
              .prepare(
                "SELECT likes FROM videos WHERE id = ?"
              )
              .bind(videoId)
              .first<any>();

          return json({
            success: true,
            likes: Number(
              video?.likes || 0
            ),
          });
        }

        return json({
          success: true,
          likes: 1,
        });
      }

      /*
       * UNLIKE
       */

      if (
        action === "like" &&
        method === "DELETE"
      ) {
        if (!user) {
          return json(
            {
              success: false,
              message:
                "Unauthorized",
            },
            401
          );
        }

        if (env.DB) {
          await env.DB
            .prepare(`
              DELETE FROM likes
              WHERE
                user_id = ?
                AND video_id = ?
            `)
            .bind(
              user.id,
              videoId
            )
            .run();

          await env.DB
            .prepare(`
              UPDATE videos
              SET likes =
                (
                  SELECT COUNT(*)
                  FROM likes
                  WHERE video_id = ?
                )
              WHERE id = ?
            `)
            .bind(
              videoId,
              videoId
            )
            .run();

          const video =
            await env.DB
              .prepare(
                "SELECT likes FROM videos WHERE id = ?"
              )
              .bind(videoId)
              .first<any>();

          return json({
            success: true,
            likes: Number(
              video?.likes || 0
            ),
          });
        }

        return json({
          success: true,
          likes: 0,
        });
      }

      /*
       * COMMENTS - GET
       */

      if (
        action === "comments" &&
        method === "GET"
      ) {
        if (!env.DB) {
          return json({
            success: true,
            items: [],
          });
        }

        const rows =
          await env.DB
            .prepare(`
              SELECT
                c.*,
                u.username,
                u.display_name
              FROM comments c
              LEFT JOIN users u
                ON u.id = c.user_id
              WHERE c.video_id = ?
              ORDER BY
                c.created_at DESC
            `)
            .bind(videoId)
            .all();

        return json({
          success: true,
          items:
            rows.results || [],
        });
      }

      /*
       * COMMENTS - POST
       */

      if (
        action === "comments" &&
        method === "POST"
      ) {
        if (!user) {
          return json(
            {
              success: false,
              message:
                "Unauthorized",
            },
            401
          );
        }

        const body =
          await request.json<any>();

        const commentText =
          String(
            body.text || ""
          ).trim();

        if (!commentText) {
          return json(
            {
              success: false,
              message:
                "Comment cannot be empty",
            },
            400
          );
        }

        const commentId =
          makeId("com");

        if (env.DB) {
          await env.DB
            .prepare(`
              INSERT INTO comments
              (
                id,
                user_id,
                video_id,
                text,
                created_at
              )
              VALUES (?, ?, ?, ?, ?)
            `)
            .bind(
              commentId,
              user.id,
              videoId,
              commentText,
              new Date().toISOString()
            )
            .run();
        }

        return json({
          success: true,
          id: commentId,
        });
      }
    }

    /*
     * FOLLOW USER
     */

    const followMatch =
      path.match(
        /^\/api\/users\/([^/]+)\/follow$/
      );

    if (followMatch) {
      const targetUserId =
        decodeURIComponent(
          followMatch[1]
        );

      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      if (
        targetUserId ===
        user.id
      ) {
        return json(
          {
            success: false,
            message:
              "Cannot follow yourself",
          },
          400
        );
      }

      if (
        method === "POST"
      ) {
        if (env.DB) {
          await env.DB
            .prepare(`
              INSERT OR IGNORE INTO follows
              (
                follower_id,
                following_id,
                created_at
              )
              VALUES (?, ?, ?)
            `)
            .bind(
              user.id,
              targetUserId,
              new Date().toISOString()
            )
            .run();
        }

        return json({
          success: true,
          following: true,
        });
      }

      if (
        method === "DELETE"
      ) {
        if (env.DB) {
          await env.DB
            .prepare(`
              DELETE FROM follows
              WHERE
                follower_id = ?
                AND following_id = ?
            `)
            .bind(
              user.id,
              targetUserId
            )
            .run();
        }

        return json({
          success: true,
          following: false,
        });
      }
    }

    /*
     * LIVE CREATE
     */

    if (
      method === "POST" &&
      path === "/api/live/create"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      const body =
        await request.json<any>();

      const liveId =
        makeId("live");

      const title =
        String(
          body.title ||
            "Wave Live"
        ).trim();

      const startedAt =
        new Date().toISOString();

      const streamUrl =
        body.streamUrl ||
        null;

      const playbackUrl =
        body.playbackUrl ||
        null;

      const rtmpsUrl =
        body.rtmpsUrl ||
        null;

      const streamKey =
        body.streamKey ||
        null;

      if (env.DB) {
        await env.DB
          .prepare(`
            INSERT INTO live
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
              started_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 'active', ?)
          `)
          .bind(
            liveId,
            user.id,
            title,
            streamUrl,
            playbackUrl,
            rtmpsUrl,
            streamKey,
            startedAt
          )
          .run();

        const row =
          await env.DB
            .prepare(`
              SELECT
                l.*,
                u.username,
                u.display_name,
                u.avatar
              FROM live l
              LEFT JOIN users u
                ON u.id = l.user_id
              WHERE l.id = ?
            `)
            .bind(liveId)
            .first();

        return json({
          success: true,
          live: parseLive(row),
        });
      }

      return json({
        success: true,
        live: {
          id: liveId,
          userId: user.id,
          username:
            user.username,
          displayName:
            user.display_name,
          avatar:
            user.avatar ||
            null,
          title,
          streamUrl,
          playbackUrl,
          rtmpsUrl,
          streamKey,
          viewerCount: 0,
          likes: 0,
          status: "active",
          startedAt,
        },
      });
    }

    /*
     * LIVE GET
     */

    const liveMatch =
      path.match(
        /^\/api\/live\/([^/]+)$/
      );

    if (
      liveMatch &&
      method === "GET"
    ) {
      const liveId =
        decodeURIComponent(
          liveMatch[1]
        );

      if (!env.DB) {
        return json(
          {
            success: false,
            message:
              "Live not found",
          },
          404
        );
      }

      const row =
        await env.DB
          .prepare(`
            SELECT
              l.*,
              u.username,
              u.display_name,
              u.avatar
            FROM live l
            LEFT JOIN users u
              ON u.id = l.user_id
            WHERE l.id = ?
          `)
          .bind(liveId)
          .first();

      const live =
        parseLive(row);

      if (!live) {
        return json(
          {
            success: false,
            message:
              "Live not found",
          },
          404
        );
      }

      return json({
        success: true,
        live,
      });
    }

    /*
     * LIVE UPDATE
     */

    if (
      liveMatch &&
      method === "PATCH"
    ) {
      const liveId =
        decodeURIComponent(
          liveMatch[1]
        );

      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      const body =
        await request.json<any>();

      const status =
        body.status ??
        null;

      const title =
        body.title ??
        null;

      if (env.DB) {
        const current =
          await env.DB
            .prepare(
              "SELECT * FROM live WHERE id = ?"
            )
            .bind(liveId)
            .first<any>();

        if (
          !current ||
          current.user_id !==
            user.id
        ) {
          return json(
            {
              success: false,
              message:
                "Live not found",
            },
            404
          );
        }

        await env.DB
          .prepare(`
            UPDATE live
            SET
              status = COALESCE(?, status),
              title = COALESCE(?, title)
            WHERE id = ?
          `)
          .bind(
            status,
            title,
            liveId
          )
          .run();

        const row =
          await env.DB
            .prepare(`
              SELECT
                l.*,
                u.username,
                u.display_name,
                u.avatar
              FROM live l
              LEFT JOIN users u
                ON u.id = l.user_id
              WHERE l.id = ?
            `)
            .bind(liveId)
            .first();

        return json({
          success: true,
          live: parseLive(row),
        });
      }

      return json({
        success: true,
        live: {
          id: liveId,
          userId: user.id,
          username:
            user.username,
          displayName:
            user.display_name,
          avatar:
            user.avatar ||
            null,
          title:
            title ||
            "Wave Live",
          streamUrl: null,
          playbackUrl: null,
          rtmpsUrl: null,
          streamKey: null,
          viewerCount: 0,
          likes: 0,
          status:
            status ||
            "active",
          startedAt:
            new Date().toISOString(),
        },
      });
    }

    /*
     * SEND GIFT
     */

    const giftMatch =
      path.match(
        /^\/api\/live\/([^/]+)\/gifts$/
      );

    if (
      giftMatch &&
      method === "POST"
    ) {
      const liveId =
        decodeURIComponent(
          giftMatch[1]
        );

      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      const body =
        await request.json<any>();

      const giftId =
        String(
          body.giftId || ""
        );

      const quantity =
        Math.min(
          Math.max(
            Number(
              body.quantity || 1
            ),
            1
          ),
          100
        );

      const gift =
        GIFTS.find(
          item =>
            item.id ===
            giftId
        );

      if (!gift) {
        return json(
          {
            success: false,
            message:
              "Gift not found",
          },
          404
        );
      }

      const totalCoins =
        gift.price *
        quantity;

      if (
        env.DB
      ) {
        const sender =
          await env.DB
            .prepare(
              "SELECT * FROM users WHERE id = ?"
            )
            .bind(user.id)
            .first<any>();

        if (
          !sender ||
          Number(sender.coins || 0) <
            totalCoins
        ) {
          return json(
            {
              success: false,
              message:
                "Insufficient coins",
            },
            400
          );
        }

        const live =
          await env.DB
            .prepare(
              "SELECT * FROM live WHERE id = ?"
            )
            .bind(liveId)
            .first<any>();

        if (!live) {
          return json(
            {
              success: false,
              message:
                "Live not found",
            },
            404
          );
        }

        const transactionId =
          makeId("gift");

        const receiverUserId =
          body.receiverUserId ||
          live.user_id ||
          null;

        await env.DB.batch([
          env.DB
            .prepare(`
              UPDATE users
              SET coins =
                coins - ?
              WHERE id = ?
            `)
            .bind(
              totalCoins,
              user.id
            ),

          env.DB
            .prepare(`
              INSERT INTO gift_transactions
              (
                id,
                live_id,
                sender_id,
                receiver_id,
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
              user.id,
              receiverUserId,
              gift.id,
              quantity,
              totalCoins,
              new Date().toISOString()
            ),
        ]);

        const updated =
          await env.DB
            .prepare(
              "SELECT coins FROM users WHERE id = ?"
            )
            .bind(user.id)
            .first<any>();

        return json({
          success: true,
          transactionId,
          remainingCoins:
            Number(
              updated?.coins || 0
            ),
          gift: {
            id: gift.id,
            name: gift.name,
            quantity,
            totalCoins,
          },
        });
      }

      return json({
        success: true,
        transactionId:
          makeId("gift"),
        remainingCoins:
          Math.max(
            0,
            1000 - totalCoins
          ),
        gift: {
          id: gift.id,
          name: gift.name,
          quantity,
          totalCoins,
        },
      });
    }

    /*
     * WALLET
     */

    if (
      method === "GET" &&
      path === "/api/wallet"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      return json({
        success: true,
        coins: Number(
          user.coins || 0
        ),
        walletNumbers:
          WALLET_NUMBERS,
        paymentMethods: [
          "wallet",
        ],
      });
    }

    /*
     * WALLET DEPOSIT
     *
     * الطلب يبقى pending.
     * لا يتم اعتماد العملات
     * تلقائيًا قبل التحقق من الدفع.
     */

    if (
      method === "POST" &&
      path ===
        "/api/wallet/deposit"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      const body =
        await request.json<any>();

      const amount =
        Math.floor(
          Number(
            body.amount || 0
          )
        );

      const walletNumber =
        String(
          body.walletNumber ||
            ""
        ).trim();

      const reference =
        body.transactionReference
          ? String(
              body.transactionReference
            ).trim()
          : null;

      if (
        amount <= 0 ||
        !walletNumber
      ) {
        return json(
          {
            success: false,
            message:
              "Invalid deposit",
          },
          400
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
          400
        );
      }

      const depositId =
        makeId("dep");

      const now =
        new Date().toISOString();

      /*
       * القيمة الحالية:
       * 1 جنيه = 1 Coin.
       * يمكن تغييرها لاحقًا
       * من منطق السيرفر.
       */
      const coins =
        amount;

      if (env.DB) {
        await env.DB
          .prepare(`
            INSERT INTO deposits
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
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          `)
          .bind(
            depositId,
            user.id,
            amount,
            walletNumber,
            reference,
            coins,
            "pending",
            now,
            now
          )
          .run();
      }

      return json({
        success: true,
        depositId,
        status: "pending",
        coins,
      });
    }

    /*
     * WALLET DEPOSITS
     */

    if (
      method === "GET" &&
      path ===
        "/api/wallet/deposits"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      if (!env.DB) {
        return json({
          success: true,
          items: [],
        });
      }

      const rows =
        await env.DB
          .prepare(`
            SELECT *
            FROM deposits
            WHERE user_id = ?
            ORDER BY
              created_at DESC
          `)
          .bind(user.id)
          .all();

      return json({
        success: true,
        items:
          rows.results || [],
      });
    }

    /*
     * REPORT
     */

    if (
      method === "POST" &&
      path === "/api/reports"
    ) {
      const user =
        await getCurrentUser(
          request,
          env
        );

      if (!user) {
        return json(
          {
            success: false,
            message:
              "Unauthorized",
          },
          401
        );
      }

      const body =
        await request.json<any>();

      if (
        !body.targetType ||
        !body.targetId ||
        !body.reason
      ) {
        return json(
          {
            success: false,
            message:
              "Missing report fields",
          },
          400
        );
      }

      return json({
        success: true,
        reportId:
          makeId("report"),
      });
    }

    /*
     * NOT FOUND
     */

    return json(
      {
        success: false,
        message: "Not Found",
        path,
      },
      404
    );
  } catch (error) {
    return json(
      {
        success: false,
        message:
          error instanceof Error
            ? error.message
            : "Server error",
      },
      500
    );
  }
}

export default {
  fetch: handleRequest,
};
