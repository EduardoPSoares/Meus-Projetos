# AdminPanel Architecture (Current)

## Frontend boot
- `public/app.js` loads domains and starts `public/modules/app/app.js`.

## Frontend domains extracted
- `ui/base`
- `services/monitoring`
- `services/runtime-config`
- `services/admin-ops`
- `services/config`
- `players/commands`
- `players/moderation`
- `players/achievements`
- `players/rooms`
- `shop/offers`
- `shop/packages-rotation`
- `rewards/survival`
- `rewards/survival-actions`
- `launcher/cdn`
- `launcher/content`
- `launcher/sync`
- `launcher/publish`

## Backend boot
- `server.js` -> `backend/bootstrap/start-admin-panel.js` -> `backend/app/server.js`.

## Backend routing
- `backend/app/server.js` delegates API routing through domain registrars first.
- Domain registrars:
  - `backend/domains/auth/register.js`
  - `backend/domains/services/register.js`
  - `backend/domains/launcher/register.js`
  - `backend/domains/shop/register.js`
  - `backend/domains/rewards/register.js`
  - `backend/domains/players/register.js`
  - `backend/domains/cdn/register.js`
- Runtime-config routes already implemented directly inside `services/register.js`.

## Status
- Backend domain routing migration concluded.
- Frontend modular migration concluded.
- `backend/app/server.js` and `public/modules/app/app.js` now operate as orchestrators/composition layers.

## Final hardening
- Keep public contracts stable (routes/methods/payloads).
- Evolve features by editing domain modules first, not orchestrators.
- Treat `REFACTOR_MAP.md` as historical migration log and this document as current architecture source of truth.
