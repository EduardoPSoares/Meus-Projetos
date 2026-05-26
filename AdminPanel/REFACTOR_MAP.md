# AdminPanel Refactor Map

## Entry Points
- `server.js`: thin backend orchestrator.
- `public/app.js`: thin frontend bootstrap.

## Runtime Core
- `backend/app/server.js`: current backend implementation under new structure.
- `public/modules/app/app.js`: current frontend implementation under new structure.

## Modular Structure
- `backend/bootstrap/`: startup and domain loader.
- `backend/domains/`: backend domains (`auth`, `services`, `launcher`, `shop`, `rewards`, `players`, `cdn`).
- `public/modules/core/`: shared frontend helpers/state.
- `public/modules/domains/`: frontend domain modules.

## Migration Flow
1. Extract one backend domain at a time from `backend/app/server.js`.
2. Extract one frontend domain at a time from `public/modules/app/app.js`.
3. Wire in bootstrap/orchestrators.
4. Remove duplicated runtime segment only after smoke/regression passes.

## Current Status
- Legacy entry files removed:
  - `backend/legacy/server-legacy.js`
  - `public/modules/legacy/app-legacy.js`
- Active entrypoints:
  - `backend/bootstrap/start-admin-panel.js` -> `backend/app/server.js`
  - `public/app.js` -> `public/modules/app/app.js`
- Backend domain progress:
  - `auth`: `/api/model` migrated to domain handler (no legacy switch for this route).
  - `cdn`: `/api/weapons/names` and `/api/weapons/enrich` migrated to domain handler.
  - `shop`: package/rotation flows migrated:
    - `/api/shop/packages`
    - `/api/shop/packages/get`
    - `/api/shop/packages/save`
    - `/api/shop/packages/delete`
    - `/api/shop/packages/apply`
    - `/api/shop/rotation`
    - `/api/shop/rotation/set`
    - `/api/shop/rotation/run`
    - `/api/shop/regenerate`
  - `shop`: active offers/catalog routes migrated:
    - `/api/shop/offers`
    - `/api/arsenal/catalog`
    - `/api/shop/offer/update`
    - `/api/shop/clear`
  - `services`: core service controls migrated:
    - `/api/services`
    - `/api/service/start`
    - `/api/service/stop`
    - `/api/service/restart`
    - `/api/services/startAll`
    - `/api/services/stopAll`
  - `services`: monitoring/runtime routes migrated:
    - `/api/logs`
    - `/api/logs/stream`
    - `/api/anticheat`
    - `/api/anticheat/set`
    - `/api/serverinfo`
    - `/api/stats`
    - `/api/stats/history`
  - `services`: config/event/maintenance routes migrated:
    - `/api/maintenance`
    - `/api/maintenance/set`
    - `/api/xp`
    - `/api/xp/disable`
    - `/api/config`
    - `/api/config/save`
    - `/api/backup/list`
  - `services`: broadcast/backup routes migrated:
    - `/api/autobroadcast`
    - `/api/autobroadcast/set`
    - `/api/backup`
  - `rewards`: survival routes migrated:
    - `/api/survival-rewards`
    - `/api/survival-rewards/save`
  - `launcher`: sync/progress/source-dir base routes migrated:
    - `/api/launcher/sync-progress`
    - `/api/launcher/publish-progress`
    - `/api/launcher/source-dir/save`
    - `/api/game/sync-progress`
    - `/api/game/publish-progress`
    - `/api/game/publish-cancel`
    - `/api/game/source-dir/save`
    - `/api/game/select-folder`
    - `/api/launcher/config/save`
    - `/api/launcher/browse-source-dir`
    - `/api/game/browse-source-dir`
  - `launcher`: version/ref/sync routes migrated:
    - `/api/launcher/version/save`
    - `/api/launcher/ref-info`
    - `/api/launcher/sync-from-cdn`
  - `launcher`: game version/ref/sync routes migrated:
    - `/api/game/version/save`
    - `/api/game/ref-info`
    - `/api/game/sync-from-cdn`
  - `launcher`: patch upload route migrated:
    - `/api/game/upload-patch`
  - `launcher`: publish routes migrated:
    - `/api/launcher/publish`
    - `/api/game/publish-folder`
  - `players`: monitoring and listing routes migrated:
    - `/api/players`
    - `/api/playerhistory`
    - `/api/banhistory`
    - `/api/chatlogs`
    - `/api/notes/get`
    - `/api/notes/set`
    - `/api/bannedips`
    - `/api/clans`
    - `/api/gamerooms`
  - `players`: moderation/clan action routes migrated:
    - `/api/banip`
    - `/api/unbanip`
    - `/api/kickbyip`
    - `/api/clan/create`
    - `/api/clan/rename`
    - `/api/clan/kick`
  - `players`: inventory/achievement routes migrated:
    - `/api/removeitem`
    - `/api/achievements/list`
    - `/api/achievements/give`
    - `/api/achievements/remove`
  - `players`: profile/ban list routes migrated:
    - `/api/profile/full`
    - `/api/profile/lookup`
    - `/api/bans`
  - `players`: control action routes migrated:
    - `/api/generateToken`
    - `/api/broadcast`
    - `/api/notification`
    - `/api/ban`
    - `/api/unban`
  - `players`: command/search/item routes migrated:
    - `/api/search`
    - `/api/giveitem`
    - `/api/command`
  - Domains without legacy fallback:
    - `players`
    - `rewards`
    - `services`
    - `shop`
    - `launcher`
- Frontend domain progress:
  - Players split into modules (`commands`, `moderation`, `rooms`, `achievements`, `item-ops`, `inspect`).
  - Services/performance behavior bound through `domains/services/performance.js`.
  - Launcher heavy block removed from `app.js` and replaced by wrappers for:
    - `domains/launcher/content.js`
    - `domains/launcher/cdn.js`
    - `domains/launcher/sync.js`
    - `domains/launcher/publish.js`
  - Services config block in `app.js` replaced by wrappers to `domains/services/config.js`.
  - Players/services fallback blocks replaced by wrappers:
    - `notes` (`services/admin-ops`)
    - `clans` and `chatlogs` (`players/moderation`)
    - `gamerooms` (`players/rooms`)
    - `performance` charts/stats (`services/performance`)
  - Runtime config and achievements fallback blocks replaced by wrappers:
    - `services/runtime-config`
    - `players/achievements`
  - Rewards and shop orchestration blocks replaced by wrappers:
    - `rewards/survival`
    - `rewards/survival-actions`
    - `shop/offers`
    - `shop/packages-rotation`
  - Services/players orchestration blocks replaced by wrappers:
    - `services/monitoring`
    - `services/admin-ops`
    - `services/anticheat`
    - `players/commands`
    - `players/inspect`
    - `players/item-ops`
    - `players/moderation`
  - `public/modules/app/app.js` reduced from 6038 lines to 2846 lines.

## Final Status
1. Backend:
- Domain migration completed.
- Runtime legacy switch removed.
2. Frontend:
- Domain migration completed.
- `public/modules/app/app.js` reduced from 6038 to 2846 lines.
3. Contracts:
- Public routes, methods and payload shapes preserved during migration.

## Operational Split (May 24, 2026)
- System tabs removed from AdminPanel UI:
  - `Launcher CDN`
  - `Jogo / Gamefiles`
- These flows were moved to a standalone local app for developers:
  - `C:\Users\ray\Pictures\SurvivorDevelopers`
  - Local app proxies to AdminPanel backend and keeps route/payload compatibility.
