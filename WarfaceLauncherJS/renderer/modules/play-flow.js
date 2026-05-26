import { LauncherState } from './state-ui.js';

export function bindPlayButton(ctx) {
  const { ipcRenderer, state, stateUi, account, updates, elements } = ctx;
  const btnPlay = document.getElementById('btn-play');
  if (!btnPlay) return;

  btnPlay.addEventListener('click', async () => {
    if (state.isProcessing || state.isLauncherUpdating) return;
    state.isProcessing = true;

    try {
      const launcherState = stateUi.getLauncherState();

      if (launcherState === LauncherState.DOWNLOADING) {
        await ipcRenderer.invoke('cancel-download');
        state.isDownloading = false;
        state.isRequiredUpdateRunning = false;
        stateUi.updatePlayButton();
        stateUi.updateStatusMessage();
        return;
      }

      if (launcherState === LauncherState.REQUIRES_UPDATE) {
        if (state.requiredGameUpdate) {
          await updates.runRequiredGameUpdate(state.requiredGameUpdate);
        }
        return;
      }

      if (launcherState === LauncherState.NO_GAME) {
        await stateUi.checkGameInstalled();
        account.updateAccountPanel();
        const refreshedState = stateUi.getLauncherState();
        if (refreshedState !== LauncherState.NO_GAME) {
          if (elements.statusEl) {
            elements.statusEl.textContent = 'JOGO LOCALIZADO. ESTADO ATUALIZADO.';
            elements.statusEl.style.color = '#4aaa4a';
            elements.statusEl.style.visibility = 'visible';
          }
          stateUi.updatePlayButton();
          stateUi.updateStatusMessage();
          return;
        }

        await updates.startInitialInstallFlow();
        return;
      }

      if (launcherState === LauncherState.NEEDS_ACCOUNT) {
        await ipcRenderer.invoke('open-account-window', { mode: 'register' });
        return;
      }

      if (launcherState === LauncherState.NEEDS_LOGIN) {
        await ipcRenderer.invoke('open-account-window', { mode: 'login' });
        return;
      }

      if (launcherState !== LauncherState.READY) {
        stateUi.updateStatusMessage();
        return;
      }

      const result = await ipcRenderer.invoke('launch-game', {
        username: state.launcherAccount?.username || '',
        accountId: state.launcherAccount?.accountId || '',
        password: state.launcherAccount?.password || ''
      });

      if (result && result.success) {
        if (elements.statusEl) {
          elements.statusEl.textContent = 'JOGO INICIADO COM SUCESSO';
          elements.statusEl.style.color = '#4aaa4a';
          elements.statusEl.style.visibility = 'visible';
        }
      } else if (elements.statusEl) {
        elements.statusEl.textContent = `ERRO: ${(result && result.error) || 'Falha ao iniciar o jogo'}`;
        elements.statusEl.style.color = '#c8371a';
        elements.statusEl.style.visibility = 'visible';
      }
    } catch (error) {
      if (elements.statusEl) {
        elements.statusEl.textContent = `ERRO: ${error.message || 'Falha no fluxo do launcher'}`;
        elements.statusEl.style.color = '#c8371a';
        elements.statusEl.style.visibility = 'visible';
      }
    } finally {
      state.isProcessing = false;
      stateUi.updatePlayButton();
    }
  });
}
