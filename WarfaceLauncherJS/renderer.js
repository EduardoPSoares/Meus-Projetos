import { createLauncherAppContext } from './renderer/modules/app-context.js';
import { bindPlayButton } from './renderer/modules/play-flow.js';
import {
  bindLauncherIpc,
  bindWindowButtons,
  initializeLauncher,
  startServerStatusPolling
} from './renderer/modules/lifecycle.js';

const ipcRenderer = window.launcherAPI;
const app = createLauncherAppContext(ipcRenderer);

window.addEventListener('beforeunload', () => app.clearAllTimeouts());

bindWindowButtons(ipcRenderer);
bindPlayButton(app);
bindLauncherIpc(app);
app.account.bindAccountButton();
app.updates.bindUpdateButtons();
initializeLauncher(app);

const serverCheckInterval = startServerStatusPolling(app);
window.addEventListener('beforeunload', () => clearInterval(serverCheckInterval));
