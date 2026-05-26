const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const { spawn } = require('child_process');
const net = require('net');
const https = require('https');
const http = require('http');
const crypto = require('crypto');
const packageInfo = require('./package.json');
const { parseVersionParts, compareVersions, withCacheBuster, isLocalNetworkHost, requireSafeRemoteUrl, sanitizeTempVersion } = require('./main/utils/version-url');
const { registerIpcHandlers } = require('./main/ipc/register-handlers');

// VERSION IDENTIFIER - Para verificar se está usando código atualizado
const LAUNCHER_VERSION = packageInfo.version || '1.0.0';
console.log('=======================================================');
console.log(`WARFACE LAUNCHER v${LAUNCHER_VERSION}`);
console.log('=======================================================');
console.log('[OK] Suporte para arquivos ZIP > 2GB via PowerShell');
console.log('[OK] Limite de download: 50GB');
console.log('[OK] Sistema de relatorios integrado');
console.log('=======================================================');

// Validate and load config
let config;
try {
  config = require('./config');
  
  // Validate config structure
  if (!config || typeof config !== 'object') {
    throw new Error('Config invalido');
  }
  
  if (!config.GAME_PATH || typeof config.GAME_PATH !== 'string') {
    throw new Error('GAME_PATH nao configurado');
  }
  
  if (!config.SERVER || typeof config.SERVER !== 'object') {
    throw new Error('SERVER nao configurado');
  }
  
  // Validate server config
  const requiredServerFields = ['host', 'ip', 'port'];
  for (const field of requiredServerFields) {
    if (!config.SERVER[field]) {
      throw new Error(`SERVER.${field} nao configurado`);
    }
  }
  
  // Validate port number
  const port = parseInt(config.SERVER.port, 10);
  if (isNaN(port) || port < 1 || port > 65535) {
    throw new Error('SERVER.port deve ser um numero entre 1 e 65535');
  }
  
  // SERVER.ip can be an IPv4/IPv6 address or a public tunnel hostname.
  const serverAddress = String(config.SERVER.ip || '').trim();
  const hostnameRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
  if (!net.isIP(serverAddress) && !hostnameRegex.test(serverAddress)) {
    throw new Error('SERVER.ip deve ser um IP ou hostname valido (ex: 192.168.1.1 ou servidor.playit.plus)');
  }
  
  // Freeze config to prevent runtime modifications
  Object.freeze(config);
  Object.freeze(config.SERVER);
  
} catch (error) {
  console.error('ERRO CRITICO: Falha ao carregar configuracao:', error.message);
  
  // Show error after app is ready
  app.whenReady().then(() => {
    dialog.showErrorBox('Erro de Configuracao', 
      `Nao foi possivel carregar o arquivo de configuracao:\n\n${error.message}\n\nVerifique o arquivo config.js`);
    app.quit();
    process.exit(1);
  });
  
  // Prevent further execution
  throw error;
}

// Disable GPU cache to avoid permission errors
app.commandLine.appendSwitch('disable-gpu-shader-disk-cache');
app.commandLine.appendSwitch('disable-gpu-program-cache');
app.commandLine.appendSwitch('disable-http-cache');
app.commandLine.appendSwitch('disable-software-rasterizer');

let mainWindow;
let accountWindow = null; // Track account creation window
let accountWindowMode = null; // Track whether account window is register or login
let currentDownload = null; // Track current download request
let downloadInterval = null; // Track update interval globally
const activeDownloadRequests = new Set(); // Track all in-flight download requests

function abortAllActiveDownloads() {
  for (const req of Array.from(activeDownloadRequests)) {
    try {
      if (req && typeof req.destroy === 'function') {
        req.destroy(new Error('Download cancelado'));
      } else if (req && typeof req.abort === 'function') {
        req.abort();
      }
    } catch {}
  }
  activeDownloadRequests.clear();
  currentDownload = null;
}

// Fixed game installation path
const GAME_PATH = config.GAME_PATH;
const GAME_EXE = path.join(GAME_PATH, 'Bin64', 'Game.exe');
const GAME_EXE_ROOT = path.join(GAME_PATH, 'Game.exe');
const GAME_MANIFEST_STATE = path.join(GAME_PATH, '.wf_manifest_state.json');
const LAUNCHER_DATA_DIR = path.join(GAME_PATH, '.wf_launcher');
const LAUNCHER_STATE_FILE = path.join(LAUNCHER_DATA_DIR, 'state.json');
const LAUNCHER_USER_DATA_DIR = path.join(LAUNCHER_DATA_DIR, 'electron_user_data');

try {
  fs.mkdirSync(LAUNCHER_USER_DATA_DIR, { recursive: true });
  app.setPath('userData', LAUNCHER_USER_DATA_DIR);
} catch (error) {
  console.warn('Nao foi possivel fixar userData do launcher:', error.message);
}

// Server/runtime config
const baseServerConfig = { ...config.SERVER };
const registerApiUrl = config.REGISTER_API || `http://${baseServerConfig.ip}:8081`;

// Update URLs
const GAME_VERSION_URL = config.GAME_VERSION_URL || '';
const GAME_MANIFEST_URL = config.GAME_MANIFEST_URL || '';
const LAUNCHER_CONFIG_URL = config.LAUNCHER_CONFIG_URL || '';
const LAUNCHER_VERSION_URL = config.LAUNCHER_VERSION_URL || '';
const LAUNCHER_MANIFEST_URL = config.LAUNCHER_MANIFEST_URL || '';
const DISCORD_INVITE_URL = String(config.DISCORD_INVITE_URL || 'https://discord.gg/YOUR_INVITE').trim();
const RUNTIME_CONFIG_WS_URL = String(config.RUNTIME_CONFIG_WS_URL || '').trim();
const RUNTIME_CONFIG_CHANNEL = String(config.RUNTIME_CONFIG_CHANNEL || 'stable').trim();
const RUNTIME_CONFIG_HMAC_SECRET = String(config.RUNTIME_CONFIG_HMAC_SECRET || '');
const RUNTIME_CONFIG_CACHE_FILE = path.join(LAUNCHER_DATA_DIR, 'runtime-config.json');

const runtimeState = {
  configVersion: '0',
  source: 'base',
  connected: false,
  server: { ...baseServerConfig },
  links: {
    discordInviteUrl: DISCORD_INVITE_URL
  },
  launcherUi: null,
  urls: {
    gameVersionUrl: GAME_VERSION_URL,
    gameManifestUrl: GAME_MANIFEST_URL,
    launcherConfigUrl: LAUNCHER_CONFIG_URL
  }
};

let runtimeSocket = null;
let runtimeReconnectTimer = null;
let runtimeReconnectAttempt = 0;

// Constants
const MAX_REDIRECTS = 5; // Prevent infinite redirect loops
const MAX_JSON_RESPONSE_BYTES = 2 * 1024 * 1024;
const MAX_LAUNCHER_UPDATE_BYTES = 512 * 1024 * 1024;
const SHA256_RE = /^[a-f0-9]{64}$/i;
const RUNTIME_SCHEMA_VERSION = 1;
const RUNTIME_MAX_CLOCK_SKEW_MS = 5 * 60 * 1000;
const RUNTIME_NONCE_TTL_MS = 10 * 60 * 1000;
const RUNTIME_MAX_SLIDES = 12;
const RUNTIME_MAX_NEWS = 30;
const RUNTIME_MAX_TEXT = 300;
const runtimeSeenNonces = new Map();

function secureEqualHex(a, b) {
  const ah = String(a || '').trim().toLowerCase();
  const bh = String(b || '').trim().toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(ah) || !/^[a-f0-9]{64}$/.test(bh)) return false;
  try {
    const ab = Buffer.from(ah, 'hex');
    const bb = Buffer.from(bh, 'hex');
    if (ab.length !== bb.length) return false;
    return crypto.timingSafeEqual(ab, bb);
  } catch {
    return false;
  }
}

function buildRuntimeSignBase(message) {
  const payloadText = JSON.stringify(message && message.payload && typeof message.payload === 'object' ? message.payload : {});
  return [
    String(message && message.type || ''),
    String(message && message.schemaVersion || ''),
    String(message && message.configVersion || ''),
    String(message && message.timestamp || ''),
    String(message && message.nonce || ''),
    payloadText
  ].join('|');
}

function verifyRuntimeMessageSignature(message) {
  if (!RUNTIME_CONFIG_HMAC_SECRET) return true;
  const received = String(message && message.signature || '');
  const base = buildRuntimeSignBase(message);
  const expected = crypto.createHmac('sha256', RUNTIME_CONFIG_HMAC_SECRET).update(base).digest('hex');
  return secureEqualHex(received, expected);
}

function parseRuntimeVersion(value) {
  const raw = String(value || '').trim();
  if (!raw) return { kind: 'none', raw };
  if (/^\d{1,20}$/.test(raw)) {
    try {
      return { kind: 'int', raw, int: BigInt(raw) };
    } catch {
      return { kind: 'text', raw };
    }
  }
  return { kind: 'text', raw };
}

function isRuntimeVersionNewer(nextVersion, currentVersion) {
  const n = parseRuntimeVersion(nextVersion);
  const c = parseRuntimeVersion(currentVersion);
  if (n.kind === 'none') return false;
  if (c.kind === 'none') return true;
  if (n.kind === 'int' && c.kind === 'int') return n.int > c.int;
  return n.raw > c.raw;
}

function getEffectiveServerConfig() {
  return runtimeState.server || baseServerConfig;
}

function getEffectiveDiscordInviteUrl() {
  return String((runtimeState.links && runtimeState.links.discordInviteUrl) || DISCORD_INVITE_URL || '').trim();
}

function getEffectiveLauncherConfigUrl() {
  return String((runtimeState.urls && runtimeState.urls.launcherConfigUrl) || LAUNCHER_CONFIG_URL || '').trim();
}

function getEffectiveGameVersionUrl() {
  return String((runtimeState.urls && runtimeState.urls.gameVersionUrl) || GAME_VERSION_URL || '').trim();
}

function getEffectiveGameManifestUrl() {
  return String((runtimeState.urls && runtimeState.urls.gameManifestUrl) || GAME_MANIFEST_URL || '').trim();
}

function getRuntimeConfigPublic() {
  return {
    configVersion: String(runtimeState.configVersion || '0'),
    source: String(runtimeState.source || 'base'),
    connected: runtimeState.connected === true,
    server: { ...getEffectiveServerConfig() },
    links: { discordInviteUrl: getEffectiveDiscordInviteUrl() },
    launcherUi: runtimeState.launcherUi && typeof runtimeState.launcherUi === 'object' ? runtimeState.launcherUi : null,
    urls: {
      gameVersionUrl: getEffectiveGameVersionUrl(),
      gameManifestUrl: getEffectiveGameManifestUrl(),
      launcherConfigUrl: getEffectiveLauncherConfigUrl()
    }
  };
}

function sanitizeRuntimeConfigPayload(raw) {
  const payload = raw && typeof raw === 'object' ? raw : {};
  const next = {};

  if (payload.server && typeof payload.server === 'object') {
    const candidate = { ...baseServerConfig, ...payload.server };
    const port = Number.parseInt(candidate.port, 10);
    if (candidate.host && String(candidate.host).trim() && candidate.ip && String(candidate.ip).trim() && Number.isFinite(port) && port >= 1 && port <= 65535) {
      next.server = {
        host: String(candidate.host).trim(),
        ip: String(candidate.ip).trim(),
        port,
        useTLS: candidate.useTLS !== false,
        useProtect: candidate.useProtect === true,
        checkCertificate: candidate.checkCertificate === true,
        disableHashValidation: candidate.disableHashValidation === true,
        disableAntiCheat: candidate.disableAntiCheat === true
      };
    }
  }

  if (payload.links && typeof payload.links === 'object') {
    const discordInviteUrl = String(payload.links.discordInviteUrl || '').trim().slice(0, 300);
    if (discordInviteUrl) {
      next.links = { discordInviteUrl };
    }
  }

  if (payload.launcherUi && typeof payload.launcherUi === 'object') {
    const ui = {};
    if (Array.isArray(payload.launcherUi.slides)) {
      ui.slides = payload.launcherUi.slides
        .slice(0, RUNTIME_MAX_SLIDES)
        .map(item => {
          const slide = item && typeof item === 'object' ? item : {};
          return {
            tag: String(slide.tag || '').slice(0, RUNTIME_MAX_TEXT),
            title: String(slide.title || '').slice(0, RUNTIME_MAX_TEXT),
            desc: String(slide.desc || '').slice(0, RUNTIME_MAX_TEXT),
            image: String(slide.image || '').slice(0, RUNTIME_MAX_TEXT)
          };
        });
    }
    if (Array.isArray(payload.launcherUi.news)) {
      ui.news = payload.launcherUi.news
        .slice(0, RUNTIME_MAX_NEWS)
        .map(item => {
          const news = item && typeof item === 'object' ? item : {};
          return {
            date: String(news.date || '').slice(0, 40),
            title: String(news.title || '').slice(0, RUNTIME_MAX_TEXT),
            badge: String(news.badge || '').slice(0, 20).toLowerCase(),
            featured: news.featured === true
          };
        });
    }
    if (Object.keys(ui).length) next.launcherUi = ui;
  }

  if (payload.urls && typeof payload.urls === 'object') {
    const urls = {};
    if (payload.urls.gameVersionUrl) urls.gameVersionUrl = String(payload.urls.gameVersionUrl).trim().slice(0, 400);
    if (payload.urls.gameManifestUrl) urls.gameManifestUrl = String(payload.urls.gameManifestUrl).trim().slice(0, 400);
    if (payload.urls.launcherConfigUrl) urls.launcherConfigUrl = String(payload.urls.launcherConfigUrl).trim().slice(0, 400);
    if (Object.keys(urls).length) next.urls = urls;
  }

  return next;
}

function cleanupRuntimeNonces() {
  const now = Date.now();
  for (const [nonce, createdAt] of runtimeSeenNonces.entries()) {
    if ((now - createdAt) > RUNTIME_NONCE_TTL_MS) {
      runtimeSeenNonces.delete(nonce);
    }
  }
}

function validateRuntimeEnvelope(message) {
  if (!message || typeof message !== 'object') return { ok: false, error: 'Mensagem invalida' };

  const type = String(message.type || '');
  if (type !== 'snapshot' && type !== 'patch') {
    return { ok: false, error: `Tipo nao suportado: ${type}` };
  }

  const schemaVersion = Number.parseInt(String(message.schemaVersion || ''), 10);
  if (!Number.isFinite(schemaVersion) || schemaVersion !== RUNTIME_SCHEMA_VERSION) {
    return { ok: false, error: `schemaVersion invalido: ${message.schemaVersion}` };
  }

  const configVersion = String(message.configVersion || '').trim();
  if (!configVersion || configVersion.length > 64) {
    return { ok: false, error: 'configVersion ausente/invalido' };
  }

  const timestamp = Number.parseInt(String(message.timestamp || ''), 10);
  if (!Number.isFinite(timestamp)) {
    return { ok: false, error: 'timestamp invalido' };
  }
  const skew = Math.abs(Date.now() - timestamp);
  if (skew > RUNTIME_MAX_CLOCK_SKEW_MS) {
    return { ok: false, error: 'timestamp fora da janela permitida' };
  }

  const nonce = String(message.nonce || '').trim();
  if (!/^[A-Za-z0-9._:-]{8,120}$/.test(nonce)) {
    return { ok: false, error: 'nonce invalido' };
  }

  cleanupRuntimeNonces();
  if (runtimeSeenNonces.has(nonce)) {
    return { ok: false, error: 'nonce repetido (replay)' };
  }

  const payload = message.payload;
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return { ok: false, error: 'payload invalido' };
  }

  if (!verifyRuntimeMessageSignature(message)) {
    return { ok: false, error: 'assinatura invalida' };
  }

  runtimeSeenNonces.set(nonce, Date.now());
  return {
    ok: true,
    envelope: {
      type,
      configVersion,
      payload
    }
  };
}

function applyRuntimeConfigPayload(payload, source = 'runtime') {
  if (source === 'ws') {
    const nextVersion = String(payload && payload.configVersion || '').trim();
    const currentVersion = String(runtimeState.configVersion || '').trim();
    if (nextVersion && currentVersion && !isRuntimeVersionNewer(nextVersion, currentVersion)) {
      return;
    }
  }

  const safe = sanitizeRuntimeConfigPayload(payload);
  if (safe.server) runtimeState.server = safe.server;
  if (safe.links) runtimeState.links = { ...runtimeState.links, ...safe.links };
  if (safe.urls) runtimeState.urls = { ...runtimeState.urls, ...safe.urls };
  if (safe.launcherUi) runtimeState.launcherUi = safe.launcherUi;
  if (payload && payload.configVersion) runtimeState.configVersion = String(payload.configVersion);
  runtimeState.source = source;

  try {
    fs.mkdirSync(path.dirname(RUNTIME_CONFIG_CACHE_FILE), { recursive: true });
    fs.writeFileSync(RUNTIME_CONFIG_CACHE_FILE, JSON.stringify({
      configVersion: runtimeState.configVersion,
      payload: {
        server: runtimeState.server,
        links: runtimeState.links,
        urls: runtimeState.urls,
        launcherUi: runtimeState.launcherUi
      }
    }, null, 2), 'utf8');
  } catch (error) {
    console.warn('Falha ao persistir runtime config:', error.message);
  }

  sendToRenderer('runtime-config-updated', getRuntimeConfigPublic());
}

function loadCachedRuntimeConfig() {
  try {
    if (!fs.existsSync(RUNTIME_CONFIG_CACHE_FILE)) return;
    const cached = JSON.parse(fs.readFileSync(RUNTIME_CONFIG_CACHE_FILE, 'utf8'));
    if (!cached || typeof cached !== 'object' || !cached.payload) return;
    applyRuntimeConfigPayload({ ...cached.payload, configVersion: cached.configVersion || '0' }, 'cache');
  } catch (error) {
    console.warn('Falha ao carregar runtime config em cache:', error.message);
  }
}

function fallbackToBaseRuntimeConfig(reason = 'ws-unavailable') {
  runtimeState.server = { ...baseServerConfig };
  runtimeState.links = { discordInviteUrl: DISCORD_INVITE_URL };
  runtimeState.urls = {
    gameVersionUrl: GAME_VERSION_URL,
    gameManifestUrl: GAME_MANIFEST_URL,
    launcherConfigUrl: LAUNCHER_CONFIG_URL
  };
  runtimeState.connected = false;
  runtimeState.source = reason;

  sendToRenderer('runtime-config-updated', getRuntimeConfigPublic());
  sendToRenderer('runtime-config-connection', { connected: false });
}

function scheduleRuntimeReconnect() {
  if (runtimeReconnectTimer) return;
  const baseMs = 2000;
  const maxMs = 30000;
  const delay = Math.min(maxMs, baseMs * Math.pow(2, Math.min(runtimeReconnectAttempt, 6)));
  const jitter = Math.floor(Math.random() * 500);
  runtimeReconnectTimer = setTimeout(() => {
    runtimeReconnectTimer = null;
    runtimeReconnectAttempt += 1;
    connectRuntimeConfigSocket();
  }, delay + jitter);
}

function connectRuntimeConfigSocket() {
  if (!RUNTIME_CONFIG_WS_URL) return;
  if (typeof WebSocket !== 'function') {
    console.warn('WebSocket indisponivel no processo principal');
    fallbackToBaseRuntimeConfig('ws-unavailable');
    return;
  }

  try {
    const ws = new WebSocket(RUNTIME_CONFIG_WS_URL);
    runtimeSocket = ws;

    ws.addEventListener('open', () => {
      runtimeReconnectAttempt = 0;
      runtimeState.connected = true;
      sendToRenderer('runtime-config-connection', { connected: true });
      ws.send(JSON.stringify({
        type: 'hello',
        schemaVersion: RUNTIME_SCHEMA_VERSION,
        product: 'warface-launcher',
        launcherVersion: LAUNCHER_VERSION,
        channel: RUNTIME_CONFIG_CHANNEL,
        gamePath: GAME_PATH
      }));
    });

    ws.addEventListener('message', event => {
      try {
        const data = JSON.parse(String(event.data || '{}'));
        if (data && data.type === 'server_status' && data.payload && typeof data.payload === 'object') {
          sendToRenderer('server-status-update', data.payload);
          return;
        }
        const validation = validateRuntimeEnvelope(data);
        if (!validation.ok) {
          console.warn('Mensagem WS rejeitada:', validation.error);
          return;
        }
        applyRuntimeConfigPayload({
          ...(validation.envelope.payload || {}),
          configVersion: validation.envelope.configVersion
        }, 'ws');
      } catch (error) {
        console.warn('Mensagem WS invalida:', error.message);
      }
    });

    ws.addEventListener('close', () => {
      if (runtimeSocket === ws) runtimeSocket = null;
      runtimeState.connected = false;
      fallbackToBaseRuntimeConfig('ws-closed');
      sendToRenderer('runtime-config-connection', { connected: false });
      scheduleRuntimeReconnect();
    });

    ws.addEventListener('error', () => {
      try { ws.close(); } catch {}
    });
  } catch (error) {
    console.warn('Falha ao conectar WS runtime config:', error.message);
    fallbackToBaseRuntimeConfig('ws-connect-failed');
    scheduleRuntimeReconnect();
  }
}

function sendToRenderer(channel, data) {
  if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
    try {
      mainWindow.webContents.send(channel, data);
    } catch (error) {
      console.error(`Error sending to renderer (${channel}):`, error);
    }
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 780,
    height: 600,
    minWidth: 780,
    minHeight: 600,
    maxWidth: 780,
    maxHeight: 600,
    frame: false,
    transparent: false,
    resizable: false,
    backgroundColor: '#060808',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
      spellcheck: false,
      preload: path.join(__dirname, 'preload-main.js')
    },
    icon: fs.existsSync(path.join(__dirname, 'icon.ico')) 
      ? path.join(__dirname, 'icon.ico') 
      : undefined,
    show: false // Don't show until ready
  });

  mainWindow.loadFile('index.html');
  mainWindow.webContents.once('did-finish-load', () => {
    checkAndApplyLauncherUpdate();
  });

  // Show window when ready
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    
    // Check game installation when window is shown
    sendToRenderer('check-installation');
    sendToRenderer('runtime-config-updated', getRuntimeConfigPublic());
    sendToRenderer('runtime-config-connection', { connected: runtimeState.connected === true });
  });
  
  // Re-check game installation when window gains focus
  mainWindow.on('focus', () => {
    console.log('Janela ganhou foco, verificando instalacao do jogo...');
    sendToRenderer('check-installation');
  });

  // Handle window closed
  mainWindow.on('closed', () => {
    // Cleanup any ongoing downloads
    if (currentDownload) {
      try {
        currentDownload.abort();
      } catch (e) {
        // Ignore
      }
      currentDownload = null;
    }
    
    if (downloadInterval) {
      clearInterval(downloadInterval);
      downloadInterval = null;
    }
    
    if (accountWindow && !accountWindow.isDestroyed()) {
      accountWindow.close();
    }
    
    mainWindow = null;
  });

  // Open DevTools in development
  // mainWindow.webContents.openDevTools();
}

app.whenReady().then(() => {
  loadCachedRuntimeConfig();
  connectRuntimeConfigSocket();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  // Cleanup downloads
  if (currentDownload) {
    try {
      currentDownload.abort();
    } catch (e) {
      // Ignore
    }
  }
  
  if (downloadInterval) {
    clearInterval(downloadInterval);
  }
  
  if (accountWindow && !accountWindow.isDestroyed()) {
    accountWindow.close();
  }
  
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

function httpGetJson(urlString) {
  return new Promise((resolve, reject) => {
    let url;
    try {
      url = requireSafeRemoteUrl(urlString, 'URL');
    } catch (error) {
      reject(error);
      return;
    }

    const transport = url.protocol === 'https:' ? https : http;
    const req = transport.request({
      hostname: url.hostname,
      port: url.port || (url.protocol === 'https:' ? 443 : 80),
      path: `${url.pathname}${url.search}`,
      method: 'GET',
      timeout: 10000
    }, res => {
      let data = '';
      let bytes = 0;
      res.on('data', chunk => {
        bytes += chunk.length;
        if (bytes > MAX_JSON_RESPONSE_BYTES) {
          req.destroy(new Error('Resposta JSON muito grande'));
          return;
        }
        data += chunk;
      });
      res.on('end', () => {
        try {
          if (res.statusCode >= 400) {
            reject(new Error(`HTTP ${res.statusCode}`));
            return;
          }
          const contentType = String(res.headers['content-type'] || '').toLowerCase();
          if (contentType && !contentType.includes('json') && !contentType.includes('text/plain')) {
            reject(new Error('Resposta JSON com tipo invalido'));
            return;
          }
          const parsed = JSON.parse(data || '{}');
          resolve(parsed);
        } catch (error) {
          reject(new Error('Resposta invalida'));
        }
      });
    });

    req.on('error', error => reject(new Error(`Falha ao conectar: ${error.message}`)));
    req.on('timeout', () => { req.destroy(); reject(new Error('Tempo esgotado')); });
    req.end();
  });
}

function normalizeManifestPath(value) {
  const raw = String(value || '').replace(/\\/g, '/').replace(/^[A-Za-z]:\//, '').replace(/^\/+/, '');
  if (!raw) return '';
  const normalized = path.posix.normalize(raw);
  if (!normalized || normalized === '.' || normalized.startsWith('../') || normalized.includes('/../')) return '';
  if (/[<>:"|?*\x00-\x1F]/.test(normalized)) return '';
  return normalized;
}

function resolveManifestFileUrl(baseUrl, relPath) {
  const safePath = normalizeManifestPath(relPath);
  if (!safePath) throw new Error(`Caminho invalido no manifest: ${relPath}`);
  const base = String(baseUrl || '').endsWith('/') ? String(baseUrl) : `${baseUrl}/`;
  return new URL(safePath.split('/').map(encodeURIComponent).join('/'), base).toString();
}

function localFileHash(filePath) {
  if (!fs.existsSync(filePath)) return null;
  const hash = crypto.createHash('sha256');
  hash.update(fs.readFileSync(filePath));
  return hash.digest('hex');
}

function readLocalManifestState() {
  try {
    if (!fs.existsSync(GAME_MANIFEST_STATE)) return { version: '0.0.0', files: [] };
    const data = JSON.parse(fs.readFileSync(GAME_MANIFEST_STATE, 'utf8'));
    return {
      version: String(data.version || '0.0.0'),
      files: Array.isArray(data.files) ? data.files : []
    };
  } catch {
    return { version: '0.0.0', files: [] };
  }
}

function writeLocalManifestState(manifest) {
  const files = Array.isArray(manifest.files)
    ? manifest.files.map(file => normalizeManifestPath(file.path)).filter(Boolean)
    : [];
  fs.mkdirSync(path.dirname(GAME_MANIFEST_STATE), { recursive: true });
  fs.writeFileSync(GAME_MANIFEST_STATE, JSON.stringify({
    version: manifest.version || '0.0.0',
    updatedAt: new Date().toISOString(),
    files
  }, null, 2), 'utf8');
}

function getValidGameExePath() {
  const candidates = [
    GAME_EXE,
    path.join(GAME_PATH, 'Bin32', 'Game.exe'),
    GAME_EXE_ROOT
  ];

  // Some installs keep the executable in custom "bin*" folders.
  try {
    if (fs.existsSync(GAME_PATH)) {
      const entries = fs.readdirSync(GAME_PATH, { withFileTypes: true });
      for (const entry of entries) {
        if (!entry || !entry.isDirectory()) continue;
        const dirName = String(entry.name || '').toLowerCase();
        if (!dirName.includes('bin')) continue;
        candidates.push(path.join(GAME_PATH, entry.name, 'Game.exe'));
      }
    }
  } catch {
    // Ignore directory scan errors and continue with known paths.
  }

  for (const exePath of candidates) {
    try {
      if (!fs.existsSync(exePath)) continue;
      const stats = fs.statSync(exePath);
      if (stats.size >= 100 * 1024) return exePath;
    } catch {
      // Try the next known executable location.
    }
  }
  return null;
}

function isGameInstallationValid() {
  return Boolean(getValidGameExePath());
}

function hasMissingTrackedFiles(files) {
  if (!Array.isArray(files) || files.length === 0) return false;
  for (const item of files) {
    const rel = normalizeManifestPath(item);
    if (!rel) continue;
    const localPath = path.resolve(GAME_PATH, ...rel.split('/'));
    if (!localPath.startsWith(GAME_PATH + path.sep)) continue;
    if (!fs.existsSync(localPath)) return true;
  }
  return false;
}

function sanitizeLauncherState(raw) {
  const state = raw && typeof raw === 'object' ? raw : {};
  const account = state.account && typeof state.account === 'object'
    ? {
        username: String(state.account.username || '').trim(),
        accountId: String(state.account.accountId || '').trim(),
        activated: state.account.activated !== false
      }
    : null;

  return {
    gameVersion: String(state.gameVersion || '0.0.0'),
    launcherVersion: String(state.launcherVersion || LAUNCHER_VERSION || '0.0.0'),
    gameDownloaded: state.gameDownloaded === true,
    hasCreatedAccount: state.hasCreatedAccount === true,
    account: account && account.username ? account : null
  };
}

function readLauncherState() {
  const gameInstalled = isGameInstallationValid();
  const manifestState = readLocalManifestState();
  const defaults = {
    gameVersion: String((manifestState && manifestState.version) || '0.0.0'),
    launcherVersion: LAUNCHER_VERSION || '0.0.0',
    gameDownloaded: gameInstalled,
    hasCreatedAccount: false,
    account: null
  };

  try {
    if (!fs.existsSync(LAUNCHER_STATE_FILE)) return defaults;
    const data = JSON.parse(fs.readFileSync(LAUNCHER_STATE_FILE, 'utf8'));
    const state = sanitizeLauncherState(data);
    if (state.account && !state.hasCreatedAccount) state.hasCreatedAccount = true;
    if (gameInstalled && !state.gameDownloaded) state.gameDownloaded = true;
    if (compareVersions(defaults.gameVersion, state.gameVersion) > 0) {
      state.gameVersion = defaults.gameVersion;
    }
    return state;
  } catch {
    return defaults;
  }
}

function writeLauncherState(patch = {}) {
  const current = readLauncherState();
  const next = sanitizeLauncherState({ ...current, ...patch });
  if (next.account && !next.hasCreatedAccount) next.hasCreatedAccount = true;
  fs.mkdirSync(path.dirname(LAUNCHER_STATE_FILE), { recursive: true });
  fs.writeFileSync(LAUNCHER_STATE_FILE, JSON.stringify(next, null, 2), 'utf8');
  return next;
}

function sanitizeLauncherStatePatch(rawPatch) {
  const patch = rawPatch && typeof rawPatch === 'object' ? rawPatch : {};
  const out = {};

  if (Object.prototype.hasOwnProperty.call(patch, 'gameVersion')) {
    out.gameVersion = String(patch.gameVersion || '0.0.0');
  }
  if (Object.prototype.hasOwnProperty.call(patch, 'launcherVersion')) {
    out.launcherVersion = String(patch.launcherVersion || LAUNCHER_VERSION || '0.0.0');
  }
  if (Object.prototype.hasOwnProperty.call(patch, 'gameDownloaded')) {
    out.gameDownloaded = patch.gameDownloaded === true;
  }
  if (Object.prototype.hasOwnProperty.call(patch, 'hasCreatedAccount')) {
    out.hasCreatedAccount = patch.hasCreatedAccount === true;
  }
  if (Object.prototype.hasOwnProperty.call(patch, 'account')) {
    out.account = patch.account;
  }

  return out;
}

function manifestFilesByPath(files) {
  const map = new Map();
  for (const file of Array.isArray(files) ? files : []) {
    const rel = normalizeManifestPath(file && file.path);
    if (rel) map.set(rel.toLowerCase(), { ...file, path: rel });
  }
  return map;
}

function getManifestDeltaFiles(manifest, key, fullFileMap) {
  if (!manifest || !Array.isArray(manifest[key])) return null;
  const result = [];
  const seen = new Set();
  for (const entry of manifest[key]) {
    const rawPath = typeof entry === 'string' ? entry : entry && entry.path;
    const rel = normalizeManifestPath(rawPath);
    if (!rel) continue;
    const lower = rel.toLowerCase();
    if (seen.has(lower)) continue;
    seen.add(lower);
    const fullFile = fullFileMap.get(lower) || {};
    result.push({ ...fullFile, ...(typeof entry === 'object' && entry ? entry : {}), path: rel });
  }
  return result;
}

function downloadFile(fileUrl, targetPath, onProgress, options = {}) {
  return new Promise((resolve, reject) => {
    let redirectCount = 0;
    let request = null;
    let stream = null;
    let downloaded = 0;
    let lastDownloaded = 0;
    let lastTime = Date.now();
    const tmpPath = `${targetPath}.download`;

    const cleanup = () => {
      if (request) activeDownloadRequests.delete(request);
      if (activeDownloadRequests.size === 0) currentDownload = null;
      try { if (stream && !stream.closed) stream.close(); } catch {}
      try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
    };

    const start = (urlString) => {
      if (redirectCount > MAX_REDIRECTS) {
        cleanup();
        reject(new Error('Muitos redirecionamentos'));
        return;
      }

      let url;
      try {
        url = requireSafeRemoteUrl(urlString, 'URL de download');
      } catch (error) {
        reject(error);
        return;
      }

      const transport = url.protocol === 'https:' ? https : http;
      fs.mkdirSync(path.dirname(targetPath), { recursive: true });
      stream = fs.createWriteStream(tmpPath);

      request = transport.get(url, (response) => {
        if ([301, 302, 307, 308].includes(response.statusCode)) {
          redirectCount++;
          let next = '';
          try {
            next = response.headers.location ? requireSafeRemoteUrl(new URL(response.headers.location, url).toString(), 'Redirect').toString() : '';
          } catch (error) {
            cleanup();
            reject(error);
            return;
          }
          try { stream.close(); } catch {}
          try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
          if (!next) {
            cleanup();
            reject(new Error('Redirect sem destino'));
            return;
          }
          start(next);
          return;
        }

        if (response.statusCode !== 200) {
          cleanup();
          reject(new Error(`HTTP ${response.statusCode}`));
          return;
        }

        const total = Number(response.headers['content-length']) || 0;
        const expectedSize = Number(options.expectedSize || 0);
        const maxBytes = Number(options.maxBytes || 0);
        if (maxBytes > 0 && total > maxBytes) {
          cleanup();
          reject(new Error('Arquivo de atualizacao maior que o limite permitido'));
          return;
        }
        if (expectedSize > 0 && total > 0 && total !== expectedSize) {
          cleanup();
          reject(new Error('Tamanho do arquivo diferente do manifest'));
          return;
        }
        response.on('data', chunk => {
          downloaded += chunk.length;
          if (maxBytes > 0 && downloaded > maxBytes) {
            cleanup();
            request.destroy(new Error('Arquivo de atualizacao maior que o limite permitido'));
            return;
          }
          const now = Date.now();
          const diffSeconds = Math.max(0.001, (now - lastTime) / 1000);
          const speed = (downloaded - lastDownloaded) / diffSeconds;
          lastDownloaded = downloaded;
          lastTime = now;
          if (onProgress) onProgress({ downloaded, total, speed });
        });
        response.pipe(stream);
      });

      request.on('error', error => {
        cleanup();
        reject(new Error(`Falha ao baixar arquivo: ${error.message}`));
      });
      request.on('abort', () => {
        cleanup();
        reject(new Error('Download cancelado'));
      });
      request.setTimeout(60000, () => {
        cleanup();
        request.destroy();
        reject(new Error('Tempo esgotado'));
      });
      activeDownloadRequests.add(request);
      currentDownload = { abort: abortAllActiveDownloads };

      stream.on('finish', () => {
        stream.close(() => {
          currentDownload = null;
          try {
            const expectedSize = Number(options.expectedSize || 0);
            if (expectedSize > 0 && downloaded !== expectedSize) {
              throw new Error('Download incompleto ou tamanho diferente do manifest');
            }
            if (fs.existsSync(targetPath)) fs.unlinkSync(targetPath);
            fs.renameSync(tmpPath, targetPath);
            resolve({ success: true, downloaded });
          } catch (error) {
            cleanup();
            reject(error);
          }
        });
      });
      stream.on('error', error => {
        cleanup();
        reject(new Error(`Falha ao gravar arquivo: ${error.message}`));
      });
    };

    start(fileUrl);
  });
}

function pickLauncherUpdateFile(manifest) {
  const files = Array.isArray(manifest && manifest.files) ? manifest.files : [];
  const currentName = path.basename(process.execPath || '').toLowerCase();
  return files.find(file => {
    const rel = normalizeManifestPath(file.path).toLowerCase();
    return rel === 'warfacesurvivorsetup.exe' || (rel.endsWith('.exe') && !rel.includes('/') && (rel.includes('setup') || rel.includes('installer')));
  })
    || files.find(file => normalizeManifestPath(file.path).toLowerCase() === 'warfacelauncher.exe')
    || files.find(file => normalizeManifestPath(file.path).toLowerCase() === currentName)
    || files.find(file => {
      const rel = normalizeManifestPath(file.path).toLowerCase();
      return rel.endsWith('.exe') && !rel.includes('/');
    });
}

function writeLauncherUpdaterScript(sourceExe, targetExe, installerMode = false) {
  const id = Date.now();
  const batchPath = path.join(app.getPath('temp'), `wf-launcher-update-${id}.bat`);
  const scriptPath = path.join(app.getPath('temp'), `wf-launcher-update-${id}.vbs`);
  const script = installerMode ? [
    '@echo off',
    'setlocal',
    `set "SETUP=${sourceExe}"`,
    `set "APP=${targetExe}"`,
    `set "VBS=${scriptPath}"`,
    'timeout /t 2 /nobreak >nul',
    '"%SETUP%" /S',
    'timeout /t 2 /nobreak >nul',
    'if exist "%APP%" start "" "%APP%"',
    'del "%SETUP%" >nul 2>nul',
    'del "%VBS%" >nul 2>nul',
    'del "%~f0" >nul 2>nul'
  ] : [
    '@echo off',
    'setlocal',
    `set "SRC=${sourceExe}"`,
    `set "DST=${targetExe}"`,
    `set "VBS=${scriptPath}"`,
    'timeout /t 2 /nobreak >nul',
    'for /l %%i in (1,1,60) do (',
    '  copy /Y "%SRC%" "%DST%" >nul 2>nul && goto copied',
    '  timeout /t 1 /nobreak >nul',
    ')',
    'exit /b 1',
    ':copied',
    'start "" "%DST%"',
    'del "%SRC%" >nul 2>nul',
    'del "%VBS%" >nul 2>nul',
    'del "%~f0" >nul 2>nul'
  ];
  fs.writeFileSync(batchPath, script.join('\r\n'), 'utf8');
  const escapedBatchPath = batchPath.replace(/"/g, '""');
  fs.writeFileSync(scriptPath, [
    'Set shell = CreateObject("WScript.Shell")',
    `shell.Run "cmd.exe /d /s /c """ & "${escapedBatchPath}" & """", 0, False`
  ].join('\r\n'), 'utf8');
  return scriptPath;
}

async function checkAndApplyLauncherUpdate() {
  if (!app.isPackaged || !LAUNCHER_VERSION_URL) return;

  try {
    const versionInfo = await httpGetJson(withCacheBuster(LAUNCHER_VERSION_URL));
    const remoteVersion = String(versionInfo && versionInfo.version || '0.0.0');
    if (!remoteVersion || remoteVersion === '0.0.0') throw new Error('Versao remota do launcher invalida');
    const state = readLauncherState();
    const packagedVersion = String(app.getVersion() || LAUNCHER_VERSION || '0.0.0');
    const storedVersion = String(state.launcherVersion || '0.0.0');
    const currentVersion = compareVersions(storedVersion, packagedVersion) > 0 ? storedVersion : packagedVersion;
    if (compareVersions(remoteVersion, currentVersion) <= 0) return;

    const manifestUrl = versionInfo.manifest_url || LAUNCHER_MANIFEST_URL;
    if (!manifestUrl) return;
    requireSafeRemoteUrl(manifestUrl, 'URL do manifest do launcher');
    const manifest = await httpGetJson(withCacheBuster(manifestUrl));
    if (!manifest || !Array.isArray(manifest.files)) throw new Error('Manifest do launcher invalido');
    if (String(manifest.version || '') !== remoteVersion) throw new Error('Versao do manifest nao confere com a versao remota');
    requireSafeRemoteUrl(manifest.base_url, 'URL base do launcher');

    const exeFile = pickLauncherUpdateFile(manifest);
    if (!exeFile || !exeFile.hash) throw new Error('Instalador do launcher nao encontrado no manifest do launcher');
    const expectedHash = String(exeFile.hash || '').toLowerCase();
    const expectedSize = Number(exeFile.size || 0);
    if (!SHA256_RE.test(expectedHash)) throw new Error('Hash do instalador invalido no manifest');
    if (!Number.isFinite(expectedSize) || expectedSize <= 0 || expectedSize > MAX_LAUNCHER_UPDATE_BYTES) {
      throw new Error('Tamanho do instalador invalido no manifest');
    }
    const installerMode = /setup|installer/i.test(path.basename(String(exeFile.path || '')));

    const safeVersion = sanitizeTempVersion(remoteVersion);
    const tempExe = path.join(app.getPath('temp'), installerMode ? `WarfaceSurvivorSetup-${safeVersion}.exe` : `WarfaceLauncher-${safeVersion}.exe`);
    const fileUrl = resolveManifestFileUrl(manifest.base_url, exeFile.path);
    sendToRenderer('launcher-update-start', {
      currentVersion,
      latestVersion: remoteVersion,
      file: exeFile.path,
      size: Number(exeFile.size || 0)
    });
    await downloadFile(fileUrl, tempExe, progress => {
      const total = Number(progress.total || exeFile.size || 0);
      const downloaded = Number(progress.downloaded || 0);
      sendToRenderer('launcher-update-progress', {
        currentVersion,
        latestVersion: remoteVersion,
        file: exeFile.path,
        downloaded,
        total,
        speed: Number(progress.speed || 0),
        progress: total > 0 ? Math.min(99, (downloaded / total) * 100) : 0
      });
    }, { expectedSize, maxBytes: MAX_LAUNCHER_UPDATE_BYTES });

    sendToRenderer('launcher-update-status', {
      currentVersion,
      latestVersion: remoteVersion,
      message: 'Validando atualizacao...'
    });
    const actualHash = localFileHash(tempExe);
    if (expectedHash && actualHash !== expectedHash) {
      throw new Error(`Hash invalido na atualizacao do launcher: ${actualHash || 'n/a'}`);
    }

    sendToRenderer('launcher-update-progress', {
      currentVersion,
      latestVersion: remoteVersion,
      file: exeFile.path,
      downloaded: Number(exeFile.size || 1),
      total: Number(exeFile.size || 1),
      speed: 0,
      progress: 100
    });
    sendToRenderer('launcher-update-status', {
      currentVersion,
      latestVersion: remoteVersion,
      message: installerMode
        ? 'Instalando atualizacao silenciosamente. O launcher sera reiniciado automaticamente...'
        : 'Aplicando atualizacao. O launcher sera reiniciado automaticamente...'
    });
    writeLauncherState({ launcherVersion: remoteVersion });

    const updaterScript = writeLauncherUpdaterScript(tempExe, process.execPath, installerMode);
    setTimeout(() => {
      spawn('wscript.exe', [updaterScript], { detached: true, stdio: 'ignore', windowsHide: true }).unref();
      app.quit();
    }, 900);
  } catch (error) {
    console.error('Launcher update failed:', error.message);
    sendToRenderer('launcher-update-error', { message: error.message || 'Falha ao atualizar o launcher' });
  }
}

async function syncGameManifest(manifestOrUrl, options = {}) {
  let manifest = manifestOrUrl;
  if (manifestOrUrl && typeof manifestOrUrl === 'object' && !Array.isArray(manifestOrUrl.files)) {
    options = { ...options, currentVersion: manifestOrUrl.currentVersion || manifestOrUrl.current_version || '' };
    manifest = manifestOrUrl.manifestUrl || manifestOrUrl.manifest_url || manifestOrUrl.url || GAME_MANIFEST_URL;
  }
    manifest = typeof manifest === 'string' ? await httpGetJson(manifest) : manifest;
  if (!manifest || !Array.isArray(manifest.files)) {
    throw new Error('Manifest invalido');
  }
  const baseUrl = manifest.base_url;
  if (!baseUrl) throw new Error('Manifest sem base_url');

  const localManifestState = readLocalManifestState();
  const canUseDeltaUpdate = isGameInstallationValid() && localManifestState.files.length > 0;
  const fullFileMap = manifestFilesByPath(manifest.files);
  const requestedCurrentVersion = String(options.currentVersion || '');
  const forceFullDownload = options.forceFullDownload === true;
  const missingTrackedFiles = canUseDeltaUpdate && hasMissingTrackedFiles(localManifestState.files);
  const isInitialInstall = forceFullDownload || missingTrackedFiles || !canUseDeltaUpdate || !requestedCurrentVersion || requestedCurrentVersion === '0.0.0';
  const deltaFiles = isInitialInstall ? null : getManifestDeltaFiles(manifest, 'changed_files', fullFileMap);
  const removedDeltaFiles = isInitialInstall ? [] : (getManifestDeltaFiles(manifest, 'removed_files', fullFileMap) || []);
  const changed = [];

  if (deltaFiles) {
    sendToRenderer('download-status', `BAIXANDO SOMENTE ALTERADOS: ${deltaFiles.length}`);
    for (const file of deltaFiles) {
      const rel = normalizeManifestPath(file.path);
      if (!rel || !file.hash) continue;
      const localPath = path.resolve(GAME_PATH, ...rel.split('/'));
      if (!localPath.startsWith(GAME_PATH + path.sep)) throw new Error(`Caminho fora do jogo: ${rel}`);
      changed.push({ ...file, path: rel, localPath });
    }
  } else if (isInitialInstall) {
    sendToRenderer('download-status', `BAIXANDO JOGO COMPLETO: ${manifest.files.length} arquivos`);
    for (const file of manifest.files) {
      const rel = normalizeManifestPath(file.path);
      if (!rel || !file.hash) continue;
      const localPath = path.resolve(GAME_PATH, ...rel.split('/'));
      if (!localPath.startsWith(GAME_PATH + path.sep)) throw new Error(`Caminho fora do jogo: ${rel}`);
      changed.push({ ...file, path: rel, localPath });
    }
  } else {
    sendToRenderer('download-status', 'COMPARANDO HASHES...');
    for (let i = 0; i < manifest.files.length; i++) {
      const file = manifest.files[i];
      const rel = normalizeManifestPath(file.path);
      if (!rel || !file.hash) continue;
      const localPath = path.resolve(GAME_PATH, ...rel.split('/'));
      if (!localPath.startsWith(GAME_PATH + path.sep)) throw new Error(`Caminho fora do jogo: ${rel}`);
      const stat = fs.existsSync(localPath) ? fs.statSync(localPath) : null;
      const sameSize = stat && Number(file.size) === stat.size;
      const sameHash = sameSize && localFileHash(localPath) === String(file.hash).toLowerCase();
      if (!sameHash) changed.push({ ...file, path: rel, localPath });

      if (i % 25 === 0) {
        sendToRenderer('download-status', `COMPARANDO HASHES ${i + 1}/${manifest.files.length}`);
      }
    }
  }

  const totalBytes = changed.reduce((sum, file) => sum + Number(file.size || 0), 0);
  const downloadedByIndex = new Array(changed.length).fill(0);
  const speedByIndex = new Array(changed.length).fill(0);
  sendToRenderer('download-progress', { progress: changed.length ? 0 : 100, downloaded: 0, total: totalBytes, speed: 0 });

  const emitAggregatedProgress = () => {
    const done = downloadedByIndex.reduce((sum, value) => sum + Number(value || 0), 0);
    const speed = speedByIndex.reduce((sum, value) => sum + Number(value || 0), 0);
    const progress = totalBytes > 0 ? Math.min(99, (done / totalBytes) * 100) : 100;
    sendToRenderer('download-progress', { progress, downloaded: done, total: totalBytes, speed });
  };

  let nextFileIndex = 0;
  const maxConcurrency = Math.min(10, Math.max(1, changed.length));
  const worker = async () => {
    while (true) {
      const i = nextFileIndex++;
      if (i >= changed.length) return;
      const file = changed[i];
      sendToRenderer('download-status', `BAIXANDO ${i + 1}/${changed.length}: ${file.path}`);
      const fileUrl = resolveManifestFileUrl(baseUrl, file.path);
      await downloadFile(fileUrl, file.localPath, ({ downloaded, speed }) => {
        downloadedByIndex[i] = downloaded;
        speedByIndex[i] = speed;
        emitAggregatedProgress();
      });

      const downloadedHash = localFileHash(file.localPath);
      const expectedHash = String(file.hash).toLowerCase();
      if (downloadedHash !== expectedHash) {
        const actualSize = fs.existsSync(file.localPath) ? fs.statSync(file.localPath).size : 0;
        const expectedSize = Number(file.size || 0);
        if (config.ALLOW_CDN_HASH_MISMATCH === true && expectedSize > 0 && actualSize === expectedSize) {
          sendToRenderer('download-status', `HASH DIFERENTE ACEITO PELO CDN: ${file.path}`);
        } else {
          throw new Error(`Hash invalido apos download: ${file.path} (esperado ${expectedHash}, recebido ${downloadedHash || 'n/a'}, tamanho ${actualSize}/${file.size || 0})`);
        }
      }

      downloadedByIndex[i] = Number(file.size || fs.statSync(file.localPath).size || downloadedByIndex[i] || 0);
      speedByIndex[i] = 0;
      emitAggregatedProgress();
    }
  };

  try {
    await Promise.all(Array.from({ length: maxConcurrency }, () => worker()));
  } catch (error) {
    abortAllActiveDownloads();
    throw error;
  }

  const previousState = localManifestState;
  if (removedDeltaFiles.length) {
    for (const file of removedDeltaFiles) {
      const rel = normalizeManifestPath(file.path);
      const localPath = path.resolve(GAME_PATH, ...rel.split('/'));
      if (rel && localPath.startsWith(GAME_PATH + path.sep) && fs.existsSync(localPath)) {
        try { fs.unlinkSync(localPath); } catch {}
      }
    }
    sendToRenderer('download-status', `REMOVIDOS ${removedDeltaFiles.length} ARQUIVOS OBSOLETOS`);
  } else if (!deltaFiles && previousState.files.length) {
    const nextFiles = new Set(manifest.files.map(file => normalizeManifestPath(file.path)).filter(Boolean).map(file => file.toLowerCase()));
    const removed = previousState.files
      .map(file => normalizeManifestPath(file))
      .filter(Boolean)
      .filter(file => !nextFiles.has(file.toLowerCase()));
    for (const rel of removed) {
      const localPath = path.resolve(GAME_PATH, ...rel.split('/'));
      if (localPath.startsWith(GAME_PATH + path.sep) && fs.existsSync(localPath)) {
        try { fs.unlinkSync(localPath); } catch {}
      }
    }
    if (removed.length) sendToRenderer('download-status', `REMOVIDOS ${removed.length} ARQUIVOS OBSOLETOS`);
  }

  const exeBin64 = path.join(GAME_PATH, 'Bin64', 'Game.exe');
  const exeRoot = path.join(GAME_PATH, 'Game.exe');
  if (!fs.existsSync(exeBin64) && !fs.existsSync(exeRoot)) {
    throw new Error('Game.exe nao encontrado apos sincronizar arquivos');
  }

  sendToRenderer('download-progress', { progress: 100, downloaded: totalBytes, total: totalBytes, speed: 0 });
  sendToRenderer('download-status', 'SINCRONIZACAO CONCLUIDA!');
  writeLocalManifestState(manifest);
  writeLauncherState({ gameVersion: manifest.version || '0.0.0', gameDownloaded: true });
  return {
    success: true,
    version: manifest.version || '0.0.0',
    filesUpdated: changed.length,
    totalFiles: manifest.files.length,
    totalSize: totalBytes
  };
}

function httpPostJson(urlString, body) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(body || {});
    let url;
    try {
      url = new URL(urlString);
    } catch (error) {
      reject(new Error('URL de cadastro invalida'));
      return;
    }

    const transport = url.protocol === 'https:' ? https : http;
    const req = transport.request({
      hostname: url.hostname,
      port: url.port || (url.protocol === 'https:' ? 443 : 80),
      path: `${url.pathname}${url.search}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data || '{}');
          if (res.statusCode >= 400 || parsed.success === false) {
            reject(new Error(parsed.error || `Erro HTTP ${res.statusCode}`));
            return;
          }
          resolve(parsed);
        } catch (error) {
          reject(new Error('Resposta invalida do servidor de cadastro'));
        }
      });
    });

    req.on('error', error => reject(new Error(`Falha ao conectar no cadastro: ${error.message}`)));
    req.setTimeout(6000, () => {
      req.destroy();
      reject(new Error('Tempo esgotado ao conectar no cadastro'));
    });
    req.write(payload);
    req.end();
  });
}

registerIpcHandlers({
  ipcMain,
  BrowserWindow,
  shell,
  path,
  fs,
  spawn,
  net,
  getMainWindow: () => mainWindow,
  getAccountWindow: () => accountWindow,
  setAccountWindow: (value) => { accountWindow = value; },
  getAccountWindowMode: () => accountWindowMode,
  setAccountWindowMode: (value) => { accountWindowMode = value; },
  getCurrentDownload: () => currentDownload,
  setCurrentDownload: (value) => { currentDownload = value; },
  getDownloadInterval: () => downloadInterval,
  setDownloadInterval: (value) => { downloadInterval = value; },
  GAME_PATH,
  GAME_EXE,
  GAME_EXE_ROOT,
  registerApiUrl,
  runtimeState,
  httpGetJson,
  httpPostJson,
  readLauncherState,
  writeLauncherState,
  sanitizeLauncherStatePatch,
  getRuntimeConfigPublic,
  getEffectiveServerConfig,
  getEffectiveLauncherConfigUrl,
  getEffectiveGameVersionUrl,
  getEffectiveGameManifestUrl,
  getEffectiveDiscordInviteUrl,
  readLocalManifestState,
  isGameInstallationValid,
  compareVersions,
  withCacheBuster,
  syncGameManifest,
  getValidGameExePath,
  sendToRenderer
});

