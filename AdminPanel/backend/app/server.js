const http = require('http');
const net = require('net');
const https = require('https');
const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const { WebSocketServer } = require('ws');
const { spawn, execFile, execFileSync } = require('child_process');
const { MongoClient } = require('mongodb');
const { registerAuthRoutes } = require('../domains/auth/register');
const { registerServicesRoutes } = require('../domains/services/register');
const { registerLauncherRoutes } = require('../domains/launcher/register');
const { registerShopRoutes } = require('../domains/shop/register');
const { registerRewardsRoutes } = require('../domains/rewards/register');
const { registerPlayersRoutes } = require('../domains/players/register');
const { registerCdnRoutes } = require('../domains/cdn/register');

function readLocalPathsConfig() {
  const cfgFile = path.join(__dirname, 'paths.local.json');
  try {
    if (!fs.existsSync(cfgFile)) return {};
    const raw = fs.readFileSync(cfgFile, 'utf8').replace(/^\uFEFF/, '');
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function resolveConfiguredPath(value, fallbackAbsolute) {
  const raw = String(value || '').trim();
  if (!raw) return fallbackAbsolute;
  return path.resolve(raw);
}

// ─── Config ───────────────────────────────────────────────────────────
const PORT = 8081;
const ADMIN_PASSWORD = 'admin123';
const MONGO_URL = 'mongodb://127.0.0.1:27017/';
const XMPP_API = 'http://127.0.0.1:8080';
const PATHS_CONFIG = readLocalPathsConfig();
const DEFAULT_ROOT = path.resolve(__dirname, '..', '..', '..', 'Servidor');
const ROOT = resolveConfiguredPath(process.env.WF_SERVER_ROOT || PATHS_CONFIG.serverRoot, DEFAULT_ROOT);
const DEFAULT_PUBLIC_DIR = path.resolve(__dirname, '..', '..', 'public');
const PUBLIC_DIR = resolveConfiguredPath(process.env.ADMIN_PUBLIC_DIR || PATHS_CONFIG.publicDir, DEFAULT_PUBLIC_DIR);

// ─── Limits ──────────────────────────────────────────────────────────
const LIMITS = {
  game_money:  { min: 0, max: 2147483647, perCmd: 100000000 },
  cry_money:   { min: 0, max: 2147483647, perCmd: 100000000 },
  crown_money: { min: 0, max: 2147483647, perCmd: 100000000 },
  experience:  { min: 0, max: 2147483647, perCmd: 10000000 },
  rank:        { min: 1, max: 90 },
};
const PANEL_MODEL = Object.freeze({
  nick: Object.freeze({
    minLen: 3,
    maxLen: 24,
  }),
  command: Object.freeze({
    allowed: Object.freeze(['addcry', 'addcrown', 'addvp', 'addxp', 'addgm', 'kick', 'addcm']),
  }),
  item: Object.freeze({
    minLen: 2,
    maxLen: 80,
    pattern: /^[a-z0-9_]+$/i,
    quantity: Object.freeze({ min: 1, max: 999 }),
    durability: Object.freeze({ min: 0, max: 1000000 }),
    expirationHours: Object.freeze({ min: 0, max: 8760 }),
    maxPendingRemoteGiveItems: 500,
  }),
  achievement: Object.freeze({
    minIdLen: 1,
    maxIdLen: 72,
    idPattern: /^[a-z0-9_:/.-]+$/i,
    progress: Object.freeze({ min: 0, max: 1000000 }),
  }),
  xp: Object.freeze({
    multiplier: Object.freeze({ min: 1, max: 9999 }),
  }),
});
const ITEM_VARIANT_SUFFIX_RE = /_(shop|default|game|bronze|silver|gold|diamond|premium)$/i;
const NODE = path.join(ROOT, 'NodeJs', 'node.exe');
const MONGO_EXE = path.join(ROOT, 'MongoDb', 'mongod.exe');
const MONGO_DB = path.join(ROOT, 'MongoDb', 'db');
const MONGO_CFG = path.join(ROOT, 'MongoDb', 'mongod.cfg');
const MASTER_DIR = path.join(ROOT, 'Masterserver');
function looksLikeGameRoot(rootDir) {
  if (!rootDir) return false;
  const bin64Dir = path.join(rootDir, 'Bin64');
  return fs.existsSync(path.join(bin64Dir, 'DedicatedStarterMany.exe')) ||
    fs.existsSync(path.join(bin64Dir, 'Game.exe')) ||
    fs.existsSync(path.join(rootDir, 'Game.exe'));
}

function resolveGameDir() {
  const configuredGameDir = resolveConfiguredPath(process.env.WF_GAME_DIR || PATHS_CONFIG.gameDir, '');
  if (configuredGameDir && looksLikeGameRoot(configuredGameDir)) return configuredGameDir;

  const defaultGameRoot = path.resolve(ROOT, '..', 'WarfaceSurvivor');
  const candidates = [
    defaultGameRoot,
    path.join(defaultGameRoot, 'WFDEV20', 'GameClient'),
    path.resolve(__dirname, '..', 'WFDEV20', 'GameClient'),
    path.resolve(__dirname, '..', 'WarfaceSurvivor'),
    path.resolve(__dirname, '..', 'WarfaceSurvivor', 'WFDEV20', 'GameClient')
  ];
  for (const candidate of candidates) {
    if (looksLikeGameRoot(candidate)) return candidate;
  }
  return defaultGameRoot;
}

const GAME_DIR = resolveGameDir();
const BIN64 = path.join(GAME_DIR, 'Bin64');
const DEDICATED_DIR = resolveConfiguredPath(process.env.WF_DEDICATED_DIR || PATHS_CONFIG.dedicatedDir, BIN64);
const DEDICATED_EXE = path.join(DEDICATED_DIR, 'DedicatedStarterMany.exe');
const DEDICATED_ROOM_PORTS = [65000, 65001, 65011, 65012, 54000, 54001, 54011, 54012];
const DEDICATED_BASE_ARGS = ['--shard_id', '0', '-nodevmode', '-language', 'Russian', '-simple_console', '-dedicated'];
const LAUNCHER_CONFIG = path.resolve(__dirname, '..', 'WarfaceLauncherJS', 'config.js');
const ONLINE_CFG = path.join(DEDICATED_DIR, 'online.cfg');
const EXP_CURVE_XML = path.join(MASTER_DIR, 'gamedata', 'libs', 'config', 'expcurve.xml');
const REGISTERED_ACCOUNTS_FILE = path.join(ROOT, 'XmppServerTcp', 'registered_accounts.json');
const LAUNCHER_DATA_FILE = path.join(PUBLIC_DIR, 'launcher-config.json');
const LAUNCHER_IMAGES_DIR = path.join(PUBLIC_DIR, 'launcher-images');
const LAUNCHER_VERSION_FILE = path.join(PUBLIC_DIR, 'launcher-version.json');
const LAUNCHER_MANIFEST_FILE = path.join(PUBLIC_DIR, 'launcher-manifest.json');
const LAUNCHER_UPDATE_HISTORY_FILE = path.join(PUBLIC_DIR, 'launcher-update-history.json');
const GAME_VERSION_FILE = path.join(PUBLIC_DIR, 'game-version.json');
const GAME_MANIFEST_FILE = path.join(PUBLIC_DIR, 'game-manifest.json');
const GAME_UPDATE_HISTORY_FILE = path.join(PUBLIC_DIR, 'game-update-history.json');
const LAUNCHER_RUNTIME_CONFIG_FILE = path.join(PUBLIC_DIR, 'launcher-runtime-config.json');
const GAME_REF_DIR = path.resolve(__dirname, '..', 'WarfaceSurvivor_Ref');
const GAME_CDN_DIR = path.join(PUBLIC_DIR, 'cdn', 'game');
const R2_CONFIG_FILE = path.join(__dirname, 'r2-config.local.json');
const WIKI_BASE = 'https://ru.warface.com/wiki';
const WEAPON_MEDIA_CACHE_FILE = path.join(PUBLIC_DIR, 'weapon-media-cache.json');
const WEAPON_MEDIA_DIR = path.join(PUBLIC_DIR, 'img', 'weapons', 'wiki');
const WIKI_ALL_IMAGES_INDEX_FILE = path.join(PUBLIC_DIR, 'wiki-allimages-index.json');
const SHOP_PACKAGES_FILE = path.join(__dirname, 'shop-packages.json');
const SHOP_ROTATION_FILE = path.join(__dirname, 'shop-rotation.json');
const ASSET_INVENTORY_ITEMS_FILE = path.resolve(__dirname, '..', 'AssetInventory', 'manifests', 'items.json');
const REWARD_CONFIG_DIR = path.join(MASTER_DIR, 'gamedata', 'libs', 'config', 'masterserver');
const REWARDS_CONFIG_FILE = path.join(REWARD_CONFIG_DIR, 'rewards_configuration.xml');
const SPECIAL_REWARD_CONFIG_FILE = path.join(REWARD_CONFIG_DIR, 'special_reward_configuration.xml');
const CUSTOM_RULES_FILE = path.join(REWARD_CONFIG_DIR, 'custom_rules.xml');
const MISSIONS_DIR = path.join(MASTER_DIR, 'gamedata', 'libs', 'missions');
const REWARD_BACKUP_DIR = path.join(__dirname, 'reward-backups');
const SHOP_ROTATION_TICK_MS = 30000;
const RUNTIME_SCHEMA_VERSION = 1;
const RUNTIME_WS_PATH = '/ws/launcher-runtime';
const RUNTIME_HMAC_SECRET = String(process.env.RUNTIME_CONFIG_HMAC_SECRET || '').trim();
const ADMIN_AUTH_TOKEN = String(process.env.ADMIN_AUTH_TOKEN || '').trim();

const runtimeWsClients = new Set();
let serverStatusBroadcastTimer = null;
let domainRouteHandlers = [];

if (!RUNTIME_HMAC_SECRET) {
  console.warn('[SECURITY] RUNTIME_CONFIG_HMAC_SECRET nao definido; assinatura HMAC de runtime ficara desativada.');
}

if (!ADMIN_AUTH_TOKEN) {
  console.warn('[SECURITY] ADMIN_AUTH_TOKEN nao definido; acesso admin limitado a loopback com token local-no-auth.');
}

const tokens = new Map();
let tokenCounter = 1;
let broadcastTimers = {};
const registerAttempts = new Map();
let shopRotationTimer = null;
let shopRotationBusy = false;
let launcherDefaultItemsCache = null;
let gameItemInventoryCache = null;
let gameItemInventoryMtimeMs = 0;
const MIME = { '.html':'text/html; charset=utf-8', '.css':'text/css; charset=utf-8', '.js':'application/javascript; charset=utf-8', '.json':'application/json; charset=utf-8', '.png':'image/png', '.jpg':'image/jpeg', '.jpeg':'image/jpeg', '.gif':'image/gif', '.webp':'image/webp', '.zip':'application/zip' };

function mimeTypeByExt(ext) {
  return MIME[String(ext || '').toLowerCase()] || 'application/octet-stream';
}

function createGamePublishProgress() {
  return {
    active: false,
    done: false,
    phase: 'idle',
    percent: 0,
    message: 'Aguardando publicacao',
    version: '',
    source_dir: '',
    current_file: '',
    total_files: 0,
    hashed_files: 0,
    changed_count: 0,
    uploaded_count: 0,
    skipped_existing_count: 0,
    removed_count: 0,
    removed_done: 0,
    upload_index: 0,
    upload_total: 0,
    upload_bytes_done: 0,
    upload_bytes_current: 0,
    upload_bytes_total: 0,
    error: '',
    startedAt: null,
    updatedAt: null,
    completedAt: null
  };
}

let gamePublishProgress = createGamePublishProgress();
let launcherPublishProgress = createGamePublishProgress();

function createDevSyncProgress(label = 'Sincronizacao') {
  return {
    active: false,
    done: false,
    phase: 'idle',
    percent: 0,
    message: `${label} aguardando`,
    source_dir: '',
    current_file: '',
    version: '',
    total_files: 0,
    checked_files: 0,
    download_index: 0,
    download_total: 0,
    download_bytes_done: 0,
    download_bytes_current: 0,
    download_bytes_total: 0,
    downloaded_count: 0,
    kept_count: 0,
    removed_count: 0,
    error: '',
    startedAt: null,
    updatedAt: null,
    completedAt: null
  };
}

let gameSyncProgress = createDevSyncProgress('Sync do jogo');
let launcherSyncProgress = createDevSyncProgress('Sync do launcher');

function clampPercent(value) {
  const n = Number(value || 0);
  if (!Number.isFinite(n)) return 0;
  return Math.max(0, Math.min(100, Math.round(n)));
}

function setGamePublishProgress(patch = {}) {
  gamePublishProgress = {
    ...gamePublishProgress,
    ...patch,
    percent: Object.prototype.hasOwnProperty.call(patch, 'percent') ? clampPercent(patch.percent) : gamePublishProgress.percent,
    updatedAt: new Date().toISOString()
  };
  return gamePublishProgress;
}

function getGamePublishProgress() {
  return { ...gamePublishProgress };
}

function setLauncherPublishProgress(patch = {}) {
  launcherPublishProgress = {
    ...launcherPublishProgress,
    ...patch,
    percent: Object.prototype.hasOwnProperty.call(patch, 'percent') ? clampPercent(patch.percent) : launcherPublishProgress.percent,
    updatedAt: new Date().toISOString()
  };
  return launcherPublishProgress;
}

function getLauncherPublishProgress() {
  return { ...launcherPublishProgress };
}

function setDevSyncProgress(kind, patch = {}) {
  const current = kind === 'launcher' ? launcherSyncProgress : gameSyncProgress;
  const next = {
    ...current,
    ...patch,
    percent: Object.prototype.hasOwnProperty.call(patch, 'percent') ? clampPercent(patch.percent) : current.percent,
    updatedAt: new Date().toISOString()
  };
  if (kind === 'launcher') launcherSyncProgress = next;
  else gameSyncProgress = next;
  return next;
}

function getDevSyncProgress(kind) {
  return { ...(kind === 'launcher' ? launcherSyncProgress : gameSyncProgress) };
}

// ─── Logging ────────────────────────────────────────────────────────────
const DEBUG = false;
const LOG_MAX = 500;
const sseClients = [];
const adminLogs = [];

function log(tag, msg, extra) {
  const line = `[${tag}] ${msg}`;
  if (extra !== undefined) console.log(line, extra);
  else console.log(line);
  const logMsg = extra !== undefined ? `${msg} ${JSON.stringify(extra)}` : msg;
  const entry = { id: 'admin', level: 'stdout', msg: `[${tag}] ${logMsg}`, time: Date.now() };
  adminLogs.push(entry);
  if (adminLogs.length > LOG_MAX) adminLogs.shift();
  sseClients.forEach(res => { try { res.write(`data: ${JSON.stringify(entry)}\n\n`); } catch {} });
}

function addLog(id, level, msg) {
  const entry = { id, level, msg, time: Date.now() };
  if (services[id]) {
    services[id].logs.push(entry);
    if (services[id].logs.length > LOG_MAX) services[id].logs.shift();
  }
  sseClients.forEach(res => { try { res.write(`data: ${JSON.stringify(entry)}\n\n`); } catch {} });
}

// ─── Process Manager ───────────────────────────────────────────────────
const services = {
  mongodb: { name: 'MongoDB', exe: MONGO_EXE, args: ['--dbpath', MONGO_DB, '--config', MONGO_CFG], cwd: path.join(ROOT, 'MongoDb'), proc: null, logs: [], color: '#4aaa4a' },
  xmpp: { name: 'XmppServerTcp', exe: NODE, args: ['--tls-min-v1.0', 'index'], cwd: path.join(ROOT, 'XmppServerTcp'), proc: null, logs: [], color: '#4a8aba' },
  conference: { name: 'ComponentConference', exe: NODE, args: ['index'], cwd: path.join(ROOT, 'ComponentConference'), proc: null, logs: [], color: '#c8a01a' },
  wfc: { name: 'ComponentWFC', exe: NODE, args: ['index'], cwd: path.join(ROOT, 'ComponentWFC'), proc: null, logs: [], color: '#c8a01a' },
  pve: { name: 'Masterserver PvE', exe: NODE, args: ['index.js', 'server_id=1', 'min_rank=1', 'max_rank=90', 'channel=pve', 'resource=pve_001', 'rank_group=all', 'bootstrap=', 'ver=1.22400.5519.45100', 'max_users=1000'], cwd: MASTER_DIR, proc: null, logs: [], color: '#c8371a' },
  pvp: { name: 'Masterserver PvP', exe: NODE, args: ['index.js', 'server_id=301', 'min_rank=1', 'max_rank=90', 'channel=pvp_pro', 'resource=pvp_pro_001', 'rank_group=all', 'bootstrap=', 'ver=1.22400.5519.45100', 'max_users=1000'], cwd: MASTER_DIR, proc: null, logs: [], color: '#c8371a' },
  cache: { name: 'Cache Shop', exe: NODE, args: ['index', 'modules=shop'], cwd: path.join(ROOT, 'Tools', 'Cache'), proc: null, logs: [], color: '#2a6a9a' },
  dedicated_pve: { name: 'Dedicated PvE', exe: DEDICATED_EXE, args: ['pve_001', '65011', '65011', '-simple_console'], cwd: DEDICATED_DIR, proc: null, logs: [], color: '#9a4aaa' },
  dedicated_pvp: { name: 'Dedicated PvP', exe: DEDICATED_EXE, args: ['pvp_pro_001', '65000', '65000', '-simple_console'], cwd: DEDICATED_DIR, proc: null, logs: [], color: '#aa6a3a' },
};

const servicePorts = {
  mongodb: [27017],
  xmpp: [5222, 5224, 5347, 8080],
  dedicated_pvp: [65000],
  dedicated_pve: [65011],
};
// Cache Shop rebuilds cache.shop from XML and overwrites live edits, so keep it manual.
// Mantemos 1 dedicado PvE + 1 dedicado PvP fixos no boot para entrada imediata em sala.
const serviceBootOrder = ['mongodb', 'xmpp', 'conference', 'wfc', 'pve', 'pvp', 'dedicated_pve', 'dedicated_pvp'];

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

function getServiceAvailability(id) {
  const svc = services[id];
  if (!svc) return { available: false, reason: 'Servico invalido' };
  if (!svc.cwd || !fs.existsSync(svc.cwd)) {
    return { available: false, reason: `Pasta de trabalho nao encontrada: ${svc.cwd}` };
  }
  if (!svc.exe || !fs.existsSync(svc.exe)) {
    return { available: false, reason: `Executavel nao encontrado: ${svc.exe}` };
  }
  return { available: true, reason: '' };
}

function isProcAlive(proc) {
  return !!(proc && proc.pid && proc.exitCode === null && !proc.killed);
}

function checkTcpPort(port, host = '127.0.0.1', timeout = 250) {
  return new Promise(resolve => {
    const socket = new net.Socket();
    let done = false;
    let timer;
    const finish = ok => {
      if (done) return;
      done = true;
      if (timer) clearTimeout(timer);
      try { socket.destroy(); } catch {}
      resolve(ok);
    };
    timer = setTimeout(() => finish(false), timeout);
    socket.once('connect', () => finish(true));
    socket.once('error', () => finish(false));
    socket.once('close', () => {
      if (!done) finish(false);
    });
    try { socket.connect(port, host); } catch { finish(false); }
  });
}

async function waitForPorts(id, timeout = 12000) {
  const ports = servicePorts[id] || [];
  if (ports.length === 0) return true;
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const results = await Promise.all(ports.map(port => checkTcpPort(port)));
    if (results.every(Boolean)) return true;
    await sleep(350);
  }
  return false;
}

function parsePidLines(output) {
  const raw = String(output || '').split(/\r?\n/);
  const pids = raw
    .map(line => Number(String(line).trim()))
    .filter(pid => Number.isSafeInteger(pid) && pid > 0);
  return [...new Set(pids)];
}

function findExternalServiceProcesses(id) {
  return new Promise(resolve => {
    const svc = services[id];
    if (!svc || !id.startsWith('dedicated_')) return resolve([]);
    const marker = String((svc.args || [])[0] || '').trim();
    if (!marker) return resolve([]);
    const safeMarker = marker.replace(/'/g, "''");

    const command = [
      '$pids = Get-CimInstance Win32_Process | Where-Object {',
      "  ($_.Name -ieq 'DedicatedStarterMany.exe' -or $_.Name -ieq 'DedicatedServer.exe') -and",
      `  $_.CommandLine -like '*${safeMarker}*'`,
      '} | Select-Object -ExpandProperty ProcessId',
      '$pids'
    ].join(' ');

    execFile('powershell.exe', ['-NoProfile', '-Command', command], { windowsHide: true, timeout: 1800 }, (err, stdout) => {
      if (err || !stdout) return resolve([]);
      resolve(parsePidLines(stdout));
    });
  });
}

function findExternalServiceProcessesSync(id) {
  const svc = services[id];
  if (!svc || !id.startsWith('dedicated_')) return [];
  const marker = String((svc.args || [])[0] || '').trim();
  if (!marker) return [];
  const safeMarker = marker.replace(/'/g, "''");

  const command = [
    '$pids = Get-CimInstance Win32_Process | Where-Object {',
    "  ($_.Name -ieq 'DedicatedStarterMany.exe' -or $_.Name -ieq 'DedicatedServer.exe') -and",
    `  $_.CommandLine -like '*${safeMarker}*'`,
    '} | Select-Object -ExpandProperty ProcessId',
    '$pids'
  ].join(' ');

  try {
    const stdout = execFileSync('powershell.exe', ['-NoProfile', '-Command', command], { windowsHide: true, timeout: 1800, encoding: 'utf8' });
    return parsePidLines(stdout);
  } catch {
    return [];
  }
}

async function findExternalServiceProcess(id) {
  const pids = await findExternalServiceProcesses(id);
  return pids.length ? pids[0] : null;
}

function findExternalServiceProcessSync(id) {
  const pids = findExternalServiceProcessesSync(id);
  return pids.length ? pids[0] : null;
}

function killProcessTree(pid) {
  if (!Number.isSafeInteger(Number(pid))) return false;
  try {
    execFileSync('taskkill.exe', ['/T', '/F', '/PID', String(pid)], { stdio: 'ignore', windowsHide: true, timeout: 5000 });
    return true;
  } catch {
    return false;
  }
}

function killNodeUdpConflictsOnDedicatedPorts() {
  const psCommand = [
    `$ports = @(${DEDICATED_ROOM_PORTS.join(',')});`,
    '$killed = @();',
    '$pids = Get-NetUDPEndpoint -ErrorAction SilentlyContinue |',
    '  Where-Object { $ports -contains $_.LocalPort } |',
    '  Select-Object -ExpandProperty OwningProcess -Unique;',
    'foreach ($targetPid in $pids) {',
    '  try {',
    '    $proc = Get-CimInstance Win32_Process -Filter ("ProcessId=" + $targetPid) -ErrorAction SilentlyContinue;',
    "    if ($proc -and $proc.Name -ieq 'node.exe') {",
    '      taskkill /T /F /PID $targetPid | Out-Null;',
    '      $killed += [int]$targetPid;',
    '    }',
    '  } catch {}',
    '}',
    '$killed'
  ].join(' ');
  try {
    const stdout = execFileSync('powershell.exe', ['-NoProfile', '-Command', psCommand], { windowsHide: true, timeout: 5000, encoding: 'utf8' });
    return parsePidLines(stdout);
  } catch {
    return [];
  }
}

function killDedicatedByMarker(marker) {
  const safeMarker = String(marker || '').replace(/'/g, "''").trim();
  if (!safeMarker) return;
  const safeBin64 = BIN64.replace(/'/g, "''").toLowerCase();
  const command = [
    `$bin64 = '${safeBin64}';`,
    '$pids = Get-CimInstance Win32_Process | Where-Object {',
    "  ($_.Name -ieq 'DedicatedStarterMany.exe' -or $_.Name -ieq 'DedicatedServer.exe') -and",
    "  ($_.ExecutablePath -and $_.ExecutablePath.ToLowerInvariant().StartsWith($bin64)) -and",
    `  $_.CommandLine -like '*${safeMarker}*'`,
    '} | Select-Object -ExpandProperty ProcessId',
    'foreach ($targetPid in $pids) {',
    '  try { taskkill /T /F /PID $targetPid | Out-Null } catch {}',
    '}'
  ].join(' ');
  try {
    execFileSync('powershell.exe', ['-NoProfile', '-Command', command], { stdio: 'ignore', windowsHide: true, timeout: 5000 });
  } catch {}
}

function findProcessesByExecutablePathsSync(executablePaths, excludePids = []) {
  const paths = executablePaths
    .filter(Boolean)
    .map(p => path.resolve(p).replace(/'/g, "''").toLowerCase());
  if (!paths.length) return [];
  const psPaths = paths.map(p => `'${p}'`).join(',');
  const exclude = excludePids
    .map(pid => Number(pid))
    .filter(pid => Number.isSafeInteger(pid) && pid > 0);
  const psExclude = exclude.length ? exclude.join(',') : '';
  const command = [
    `$paths = @(${psPaths});`,
    `$exclude = @(${psExclude});`,
    'Get-CimInstance Win32_Process | Where-Object {',
    '  $_.ExecutablePath -and',
    '  ($paths -contains $_.ExecutablePath.ToLowerInvariant()) -and',
    '  ($exclude -notcontains [int]$_.ProcessId)',
    '} | Select-Object -ExpandProperty ProcessId'
  ].join(' ');
  try {
    const stdout = execFileSync('powershell.exe', ['-NoProfile', '-Command', command], { windowsHide: true, timeout: 2500, encoding: 'utf8' });
    return parsePidLines(stdout);
  } catch {
    return [];
  }
}

function forceKillRuntimeProcessesSync() {
  const targets = [
    DEDICATED_EXE,
    path.join(BIN64, 'DedicatedServer.exe'),
    NODE,
    MONGO_EXE
  ];
  const pids = findProcessesByExecutablePathsSync(targets, [process.pid]);
  const killed = [];
  pids.forEach(pid => {
    if (killProcessTree(pid)) killed.push(pid);
  });
  if (killed.length) {
    Object.keys(services).forEach(id => {
      const proc = services[id].proc;
      if (proc && killed.includes(proc.pid)) services[id].proc = null;
    });
    log('SVC', `Hard stop removeu processos restantes: ${killed.join(', ')}`);
  }
  return killed;
}

async function getServiceStatus(id) {
  const svc = services[id];
  const availability = getServiceAvailability(id);
  const ports = servicePorts[id] || [];
  const portStatus = {};
  if (ports.length) {
    const checks = await Promise.all(ports.map(async port => [port, await checkTcpPort(port)]));
    checks.forEach(([port, ok]) => { portStatus[port] = ok; });
  }
  const procRunning = isProcAlive(svc.proc);
  const portsReady = ports.length ? Object.values(portStatus).every(Boolean) : false;
  const externalPids = await findExternalServiceProcesses(id);
  const externalPid = externalPids.length ? externalPids[0] : null;
  const running = procRunning || portsReady || externalPids.length > 0;
  const onDemand = !!svc.onDemand;
  return {
    name: svc.name,
    running,
    ready: ports.length ? portsReady : running,
    onDemand,
    autoStart: svc.autoStart !== false,
    managed: procRunning,
    managedPid: procRunning && svc.proc && svc.proc.pid ? svc.proc.pid : null,
    externalPid,
    externalPids,
    externalCount: externalPids.length,
    ports: portStatus,
    available: availability.available,
    unavailableReason: availability.reason,
    color: svc.color
  };
}

async function startService(id) {
  const svc = services[id];
  if (!svc) return false;
  if (isProcAlive(svc.proc)) return true;
  if (id.startsWith('dedicated_')) {
    const killedNodePids = killNodeUdpConflictsOnDedicatedPorts();
    if (killedNodePids.length) {
      addLog(id, 'info', `[${svc.name}] Portas dedicadas liberadas (node/udp): ${killedNodePids.join(', ')}`);
    }
    await sleep(250);
  }
  const ports = servicePorts[id] || [];
  if (ports.length && (await waitForPorts(id, 500))) {
    addLog(id, 'info', `[${svc.name}] Ja esta ativo nas portas: ${ports.join(', ')}`);
    return true;
  }
  const externalPids = await findExternalServiceProcesses(id);
  if (externalPids.length) {
    addLog(id, 'info', `[${svc.name}] Ja esta ativo fora do painel (${externalPids.length} processo(s): ${externalPids.join(', ')})`);
    return true;
  }
  const availability = getServiceAvailability(id);
  if (!availability.available) {
    addLog(id, 'error', `[${svc.name}] ${availability.reason}`);
    return false;
  }
  log('SVC', `Iniciando ${svc.name}`);
  addLog(id, 'info', `[${svc.name}] Iniciando...`);
  try {
    const child = spawn(svc.exe, svc.args, { cwd: svc.cwd, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
    svc.proc = child;
    child.stdout.on('data', (d) => d.toString().split('\n').filter(l=>l).forEach(l => addLog(id, 'stdout', `[${svc.name}] ${l}`)));
    child.stderr.on('data', (d) => d.toString().split('\n').filter(l=>l).forEach(l => addLog(id, 'stderr', `[${svc.name}] ${l}`)));
    child.on('error', (e) => { addLog(id, 'error', `[${svc.name}] Erro: ${e.message}`); svc.proc = null; });
    child.on('exit', (code) => { addLog(id, 'info', `[${svc.name}] Finalizado (codigo ${code})`); svc.proc = null; });
    if (ports.length) {
      const ready = await waitForPorts(id);
      addLog(id, ready ? 'info' : 'error', `[${svc.name}] ${ready ? 'Portas prontas' : 'Nao abriu portas esperadas'}: ${ports.join(', ')}`);
      return ready;
    }
    return true;
  } catch (e) {
    addLog(id, 'error', `[${svc.name}] Falha ao iniciar: ${e.message}`);
    return false;
  }
}

function stopService(id, options = {}) {
  const svc = services[id];
  if (!svc) return;
  const includeExternalDedicated = options.includeExternalDedicated !== false;
  const managedPid = svc.proc && svc.proc.pid ? svc.proc.pid : null;
  const externalPids = !managedPid && includeExternalDedicated && id.startsWith('dedicated_')
    ? findExternalServiceProcessesSync(id)
    : [];
  const pids = managedPid ? [managedPid] : externalPids;
  if (!pids.length) return;
  log('SVC', `Parando ${svc.name}`);
  pids.forEach(pid => {
    try { killProcessTree(pid); } catch {}
  });
  if (id.startsWith('dedicated_')) {
    const marker = String((svc.args || [])[0] || '');
    killDedicatedByMarker(marker);
  }
  try { svc.proc.kill('SIGKILL'); } catch {}
  svc.proc = null;
  addLog(id, 'info', `[${svc.name}] Parado`);
}

function restartService(id) { stopService(id); setTimeout(() => startService(id), 1000); }

async function startAllServices() {
  disableClientMismatchChecks();
  log('SVC', 'Modo hibrido ativo: 1 dedicado PvE + 1 PvP fixos no boot.');
  for (const id of serviceBootOrder) {
    await startService(id);
    if (id === 'mongodb') await waitForPorts('mongodb', 15000);
    if (id === 'xmpp') await waitForPorts('xmpp', 15000);
    if (['conference', 'wfc', 'pve', 'pvp'].includes(id)) await sleep(900);
    if (['dedicated_pve', 'dedicated_pvp'].includes(id)) await sleep(1800);
  }
}

function stopAllServices(options = {}) {
  const stopOrder = ['dedicated_pvp', 'dedicated_pve', 'pvp', 'pve', 'cache', 'wfc', 'conference', 'xmpp', 'mongodb'];
  stopOrder.forEach(id => stopService(id));
  const killed = options.hard ? forceKillRuntimeProcessesSync() : [];
  return { killed };
}

// ─── Anti-Cheat Config ─────────────────────────────────────────────────
function readOnlineCfg() { try { return fs.readFileSync(ONLINE_CFG, 'utf8'); } catch { return ''; } }
function writeOnlineCfg(content) { try { fs.writeFileSync(ONLINE_CFG, content, 'utf8'); return true; } catch { return false; } }

function getAcConfig() {
  const cfg = readOnlineCfg();
  return {
    checkCertificate: !cfg.includes('online_check_certificate = 0'),
    useProtect: cfg.includes('online_use_protect = 1'),
    cvarHash: !cfg.includes('sv_cvars_hash_enable = 0'),
    antiCheatHash: !cfg.includes('anti_cheat_exe_hash_validation = 0'),
    releaseBuild: cfg.includes('cl_release_build = 1'),
    consoleRestricted: cfg.includes('con_restricted = 1'),
    deactivateConsole: cfg.includes('sys_DeactivateConsole = 1'),
    raw: cfg,
  };
}

function setAcFlag(flag, enabled) {
  let cfg = readOnlineCfg();
  const lines = cfg.split('\n');
  const flagMap = {
    checkCertificate: { on: 'online_check_certificate = 1', off: 'online_check_certificate = 0' },
    useProtect: { on: 'online_use_protect = 1', off: 'online_use_protect = 0' },
    cvarHash: { on: 'sv_cvars_hash_enable = 1', off: 'sv_cvars_hash_enable = 0' },
    antiCheatHash: { on: 'anti_cheat_exe_hash_validation = 1', off: 'anti_cheat_exe_hash_validation = 0' },
    releaseBuild: { on: 'cl_release_build = 1', off: 'cl_release_build = 0' },
    consoleRestricted: { on: 'con_restricted = 1', off: 'con_restricted = 0' },
    deactivateConsole: { on: 'sys_DeactivateConsole = 1', off: 'sys_DeactivateConsole = 0' },
  };
  const f = flagMap[flag];
  if (!f) return false;
  const onLine = f.on;
  const offLine = f.off;
  let found = false;
  const newLines = lines.map(l => {
    const trimmed = l.trim();
    if (trimmed === onLine || trimmed === offLine) { found = true; return enabled ? f.on : f.off; }
    return l;
  });
  if (!found) newLines.push(enabled ? f.on : f.off);
  return writeOnlineCfg(newLines.join('\n'));
}

function disableClientMismatchChecks() {
  setAcFlag('checkCertificate', false);
  setAcFlag('useProtect', false);
  setAcFlag('cvarHash', false);
  setAcFlag('antiCheatHash', false);
}

// ─── Helpers ───────────────────────────────────────────────────────────
function json(res, data, status = 200) {
  log('JSON', `status=${status}`, JSON.stringify(data).substring(0, 200));
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*' });
  res.end(JSON.stringify(data));
}

function safeCompareText(a, b) {
  const aa = Buffer.from(String(a || ''), 'utf8');
  const bb = Buffer.from(String(b || ''), 'utf8');
  if (aa.length !== bb.length) return false;
  try {
    return crypto.timingSafeEqual(aa, bb);
  } catch {
    return false;
  }
}

function isLoopbackRemoteAddress(remoteAddress) {
  const addr = String(remoteAddress || '').trim();
  if (!addr) return false;
  return addr === '127.0.0.1' || addr === '::1' || addr === '::ffff:127.0.0.1';
}

function auth(req) {
  // TEMP: auth bypass requested for local development/admin panel usage.
  // Re-enable token checks by removing this early return.
  return true;

  const provided = String(req && req.headers && req.headers['x-auth-token'] || '').trim();

  if (ADMIN_AUTH_TOKEN) {
    return safeCompareText(provided, ADMIN_AUTH_TOKEN);
  }

  if (!isLoopbackRemoteAddress(req && req.socket && req.socket.remoteAddress)) {
    return false;
  }

  return safeCompareText(provided, 'local-no-auth');
}

function getDefaultRuntimePayload() {
  return {
    configVersion: String(Date.now()),
    server: {
      host: 'warface',
      ip: '127.0.0.1',
      port: 1050,
      useTLS: true,
      useProtect: false,
      checkCertificate: false,
      disableHashValidation: true,
      disableAntiCheat: true
    },
    links: {
      discordInviteUrl: 'https://discord.gg/YOUR_INVITE'
    },
    launcherUi: {
      slides: [],
      news: []
    },
    urls: {
      gameVersionUrl: '',
      gameManifestUrl: '',
      launcherConfigUrl: ''
    }
  };
}

function sanitizeRuntimeString(value, maxLen = 512) {
  return String(value || '').trim().slice(0, maxLen);
}

function validateRuntimeUrl(raw, label, required = false) {
  const value = sanitizeRuntimeString(raw, 2048);
  if (!value) {
    if (required) throw new Error(`${label} obrigatoria`);
    return '';
  }

  let parsed;
  try {
    parsed = new URL(value);
  } catch {
    throw new Error(`${label} invalida`);
  }

  const localHost = parsed.hostname === '127.0.0.1' || parsed.hostname === 'localhost' || parsed.hostname === '::1';
  if (parsed.protocol !== 'https:' && !(localHost && parsed.protocol === 'http:')) {
    throw new Error(`${label} deve usar HTTPS (ou HTTP local)`);
  }
  if (parsed.username || parsed.password) {
    throw new Error(`${label} nao pode conter credenciais`);
  }

  return parsed.toString();
}

function sanitizeRuntimeConfigPayload(payload) {
  const base = getDefaultRuntimePayload();
  const safe = payload && typeof payload === 'object' ? payload : {};
  const merged = { ...base, ...safe };

  const host = sanitizeRuntimeString(merged.server && merged.server.host, 120);
  const ip = sanitizeRuntimeString(merged.server && merged.server.ip, 255);
  const port = Number.parseInt(String(merged.server && merged.server.port || ''), 10);

  if (!host) throw new Error('Servidor Host invalido');
  if (!ip) throw new Error('Servidor IP/Hostname invalido');
  if (!Number.isFinite(port) || port < 1 || port > 65535) throw new Error('Porta invalida (1-65535)');

  return {
    configVersion: sanitizeRuntimeString(merged.configVersion || Date.now(), 64),
    server: {
      ...base.server,
      ...(safe.server && typeof safe.server === 'object' ? safe.server : {}),
      host,
      ip,
      port
    },
    links: {
      discordInviteUrl: validateRuntimeUrl(merged.links && merged.links.discordInviteUrl, 'Discord Invite URL', true)
    },
    launcherUi: merged.launcherUi && typeof merged.launcherUi === 'object' ? merged.launcherUi : base.launcherUi,
    urls: {
      gameVersionUrl: validateRuntimeUrl(merged.urls && merged.urls.gameVersionUrl, 'URL Game Version', true),
      gameManifestUrl: validateRuntimeUrl(merged.urls && merged.urls.gameManifestUrl, 'URL Game Manifest', true),
      launcherConfigUrl: validateRuntimeUrl(merged.urls && merged.urls.launcherConfigUrl, 'URL Launcher Config', true)
    }
  };
}

function readRuntimeConfigPayload() {
  try {
    if (!fs.existsSync(LAUNCHER_RUNTIME_CONFIG_FILE)) return getDefaultRuntimePayload();
    const parsed = JSON.parse(fs.readFileSync(LAUNCHER_RUNTIME_CONFIG_FILE, 'utf8'));
    if (!parsed || typeof parsed !== 'object') return getDefaultRuntimePayload();
    return sanitizeRuntimeConfigPayload(parsed);
  } catch {
    return getDefaultRuntimePayload();
  }
}

function writeRuntimeConfigPayload(payload) {
  const next = sanitizeRuntimeConfigPayload(payload);
  fs.writeFileSync(LAUNCHER_RUNTIME_CONFIG_FILE, JSON.stringify(next, null, 2), 'utf8');
  return next;
}

function signRuntimeEnvelope(envelope) {
  const payloadText = JSON.stringify(envelope && envelope.payload && typeof envelope.payload === 'object' ? envelope.payload : {});
  const base = [
    String(envelope.type || ''),
    String(envelope.schemaVersion || ''),
    String(envelope.configVersion || ''),
    String(envelope.timestamp || ''),
    String(envelope.nonce || ''),
    payloadText
  ].join('|');

  if (!RUNTIME_HMAC_SECRET) return '';
  return crypto.createHmac('sha256', RUNTIME_HMAC_SECRET).update(base).digest('hex');
}

function buildRuntimeEnvelope(type, payload) {
  const safePayload = payload && typeof payload === 'object' ? payload : getDefaultRuntimePayload();
  const envelope = {
    type,
    schemaVersion: RUNTIME_SCHEMA_VERSION,
    configVersion: String(safePayload.configVersion || Date.now()),
    timestamp: Date.now(),
    nonce: crypto.randomBytes(16).toString('hex'),
    payload: safePayload
  };
  envelope.signature = signRuntimeEnvelope(envelope);
  return envelope;
}

function broadcastRuntimeEnvelope(envelope) {
  const text = JSON.stringify(envelope);
  for (const ws of runtimeWsClients) {
    try {
      if (ws.readyState === 1) ws.send(text);
    } catch {}
  }
}

async function getPublicServerInfoPayload() {
  let online = 0;
  try {
    const raw = await apiGet(`${XMPP_API}/getonline`);
    if (raw) {
      try { online = JSON.parse(raw).online || 0; } catch {}
    }
  } catch {}

  let players = 0;
  try {
    await withMongo(async (db) => {
      players = await db.collection('profiles').countDocuments();
    });
  } catch {}

  return {
    online: Number(online || 0),
    players: Number(players || 0),
    version: '1.22400.5519.45100',
    uptime: process.uptime()
  };
}

async function broadcastServerStatusSnapshot() {
  if (!runtimeWsClients.size) return;
  try {
    const info = await getPublicServerInfoPayload();
    const envelope = buildRuntimeEnvelope('server_status', {
      ...info,
      status: info.online > 0 ? 'online' : 'offline',
      timestamp: Date.now()
    });
    broadcastRuntimeEnvelope(envelope);
  } catch {}
}

function serveStatic(res, filePath) {
  log('STATIC', `serving: ${filePath}`);
  const ext = path.extname(filePath);
  fs.readFile(filePath, (err, data) => {
    if (err) {
      log('STATIC', `NOT FOUND: ${filePath}`);
      json(res, { error: 'Not Found' }, 404);
      return;
    }
    log('STATIC', `OK: ${filePath} (${data.length} bytes)`);
    const headers = { 'Content-Type': MIME[ext] || 'text/plain' };
    if (['.html', '.css', '.js'].includes(ext)) {
      headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, proxy-revalidate';
      headers['Pragma'] = 'no-cache';
      headers['Expires'] = '0';
    }
    res.writeHead(200, headers);
    res.end(data);
  });
}

function parseBody(req) {
  return new Promise(r => { let b = ''; req.on('data', c => b += c); req.on('end', () => { try { r(JSON.parse(b)); } catch { r({}); } }); });
}

function normalizeVersionLabel(value) {
  const version = asTrimmedString(value);
  if (!version) return '';
  return /^[A-Za-z0-9._-]+$/.test(version) ? version : '';
}

function nextVersionLabel(current) {
  const raw = normalizeVersionLabel(current) || '0.0.0';
  const parts = raw.split('.');
  for (let i = parts.length - 1; i >= 0; i--) {
    if (/^\d+$/.test(parts[i])) {
      parts[i] = String(Number(parts[i]) + 1);
      return parts.join('.');
    }
  }
  return `${raw}.${Date.now()}`;
}

function getPublicManifestUrl(req) {
  const host = req.headers.host || `localhost:${PORT}`;
  return `http://${host}/api/public/game-manifest`;
}

function getPublicGameCdnBase(req) {
  const host = req.headers.host || `localhost:${PORT}`;
  return `http://${host}/cdn/game/`;
}

function getPublicLauncherManifestUrl(req) {
  const host = req.headers.host || `localhost:${PORT}`;
  return `http://${host}/api/public/launcher-manifest`;
}

function getBaseUrl(req) {
  const host = (req && req.headers && req.headers.host) ? req.headers.host : `localhost:${PORT}`;
  return `http://${host}/`;
}

function buildLauncherManifestUrl(baseUrl, req) {
  const base = normalizeBaseUrl(baseUrl);
  if (base) return `${base}launcher-manifest.json`;
  return getPublicLauncherManifestUrl(req);
}

function normalizeBaseUrl(value) {
  const raw = asTrimmedString(value);
  if (!raw) return '';
  return raw.endsWith('/') ? raw : `${raw}/`;
}

function normalizePublicBaseForPrefix(baseUrl, prefix) {
  const base = normalizeBaseUrl(baseUrl);
  const cleanPrefix = normalizeR2Key(prefix);
  if (!base || !cleanPrefix) return base;
  try {
    const url = new URL(base);
    const suffix = `/${cleanPrefix}/`;
    while (url.pathname.endsWith(suffix)) {
      url.pathname = url.pathname.slice(0, -suffix.length + 1) || '/';
    }
    return normalizeBaseUrl(url.toString());
  } catch {}
  return base;
}

function normalizeR2Key(value) {
  const raw = asTrimmedString(value).replace(/\\/g, '/').replace(/^\/+/, '');
  if (!raw) return '';
  const normalized = path.posix.normalize(raw);
  if (!normalized || normalized === '.' || normalized.startsWith('../') || normalized.includes('/../')) return '';
  return normalized;
}

function readR2Config() {
  if (!fs.existsSync(R2_CONFIG_FILE)) return { enabled: false };
  try {
    const data = JSON.parse(fs.readFileSync(R2_CONFIG_FILE, 'utf8').replace(/^\uFEFF/, ''));
    const endpointUrl = new URL(String(data.endpoint || ''));
    const endpointPath = endpointUrl.pathname.replace(/^\/+|\/+$/g, '');
    const bucket = asTrimmedString(data.bucket) || endpointPath.split('/').filter(Boolean)[0] || '';
    const publicBaseUrl = normalizeBaseUrl(data.publicBaseUrl || data.public_base_url);
    return {
      enabled: data.enabled !== false,
      endpoint: `${endpointUrl.protocol}//${endpointUrl.host}`,
      bucket,
      accessKeyId: asTrimmedString(data.accessKeyId || data.access_key_id),
      secretAccessKey: asTrimmedString(data.secretAccessKey || data.secret_access_key),
      publicBaseUrl,
      prefix: normalizeR2Key(data.prefix || ''),
      sourceDir: path.resolve(asTrimmedString(data.sourceDir || data.source_dir || GAME_DIR))
    };
  } catch (e) {
    return { enabled: false, error: e.message };
  }
}

function writeR2SourceDir(sourceDir) {
  const raw = asTrimmedString(sourceDir);
  if (!raw) throw new Error('Pasta do client obrigatoria');
  const selected = path.resolve(raw);
  if (!fs.existsSync(selected) || !fs.statSync(selected).isDirectory()) {
    throw new Error(`Pasta do client nao encontrada: ${selected}`);
  }
  const hasGameExe = fs.existsSync(path.join(selected, 'Bin64', 'Game.exe')) || fs.existsSync(path.join(selected, 'Game.exe'));
  if (!hasGameExe) {
    throw new Error('Pasta invalida: selecione a raiz do client, contendo Bin64\\Game.exe ou Game.exe');
  }

  let data = {};
  if (fs.existsSync(R2_CONFIG_FILE)) {
    data = JSON.parse(fs.readFileSync(R2_CONFIG_FILE, 'utf8').replace(/^\uFEFF/, ''));
  }
  data.sourceDir = selected;
  fs.writeFileSync(R2_CONFIG_FILE, JSON.stringify(data, null, 2), 'utf8');
  return selected;
}

function readLauncherSourceDir() {
  const fallback = path.join(PUBLIC_DIR);
  if (!fs.existsSync(R2_CONFIG_FILE)) return fallback;
  try {
    const data = JSON.parse(fs.readFileSync(R2_CONFIG_FILE, 'utf8').replace(/^\uFEFF/, ''));
    const selected = path.resolve(asTrimmedString(data.launcherSourceDir || data.launcher_source_dir || fallback));
    return fs.existsSync(selected) && fs.statSync(selected).isDirectory() ? selected : fallback;
  } catch {
    return fallback;
  }
}

function writeLauncherSourceDir(sourceDir) {
  const raw = asTrimmedString(sourceDir);
  if (!raw) throw new Error('Pasta do launcher obrigatoria');
  const selected = path.resolve(raw);
  if (!fs.existsSync(selected) || !fs.statSync(selected).isDirectory()) {
    throw new Error(`Pasta do launcher nao encontrada: ${selected}`);
  }
  const hasPackageJson = fs.existsSync(path.join(selected, 'package.json'));
  const hasLauncherConfig = fs.existsSync(path.join(selected, 'launcher-config.json')) || fs.existsSync(path.join(selected, 'public', 'launcher-config.json'));
  if (!hasPackageJson && !hasLauncherConfig) {
    throw new Error('Pasta invalida: selecione a raiz do projeto launcher (package.json) ou pasta com launcher-config.json');
  }
  let data = {};
  if (fs.existsSync(R2_CONFIG_FILE)) {
    data = JSON.parse(fs.readFileSync(R2_CONFIG_FILE, 'utf8').replace(/^\uFEFF/, ''));
  }
  data.launcherSourceDir = selected;
  fs.writeFileSync(R2_CONFIG_FILE, JSON.stringify(data, null, 2), 'utf8');
  return selected;
}

function requireR2Config() {
  const cfg = readR2Config();
  if (cfg.error) throw new Error(`Config R2 invalida: ${cfg.error}`);
  if (!cfg.enabled) throw new Error('Cloudflare R2 nao configurado');
  if (!cfg.endpoint || !cfg.bucket || !cfg.accessKeyId || !cfg.secretAccessKey || !cfg.publicBaseUrl) {
    throw new Error('Cloudflare R2 incompleto: endpoint, bucket, accessKeyId, secretAccessKey e publicBaseUrl sao obrigatorios');
  }
  return cfg;
}

function r2ObjectKey(cfg, relPath) {
  const rel = normalizeR2Key(relPath);
  if (!rel) throw new Error(`Objeto R2 invalido: ${relPath}`);
  return cfg.prefix ? `${cfg.prefix}/${rel}` : rel;
}

function encodeS3Path(value) {
  const encodeRfc3986 = (part) => encodeURIComponent(part).replace(/[!'()*]/g, ch => `%${ch.charCodeAt(0).toString(16).toUpperCase()}`);
  return String(value || '').split('/').map(part => encodeRfc3986(part)).join('/');
}

function hmacSha256(key, value, encoding) {
  return crypto.createHmac('sha256', key).update(value, 'utf8').digest(encoding);
}

function sha256Hex(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function awsDateParts(date = new Date()) {
  const iso = date.toISOString().replace(/[:-]|\.\d{3}/g, '');
  return { amzDate: iso, dateStamp: iso.slice(0, 8) };
}

function r2SigningKey(secretAccessKey, dateStamp) {
  const kDate = hmacSha256(`AWS4${secretAccessKey}`, dateStamp);
  const kRegion = hmacSha256(kDate, 'auto');
  const kService = hmacSha256(kRegion, 's3');
  return hmacSha256(kService, 'aws4_request');
}

function r2SignedHeaders(cfg, method, key, options = {}) {
  const endpoint = new URL(cfg.endpoint);
  const payloadHash = options.payloadHash || sha256Hex(options.body || Buffer.alloc(0));
  const { amzDate, dateStamp } = awsDateParts();
  const canonicalUri = `/${encodeS3Path(cfg.bucket)}/${encodeS3Path(key)}`;
  const headers = {
    host: endpoint.host,
    'x-amz-content-sha256': payloadHash,
    'x-amz-date': amzDate
  };
  if (options.contentType) headers['content-type'] = options.contentType;
  if (options.metadata && typeof options.metadata === 'object') {
    for (const [metaKey, metaValue] of Object.entries(options.metadata)) {
      headers[`x-amz-meta-${metaKey}`] = String(metaValue);
    }
  }

  const signedHeaderNames = Object.keys(headers).map(k => k.toLowerCase()).sort();
  const canonicalHeaders = signedHeaderNames.map(name => `${name}:${String(headers[name]).trim()}\n`).join('');
  const signedHeaders = signedHeaderNames.join(';');
  const canonicalRequest = [
    method,
    canonicalUri,
    '',
    canonicalHeaders,
    signedHeaders,
    payloadHash
  ].join('\n');
  const scope = `${dateStamp}/auto/s3/aws4_request`;
  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    scope,
    sha256Hex(canonicalRequest)
  ].join('\n');
  const signature = hmacSha256(r2SigningKey(cfg.secretAccessKey, dateStamp), stringToSign, 'hex');
  headers.authorization = `AWS4-HMAC-SHA256 Credential=${cfg.accessKeyId}/${scope}, SignedHeaders=${signedHeaders}, Signature=${signature}`;
  if (Number.isFinite(options.contentLength)) headers['content-length'] = String(options.contentLength);
  return { endpoint, canonicalUri, headers };
}

function r2Request(cfg, method, key, options = {}) {
  return new Promise((resolve, reject) => {
    const { endpoint, canonicalUri, headers } = r2SignedHeaders(cfg, method, key, options);
    let done = false;
    const finish = (fn, value) => {
      if (done) return;
      done = true;
      fn(value);
    };
    const req = https.request({
      hostname: endpoint.hostname,
      port: endpoint.port || 443,
      method,
      path: canonicalUri,
      headers
    }, res => {
      let body = '';
      res.on('data', chunk => body += chunk.toString('utf8'));
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          finish(resolve, { statusCode: res.statusCode, headers: res.headers, body });
          return;
        }
        finish(reject, new Error(`R2 ${method} ${key} falhou: HTTP ${res.statusCode} ${body.slice(0, 300)}`));
      });
    });
    req.on('error', error => finish(reject, error));
    req.setTimeout(Number(options.timeoutMs || 300000), () => {
      req.destroy(new Error(`Timeout R2 em ${method} ${key}`));
    });
    if (options.filePath) {
      const stream = fs.createReadStream(options.filePath);
      stream.on('error', error => {
        req.destroy(error);
        finish(reject, error);
      });
      stream.on('data', chunk => {
        if (done) return;
        if (typeof options.onUploadProgress === 'function') options.onUploadProgress(chunk.length);
        if (!req.write(chunk)) stream.pause();
      });
      req.on('drain', () => stream.resume());
      stream.on('end', () => {
        if (!done) req.end();
      });
    } else {
      req.end(options.body || Buffer.alloc(0));
    }
  });
}

async function retryR2(label, fn, attempts = 3) {
  let lastError = null;
  for (let i = 1; i <= attempts; i++) {
    try {
      return await fn();
    } catch (e) {
      lastError = e;
      log('R2', `${label} tentativa ${i}/${attempts} falhou: ${e.message}`);
      if (i < attempts) await new Promise(resolve => setTimeout(resolve, 1500 * i));
    }
  }
  throw lastError;
}

function r2PutFile(cfg, relPath, filePath, fileMeta, options = {}) {
  const key = r2ObjectKey(cfg, relPath);
  const payloadHash = fileSha256(filePath).toLowerCase();
  return r2Request(cfg, 'PUT', key, {
    filePath,
    payloadHash,
    contentLength: Number((fileMeta && fileMeta.size) || fs.statSync(filePath).size),
    contentType: MIME[path.extname(relPath).toLowerCase()] || 'application/octet-stream',
    metadata: { 'sha256': payloadHash },
    onUploadProgress: options.onUploadProgress
  });
}

function r2PutJson(cfg, relPath, data) {
  const body = Buffer.from(JSON.stringify(data, null, 2), 'utf8');
  return r2Request(cfg, 'PUT', r2ObjectKey(cfg, relPath), {
    body,
    payloadHash: sha256Hex(body),
    contentLength: body.length,
    contentType: 'application/json; charset=utf-8'
  });
}

async function r2GetJson(cfg, relPath) {
  const res = await r2Request(cfg, 'GET', r2ObjectKey(cfg, relPath), {
    body: Buffer.alloc(0),
    payloadHash: sha256Hex(Buffer.alloc(0)),
    contentLength: 0,
    timeoutMs: 60000
  });
  const body = String(res.body || '').trim();
  if (!body) return null;
  return JSON.parse(body.replace(/^\uFEFF/, ''));
}

function r2DeleteObject(cfg, relPath) {
  return r2Request(cfg, 'DELETE', r2ObjectKey(cfg, relPath), {
    body: Buffer.alloc(0),
    payloadHash: sha256Hex(Buffer.alloc(0)),
    contentLength: 0
  });
}

function r2HeadObject(cfg, relPath) {
  return r2Request(cfg, 'HEAD', r2ObjectKey(cfg, relPath), {
    body: Buffer.alloc(0),
    payloadHash: sha256Hex(Buffer.alloc(0)),
    contentLength: 0,
    timeoutMs: 60000
  });
}

function getDevPublishStateFile(sourceDir) {
  return path.join(path.resolve(sourceDir), '.wf_dev_publish_state.json');
}

function readDevPublishState(sourceDir) {
  try {
    const file = getDevPublishStateFile(sourceDir);
    if (!fs.existsSync(file)) return {};
    const data = JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
    return data && typeof data === 'object' ? data : {};
  } catch {
    return {};
  }
}

function writeDevPublishState(sourceDir, key, manifest) {
  const state = readDevPublishState(sourceDir);
  const files = Array.isArray(manifest && manifest.files)
    ? manifest.files.map(file => normalizePatchEntryPath(file && file.path)).filter(Boolean)
    : [];
  state[key] = {
    version: String(manifest && manifest.version || '0.0.0'),
    generated_at: manifest && manifest.generated_at || null,
    file_count: files.length,
    files,
    syncedAt: new Date().toISOString()
  };
  fs.writeFileSync(getDevPublishStateFile(sourceDir), JSON.stringify(state, null, 2), 'utf8');
  return state[key];
}

function getDevPublishStateInfo(sourceDir, key, remoteManifest) {
  const local = readDevPublishState(sourceDir)[key] || {};
  const remoteVersion = String(remoteManifest && remoteManifest.version || '0.0.0');
  const localVersion = String(local.version || '0.0.0');
  const hasRemote = !!remoteManifest && Array.isArray(remoteManifest.files) && remoteVersion !== '0.0.0';
  return {
    localVersion,
    remoteVersion,
    synced: !hasRemote || localVersion === remoteVersion,
    syncedAt: local.syncedAt || null,
    fileCount: Number(local.file_count || 0)
  };
}

function resolveManifestFilePublicUrl(baseUrl, relPath) {
  const rel = normalizePatchEntryPath(relPath);
  if (!rel) throw new Error(`Caminho invalido no manifest: ${relPath}`);
  const base = normalizeBaseUrl(baseUrl);
  if (!base) throw new Error('Manifest sem base_url');
  return new URL(rel.split('/').map(encodeURIComponent).join('/'), base).toString();
}

function downloadRemoteFile(fileUrl, targetPath, onProgress) {
  return new Promise((resolve, reject) => {
    let redirects = 0;
    let req = null;
    let stream = null;
    const tmpPath = `${targetPath}.download`;
    const cleanup = () => {
      try { if (stream && !stream.closed) stream.close(); } catch {}
      try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
    };
    const start = (nextUrl) => {
      if (redirects > 5) {
        cleanup();
        reject(new Error('Muitos redirecionamentos no download'));
        return;
      }
      let parsed;
      try { parsed = new URL(nextUrl); } catch { reject(new Error(`URL invalida: ${nextUrl}`)); return; }
      const client = parsed.protocol === 'https:' ? https : http;
      fs.mkdirSync(path.dirname(targetPath), { recursive: true });
      stream = fs.createWriteStream(tmpPath);
      req = client.get(parsed, response => {
        if ([301, 302, 303, 307, 308].includes(response.statusCode || 0)) {
          redirects += 1;
          const location = response.headers.location ? new URL(response.headers.location, parsed).toString() : '';
          try { stream.close(); } catch {}
          try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
          if (!location) return reject(new Error('Redirect sem destino'));
          start(location);
          return;
        }
        if ((response.statusCode || 0) < 200 || (response.statusCode || 0) >= 300) {
          cleanup();
          reject(new Error(`HTTP ${response.statusCode} ao baixar ${nextUrl}`));
          return;
        }
        const total = Number(response.headers['content-length'] || 0);
        let downloaded = 0;
        response.on('data', chunk => {
          downloaded += Buffer.isBuffer(chunk) ? chunk.length : Buffer.byteLength(String(chunk || ''));
          if (typeof onProgress === 'function') onProgress({ downloaded, total });
        });
        response.pipe(stream);
      });
      req.on('error', error => { cleanup(); reject(error); });
      req.setTimeout(300000, () => {
        try { req.destroy(new Error('Timeout no download')); } catch {}
      });
      stream.on('finish', () => {
        stream.close(() => {
          try {
            if (fs.existsSync(targetPath)) fs.unlinkSync(targetPath);
            fs.renameSync(tmpPath, targetPath);
            resolve(true);
          } catch (error) {
            cleanup();
            reject(error);
          }
        });
      });
      stream.on('error', error => { cleanup(); reject(error); });
    };
    start(fileUrl);
  });
}

async function readRemoteGameManifest(req) {
  try {
    const data = await r2GetJson(requireR2Config(), 'game-manifest.json');
    if (data && Array.isArray(data.files)) return data;
  } catch {}
  return readGameManifest(req);
}

async function readRemoteLauncherManifest(req, launcherPrefix = 'warface-launcher') {
  const r2 = readR2Config();
  const prefix = normalizeR2Key(launcherPrefix || 'warface-launcher') || 'warface-launcher';
  try {
    if (r2.enabled && !r2.error) {
      const data = await r2GetJson(requireR2Config(), `${prefix}/launcher-manifest.json`);
      if (data && Array.isArray(data.files)) return data;
    }
  } catch {}
  return readLauncherManifest(req);
}

function assertDevPublishReady(sourceDir, key, remoteManifest) {
  const info = getDevPublishStateInfo(sourceDir, key, remoteManifest);
  if (!info.synced) {
    const err = new Error(`Pasta local desatualizada (${info.localVersion} != ${info.remoteVersion}). Sincronize com o CDN antes de publicar.`);
    err.code = 'DEV_FOLDER_OUTDATED';
    err.info = info;
    throw err;
  }
  return info;
}

async function syncLocalFolderFromManifest(sourceDir, manifest, options = {}) {
  const root = path.resolve(sourceDir);
  const key = options.key || 'game';
  const skipFn = typeof options.skip === 'function' ? options.skip : () => false;
  if (!manifest || !Array.isArray(manifest.files)) throw new Error('Manifest remoto invalido');
  if (!normalizeBaseUrl(manifest.base_url)) throw new Error('Manifest remoto sem base_url');

  const previous = readDevPublishState(root)[key] || {};
  const nextPaths = new Set();
  let downloaded = 0;
  let kept = 0;
  let removed = 0;
  const onProgress = typeof options.onProgress === 'function' ? options.onProgress : () => {};
  const candidates = [];

  onProgress({ phase: 'scan', percent: 2, message: 'Verificando arquivos locais', total_files: manifest.files.length, checked_files: 0 });
  for (let i = 0; i < manifest.files.length; i++) {
    const file = manifest.files[i];
    const rel = normalizePatchEntryPath(file && file.path);
    if (!rel || skipFn(rel)) continue;
    nextPaths.add(rel.toLowerCase());
    const target = path.resolve(root, ...rel.split('/'));
    if (!target.startsWith(root + path.sep)) throw new Error(`Caminho fora da pasta local: ${rel}`);
    const exists = fs.existsSync(target);
    const sameSize = exists && fs.statSync(target).size === Number(file.size || 0);
    let sameHash = false;
    if (sameSize) {
      sameHash = (await fileSha256Stream(target)).toLowerCase() === String(file.hash || '').toLowerCase();
    }
    if (sameHash) {
      kept += 1;
    } else {
      candidates.push({ file, rel, target });
    }
    if (i % 10 === 0 || i + 1 === manifest.files.length) {
      onProgress({ phase: 'scan', percent: 2 + ((i + 1) / Math.max(1, manifest.files.length)) * 18, message: `Verificando ${i + 1}/${manifest.files.length}`, total_files: manifest.files.length, checked_files: i + 1, current_file: rel });
    }
    if (i % 5 === 0) await new Promise(resolve => setTimeout(resolve, 0));
  }

  const totalBytes = candidates.reduce((sum, item) => sum + Number(item.file.size || 0), 0);
  let doneBytes = 0;
  onProgress({ phase: 'download', percent: candidates.length ? 20 : 90, message: candidates.length ? `Baixando ${candidates.length} arquivo(s)` : 'Nenhum arquivo para baixar', download_total: candidates.length, download_bytes_total: totalBytes });

  for (let i = 0; i < candidates.length; i++) {
    const { file, rel, target } = candidates[i];
    let currentBytes = 0;
    onProgress({ phase: 'download', percent: 20 + (doneBytes / Math.max(1, totalBytes)) * 70, message: `Baixando ${i + 1}/${candidates.length}: ${rel}`, current_file: rel, download_index: i + 1, download_total: candidates.length, download_bytes_done: doneBytes, download_bytes_current: 0, download_bytes_total: totalBytes });
    await downloadRemoteFile(resolveManifestFilePublicUrl(manifest.base_url, rel), target, progress => {
      currentBytes = Number(progress.downloaded || 0);
      const pct = totalBytes > 0 ? 20 + ((doneBytes + currentBytes) / totalBytes) * 70 : 90;
      onProgress({ phase: 'download', percent: pct, message: `Baixando ${i + 1}/${candidates.length}: ${rel}`, current_file: rel, download_index: i + 1, download_total: candidates.length, download_bytes_done: doneBytes, download_bytes_current: currentBytes, download_bytes_total: totalBytes });
    });
    const actualHash = (await fileSha256Stream(target)).toLowerCase();
    if (String(file.hash || '').toLowerCase() !== actualHash) {
      throw new Error(`Hash invalido apos sincronizar: ${rel}`);
    }
    doneBytes += Number(file.size || currentBytes || 0);
    downloaded += 1;
  }

  onProgress({ phase: 'cleanup', percent: 92, message: 'Removendo arquivos obsoletos', current_file: '', downloaded_count: downloaded, kept_count: kept });

  const tracked = Array.isArray(previous.files) ? previous.files : [];
  for (const oldPath of tracked) {
    const rel = normalizePatchEntryPath(oldPath);
    if (!rel || nextPaths.has(rel.toLowerCase()) || skipFn(rel)) continue;
    const target = path.resolve(root, ...rel.split('/'));
    if (target.startsWith(root + path.sep) && fs.existsSync(target)) {
      try { fs.unlinkSync(target); removed += 1; } catch {}
    }
  }

  const state = writeDevPublishState(root, key, manifest);
  onProgress({ phase: 'complete', percent: 100, message: `Sincronizado com CDN v${state.version}`, downloaded_count: downloaded, kept_count: kept, removed_count: removed, download_bytes_done: totalBytes, download_bytes_current: 0, download_bytes_total: totalBytes });
  return { version: state.version, downloaded, kept, removed, file_count: state.file_count, syncedAt: state.syncedAt };
}

function openFolderDialog(defaultPath, description = 'Selecione a pasta') {
  return new Promise((resolve, reject) => {
    const initial = fs.existsSync(defaultPath || '') ? path.resolve(defaultPath) : GAME_DIR;
    const safeDescription = String(description || 'Selecione a pasta').replace(/'/g, "''");
    const safeInitial = initial.replace(/'/g, "''");
    const script = `
Add-Type -AssemblyName System.Windows.Forms
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$form = New-Object System.Windows.Forms.Form
$form.TopMost = $true
$form.StartPosition = 'CenterScreen'
$form.Width = 1
$form.Height = 1
$form.ShowInTaskbar = $false
$form.Opacity = 0
$form.Show()
$form.Activate()
$dialog = New-Object System.Windows.Forms.FolderBrowserDialog
$dialog.Description = '${safeDescription}'
$dialog.ShowNewFolderButton = $false
$dialog.SelectedPath = '${safeInitial}'
if ($dialog.ShowDialog($form) -eq [System.Windows.Forms.DialogResult]::OK) {
  [Console]::WriteLine($dialog.SelectedPath)
}
$form.Close()
`;
    execFile('powershell.exe', ['-NoProfile', '-STA', '-ExecutionPolicy', 'Bypass', '-Command', script], {
      windowsHide: false,
      timeout: 120000,
      maxBuffer: 1024 * 1024
    }, (error, stdout) => {
      if (error) {
        if (error.killed || error.signal === 'SIGTERM') return reject(new Error('Selecao de pasta expirou. Digite o caminho manualmente ou tente novamente.'));
        return reject(error);
      }
      const selected = asTrimmedString(stdout).split(/\r?\n/).filter(Boolean).pop() || '';
      resolve(selected);
    });
  });
}

function openGameFolderDialog(defaultPath) {
  return openFolderDialog(defaultPath, 'Selecione a pasta do Dev onde o jogo esta sendo modificado');
}

function readGameVersionData() {
  if (!fs.existsSync(GAME_VERSION_FILE)) {
    return { version: '0.0.0', manifest_url: '', base_url: '', required: true, notes: '', update_mode: 'manifest' };
  }
  try {
    const data = JSON.parse(fs.readFileSync(GAME_VERSION_FILE, 'utf8'));
    return {
      version: String(data.version || '0.0.0'),
      manifest_url: String(data.manifest_url || ''),
      base_url: String(data.base_url || ''),
      required: data.required !== false,
      notes: String(data.notes || ''),
      update_mode: 'manifest',
      file_count: Number(data.file_count || 0),
      total_size: Number(data.total_size || 0),
      updatedAt: data.updatedAt || null
    };
  } catch {
    return { version: '0.0.0', manifest_url: '', base_url: '', required: true, notes: '', update_mode: 'manifest' };
  }
}

function readGameUpdateHistory() {
  if (!fs.existsSync(GAME_UPDATE_HISTORY_FILE)) return [];
  try {
    const data = JSON.parse(fs.readFileSync(GAME_UPDATE_HISTORY_FILE, 'utf8').replace(/^\uFEFF/, ''));
    if (Array.isArray(data)) return data;
    if (Array.isArray(data.updates)) return data.updates;
  } catch {}
  return [];
}

function writeGameUpdateHistory(updates) {
  const list = Array.isArray(updates) ? updates : [];
  fs.writeFileSync(GAME_UPDATE_HISTORY_FILE, JSON.stringify({ updates: list }, null, 2), 'utf8');
  return list;
}

function appendGameUpdateHistory(entry) {
  const current = readGameUpdateHistory();
  const next = [
    entry,
    ...current.filter(item => item && item.version !== entry.version)
  ].slice(0, 100);
  return writeGameUpdateHistory(next);
}

function fileSha256(filePath) {
  const hash = crypto.createHash('sha256');
  hash.update(fs.readFileSync(filePath));
  return hash.digest('hex');
}

function fileSha256Stream(filePath) {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash('sha256');
    const stream = fs.createReadStream(filePath);
    stream.on('data', chunk => hash.update(chunk));
    stream.on('error', reject);
    stream.on('end', () => resolve(hash.digest('hex')));
  });
}

function scanLauncherPublishFiles(sourceDir = null) {
  const root = sourceDir ? path.resolve(sourceDir) : readLauncherSourceDir();
  const launcherDataFile = fs.existsSync(path.join(root, 'launcher-config.json'))
    ? path.join(root, 'launcher-config.json')
    : fs.existsSync(path.join(root, 'public', 'launcher-config.json'))
      ? path.join(root, 'public', 'launcher-config.json')
      : LAUNCHER_DATA_FILE;
  const launcherImagesDir = fs.existsSync(path.join(root, 'launcher-images'))
    ? path.join(root, 'launcher-images')
    : fs.existsSync(path.join(root, 'public', 'launcher-images'))
      ? path.join(root, 'public', 'launcher-images')
      : LAUNCHER_IMAGES_DIR;
  const buildDirCandidates = [
    path.join(root, 'dist'),
    path.join(root, 'release'),
    path.join(root, 'build'),
    path.join(root, 'out')
  ];
  const buildDir = buildDirCandidates.find(dir => fs.existsSync(dir) && fs.statSync(dir).isDirectory()) || null;

  const files = [];
  const addTreeFiles = (baseDir, relPrefix = '') => {
    const walk = (dir) => {
      for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          const lowerName = String(entry.name || '').toLowerCase();
          if (['node_modules', '.git', '.cache'].includes(lowerName)) continue;
          walk(full);
          continue;
        }
        const rel = path.relative(baseDir, full).replace(/\\/g, '/');
        const lower = rel.toLowerCase();
        if (!lower || lower.endsWith('.tmp') || lower.endsWith('.bak') || lower.endsWith('.download') || lower.endsWith('.map')) continue;
        if (lower === 'warfacelauncher.exe' && fs.existsSync(path.join(baseDir, 'WarfaceSurvivorSetup.exe'))) continue;
        files.push({ rel: relPrefix ? `${relPrefix}/${rel}` : rel, fullPath: full });
      }
    };
    if (fs.existsSync(baseDir) && fs.statSync(baseDir).isDirectory()) walk(baseDir);
  };

  if (buildDir) addTreeFiles(buildDir, '');
  if (fs.existsSync(launcherDataFile) && fs.statSync(launcherDataFile).isFile()) {
    files.push({ rel: 'launcher-config.json', fullPath: launcherDataFile });
  }
  if (fs.existsSync(launcherImagesDir) && fs.statSync(launcherImagesDir).isDirectory()) {
    const entries = fs.readdirSync(launcherImagesDir, { withFileTypes: true });
    for (const entry of entries) {
      if (!entry.isFile()) continue;
      const name = String(entry.name || '');
      const lower = name.toLowerCase();
      if (!lower || lower.endsWith('.tmp') || lower.endsWith('.bak') || lower.endsWith('.download')) continue;
      files.push({ rel: `launcher-images/${name}`, fullPath: path.join(launcherImagesDir, name) });
    }
  }
  files.sort((a, b) => a.rel.localeCompare(b.rel));
  const unique = [];
  const seen = new Set();
  for (const file of files) {
    const key = String(file.rel || '').toLowerCase();
    if (!key || seen.has(key)) continue;
    seen.add(key);
    unique.push(file);
  }
  return unique;
}

function runLauncherBuildIfNeeded(sourceDir) {
  const projectDir = sourceDir ? path.resolve(sourceDir) : readLauncherSourceDir();
  const pkgFile = path.join(projectDir, 'package.json');
  if (!fs.existsSync(pkgFile) || !fs.statSync(pkgFile).isFile()) {
    return { built: false, skipped: true, reason: 'package.json nao encontrado' };
  }
  const buildEnv = Object.fromEntries(Object.entries(process.env).filter(([key]) => key && !key.startsWith('=')));
  buildEnv.CSC_IDENTITY_AUTO_DISCOVERY = 'false';
  return new Promise((resolve, reject) => {
    const child = process.platform === 'win32'
      ? spawn('cmd.exe', ['/d', '/s', '/c', 'npm run build -- --config.directories.output=dist'], {
        cwd: projectDir,
        env: buildEnv,
        shell: false,
        windowsHide: true
      })
      : spawn('npm', ['run', 'build', '--', '--config.directories.output=dist'], {
      cwd: projectDir,
      env: buildEnv,
      shell: false,
      windowsHide: true
    });
    let output = '';
    child.stdout.on('data', chunk => { output += String(chunk || ''); });
    child.stderr.on('data', chunk => { output += String(chunk || ''); });
    child.on('error', reject);
    child.on('close', code => {
      if (code === 0) return resolve({ built: true, skipped: false, code, output: output.slice(-4000) });
      reject(new Error(`Build do launcher falhou (exit ${code}): ${output.slice(-1500)}`));
    });
  });
}

async function scanLauncherManifestFiles(onProgress, sourceDir = null) {
  const paths = scanLauncherPublishFiles(sourceDir);
  if (typeof onProgress === 'function') onProgress({ phase: 'hash', total_files: paths.length, hashed_files: 0 });
  const files = [];
  for (let i = 0; i < paths.length; i++) {
    const item = paths[i];
    const stat = fs.statSync(item.fullPath);
    files.push({
      path: item.rel,
      size: stat.size,
      hash: await fileSha256Stream(item.fullPath),
      mtime: stat.mtime.toISOString(),
      fullPath: item.fullPath
    });
    if (typeof onProgress === 'function' && ((i + 1) % 5 === 0 || i + 1 === paths.length)) {
      onProgress({ phase: 'hash', total_files: paths.length, hashed_files: i + 1, current_file: item.rel });
    }
  }
  return files;
}

function readLauncherVersionData() {
  if (!fs.existsSync(LAUNCHER_VERSION_FILE)) {
    return { version: '0.0.0', manifest_url: '', base_url: '', required: true, notes: '', update_mode: 'manifest' };
  }
  try {
    const data = JSON.parse(fs.readFileSync(LAUNCHER_VERSION_FILE, 'utf8'));
    return {
      version: String(data.version || '0.0.0'),
      manifest_url: String(data.manifest_url || ''),
      base_url: String(data.base_url || ''),
      required: data.required !== false,
      notes: String(data.notes || ''),
      update_mode: 'manifest',
      file_count: Number(data.file_count || 0),
      total_size: Number(data.total_size || 0),
      updatedAt: data.updatedAt || null
    };
  } catch {
    return { version: '0.0.0', manifest_url: '', base_url: '', required: true, notes: '', update_mode: 'manifest' };
  }
}

function readLauncherManifest(req) {
  const current = readLauncherVersionData();
  const fallbackBase = normalizeBaseUrl(readR2Config().publicBaseUrl) || getBaseUrl(req);
  if (fs.existsSync(LAUNCHER_MANIFEST_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(LAUNCHER_MANIFEST_FILE, 'utf8').replace(/^\uFEFF/, ''));
      return {
        ...data,
        version: String(data.version || current.version || '0.0.0'),
        base_url: normalizeBaseUrl(data.base_url) || fallbackBase,
        required: data.required !== false,
        files: Array.isArray(data.files) ? data.files : []
      };
    } catch {}
  }
  return {
    version: current.version || '0.0.0',
    base_url: fallbackBase,
    required: true,
    notes: current.notes || '',
    generated_at: null,
    file_count: 0,
    total_size: 0,
    files: []
  };
}

function writeLauncherManifest(req, options = {}) {
  const current = readLauncherVersionData();
  const previous = readLauncherManifest(req);
  const files = Array.isArray(options.files) ? options.files : [];
  const hasChangedFiles = Object.prototype.hasOwnProperty.call(options, 'changed_files') || Object.prototype.hasOwnProperty.call(options, 'changedFiles');
  const hasRemovedFiles = Object.prototype.hasOwnProperty.call(options, 'removed_files') || Object.prototype.hasOwnProperty.call(options, 'removedFiles');
  const changedFiles = normalizeManifestDeltaFiles(options.changed_files || options.changedFiles || [], files);
  const removedFiles = normalizeManifestDeltaFiles(options.removed_files || options.removedFiles || [], previous.files || []);
  const fallbackBase = normalizeBaseUrl(readR2Config().publicBaseUrl) || getBaseUrl(req);
  const manifest = {
    version: String(options.version || current.version || '0.0.0'),
    previous_version: String(options.previous_version || previous.version || current.version || '0.0.0'),
    update_mode: 'manifest',
    required: true,
    base_url: normalizeBaseUrl(options.base_url) || normalizeBaseUrl(previous.base_url) || fallbackBase,
    notes: asTrimmedString(options.notes),
    generated_at: new Date().toISOString(),
    file_count: files.length,
    total_size: files.reduce((sum, file) => sum + Number(file.size || 0), 0),
    files
  };
  if (hasChangedFiles) {
    manifest.changed_files = changedFiles;
    manifest.changed_count = changedFiles.length;
  }
  if (hasRemovedFiles) {
    manifest.removed_files = removedFiles;
    manifest.removed_count = removedFiles.length;
  }
  fs.writeFileSync(LAUNCHER_MANIFEST_FILE, JSON.stringify(manifest, null, 2), 'utf8');
  return manifest;
}

function scanLauncherManifestFilesSync() {
  const files = [];
  for (const item of scanLauncherPublishFiles()) {
    const stat = fs.statSync(item.fullPath);
    files.push({
      path: item.rel,
      size: stat.size,
      hash: fileSha256(item.fullPath),
      mtime: stat.mtime.toISOString()
    });
  }
  files.sort((a, b) => a.path.localeCompare(b.path));
  return files;
}

function readLauncherUpdateHistory() {
  if (!fs.existsSync(LAUNCHER_UPDATE_HISTORY_FILE)) return [];
  try {
    const data = JSON.parse(fs.readFileSync(LAUNCHER_UPDATE_HISTORY_FILE, 'utf8').replace(/^\uFEFF/, ''));
    if (Array.isArray(data)) return data;
    if (Array.isArray(data.updates)) return data.updates;
  } catch {}
  return [];
}

function appendLauncherUpdateHistory(entry) {
  const current = readLauncherUpdateHistory();
  const next = [
    entry,
    ...current.filter(item => item && item.version !== entry.version)
  ].slice(0, 100);
  fs.writeFileSync(LAUNCHER_UPDATE_HISTORY_FILE, JSON.stringify({ updates: next }, null, 2), 'utf8');
  return next;
}

function shouldSkipGamePublishPath(relPath) {
  const rel = String(relPath || '').replace(/\\/g, '/').toLowerCase();
  const parts = rel.split('/').filter(Boolean);
  const firstDir = parts[0] || '';
  const rootName = rel.split('/').pop();
  if (!rel) return true;
  if (['masterserver', 'mongodb', 'nodejs', 'xmppservertcp', 'componentconference', 'componentwfc', 'tools', 'cache', 'logs', 'warface-launcher', 'launcher-images'].includes(firstDir)) return true;
  if (rel.startsWith('bin64/editor/') || rel.startsWith('bin64/editors/')) return true;
  if (rel.includes('/dedicated/')) return true;
  if (rel.includes('/server/')) return true;
  if (rel.includes('/developer/')) return true;
  if (rel.includes('/dev/')) return true;
  if (rel.includes('/tests/')) return true;
  if (rel.includes('/test/')) return true;
  if (rel.startsWith('logbackups/')) return true;
  if (rel.startsWith('logstatoscope/')) return true;
  if (rel.startsWith('logstatoscopededicated/')) return true;
  if (rel.endsWith('.log')) return true;
  if (rel === 'game-manifest.json' || rel === 'game-version.json' || rel === 'game-update-history.json') return true;
  if (rel === 'launcher-manifest.json' || rel === 'launcher-version.json' || rel === 'launcher-update-history.json' || rel === 'launcher-config.json') return true;
  if (rel === '.wf_dev_publish_state.json') return true;
  if (rel.endsWith('.download') || rel.endsWith('.tmp')) return true;
  if (rel.endsWith('.bak') || rootName.endsWith('.bak') || rootName.includes('.bak-')) return true;
  if (rootName.endsWith('.old') || rootName.endsWith('.orig') || rootName.endsWith('.rej')) return true;
  if (['.pdb', '.ilk', '.iobj', '.ipdb', '.exp', '.lib', '.idb', '.obj', '.sln', '.vcxproj', '.filters', '.user', '.tlog'].some(ext => rel.endsWith(ext))) return true;
  if (['.bat', '.cmd', '.ps1', '.sh', '.py'].some(ext => rel.endsWith(ext))) return true;
  if (rel === 'game.zip' || rel.endsWith('/game.zip')) return true;
  if ([
    'editor.exe',
    'luacompiler.exe',
    'testbot.dll',
    'enableaidevmode.reg',
    'server_profile.txt',
    'dedicatedserver.exe',
    'dedicatedserverstartpve.bat',
    'dedicatedserverstartpve.cmd',
    'dedicatedserverstartpvp.bat',
    'dedicatedserverstartpvp.cmd',
    'dedicatedserversstartpve.bat',
    'dedicatedserversstartpve.cmd',
    'dedicatedserversstartpvp.bat',
    'dedicatedserversstartpvp.cmd',
    'dedicatedstartermany.exe'
  ].includes(rootName)) return true;
  return false;
}

function scanManifestFiles(rootDir) {
  const files = [];
  const walk = (dir) => {
    if (!fs.existsSync(dir)) return;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(fullPath);
        continue;
      }
      const rel = path.relative(rootDir, fullPath).replace(/\\/g, '/');
      if (shouldSkipGamePublishPath(rel)) continue;
      const stat = fs.statSync(fullPath);
      files.push({
        path: rel,
        size: stat.size,
        hash: fileSha256(fullPath),
        mtime: stat.mtime.toISOString()
      });
    }
  };
  walk(rootDir);
  files.sort((a, b) => a.path.localeCompare(b.path));
  return files;
}

async function scanManifestFilesStream(rootDir, onProgress) {
  const paths = [];
  const walk = (dir) => {
    if (!fs.existsSync(dir)) return;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(fullPath);
        continue;
      }
      const rel = path.relative(rootDir, fullPath).replace(/\\/g, '/');
      if (shouldSkipGamePublishPath(rel)) continue;
      paths.push({ rel, fullPath });
      if (typeof onProgress === 'function' && paths.length % 100 === 0) {
        onProgress({ phase: 'scan', scanned_files: paths.length });
      }
    }
  };
  if (typeof onProgress === 'function') onProgress({ phase: 'scan', scanned_files: 0 });
  walk(rootDir);
  paths.sort((a, b) => a.rel.localeCompare(b.rel));
  if (typeof onProgress === 'function') onProgress({ phase: 'hash', total_files: paths.length, hashed_files: 0 });

  const files = [];
  for (let i = 0; i < paths.length; i++) {
    const item = paths[i];
    const stat = fs.statSync(item.fullPath);
    files.push({
      path: item.rel,
      size: stat.size,
      hash: await fileSha256Stream(item.fullPath),
      mtime: stat.mtime.toISOString()
    });
    if (typeof onProgress === 'function' && ((i + 1) % 5 === 0 || i + 1 === paths.length)) {
      onProgress({ phase: 'hash', total_files: paths.length, hashed_files: i + 1, current_file: item.rel });
    }
  }
  return files;
}

function manifestFileMap(manifest) {
  const map = new Map();
  for (const file of Array.isArray(manifest && manifest.files) ? manifest.files : []) {
    const rel = normalizePatchEntryPath(file.path);
    if (rel) map.set(rel.toLowerCase(), file);
  }
  return map;
}

function getManifestDiff(previousManifest, nextFiles) {
  const oldMap = manifestFileMap(previousManifest);
  const nextMap = new Map();
  const changed = [];
  for (const file of nextFiles) {
    const rel = normalizePatchEntryPath(file.path);
    if (!rel) continue;
    nextMap.set(rel.toLowerCase(), file);
    const old = oldMap.get(rel.toLowerCase());
    if (!old || String(old.hash || '').toLowerCase() !== String(file.hash || '').toLowerCase() || Number(old.size || 0) !== Number(file.size || 0)) {
      changed.push({ ...file, path: rel });
    }
  }
  const removed = [];
  for (const [key, old] of oldMap.entries()) {
    if (!nextMap.has(key)) removed.push({ ...old, path: normalizePatchEntryPath(old.path) });
  }
  return { changed, removed };
}

function normalizeManifestDeltaFiles(files, allFiles = []) {
  if (!Array.isArray(files)) return [];
  const fullMap = new Map();
  for (const file of Array.isArray(allFiles) ? allFiles : []) {
    const rel = normalizePatchEntryPath(file && file.path);
    if (rel) fullMap.set(rel.toLowerCase(), { ...file, path: rel });
  }

  const result = [];
  const seen = new Set();
  for (const entry of files) {
    const rawPath = typeof entry === 'string' ? entry : entry && entry.path;
    const rel = normalizePatchEntryPath(rawPath);
    if (!rel) continue;
    const key = rel.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    result.push({ ...(fullMap.get(key) || {}), ...(typeof entry === 'object' && entry ? entry : {}), path: rel });
  }
  return result;
}

function readGameManifest(req) {
  const current = readGameVersionData();
  if (fs.existsSync(GAME_MANIFEST_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(GAME_MANIFEST_FILE, 'utf8').replace(/^\uFEFF/, ''));
      return {
        ...data,
        version: String(data.version || current.version || '0.0.0'),
        base_url: normalizeBaseUrl(data.base_url) || getPublicGameCdnBase(req),
        required: data.required !== false,
        files: Array.isArray(data.files) ? data.files : []
      };
    } catch {}
  }
  return {
    version: current.version || '0.0.0',
    base_url: getPublicGameCdnBase(req),
    required: true,
    notes: current.notes || '',
    generated_at: null,
    file_count: 0,
    total_size: 0,
    files: []
  };
}

function writeGameManifest(req, options = {}) {
  const current = readGameVersionData();
  const previous = readGameManifest(req);
  const files = Array.isArray(options.files) ? options.files : scanManifestFiles(GAME_CDN_DIR);
  const hasChangedFiles = Object.prototype.hasOwnProperty.call(options, 'changed_files') || Object.prototype.hasOwnProperty.call(options, 'changedFiles');
  const hasRemovedFiles = Object.prototype.hasOwnProperty.call(options, 'removed_files') || Object.prototype.hasOwnProperty.call(options, 'removedFiles');
  const changedFiles = normalizeManifestDeltaFiles(options.changed_files || options.changedFiles || [], files);
  const removedFiles = normalizeManifestDeltaFiles(options.removed_files || options.removedFiles || [], previous.files || []);
  const manifest = {
    version: String(options.version || current.version || '0.0.0'),
    previous_version: String(options.previous_version || previous.version || current.version || '0.0.0'),
    update_mode: 'manifest',
    required: true,
    base_url: normalizeBaseUrl(options.base_url) || normalizeBaseUrl(previous.base_url) || getPublicGameCdnBase(req),
    notes: asTrimmedString(options.notes),
    generated_at: new Date().toISOString(),
    file_count: files.length,
    total_size: files.reduce((sum, file) => sum + Number(file.size || 0), 0),
    files
  };
  if (hasChangedFiles) {
    manifest.changed_files = changedFiles;
    manifest.changed_count = changedFiles.length;
  }
  if (hasRemovedFiles) {
    manifest.removed_files = removedFiles;
    manifest.removed_count = removedFiles.length;
  }
  fs.writeFileSync(GAME_MANIFEST_FILE, JSON.stringify(manifest, null, 2), 'utf8');
  return manifest;
}

function writePublishedGameFile(relPath, buffer) {
  const rel = normalizePatchEntryPath(relPath);
  if (!rel) throw new Error(`Caminho de arquivo invalido: ${relPath || '(sem nome)'}`);
  if (!buffer || !buffer.length) throw new Error(`Arquivo vazio ou invalido: ${rel}`);

  for (const root of [GAME_CDN_DIR, GAME_REF_DIR]) {
    const target = path.resolve(root, ...rel.split('/'));
    if (!target.startsWith(root + path.sep)) throw new Error(`Caminho fora do jogo: ${rel}`);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, buffer);
  }
  return rel;
}

function copyPublishedGameFile(relPath, sourceFile) {
  const rel = normalizePatchEntryPath(relPath);
  if (!rel) throw new Error(`Caminho de arquivo invalido: ${relPath || '(sem nome)'}`);
  if (!fs.existsSync(sourceFile)) throw new Error(`Arquivo fonte nao encontrado: ${sourceFile}`);

  for (const root of [GAME_CDN_DIR, GAME_REF_DIR]) {
    const target = path.resolve(root, ...rel.split('/'));
    if (!target.startsWith(root + path.sep)) throw new Error(`Caminho fora do jogo: ${rel}`);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.copyFileSync(sourceFile, target);
  }
  return rel;
}

function removePublishedGameFile(relPath) {
  const rel = normalizePatchEntryPath(relPath);
  if (!rel) return;
  for (const root of [GAME_CDN_DIR, GAME_REF_DIR]) {
    const target = path.resolve(root, ...rel.split('/'));
    if (!target.startsWith(root + path.sep)) continue;
    try { if (fs.existsSync(target)) fs.unlinkSync(target); } catch {}
  }
}

function normalizePatchEntryPath(value) {
  const raw = asTrimmedString(value).replace(/\\/g, '/').replace(/^[A-Za-z]:\//, '').replace(/^\/+/, '');
  if (!raw) return '';
  const normalized = path.posix.normalize(raw);
  if (!normalized || normalized === '.' || normalized.startsWith('../') || normalized.includes('/../')) return '';
  if (/[<>:"|?*\x00-\x1F]/.test(normalized)) return '';
  return normalized;
}

function asTrimmedString(value) {
  if (typeof value === 'string') return value.trim();
  if (value === undefined || value === null) return '';
  return String(value).trim();
}

function parseStrictInt(value) {
  if (typeof value === 'number') {
    if (!Number.isFinite(value) || !Number.isInteger(value) || !Number.isSafeInteger(value)) return null;
    return value;
  }
  if (typeof value !== 'string') return null;
  const s = value.trim();
  if (!/^-?\d+$/.test(s)) return null;
  const n = Number(s);
  if (!Number.isSafeInteger(n)) return null;
  return n;
}

function validateNickInput(raw, label = 'Nick') {
  const nick = asTrimmedString(raw);
  if (!nick) return { ok: false, error: `${label} obrigatorio` };
  if (nick.length < PANEL_MODEL.nick.minLen || nick.length > PANEL_MODEL.nick.maxLen) {
    return { ok: false, error: `${label} deve ter entre ${PANEL_MODEL.nick.minLen} e ${PANEL_MODEL.nick.maxLen} caracteres` };
  }
  if (/[\x00-\x1F\x7F]/.test(nick)) return { ok: false, error: `${label} contem caracteres invalidos` };
  if (/[<>"'`\\]/.test(nick)) return { ok: false, error: `${label} contem caracteres bloqueados` };
  return { ok: true, value: nick };
}

function validateCommandName(raw) {
  const cmd = asTrimmedString(raw).toLowerCase();
  if (!PANEL_MODEL.command.allowed.includes(cmd)) return { ok: false, error: `Comando desconhecido: ${raw}` };
  return { ok: true, value: cmd };
}

function validateBoundedInt(rawValue, bounds, label, options = {}) {
  const { required = true } = options;
  const raw = rawValue === undefined || rawValue === null ? '' : String(rawValue).trim();
  if (!raw) {
    if (!required) return { ok: true, value: null };
    return { ok: false, error: `${label} obrigatorio` };
  }
  const n = parseStrictInt(raw);
  if (n === null) return { ok: false, error: `${label} invalido` };
  if (n < bounds.min || n > bounds.max) {
    return { ok: false, error: `${label} deve estar entre ${bounds.min.toLocaleString('pt-BR')} e ${bounds.max.toLocaleString('pt-BR')}` };
  }
  return { ok: true, value: n };
}

function validatePositiveDelta(rawValue, bounds, label) {
  const parsed = validateBoundedInt(rawValue, { min: 1, max: bounds.perCmd }, label);
  if (!parsed.ok) return parsed;
  return { ok: true, value: parsed.value };
}

function getProfileInt(profile, fieldName, defaultValue = 0) {
  const raw = profile ? profile[fieldName] : undefined;
  if (raw === undefined || raw === null || raw === '') return { ok: true, value: defaultValue };
  const parsed = parseStrictInt(raw);
  if (parsed === null) return { ok: false, error: `Campo do perfil invalido: ${fieldName}` };
  return { ok: true, value: parsed };
}

function normalizeItemName(raw) {
  return asTrimmedString(raw).toLowerCase();
}

function validateItemNameInput(raw) {
  const itemName = normalizeItemName(raw);
  if (!itemName) return { ok: false, error: 'Nome do item obrigatorio' };
  if (itemName.length < PANEL_MODEL.item.minLen || itemName.length > PANEL_MODEL.item.maxLen) {
    return { ok: false, error: `Nome do item deve ter entre ${PANEL_MODEL.item.minLen} e ${PANEL_MODEL.item.maxLen} caracteres` };
  }
  if (!PANEL_MODEL.item.pattern.test(itemName)) return { ok: false, error: 'Nome do item contem caracteres invalidos' };
  const names = loadItemNames();
  const base = itemName.replace(ITEM_VARIANT_SUFFIX_RE, '');
  const localImage = getLocalWeaponImagePath(base);
  const hasLocalRealImage = localImage && !String(localImage).endsWith('/_default.png');
  if (!names[itemName] && !names[base] && !hasLocalRealImage) {
    return { ok: false, error: `Item inexistente: ${itemName}` };
  }
  return { ok: true, value: itemName, baseKey: base, displayName: names[itemName] || names[base] || base };
}

function validateFreeItemToken(raw) {
  const itemName = normalizeItemName(raw);
  if (!itemName) return { ok: false, error: 'Nome do item obrigatorio' };
  if (itemName.length < PANEL_MODEL.item.minLen || itemName.length > PANEL_MODEL.item.maxLen) {
    return { ok: false, error: `Nome do item deve ter entre ${PANEL_MODEL.item.minLen} e ${PANEL_MODEL.item.maxLen} caracteres` };
  }
  if (!PANEL_MODEL.item.pattern.test(itemName)) return { ok: false, error: 'Nome do item contem caracteres invalidos' };
  return { ok: true, value: itemName };
}

function validateAchievementId(raw) {
  const achievementId = asTrimmedString(raw);
  if (!achievementId) return { ok: false, error: 'achievement_id obrigatorio' };
  if (achievementId.length < PANEL_MODEL.achievement.minIdLen || achievementId.length > PANEL_MODEL.achievement.maxIdLen) {
    return { ok: false, error: `achievement_id deve ter entre ${PANEL_MODEL.achievement.minIdLen} e ${PANEL_MODEL.achievement.maxIdLen} caracteres` };
  }
  if (!PANEL_MODEL.achievement.idPattern.test(achievementId)) return { ok: false, error: 'achievement_id invalido' };
  return { ok: true, value: achievementId };
}

// ─── Helper: HTTP GET with timeout and error handling ──────────────────────
function apiGet(url) {
  return new Promise(r => {
    try {
      const req = http.get(url, res => {
        let b = '';
        res.on('data', c => b += c);
        res.on('end', () => {
          // Log response status
          const status = res.statusCode || '?';
          log('API', `${url} -> ${status} (${b.length} bytes)`);
          r(b);
        });
      });
      req.on('error', (e) => {
        log('API', `${url} -> error: ${e.message}`);
        req.destroy();
        r(null);
      });
      req.setTimeout(3000, () => {
        log('API', `${url} -> timeout`);
        req.destroy();
        r(null);
      });
    } catch (err) {
      log('API', `${url} -> exception: ${err.message}`);
      r(null);
    }
  });
}

// ─── Weapon Name Map ──────────────────────────────────────────────────────
let itemNameMap = null;
function normalizeItemKeyToken(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function cleanItemDisplayName(value) {
  return String(value || '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function addGameItemAlias(set, rawValue) {
  const key = normalizeItemKeyToken(rawValue);
  if (!key || key.length < 2) return;
  set.add(key);
  const base = key.replace(ITEM_VARIANT_SUFFIX_RE, '');
  if (base && base.length >= 2) {
    set.add(base);
    set.add(`${base}_shop`);
  }
  if (!ITEM_VARIANT_SUFFIX_RE.test(key)) {
    set.add(`${key}_shop`);
  }
}

function loadGameItemInventory() {
  try {
    if (!fs.existsSync(ASSET_INVENTORY_ITEMS_FILE)) {
      return { available: false, items: [], validNames: new Set(), byAlias: new Map(), reason: 'AssetInventory/manifests/items.json nao encontrado' };
    }

    const stat = fs.statSync(ASSET_INVENTORY_ITEMS_FILE);
    if (gameItemInventoryCache && gameItemInventoryMtimeMs === stat.mtimeMs) {
      return gameItemInventoryCache;
    }

    const raw = JSON.parse(fs.readFileSync(ASSET_INVENTORY_ITEMS_FILE, 'utf8'));
    const sourceItems = Array.isArray(raw) ? raw : [];
    const items = [];
    const validNames = new Set();
    const byAlias = new Map();

    for (const row of sourceItems) {
      if (!row || row.isDefinition !== true) continue;
      const id = normalizeItemKeyToken(row.id);
      if (!id) continue;
      const item = Object.assign({}, row, { id });
      items.push(item);

      const aliases = new Set();
      addGameItemAlias(aliases, id);
      addGameItemAlias(aliases, row.icon);
      for (const alias of aliases) {
        validNames.add(alias);
        if (!byAlias.has(alias)) byAlias.set(alias, item);
      }
    }

    gameItemInventoryCache = {
      available: true,
      items,
      validNames,
      byAlias,
      loadedAt: Date.now(),
      mtimeMs: stat.mtimeMs
    };
    gameItemInventoryMtimeMs = stat.mtimeMs;
    log('ITEMS', `loaded ${items.length} game item definitions from AssetInventory`);
    return gameItemInventoryCache;
  } catch (e) {
    log('ITEMS', `game inventory load error: ${e.message}`);
    return { available: false, items: [], validNames: new Set(), byAlias: new Map(), reason: e.message };
  }
}

function findGameItemForOfferName(rawName) {
  const inventory = loadGameItemInventory();
  const key = normalizeItemKeyToken(rawName);
  if (!inventory.available || !key) return { ok: !inventory.available, inventory, key, item: null };
  const base = key.replace(ITEM_VARIANT_SUFFIX_RE, '');
  const item = inventory.byAlias.get(key) || inventory.byAlias.get(base) || inventory.byAlias.get(`${base}_shop`);
  return { ok: !!item, inventory, key, item: item || null };
}

function filterShopOffersToGameItems(rawOffers) {
  const inventory = loadGameItemInventory();
  const offers = Array.isArray(rawOffers) ? rawOffers : [];
  if (!inventory.available) {
    return { offers: offers.slice(), removed: [], inventoryAvailable: false, reason: inventory.reason || '' };
  }

  const kept = [];
  const removed = [];
  for (const offer of offers) {
    const match = findGameItemForOfferName(offer && offer.name);
    if (match.ok) {
      kept.push(offer);
    } else {
      removed.push({
        id: offer && offer.id,
        name: offer && offer.name,
        reason: 'item_not_found_in_GameData'
      });
    }
  }

  return { offers: kept, removed, inventoryAvailable: true };
}

function inventoryImagePathToPublicUrl(rawPath) {
  const rel = String(rawPath || '').replace(/\\/g, '/');
  const marker = 'AdminPanel/public/';
  const idx = rel.indexOf(marker);
  if (idx >= 0) return `/${rel.slice(idx + marker.length)}`;
  if (rel.startsWith('img/')) return `/${rel}`;
  return '';
}

function inferArsenalTypeFromInventoryItem(item) {
  const category = normalizeItemKeyToken(item && item.category);
  if (category === 'weapons' || category === 'ammo') return 'weapon';
  if (category === 'armor' || category === 'accessories' || category === 'skins' || category === 'characterparts') return 'equipment';
  return 'other';
}

function buildItemKeyAliasesFromLocKey(localizationKeyNoLabel) {
  const out = new Set();
  const add = raw => {
    const norm = normalizeItemKeyToken(raw);
    if (!norm || norm.length < 2) return;
    out.add(norm);
  };

  const key = normalizeItemKeyToken(localizationKeyNoLabel);
  if (!key) return [];

  add(key);
  add(key.replace(ITEM_VARIANT_SUFFIX_RE, ''));

  const stripped = key
    .replace(/^ui_(weapon|weapons|armor|item)_/, '')
    .replace(/^item_/, '');
  if (stripped !== key) {
    add(stripped);
    add(stripped.replace(ITEM_VARIANT_SUFFIX_RE, ''));
  }

  const armorClassStripped = key.replace(/^ui_armor_(shared|soldier|medic|engineer|sniper|gunner)_/, '$1_');
  if (armorClassStripped !== key) {
    add(armorClassStripped);
    add(armorClassStripped.replace(ITEM_VARIANT_SUFFIX_RE, ''));
  }

  const compact = key.replace(/^ui_/, '');
  if (compact !== key) {
    add(compact);
    add(compact.replace(ITEM_VARIANT_SUFFIX_RE, ''));
  }

  return Array.from(out);
}

function loadItemNames() {
  if (itemNameMap) return itemNameMap;
  const langsDir = path.join(__dirname, 'languages');
  const map = {};
  const scoreMap = {};
  const addName = (rawKey, displayNameRaw, quality = 15) => {
    const key = normalizeItemKeyToken(rawKey);
    const cleaned = cleanItemDisplayName(displayNameRaw);
    if (!key || !cleaned || cleaned.length > 200) return;
    if ((scoreMap[key] || 0) >= quality) return;
    map[key] = cleaned;
    scoreMap[key] = quality;
  };

  const inventory = loadGameItemInventory();
  if (inventory.available) {
    for (const item of inventory.items) {
      const displayName = item.displayName || item.id;
      addName(item.id, displayName, 18);
      const base = normalizeItemKeyToken(item.id).replace(ITEM_VARIANT_SUFFIX_RE, '');
      addName(base, displayName, 17);
      addName(`${base}_shop`, displayName, 16);
    }
  }

  const re = /<entry\s+key="([^"]+)">[\s\S]*?<original\s+value="([^"]*)"\s*\/>/g;
  const decode = s => s.replace(/&quot;/g, '"').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
  try {
    const files = fs.readdirSync(langsDir).filter(f => f.startsWith('text_') && f.endsWith('.xml'));
    for (const file of files) {
      const xml = fs.readFileSync(path.join(langsDir, file), 'utf8');
      let m;
      while ((m = re.exec(xml)) !== null) {
        const key = String(m[1] || '');
        if (!/_((name)|(description))$/i.test(key)) continue;
        const keyNoLabel = key.replace(/_(name|description)$/i, '');
        const aliases = buildItemKeyAliasesFromLocKey(keyNoLabel);
        if (!aliases.length) continue;

        const cleaned = cleanItemDisplayName(decode(m[2]));
        if (!cleaned || cleaned.length > 200) continue;
        if (cleaned.includes('<') || cleaned.includes('>')) continue;

        const quality = /_name$/i.test(key) ? 20 : 10;
        for (const alias of aliases) {
          addName(alias, cleaned, quality);
        }
      }
    }
    log('ITEMS', `loaded ${Object.keys(map).length} item names from ${files.length} files`);
    itemNameMap = map;
    return map;
  } catch (e) {
    log('ITEMS', `error: ${e.message}`);
    log('ITEMS', `using ${Object.keys(map).length} item names from AssetInventory`);
    itemNameMap = map;
    return itemNameMap;
  }
}

const ARSENAL_WEAPON_PREFIXES = new Set([
  'ar', 'sr', 'smg', 'shg', 'pt', 'kn', 'mg', 'lmg', 'rl', 'sg', 'sn'
]);
const ARSENAL_EQUIPMENT_TOKENS = [
  'helmet', 'vest', 'gloves', 'boots', 'shoes', 'hands', 'head', 'body',
  'armor', 'bag', 'backpack', 'parachute', 'shared_', 'soldier_', 'medic_',
  'engineer_', 'sniper_', 'gunner_', 'skin', 'camo', 'uniform'
];

function inferArsenalType(itemKey) {
  const base = normalizeItemKeyToken(itemKey);
  const prefix = base.split('_')[0] || '';
  if (ARSENAL_WEAPON_PREFIXES.has(prefix)) return 'weapon';
  if (ARSENAL_EQUIPMENT_TOKENS.some(token => base.includes(token))) return 'equipment';
  return 'other';
}

function resolveArsenalLocalVisual(itemKey, displayName) {
  const baseKey = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  const preloaded = getPreloadedWikiImagePath(baseKey);
  if (preloaded) {
    return {
      image: preloaded,
      source: 'wiki-preloaded',
      wikiName: displayName || baseKey
    };
  }

  const local = getLocalWeaponImagePath(baseKey);
  if (local !== '/img/weapons/_default.png') {
    return {
      image: local,
      source: 'local',
      wikiName: null
    };
  }

  const cache = loadWeaponMediaCache();
  const cached = cache[baseKey];
  if (cached && cached.image) {
    const abs = path.join(PUBLIC_DIR, String(cached.image).replace(/^\//, '').replace(/\//g, path.sep));
    if (fs.existsSync(abs)) {
      return {
        image: cached.image,
        source: 'wiki-cache',
        wikiName: cached.wikiName || null
      };
    }
  }

  return {
    image: '/img/weapons/_default.png',
    source: 'fallback',
    wikiName: null
  };
}

async function getCurrentShopOfferNameSet(db) {
  const out = { set: new Set(), hash: 0 };
  try {
    const cache = await db.collection('cache').findOne({ _id: 'shop' }, { projection: { data: 1, hash: 1 } });
    if (!cache || !Array.isArray(cache.data)) return out;
    out.hash = Number(cache.hash) || 0;
    cache.data.forEach(row => {
      const name = normalizeItemKeyToken(asTrimmedString(row && row.name));
      if (!name) return;
      out.set.add(name);
      out.set.add(name.replace(ITEM_VARIANT_SUFFIX_RE, ''));
    });
  } catch (e) {
    log('ARSENAL', `shop cache read error: ${e.message}`);
  }
  return out;
}

function buildArsenalCatalog(shopSet) {
  const names = loadItemNames();
  const byKey = new Map();
  const safeShopSet = shopSet instanceof Set ? shopSet : new Set();
  const inventory = loadGameItemInventory();

  if (inventory.available && inventory.items.length) {
    for (const item of inventory.items) {
      const key = normalizeItemKeyToken(item.id);
      if (!key || byKey.has(key)) continue;

      const base = key.replace(ITEM_VARIANT_SUFFIX_RE, '');
      const aliases = [key, base, `${base}_shop`].filter(Boolean);
      const displayName = cleanItemDisplayName(item.displayName || names[key] || names[base] || key);
      const manifestImage = inventoryImagePathToPublicUrl(item.imagePath);
      const fallbackVisual = manifestImage ? null : resolveArsenalLocalVisual(base, displayName);

      byKey.set(key, {
        key,
        displayName,
        type: inferArsenalTypeFromInventoryItem(item),
        category: item.category || '',
        itemCategory: item.itemCategory || '',
        inShop: aliases.some(alias => safeShopSet.has(alias)),
        image: manifestImage || (fallbackVisual && fallbackVisual.image) || '/img/weapons/_default.png',
        imageSource: manifestImage ? (item.imageStatus || 'asset-inventory') : ((fallbackVisual && fallbackVisual.source) || 'fallback'),
        wikiName: item.wikiName || null,
        sourcePath: item.relativePath || ''
      });
    }

    const out = Array.from(byKey.values());
    out.sort((a, b) => {
      if (a.inShop !== b.inShop) return a.inShop ? -1 : 1;
      if (a.type !== b.type) return a.type.localeCompare(b.type);
      return a.displayName.localeCompare(b.displayName);
    });
    return out;
  }

  const addItem = (rawKey, displayNameRaw) => {
    const keyNorm = normalizeItemKeyToken(rawKey);
    if (!keyNorm) return;
    const base = keyNorm.replace(ITEM_VARIANT_SUFFIX_RE, '');
    if (!base || base.length < 2 || base.length > 120) return;

    const displayName = cleanItemDisplayName(displayNameRaw || names[keyNorm] || names[base] || base);
    const current = byKey.get(base);
    if (current) {
      if (!current.displayName || current.displayName === current.key) current.displayName = displayName;
      if (!current.inShop && (safeShopSet.has(keyNorm) || safeShopSet.has(base))) current.inShop = true;
      return;
    }

    const type = inferArsenalType(base);
    const visual = resolveArsenalLocalVisual(base, displayName);
    byKey.set(base, {
      key: base,
      displayName,
      type,
      inShop: safeShopSet.has(keyNorm) || safeShopSet.has(base),
      image: visual.image,
      imageSource: visual.source,
      wikiName: visual.wikiName || null
    });
  };

  Object.keys(names || {}).forEach(key => addItem(key, names[key]));
  safeShopSet.forEach(key => addItem(key, names[key] || names[key.replace(ITEM_VARIANT_SUFFIX_RE, '')] || key));

  const out = Array.from(byKey.values());
  out.sort((a, b) => {
    if (a.inShop !== b.inShop) return a.inShop ? -1 : 1;
    return a.displayName.localeCompare(b.displayName);
  });
  return out;
}

function xmlDecodeAttr(value) {
  return String(value || '')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&');
}

function xmlEscapeAttr(value) {
  return String(value === undefined || value === null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function parseXmlAttrs(raw) {
  const attrs = {};
  const re = /([A-Za-z0-9_:-]+)\s*=\s*"([^"]*)"/g;
  let m;
  while ((m = re.exec(String(raw || ''))) !== null) {
    attrs[m[1]] = xmlDecodeAttr(m[2]);
  }
  return attrs;
}

function attrsToXml(attrs, preferredOrder = []) {
  const seen = new Set();
  const parts = [];
  const push = key => {
    if (seen.has(key)) return;
    if (!Object.prototype.hasOwnProperty.call(attrs, key)) return;
    const value = attrs[key];
    if (value === undefined || value === null) return;
    seen.add(key);
    parts.push(`${key}="${xmlEscapeAttr(value)}"`);
  };
  preferredOrder.forEach(push);
  Object.keys(attrs || {}).forEach(push);
  return parts.join(' ');
}

function escapeRegExp(value) {
  return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function readTextFileRequired(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Arquivo nao encontrado: ${filePath}`);
  }
  return fs.readFileSync(filePath, 'utf8');
}

function ensureRewardBackupDir(reason) {
  if (!fs.existsSync(REWARD_BACKUP_DIR)) fs.mkdirSync(REWARD_BACKUP_DIR, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const dir = path.join(REWARD_BACKUP_DIR, `${stamp}-${makeSafeFileToken(reason)}`);
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

function backupRewardFileToDir(filePath, backupDir) {
  if (!fs.existsSync(filePath)) return null;
  const rel = path.relative(ROOT, filePath);
  const safeRel = rel.replace(/^[.\\/]+/, '').replace(/[\\/:"<>|?*\x00-\x1F]+/g, '__');
  const target = path.join(backupDir, safeRel || path.basename(filePath));
  fs.copyFileSync(filePath, target);
  return target;
}

function writeTextFileIfChanged(filePath, nextText, backupDir, changedFiles) {
  const current = fs.existsSync(filePath) ? fs.readFileSync(filePath, 'utf8') : '';
  if (current === nextText) return false;
  if (backupDir) backupRewardFileToDir(filePath, backupDir);
  fs.writeFileSync(filePath, nextText, 'utf8');
  if (changedFiles) changedFiles.push(filePath);
  return true;
}

function normalizeMissionType(raw) {
  const value = normalizeItemKeyToken(raw);
  return /^[a-z0-9_]+$/.test(value) ? value : '';
}

function parseMissionRewardRules(customRulesXml) {
  const out = [];
  const re = /<mission_reward\b([^>]*?)\/>/gi;
  let m;
  while ((m = re.exec(customRulesXml)) !== null) {
    const attrs = parseXmlAttrs(m[1]);
    const missionType = normalizeMissionType(attrs.mission_type);
    const rewardSet = normalizeItemKeyToken(attrs.reward_set);
    if (!missionType || !rewardSet) continue;
    out.push({
      missionType,
      rewardSet,
      enabled: String(attrs.enabled || '1') !== '0',
      useNotification: String(attrs.use_notification || '1') !== '0',
      attrs
    });
  }
  return out;
}

function parseSpecialRewardEvents(specialXml) {
  const events = new Map();
  const eventRe = /<event\b([^>]*)>([\s\S]*?)<\/event>/gi;
  let m;
  while ((m = eventRe.exec(specialXml)) !== null) {
    const attrs = parseXmlAttrs(m[1]);
    const name = normalizeItemKeyToken(attrs.name);
    if (!name) continue;
    const body = m[2] || '';
    const rewards = [];
    const childRe = /<(item|money)\b([^>]*?)\/>/gi;
    let c;
    while ((c = childRe.exec(body)) !== null) {
      const type = c[1].toLowerCase();
      const childAttrs = parseXmlAttrs(c[2]);
      if (type === 'item') {
        childAttrs.name = normalizeItemKeyToken(childAttrs.name);
        if (!childAttrs.name) continue;
        rewards.push({ type, attrs: childAttrs });
      } else if (type === 'money') {
        const currency = normalizeItemKeyToken(childAttrs.currency);
        if (!currency) continue;
        childAttrs.currency = currency;
        rewards.push({ type, attrs: childAttrs });
      }
    }
    events.set(name, {
      name,
      attrs,
      rewards,
      raw: m[0],
      body
    });
  }
  return events;
}

function parseNamedValueSection(xml, sectionName) {
  const out = {};
  const sectionRe = new RegExp(`<${escapeRegExp(sectionName)}\\b[^>]*>([\\s\\S]*?)<\\/${escapeRegExp(sectionName)}>`, 'i');
  const section = sectionRe.exec(xml);
  if (!section) return out;
  const itemRe = /<([A-Za-z0-9_]+)>\s*([^<]*?)\s*<\/\1>/g;
  let m;
  while ((m = itemRe.exec(section[1])) !== null) {
    out[normalizeMissionType(m[1])] = asTrimmedString(m[2]);
  }
  return out;
}

function parseRewardsConfigurationSummary(xml) {
  const crown = {};
  const crownRe = /<Reward\b([^>]*?)\/>/gi;
  let m;
  while ((m = crownRe.exec(xml)) !== null) {
    const attrs = parseXmlAttrs(m[1]);
    const type = normalizeMissionType(attrs.type);
    if (!type) continue;
    crown[type] = {
      bronze: attrs.bronze || '',
      silver: attrs.silver || '',
      gold: attrs.gold || ''
    };
  }

  const bonusPools = {};
  const bonusRe = /<BonusRewardPool\b([^>]*?)\/>/gi;
  while ((m = bonusRe.exec(xml)) !== null) {
    const attrs = parseXmlAttrs(m[1]);
    const type = normalizeMissionType(attrs.mission_type);
    if (!type) continue;
    bonusPools[type] = attrs.value || '';
  }

  return {
    moneyMultiplier: parseNamedValueSection(xml, 'MoneyMultiplier'),
    experienceMultiplier: parseNamedValueSection(xml, 'ExperienceMultiplier'),
    sponsorPointsMultiplier: parseNamedValueSection(xml, 'SponsorPointsMultiplier'),
    crown,
    bonusPools
  };
}

function listXmlFilesRecursive(dir) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  const stack = [dir];
  while (stack.length) {
    const current = stack.pop();
    const entries = fs.readdirSync(current, { withFileTypes: true });
    for (const entry of entries) {
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(full);
      } else if (entry.isFile() && entry.name.toLowerCase().endsWith('.xml')) {
        out.push(full);
      }
    }
  }
  return out;
}

function summarizeMissionXml(filePath) {
  const xml = fs.readFileSync(filePath, 'utf8');
  const rootMatch = /<mission\b([^>]*)>/i.exec(xml);
  if (!rootMatch) return null;
  const rootAttrs = parseXmlAttrs(rootMatch[1]);
  const missionType = normalizeMissionType(rootAttrs.mission_type);
  if (!missionType) return null;

  const sublevels = [];
  const sublevelRe = /<Sublevel\b([^>]*)>/gi;
  let m;
  while ((m = sublevelRe.exec(xml)) !== null) {
    sublevels.push(parseXmlAttrs(m[1]));
  }

  const rewardPools = [];
  const poolRe = /<Pool\b([^>]*?)\/>/gi;
  while ((m = poolRe.exec(xml)) !== null) {
    const attrs = parseXmlAttrs(m[1]);
    const value = parseStrictInt(attrs.value);
    if (value !== null) rewardPools.push(value);
  }

  let thresholds = null;
  const thresholdsMatch = /<CrownRewardsThresholds\b[^>]*>([\s\S]*?)<\/CrownRewardsThresholds>/i.exec(xml);
  if (thresholdsMatch) {
    const totalMatch = /<TotalPerformance\b([^>]*?)\/>/i.exec(thresholdsMatch[1]);
    const timeMatch = /<Time\b([^>]*?)\/>/i.exec(thresholdsMatch[1]);
    thresholds = {
      score: totalMatch ? parseXmlAttrs(totalMatch[1]) : {},
      time: timeMatch ? parseXmlAttrs(timeMatch[1]) : {}
    };
  }

  const firstSub = sublevels[0] || {};
  return {
    file: filePath,
    relativePath: path.relative(MISSIONS_DIR, filePath).replace(/\\/g, '/'),
    name: rootAttrs.name || path.basename(filePath, '.xml'),
    uid: rootAttrs.uid || '',
    missionType,
    difficulty: rootAttrs.difficulty || '',
    releaseMission: rootAttrs.release_mission || '',
    sublevelCount: sublevels.length,
    pools: {
      win: firstSub.win_pool || '',
      lose: firstSub.lose_pool || '',
      draw: firstSub.draw_pool || '',
      score: firstSub.score_pool || ''
    },
    rewardPools: {
      count: rewardPools.length,
      min: rewardPools.length ? Math.min(...rewardPools) : null,
      max: rewardPools.length ? Math.max(...rewardPools) : null
    },
    thresholds
  };
}

function getMissionSummariesByType() {
  const byType = new Map();
  for (const filePath of listXmlFilesRecursive(MISSIONS_DIR)) {
    try {
      const summary = summarizeMissionXml(filePath);
      if (!summary) continue;
      if (!byType.has(summary.missionType)) byType.set(summary.missionType, []);
      byType.get(summary.missionType).push(summary);
    } catch (e) {
      log('REWARDS', `mission parse skipped ${filePath}: ${e.message}`);
    }
  }
  for (const list of byType.values()) {
    list.sort((a, b) => a.relativePath.localeCompare(b.relativePath));
  }
  return byType;
}

function getPreloadedStemPath(stem) {
  const cleanStem = normalizeItemKeyToken(stem);
  if (!cleanStem) return null;
  const idx = loadWikiAllImagesIndex();
  const byStem = idx && idx.byStem ? idx.byStem : {};
  const candidate = byStem[cleanStem];
  const list = Array.isArray(candidate) ? candidate.filter(Boolean) : (candidate ? [candidate] : []);
  if (!list.length) return null;
  const exactPng = list.find(value => path.basename(String(value)).toLowerCase() === `${cleanStem}.png`);
  return exactPng || list[0];
}

function stripRewardVariantSuffix(key) {
  let out = normalizeItemKeyToken(String(key || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  out = out.replace(/_(gold|crown|silver|bronze|diamond|premium)\d*$/i, '');
  out = out.replace(/_(gp|sp|sc|rds|as|bp|ugl|ss|is)\d*_?d$/i, '');
  return normalizeItemKeyToken(out);
}

function extractRewardWeaponToken(key) {
  const clean = stripRewardVariantSuffix(key);
  const match = clean.match(/^((?:ar|sr|smg|shg|mg|hmg|pt|kn)\d{1,5})(?:_|$)/i);
  return match ? normalizeItemKeyToken(match[1]) : clean;
}

function getRandomBoxPrimaryPrizeFromXml(item) {
  const rel = String(item && item.relativePath || '').replace(/\\/g, '/');
  if (!rel) return '';
  const candidates = [
    path.resolve(__dirname, '..', 'AssetInventory', 'extracted', 'GameData', 'items', rel),
    path.join(MASTER_DIR, 'gamedata', 'items', rel)
  ];
  for (const filePath of candidates) {
    try {
      if (!fs.existsSync(filePath)) continue;
      const xml = fs.readFileSync(filePath, 'utf8');
      const randomBox = xml.match(/<random_box\b[\s\S]*?<\/random_box>/i);
      const scope = randomBox ? randomBox[0] : xml;
      const itemRe = /<item\b([^>]*)\/?>/ig;
      let firstName = '';
      let match;
      while ((match = itemRe.exec(scope))) {
        const attrs = match[1] || '';
        const nameMatch = attrs.match(/\bname\s*=\s*"([^"]+)"/i);
        const name = normalizeItemKeyToken(nameMatch && nameMatch[1]);
        if (!name) continue;
        if (!firstName) firstName = name;
        const weaponToken = extractRewardWeaponToken(name);
        if (/^(?:ar|sr|smg|shg|mg|hmg|pt|kn)\d{1,5}$/i.test(weaponToken)) return name;
      }
      if (firstName) return firstName;
    } catch (_) {
      continue;
    }
  }
  return '';
}

function resolveWarboxIconImage(item) {
  if (!item) return null;
  const candidates = [];
  const add = value => {
    const key = normalizeItemKeyToken(value);
    if (key && !candidates.includes(key)) candidates.push(key);
  };
  const icon = normalizeItemKeyToken(item.icon);
  add(icon);
  if (icon && !icon.startsWith('icons_')) add(`icons_${icon}`);

  const topPrize = Array.isArray(item.topPrizes) && item.topPrizes.length ? item.topPrizes[0] : getRandomBoxPrimaryPrizeFromXml(item);
  const weaponToken = extractRewardWeaponToken(topPrize);
  if (weaponToken) {
    add(`icons_randombox_${weaponToken}`);
    add(`${weaponToken}randombox`);
    add(`randombox_${weaponToken}`);
  }

  for (const candidate of candidates) {
    const image = getPreloadedStemPath(candidate);
    if (image) return image;
  }
  return null;
}

function getRewardItemSuggestions(limit = 1200) {
  const inventory = loadGameItemInventory();
  const names = loadItemNames();
  if (!inventory.available) return { available: false, items: [], reason: inventory.reason || '' };

  const isGenericRewardImage = image => !image || /\/_default\.png$|icons_randombox_skins\.png$/i.test(String(image));
  const scored = inventory.items.map(item => {
    const key = normalizeItemKeyToken(item.id);
    const displayName = cleanItemDisplayName(item.displayName || names[key] || key);
    const category = normalizeItemKeyToken(item.category || item.itemCategory || '');
    const manifestImage = inventoryImagePathToPublicUrl(item.imagePath);
    const warboxIconImage = /random_box|randombox|warbox|warcase|box/i.test(`${key} ${item.type || ''} ${item.itemCategory || ''}`) ? resolveWarboxIconImage(item) : null;
    let visual = !manifestImage || isGenericRewardImage(manifestImage) ? resolveArsenalLocalVisual(key, displayName) : null;
    if (!warboxIconImage && (!visual || isGenericRewardImage(visual.image)) && Array.isArray(item.topPrizes) && item.topPrizes.length) {
      const topPrizeKey = normalizeItemKeyToken(String(item.topPrizes[0] || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
      if (topPrizeKey) visual = resolveArsenalLocalVisual(topPrizeKey, names[topPrizeKey] || item.topPrizes[0]);
    }
    let score = 0;
    if (key.includes('random_box') || key.startsWith('box_')) score += 80;
    if (key.includes('warbox') || key.includes('_box_')) score += 55;
    if (key.includes('key_') || key.includes('coin_')) score += 20;
    if (category === 'shopitems' || category === 'items') score += 25;
    if (category === 'weapons' || category === 'armor') score += 8;
    return {
      key,
      displayName,
      category: item.category || '',
      image: warboxIconImage || (visual && !isGenericRewardImage(visual.image) ? visual.image : '') || manifestImage || (visual && visual.image) || '/img/weapons/_default.png',
      imageSource: warboxIconImage ? 'warbox-icon' : ((visual && !isGenericRewardImage(visual.image) ? visual.source : '') || (manifestImage ? (item.imageStatus || 'asset-inventory') : '') || ((visual && visual.source) || 'fallback')),
      score
    };
  }).filter(item => item.key);

  scored.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    return a.key.localeCompare(b.key);
  });

  return {
    available: true,
    items: scored.slice(0, limit).map(({ score, ...item }) => item)
  };
}

function getSurvivalRewardConfig() {
  const customXml = readTextFileRequired(CUSTOM_RULES_FILE);
  const specialXml = readTextFileRequired(SPECIAL_REWARD_CONFIG_FILE);
  const rewardsXml = readTextFileRequired(REWARDS_CONFIG_FILE);
  const missionRules = parseMissionRewardRules(customXml);
  const events = parseSpecialRewardEvents(specialXml);
  const rewards = parseRewardsConfigurationSummary(rewardsXml);
  const missionsByType = getMissionSummariesByType();
  const itemSuggestions = getRewardItemSuggestions();

  const rows = missionRules.map(rule => {
    const event = events.get(rule.rewardSet) || { rewards: [], attrs: { name: rule.rewardSet } };
    const money = { game_money: 0, cry_money: 0, crown_money: 0 };
    const items = [];
    for (const reward of event.rewards || []) {
      if (reward.type === 'money') {
        const currency = normalizeItemKeyToken(reward.attrs.currency);
        if (Object.prototype.hasOwnProperty.call(money, currency)) {
          money[currency] += Number(reward.attrs.amount) || 0;
        }
      } else if (reward.type === 'item') {
        items.push({
          name: reward.attrs.name || '',
          amount: reward.attrs.amount || '',
          expiration: reward.attrs.expiration || '',
          durability: reward.attrs.durability || '',
          regular: Object.prototype.hasOwnProperty.call(reward.attrs, 'regular')
        });
      }
    }

    const missionFiles = missionsByType.get(rule.missionType) || [];
    const sample = missionFiles[0] || null;
    return {
      missionType: rule.missionType,
      rewardSet: rule.rewardSet,
      enabled: rule.enabled,
      useNotification: rule.useNotification,
      rewards: { items, money },
      multipliers: {
        gp: rewards.moneyMultiplier[rule.missionType] || rewards.moneyMultiplier.default || '',
        xp: rewards.experienceMultiplier[rule.missionType] || rewards.experienceMultiplier.default || '',
        cash: rewards.sponsorPointsMultiplier[rule.missionType] || rewards.sponsorPointsMultiplier.default || ''
      },
      crown: rewards.crown[rule.missionType] || null,
      bonusPool: rewards.bonusPools[rule.missionType] || '',
      missions: {
        count: missionFiles.length,
        sample: sample ? {
          name: sample.name,
          relativePath: sample.relativePath,
          releaseMission: sample.releaseMission,
          difficulty: sample.difficulty,
          sublevelCount: sample.sublevelCount,
          pools: sample.pools,
          rewardPools: sample.rewardPools,
          thresholds: sample.thresholds
        } : null
      }
    };
  });

  rows.sort((a, b) => a.missionType.localeCompare(b.missionType));
  return {
    generatedAt: new Date().toISOString(),
    paths: {
      customRules: CUSTOM_RULES_FILE,
      specialRewards: SPECIAL_REWARD_CONFIG_FILE,
      rewardsConfig: REWARDS_CONFIG_FILE,
      missionsDir: MISSIONS_DIR
    },
    rows,
    itemSuggestions
  };
}

function parseOptionalRewardInt(fields, key, label, min = 0, max = 2147483647) {
  if (!Object.prototype.hasOwnProperty.call(fields, key)) return { set: false };
  const raw = fields[key];
  if (raw === '' || raw === null || raw === undefined) return { set: false };
  const parsed = parseStrictInt(String(raw));
  if (parsed === null || parsed < min || parsed > max) {
    throw new Error(`${label} invalido`);
  }
  return { set: true, value: parsed };
}

function parseOptionalRewardFloat(fields, key, label, min = 0, max = 9999) {
  if (!Object.prototype.hasOwnProperty.call(fields, key)) return { set: false };
  const raw = fields[key];
  if (raw === '' || raw === null || raw === undefined) return { set: false };
  const text = String(raw).trim().replace(',', '.');
  if (!/^\d+(?:\.\d+)?$/.test(text)) throw new Error(`${label} invalido`);
  const value = Number(text);
  if (!Number.isFinite(value) || value < min || value > max) throw new Error(`${label} invalido`);
  return { set: true, value: String(value) };
}

function parseOptionalRewardString(fields, key, label, maxLen = 160) {
  if (!Object.prototype.hasOwnProperty.call(fields, key)) return { set: false };
  const value = asTrimmedString(fields[key]);
  if (value.length > maxLen) throw new Error(`${label} muito longo`);
  return { set: true, value };
}

function sanitizeSurvivalRewardItemEntry(rawItem, index) {
  const item = rawItem && typeof rawItem === 'object' ? rawItem : {};
  const rawName = asTrimmedString(item.name || item.key || item.item);
  const normalized = normalizeItemKeyToken(rawName);
  if (!normalized) throw new Error(`Item final ${index + 1} invalido`);
  const match = findGameItemForOfferName(normalized);
  if (match.inventory.available && !match.ok) {
    throw new Error(`Item final nao existe no GameData: ${normalized}`);
  }

  const amount = parseOptionalRewardInt(item, 'amount', `Quantidade do item ${index + 1}`, 0, 999999);
  const expiration = parseOptionalRewardString(item, 'expiration', `Expiracao do item ${index + 1}`, 16);
  const durability = parseOptionalRewardInt(item, 'durability', `Durabilidade do item ${index + 1}`, 0, 1000000);

  return {
    name: match.item && match.item.id ? normalizeItemKeyToken(match.item.id) : normalized,
    amount: amount.set ? amount.value : null,
    expiration: expiration.set ? expiration.value : '',
    durability: durability.set ? durability.value : null
  };
}

function sanitizeSurvivalRewardFields(rawFields) {
  const fields = rawFields && typeof rawFields === 'object' ? rawFields : {};
  const itemNameRaw = parseOptionalRewardString(fields, 'rewardItemName', 'Item final');
  let rewardItemName = itemNameRaw;
  if (itemNameRaw.set && itemNameRaw.value) {
    const normalized = normalizeItemKeyToken(itemNameRaw.value);
    if (!normalized) throw new Error('Item final invalido');
    const match = findGameItemForOfferName(normalized);
    if (match.inventory.available && !match.ok) {
      throw new Error(`Item final nao existe no GameData: ${normalized}`);
    }
    rewardItemName = { set: true, value: match.item && match.item.id ? normalizeItemKeyToken(match.item.id) : normalized };
  }

  let rewardItems = { set: false };
  if (Object.prototype.hasOwnProperty.call(fields, 'rewardItems')) {
    if (!Array.isArray(fields.rewardItems)) throw new Error('Lista de itens finais invalida');
    if (fields.rewardItems.length > 50) throw new Error('Lista de itens finais muito grande');
    rewardItems = {
      set: true,
      value: fields.rewardItems.map((item, index) => sanitizeSurvivalRewardItemEntry(item, index))
    };
  }

  const clean = {
    rewardItems,
    rewardItemName,
    rewardItemAmount: parseOptionalRewardInt(fields, 'rewardItemAmount', 'Quantidade do item', 0, 999999),
    rewardItemExpiration: parseOptionalRewardString(fields, 'rewardItemExpiration', 'Expiracao do item', 16),
    rewardItemDurability: parseOptionalRewardInt(fields, 'rewardItemDurability', 'Durabilidade do item', 0, 1000000),
    gpAmount: parseOptionalRewardInt(fields, 'gpAmount', 'GP final'),
    cashAmount: parseOptionalRewardInt(fields, 'cashAmount', 'Cash final'),
    crownAmount: parseOptionalRewardInt(fields, 'crownAmount', 'Coroas finais'),
    moneyMultiplier: parseOptionalRewardFloat(fields, 'moneyMultiplier', 'Multiplicador GP'),
    xpMultiplier: parseOptionalRewardFloat(fields, 'xpMultiplier', 'Multiplicador XP'),
    cashMultiplier: parseOptionalRewardFloat(fields, 'cashMultiplier', 'Multiplicador cash'),
    crownBronze: parseOptionalRewardInt(fields, 'crownBronze', 'Coroa bronze', 0, 1000000),
    crownSilver: parseOptionalRewardInt(fields, 'crownSilver', 'Coroa prata', 0, 1000000),
    crownGold: parseOptionalRewardInt(fields, 'crownGold', 'Coroa ouro', 0, 1000000),
    bonusPool: parseOptionalRewardInt(fields, 'bonusPool', 'Bonus pool', 0, 2147483647),
    winPool: parseOptionalRewardInt(fields, 'winPool', 'Win pool', 0, 2147483647),
    losePool: parseOptionalRewardInt(fields, 'losePool', 'Lose pool', 0, 2147483647),
    drawPool: parseOptionalRewardInt(fields, 'drawPool', 'Draw pool', 0, 2147483647),
    scorePool: parseOptionalRewardInt(fields, 'scorePool', 'Score pool', 0, 2147483647),
    rewardPoolValue: parseOptionalRewardInt(fields, 'rewardPoolValue', 'Pool checkpoint', 0, 2147483647),
    timeBronze: parseOptionalRewardInt(fields, 'timeBronze', 'Tempo bronze', 0, 2147483647),
    timeSilver: parseOptionalRewardInt(fields, 'timeSilver', 'Tempo prata', 0, 2147483647),
    timeGold: parseOptionalRewardInt(fields, 'timeGold', 'Tempo ouro', 0, 2147483647),
    scoreBronze: parseOptionalRewardInt(fields, 'scoreBronze', 'Pontuacao bronze', 0, 2147483647),
    scoreSilver: parseOptionalRewardInt(fields, 'scoreSilver', 'Pontuacao prata', 0, 2147483647),
    scoreGold: parseOptionalRewardInt(fields, 'scoreGold', 'Pontuacao ouro', 0, 2147483647)
  };

  const anyChange = Object.values(clean).some(value => value && value.set);
  if (!anyChange) throw new Error('Preencha ao menos um campo para aplicar');
  return clean;
}

function rewardFieldValue(field) {
  return field && field.set ? field.value : undefined;
}

function updateRewardMoney(rewards, field, currency) {
  if (!field || !field.set) return rewards;
  const next = rewards.filter(row => !(row.type === 'money' && normalizeItemKeyToken(row.attrs.currency) === currency));
  if (field.value > 0) {
    next.push({ type: 'money', attrs: { currency, amount: String(field.value) } });
  }
  return next;
}

function buildSurvivalItemRewardAttrs(item) {
  const attrs = { name: item.name, regular: '' };
  if (item.amount !== null && item.amount !== undefined && item.amount > 0) {
    attrs.amount = String(item.amount);
    delete attrs.regular;
  }
  if (item.expiration) attrs.expiration = item.expiration;
  if (item.durability !== null && item.durability !== undefined && item.durability > 0) {
    attrs.durability = String(item.durability);
  }
  return attrs;
}

function updateEventRewards(event, fields) {
  let rewards = event && Array.isArray(event.rewards) ? event.rewards.map(row => ({
    type: row.type,
    attrs: Object.assign({}, row.attrs || {})
  })) : [];

  const firstItemIndex = rewards.findIndex(row => row.type === 'item');
  if (fields.rewardItems && fields.rewardItems.set) {
    const nonItemRewards = rewards.filter(row => row.type !== 'item');
    const itemRewards = fields.rewardItems.value.map(item => ({
      type: 'item',
      attrs: buildSurvivalItemRewardAttrs(item)
    }));
    rewards = itemRewards.concat(nonItemRewards);
  } else if (fields.rewardItemName.set) {
    rewards = rewards.filter(row => row.type !== 'item');
    if (fields.rewardItemName.value) {
      rewards.unshift({
        type: 'item',
        attrs: buildSurvivalItemRewardAttrs({
          name: fields.rewardItemName.value,
          amount: fields.rewardItemAmount.set ? fields.rewardItemAmount.value : null,
          expiration: fields.rewardItemExpiration.set ? fields.rewardItemExpiration.value : '',
          durability: fields.rewardItemDurability.set ? fields.rewardItemDurability.value : null
        })
      });
    }
  } else if (firstItemIndex >= 0) {
    const attrs = rewards[firstItemIndex].attrs;
    if (fields.rewardItemAmount.set) {
      if (fields.rewardItemAmount.value > 0) {
        attrs.amount = String(fields.rewardItemAmount.value);
        delete attrs.regular;
      } else {
        delete attrs.amount;
        attrs.regular = '';
      }
    }
    if (fields.rewardItemExpiration.set) {
      if (fields.rewardItemExpiration.value) attrs.expiration = fields.rewardItemExpiration.value;
      else delete attrs.expiration;
    }
    if (fields.rewardItemDurability.set) {
      if (fields.rewardItemDurability.value > 0) attrs.durability = String(fields.rewardItemDurability.value);
      else delete attrs.durability;
    }
  }

  rewards = updateRewardMoney(rewards, fields.gpAmount, 'game_money');
  rewards = updateRewardMoney(rewards, fields.cashAmount, 'cry_money');
  rewards = updateRewardMoney(rewards, fields.crownAmount, 'crown_money');
  return rewards;
}

function renderSpecialRewardEvent(eventName, oldEvent, fields) {
  const attrs = Object.assign({}, oldEvent && oldEvent.attrs ? oldEvent.attrs : {});
  attrs.name = eventName;
  if (!Object.prototype.hasOwnProperty.call(attrs, 'use_notification')) attrs.use_notification = '1';
  const rewards = updateEventRewards(oldEvent, fields);
  const childLines = rewards.map(reward => {
    const order = reward.type === 'item'
      ? ['name', 'amount', 'expiration', 'durability', 'regular']
      : ['currency', 'amount'];
    return `\t\t<${reward.type} ${attrsToXml(reward.attrs, order)}/>`;
  });
  const body = childLines.length ? `\n${childLines.join('\n')}\n\t` : '';
  return `\t<event ${attrsToXml(attrs, ['name', 'use_notification'])}>${body}</event>`;
}

function updateSpecialRewardXml(xml, selectedRules, fields) {
  let nextXml = xml;
  const events = parseSpecialRewardEvents(nextXml);
  for (const rule of selectedRules) {
    const eventName = rule.rewardSet;
    const oldEvent = events.get(eventName) || { attrs: { name: eventName, use_notification: rule.useNotification ? '1' : '0' }, rewards: [] };
    const rendered = renderSpecialRewardEvent(eventName, oldEvent, fields);
    if (oldEvent.raw) {
      nextXml = nextXml.replace(oldEvent.raw, rendered);
    } else {
      nextXml = nextXml.replace(/\s*<\/Settings>\s*$/i, `\n${rendered}\n</Settings>\n`);
    }
    events.set(eventName, Object.assign({}, oldEvent, { raw: rendered }));
  }
  return nextXml;
}

function updateNamedMultiplierEntry(xml, sectionName, missionType, field) {
  if (!field || !field.set) return xml;
  const sectionRe = new RegExp(`(<${escapeRegExp(sectionName)}\\b[^>]*>)([\\s\\S]*?)(\\s*<\\/${escapeRegExp(sectionName)}>)`, 'i');
  return xml.replace(sectionRe, (full, open, body, close) => {
    const entryRe = new RegExp(`\\s*<${escapeRegExp(missionType)}>[^<]*<\\/${escapeRegExp(missionType)}>`, 'i');
    const line = `\n\t\t\t<${missionType}>${xmlEscapeAttr(field.value)}</${missionType}>`;
    if (entryRe.test(body)) {
      return `${open}${body.replace(entryRe, line)}${close}`;
    }
    return `${open}${body}${line}${close}`;
  });
}

function updateCrownRewardEntry(xml, missionType, fields) {
  const hasAny = fields.crownBronze.set || fields.crownSilver.set || fields.crownGold.set;
  if (!hasAny) return xml;
  const currentMatch = new RegExp(`<Reward\\b([^>]*?type="${escapeRegExp(missionType)}"[^>]*?)\\/>`, 'i').exec(xml);
  const current = currentMatch ? parseXmlAttrs(currentMatch[1]) : { type: missionType, bronze: '0', silver: '0', gold: '0' };
  if (fields.crownBronze.set) current.bronze = String(fields.crownBronze.value);
  if (fields.crownSilver.set) current.silver = String(fields.crownSilver.value);
  if (fields.crownGold.set) current.gold = String(fields.crownGold.value);
  const rendered = `<Reward ${attrsToXml(current, ['type', 'bronze', 'silver', 'gold'])}/>`;
  if (currentMatch) return xml.replace(currentMatch[0], rendered);
  return xml.replace(/\s*<\/CrownRewards>/i, `\n\t\t${rendered}\n\t</CrownRewards>`);
}

function updateBonusRewardPoolEntry(xml, missionType, field) {
  if (!field || !field.set) return xml;
  const currentMatch = new RegExp(`<BonusRewardPool\\b([^>]*?mission_type="${escapeRegExp(missionType)}"[^>]*?)\\/>`, 'i').exec(xml);
  const attrs = currentMatch ? parseXmlAttrs(currentMatch[1]) : { mission_type: missionType };
  attrs.value = String(field.value);
  const rendered = `<BonusRewardPool ${attrsToXml(attrs, ['mission_type', 'value'])} />`;
  if (currentMatch) return xml.replace(currentMatch[0], rendered);
  return xml.replace(/\s*<player_count_reward_mults>/i, `\n\t\t${rendered}\n\t\t<player_count_reward_mults>`);
}

function updateRewardsConfigurationXml(xml, selectedRules, fields) {
  let nextXml = xml;
  for (const rule of selectedRules) {
    nextXml = updateNamedMultiplierEntry(nextXml, 'MoneyMultiplier', rule.missionType, fields.moneyMultiplier);
    nextXml = updateNamedMultiplierEntry(nextXml, 'ExperienceMultiplier', rule.missionType, fields.xpMultiplier);
    nextXml = updateNamedMultiplierEntry(nextXml, 'SponsorPointsMultiplier', rule.missionType, fields.cashMultiplier);
    nextXml = updateBonusRewardPoolEntry(nextXml, rule.missionType, fields.bonusPool);
    nextXml = updateCrownRewardEntry(nextXml, rule.missionType, fields);
  }
  return nextXml;
}

function setXmlTagAttrs(tagText, attrs) {
  let out = tagText;
  for (const [key, value] of Object.entries(attrs)) {
    if (value === undefined || value === null) continue;
    const attrRe = new RegExp(`\\s${escapeRegExp(key)}="[^"]*"`, 'i');
    const nextAttr = ` ${key}="${xmlEscapeAttr(value)}"`;
    if (attrRe.test(out)) out = out.replace(attrRe, nextAttr);
    else out = out.replace(/(\s*\/?>)$/, `${nextAttr}$1`);
  }
  return out;
}

function anyFieldsSet(fields, keys) {
  return keys.some(key => fields[key] && fields[key].set);
}

function updateThresholdNode(block, tagName, fieldMap) {
  const hasAny = Object.values(fieldMap).some(field => field && field.set);
  if (!hasAny) return block;
  const tagRe = new RegExp(`<${escapeRegExp(tagName)}\\b([^>]*?)\\/>`, 'i');
  const currentMatch = tagRe.exec(block);
  const attrs = currentMatch ? parseXmlAttrs(currentMatch[1]) : { bronze: '0', silver: '0', gold: '0' };
  if (fieldMap.bronze.set) attrs.bronze = String(fieldMap.bronze.value);
  if (fieldMap.silver.set) attrs.silver = String(fieldMap.silver.value);
  if (fieldMap.gold.set) attrs.gold = String(fieldMap.gold.value);
  const rendered = `<${tagName} ${attrsToXml(attrs, ['bronze', 'silver', 'gold'])}/>`;
  if (currentMatch) return block.replace(currentMatch[0], rendered);
  return block.replace(/\s*<\/CrownRewardsThresholds>/i, `\n\t\t\t${rendered}\n\t\t</CrownRewardsThresholds>`);
}

function updateSublevelThresholds(block, fields) {
  const hasScore = anyFieldsSet(fields, ['scoreBronze', 'scoreSilver', 'scoreGold']);
  const hasTime = anyFieldsSet(fields, ['timeBronze', 'timeSilver', 'timeGold']);
  if (!hasScore && !hasTime) return block;

  const existingRe = /<CrownRewardsThresholds\b[^>]*>[\s\S]*?<\/CrownRewardsThresholds>/i;
  let thresholdBlock = existingRe.exec(block);
  let nextThreshold = thresholdBlock ? thresholdBlock[0] : '<CrownRewardsThresholds>\n\t\t</CrownRewardsThresholds>';
  nextThreshold = updateThresholdNode(nextThreshold, 'TotalPerformance', {
    bronze: fields.scoreBronze,
    silver: fields.scoreSilver,
    gold: fields.scoreGold
  });
  nextThreshold = updateThresholdNode(nextThreshold, 'Time', {
    bronze: fields.timeBronze,
    silver: fields.timeSilver,
    gold: fields.timeGold
  });

  if (thresholdBlock) return block.replace(existingRe, nextThreshold);
  return block.replace(/(<Sublevel\b[^>]*>)/i, `$1\n\t\t${nextThreshold}`);
}

function updateMissionXmlContent(xml, fields) {
  let nextXml = xml;
  const poolAttrs = {};
  if (fields.winPool.set) poolAttrs.win_pool = String(fields.winPool.value);
  if (fields.losePool.set) poolAttrs.lose_pool = String(fields.losePool.value);
  if (fields.drawPool.set) poolAttrs.draw_pool = String(fields.drawPool.value);
  if (fields.scorePool.set) poolAttrs.score_pool = String(fields.scorePool.value);
  if (Object.keys(poolAttrs).length) {
    nextXml = nextXml.replace(/<Sublevel\b[^>]*>/gi, tag => setXmlTagAttrs(tag, poolAttrs));
  }
  if (fields.rewardPoolValue.set) {
    nextXml = nextXml.replace(/<Pool\b[^>]*?\/>/gi, tag => setXmlTagAttrs(tag, { value: String(fields.rewardPoolValue.value) }));
  }
  if (anyFieldsSet(fields, ['scoreBronze', 'scoreSilver', 'scoreGold', 'timeBronze', 'timeSilver', 'timeGold'])) {
    nextXml = nextXml.replace(/<Sublevel\b[^>]*>[\s\S]*?<\/Sublevel>/gi, block => updateSublevelThresholds(block, fields));
  }
  return nextXml;
}

function getSelectedSurvivalRules(allRules, body) {
  const scope = asTrimmedString(body && body.scope).toLowerCase() === 'all' ? 'all' : 'selected';
  if (scope === 'all') return allRules.slice();
  const requested = Array.isArray(body && body.missionTypes) ? body.missionTypes.map(normalizeMissionType).filter(Boolean) : [];
  const requestedSet = new Set(requested);
  return allRules.filter(rule => requestedSet.has(rule.missionType));
}

function applySurvivalRewardChanges(body) {
  const customXml = readTextFileRequired(CUSTOM_RULES_FILE);
  const missionRules = parseMissionRewardRules(customXml);
  const selectedRules = getSelectedSurvivalRules(missionRules, body);
  if (!selectedRules.length) throw new Error('Nenhuma sobrevivencia selecionada');
  const fields = sanitizeSurvivalRewardFields(body && body.fields);
  const dryRun = !!(body && body.dryRun);

  const missionTypes = new Set(selectedRules.map(rule => rule.missionType));
  const missionFiles = [];
  if (anyFieldsSet(fields, [
    'winPool', 'losePool', 'drawPool', 'scorePool', 'rewardPoolValue',
    'scoreBronze', 'scoreSilver', 'scoreGold', 'timeBronze', 'timeSilver', 'timeGold'
  ])) {
    for (const filePath of listXmlFilesRecursive(MISSIONS_DIR)) {
      try {
        const summary = summarizeMissionXml(filePath);
        if (summary && missionTypes.has(summary.missionType)) missionFiles.push(filePath);
      } catch (e) {
        log('REWARDS', `mission update skipped ${filePath}: ${e.message}`);
      }
    }
  }

  if (dryRun) {
    return {
      dryRun: true,
      missionTypes: Array.from(missionTypes).sort(),
      missionTypeCount: missionTypes.size,
      missionFileCount: missionFiles.length,
      changedFiles: []
    };
  }

  const changedFiles = [];
  const backupDir = ensureRewardBackupDir('survival-rewards');

  const specialXml = readTextFileRequired(SPECIAL_REWARD_CONFIG_FILE);
  const updatedSpecialXml = updateSpecialRewardXml(specialXml, selectedRules, fields);
  writeTextFileIfChanged(SPECIAL_REWARD_CONFIG_FILE, updatedSpecialXml, backupDir, changedFiles);

  const rewardsXml = readTextFileRequired(REWARDS_CONFIG_FILE);
  const updatedRewardsXml = updateRewardsConfigurationXml(rewardsXml, selectedRules, fields);
  writeTextFileIfChanged(REWARDS_CONFIG_FILE, updatedRewardsXml, backupDir, changedFiles);

  for (const filePath of missionFiles) {
    const xml = readTextFileRequired(filePath);
    const updatedXml = updateMissionXmlContent(xml, fields);
    writeTextFileIfChanged(filePath, updatedXml, backupDir, changedFiles);
  }

  return {
    dryRun: false,
    missionTypes: Array.from(missionTypes).sort(),
    missionTypeCount: missionTypes.size,
    missionFileCount: missionFiles.length,
    changedFiles,
    backupDir
  };
}

let weaponMediaCache = null;
let wikiAllImagesIndexCache = null;
let wikiAllImagesIndexMtimeMs = 0;

function loadWeaponMediaCache() {
  if (weaponMediaCache) return weaponMediaCache;
  try {
    if (!fs.existsSync(WEAPON_MEDIA_CACHE_FILE)) {
      weaponMediaCache = {};
      return weaponMediaCache;
    }
    const raw = JSON.parse(fs.readFileSync(WEAPON_MEDIA_CACHE_FILE, 'utf8'));
    weaponMediaCache = raw && typeof raw === 'object' ? raw : {};
    return weaponMediaCache;
  } catch (e) {
    log('WIKI', `cache load error: ${e.message}`);
    weaponMediaCache = {};
    return weaponMediaCache;
  }
}

function saveWeaponMediaCache() {
  try {
    const cache = loadWeaponMediaCache();
    fs.writeFileSync(WEAPON_MEDIA_CACHE_FILE, JSON.stringify(cache, null, 2), 'utf8');
  } catch (e) {
    log('WIKI', `cache save error: ${e.message}`);
  }
}

function loadWikiAllImagesIndex() {
  try {
    if (!fs.existsSync(WIKI_ALL_IMAGES_INDEX_FILE)) {
      wikiAllImagesIndexCache = { byStem: {}, byName: {} };
      wikiAllImagesIndexMtimeMs = 0;
      return wikiAllImagesIndexCache;
    }
    const st = fs.statSync(WIKI_ALL_IMAGES_INDEX_FILE);
    const mtimeMs = Number(st.mtimeMs || 0);
    if (wikiAllImagesIndexCache && wikiAllImagesIndexMtimeMs === mtimeMs) return wikiAllImagesIndexCache;
    const raw = JSON.parse(fs.readFileSync(WIKI_ALL_IMAGES_INDEX_FILE, 'utf8'));
    wikiAllImagesIndexCache = raw && typeof raw === 'object' ? raw : { byStem: {}, byName: {} };
    if (!wikiAllImagesIndexCache.byStem || typeof wikiAllImagesIndexCache.byStem !== 'object') wikiAllImagesIndexCache.byStem = {};
    if (!wikiAllImagesIndexCache.byName || typeof wikiAllImagesIndexCache.byName !== 'object') wikiAllImagesIndexCache.byName = {};
    wikiAllImagesIndexMtimeMs = mtimeMs;
    return wikiAllImagesIndexCache;
  } catch (e) {
    log('WIKI', `wiki-allimages index load error: ${e.message}`);
    wikiAllImagesIndexCache = { byStem: {}, byName: {} };
    wikiAllImagesIndexMtimeMs = 0;
    return wikiAllImagesIndexCache;
  }
}

function toSafeFileName(value) {
  return String(value || '').toLowerCase().replace(/[^a-z0-9_-]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 120) || 'weapon';
}

function normalizeForMatch(value) {
  return String(value || '')
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/&amp;/g, '&')
    .replace(/[^a-z0-9а-яё]+/gi, '');
}

function decodeHtmlEntities(value) {
  return String(value || '')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

function getItemCategory(name) {
  const n = String(name || '').toLowerCase();
  if (n.includes('box')||n.includes('case')||n.includes('container')||n.includes('caixa')||n.startsWith('key_')) return 'box';
  if (['ar','smg','shg','sr','mg','hmg','pt','kn'].some(x=>n.startsWith(x))||n.includes('gl')) return 'weapon';
  if (n.includes('helmet')||n.includes('vest')||n.includes('hands')||n.includes('shoes')) return 'armor';
  if (n.includes('skin')||n.includes('camo')||n.includes('fbs')||n.includes('set12')||n.includes('bra')||n.includes('cartel')||n.includes('carbon')) return 'skin';
  if (n.includes('booster')||n.includes('consum')||n.includes('voucher')||n.includes('xp_')||n.includes('vp_')||n.includes('crown_')||n.includes('credit')) return 'consumable';
  if (n.includes('bundle')||n.includes('kit')||n.includes('pack')) return 'bundle';
  return 'default';
}

function getCategoryFallbackImagePath(cat) {
  const candidates = cat === 'box'
    ? [
      '/img/weapons/wiki_all/icons_randombox_skins.png',
      '/img/weapons/wiki_all/randombox_skin05.png',
      '/img/weapons/_box.png'
    ]
    : [`/img/weapons/_${cat}.png`];

  for (const rel of candidates) {
    const abs = path.join(PUBLIC_DIR, rel.replace(/^\//, '').replace(/\//g, path.sep));
    if (fs.existsSync(abs)) return rel;
  }

  return '/img/weapons/_default.png';
}

function getLocalWeaponImagePath(itemKey) {
  const baseKey = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  const extensions = ['.png', '.jpg', '.jpeg', '.webp'];

  // Priority 1: real IDs mapped 1:1 to files in wiki_all.
  for (const ext of extensions) {
    const rel = `/img/weapons/wiki_all/${baseKey}${ext}`;
    const abs = path.join(PUBLIC_DIR, rel.replace(/^\//, '').replace(/\//g, path.sep));
    if (fs.existsSync(abs)) return rel;
  }

  // Priority 2: local curated folders.
  for (const ext of extensions) {
    const relWeapons = `/img/weapons/${baseKey}${ext}`;
    const absWeapons = path.join(PUBLIC_DIR, relWeapons.replace(/^\//, '').replace(/\//g, path.sep));
    if (fs.existsSync(absWeapons)) return relWeapons;

    const relWiki = `/img/weapons/wiki/${baseKey}${ext}`;
    const absWiki = path.join(PUBLIC_DIR, relWiki.replace(/^\//, '').replace(/\//g, path.sep));
    if (fs.existsSync(absWiki)) return relWiki;
  }

  const preloadedWikiImage = getPreloadedWikiImagePath(baseKey);
  if (preloadedWikiImage) return preloadedWikiImage;
  const cat = getItemCategory(itemKey);
  if (cat !== 'default') return getCategoryFallbackImagePath(cat);
  return '/img/weapons/_default.png';
}

const WIKI_USER_AGENT = 'WFDEV20-AdminPanel/1.0';
const WIKI_FILEPATH_URL_PREFIX = `${WIKI_BASE}/index.php/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:FilePath/`;
const WIKI_INVALID_IMAGE_TOKEN_RE = /(rankicon|class[a-z]*icon|challenge_|badge|strip|mark|banner|operator|icon|randombox|bundle)/i;
const wikiAllImagesPrefixCache = new Map();
const wikiFilePathCache = new Map();

function safeJsonParse(value) {
  try { return JSON.parse(value); } catch { return null; }
}

function requestRemote(urlString, options = {}) {
  return new Promise(resolve => {
    let parsedUrl;
    try {
      parsedUrl = new URL(urlString);
    } catch (e) {
      return resolve({
        ok: false,
        status: 0,
        headers: {},
        location: null,
        body: Buffer.alloc(0),
        text: '',
        url: urlString,
        error: e.message
      });
    }

    const client = parsedUrl.protocol === 'https:' ? https : http;
    const timeoutMs = Number.isFinite(options.timeoutMs) ? Number(options.timeoutMs) : 3500;
    const req = client.request({
      protocol: parsedUrl.protocol,
      hostname: parsedUrl.hostname,
      port: parsedUrl.port ? Number(parsedUrl.port) : (parsedUrl.protocol === 'https:' ? 443 : 80),
      path: `${parsedUrl.pathname}${parsedUrl.search}`,
      method: options.method || 'GET',
      headers: options.headers || {}
    }, res => {
      const chunks = [];
      res.on('data', chunk => chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk)));
      res.on('end', () => {
        const body = chunks.length ? Buffer.concat(chunks) : Buffer.alloc(0);
        const locationHeader = res.headers && res.headers.location
          ? (Array.isArray(res.headers.location) ? res.headers.location[0] : res.headers.location)
          : null;
        resolve({
          ok: (res.statusCode || 0) >= 200 && (res.statusCode || 0) < 300,
          status: res.statusCode || 0,
          headers: res.headers || {},
          location: locationHeader,
          body,
          text: body.toString('utf8'),
          url: urlString,
          error: null
        });
      });
    });

    req.on('error', e => {
      resolve({
        ok: false,
        status: 0,
        headers: {},
        location: null,
        body: Buffer.alloc(0),
        text: '',
        url: urlString,
        error: e.message
      });
    });

    req.setTimeout(timeoutMs, () => {
      try { req.destroy(new Error('timeout')); } catch {}
    });

    if (options.body) req.write(options.body);
    req.end();
  });
}

async function requestRemoteFollow(urlString, options = {}, maxRedirects = 5) {
  let current = String(urlString || '');
  let response = null;
  for (let i = 0; i <= maxRedirects; i++) {
    response = await requestRemote(current, options);
    if (!response) break;
    if ([301, 302, 303, 307, 308].includes(response.status) && response.location) {
      try {
        current = new URL(response.location, current).toString();
        continue;
      } catch {
        return Object.assign({}, response, { finalUrl: current });
      }
    }
    return Object.assign({}, response, { finalUrl: current });
  }
  return Object.assign({}, response || {
    ok: false,
    status: 0,
    headers: {},
    location: null,
    body: Buffer.alloc(0),
    text: '',
    url: current,
    error: 'too_many_redirects'
  }, { finalUrl: current });
}

function normalizeWikiImageUrl(imageUrl) {
  try {
    if (!imageUrl) return null;
    const parsed = new URL(imageUrl, WIKI_BASE);
    if (!/\/wiki\/images\//i.test(parsed.pathname)) return null;
    parsed.protocol = 'https:';
    if (parsed.hostname.toLowerCase() === 'wf.cdn.gmru.net') {
      parsed.hostname = 'ru.warface.com';
    }
    return parsed.toString();
  } catch {
    return null;
  }
}

function isLikelyWikiItemImageName(fileName) {
  const lower = String(fileName || '').toLowerCase();
  if (!lower) return false;
  if (!/\.(png|jpg|jpeg|webp)$/i.test(lower)) return false;
  if (WIKI_INVALID_IMAGE_TOKEN_RE.test(lower)) return false;
  return true;
}

function normalizeWikiImageName(fileName) {
  return String(fileName || '')
    .replace(/\.[a-z0-9]+$/i, '')
    .replace(/\s+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function selectBestPreloadedPath(candidate, stem) {
  const list = Array.isArray(candidate) ? candidate.filter(Boolean) : (candidate ? [candidate] : []);
  if (!list.length) return null;
  const cleanStem = normalizeItemKeyToken(stem);
  const score = value => {
    const base = path.basename(String(value || '')).toLowerCase();
    let s = 0;
    if (base === `${cleanStem}.png`) s += 300;
    else if (base === `${cleanStem}.jpg` || base === `${cleanStem}.jpeg`) s += 260;
    else if (base.startsWith(`${cleanStem}.`)) s += 220;
    else if (base.startsWith(`${cleanStem}_`)) s += 120;
    if (/\.(png)$/i.test(base)) s += 20;
    if (!/_\d+\./.test(base)) s += 30;
    if (/(_card|_randombox|challenge_|badge|strip|mark|icon)/i.test(base)) s -= 300;
    return s;
  };
  const ranked = list
    .map(v => ({ v, s: score(v) }))
    .sort((a, b) => b.s - a.s);
  return ranked[0] ? ranked[0].v : list[0];
}

function buildPreloadedImageCandidates(itemKey) {
  const out = [];
  const seen = new Set();
  const add = value => {
    const n = normalizeItemKeyToken(value);
    if (!n || seen.has(n)) return;
    seen.add(n);
    out.push(n);
  };

  const base = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  if (!base) return out;

  add(base);
  const addBoxVisualCandidates = (rawRest, options = {}) => {
    const rest = normalizeItemKeyToken(rawRest);
    if (!rest) return;
    if (options.crown) add(`${rest}randomboxcrown`);
    add(`${rest}randombox`);
    add(`randombox_${rest}`);
    add(`randombox_${rest.replace(/_/g, '')}`);

    const weaponTokens = Array.from(rest.matchAll(/(?:^|_)((?:ar|sr|smg|shg|mg|hmg|pt|kn)\d{1,5})(?=_|$)/gi), m => m[1]);
    weaponTokens.forEach(tokenRaw => {
      const token = normalizeItemKeyToken(tokenRaw);
      const decor = rest
        .replace(new RegExp(`(^|_)${token}(_|$)`, 'i'), '_')
        .replace(/^_+|_+$/g, '');
      add(`${token}randombox`);
      if (options.crown) add(`${token}randomboxcrown`);
      if (decor) {
        add(`${token}randombox${decor.replace(/_/g, '')}`);
        add(`${token}randombox_${decor}`);
      }
      add(token);
    });
    add(rest);
  };

  if (base.startsWith('box_crown_')) {
    const rest = base.slice('box_crown_'.length);
    addBoxVisualCandidates(rest, { crown: true });
  } else if (base.startsWith('box_')) {
    const rest = base.slice('box_'.length);
    addBoxVisualCandidates(rest);
  } else if (base.startsWith('random_box_')) {
    addBoxVisualCandidates(base.slice('random_box_'.length));
  }

  add(base.replace(/_shop$/, ''));
  add(base.replace(/_bundle$/, ''));
  add(base.replace(/_card$/, ''));
  add(base.replace(/_console$/, ''));
  add(base.replace(/_skin\d*$/, ''));
  add(base.replace(/skin$/, ''));
  add(base.replace(/_unique_perk(?:_\d+)?$/, ''));
  add(base.replace(/_mvt\d{2}[a-z0-9_]*$/, ''));
  add(base.replace(/_(bp|inx)\d{2}[a-z0-9_]*$/, ''));
  add(base.replace(/_(gp|sp|sc)_d(?:_[a-z0-9]+)?$/, ''));
  add(base.replace(/_(gp|sp|sc)_d$/, ''));
  add(base.replace(/_(gp|sp)_d(?:_[a-z0-9]+)?$/, ''));
  add(base.replace(/^ui_(weapon|weapons|armor|item)_/, ''));
  add(base.replace(/^ui_armor_(shared|soldier|medic|engineer|sniper|gunner)_/, '$1_'));
  add(base.replace(/^ui_/, ''));
  add(base.replace('_hands_', '_gloves_'));
  add(base.replace('_gloves_', '_hands_'));
  add(base.replace('_shoes_', '_boots_'));
  add(base.replace('_boots_', '_shoes_'));
  add(base.replace('_feet_', '_boots_'));
  add(base.replace('_boots_', '_feet_'));

  const firstTokenMatch = base.match(/^([a-z]{2,6}\d{1,5})/i);
  if (firstTokenMatch) add(firstTokenMatch[1]);
  const firstToken = base.split('_')[0];
  if (firstToken) add(firstToken);
  const codeToken = (firstToken || '').match(/^([a-z]{2,6})(\d{2,6})$/i);
  if (codeToken) {
    const prefix = codeToken[1].toLowerCase();
    const num = Number.parseInt(codeToken[2], 10);
    if (Number.isFinite(num)) {
      add(`${prefix}${num}`);
      add(`${prefix}${String(num).padStart(2, '0')}`);
      add(`${prefix}${String(num).padStart(3, '0')}`);
    }
  }

  return out;
}

function getPreloadedWikiImagePath(itemKey) {
  const baseKey = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  if (!baseKey) return null;
  const idx = loadWikiAllImagesIndex();
  const byStem = idx && idx.byStem ? idx.byStem : {};
  const byName = idx && idx.byName ? idx.byName : {};

  const candidates = buildPreloadedImageCandidates(baseKey);
  for (const cand of candidates) {
    const exactStem = byStem[cand];
    const selected = selectBestPreloadedPath(exactStem, cand);
    if (selected) return selected;
  }

  for (const cand of candidates) {
    const titleCase = cand
      .split('_')
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join('_');
    const exactName = byName[`${titleCase}.png`.toLowerCase()] || byName[`${cand}.png`.toLowerCase()];
    if (typeof exactName === 'string' && exactName) return exactName;
  }

  return null;
}

function toTitleCaseToken(token) {
  const value = String(token || '');
  if (!value) return value;
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function buildWikiImagePrefixCandidates(itemKey, displayName) {
  const base = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  const out = new Set();
  const add = raw => {
    const norm = normalizeWikiImageName(raw);
    if (!norm || norm.length < 2) return;
    out.add(norm);
  };

  if (base) add(base);
  const parts = base.split('_').filter(Boolean);
  if (parts.length) {
    const titleParts = parts.map(toTitleCaseToken);
    add(titleParts.join('_'));
    const firstUpper = [toTitleCaseToken(parts[0]), ...parts.slice(1)].join('_');
    add(firstUpper);
  }

  if (/^[a-z]{2,4}\d+/i.test(parts[0] || '')) {
    add(toTitleCaseToken(parts[0]));
  }

  const display = normalizeWikiImageName(String(displayName || '')
    .replace(/["'`]/g, '')
    .replace(/[()]/g, ' ')
    .replace(/[^\w\s-]+/g, ' '));
  if (display) {
    add(display);
    const displayTokens = display.split('_').filter(Boolean).map(toTitleCaseToken);
    if (displayTokens.length) add(displayTokens.join('_'));
  }

  return Array.from(out).slice(0, 12);
}

function scoreWikiImageCandidate(name, prefix, baseKey) {
  const normalizedName = normalizeWikiImageName(name).toLowerCase();
  const normalizedPrefix = normalizeWikiImageName(prefix).toLowerCase();
  const normalizedBase = normalizeWikiImageName(baseKey).toLowerCase();
  let score = 0;
  if (normalizedName === normalizedPrefix) score += 200;
  if (normalizedName === normalizedBase) score += 180;
  if (normalizedName.startsWith(`${normalizedPrefix}_`)) score += 80;
  if (normalizedName.startsWith(`${normalizedBase}_`)) score += 60;
  if (normalizedName.includes(normalizedBase)) score += 30;
  if (/\.(png)$/i.test(String(name || ''))) score += 10;
  score -= Math.max(0, normalizedName.length - normalizedPrefix.length);
  return score;
}

async function listWikiImagesByPrefix(prefix) {
  if (wikiAllImagesPrefixCache.has(prefix)) return wikiAllImagesPrefixCache.get(prefix);
  const apiUrl = `${WIKI_BASE}/api.php?action=query&list=allimages&aisort=name&ailimit=25&format=json&aiprefix=${encodeURIComponent(prefix)}`;
  try {
    const apiRes = await requestRemoteFollow(apiUrl, { headers: { 'User-Agent': WIKI_USER_AGENT } }, 2);
    if (!apiRes.ok) {
      wikiAllImagesPrefixCache.set(prefix, []);
      return [];
    }
    const apiJson = safeJsonParse(apiRes.text);
    if (!apiJson) {
      wikiAllImagesPrefixCache.set(prefix, []);
      return [];
    }
    const list = (apiJson && apiJson.query && Array.isArray(apiJson.query.allimages)) ? apiJson.query.allimages : [];
    const out = list
      .map(row => String((row && row.name) || '').trim())
      .filter(Boolean);
    wikiAllImagesPrefixCache.set(prefix, out);
    return out;
  } catch (e) {
    log('WIKI', `allimages error for "${prefix}": ${e.message}`);
    wikiAllImagesPrefixCache.set(prefix, []);
    return [];
  }
}

async function resolveWikiFilePathImageUrl(fileName) {
  const normalized = String(fileName || '').trim();
  if (!normalized) return null;
  if (wikiFilePathCache.has(normalized)) return wikiFilePathCache.get(normalized);

  const url = `${WIKI_FILEPATH_URL_PREFIX}${encodeURIComponent(normalized)}`;
  try {
    const res = await requestRemote(url, { headers: { 'User-Agent': WIKI_USER_AGENT }, timeoutMs: 12000 });
    if (![301, 302, 303, 307, 308].includes(res.status)) {
      wikiFilePathCache.set(normalized, null);
      return null;
    }
    const location = res.location;
    const normalizedLocation = normalizeWikiImageUrl(location);
    wikiFilePathCache.set(normalized, normalizedLocation || null);
    return normalizedLocation || null;
  } catch (e) {
    log('WIKI', `filepath resolve error for "${normalized}": ${e.message}`);
    wikiFilePathCache.set(normalized, null);
    return null;
  }
}

async function findWikiImageByItemKey(itemKey, displayName) {
  const baseKey = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  const prefixes = buildWikiImagePrefixCandidates(baseKey, displayName);
  if (!prefixes.length) return null;

  let best = null;
  for (const prefix of prefixes) {
    const names = await listWikiImagesByPrefix(prefix);
    if (!names.length) continue;
    for (const name of names) {
      if (!isLikelyWikiItemImageName(name)) continue;
      const score = scoreWikiImageCandidate(name, prefix, baseKey);
      if (!best || score > best.score) best = { name, prefix, score };
    }
    if (best && best.score >= 180) break;
  }

  if (!best) return null;
  const imageUrl = await resolveWikiFilePathImageUrl(best.name);
  if (!imageUrl) return null;

  return {
    imageUrl,
    fileName: best.name,
    pageUrl: `${WIKI_FILEPATH_URL_PREFIX}${encodeURIComponent(best.name)}`,
    score: best.score,
  };
}

function extractWikiImageFromHtml(html) {
  if (!html) return null;
  const candidates = [];
  const re = /<img[^>]+src="([^"]*\/wiki\/images\/[^"]+)"[^>]*>/gi;
  let m;
  while ((m = re.exec(html)) !== null) {
    const tag = m[0];
    const src = normalizeWikiImageUrl(m[1]);
    if (!src) continue;
    if (WIKI_INVALID_IMAGE_TOKEN_RE.test(src)) continue;
    const widthMatch = tag.match(/\bwidth="(\d+)"/i);
    const heightMatch = tag.match(/\bheight="(\d+)"/i);
    const width = widthMatch ? Number(widthMatch[1]) : 0;
    const height = heightMatch ? Number(heightMatch[1]) : 0;
    const score = (
      (width === 200 && height >= 40 && height <= 70 ? 100 : 0) +
      (width >= 180 && width <= 260 && height >= 30 && height <= 90 ? 60 : 0) +
      (width >= 700 && height >= 180 && height <= 320 ? 40 : 0) +
      (src.toLowerCase().endsWith('.png') ? 10 : 0)
    );
    candidates.push({ src, score });
  }
  if (!candidates.length) return null;
  candidates.sort((a, b) => b.score - a.score);
  return candidates[0].src;
}

async function findWikiWeaponEntry(displayName) {
  const q = asTrimmedString(displayName);
  if (!q) return null;
  try {
    const apiUrl = `${WIKI_BASE}/api.php?action=query&list=search&srlimit=5&format=json&srsearch=${encodeURIComponent(q)}`;
    const apiRes = await requestRemoteFollow(apiUrl, { headers: { 'User-Agent': WIKI_USER_AGENT } }, 2);
    if (!apiRes.ok) return null;
    const apiJson = safeJsonParse(apiRes.text);
    if (!apiJson) return null;
    const results = (apiJson && apiJson.query && Array.isArray(apiJson.query.search)) ? apiJson.query.search : [];
    if (!results.length) return null;

    const wanted = normalizeForMatch(q);
    let best = results[0];
    let bestScore = -1;
    for (const row of results) {
      const title = decodeHtmlEntities(row.title || '');
      const tnorm = normalizeForMatch(title);
      let score = 0;
      if (tnorm === wanted) score += 120;
      if (tnorm.includes(wanted) || wanted.includes(tnorm)) score += 60;
      if (row.snippet && normalizeForMatch(row.snippet).includes(wanted)) score += 20;
      score += Math.max(0, 10 - Math.min(10, Number(row.size || 0) / 4000));
      if (score > bestScore) {
        bestScore = score;
        best = row;
      }
    }

    const title = decodeHtmlEntities(best.title || '').trim();
    if (!title) return null;
    const pageUrl = `${WIKI_BASE}/index.php/${encodeURIComponent(title.replace(/\s+/g, '_'))}`;
    const pageRes = await requestRemoteFollow(pageUrl, { headers: { 'User-Agent': WIKI_USER_AGENT } }, 2);
    if (!pageRes.ok) return { title, pageUrl, imageUrl: null };
    const pageHtml = pageRes.text || '';
    const imageUrl = normalizeWikiImageUrl(extractWikiImageFromHtml(pageHtml));
    return { title, pageUrl, imageUrl };
  } catch (e) {
    log('WIKI', `search error for "${q}": ${e.message}`);
    return null;
  }
}

async function downloadWikiImage(itemKey, imageUrl) {
  try {
    const finalUrl = normalizeWikiImageUrl(imageUrl);
    if (!finalUrl) return null;
    if (!fs.existsSync(WEAPON_MEDIA_DIR)) fs.mkdirSync(WEAPON_MEDIA_DIR, { recursive: true });
    const ext = path.extname(new URL(finalUrl).pathname) || '.png';
    const fileName = `${toSafeFileName(itemKey)}${ext.toLowerCase()}`;
    const absPath = path.join(WEAPON_MEDIA_DIR, fileName);
    if (!fs.existsSync(absPath)) {
      const imgRes = await requestRemoteFollow(finalUrl, { headers: { 'User-Agent': WIKI_USER_AGENT }, timeoutMs: 20000 }, 3);
      if (!imgRes.ok) return null;
      fs.writeFileSync(absPath, imgRes.body);
    }
    return `/img/weapons/wiki/${fileName}`;
  } catch (e) {
    log('WIKI', `image download error: ${e.message}`);
    return null;
  }
}

async function resolveWeaponVisual(itemKey, displayName, options = {}) {
  const baseKey = normalizeItemKeyToken(String(itemKey || '').replace(ITEM_VARIANT_SUFFIX_RE, ''));
  const localImage = getLocalWeaponImagePath(baseKey);
  const cache = loadWeaponMediaCache();
  const cacheKey = baseKey;

  const preloadedWikiImage = getPreloadedWikiImagePath(baseKey);
  if (preloadedWikiImage) {
    return {
      key: baseKey,
      displayName: displayName || baseKey,
      wikiName: displayName || baseKey,
      image: preloadedWikiImage,
      pageUrl: null,
      source: 'wiki-preloaded'
    };
  }

  if (localImage !== '/img/weapons/_default.png') {
    return {
      key: baseKey,
      displayName: displayName || baseKey,
      wikiName: null,
      image: localImage,
      pageUrl: null,
      source: 'local'
    };
  }

  const cached = cache[cacheKey];
  if (cached && cached.image && fs.existsSync(path.join(PUBLIC_DIR, cached.image.replace(/^\//, '').replace(/\//g, path.sep)))) {
    return {
      key: baseKey,
      displayName: displayName || baseKey,
      wikiName: cached.wikiName || null,
      image: cached.image,
      pageUrl: cached.pageUrl || null,
      source: 'wiki-cache'
    };
  }

  if (options.noNetwork) {
    return {
      key: baseKey,
      displayName: displayName || baseKey,
      wikiName: null,
      image: '/img/weapons/_default.png',
      pageUrl: null,
      source: 'fallback'
    };
  }

  let wikiName = null;
  let pageUrl = null;
  let imageUrl = null;
  let source = 'fallback';

  const keyMatch = await findWikiImageByItemKey(baseKey, displayName);
  if (keyMatch && keyMatch.imageUrl) {
    imageUrl = keyMatch.imageUrl;
    pageUrl = keyMatch.pageUrl || null;
    wikiName = displayName || baseKey;
    source = 'wiki-prefix';
  } else {
    const wikiSearch = await findWikiWeaponEntry(displayName || baseKey);
    if (wikiSearch && wikiSearch.imageUrl) {
      imageUrl = wikiSearch.imageUrl;
      pageUrl = wikiSearch.pageUrl || null;
      wikiName = wikiSearch.title || displayName || baseKey;
      source = 'wiki-search';
    }
  }

  if (!imageUrl) {
    return {
      key: baseKey,
      displayName: displayName || baseKey,
      wikiName: wikiName,
      image: '/img/weapons/_default.png',
      pageUrl: pageUrl,
      source: 'fallback'
    };
  }

  const downloaded = await downloadWikiImage(baseKey, imageUrl);
  if (!downloaded) {
    return {
      key: baseKey,
      displayName: displayName || baseKey,
      wikiName: wikiName || displayName || baseKey,
      image: '/img/weapons/_default.png',
      pageUrl: pageUrl,
      source: 'fallback'
    };
  }

  cache[cacheKey] = {
    image: downloaded,
    wikiName: wikiName || displayName || baseKey,
    pageUrl: pageUrl || null,
    imageUrl: imageUrl,
    updatedAt: new Date().toISOString()
  };
  saveWeaponMediaCache();

  return {
    key: baseKey,
    displayName: displayName || baseKey,
    wikiName: wikiName || displayName || baseKey,
    image: downloaded,
    pageUrl: pageUrl || null,
    source: source
  };
}

async function callApi(desc, url) {
  log('API', `${desc} -> ${url}`);
  const raw = await apiGet(url);
  if (raw === null) {
    log('API', `${desc} -> ERROR: connection failed`);
    return null;
  }
  log('API', `${desc} -> response: ${raw.substring(0, 200)}`);
  return raw;
}

function apiPostJson(desc, urlString, body) {
  return new Promise(resolve => {
    const payload = JSON.stringify(body || {});
    let url;
    try {
      url = new URL(urlString);
    } catch (e) {
      log('API', `${desc} invalid url: ${e.message}`);
      return resolve(null);
    }

    const req = http.request({
      hostname: url.hostname,
      port: url.port || 80,
      path: `${url.pathname}${url.search}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, res => {
      let b = '';
      res.on('data', c => b += c);
      res.on('end', () => {
        log('API', `${desc} -> ${res.statusCode || '?'} (${b.length} bytes)`);
        try { resolve(JSON.parse(b)); } catch { resolve({ raw: b }); }
      });
    });
    req.on('error', e => {
      log('API', `${desc} -> error: ${e.message}`);
      resolve(null);
    });
    req.setTimeout(3000, () => {
      log('API', `${desc} -> timeout`);
      req.destroy();
      resolve(null);
    });
    req.write(payload);
    req.end();
  });
}

function normalizeAccountLogin(login) {
  return String(login || '').trim().toLowerCase();
}

function readRegisteredAccounts() {
  try {
    if (!fs.existsSync(REGISTERED_ACCOUNTS_FILE)) return {};
    const data = JSON.parse(fs.readFileSync(REGISTERED_ACCOUNTS_FILE, 'utf8'));
    return data && typeof data === 'object' ? data : {};
  } catch (e) {
    log('ACCOUNT', `registered account store read error: ${e.message}`);
    return {};
  }
}

function writeRegisteredAccount(login, entry) {
  const accounts = readRegisteredAccounts();
  accounts[login] = entry;
  fs.writeFileSync(REGISTERED_ACCOUNTS_FILE, JSON.stringify(accounts, null, 2), 'utf8');
}

function validateLauncherAccount(login, password) {
  if (!/^[a-z][a-z0-9_-]{2,19}$/.test(login)) {
    return 'Usuario deve comecar com letra e usar 3 a 20 caracteres: letras, numeros, _ ou -';
  }
  if (password.length < 3 || password.length > 32 || !/^[A-Za-z0-9_.@-]+$/.test(password)) {
    return 'Senha deve ter 3 a 32 caracteres sem espacos';
  }
  if (password.toLowerCase() === login) {
    return 'Senha nao pode ser igual ao usuario';
  }
  if (login === 'masterserver' || login === 'dedicated') {
    return 'Usuario reservado';
  }
  return null;
}

function getRequestIp(req) {
  const forwarded = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
  return forwarded || req.socket.remoteAddress || 'unknown';
}

function allowRegisterAttempt(req) {
  const ip = getRequestIp(req);
  const now = Date.now();
  const windowMs = 10 * 60 * 1000;
  const maxAttempts = 8;
  const attempts = (registerAttempts.get(ip) || []).filter(t => now - t < windowMs);
  if (attempts.length >= maxAttempts) {
    registerAttempts.set(ip, attempts);
    return false;
  }
  attempts.push(now);
  registerAttempts.set(ip, attempts);
  return true;
}

async function getNextAccountId(db) {
  let maxId = 2;
  const registered = readRegisteredAccounts();
  Object.keys(registered).forEach(login => {
    const id = Number(registered[login] && registered[login].id);
    if (Number.isSafeInteger(id) && id > maxId) maxId = id;
  });

  const accounts = await db.collection('accounts').find({}, { projection: { _id: 1 } }).toArray();
  accounts.forEach(a => {
    const id = Number(a && a._id);
    if (Number.isSafeInteger(id) && id > maxId) maxId = id;
  });

  const profiles = await db.collection('profiles').find({}, { projection: { username: 1 } }).toArray();
  profiles.forEach(p => {
    const id = Number(p && p.username);
    if (Number.isSafeInteger(id) && id > maxId) maxId = id;
  });

  return maxId + 1;
}

const LAUNCHER_PROFILE_MISSIONS = Object.freeze([
  'trainingmission',
  'easymission',
  'normalmission',
  'hardmission',
  'survivalmission',
  'campaignsections',
  'campaignsection1',
  'campaignsection2',
  'campaignsection3',
  'volcanoeasy',
  'volcanonormal',
  'volcanohard',
  'zombieeasy',
  'zombienormal',
  'zombiehard',
  'anubiseasy',
  'anubisnormal',
  'anubishard',
  'anubiseasy2',
  'anubisnormal2',
  'anubishard2',
  'zombietowereasy',
  'zombietowernormal',
  'zombietowerhard',
  'icebreakereasy',
  'icebreakernormal',
  'icebreakerhard',
  'chernobyleasy',
  'chernobylnormal',
  'chernobylhard',
  'japaneasy',
  'japannormal',
  'japanhard',
  'marseasy',
  'marsnormal',
  'marshard',
  'pve_arena',
  'blackwood'
]);

const LAUNCHER_FALLBACK_STATS = Object.freeze([
  Object.freeze({ stat: 'player_online_time', Value: 0 }),
  Object.freeze({ stat: 'player_max_session_time', Value: 0 }),
  Object.freeze({ stat: 'player_gained_money', Value: 0 }),
  Object.freeze({ stat: 'player_damage', Value: 0 }),
  Object.freeze({ stat: 'player_max_damage', Value: 0 }),
  Object.freeze({ mode: 'PVE', stat: 'player_kills_ai', Value: 0 }),
  Object.freeze({ mode: 'PVE', stat: 'player_deaths', Value: 0 }),
  Object.freeze({ mode: 'PVP', stat: 'player_kills_player', Value: 0 }),
  Object.freeze({ mode: 'PVP', stat: 'player_deaths', Value: 0 }),
  Object.freeze({ class: 'Rifleman', item_type: '', stat: 'player_wpn_usage', Value: 0 }),
  Object.freeze({ class: 'Heavy', item_type: '', stat: 'player_wpn_usage', Value: 0 }),
  Object.freeze({ class: 'Recon', item_type: '', stat: 'player_wpn_usage', Value: 0 }),
  Object.freeze({ class: 'Engineer', item_type: '', stat: 'player_wpn_usage', Value: 0 }),
  Object.freeze({ class: 'Medic', item_type: '', stat: 'player_wpn_usage', Value: 0 })
]);

function cloneJson(value) {
  return JSON.parse(JSON.stringify(value));
}

function isDuplicateKeyError(error) {
  return !!(error && (error.code === 11000 || /duplicate key/i.test(String(error.message || error.errmsg || ''))));
}

function makeLauncherNick(login, accountId) {
  const cleaned = String(login || '').replace(/[^A-Za-z0-9_.-]/g, '');
  if (cleaned.length >= 4) return cleaned.slice(0, 16);
  return `Player${accountId}`.slice(0, 16);
}

async function getUniqueLauncherNick(db, login, accountId) {
  const base = makeLauncherNick(login, accountId);
  const existing = await db.collection('profiles').findOne({ nick: base }, { projection: { _id: 1, username: 1 } });
  if (!existing || String(existing.username) === String(accountId)) return base;

  const suffix = String(accountId);
  const trimmed = base.slice(0, Math.max(4, 16 - suffix.length));
  const withId = `${trimmed}${suffix}`.slice(0, 16);
  const existingWithId = await db.collection('profiles').findOne({ nick: withId }, { projection: { _id: 1, username: 1 } });
  if (!existingWithId) return withId;

  return `Player${accountId}`.slice(0, 16);
}

function loadLauncherDefaultItems() {
  if (launcherDefaultItemsCache) return cloneJson(launcherDefaultItemsCache);

  try {
    const ltx = require(path.join(MASTER_DIR, 'node_modules', 'ltx'));
    const defaultSlotsXml = fs.readFileSync(path.join(MASTER_DIR, 'gamedata', 'libs', 'config', 'default_slots.xml'));
    const defaultItemsXml = fs.readFileSync(path.join(MASTER_DIR, 'gamedata', 'libs', 'config', 'defaultitems.xml'));
    const defaultSlots = {};

    ltx.parse(defaultSlotsXml).getChildren('slot_def').forEach(slot => {
      defaultSlots[slot.attrs.name] = { id: Number(slot.attrs.id) };
    });

    const classIndexes = ['R', 'H', 'S', 'M', 'E'];
    const items = [];
    let itemId = 1;

    ltx.parse(defaultItemsXml).getChildren('item').forEach(item => {
      const itemName = item.attrs.name;
      const itemClasses = String(item.attrs.classes || '').split(';').filter(Boolean);
      const slotForClass = [0, 0, 0, 0, 0];
      let equipped = 0;

      itemClasses.forEach(classSlot => {
        const parts = classSlot.split(':');
        const classIndex = classIndexes.indexOf(parts[0]);
        const slotInfo = defaultSlots[parts[1]];
        if (classIndex === -1 || !slotInfo) return;
        equipped += (1 << classIndex);
        slotForClass[classIndex] = slotInfo.id;
      });

      const slot = (
        (slotForClass[0] & 0x3F) |
        (((slotForClass[1] & 0x3F) |
        (((((slotForClass[3] & 0x3F) | ((slotForClass[4] & 0x3F) << 6)) << 6) |
        (slotForClass[2] & 0x3F)) << 6)) << 6) |
        0x40000000
      );

      items.push({
        id: itemId++,
        name: itemName,
        attached_to: '0',
        config: 'dm=0;material=default',
        slot,
        equipped,
        default: 1
      });
    });

    launcherDefaultItemsCache = items;
    return cloneJson(launcherDefaultItemsCache);
  } catch (e) {
    log('ACCOUNT', `default item load failed: ${e.message}`);
    return null;
  }
}

function resetStats(stats) {
  const source = Array.isArray(stats) && stats.length ? stats : LAUNCHER_FALLBACK_STATS;
  return cloneJson(source).map(stat => ({ ...stat, Value: 0 }));
}

async function getNextProfileId(db) {
  const docs = await db.collection('profiles').find({}, { projection: { _id: 1 } }).sort({ _id: -1 }).limit(1).toArray();
  const maxId = Number(docs[0] && docs[0]._id) || 0;
  return maxId + 1;
}

async function buildLauncherProfileDoc(db, login, accountId, profileId) {
  const template = await db.collection('profiles').find({}, { projection: { _id: 0, username: 0, nick: 0 } }).sort({ _id: 1 }).limit(1).toArray();
  const source = template[0] || {};
  const nowSeconds = Math.round(Date.now() / 1000);
  const defaultItems = loadLauncherDefaultItems();
  const nick = await getUniqueLauncherNick(db, login, accountId);

  return {
    _id: profileId,
    username: String(accountId),
    gender: 'male',
    height: 1,
    fatness: 0,
    game_money: 100000,
    cry_money: 25000,
    crown_money: 25000,
    experience: 0,
    rank: 1,
    current_class: 0,
    banner_badge: 4294967295,
    banner_mark: 4294967295,
    banner_stripe: 4294967295,
    status: 9,
    location: '',
    nick,
    clan_name: '',
    head: source.head || 'default_head_13',
    items: defaultItems || cloneJson(source.items || []),
    expired_items: [],
    missions_unlocked: cloneJson(LAUNCHER_PROFILE_MISSIONS),
    tutorials_passed: [],
    classes_unlocked: [0, 1, 2, 3, 4],
    persistent_settings: {},
    achievements: [],
    is_starting_achievements_issued: false,
    stats: resetStats(source.stats),
    contracts: {
      rotation_id: 0,
      contract_name: '',
      current: 0,
      total: 0,
      rotation_time: 0,
      status: 0,
      is_available: 0
    },
    last_seen_date: 0,
    profile_performance: {},
    wpn_usage: {},
    login_bonus: {
      prvday: Math.floor((Date.now() + 10800000) / 86400000),
      reward: -1
    },
    first_win_of_day: {
      time: nowSeconds,
      modes: []
    },
    clan_points: 0,
    clan_role: 0,
    invite_date: 0,
    notifications: [],
    last_notification_id: 1,
    friends: [],
    remote_give: {
      items: [],
      achievements: []
    },
    authorization_events: [],
    mute: { time: 0, reason: '' }
  };
}

async function ensureLauncherProfileForAccount(db, login, accountId) {
  const numericId = Number(accountId);
  if (!Number.isSafeInteger(numericId) || numericId < 1) {
    throw new Error('ID da conta invalido para criar perfil');
  }

  const username = String(numericId);
  const existing = await db.collection('profiles').findOne(
    { username },
    { projection: { _id: 1, username: 1, nick: 1 } }
  );
  if (existing) return { created: false, profile: existing };

  for (let attempt = 0; attempt < 5; attempt++) {
    const profileId = await getNextProfileId(db);
    const doc = await buildLauncherProfileDoc(db, login, numericId, profileId);
    try {
      await db.collection('profiles').insertOne(doc);
      log('ACCOUNT', `created launcher profile ${doc.nick}/${doc._id} for account ${login}/${numericId}`);
      return { created: true, profile: { _id: doc._id, username: doc.username, nick: doc.nick } };
    } catch (e) {
      if (isDuplicateKeyError(e)) {
        const alreadyCreated = await db.collection('profiles').findOne(
          { username },
          { projection: { _id: 1, username: 1, nick: 1 } }
        );
        if (alreadyCreated) return { created: false, profile: alreadyCreated };
        if (/_id/i.test(String(e.message || e.errmsg || ''))) continue;
      }
      throw e;
    }
  }

  throw new Error('Nao foi possivel reservar ID de perfil para a conta');
}

async function registerLauncherAccount(input) {
  const login = normalizeAccountLogin(input.username || input.login || input.usuario);
  const password = String(input.password || input.senha || '');
  const validationError = validateLauncherAccount(login, password);
  if (validationError) return { success: false, error: validationError, status: 400 };

  const registered = readRegisteredAccounts();
  if (registered[login]) {
    return { success: false, error: 'Usuario ja existe', status: 409 };
  }

  const client = new MongoClient(MONGO_URL, { serverSelectionTimeoutMS: 3000, useUnifiedTopology: true });
  await client.connect();
  try {
    const db = client.db('warface');
    const existing = await db.collection('accounts').findOne({ login });
    if (existing) return { success: false, error: 'Usuario ja existe', status: 409 };

    const accountId = await getNextAccountId(db);
    const createdAt = new Date().toISOString();
    const accountDoc = {
      _id: accountId,
      login,
      password,
      source: 'launcher',
      createdAt
    };

    await db.collection('accounts').insertOne(accountDoc);
    writeRegisteredAccount(login, { id: accountId, password, createdAt });

    const xmppResult = await apiPostJson('registeraccount', `${XMPP_API}/registeraccount`, {
      login,
      id: accountId,
      password,
      createdAt
    });
    const activated = !!(xmppResult && xmppResult.code === 0);
    if (!activated) {
      log('ACCOUNT', `created ${login}/${accountId}, but XMPP activation failed`);
    } else {
      log('ACCOUNT', `created and activated ${login}/${accountId}`);
    }

    return {
      success: true,
      username: login,
      accountId,
      needsProfile: true,
      activated,
      message: activated
        ? 'Conta criada e liberada; escolha o nick dentro do jogo'
        : 'Conta criada; reinicie ou ligue o XMPP para liberar o login'
    };
  } finally {
    await client.close();
  }
}

// ─── Ban Cache ────────────────────────────────────────────────────────
const BAN_CACHE_PATH = path.join(ROOT, 'XmppServerTcp', 'bancache.json');

async function writeBanCache() {
  let client;
  try {
    client = new MongoClient(MONGO_URL, { serverSelectionTimeoutMS: 2000, useUnifiedTopology: true });
    await client.connect();
    const db = client.db('warface');
    const accounts = await db.collection('accounts').find({ ban: { $exists: true } }).toArray();
    const cache = {};
    accounts.forEach(a => {
      if (a.ban && a.ban.expires && a.ban.expires > Math.floor(Date.now() / 1000)) {
        cache[String(a._id)] = { expires: a.ban.expires, reason: a.ban.reason || 'Banido pelo painel' };
      }
    });
    fs.writeFileSync(BAN_CACHE_PATH, JSON.stringify(cache), 'utf8');
    log('BANCACHE', `wrote ${Object.keys(cache).length} bans to ${BAN_CACHE_PATH}`);
    // Notify XmppServer to refresh its in-memory cache
    try {
      const postData = JSON.stringify({ banned: cache });
      const req = http.request({ hostname: '127.0.0.1', port: 8080, method: 'POST', path: '/updatebancache', headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData) } });
      req.write(postData);
      req.end();
      log('BANCACHE', 'notified XmppServer');
    } catch (e) { log('BANCACHE', `notify failed: ${e.message}`); }
  } catch (e) {
    log('BANCACHE', `ERROR: ${e.message} — keeping existing cache file`);
  } finally {
    if (client) try { await client.close(); } catch {}
  }
}

async function writeIpBanCache() {
  let client;
  try {
    client = new MongoClient(MONGO_URL, { serverSelectionTimeoutMS: 2000, useUnifiedTopology: true });
    await client.connect();
    const db = client.db('warface');
    const bans = await db.collection('ip_bans').find({}).toArray();
    const cache = {};
    const now = Math.floor(Date.now() / 1000);
    bans.forEach(b => {
      if (b.expires > now) {
        cache[b.ip] = { expires: b.expires, reason: b.reason || 'IP banido pelo painel' };
      }
    });
    const cachePath = path.join(ROOT, 'XmppServerTcp', 'ipbancache.json');
    fs.writeFileSync(cachePath, JSON.stringify(cache), 'utf8');
    log('IPBANCACHE', `wrote ${Object.keys(cache).length} IP bans to ${cachePath}`);
    try {
      const postData = JSON.stringify({ banned: cache });
      const req = http.request({ hostname: '127.0.0.1', port: 8080, method: 'POST', path: '/updateipbancache', headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData) } });
      req.write(postData);
      req.end();
      log('IPBANCACHE', 'notified XmppServer');
    } catch (e) { log('IPBANCACHE', `notify failed: ${e.message}`); }
  } catch (e) {
    log('IPBANCACHE', `ERROR: ${e.message} — keeping existing cache file`);
  } finally {
    if (client) try { await client.close(); } catch {}
  }
}

async function withMongo(cb) {
  let client;
  try {
    log('MONGO', 'connecting...');
    client = new MongoClient(MONGO_URL, { serverSelectionTimeoutMS: 2000, useUnifiedTopology: true });
    await client.connect();
    log('MONGO', 'connected');
    const result = await cb(client.db('warface'), client);
    log('MONGO', 'query done');
    return result;
  } catch (e) {
    log('MONGO', `ERROR: ${e.message}`);
    throw e;
  } finally {
    if (client) try { await client.close(); log('MONGO', 'closed'); } catch {}
  }
}

function normalizeXpEventDoc(dynamicDoc) {
  const data = dynamicDoc && dynamicDoc.data && typeof dynamicDoc.data === 'object' ? dynamicDoc.data : {};
  const multiplierParsed = Number(data.multiplier);
  const multiplier = Number.isFinite(multiplierParsed) && multiplierParsed >= PANEL_MODEL.xp.multiplier.min && multiplierParsed <= PANEL_MODEL.xp.multiplier.max
    ? Math.round(multiplierParsed)
    : 1;
  const enabled = !!data.enabled && multiplier > 1;
  const info = asTrimmedString(data.info || '');
  const startedAt = Number(data.startedAt || 0) || 0;
  const expiresAt = Number(data.expiresAt || 0) || 0;
  const temporary = expiresAt > 0;
  const now = Date.now();
  const remainingMs = temporary ? Math.max(0, expiresAt - now) : null;
  const elapsedMs = startedAt > 0 ? Math.max(0, now - startedAt) : 0;
  return { enabled, multiplier, info, startedAt, expiresAt, temporary, remainingMs, elapsedMs };
}

async function disableXpEvent(db) {
  await db.collection('config').updateOne(
    { _id: 'xp_multiplier' },
    { $set: { multiplier: 1 } },
    { upsert: true }
  );
  await db.collection('cache').updateOne(
    { _id: 'dynamic_multipliers' },
    {
      $set: {
        data: {
          enabled: false,
          multiplier: 1,
          info: '',
          startedAt: null,
          expiresAt: null
        },
        hash: Date.now(),
        updatedAt: Date.now()
      }
    },
    { upsert: true }
  );
  try {
    await callApi('setxp', `${XMPP_API}/setxp?rate=1`);
  } catch (e) {
    log('XP', `disable notify error: ${e.message}`);
  }
}

async function autoDisableExpiredXpEvent() {
  try {
    await withMongo(async (db) => {
      const dynamicDoc = await db.collection('cache').findOne({ _id: 'dynamic_multipliers' });
      const state = normalizeXpEventDoc(dynamicDoc);
      if (!state.enabled || !state.temporary || !state.expiresAt) return;
      if (Date.now() < state.expiresAt) return;
      await disableXpEvent(db);
      log('XP', 'evento XP expirado e desativado automaticamente');
    });
  } catch (e) {
    log('XP', `auto-disable check error: ${e.message}`);
  }
}

function readJsonFileSafe(filePath, fallback) {
  try {
    if (!fs.existsSync(filePath)) return fallback;
    const raw = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return raw && typeof raw === 'object' ? raw : fallback;
  } catch {
    return fallback;
  }
}

function writeJsonFileSafe(filePath, data) {
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
}

let achievementCatalogCache = null;
let achievementCatalogCacheMtime = 0;
let achievementVisualCatalogCache = null;
let achievementVisualCatalogCacheMtime = 0;
let achievementNumericIconCache = null;
let achievementNumericIconCacheMtime = 0;

const ACH_UNLOCK_ITEMS_DIR = path.resolve(__dirname, '..', 'AssetInventory', 'extracted', 'GameData', 'items', 'shopitems');
const ACH_TEXT_FILE = path.join(MASTER_DIR, 'gamedata', 'languages', 'text_achievements.xml');
let achievementUnlockCatalogCache = null;
let achievementUnlockCatalogCacheMtime = 0;
let achievementTextMapCache = null;
let achievementTextMapMtime = 0;

function loadAchievementCatalogFromWikiIndex() {
  try {
    if (!fs.existsSync(WIKI_ALL_IMAGES_INDEX_FILE)) return [];
    const stat = fs.statSync(WIKI_ALL_IMAGES_INDEX_FILE);
    const mtime = Number(stat.mtimeMs || 0);
    if (achievementCatalogCache && achievementCatalogCacheMtime === mtime) return achievementCatalogCache;

    const raw = JSON.parse(fs.readFileSync(WIKI_ALL_IMAGES_INDEX_FILE, 'utf8'));
    const byName = raw && raw.byName ? raw.byName : {};
    const ids = new Set();
    Object.keys(byName).forEach(name => {
      const lower = String(name || '').toLowerCase();
      if (!lower.startsWith('challenge_') && !lower.startsWith('achievement_')) return;
      const match = lower.match(/(?:^|_)(\d{1,5})(?:\.[a-z0-9]+)$/i);
      if (!match) return;
      const parsed = parseStrictInt(match[1]);
      if (parsed === null || parsed < 1) return;
      ids.add(String(parsed));
    });
    const list = Array.from(ids).sort((a, b) => Number(a) - Number(b));
    achievementCatalogCache = list;
    achievementCatalogCacheMtime = mtime;
    return list;
  } catch {
    return [];
  }
}

function loadAchievementNumericEntriesFromWikiIndex() {
  try {
    if (!fs.existsSync(WIKI_ALL_IMAGES_INDEX_FILE)) return [];
    const stat = fs.statSync(WIKI_ALL_IMAGES_INDEX_FILE);
    const mtime = Number(stat.mtimeMs || 0);
    if (achievementNumericIconCache && achievementNumericIconCacheMtime === mtime) return achievementNumericIconCache;

    const raw = JSON.parse(fs.readFileSync(WIKI_ALL_IMAGES_INDEX_FILE, 'utf8'));
    const byName = raw && raw.byName ? raw.byName : {};
    const bestById = new Map();
    Object.keys(byName).forEach(fileName => {
      const lower = String(fileName || '').toLowerCase();
      if (!/^challenge_(badge|mark|strip)_/.test(lower)) return;
      const extMatch = lower.match(/\.(png|jpg|jpeg|webp)$/i);
      if (!extMatch) return;
      const stem = lower.replace(/\.(png|jpg|jpeg|webp)$/i, '');
      const idMatch = stem.match(/_(\d{1,5})$/);
      if (!idMatch) return;
      const id = String(parseStrictInt(idMatch[1]) || '');
      if (!id) return;

      const score = stem.startsWith('challenge_badge_') ? 100 : stem.startsWith('challenge_mark_') ? 80 : 60;
      const prev = bestById.get(id);
      if (!prev || score > prev.score) bestById.set(id, { id, icon: stem, score });
    });

    const out = Array.from(bestById.values())
      .sort((a, b) => Number(a.id) - Number(b.id))
      .map(x => ({ id: x.id, icon: x.icon, name: '', source: 'wiki_numeric_icon' }));

    achievementNumericIconCache = out;
    achievementNumericIconCacheMtime = mtime;
    return out;
  } catch {
    return [];
  }
}

function loadAchievementVisualCatalogFromWikiIndex() {
  try {
    if (!fs.existsSync(WIKI_ALL_IMAGES_INDEX_FILE)) return [];
    const stat = fs.statSync(WIKI_ALL_IMAGES_INDEX_FILE);
    const mtime = Number(stat.mtimeMs || 0);
    if (achievementVisualCatalogCache && achievementVisualCatalogCacheMtime === mtime) return achievementVisualCatalogCache;

    const raw = JSON.parse(fs.readFileSync(WIKI_ALL_IMAGES_INDEX_FILE, 'utf8'));
    const byName = raw && raw.byName ? raw.byName : {};
    const out = [];
    Object.keys(byName).forEach(fileName => {
      const lower = String(fileName || '').toLowerCase();
      if (!/^challenge_(badge|mark|strip)_/.test(lower)) return;
      if (!/\.(png|jpg|jpeg|webp)$/.test(lower)) return;
      const key = lower.replace(/\.(png|jpg|jpeg|webp)$/i, '');
      if (!key) return;
      out.push({ id: key, icon: key, name: '', source: 'wiki_visual' });
    });
    out.sort((a, b) => a.id.localeCompare(b.id));
    achievementVisualCatalogCache = out;
    achievementVisualCatalogCacheMtime = mtime;
    return out;
  } catch {
    return [];
  }
}

function loadAchievementCatalogFromUnlockItems() {
  try {
    if (!fs.existsSync(ACH_UNLOCK_ITEMS_DIR)) return [];
    const stat = fs.statSync(ACH_UNLOCK_ITEMS_DIR);
    const mtime = Number(stat.mtimeMs || 0);
    if (achievementUnlockCatalogCache && achievementUnlockCatalogCacheMtime === mtime) return achievementUnlockCatalogCache;

    const out = [];
    const entries = fs.readdirSync(ACH_UNLOCK_ITEMS_DIR, { withFileTypes: true });
    const textMap = loadAchievementTextMap();
    for (const entry of entries) {
      if (!entry.isFile()) continue;
      const file = String(entry.name || '');
      if (!/^achievement_unlock_.*\.xml$/i.test(file)) continue;
      const fileStem = file.replace(/\.xml$/i, '');
      let id = '';
      let icon = '';
      let name = '';
      try {
        const xml = fs.readFileSync(path.join(ACH_UNLOCK_ITEMS_DIR, file), 'utf8');
        const unlockMatch = xml.match(/<on_activate\s+[^>]*unlock_achievement="([^"]+)"/i);
        if (unlockMatch && unlockMatch[1]) {
          id = asTrimmedString(unlockMatch[1]);
        }
        if (!id) {
          const trailingNum = fileStem.match(/_(\d+)$/);
          if (trailingNum && trailingNum[1]) id = String(parseStrictInt(trailingNum[1]) || '');
        }
        if (!id) id = fileStem.toLowerCase();
        const iconMatch = xml.match(/<param\s+name="icon"\s+value="([^"]+)"\s*\/?>/i);
        if (iconMatch && iconMatch[1]) icon = asTrimmedString(iconMatch[1]);
        const nameMatch = xml.match(/<param\s+name="name"\s+value="([^"]+)"\s*\/?>/i);
        if (nameMatch && nameMatch[1]) {
          const token = asTrimmedString(nameMatch[1]).replace(/^@+/, '');
          name = textMap[token] || token;
        }
      } catch {}
      if (!id) continue;
      out.push({ id, icon, name, source: 'unlock_items' });
    }
    out.sort((a, b) => {
      const na = Number(a.id);
      const nb = Number(b.id);
      if (Number.isFinite(na) && Number.isFinite(nb)) return na - nb;
      if (Number.isFinite(na) && !Number.isFinite(nb)) return -1;
      if (!Number.isFinite(na) && Number.isFinite(nb)) return 1;
      return String(a.id).localeCompare(String(b.id));
    });
    achievementUnlockCatalogCache = out;
    achievementUnlockCatalogCacheMtime = mtime;
    return out;
  } catch {
    return [];
  }
}

function loadAchievementTextMap() {
  try {
    if (!fs.existsSync(ACH_TEXT_FILE)) return {};
    const stat = fs.statSync(ACH_TEXT_FILE);
    const mtime = Number(stat.mtimeMs || 0);
    if (achievementTextMapCache && achievementTextMapMtime === mtime) return achievementTextMapCache;
    const xml = fs.readFileSync(ACH_TEXT_FILE, 'utf8');
    const map = {};
    const entryRe = /<entry\s+key="([^"]+)"\s*>[\s\S]*?<original\s+value="([^"]*)"\s*\/>[\s\S]*?<\/entry>/gi;
    let m;
    while ((m = entryRe.exec(xml)) !== null) {
      const key = asTrimmedString(m[1]);
      const value = asTrimmedString(m[2]);
      if (key && value) map[key] = value;
    }
    achievementTextMapCache = map;
    achievementTextMapMtime = mtime;
    return map;
  } catch {
    return {};
  }
}

function makeSafeFileToken(value) {
  return asTrimmedString(value).replace(/[^A-Za-z0-9_.-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 80) || 'shop-change';
}

async function backupCurrentShopCache(db, reason) {
  const cache = await db.collection('cache').findOne({ _id: 'shop' });
  if (!cache || !Array.isArray(cache.data)) return null;
  const dir = path.join(__dirname, 'shop-backups');
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filePath = path.join(dir, `${timestamp}-${makeSafeFileToken(reason)}.json`);
  fs.writeFileSync(filePath, JSON.stringify({ reason, savedAt: new Date().toISOString(), cache }, null, 2), 'utf8');
  log('SHOP', `backup saved: ${filePath}`);
  return filePath;
}

function clampInt(n, min, max) {
  return Math.max(min, Math.min(max, n));
}

function getDefaultShopPackagesStore() {
  return { version: 1, packages: [] };
}

function makeShopPackageId(name) {
  const slug = normalizeItemKeyToken(name).replace(/_+/g, '-').slice(0, 40) || 'shop-package';
  return `${slug}-${Date.now()}`;
}

function loadShopPackagesStore() {
  const store = readJsonFileSafe(SHOP_PACKAGES_FILE, getDefaultShopPackagesStore());
  if (!Array.isArray(store.packages)) store.packages = [];
  store.packages = store.packages.filter(p => p && typeof p === 'object' && asTrimmedString(p.id));
  return store;
}

function saveShopPackagesStore(store) {
  const next = store && typeof store === 'object' ? store : getDefaultShopPackagesStore();
  if (!Array.isArray(next.packages)) next.packages = [];
  writeJsonFileSafe(SHOP_PACKAGES_FILE, next);
}

const SHOP_EXPIRATION_MAX_SECONDS = 315360000; // 10 years

function normalizeShopExpirationTime(rawValue) {
  const raw = asTrimmedString(rawValue).toLowerCase();
  if (!raw || raw === '0') return '';

  let amount = null;
  let unit = null;

  const shortMatch = raw.match(/^(\d+)\s*([dhm])$/i);
  if (shortMatch) {
    amount = parseStrictInt(shortMatch[1]);
    unit = shortMatch[2].toLowerCase();
  } else {
    const longMatch = raw.match(/^(\d+)\s*(day|days|hour|hours|month|months|dia|dias|hora|horas|mes|meses)$/i);
    if (!longMatch) return null;
    amount = parseStrictInt(longMatch[1]);
    const unitRaw = longMatch[2].toLowerCase();
    if (unitRaw === 'day' || unitRaw === 'days' || unitRaw === 'dia' || unitRaw === 'dias') unit = 'd';
    else if (unitRaw === 'hour' || unitRaw === 'hours' || unitRaw === 'hora' || unitRaw === 'horas') unit = 'h';
    else if (unitRaw === 'month' || unitRaw === 'months' || unitRaw === 'mes' || unitRaw === 'meses') unit = 'm';
  }

  if (amount === null || !Number.isSafeInteger(amount) || amount < 0) return null;
  if (amount === 0) return '';
  if (!unit) return null;

  let seconds = 0;
  if (unit === 'd') seconds = amount * 86400;
  else if (unit === 'h') seconds = amount * 3600;
  else if (unit === 'm') seconds = amount * 2419200;
  else return null;

  if (!Number.isSafeInteger(seconds) || seconds > SHOP_EXPIRATION_MAX_SECONDS) return null;
  return `${amount}${unit}`;
}

function sanitizeShopOfferEntry(raw, index, options = {}) {
  if (!raw || typeof raw !== 'object') return null;
  const allowMissingId = !!(options && options.allowMissingId);
  const warnings = options && Array.isArray(options.warnings) ? options.warnings : null;
  const idParsed = parseStrictInt(raw.id);
  let id = null;
  if (idParsed !== null) {
    id = idParsed;
  } else if (!allowMissingId) {
    id = index + 1;
  }
  if (id !== null && (!Number.isSafeInteger(id) || id < 1)) return null;
  const name = normalizeItemKeyToken(asTrimmedString(raw.name));
  if (!name || name.length < 2 || name.length > 120) return null;

  const priceMax = 2147483647;
  const quantityMax = 999999;
  const durabilityMax = 1000000;
  const rankMax = LIMITS.rank.max;

  const parseBounded = (v, min, max, fallback) => {
    const n = parseStrictInt(v);
    if (n === null) return fallback;
    if (n < min) return min;
    if (n > max) return max;
    return n;
  };

  const clean = Object.assign({}, raw);
  const game_price = parseBounded(raw.game_price, 0, priceMax, 0);
  const cry_price = parseBounded(raw.cry_price, 0, priceMax, 0);
  const crown_price = parseBounded(raw.crown_price, 0, priceMax, 0);
  const durabilityPoints = parseBounded(raw.durabilityPoints, 0, durabilityMax, 0);
  const quantity = parseBounded(raw.quantity, 0, quantityMax, 0);
  const offer_status = asTrimmedString(raw.offer_status) || 'NORMAL';

  clean.id = id;
  clean.name = name;
  clean.game_price = game_price;
  clean.cry_price = cry_price;
  clean.crown_price = crown_price;
  clean.offer_status = offer_status;
  clean.durabilityPoints = durabilityPoints;
  clean.quantity = quantity;

  clean.game_price_origin = parseBounded(raw.game_price_origin, 0, priceMax, game_price);
  clean.cry_price_origin = parseBounded(raw.cry_price_origin, 0, priceMax, cry_price);
  clean.crown_price_origin = parseBounded(raw.crown_price_origin, 0, priceMax, crown_price);
  clean.repair_cost = parseBounded(raw.repair_cost, 0, priceMax, 0);
  clean.supplier_id = parseBounded(raw.supplier_id, 0, 999999, 1);
  clean.rank = parseBounded(raw.rank, 0, rankMax, 0);
  clean.discount = parseBounded(raw.discount, 0, priceMax, 0);
  clean.sorting_index = parseBounded(raw.sorting_index, 0, priceMax, 0);

  ['key_item_name', 'item_category_override'].forEach(field => {
    clean[field] = asTrimmedString(raw[field]);
  });

  const rawExpirationTime = asTrimmedString(raw.expirationTime);
  const normalizedExpirationTime = normalizeShopExpirationTime(rawExpirationTime);
  if (normalizedExpirationTime === null) {
    clean.expirationTime = '';
    if (warnings) {
      warnings.push({
        id: clean.id,
        name: clean.name,
        rawExpirationTime
      });
    }
  } else {
    clean.expirationTime = normalizedExpirationTime;
  }

  const offerType = inferArsenalType(clean.name);
  const hasCrownPrice = Number(clean.crown_price || 0) > 0;
  // Equipment must always be permanent. Time-limited offers are only allowed for crown items.
  if (offerType === 'equipment' || !hasCrownPrice) {
    clean.expirationTime = '';
  }

  return clean;
}

function sanitizePackageName(raw) {
  const value = asTrimmedString(raw);
  if (!value) return null;
  if (value.length < 3 || value.length > 64) return null;
  if (/[\x00-\x1F\x7F]/.test(value)) return null;
  return value;
}

function sanitizePackageDescription(raw) {
  const value = asTrimmedString(raw);
  if (!value) return '';
  return value.slice(0, 280);
}

function summarizeShopPackage(pkg) {
  const filtered = filterShopOffersToGameItems(Array.isArray(pkg.offers) ? pkg.offers : []);
  return {
    id: asTrimmedString(pkg.id),
    name: asTrimmedString(pkg.name),
    description: asTrimmedString(pkg.description),
    offersCount: filtered.offers.length,
    removedInvalidCount: filtered.removed.length,
    createdAt: Number(pkg.createdAt) || Date.now(),
    updatedAt: Number(pkg.updatedAt) || Date.now()
  };
}

function sanitizePackageOffers(rawOffers, options = {}) {
  const offers = Array.isArray(rawOffers) ? rawOffers : [];
  const warnings = options && Array.isArray(options.warnings) ? options.warnings : null;
  const out = [];
  const seen = new Set();
  for (let i = 0; i < offers.length; i += 1) {
    const clean = sanitizeShopOfferEntry(offers[i], i, { allowMissingId: true, warnings });
    if (!clean) continue;
    const key = `${clean.id === null ? 'noid' : clean.id}|${clean.name}`;
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(clean);
  }
  const filtered = filterShopOffersToGameItems(out);
  return normalizeShopOfferOrderFields(filtered.offers);
}

function isShopOfferActiveStatus(rawStatus) {
  const status = asTrimmedString(rawStatus).toLowerCase();
  return !status || status === 'normal' || status === 'enabled' || status === 'active' || status === 'new' || status === 'hot';
}

function getShopOfferInt(offer, field, fallback = 0) {
  const parsed = parseStrictInt(offer && offer[field]);
  return parsed === null ? fallback : parsed;
}

function getShopOfferDisplayOrder(offer, fallbackIndex = 0) {
  const sortingIndex = getShopOfferInt(offer, 'sorting_index', 0);
  if (sortingIndex > 0) return sortingIndex;
  const id = getShopOfferInt(offer, 'id', 0);
  if (id > 0) return id;
  return fallbackIndex + 1;
}

function compareShopText(a, b) {
  return String(a || '').localeCompare(String(b || ''), 'en', { numeric: true, sensitivity: 'base' });
}

function sortShopOffersForPanel(offers, sortMode = 'position', sortDir = 'asc') {
  const modeRaw = asTrimmedString(sortMode).toLowerCase();
  const mode = ['position', 'id', 'name', 'status', 'price'].includes(modeRaw) ? modeRaw : 'position';
  const dir = asTrimmedString(sortDir).toLowerCase() === 'desc' ? -1 : 1;
  return (Array.isArray(offers) ? offers : [])
    .map((offer, index) => ({ offer, index }))
    .sort((a, b) => {
      let cmp = 0;
      if (mode === 'id') {
        cmp = getShopOfferInt(a.offer, 'id', 0) - getShopOfferInt(b.offer, 'id', 0);
      } else if (mode === 'name') {
        cmp = compareShopText(a.offer && a.offer.name, b.offer && b.offer.name);
      } else if (mode === 'status') {
        cmp = compareShopText(a.offer && a.offer.offer_status, b.offer && b.offer.offer_status);
      } else if (mode === 'price') {
        const aPrice = getShopOfferInt(a.offer, 'game_price', 0) + getShopOfferInt(a.offer, 'cry_price', 0) + getShopOfferInt(a.offer, 'crown_price', 0);
        const bPrice = getShopOfferInt(b.offer, 'game_price', 0) + getShopOfferInt(b.offer, 'cry_price', 0) + getShopOfferInt(b.offer, 'crown_price', 0);
        cmp = aPrice - bPrice;
      } else {
        cmp = getShopOfferDisplayOrder(a.offer, a.index) - getShopOfferDisplayOrder(b.offer, b.index);
      }
      if (cmp !== 0) return cmp * dir;

      return (getShopOfferDisplayOrder(a.offer, a.index) - getShopOfferDisplayOrder(b.offer, b.index)) ||
        (getShopOfferInt(a.offer, 'id', 0) - getShopOfferInt(b.offer, 'id', 0)) ||
        compareShopText(a.offer && a.offer.name, b.offer && b.offer.name) ||
        (a.index - b.index);
    })
    .map(row => row.offer);
}

function normalizeShopOfferOrderFields(offers) {
  return sortShopOffersForPanel(offers, 'position', 'asc').map((offer, index) => {
    const clean = Object.assign({}, offer);
    clean.sorting_index = index + 1;
    return clean;
  });
}

function getDefaultShopRotationConfig() {
  return {
    enabled: false,
    intervalMinutes: 60,
    packageIds: [],
    currentIndex: 0,
    nextRunAt: 0,
    lastAppliedAt: 0,
    lastAppliedPackageId: null,
    updatedAt: Date.now()
  };
}

function normalizeShopRotationConfig(raw, packagesStore) {
  const cfg = raw && typeof raw === 'object' ? Object.assign({}, raw) : getDefaultShopRotationConfig();
  const intervalParsed = parseStrictInt(cfg.intervalMinutes);
  cfg.intervalMinutes = intervalParsed === null ? 60 : clampInt(intervalParsed, 5, 10080);
  cfg.enabled = !!cfg.enabled;
  const packageIdsRaw = Array.isArray(cfg.packageIds) ? cfg.packageIds : [];
  const validIds = new Set((packagesStore && Array.isArray(packagesStore.packages) ? packagesStore.packages : []).map(p => asTrimmedString(p.id)).filter(Boolean));
  const packageIds = [];
  const seen = new Set();
  packageIdsRaw.forEach(idRaw => {
    const id = asTrimmedString(idRaw);
    if (!id || seen.has(id) || !validIds.has(id)) return;
    seen.add(id);
    packageIds.push(id);
  });
  cfg.packageIds = packageIds;
  const currentIndexParsed = parseStrictInt(cfg.currentIndex);
  cfg.currentIndex = currentIndexParsed === null || currentIndexParsed < 0 ? 0 : currentIndexParsed;
  const nextRunAtParsed = parseStrictInt(cfg.nextRunAt);
  cfg.nextRunAt = nextRunAtParsed === null || nextRunAtParsed < 0 ? 0 : nextRunAtParsed;
  const lastAppliedAtParsed = parseStrictInt(cfg.lastAppliedAt);
  cfg.lastAppliedAt = lastAppliedAtParsed === null || lastAppliedAtParsed < 0 ? 0 : lastAppliedAtParsed;
  cfg.lastAppliedPackageId = asTrimmedString(cfg.lastAppliedPackageId) || null;
  cfg.updatedAt = Date.now();
  return cfg;
}

function loadShopRotationConfig(packagesStore) {
  const store = packagesStore || loadShopPackagesStore();
  const cfg = readJsonFileSafe(SHOP_ROTATION_FILE, getDefaultShopRotationConfig());
  return normalizeShopRotationConfig(cfg, store);
}

function saveShopRotationConfig(cfg, packagesStore) {
  const store = packagesStore || loadShopPackagesStore();
  const normalized = normalizeShopRotationConfig(cfg, store);
  writeJsonFileSafe(SHOP_ROTATION_FILE, normalized);
  return normalized;
}

async function applyShopPackageById(packageId, mode = 'replace', regenerate = true, reason = 'manual') {
  const store = loadShopPackagesStore();
  const pkg = store.packages.find(p => asTrimmedString(p.id) === asTrimmedString(packageId));
  if (!pkg) throw new Error('Pacote de loja nao encontrado');
  const packageOffers = sanitizePackageOffers(pkg.offers);
  if (!packageOffers.length) throw new Error('Pacote sem ofertas validas');

  const applyMode = mode === 'merge' ? 'merge' : 'replace';
  let resultMeta = null;

    await withMongo(async db => {
    let cache = await db.collection('cache').findOne({ _id: 'shop' });
    if (!cache) {
      await db.collection('cache').insertOne({ _id: 'shop', data: [], hash: 0, updatedAt: Date.now() });
      cache = await db.collection('cache').findOne({ _id: 'shop' });
    }
    if (!Array.isArray(cache.data)) {
      await db.collection('cache').updateOne({ _id: 'shop' }, { $set: { data: [], hash: Math.floor(Date.now() / 1000), updatedAt: Date.now() } });
      cache.data = [];
    }
    await backupCurrentShopCache(db, `package-${reason}-${applyMode}`);
    const usedIds = new Set();
    let maxId = 0;
    (Array.isArray(cache.data) ? cache.data : []).forEach(offer => {
      const idNum = parseStrictInt(offer && offer.id);
      if (idNum === null || idNum < 1) return;
      if (applyMode === 'merge') usedIds.add(idNum);
      if (idNum > maxId) maxId = idNum;
    });
    const allocNextId = () => {
      do { maxId += 1; } while (usedIds.has(maxId));
      usedIds.add(maxId);
      return maxId;
    };

    let dataOut = [];

    if (applyMode === 'replace') {
      dataOut = packageOffers.map(o => {
        const clone = Object.assign({}, o);
        const idNum = parseStrictInt(clone.id);
        if (idNum !== null && idNum > 0 && !usedIds.has(idNum)) {
          usedIds.add(idNum);
          if (idNum > maxId) maxId = idNum;
          clone.id = idNum;
        } else {
          clone.id = allocNextId();
        }
        return clone;
      });
    } else {
      dataOut = Array.isArray(cache.data) ? cache.data.map(o => Object.assign({}, o)) : [];
      const byId = new Map();
      const byName = new Map();
      dataOut.forEach((offer, idx) => {
        const idNum = parseStrictInt(offer.id);
        if (idNum !== null) byId.set(idNum, idx);
        byName.set(normalizeItemKeyToken(offer.name), idx);
      });
      packageOffers.forEach(offer => {
        const keyName = normalizeItemKeyToken(offer.name);
        const idNum = parseStrictInt(offer.id);
        let idx = -1;
        if (idNum !== null && byId.has(idNum)) idx = byId.get(idNum);
        else if (keyName && byName.has(keyName)) idx = byName.get(keyName);

        if (idx >= 0) {
          const merged = Object.assign({}, dataOut[idx], offer);
          const existingId = parseStrictInt(dataOut[idx] && dataOut[idx].id);
          if (existingId !== null && existingId > 0) merged.id = existingId;
          else if (idNum !== null && idNum > 0 && !usedIds.has(idNum)) {
            merged.id = idNum;
            usedIds.add(idNum);
            if (idNum > maxId) maxId = idNum;
          } else {
            merged.id = allocNextId();
          }
          dataOut[idx] = merged;
        } else {
          const next = Object.assign({}, offer);
          if (idNum !== null && idNum > 0 && !usedIds.has(idNum)) {
            next.id = idNum;
            usedIds.add(idNum);
            if (idNum > maxId) maxId = idNum;
          } else {
            next.id = allocNextId();
          }
          dataOut.push(next);
          const newIdx = dataOut.length - 1;
          const finalId = parseStrictInt(next.id);
          if (finalId !== null) byId.set(finalId, newIdx);
          if (keyName) byName.set(keyName, newIdx);
        }
      });
    }

    dataOut = normalizeShopOfferOrderFields(dataOut);

    await db.collection('cache').updateOne(
      { _id: 'shop' },
      {
        $set: {
          data: dataOut,
          hash: Math.floor(Date.now() / 1000),
          updatedAt: Date.now(),
          last_shop_package_id: pkg.id,
          last_shop_package_name: pkg.name
        }
      }
    );

    resultMeta = {
      packageId: pkg.id,
      packageName: pkg.name,
      offersCount: packageOffers.length,
      finalOffersCount: dataOut.length,
      mode: applyMode
    };
  });

  if (regenerate) {
    log('SHOPPKG', 'shop hash updated; masterserver will reload cache on next poll');
  }

  log('SHOPPKG', `applied package=${pkg.id} mode=${applyMode} reason=${reason}`);
  return resultMeta;
}

async function processShopRotationTick(forceRunNow = false) {
  if (shopRotationBusy) return false;
  shopRotationBusy = true;
  try {
    const packagesStore = loadShopPackagesStore();
    const cfg = loadShopRotationConfig(packagesStore);
    if (!cfg.enabled) return false;
    if (!cfg.packageIds.length) {
      cfg.enabled = false;
      saveShopRotationConfig(cfg, packagesStore);
      log('SHOPROT', 'disabled rotation because package list is empty');
      return false;
    }

    const now = Date.now();
    if (!forceRunNow && cfg.nextRunAt && now < cfg.nextRunAt) return false;

    const index = cfg.currentIndex % cfg.packageIds.length;
    const packageId = cfg.packageIds[index];
    await applyShopPackageById(packageId, 'merge', true, 'rotation');

    cfg.lastAppliedAt = now;
    cfg.lastAppliedPackageId = packageId;
    cfg.currentIndex = (index + 1) % cfg.packageIds.length;
    cfg.nextRunAt = now + cfg.intervalMinutes * 60 * 1000;
    saveShopRotationConfig(cfg, packagesStore);

    log('SHOPROT', `applied package ${packageId}, next run in ${cfg.intervalMinutes} min`);
    return true;
  } catch (e) {
    log('SHOPROT', `tick error: ${e.message}`);
    return false;
  } finally {
    shopRotationBusy = false;
  }
}

async function initShopCacheIfEmpty() {
  log('INIT', 'checking shop cache...');
  const store = loadShopPackagesStore();
  if (!store || !store.packages || !store.packages.length) {
    log('INIT', 'no shop packages found, skipping init');
    return;
  }
  await sleep(4000);
  try {
    await withMongo(async db => {
      const cache = await db.collection('cache').findOne({ _id: 'shop' });
      if (cache && Array.isArray(cache.data) && cache.data.length > 0) {
        log('INIT', `shop cache already has ${cache.data.length} offers, skipping init`);
        return;
      }
      log('INIT', 'shop cache is empty, applying first package...');
      const firstPkg = store.packages[0];
      await applyShopPackageById(firstPkg.id, 'replace', true, 'auto-init');
      log('INIT', `shop cache initialized with package: ${firstPkg.name}`);
    });
  } catch (e) {
    log('INIT', `shop init skipped: ${e.message}`);
  }
}

function startShopRotationScheduler() {
  if (shopRotationTimer) {
    clearInterval(shopRotationTimer);
    shopRotationTimer = null;
  }
  shopRotationTimer = setInterval(() => {
    processShopRotationTick(false).catch(e => log('SHOPROT', `interval error: ${e.message}`));
  }, SHOP_ROTATION_TICK_MS);
  setTimeout(() => {
    processShopRotationTick(false).catch(e => log('SHOPROT', `initial tick error: ${e.message}`));
  }, 8000);
}

let expCurveCache = null;
function getExpCurve() {
  if (expCurveCache) return expCurveCache;
  const curve = {};
  try {
    const xml = fs.readFileSync(EXP_CURVE_XML, 'utf8');
    const re = /<level(\d+)\s+exp="(\d+)"\s*\/>/g;
    let match;
    while ((match = re.exec(xml)) !== null) {
      curve[Number(match[1])] = Number(match[2]);
    }
  } catch (e) {
    log('EXP', `failed to read expcurve.xml: ${e.message}`);
  }
  expCurveCache = curve;
  return curve;
}

function getExpForRank(rank) {
  const curve = getExpCurve();
  return curve[rank] != null ? curve[rank] : rank * 10000;
}

function getMaxAllowedExp() {
  return getExpForRank(LIMITS.rank.max);
}

// ─── Stats History ─────────────────────────────────────────────────────
const statsHistory = [];
const STATS_HISTORY_MAX = 1440; // 24h at 1 per minute

function collectStats() {
  const cpus = os.cpus();
  const cpuAvg = cpus.reduce((s, c) => {
    const total = Object.values(c.times).reduce((a, b) => a + b, 0);
    const idle = c.times.idle;
    return s + (1 - idle / total);
  }, 0) / cpus.length * 100;
  const mem = {
    total: os.totalmem(),
    free: os.freemem(),
    used: os.totalmem() - os.freemem(),
  };
  const entry = { time: Date.now(), cpu: Math.round(cpuAvg * 10) / 10, mem, uptime: process.uptime() };
  statsHistory.push(entry);
  if (statsHistory.length > STATS_HISTORY_MAX) statsHistory.shift();
}

setInterval(collectStats, 60000);
setTimeout(collectStats, 5000);

// Player count history (collected async)
async function collectPlayerHistory() {
  try {
    const raw = await callApi('getonline', `${XMPP_API}/getonline`);
    if (raw) {
      const data = JSON.parse(raw);
      const online = data.online || 0;
      if (statsHistory.length > 0) statsHistory[statsHistory.length - 1].online = online;
    }
  } catch (e) {
    log('STATS', `player history error: ${e.message}`);
  }
}
setInterval(collectPlayerHistory, 60000);
setTimeout(collectPlayerHistory, 6000);

// ─── Routes ────────────────────────────────────────────────────────────
async function tryHandleDomainRoute(req, res, route) {
  for (const handler of domainRouteHandlers) {
    const handled = await handler(req, res, route);
    if (handled) return true;
  }
  return false;
}

function createDomainRouteHandlers() {
  const sharedContext = {
    json,
    parseBody,
    PANEL_MODEL,
    LIMITS,
    loadItemNames,
    parseStrictInt,
    asTrimmedString,
    normalizeItemKeyToken,
    ITEM_VARIANT_SUFFIX_RE,
    resolveWeaponVisual,
    log,
    asTrimmedString,
    parseStrictInt,
    clampInt,
    loadShopPackagesStore,
    saveShopPackagesStore,
    summarizeShopPackage,
    sanitizePackageName,
    sanitizePackageDescription,
    sanitizePackageOffers,
    normalizeShopExpirationTime,
    makeShopPackageId,
    loadShopRotationConfig,
    saveShopRotationConfig,
    normalizeShopRotationConfig,
    inferArsenalType,
    applyShopPackageById,
    processShopRotationTick,
    services,
    getServiceStatus,
    startService,
    stopService,
    restartService,
    startAllServices,
    stopAllServices,
    getSurvivalRewardConfig,
    applySurvivalRewardChanges,
    getDevSyncProgress,
    getLauncherPublishProgress,
    getGamePublishProgress,
    setLauncherPublishProgress,
    setGamePublishProgress,
    writeLauncherSourceDir,
    writeR2SourceDir,
    readR2Config,
    requireR2Config,
    GAME_DIR,
    fs,
    path,
    resetGamePublishProgress: () => {
      gamePublishProgress = createGamePublishProgress();
      return getGamePublishProgress();
    },
    normalizeVersionLabel,
    readLauncherManifest,
    normalizeBaseUrl,
    scanLauncherManifestFilesSync,
    scanLauncherManifestFiles,
    writeLauncherManifest,
    buildLauncherManifestUrl,
    getBaseUrl,
    readLauncherSourceDir,
    scanLauncherPublishFiles,
    readRemoteLauncherManifest,
    getDevPublishStateInfo,
    asTrimmedString,
    LAUNCHER_VERSION_FILE,
    normalizeR2Key,
    normalizePublicBaseForPrefix,
    r2ObjectKey,
    retryR2,
    r2PutJson,
    r2PutFile,
    r2GetJson,
    r2DeleteObject,
    r2HeadObject,
    mimeTypeByExt,
    getManifestDiff,
    runLauncherBuildIfNeeded,
    assertDevPublishReady,
    appendLauncherUpdateHistory,
    readLauncherUpdateHistory,
    setDevSyncProgress,
    syncLocalFolderFromManifest,
    writeDevPublishState,
    readGameManifest,
    writeGameManifest,
    readGameVersionData,
    nextVersionLabel,
    getPublicGameCdnBase,
    getPublicManifestUrl,
    readRemoteGameManifest,
    shouldSkipGamePublishPath,
    scanManifestFilesStream,
    copyPublishedGameFile,
    removePublishedGameFile,
    appendGameUpdateHistory,
    GAME_VERSION_FILE,
    GAME_REF_DIR,
    LAUNCHER_DATA_FILE,
    LAUNCHER_MANIFEST_FILE,
    openFolderDialog,
    openGameFolderDialog,
    withMongo,
    normalizePatchEntryPath,
    shouldSkipGamePublishPath,
    writePublishedGameFile,
    scanManifestFiles,
    GAME_CDN_DIR,
    callApi,
    XMPP_API,
    statsHistory,
    writeIpBanCache,
    validateNickInput,
    validateBoundedInt,
    validateFreeItemToken,
    normalizeItemName,
    parseStrictInt,
    asTrimmedString,
    validateAchievementId,
    loadAchievementCatalogFromWikiIndex,
    loadAchievementNumericEntriesFromWikiIndex,
    loadAchievementCatalogFromUnlockItems,
    loadAchievementVisualCatalogFromWikiIndex,
    PANEL_MODEL,
    writeBanCache,
    validateCommandName,
    validatePositiveDelta,
    getProfileInt,
    getMaxAllowedExp,
    getExpForRank,
    LIMITS,
    validateItemNameInput,
    filterShopOffersToGameItems,
    isShopOfferActiveStatus,
    sortShopOffersForPanel,
    backupCurrentShopCache,
    getCurrentShopOfferNameSet,
    buildArsenalCatalog,
    adminLogs,
    sseClients,
    getAcConfig,
    setAcFlag,
    callApi,
    withMongo,
    XMPP_API,
    disableXpEvent,
    normalizeXpEventDoc,
    validateBoundedInt,
    parseStrictInt,
    asTrimmedString,
    PANEL_MODEL,
    fs,
    path,
    MASTER_DIR,
    getDirSize,
    http,
    ROOT,
    broadcastTimers,
    normalizeVersionLabel,
    normalizeBaseUrl,
    getPublicGameCdnBase,
    readGameManifest,
    writeGameManifest,
    getPublicManifestUrl,
    GAME_VERSION_FILE,
    os,
    process,
    readRuntimeConfigPayload,
    writeRuntimeConfigPayload,
    buildRuntimeEnvelope,
    broadcastRuntimeEnvelope,
    getRuntimeClientCount: () => runtimeWsClients.size,
    RUNTIME_WS_PATH,
    RUNTIME_SCHEMA_VERSION
  };
  const registrars = [
    registerAuthRoutes,
    registerServicesRoutes,
    registerLauncherRoutes,
    registerShopRoutes,
    registerRewardsRoutes,
    registerPlayersRoutes,
    registerCdnRoutes
  ];
  const handlers = [];
  for (const registrar of registrars) {
    const out = registrar(sharedContext);
    if (Array.isArray(out)) handlers.push(...out);
  }
  return handlers;
}

async function router(req, res) {
  const url = new URL(req.url, 'http://localhost');
  const pathname = url.pathname;
  const method = req.method;

  log('REQ', `${method} ${pathname}`);

  // CORS
  if (req.method === 'OPTIONS') {
    log('CORS', 'OPTIONS preflight');
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE', 'Access-Control-Allow-Headers': 'Content-Type,X-Auth-Token' });
    return res.end();
  }

  // Static files (no auth)
  if (!pathname.startsWith('/api/')) {
    let filePath = path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname);
    serveStatic(res, filePath);
    return;
  }

  // Auth
  if (pathname === '/api/login') {
    const pwd = url.searchParams.get('password');
    log('LOGIN', `attempt with password: ${pwd ? '***' : '(empty)'}`);
    if (pwd !== ADMIN_PASSWORD) {
      log('LOGIN', 'FAIL: wrong password');
      return json(res, { success: false, error: 'Senha invalida' }, 401);
    }
    const token = String(tokenCounter++);
    tokens.set(token, Date.now());
    setTimeout(() => tokens.delete(token), 14400000);
    log('LOGIN', `SUCCESS: token=${token}`);
    return json(res, { success: true, token });
  }

  if (pathname === '/api/public/authenticate' && method === 'POST') {
    (async () => {
      const body = await parseBody(req);
      const login = normalizeAccountLogin(body.username || body.login || '');
      const password = String(body.password || '');

      if (!login || !password) {
        return json(res, { success: false, error: 'Usuario e senha obrigatorios' }, 400);
      }

      const registered = readRegisteredAccounts();
      const account = registered[login];

      if (!account || account.password !== password) {
        return json(res, { success: false, error: 'Usuario ou senha invalidos' }, 401);
      }

      log('ACCOUNT', `authenticated ${login}`);
      return json(res, {
        success: true,
        username: login,
        accountId: account.id,
        activated: true
      });
    })().catch(e => {
      log('ACCOUNT', `authenticate error: ${e.message}`);
      json(res, { success: false, error: e.message }, 500);
    });
    return;
  }

  if (pathname === '/api/public/register' && method === 'POST') {
    if (!allowRegisterAttempt(req)) {
      return json(res, { success: false, error: 'Muitas tentativas. Aguarde alguns minutos.' }, 429);
    }
    (async () => {
      const body = await parseBody(req);
      const result = await registerLauncherAccount(body);
      const status = result.status || (result.success ? 200 : 400);
      delete result.status;
      json(res, result, status);
    })().catch(e => {
      log('ACCOUNT', `public register error: ${e.message}`);
      json(res, { success: false, error: e.message }, 500);
    });
    return;
  }

  if (pathname === '/api/public/serverinfo') {
    (async () => {
      try {
        const info = await getPublicServerInfoPayload();
        json(res, {
          success: true,
          server: info
        });
      } catch (e) {
        json(res, { success: false, error: e.message }, 500);
      }
    })();
    return;
  }

  if (pathname === '/api/public/launcher-config') {
    try {
      if (!fs.existsSync(LAUNCHER_DATA_FILE)) {
        return json(res, { success: true, config: { slides: [], news: [] } });
      }
      const data = JSON.parse(fs.readFileSync(LAUNCHER_DATA_FILE, 'utf8'));
      json(res, { success: true, config: data });
    } catch (e) {
      json(res, { success: false, error: e.message }, 500);
    }
    return;
  }

  if (pathname === '/api/public/launcher-version') {
    try {
      const data = readLauncherVersionData();
      const fallbackBase = normalizeBaseUrl(readLauncherManifest(req).base_url) || normalizeBaseUrl(readR2Config().publicBaseUrl) || getBaseUrl(req);
      if (!data.base_url) data.base_url = fallbackBase;
      if (!data.manifest_url) data.manifest_url = buildLauncherManifestUrl(data.base_url, req);
      data.update_mode = 'manifest';
      json(res, data);
    } catch (e) {
      const fallbackBase = normalizeBaseUrl(readR2Config().publicBaseUrl) || getBaseUrl(req);
      json(res, { version: '0.0.0', manifest_url: buildLauncherManifestUrl(fallbackBase, req), base_url: fallbackBase, required: true, notes: '', update_mode: 'manifest' });
    }
    return;
  }

  if (pathname === '/api/public/launcher-manifest') {
    try {
      json(res, readLauncherManifest(req));
    } catch (e) {
      const fallbackBase = normalizeBaseUrl(readR2Config().publicBaseUrl) || getBaseUrl(req);
      json(res, { version: '0.0.0', base_url: fallbackBase, required: true, files: [], error: e.message }, 500);
    }
    return;
  }

  if (pathname === '/api/public/launcher-updates' || pathname === '/api/public/launcher-update-history') {
    json(res, { success: true, updates: readLauncherUpdateHistory() });
    return;
  }

  if (pathname === '/api/public/game-version') {
    try {
      const data = readGameVersionData();
      if (!data.manifest_url) data.manifest_url = getPublicManifestUrl(req);
      data.update_mode = 'manifest';
      if (!data.base_url) data.base_url = readGameManifest(req).base_url;
      json(res, data);
    } catch (e) {
      json(res, { version: '0.0.0', manifest_url: getPublicManifestUrl(req), base_url: getPublicGameCdnBase(req), required: true, notes: '', update_mode: 'manifest' });
    }
    return;
  }

  if (pathname === '/api/public/game-manifest') {
    try {
      json(res, readGameManifest(req));
    } catch (e) {
      json(res, { version: '0.0.0', base_url: getPublicGameCdnBase(req), required: true, files: [], error: e.message }, 500);
    }
    return;
  }

  if (pathname === '/api/public/game-updates' || pathname === '/api/public/game-update-history') {
    json(res, { success: true, updates: readGameUpdateHistory() });
    return;
  }

  if (pathname === '/api/public/launcher-runtime-config') {
    try {
      json(res, { success: true, runtime: readRuntimeConfigPayload() });
    } catch (e) {
      json(res, { success: false, error: e.message }, 500);
    }
    return;
  }

  if (pathname === '/api/public/launcher-image' && method === 'POST') {
    (async () => {
      try {
        const body = await parseBody(req);
        const fileName = String(body.name || '').replace(/[^a-zA-Z0-9_-]/g, '') + '.png';
        const filePath = path.join(LAUNCHER_IMAGES_DIR, fileName);
        const buffer = Buffer.from(body.data, 'base64');
        fs.writeFileSync(filePath, buffer);
        log('LAUNCHER', `image saved: ${fileName}`);
        json(res, { success: true, fileName });
      } catch (e) {
        json(res, { success: false, error: e.message }, 500);
      }
    })();
    return;
  }

  // Public thumb endpoint (used in <img> tags, no auth)
  if (pathname === '/api/weapons/thumb' || pathname.startsWith('/api/weapons/thumb/')) {
    let imgName = asTrimmedString(url.searchParams.get('name') || '');
    if (!imgName && pathname.startsWith('/api/weapons/thumb/')) {
      imgName = asTrimmedString(decodeURIComponent(pathname.slice('/api/weapons/thumb/'.length)));
    }
    if (!imgName) { res.writeHead(302, { Location: '/img/weapons/_default.png' }); res.end(); return; }
    const baseKey = String(imgName).replace(ITEM_VARIANT_SUFFIX_RE, '');
    const localPath = getLocalWeaponImagePath(baseKey);
    res.writeHead(302, { Location: localPath }); res.end();
    return;
  }

  if (!auth(req)) {
    log('AUTH', 'BLOCKED (no valid token)');
    return json(res, { success: false, error: 'Nao autorizado' }, 401);
  }

  if (await tryHandleDomainRoute(req, res, { pathname, method })) {
    return;
  }

  log('ROUTE', `not found: ${pathname}`);
  return json(res, { success: false, error: 'Rota nao encontrada' }, 404);
}

function getDirSize(dirPath) {
  let total = 0;
  try {
    const items = fs.readdirSync(dirPath, { withFileTypes: true });
    for (const item of items) {
      const fullPath = path.join(dirPath, item.name);
      if (item.isDirectory()) total += getDirSize(fullPath);
      else total += fs.statSync(fullPath).size;
    }
  } catch {}
  return total;
}

// ─── Error Handling ─────────────────────────────────────────────────────
process.on('unhandledRejection', (err) => console.error('[AdminPanel] Unhandled Rejection:', err));
process.on('uncaughtException', (err) => console.error('[AdminPanel] Uncaught Exception:', err));

// ─── Start ─────────────────────────────────────────────────────────────
function startLegacyServer() {
  if (global.__ADMIN_PANEL_LEGACY_STARTED__) return null;
  global.__ADMIN_PANEL_LEGACY_STARTED__ = true;
  domainRouteHandlers = createDomainRouteHandlers();

  log('INIT', 'AdminPanel starting on port ' + PORT);
  log('INIT', 'MONGO_URL=' + MONGO_URL);
  log('INIT', 'XMPP_API=' + XMPP_API);
  log('INIT', 'ROOT=' + ROOT);
  log('INIT', Object.keys(services).length + ' services registered: ' + Object.keys(services).join(', '));

  if (!fs.existsSync(GAME_REF_DIR)) fs.mkdirSync(GAME_REF_DIR, { recursive: true });
  if (!fs.existsSync(GAME_CDN_DIR)) fs.mkdirSync(GAME_CDN_DIR, { recursive: true });
  if (!fs.existsSync(LAUNCHER_RUNTIME_CONFIG_FILE)) {
    try { writeRuntimeConfigPayload(getDefaultRuntimePayload()); } catch {}
  }

  const httpServer = http.createServer(router);
  const runtimeWss = new WebSocketServer({ noServer: true });

  runtimeWss.on('connection', ws => {
    runtimeWsClients.add(ws);
    try {
      const snapshot = buildRuntimeEnvelope('snapshot', readRuntimeConfigPayload());
      ws.send(JSON.stringify(snapshot));
    } catch {}

    ws.on('message', raw => {
      try {
        const data = JSON.parse(String(raw || '{}'));
        if (data && data.type === 'hello') {
          const snapshot = buildRuntimeEnvelope('snapshot', readRuntimeConfigPayload());
          ws.send(JSON.stringify(snapshot));
          broadcastServerStatusSnapshot();
        }
      } catch {}
    });

    ws.on('close', () => {
      runtimeWsClients.delete(ws);
    });

    ws.on('error', () => {
      try { ws.close(); } catch {}
    });
  });

  httpServer.on('upgrade', (req, socket, head) => {
    try {
      const parsed = new URL(req.url, 'http://localhost');
      if (parsed.pathname !== RUNTIME_WS_PATH) {
        socket.destroy();
        return;
      }
      runtimeWss.handleUpgrade(req, socket, head, ws => {
        runtimeWss.emit('connection', ws, req);
      });
    } catch {
      socket.destroy();
    }
  });

  httpServer.listen(PORT, () => {
    log('INIT', 'AdminPanel listening on http://localhost:' + PORT);
    log('INIT', 'Runtime WS listening on ws://localhost:' + PORT + RUNTIME_WS_PATH);
    disableClientMismatchChecks();
    startShopRotationScheduler();
    autoDisableExpiredXpEvent();
    setInterval(autoDisableExpiredXpEvent, 15000);
    writeBanCache();
    initShopCacheIfEmpty().catch(e => log('INIT', 'shop init error: ' + e.message));
  });

  if (serverStatusBroadcastTimer) clearInterval(serverStatusBroadcastTimer);
  serverStatusBroadcastTimer = setInterval(() => {
    broadcastServerStatusSnapshot();
  }, 5000);

  return { httpServer, runtimeWss };
}

module.exports = {
  router,
  startModernServer: startLegacyServer,
  startLegacyServer
};

if (require.main === module) {
  startLegacyServer();
}




