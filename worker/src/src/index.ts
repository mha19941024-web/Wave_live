export interface Env {
  DB: D1Database;

  ENVIRONMENT?: string;

  CLOUDFLARE_ACCOUNT_ID?: string;
  CLOUDFLARE_API_TOKEN?: string;
  STREAM_CUSTOMER_SUBDOMAIN?: string;

  SESSION_DAYS?: string;
  CORS_ORIGIN?: string;
}

type VideoRow = {
  id: string;
  url: string;
  user: string;
  caption: string;
  likes: number;
  created_at: string;
};

type GiftRow = {
  id: string;
  name: string;
  icon: string;
  priceCoins: number;
};

const bearer = (request: Request) => {
  const header = request.headers.get("Authorization") ?? "";

  if (!header.startsWith("Bearer ")) {
    return null;
  }

  return header.slice(7).trim() || null;
};

const createUsername = () =>
  `user_${crypto.randomUUID().replaceAll("-", "").slice(0, 10)}`;

const json = (data: unknown, status = 200) =>
  Response.json(data, {
    status,
    headers: {
      "Cache-Control": "no-store",
    },
  });

function cors(response: Response, env: Env) {
  response.headers.set(
    "Access-Control-Allow-Origin",
    env.CORS_ORIGIN ?? "*"
  );

  response.headers.set(
    "Access-Control-Allow-Headers",
    "Content-Type, Authorization"
  );

  response.headers.set(
    "Access-Control-Allow-Methods",
    "GET,POST,PATCH,DELETE,OPTIONS"
  );

  return response;
}

const out = (
  data: unknown,
  status = 200,
  env?: Env
): Response => {
  const response = json(data, status);
  return env ? cors(response, env) : response;
};

async function auth(
  request: Request,
  env: Env
): Promise<string | null> {
  const token = bearer(request);

  if (!token) {
    return null;
  }

  const row = await env.DB
    .prepare(
      `SELECT user_id
       FROM sessions
       WHERE token = ?
       AND expires_at > ?`
    )
    .bind(token, new Date().toISOString())
    .first<{ user_id: string }>();

  return row?.user_id ?? null;
}

function cloudflareHeaders(env: Env) {
  return {
    Authorization: `Bearer ${env.CLOUDFLARE_API_TOKEN}`,
    "Content-Type": "application/json",
  };
}

function isCloudflareConfigured(env: Env) {
  return Boolean(
    env.CLOUDFLARE_ACCOUNT_ID &&
    env.CLOUDFLARE_API_TOKEN
  );
}

export default {
  async fetch(
    request: Request,
    env: Env
  ): Promise<Response> {

    if (request.method === "OPTIONS") {
      return cors(
        new Response(null, { status: 204 }),
        env
      );
    }

    const url = new URL(request.url);

    try {

      /*
       * HEALTH
       */

      if (
        url.pathname === "/health" &&
        request.method === "GET"
      ) {
        return out(
          {
            ok: true,
            service: "vyro-api",
            version: "1.0.0",
            environment: env.ENVIRONMENT ?? "unknown",
          },
          200,
          env
        );
      }

      /*
       * CREATE SESSION
       */

      if (
        url.pathname === "/api/session" &&
        request.method === "POST"
      ) {
        const userId = crypto.randomUUID();

        const token =
          crypto.randomUUID() +
          crypto.randomUUID();

        const now = new Date();

        const days = Math.min(
          Math.max(
            Number(env.SESSION_DAYS ?? 30),
            1
          ),
          90
        );

        const expires = new Date(
          now.getTime() +
            days * 86400000
        ).toISOString();

        const user = createUsername();

        await env.DB.batch([
          env.DB
            .prepare(
              `INSERT INTO users
              (
                id,
                username,
                display_name,
                bio,
                avatar_url,
                created_at
              )
              VALUES (?, ?, ?, ?, ?, ?)`
            )
            .bind(
              userId,
              user,
              "",
              "",
              "",
              now.toISOString()
            ),

          env.DB
            .prepare(
              `INSERT INTO sessions
              (
                token,
                user_id,
                created_at,
                expires_at
              )
              VALUES (?, ?, ?, ?)`
            )
            .bind(
              token,
              userId,
              now.toISOString(),
              expires
            ),

          env.DB
            .prepare(
              `INSERT INTO wallets
              (
                user_id,
                coins,
                updated_at
              )
              VALUES (?, ?, ?)`
            )
            .bind(
              userId,
              0,
              now.toISOString()
            ),
        ]);

        return out(
          {
            token,

            user: {
              id: userId,
              username: user,
              displayName: "",
              bio: "",
              avatarUrl: "",
            },

            wallet: {
              coins: 0,
            },

            expiresAt: expires,
          },
          201,
          env
        );
      }

      const userId = await auth(request, env);

      /*
       * CURRENT USER
       */

      if (
        url.pathname === "/api/me" &&
        request.method === "GET"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const user = await env.DB
          .prepare(
            `SELECT
              id,
              username,
              display_name AS displayName,
              bio,
              avatar_url AS avatarUrl,
              created_at
             FROM users
             WHERE id = ?`
          )
          .bind(userId)
          .first();

        if (!user) {
          return out(
            { error: "user not found" },
            404,
            env
          );
        }

        const wallet = await env.DB
          .prepare(
            `SELECT coins
             FROM wallets
             WHERE user_id = ?`
          )
          .bind(userId)
          .first<{ coins: number }>();

        return out(
          {
            user,
            wallet: {
              coins: wallet?.coins ?? 0,
            },
          },
          200,
          env
        );
      }

      /*
       * FEED
       */

      if (
        url.pathname === "/api/feed" &&
        request.method === "GET"
      ) {
        const limit = Math.min(
          Math.max(
            Number(
              url.searchParams.get("limit") ?? 20
            ),
            1
          ),
          50
        );

        const cursor = Math.max(
          Number(
            url.searchParams.get("cursor") ?? 0
          ),
          0
        );

        const rows = await env.DB
          .prepare(
            `SELECT
              v.id,
              v.url,
              v.user,
              v.caption,
              v.likes,
              v.created_at
             FROM videos v
             LEFT JOIN users u
               ON u.username = v.user
             WHERE
               u.id IS NULL
               OR NOT EXISTS (
                 SELECT 1
                 FROM blocks b
                 WHERE b.blocker_id = ?
                 AND b.blocked_id = u.id
               )
             ORDER BY v.created_at DESC
             LIMIT ?
             OFFSET ?`
          )
          .bind(
            userId ?? "",
            limit,
            cursor
          )
          .all<VideoRow>();

        return out(
          {
            items: rows.results,

            nextCursor:
              rows.results.length === limit
                ? cursor + rows.results.length
                : null,
          },
          200,
          env
        );
      }

      /*
       * CREATE VIDEO
       */

      if (
        url.pathname === "/api/videos" &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const body =
          (await request.json()) as {
            url?: unknown;
            streamId?: unknown;
            caption?: unknown;
          };

        let playback =
          typeof body.url === "string"
            ? body.url.trim()
            : "";

        if (
          !playback &&
          typeof body.streamId === "string" &&
          env.STREAM_CUSTOMER_SUBDOMAIN
        ) {
          playback =
            `https://${env.STREAM_CUSTOMER_SUBDOMAIN}` +
            `.cloudflarestream.com/` +
            `${encodeURIComponent(body.streamId)}` +
            `/manifest/video.m3u8`;
        }

        if (!playback) {
          return out(
            {
              error:
                "url or streamId is required",
            },
            400,
            env
          );
        }

        const user = await env.DB
          .prepare(
            `SELECT username AS user
             FROM users
             WHERE id = ?`
          )
          .bind(userId)
          .first<{ user: string }>();

        const id = crypto.randomUUID();

        const now =
          new Date().toISOString();

        const caption =
          typeof body.caption === "string"
            ? body.caption.trim().slice(0, 500)
            : "";

        await env.DB
          .prepare(
            `INSERT INTO videos
            (
              id,
              url,
              user,
              caption,
              likes,
              created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)`
          )
          .bind(
            id,
            playback,
            user?.user ?? "user",
            caption,
            0,
            now
          )
          .run();

        return out(
          {
            id,
            url: playback,
            user: user?.user ?? "user",
            caption,
            likes: 0,
            created_at: now,
          },
          201,
          env
        );
      }

      /*
       * VIDEO LIKE
       */

      const likeMatch =
        url.pathname.match(
          /^\/api\/videos\/([^/]+)\/like$/
        );

      if (
        likeMatch &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const videoId = likeMatch[1];

        const video = await env.DB
          .prepare(
            `SELECT id
             FROM videos
             WHERE id = ?`
          )
          .bind(videoId)
          .first();

        if (!video) {
          return out(
            { error: "video not found" },
            404,
            env
          );
        }

        const result = await env.DB
          .prepare(
            `INSERT OR IGNORE INTO video_likes
            (
              video_id,
              user_id,
              created_at
            )
            VALUES (?, ?, ?)`
          )
          .bind(
            videoId,
            userId,
            new Date().toISOString()
          )
          .run();

        if (result.meta.changes) {
          await env.DB
            .prepare(
              `UPDATE videos
               SET likes = likes + 1
               WHERE id = ?`
            )
            .bind(videoId)
            .run();
        }

        const row = await env.DB
          .prepare(
            `SELECT likes
             FROM videos
             WHERE id = ?`
          )
          .bind(videoId)
          .first<{ likes: number }>();

        return out(
          {
            id: videoId,
            likes: row?.likes ?? 0,
            liked: true,
          },
          200,
          env
        );
      }

      if (
        likeMatch &&
        request.method === "DELETE"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const videoId = likeMatch[1];

        const result = await env.DB
          .prepare(
            `DELETE FROM video_likes
             WHERE video_id = ?
             AND user_id = ?`
          )
          .bind(
            videoId,
            userId
          )
          .run();

        if (result.meta.changes) {
          await env.DB
            .prepare(
              `UPDATE videos
               SET likes = MAX(likes - 1, 0)
               WHERE id = ?`
            )
            .bind(videoId)
            .run();
        }

        const row = await env.DB
          .prepare(
            `SELECT likes
             FROM videos
             WHERE id = ?`
          )
          .bind(videoId)
          .first<{ likes: number }>();

        return out(
          {
            id: videoId,
            likes: row?.likes ?? 0,
            liked: false,
          },
          200,
          env
        );
      }

      /*
       * FOLLOW / UNFOLLOW
       */

      const followMatch =
        url.pathname.match(
          /^\/api\/users\/([^/]+)\/follow$/
        );

      if (
        followMatch &&
        (
          request.method === "POST" ||
          request.method === "DELETE"
        )
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const target = await env.DB
          .prepare(
            `SELECT id
             FROM users
             WHERE id = ?
             OR username = ?`
          )
          .bind(
            followMatch[1],
            followMatch[1]
          )
          .first<{ id: string }>();

        if (!target) {
          return out(
            { error: "user not found" },
            404,
            env
          );
        }

        if (target.id === userId) {
          return out(
            {
              error:
                "cannot follow yourself",
            },
            400,
            env
          );
        }

        if (request.method === "POST") {
          await env.DB
            .prepare(
              `INSERT OR IGNORE INTO follows
              (
                follower_id,
                following_id,
                created_at
              )
              VALUES (?, ?, ?)`
            )
            .bind(
              userId,
              target.id,
              new Date().toISOString()
            )
            .run();
        } else {
          await env.DB
            .prepare(
              `DELETE FROM follows
               WHERE follower_id = ?
               AND following_id = ?`
            )
            .bind(
              userId,
              target.id
            )
            .run();
        }

        return out(
          {
            following:
              request.method === "POST",

            userId: target.id,
          },
          200,
          env
        );
      }

      /*
       * PROFILE UPDATE
       */

      if (
        url.pathname === "/api/profile" &&
        request.method === "PATCH"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const body =
          (await request.json()) as {
            displayName?: unknown;
            bio?: unknown;
            avatarUrl?: unknown;
          };

        const displayName =
          typeof body.displayName === "string"
            ? body.displayName.trim().slice(0, 80)
            : null;

        const bio =
          typeof body.bio === "string"
            ? body.bio.trim().slice(0, 500)
            : null;

        const avatarUrl =
          typeof body.avatarUrl === "string"
            ? body.avatarUrl.trim().slice(0, 500)
            : null;

        await env.DB
          .prepare(
            `UPDATE users
             SET
               display_name =
                 COALESCE(?, display_name),
               bio =
                 COALESCE(?, bio),
               avatar_url =
                 COALESCE(?, avatar_url)
             WHERE id = ?`
          )
          .bind(
            displayName,
            bio,
            avatarUrl,
            userId
          )
          .run();

        const user = await env.DB
          .prepare(
            `SELECT
              id,
              username,
              display_name AS displayName,
              bio,
              avatar_url AS avatarUrl
             FROM users
             WHERE id = ?`
          )
          .bind(userId)
          .first();

        return out(
          { user },
          200,
          env
        );
      }

      /*
       * COMMENTS
       */

      const commentsMatch =
        url.pathname.match(
          /^\/api\/videos\/([^/]+)\/comments$/
        );

      if (
        commentsMatch &&
        request.method === "GET"
      ) {
        const limit = Math.min(
          Math.max(
            Number(
              url.searchParams.get("limit") ?? 30
            ),
            1
          ),
          100
        );

        const rows = await env.DB
          .prepare(
            `SELECT
              c.id,
              c.text,
              c.created_at,
              u.username AS user,
              u.avatar_url AS avatarUrl
             FROM comments c
             JOIN users u
               ON u.id = c.user_id
             WHERE c.video_id = ?
             ORDER BY c.created_at DESC
             LIMIT ?`
          )
          .bind(
            commentsMatch[1],
            limit
          )
          .all();

        return out(
          {
            items: rows.results,
          },
          200,
          env
        );
      }

      if (
        commentsMatch &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const body =
          (await request.json()) as {
            text?: unknown;
          };

        if (
          typeof body.text !== "string" ||
          !body.text.trim() ||
          body.text.length > 500
        ) {
          return out(
            {
              error:
                "comment must be 1-500 chars",
            },
            400,
            env
          );
        }

        const video = await env.DB
          .prepare(
            `SELECT id
             FROM videos
             WHERE id = ?`
          )
          .bind(commentsMatch[1])
          .first();

        if (!video) {
          return out(
            { error: "video not found" },
            404,
            env
          );
        }

        const id = crypto.randomUUID();

        const now =
          new Date().toISOString();

        const text =
          body.text.trim();

        await env.DB
          .prepare(
            `INSERT INTO comments
            (
              id,
              video_id,
              user_id,
              text,
              created_at
            )
            VALUES (?, ?, ?, ?, ?)`
          )
          .bind(
            id,
            commentsMatch[1],
            userId,
            text,
            now
          )
          .run();

        const user = await env.DB
          .prepare(
            `SELECT
              username AS user,
              avatar_url AS avatarUrl
             FROM users
             WHERE id = ?`
          )
          .bind(userId)
          .first();

        return out(
          {
            id,
            text,
            created_at: now,
            ...user,
          },
          201,
          env
        );
      }

      /*
       * ACCOUNT DELETE
       */

      if (
        url.pathname === "/api/account" &&
        request.method === "DELETE"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        await env.DB.batch([
          env.DB
            .prepare(
              `DELETE FROM comments
               WHERE user_id = ?`
            )
            .bind(userId),

          env.DB
            .prepare(
              `DELETE FROM video_likes
               WHERE user_id = ?`
            )
            .bind(userId),

          env.DB
            .prepare(
              `DELETE FROM follows
               WHERE follower_id = ?
               OR following_id = ?`
            )
            .bind(
              userId,
              userId
            ),

          env.DB
            .prepare(
              `DELETE FROM blocks
               WHERE blocker_id = ?
               OR blocked_id = ?`
            )
            .bind(
              userId,
              userId
            ),

          env.DB
            .prepare(
              `DELETE FROM reports
               WHERE reporter_id = ?
               OR reported_user_id = ?`
            )
            .bind(
              userId,
              userId
            ),

          env.DB
            .prepare(
              `DELETE FROM gift_transactions
               WHERE sender_user_id = ?
               OR receiver_user_id = ?`
            )
            .bind(
              userId,
              userId
            ),

          env.DB
            .prepare(
              `DELETE FROM wallets
               WHERE user_id = ?`
            )
            .bind(userId),

          env.DB
            .prepare(
              `DELETE FROM sessions
               WHERE user_id = ?`
            )
            .bind(userId),

          env.DB
            .prepare(
              `DELETE FROM users
               WHERE id = ?`
            )
            .bind(userId),
        ]);

        return out(
          { ok: true },
          200,
          env
        );
      }

      /*
       * BLOCK / UNBLOCK
       */

      const blockMatch =
        url.pathname.match(
          /^\/api\/users\/([^/]+)\/block$/
        );

      if (
        blockMatch &&
        (
          request.method === "POST" ||
          request.method === "DELETE"
        )
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const target = await env.DB
          .prepare(
            `SELECT id
             FROM users
             WHERE id = ?
             OR username = ?`
          )
          .bind(
            blockMatch[1],
            blockMatch[1]
          )
          .first<{ id: string }>();

        if (!target) {
          return out(
            { error: "user not found" },
            404,
            env
          );
        }

        if (target.id === userId) {
          return out(
            {
              error:
                "cannot block yourself",
            },
            400,
            env
          );
        }

        if (request.method === "POST") {
          await env.DB
            .prepare(
              `INSERT OR IGNORE INTO blocks
              (
                blocker_id,
                blocked_id,
                created_at
              )
              VALUES (?, ?, ?)`
            )
            .bind(
              userId,
              target.id,
              new Date().toISOString()
            )
            .run();
        } else {
          await env.DB
            .prepare(
              `DELETE FROM blocks
               WHERE blocker_id = ?
               AND blocked_id = ?`
            )
            .bind(
              userId,
              target.id
            )
            .run();
        }

        return out(
          {
            blocked:
              request.method === "POST",

            userId: target.id,
          },
          200,
          env
        );
      }

      /*
       * REPORT
       */

      if (
        url.pathname === "/api/reports" &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const body =
          (await request.json()) as {
            videoId?: unknown;
            reportedUserId?: unknown;
            reason?: unknown;
            details?: unknown;
          };

        if (
          typeof body.reason !== "string" ||
          !body.reason.trim()
        ) {
          return out(
            {
              error: "reason is required",
            },
            400,
            env
          );
        }

        const id = crypto.randomUUID();

        const now =
          new Date().toISOString();

        await env.DB
          .prepare(
            `INSERT INTO reports
            (
              id,
              reporter_id,
              video_id,
              reported_user_id,
              reason,
              details,
              created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)`
          )
          .bind(
            id,
            userId,
            typeof body.videoId === "string"
              ? body.videoId
              : null,
            typeof body.reportedUserId === "string"
              ? body.reportedUserId
              : null,
            body.reason.trim().slice(0, 100),
            typeof body.details === "string"
              ? body.details.trim().slice(0, 1000)
              : "",
            now
          )
          .run();

        return out(
          {
            ok: true,
            id,
          },
          201,
          env
        );
      }

      /*
       * CLOUDFLARE DIRECT UPLOAD
       */

      if (
        url.pathname === "/api/upload/direct" &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        if (!isCloudflareConfigured(env)) {
          return out(
            {
              error:
                "Upload service is not configured",

              code:
                "CLOUDFLARE_NOT_CONFIGURED",
            },
            503,
            env
          );
        }

        const cloudflareResponse =
          await fetch(
            `https://api.cloudflare.com/client/v4/` +
            `accounts/${env.CLOUDFLARE_ACCOUNT_ID}` +
            `/stream/direct_upload`,
            {
              method: "POST",

              headers:
                cloudflareHeaders(env),

              body: JSON.stringify({
                maxDurationSeconds: 600,
              }),
            }
          );

        const payload =
          await cloudflareResponse.json();

        if (!cloudflareResponse.ok) {
          return out(
            {
              error:
                "Cloudflare upload creation failed",

              details: payload,
            },
            502,
            env
          );
        }

        return out(
          payload,
          201,
          env
        );
      }

      /*
       * GIFTS CATALOG
       */

      if (
        url.pathname === "/api/gifts" &&
        request.method === "GET"
      ) {
        const rows = await env.DB
          .prepare(
            `SELECT
              id,
              name,
              icon,
              price_coins AS priceCoins
             FROM gift_catalog
             WHERE active = 1
             ORDER BY sort_order ASC`
          )
          .all<GiftRow>();

        return out(
          {
            items: rows.results,
          },
          200,
          env
        );
      }

      /*
       * LIVE DETAILS
       */

      const liveMatch =
        url.pathname.match(
          /^\/api\/live\/([^/]+)$/
        );

      if (
        liveMatch &&
        request.method === "GET"
      ) {
        const live = await env.DB
          .prepare(
            `SELECT
              l.id,
              l.title,
              l.status,
              l.created_at AS createdAt,
              l.ended_at AS endedAt,
              u.id AS hostUserId,
              u.username AS host,
              u.display_name AS hostDisplayName,
              u.avatar_url AS hostAvatarUrl
             FROM live_sessions l
             JOIN users u
               ON u.id = l.host_user_id
             WHERE l.id = ?`
          )
          .bind(liveMatch[1])
          .first();

        if (!live) {
          return out(
            { error: "live not found" },
            404,
            env
          );
        }

        return out(
          {
            live,
          },
          200,
          env
        );
      }

      /*
       * UPDATE LIVE
       */

      if (
        liveMatch &&
        request.method === "PATCH"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const body =
          (await request.json()) as {
            status?: unknown;
            title?: unknown;
          };

        const live = await env.DB
          .prepare(
            `SELECT
              host_user_id
             FROM live_sessions
             WHERE id = ?`
          )
          .bind(liveMatch[1])
          .first<{
            host_user_id: string;
          }>();

        if (!live) {
          return out(
            { error: "live not found" },
            404,
            env
          );
        }

        if (
          live.host_user_id !== userId
        ) {
          return out(
            { error: "forbidden" },
            403,
            env
          );
        }

        const status =
          typeof body.status === "string" &&
          [
            "created",
            "live",
            "ended",
          ].includes(body.status)
            ? body.status
            : null;

        const title =
          typeof body.title === "string"
            ? body.title.trim().slice(0, 120)
            : null;

        const endedAt =
          status === "ended"
            ? new Date().toISOString()
            : null;

        await env.DB
          .prepare(
            `UPDATE live_sessions
             SET
               status =
                 COALESCE(?, status),
               title =
                 COALESCE(?, title),
               ended_at =
                 CASE
                   WHEN ? = 'ended'
                   THEN ?
                   ELSE ended_at
                 END
             WHERE id = ?`
          )
          .bind(
            status,
            title,
            status,
            endedAt,
            liveMatch[1]
          )
          .run();

        return out(
          {
            ok: true,
            liveId: liveMatch[1],
            status,
            title,
          },
          200,
          env
        );
      }

      /*
       * SEND LIVE GIFT
       */

      const sendGiftMatch =
        url.pathname.match(
          /^\/api\/live\/([^/]+)\/gifts$/
        );

      if (
        sendGiftMatch &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        const body =
          (await request.json()) as {
            giftId?: unknown;
            quantity?: unknown;
            receiverUserId?: unknown;
          };

        const giftId =
          typeof body.giftId === "string"
            ? body.giftId.trim()
            : "";

        const quantityValue =
          Number(body.quantity ?? 1);

        if (
          !giftId ||
          !Number.isInteger(quantityValue) ||
          quantityValue < 1
        ) {
          return out(
            {
              error:
                "giftId and valid quantity are required",
            },
            400,
            env
          );
        }

        const quantity = Math.min(
          quantityValue,
          100
        );

        const live = await env.DB
          .prepare(
            `SELECT
              host_user_id,
              status
             FROM live_sessions
             WHERE id = ?`
          )
          .bind(sendGiftMatch[1])
          .first<{
            host_user_id: string;
            status: string;
          }>();

        if (!live) {
          return out(
            { error: "live not found" },
            404,
            env
          );
        }

        if (live.status !== "live") {
          return out(
            {
              error:
                "live is not active",
            },
            409,
            env
          );
        }

        const gift = await env.DB
          .prepare(
            `SELECT
              id,
              name,
              icon,
              price_coins AS priceCoins
             FROM gift_catalog
             WHERE id = ?
             AND active = 1`
          )
          .bind(giftId)
          .first<GiftRow>();

        if (!gift) {
          return out(
            { error: "gift not found" },
            404,
            env
          );
        }

        const receiver =
          typeof body.receiverUserId === "string"
            ? body.receiverUserId
            : live.host_user_id;

        if (
          receiver !== live.host_user_id
        ) {
          return out(
            {
              error:
                "receiver must be the live host",
            },
            400,
            env
          );
        }

        const total =
          gift.priceCoins * quantity;

        const wallet = await env.DB
          .prepare(
            `SELECT coins
             FROM wallets
             WHERE user_id = ?`
          )
          .bind(userId)
          .first<{ coins: number }>();

        const available =
          wallet?.coins ?? 0;

        if (available < total) {
          return out(
            {
              error: "insufficient coins",
              requiredCoins: total,
              availableCoins: available,
            },
            402,
            env
          );
        }

        const transactionId =
          crypto.randomUUID();

        const now =
          new Date().toISOString();

        /*
         * If sender and host are different:
         * sender loses coins and host receives them.
         *
         * If sender is the host:
         * the balance remains unchanged.
         */

        if (userId === receiver) {

          await env.DB.batch([
            env.DB
              .prepare(
                `INSERT INTO gift_transactions
                (
                  id,
                  live_id,
                  sender_user_id,
                  receiver_user_id,
                  gift_id,
                  quantity,
                  coins_total,
                  created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
              )
              .bind(
                transactionId,
                sendGiftMatch[1],
                userId,
                receiver,
                giftId,
                quantity,
                total,
                now
              ),
          ]);

        } else {

          const senderUpdate =
            await env.DB
              .prepare(
                `UPDATE wallets
                 SET
                   coins = coins - ?,
                   updated_at = ?
                 WHERE user_id = ?
                 AND coins >= ?`
              )
              .bind(
                total,
                now,
                userId,
                total
              );

          const receiverUpdate =
            env.DB
              .prepare(
                `UPDATE wallets
                 SET
                   coins = coins + ?,
                   updated_at = ?
                 WHERE user_id = ?`
              )
              .bind(
                total,
                now,
                receiver
              );

          const transaction =
            env.DB
              .prepare(
                `INSERT INTO gift_transactions
                (
                  id,
                  live_id,
                  sender_user_id,
                  receiver_user_id,
                  gift_id,
                  quantity,
                  coins_total,
                  created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
              )
              .bind(
                transactionId,
                sendGiftMatch[1],
                userId,
                receiver,
                giftId,
                quantity,
                total,
                now
              );

          await env.DB.batch([
            senderUpdate,
            receiverUpdate,
            transaction,
          ]);
        }

        return out(
          {
            ok: true,

            transactionId,

            gift: {
              id: gift.id,
              name: gift.name,
              icon: gift.icon,
              priceCoins: gift.priceCoins,
              quantity,
            },

            coinsSpent: total,

            remainingCoins:
              userId === receiver
                ? available
                : available - total,
          },
          201,
          env
        );
      }

      /*
       * CREATE LIVE
       */

      if (
        url.pathname === "/api/live/create" &&
        request.method === "POST"
      ) {
        if (!userId) {
          return out(
            { error: "unauthorized" },
            401,
            env
          );
        }

        if (!isCloudflareConfigured(env)) {
          return out(
            {
              error:
                "Live service is not configured",

              code:
                "CLOUDFLARE_NOT_CONFIGURED",
            },
            503,
            env
          );
        }

        const body =
          (await request.json().catch(
            () => ({})
          )) as {
            title?: unknown;
          };

        const cloudflareResponse =
          await fetch(
            `https://api.cloudflare.com/client/v4/` +
            `accounts/${env.CLOUDFLARE_ACCOUNT_ID}` +
            `/stream/live_inputs`,
            {
              method: "POST",

              headers:
                cloudflareHeaders(env),

              body: JSON.stringify({
                recording: {
                  mode: "automatic",
                },
              }),
            }
          );

        const payload =
          await cloudflareResponse.json();

        if (!cloudflareResponse.ok) {
          return out(
            {
              error:
                "Cloudflare live input creation failed",

              details: payload,
            },
            502,
            env
          );
        }

        const result =
          (payload as {
            result?: {
              uid?: string;
              rtmps?: unknown;
              webRTC?: unknown;
            };
          }).result;

        if (!result?.uid) {
          return out(
            {
              error:
                "Cloudflare did not return a live input id",
            },
            502,
            env
          );
        }

        const liveId =
          crypto.randomUUID();

        const title =
          typeof body.title === "string"
            ? body.title.trim().slice(0, 120)
            : "Wave Live";

        const now =
          new Date().toISOString();

        await env.DB
          .prepare(
            `INSERT INTO live_sessions
            (
              id,
              host_user_id,
              title,
              status,
              cloudflare_input_id,
              created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)`
          )
          .bind(
            liveId,
            userId,
            title,
            "created",
            result.uid,
            now
          )
          .run();

        return out(
          {
            ok: true,

            liveId,

            inputId: result.uid,

            title,

            status: "created",

            cloudflare: result,
          },
          201,
          env
        );
      }

      /*
       * NOT FOUND
       */

      return out(
        {
          error: "not found",
        },
        404,
        env
      );

    } catch (error) {

      console.error(
        "Wave API error:",
        error
      );

      return out(
        {
          error:
            "internal server error",
        },
        500,
        env
      );
    }
  },
};
