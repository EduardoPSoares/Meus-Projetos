import { formatBytes, formatSpeed, formatTime, parseDownloadStatusMessage, shortenPathMiddle } from './helpers.js';

export function createOverlayModule(ctx) {
  const { state, statusEl, setManagedTimeout, updatePlayButton, updateAccountPanel } = ctx;

  function showUpdateOverlay(title, desc, details = {}) {
    const overlay = document.getElementById('update-overlay');
    const titleEl = document.getElementById('update-title');
    const descEl = document.getElementById('update-desc');
    const versionInfo = document.getElementById('update-version-info');
    const currentEl = document.getElementById('update-current-version');
    const targetEl = document.getElementById('update-target-version');
    const progressBlock = document.getElementById('update-progress-block');
    const errorEl = document.getElementById('update-error');
    const actionsEl = document.getElementById('update-actions');
    const retryBtn = document.getElementById('update-retry-btn');
    const cancelBtn = document.getElementById('update-cancel-btn');
    const spinner = document.getElementById('update-spinner');

    if (overlay) overlay.classList.remove('hidden');
    if (titleEl) titleEl.textContent = title;
    if (descEl) descEl.textContent = desc;
    if (versionInfo) versionInfo.classList.toggle('hidden', !details.currentVersion && !details.latestVersion);
    if (currentEl) currentEl.textContent = details.currentVersion || '--';
    if (targetEl) targetEl.textContent = details.latestVersion || '--';
    if (progressBlock) progressBlock.classList.toggle('hidden', details.progress === false);
    if (errorEl) {
      errorEl.textContent = details.error || '';
      errorEl.classList.toggle('hidden', !details.error);
    }
    if (actionsEl) actionsEl.classList.toggle('hidden', !details.actions);
    if (retryBtn) retryBtn.style.display = details.retry ? '' : 'none';
    if (cancelBtn) {
      cancelBtn.style.display = details.cancel === false ? 'none' : '';
      cancelBtn.textContent = details.cancelLabel || 'CANCELAR';
    }
    if (spinner) spinner.style.display = details.spinner === false ? 'none' : '';
  }

  function hideUpdateOverlay() {
    const overlay = document.getElementById('update-overlay');
    if (overlay) overlay.classList.add('hidden');
  }

  function setUpdateOverlayProgress(data) {
    const bar = document.getElementById('update-overlay-pbar');
    const meta = document.getElementById('update-progress-meta');
    const progress = data && typeof data.progress === 'number' ? Math.min(100, Math.max(0, data.progress)) : 0;
    if (bar) bar.style.width = `${progress}%`;
    if (meta) {
      const downloaded = Number((data && data.downloaded) || 0);
      const total = Number((data && data.total) || 0);
      const speed = Number((data && data.speed) || 0);
      const remainingBytes = Math.max(0, total - downloaded);
      const etaSeconds = (speed > 0 && remainingBytes > 0) ? Math.round(remainingBytes / speed) : 0;
      const eta = etaSeconds > 0 ? formatTime(etaSeconds) : '--';
      const pct = `${Math.round(progress)}%`;
      meta.textContent = `${pct} • ${formatBytes(downloaded)} / ${formatBytes(total)} • ${formatSpeed(speed)} • ETA ${eta}`;
    }
  }

  function showUpdateOverlayError(message) {
    const errorEl = document.getElementById('update-error');
    const actionsEl = document.getElementById('update-actions');
    const retryBtn = document.getElementById('update-retry-btn');
    const cancelBtn = document.getElementById('update-cancel-btn');
    const spinner = document.getElementById('update-spinner');
    if (errorEl) {
      errorEl.textContent = message || 'Falha na atualizacao';
      errorEl.classList.remove('hidden');
    }
    if (actionsEl) actionsEl.classList.remove('hidden');
    if (retryBtn) retryBtn.style.display = '';
    if (cancelBtn) {
      cancelBtn.style.display = '';
      cancelBtn.textContent = 'FECHAR';
    }
    if (spinner) spinner.style.display = 'none';
  }

  function showLauncherUpdateOverlay(details = {}) {
    state.isLauncherUpdating = true;
    state.isDownloading = true;
    showUpdateOverlay('ATUALIZANDO LAUNCHER', 'Baixando atualização silenciosa...', {
      currentVersion: details.currentVersion,
      latestVersion: details.latestVersion,
      actions: false,
      cancel: false,
      retry: false
    });
    setUpdateOverlayProgress({ progress: 0, downloaded: 0, total: details.size || 0, speed: 0 });
    if (statusEl) {
      statusEl.style.visibility = 'visible';
      statusEl.textContent = `ATUALIZANDO LAUNCHER: ${details.currentVersion || '--'} -> ${details.latestVersion || '--'}`;
      statusEl.style.color = '#c8a01a';
    }
    updatePlayButton();
    updateAccountPanel();
  }

  function handleDownloadStatus(message, requiredGameUpdate, setTransferLabel) {
    const parsed = parseDownloadStatusMessage(message);
    const text = parsed.text;
    const descEl = document.getElementById('update-desc');
    setTransferLabel(parsed.transferLabel);

    if (descEl && requiredGameUpdate) descEl.textContent = parsed.desc;
    if (statusEl) {
      statusEl.style.visibility = 'visible';
      statusEl.textContent = parsed.status;
      statusEl.style.color = requiredGameUpdate ? '#c8a01a' : '#5a9a5a';
    }

    if (text.includes('EXTRAINDO') || text.includes('INSTALANDO')) {
      const pbarEl = document.getElementById('pbar');
      const pctEl = document.getElementById('pct');
      if (pbarEl) pbarEl.style.width = '100%';
      if (pctEl) pctEl.textContent = '...';
      setUpdateOverlayProgress({ progress: 100, downloaded: 1, total: 1, speed: 0 });
    }
  }

  function bindLauncherUpdateIpc(ipcRenderer) {
    ipcRenderer.on('launcher-update-start', (event, details = {}) => {
      showLauncherUpdateOverlay(details);
    });

    ipcRenderer.on('launcher-update-progress', (event, details = {}) => {
      showLauncherUpdateOverlay(details);
      setUpdateOverlayProgress(details);
      const descEl = document.getElementById('update-desc');
      if (descEl) {
        const fileLabel = details.file ? shortenPathMiddle(String(details.file), 58) : '';
        descEl.textContent = fileLabel ? `Baixando: ${fileLabel}` : 'Baixando atualização silenciosa...';
      }
    });

    ipcRenderer.on('launcher-update-status', (event, details = {}) => {
      showUpdateOverlay('ATUALIZANDO LAUNCHER', details.message || 'Aplicando atualização...', {
        currentVersion: details.currentVersion,
        latestVersion: details.latestVersion,
        actions: false,
        cancel: false,
        retry: false
      });
    });

    ipcRenderer.on('launcher-update-error', (event, details = {}) => {
      state.isLauncherUpdating = false;
      state.isDownloading = false;
      showUpdateOverlay('FALHA NA ATUALIZAÇÃO', 'Não foi possível atualizar o launcher automaticamente.', {
        actions: false,
        spinner: false,
        progress: false,
        error: details.message || 'Falha ao atualizar o launcher'
      });
      setManagedTimeout(hideUpdateOverlay, 4500);
      updatePlayButton();
      updateAccountPanel();
    });
  }

  return {
    showUpdateOverlay,
    hideUpdateOverlay,
    setUpdateOverlayProgress,
    showUpdateOverlayError,
    showLauncherUpdateOverlay,
    handleDownloadStatus,
    bindLauncherUpdateIpc,
    formatBytes,
    formatSpeed,
    formatTime,
    shortenPathMiddle
  };
}
