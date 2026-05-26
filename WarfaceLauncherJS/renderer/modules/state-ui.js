export const LauncherState = Object.freeze({
  BLOCKED: 'blocked',
  REQUIRES_UPDATE: 'requires_update',
  DOWNLOADING: 'downloading',
  NO_GAME: 'no_game',
  NEEDS_ACCOUNT: 'needs_account',
  NEEDS_LOGIN: 'needs_login',
  READY: 'ready'
});

export function createStateUiModule(ctx) {
  const { ipcRenderer, state, pctEl, pbarEl, statusEl } = ctx;

  function getLauncherState() {
    const hasAccountSession = Boolean(
      state.launcherAccount &&
      typeof state.launcherAccount === 'object' &&
      String(state.launcherAccount.username || '').trim() &&
      String(state.launcherAccount.password || '').length > 0
    );

    if (state.isLauncherUpdating) return LauncherState.BLOCKED;
    if (state.requiredGameUpdate && !state.isRequiredUpdateRunning && !state.isDownloading) return LauncherState.REQUIRES_UPDATE;
    if (state.isDownloading || state.isRequiredUpdateRunning) return LauncherState.DOWNLOADING;
    if (!state.isGameInstalled) return LauncherState.NO_GAME;
    if (!state.hasCreatedAccount) return LauncherState.NEEDS_ACCOUNT;
    if (!hasAccountSession) return LauncherState.NEEDS_LOGIN;
    return LauncherState.READY;
  }

  function updateStatusMessage() {
    if (!pctEl || !pbarEl || !statusEl) return;
    if (state.isDownloading) return;

    const launcherState = getLauncherState();
    if (state.requiredGameUpdate) {
      const isInitial = state.requiredGameUpdate.isInitialInstall === true;
      pctEl.textContent = state.requiredGameUpdate.error ? '!' : '0%';
      pbarEl.style.width = '0%';
      statusEl.textContent = state.requiredGameUpdate.error
        ? `ERRO NA ${isInitial ? 'INSTALACAO' : 'ATUALIZACAO'}: ${state.requiredGameUpdate.error}`
        : (isInitial
          ? `INSTALACAO DO JOGO: ${state.requiredGameUpdate.latestVersion}`
          : `ATUALIZACAO OBRIGATORIA: ${state.requiredGameUpdate.currentVersion} -> ${state.requiredGameUpdate.latestVersion}`);
      statusEl.style.color = state.requiredGameUpdate.error ? '#c8371a' : '#c8a01a';
      statusEl.style.visibility = 'visible';
      return;
    }

    if (launcherState !== LauncherState.NO_GAME) {
      pctEl.textContent = '100%';
      pbarEl.style.width = '100%';
      if (launcherState === LauncherState.READY) {
        statusEl.textContent = 'PRONTO PARA JOGAR';
        statusEl.style.color = '#4aaa4a';
      } else if (launcherState === LauncherState.NEEDS_LOGIN) {
        statusEl.textContent = 'FACA LOGIN PARA JOGAR';
        statusEl.style.color = '#c8a01a';
      } else {
        statusEl.textContent = 'CRIE UMA CONTA PARA JOGAR';
        statusEl.style.color = '#5a7a5a';
      }
    } else {
      pctEl.textContent = '0%';
      pbarEl.style.width = '0%';
      statusEl.textContent = 'AGUARDANDO DOWNLOAD';
      statusEl.style.color = '#5a7a5a';
    }
    statusEl.style.visibility = 'visible';
  }

  function updatePlayButton() {
    const playBtn = document.getElementById('btn-play');
    if (!playBtn) return;
    const launcherState = getLauncherState();

    if (launcherState === LauncherState.BLOCKED) {
      playBtn.textContent = 'ATUALIZANDO';
      playBtn.disabled = true;
      return;
    }
    if (launcherState === LauncherState.DOWNLOADING) {
      playBtn.textContent = 'CANCELAR';
      playBtn.disabled = false;
      return;
    }
    if (launcherState === LauncherState.REQUIRES_UPDATE) {
      playBtn.textContent = state.requiredGameUpdate.error ? 'TENTAR NOVAMENTE' : (state.requiredGameUpdate.isInitialInstall ? 'INSTALAR' : 'ATUALIZAR');
      playBtn.disabled = false;
      return;
    }
    if (launcherState === LauncherState.NO_GAME) {
      playBtn.textContent = 'BAIXAR';
      playBtn.disabled = false;
      return;
    }
    if (launcherState === LauncherState.NEEDS_ACCOUNT) {
      playBtn.textContent = 'CRIAR CONTA';
      playBtn.disabled = false;
      return;
    }
    if (launcherState === LauncherState.READY) {
      playBtn.textContent = 'JOGAR';
      playBtn.disabled = false;
      return;
    }
    playBtn.textContent = 'LOGIN';
    playBtn.disabled = false;
  }

  async function persistLauncherState(patch) {
    try {
      await ipcRenderer.invoke('set-launcher-state', patch || {});
    } catch (error) {
      console.error('Error persisting launcher state:', error.message);
    }
  }

  async function hydrateLauncherState() {
    try {
      const result = await ipcRenderer.invoke('get-launcher-state');
      if (!result || !result.success || !result.state) return;
      const persisted = result.state;
      if (persisted.gameDownloaded === true) localStorage.setItem('wf_game_downloaded', 'true');
      if (persisted.gameVersion) localStorage.setItem('wf_game_version', String(persisted.gameVersion));
      if (persisted.hasCreatedAccount === true) localStorage.setItem('wf_has_created_account', 'true');
      if (persisted.account && persisted.account.username) {
        localStorage.setItem('wf_launcher_account', JSON.stringify({
          username: persisted.account.username,
          accountId: persisted.account.accountId,
          activated: persisted.account.activated !== false
        }));
        localStorage.setItem('wf_has_created_account', 'true');
      }
    } catch (error) {
      console.error('Error hydrating launcher state:', error.message);
    }
  }

  async function checkGameInstalled() {
    try {
      state.gameExeExists = await ipcRenderer.invoke('check-game-installed');
    } catch {
      state.gameExeExists = false;
    }

    let hasFlag = localStorage.getItem('wf_game_downloaded') === 'true';
    if (!state.gameExeExists && hasFlag && !state.isDownloading) {
      localStorage.removeItem('wf_game_downloaded');
      persistLauncherState({ gameDownloaded: false });
      hasFlag = false;
    }
    if (state.gameExeExists && !hasFlag) {
      localStorage.setItem('wf_game_downloaded', 'true');
      persistLauncherState({ gameDownloaded: true });
    }

    state.isGameInstalled = state.gameExeExists && (hasFlag || state.gameExeExists);
    updatePlayButton();
    updateStatusMessage();
  }

  return {
    getLauncherState,
    updateStatusMessage,
    updatePlayButton,
    persistLauncherState,
    hydrateLauncherState,
    checkGameInstalled
  };
}
