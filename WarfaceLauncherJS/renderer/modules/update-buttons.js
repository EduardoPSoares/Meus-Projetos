export function bindUpdateButtons(ctx) {
  const { ipcRenderer, state, updatePlayButton, updateStatusMessage, onRetry } = ctx;

  const updateCancelBtn = document.getElementById('update-cancel-btn');
  if (updateCancelBtn) {
    updateCancelBtn.addEventListener('click', async () => {
      await ipcRenderer.invoke('cancel-download');
      state.isDownloading = false;
      state.isRequiredUpdateRunning = false;
      updatePlayButton();
      updateStatusMessage();
    });
  }

  const updateRetryBtn = document.getElementById('update-retry-btn');
  if (updateRetryBtn) {
    updateRetryBtn.addEventListener('click', () => {
      if (typeof onRetry === 'function') onRetry();
    });
  }
}
