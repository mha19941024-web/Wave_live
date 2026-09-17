import express from "express";
import cors from "cors";
import crypto from "node:crypto";

type Video = { id: string; url: string; user: string; caption: string; likes: number; createdAt: string };

const app = express();
app.use(cors());
app.use(express.json({ limit: "2mb" }));

const videos: Video[] = [];

app.get("/health", (_req, res) => res.json({ ok: true, service: "vyro-api", version: "0.2.0" }));

app.get("/api/feed", (req, res) => {
  const limit = Math.min(Math.max(Number(req.query.limit ?? 20), 1), 50);
  const cursor = Number(req.query.cursor ?? 0);
  const items = videos.slice(cursor, cursor + limit);
  const nextCursor = cursor + items.length < videos.length ? cursor + items.length : null;
  res.json({ items, nextCursor });
});

app.post("/api/videos", (req, res) => {
  const { url, user, caption = "" } = req.body ?? {};
  if (typeof url !== "string" || typeof user !== "string" || !url || !user) {
    return res.status(400).json({ error: "url and user are required" });
  }
  const video: Video = { id: crypto.randomUUID(), url, user, caption: String(caption), likes: 0, createdAt: new Date().toISOString() };
  videos.unshift(video);
  res.status(201).json(video);
});

app.post("/api/live/create", (_req, res) => {
  if (!process.env.CLOUDFLARE_ACCOUNT_ID || !process.env.CLOUDFLARE_API_TOKEN) {
    return res.status(503).json({ error: "Live service is not configured", code: "CLOUDFLARE_NOT_CONFIGURED" });
  }
  res.status(501).json({ error: "Cloudflare Stream Live integration is the next backend step" });
});

const port = Number(process.env.PORT ?? 8080);
app.listen(port, () => console.log(`VYRO API listening on ${port}`));
