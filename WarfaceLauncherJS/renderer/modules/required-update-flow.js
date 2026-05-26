export function createRequiredUpdateFlow(ctx) {
  const {
    ipcRenderer,
    state,
    overlay,
    setManagedTimeout,
    updatePlayButton,
    persistLauncherState,
    finishInitRef
  } = ctx;

  async function runRequiredGameUpdate(update) {
    if (!update) return false;
    update.error = '';
    state.currentTransferLabel = '';
    state.isDownloading = true;
    state.isRequiredUpdateRunning = true;
    updatePlayButton();
    overlay.setUpdateOverlayProgress({ progress: 0, downloaded: 0, total: 0, speed: 0 });

    const isInitial = update.isInitialInstall === true;
    overlay.showUpdateOverlay(
      isInitial ? 'INSTALACAO DO JOGO' : 'ATUALIZACAO OBRIGATORIA',
      isInitial ? 'Preparando download do jogo...' : 'Preparando arquivos alterados...',
      {
        currentVersion: update.currentVersion,
        latestVersion: update.latestVersion,
        actions: true,
        retry: false,
        cancel: true
      }
    );

    try {
      const result = await ipcRenderer.invoke('sync-game-manifest', {
        manifestUrl: update.manifestUrl,
        currentVersion: update.currentVersion,
        latestVersion: update.latestVersion,
        forceFullDownload: update.forceFullDownload === true
      });
      if (!result || !result.success) {
        throw new Error(result && result.error ? result.error : 'Falha ao sincronizar arquivos');
      }

      localStorage.setItem('wf_game_version', update.latestVersion || result.version || '0.0.0');
      localStorage.setItem('wf_game_downloaded', 'true');
      persistLauncherState({
        gameVersion: update.latestVersion || result.version || '0.0.0',
        gameDownloaded: true
      });

      state.requiredGameUpdate = null;
      state.currentTransferLabel = '';
      state.isDownloading = false;
      state.isRequiredUpdateRunning = false;
      state.gameExeExists = true;
      state.isGameInstalled = true;

      overlay.showUpdateOverlay(isInitial ? 'JOGO INSTALADO' : 'JOGO ATUALIZADO', 'Arquivos sincronizados com sucesso.', {
        currentVersion: update.currentVersion,
        latestVersion: update.latestVersion,
        actions: false,
        spinner: false
      });
      overlay.setUpdateOverlayProgress({ progress: 100, downloaded: 1, total: 1, speed: 0 });
      setManagedTimeout(overlay.hideUpdateOverlay, 1200);
      finishInitRef.get()();
      return true;
    } catch (error) {
      state.currentTransferLabel = '';
      state.isDownloading = false;
      state.isRequiredUpdateRunning = false;
      update.error = error.message || 'Falha na atualizacao';
      overlay.showUpdateOverlayError(update.error);
      updatePlayButton();
      return false;
    }
  }

  async function startInitialInstallFlow() {
    const verCheck = await ipcRenderer.invoke('check-game-update', '0.0.0');
    const manifestUrl = String(verCheck && verCheck.manifest_url ? verCheck.manifest_url : '').trim();
    if (!manifestUrl) {
      throw new Error('Manifest do jogo nao configurado no painel.');
    }

    state.requiredGameUpdate = {
      currentVersion: '0.0.0',
      latestVersion: verCheck.latest_version || '0.0.0',
      manifestUrl,
      updateMode: 'manifest',
      notes: verCheck.notes || '',
      error: '',
      isInitialInstall: true,
      forceFullDownload: true
    };

    return runRequiredGameUpdate(state.requiredGameUpdate);
  }

  return {
    runRequiredGameUpdate,
    startInitialInstallFlow
  };
}
