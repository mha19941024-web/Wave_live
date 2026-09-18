# VYRO Wave Commercial 0.9.0

Wave is a short-video Android client backed by Cloudflare Workers + D1 + Stream.

## Implemented in this build
- Anonymous session bootstrap
- Persistent session token on Android
- Real feed from D1
- Cloudflare Stream direct-upload ticket + Android video upload
- Publish uploaded Stream video into the feed
- Likes/unlikes
- Comments API
- Follow/unfollow API
- Profile read/update
- Live-input creation with RTMPS URL and stream key
- Live session records with host ownership and live/ended state
- Gift catalog and coin-spend transaction foundation for LIVE gifts
- Feed filtering for blocked creators
- ExoPlayer playback for HLS/MP4

Cloudflare documents native Android playback of Stream content with ExoPlayer, including HLS manifests. citeturn0search1

## Production setup still requiring your account
1. Create D1 database and replace `database_id` in `worker/wrangler.toml`.
2. Apply migrations remotely.
3. Add Worker secrets: `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`, `STREAM_CUSTOMER_SUBDOMAIN`.
4. Deploy the Worker.
5. Verify `/health`.
6. Point the Android `API_BASE_URL` to the deployed Worker.
7. Build/sign an AAB with your Play signing key.

Cloudflare's current D1 workflow uses bindings and Wrangler migrations/deployments. citeturn0search0turn0search4

## Important
This build deliberately does not embed Cloudflare API secrets in Android. Secrets belong on the Worker.


### 0.9.0 hardening
- Added migration 0005 for live sessions, gift catalog, wallets and gift transactions.
- Added `/api/gifts`, `/api/live/:id`, and `/api/live/:id/gifts`.
- Added host-only live status updates and blocked-creator filtering.

### 0.8.0 hardening
- Fixed Android theme resource case mismatch.
- Added account deletion endpoint.
- Added user block/unblock endpoint.
- Added report endpoint and D1 moderation tables.
