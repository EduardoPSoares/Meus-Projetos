// Launcher configuration.
// The old full-ZIP game download path was removed. Game install/update now uses
// the CDN manifest and prefers the published changed_files list. Older manifests
// still fall back to comparing local hashes.

module.exports = {
  // Fixed game installation path.
  GAME_PATH: 'C:\\WarfaceSurvivor',

  // Public AdminPanel endpoints used by the launcher update/sync flow.
  GAME_VERSION_URL: 'http://127.0.0.1:8081/api/public/game-version',
  GAME_MANIFEST_URL: 'http://127.0.0.1:8081/api/public/game-manifest',
  LAUNCHER_CONFIG_URL: 'http://127.0.0.1:8081/api/public/launcher-config',
  LAUNCHER_VERSION_URL: 'http://127.0.0.1:8081/api/public/launcher-version',
  LAUNCHER_MANIFEST_URL: 'http://127.0.0.1:8081/api/public/launcher-manifest',
  RUNTIME_CONFIG_WS_URL: 'ws://127.0.0.1:8081/ws/launcher-runtime',
  RUNTIME_CONFIG_CHANNEL: 'stable',
  RUNTIME_CONFIG_HMAC_SECRET: 'wf_runtime_hmac_2026_prod_local',
  DISCORD_INVITE_URL: 'https://discord.gg/YOUR_INVITE',

  // O CDN e a fonte final dos arquivos do client. Quando um arquivo corrigido foi
  // enviado direto no bucket mas o manifest antigo ainda tem outro hash, aceite o
  // download se o tamanho final bater com o manifest.
  ALLOW_CDN_HASH_MISMATCH: true,

  // Account/register API.
  REGISTER_API: 'https://lawsuit-scotia-dentists-thanks.trycloudflare.com',

  SERVER: {
    host: 'warface',
    ip: 'subscriber-margaret.with.playit.plus',
    port: 1050,
    useTLS: true,
    useProtect: false,
    checkCertificate: false,
    disableHashValidation: true,
    disableAntiCheat: true
  }
};
