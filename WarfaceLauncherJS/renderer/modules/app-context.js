import { createContentModule } from './content.js';
import { createOverlayModule } from './overlay.js';
import { createStateUiModule } from './state-ui.js';
import { createAccountModule } from './account.js';
import { createUpdatesModule } from './updates.js';

export function createLauncherAppContext(ipcRenderer) {
  const state = {
    isGameInstalled: false,
    isDownloading: false,
    isProcessing: false,
    activeTimeouts: [],
    launcherAccount: null,
    hasCreatedAccount: false,
    gameExeExists: false,
    launcherConfig: null,
    currentSlide: 0,
    requiredGameUpdate: null,
    isRequiredUpdateRunning: false,
    isLauncherUpdating: false,
    currentTransferLabel: '',
    runtimeConfig: null,
    runtimeConnected: false
  };

  const elements = {
    pctEl: document.getElementById('pct'),
    pbarEl: document.getElementById('pbar'),
    statusEl: document.getElementById('upd-status')
  };

  function setManagedTimeout(callback, delay) {
    const timeoutId = setTimeout(() => {
      const index = state.activeTimeouts.indexOf(timeoutId);
      if (index > -1) state.activeTimeouts.splice(index, 1);
      callback();
    }, delay);
    state.activeTimeouts.push(timeoutId);
    return timeoutId;
  }

  function clearAllTimeouts() {
    state.activeTimeouts.forEach((id) => clearTimeout(id));
    state.activeTimeouts = [];
  }

  const stateUi = createStateUiModule({
    ipcRenderer,
    state,
    pctEl: elements.pctEl,
    pbarEl: elements.pbarEl,
    statusEl: elements.statusEl
  });

  const account = createAccountModule({
    ipcRenderer,
    state,
    getLauncherState: stateUi.getLauncherState,
    persistLauncherState: stateUi.persistLauncherState,
    updatePlayButton: stateUi.updatePlayButton,
    updateStatusMessage: stateUi.updateStatusMessage
  });

  const overlay = createOverlayModule({
    state,
    statusEl: elements.statusEl,
    setManagedTimeout,
    updatePlayButton: stateUi.updatePlayButton,
    updateAccountPanel: account.updateAccountPanel
  });

  const content = createContentModule({
    ipcRenderer,
    state,
    requiredGameUpdateRef: { get: () => state.requiredGameUpdate }
  });

  let finishInit = () => {};

  const updates = createUpdatesModule({
    ipcRenderer,
    state,
    overlay,
    content,
    pbarEl: elements.pbarEl,
    pctEl: elements.pctEl,
    statusEl: elements.statusEl,
    setManagedTimeout,
    updatePlayButton: stateUi.updatePlayButton,
    updateStatusMessage: stateUi.updateStatusMessage,
    persistLauncherState: stateUi.persistLauncherState,
    finishInitRef: { get: () => finishInit }
  });

  function setFinishInit(next) {
    finishInit = next;
  }

  return {
    ipcRenderer,
    state,
    elements,
    stateUi,
    account,
    overlay,
    content,
    updates,
    setManagedTimeout,
    clearAllTimeouts,
    setFinishInit
  };
}
