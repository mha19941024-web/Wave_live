# Production deployment

1. Create a D1 database named `vyro-wave-prod` in your Cloudflare account.
2. Put its real ID into `wrangler.toml`.
3. Run:
   - `npx wrangler d1 migrations apply vyro-wave-prod --remote`
   - `npx wrangler deploy`
4. Add secrets with `npx wrangler secret put` for:
   - `CLOUDFLARE_ACCOUNT_ID`
   - `CLOUDFLARE_API_TOKEN`
   - `STREAM_CUSTOMER_SUBDOMAIN`
5. Test `GET /health` and then the app feed/session endpoints.

The Cloudflare API token needs Stream Write permission for creating live inputs. citeturn1search0
