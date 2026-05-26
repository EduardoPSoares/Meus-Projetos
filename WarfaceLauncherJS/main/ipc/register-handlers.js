function registerIpcHandlers(ctx) {
  const {
    ipcMain,
    BrowserWindow,
    shell,
    path,
    fs,
    spawn,
    net,
    getMainWindow,
    getAccountWindow,
    setAccountWindow,
    getAccountWindowMode,
    setAccountWindowMode,
    getCurrentDownload,
    setCurrentDownload,
    getDownloadInterval,
    setDownloadInterval,
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
  } = ctx;

  let lastServerCheck = null;
  let lastServerCheckResult = null;

  function generateOnlineConfig() {
    const serverConfig = getEffectiveServerConfig();
    const escapeValue = (val) => {
      if (typeof val === 'string') {
        return val.replace(/[\\]/g, '\\\\').replace(/[\n]/g, '\\n').replace(/[\r]/g, '\\r');
      }
      return val;
    };

    return `; Warface - Servidor
; Coloque este arquivo na pasta raiz do jogo (onde esta o Game.exe)
online_host = ${escapeValue(serverConfig.host)}
online_server = ${escapeValue(serverConfig.ip)}
online_server_port = ${serverConfig.port}
online_use_tls = ${serverConfig.useTLS ? 1 : 0}
online_use_protect = ${serverConfig.useProtect ? 1 : 0}
online_check_certificate = ${serverConfig.checkCertificate ? 1 : 0}
sv_cvars_hash_enable = ${serverConfig.disableHashValidation ? 0 : 1}
anti_cheat_exe_hash_validation = ${serverConfig.disableAntiCheat ? 0 : 1}

; Modo release
cl_release_build = 1
ui_debug_show_skill = 0
r_DisplayInfo = 0
con_restricted = 1
con_showonload = 0
con_display_last_messages = 0
sys_DeactivateConsole = 1
VisualConsole = 0

; Interface
ui_show_cohtml = 0
sys_use_cohtml_ui = 0
`;
  }

  ipcMain.on('window-minimize', () => {
    const mainWindow = getMainWindow();
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.minimize();
  });

  ipcMain.on('window-maximize', () => {});

  ipcMain.on('window-close', () => {
    const mainWindow = getMainWindow();
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.close();
  });

  ipcMain.on('account-window-minimize', () => {
    const accountWindow = getAccountWindow();
    if (accountWindow && !accountWindow.isDestroyed()) accountWindow.minimize();
  });

  ipcMain.on('account-window-close', () => {
    const accountWindow = getAccountWindow();
    if (accountWindow && !accountWindow.isDestroyed()) accountWindow.close();
  });

  ipcMain.on('account-created', (event, account) => {
    const mainWindow = getMainWindow();
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('account-created', account);
  });

  ipcMain.on('account-logged-in', (event, account) => {
    const mainWindow = getMainWindow();
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('account-logged-in', account);
  });

  ipcMain.handle('check-game-installed', async () => {
    // Always detect by real executable on disk so reinstalling/deleting the launcher
    // does not lose the "installed game" state.
    const validExe = getValidGameExePath();
    return Boolean(validExe);
  });

  ipcMain.handle('open-account-window', async (event, args) => {
    const mainWindow = getMainWindow();
    if (!mainWindow || mainWindow.isDestroyed()) {
      return { success: false, error: 'Janela principal nao disponivel' };
    }

    const requestedMode = (args && args.mode) || 'register';
    const mode = requestedMode === 'login' ? 'login' : 'register';

    const accountWindow = getAccountWindow();
    if (accountWindow && !accountWindow.isDestroyed() && getAccountWindowMode() === mode) {
      accountWindow.focus();
      return { success: true };
    }

    if (accountWindow && !accountWindow.isDestroyed()) {
      accountWindow.close();
      setAccountWindow(null);
      setAccountWindowMode(null);
    }

    try {
      const nextAccountWindow = new BrowserWindow({
        width: 520,
        height: mode === 'login' ? 430 : 500,
        frame: false,
        transparent: false,
        resizable: false,
        backgroundColor: '#060808',
        parent: mainWindow,
        modal: true,
        webPreferences: {
          nodeIntegration: false,
          contextIsolation: true,
          enableRemoteModule: false,
          spellcheck: false,
          preload: path.join(__dirname, '..', '..', 'preload-account.js')
        },
        show: false
      });

      setAccountWindow(nextAccountWindow);
      setAccountWindowMode(mode);

      nextAccountWindow.loadFile('account.html', { query: { mode } });
      nextAccountWindow.once('ready-to-show', () => {
        if (!nextAccountWindow.isDestroyed()) nextAccountWindow.show();
      });
      nextAccountWindow.on('closed', () => {
        setAccountWindow(null);
        setAccountWindowMode(null);
      });

      return { success: true };
    } catch (error) {
      setAccountWindow(null);
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('cancel-download', async () => {
    const currentDownload = getCurrentDownload();
    if (currentDownload) {
      try { currentDownload.abort(); } catch {}
      setCurrentDownload(null);
    }

    const downloadInterval = getDownloadInterval();
    if (downloadInterval) {
      clearInterval(downloadInterval);
      setDownloadInterval(null);
    }

    return { success: true };
  });

  ipcMain.handle('login-account', async (event, account) => {
    try {
      const username = String((account && account.username) || '').trim().toLowerCase();
      const password = String((account && account.password) || '');
      if (!username || !password) return { success: false, error: 'Preencha usuario e senha' };
      return await httpPostJson(`${registerApiUrl}/api/public/authenticate`, { username, password });
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('register-account', async (event, account) => {
    try {
      const username = String((account && account.username) || '').trim().toLowerCase();
      const password = String((account && account.password) || '');
      if (!/^[a-z][a-z0-9_-]{2,19}$/.test(username)) {
        return { success: false, error: 'Usuario deve comecar com letra e ter 3 a 20 caracteres' };
      }
      if (password.length < 3 || password.length > 32 || !/^[A-Za-z0-9_.@-]+$/.test(password)) {
        return { success: false, error: 'Senha deve ter 3 a 32 caracteres sem espacos' };
      }
      if (password.toLowerCase() === username) {
        return { success: false, error: 'Senha nao pode ser igual ao usuario' };
      }
      return await httpPostJson(`${registerApiUrl}/api/public/register`, { username, password });
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('get-launcher-state', async () => {
    try {
      return { success: true, state: readLauncherState() };
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('set-launcher-state', async (event, patch) => {
    try {
      return { success: true, state: writeLauncherState(sanitizeLauncherStatePatch(patch)) };
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('get-runtime-config', async () => ({ success: true, runtime: getRuntimeConfigPublic() }));

  ipcMain.handle('launch-game', async (event, credentials) => {
    const launchExe = getValidGameExePath();
    if (!launchExe) return { success: false, error: 'Jogo nao instalado! Clique em BAIXAR primeiro.' };

    try {
      const configContent = generateOnlineConfig();
      const configFilePath = path.join(GAME_PATH, 'online.cfg');
      fs.writeFileSync(configFilePath, configContent, 'utf8');

      const activeServerConfig = getEffectiveServerConfig();
      const gameArgs = [
        '+online_server', activeServerConfig.ip,
        '+online_server_port', String(activeServerConfig.port),
        '+online_use_tls', activeServerConfig.useTLS ? '1' : '0',
        '+online_use_protect', activeServerConfig.useProtect ? '1' : '0',
        '+ui_show_cohtml', '0',
        '+sys_use_cohtml_ui', '0',
        '+r_DisplayInfo', '0',
        '-Language', 'Russian'
      ];

      const accountId = credentials && credentials.accountId ? String(credentials.accountId).trim() : '';
      const username = credentials && credentials.username ? String(credentials.username).trim() : '';
      const password = credentials && credentials.password ? String(credentials.password).trim() : '';
      const gameLogin = username || accountId;
      if (gameLogin && password) {
        gameArgs.push('-username', gameLogin);
        gameArgs.push('-password', password);
      }

      const gameProcess = spawn(launchExe, gameArgs, {
        cwd: GAME_PATH,
        detached: true,
        stdio: 'ignore',
        windowsHide: true
      });

      gameProcess.unref();
      if (!gameProcess.pid) return { success: false, error: 'Falha ao iniciar o jogo' };
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('fetch-server-info', async () => {
    try {
      const result = await httpPostJson(`${registerApiUrl}/api/public/serverinfo`, {});
      if (result && result.success && result.server) {
        return {
          success: true,
          online: result.server.online || 0,
          players: result.server.players || 0,
          version: result.server.version || '--',
          uptime: result.server.uptime || 0,
          status: result.server.status || 'offline'
        };
      }
      return { success: false };
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('fetch-launcher-config', async () => {
    try {
      if (runtimeState.launcherUi && (Array.isArray(runtimeState.launcherUi.slides) || Array.isArray(runtimeState.launcherUi.news))) {
        return { success: true, config: runtimeState.launcherUi, source: 'runtime' };
      }
      const effectiveLauncherConfigUrl = getEffectiveLauncherConfigUrl();
      if (effectiveLauncherConfigUrl) {
        const cdnConfig = await httpGetJson(withCacheBuster(effectiveLauncherConfigUrl));
        if (cdnConfig && (Array.isArray(cdnConfig.slides) || Array.isArray(cdnConfig.news))) {
          return { success: true, config: cdnConfig, source: 'cdn' };
        }
      }
      return await httpPostJson(`${registerApiUrl}/api/public/launcher-config`, {});
    } catch (error) {
      try {
        return await httpPostJson(`${registerApiUrl}/api/public/launcher-config`, {});
      } catch {
        return { success: false, error: error.message };
      }
    }
  });

  ipcMain.handle('check-game-update', async (event, currentVersion) => {
    const effectiveGameVersionUrl = getEffectiveGameVersionUrl();
    if (!effectiveGameVersionUrl) return { update_available: false };

    try {
      const data = await httpGetJson(withCacheBuster(effectiveGameVersionUrl));
      if (!data || !data.version) return { update_available: false };

      const remoteVer = String(data.version || '0.0.0');
      const localState = readLocalManifestState();
      const canUseLocalVersion = isGameInstallationValid() && localState.files.length > 0;
      const clientVer = String(currentVersion || '0.0.0');
      const localVer = String((localState && localState.version) ? localState.version : '0.0.0');
      const effectiveCurrent = canUseLocalVersion
        ? (compareVersions(localVer, clientVer) > 0 ? localVer : clientVer)
        : '0.0.0';
      const needsUpdate = compareVersions(remoteVer, effectiveCurrent) > 0;

      return {
        update_available: needsUpdate,
        latest_version: remoteVer,
        current_version: effectiveCurrent,
        manifest_url: data.manifest_url || getEffectiveGameManifestUrl() || '',
        update_mode: 'manifest',
        is_initial_install: !canUseLocalVersion,
        base_url: data.base_url || '',
        required: data.required !== false,
        notes: data.notes || ''
      };
    } catch {
      return { update_available: false };
    }
  });

  ipcMain.handle('sync-game-manifest', async (event, payload) => {
    try {
      const url = typeof payload === 'object' && payload
        ? (payload.manifestUrl || payload.manifest_url || payload.url || getEffectiveGameManifestUrl())
        : (payload || getEffectiveGameManifestUrl());
      if (!url) return { success: false, error: 'Manifest URL nao configurada' };
      return await syncGameManifest(withCacheBuster(url), {
        currentVersion: payload && typeof payload === 'object' ? (payload.currentVersion || payload.current_version || '') : '',
        forceFullDownload: payload && typeof payload === 'object' ? payload.forceFullDownload === true : false
      });
    } catch (error) {
      return { success: false, error: error.message };
    }
  });

  ipcMain.handle('check-server-status', async () => {
    const now = Date.now();
    if (lastServerCheck && (now - lastServerCheck) < 2000) {
      return lastServerCheckResult || { online: false, ping: -1 };
    }
    lastServerCheck = now;

    return new Promise((resolve) => {
      const socket = new net.Socket();
      const timeout = 5000;
      let resolved = false;

      const cleanup = () => {
        if (!resolved) {
          resolved = true;
          try { socket.destroy(); } catch {}
        }
      };

      socket.setTimeout(timeout);
      socket.on('connect', () => {
        const ping = Date.now() - startTime;
        cleanup();
        const result = { online: true, ping };
        lastServerCheckResult = result;
        resolve(result);
      });
      socket.on('timeout', () => {
        cleanup();
        const result = { online: false, ping: -1 };
        lastServerCheckResult = result;
        resolve(result);
      });
      socket.on('error', () => {
        cleanup();
        const result = { online: false, ping: -1 };
        lastServerCheckResult = result;
        resolve(result);
      });

      const startTime = Date.now();
      try {
        const activeServerConfig = getEffectiveServerConfig();
        socket.connect(activeServerConfig.port, activeServerConfig.ip);
      } catch {
        cleanup();
        const result = { online: false, ping: -1 };
        lastServerCheckResult = result;
        resolve(result);
      }
    });
  });

  ipcMain.handle('open-discord-invite', async () => {
    try {
      const effectiveDiscordInviteUrl = getEffectiveDiscordInviteUrl();
      if (!effectiveDiscordInviteUrl || effectiveDiscordInviteUrl === 'https://discord.gg/YOUR_INVITE') {
        return { success: false, error: 'Discord nao configurado' };
      }

      let url;
      try {
        url = new URL(effectiveDiscordInviteUrl);
      } catch {
        return { success: false, error: 'URL do Discord invalida' };
      }

      const host = String(url.hostname || '').toLowerCase();
      const isAllowedHost = host === 'discord.gg' || host.endsWith('.discord.gg') || host === 'discord.com' || host.endsWith('.discord.com');
      if (url.protocol !== 'https:' || !isAllowedHost) {
        return { success: false, error: 'URL do Discord bloqueada por seguranca' };
      }

      await shell.openExternal(url.toString());
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message || 'Falha ao abrir Discord' };
    }
  });
}

module.exports = { registerIpcHandlers };
