# VYRO Wave API 0.7.0

Production-oriented Cloudflare Worker API for Wave.

## Services
- D1 users/sessions/videos/social data
- Cloudflare Stream direct upload ticket
- Cloudflare Stream live input creation
- Feed, likes, comments, follows, profile

## Required production secrets
- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_API_TOKEN`
- `STREAM_CUSTOMER_SUBDOMAIN`
- Optional `CORS_ORIGIN`

Create the D1 database and binding in Cloudflare, then run migrations remotely and deploy the Worker. Cloudflare's current D1 workflow uses a Worker binding and Wrangler migrations/deployments. citeturn0search0turn0search8
