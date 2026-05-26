import { createRequiredUpdateFlow } from './required-update-flow.js';
import { bindUpdateIpc as bindUpdateIpcInternal } from './update-ipc-bindings.js';
import { bindUpdateButtons as bindUpdateButtonsInternal } from './update-buttons.js';

export function createUpdatesModule(ctx) {
  const {
    ipcRenderer,
    state,
    finishInitRef,
    updatePlayButton,
    updateStatusMessage
  } = ctx;

  const requiredUpdateFlow = createRequiredUpdateFlow(ctx);
  let retryRequiredUpdate = () => {};

  async function checkForUpdates() {
    try {
      const installed = await ipcRenderer.invoke('check-game-installed');
      if (!installed) {
        state.requiredGameUpdate = null;
        finishInitRef.get()();
        return;
      }

      const localCurrentVersion = localStorage.getItem('wf_game_version') || '0.0.0';
      const gameUpdate = await ipcRenderer.invoke('check-game-update', localCurrentVersion);
      if (gameUpdate && gameUpdate.update_available && gameUpdate.latest_version) {
        state.requiredGameUpdate = {
          currentVersion: gameUpdate.current_version || localCurrentVersion,
          latestVersion: gameUpdate.latest_version,
          manifestUrl: String(gameUpdate.manifest_url || '').trim(),
          updateMode: 'manifest',
          notes: gameUpdate.notes || '',
          error: '',
          isInitialInstall: gameUpdate.is_initial_install === true || gameUpdate.current_version === '0.0.0'
        };
        finishInitRef.get()();
        await requiredUpdateFlow.runRequiredGameUpdate(state.requiredGameUpdate);
        return;
      }
    } catch (error) {
      console.error('Game update check error:', error.message);
    }

    finishInitRef.get()();
  }

  function bindUpdateIpc() {
    const bindings = bindUpdateIpcInternal({
      ...ctx,
      requiredUpdateFlow
    });
    retryRequiredUpdate = bindings.retryRequiredUpdate;
  }

  function bindUpdateButtons() {
    bindUpdateButtonsInternal({
      ipcRenderer,
      state,
      updatePlayButton,
      updateStatusMessage,
      onRetry: () => retryRequiredUpdate()
    });
  }

  return {
    checkForUpdates,
    runRequiredGameUpdate: requiredUpdateFlow.runRequiredGameUpdate,
    startInitialInstallFlow: requiredUpdateFlow.startInitialInstallFlow,
    bindUpdateIpc,
    bindUpdateButtons
  };
}
