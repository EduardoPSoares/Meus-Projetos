export function bindUpdateIpc(ctx) {
  const {
    ipcRenderer,
    state,
    overlay,
    pbarEl,
    pctEl,
    statusEl,
    requiredUpdateFlow
  } = ctx;

  ipcRenderer.on('download-progress', (event, data) => {
    if (!data) return;
    const { progress, downloaded, total, speed } = data;
    if (pbarEl) pbarEl.style.width = `${Math.min(100, Math.max(0, progress || 0))}%`;
    if (pctEl) pctEl.textContent = `${Math.round(progress || 0)}%`;
    overlay.setUpdateOverlayProgress(data);
    const fileLabel = state.currentTransferLabel ? ` • ${overlay.shortenPathMiddle(state.currentTransferLabel, 34)}` : '';
    if (statusEl) {
      statusEl.style.visibility = 'visible';
      statusEl.textContent = `BAIXANDO${fileLabel} • ${overlay.formatBytes(downloaded)} / ${overlay.formatBytes(total)} • ${overlay.formatSpeed(speed)} • ETA: ${overlay.formatTime(speed > 0 ? Math.round((total - downloaded) / speed) : 0)}`;
    }
  });

  ipcRenderer.on('download-status', (event, message) => {
    overlay.handleDownloadStatus(message, state.requiredGameUpdate, (label) => {
      state.currentTransferLabel = label;
    });
  });

  overlay.bindLauncherUpdateIpc(ipcRenderer);

  return {
    retryRequiredUpdate() {
      if (state.requiredGameUpdate && !state.isRequiredUpdateRunning) {
        requiredUpdateFlow.runRequiredGameUpdate(state.requiredGameUpdate);
      }
    }
  };
}
