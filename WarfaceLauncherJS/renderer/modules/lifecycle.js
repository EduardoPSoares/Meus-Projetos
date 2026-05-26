export function bindWindowButtons(ipcRenderer) {
  const btnMinimize = document.getElementById('btn-minimize');
  const btnMaximize = document.getElementById('btn-maximize');
  const btnClose = document.getElementById('btn-close');

  if (btnMinimize) btnMinimize.addEventListener('click', () => ipcRenderer.send('window-minimize'));
  if (btnMaximize) btnMaximize.addEventListener('click', () => ipcRenderer.send('window-maximize'));
  if (btnClose) btnClose.addEventListener('click', () => ipcRenderer.send('window-close'));
}

export function bindLauncherIpc(ctx) {
  const { ipcRenderer, state, stateUi, account, updates, content } = ctx;

  ipcRenderer.on('check-installation', () => stateUi.checkGameInstalled().then(() => account.updateAccountPanel()));
  ipcRenderer.on('runtime-config-updated', (event, nextRuntime) => {
    state.runtimeConfig = nextRuntime;
    content.checkServerStatus();
  });
  ipcRenderer.on('runtime-config-connection', (event, data) => {
    state.runtimeConnected = Boolean(data && data.connected === true);
  });
  ipcRenderer.on('server-status-update', (event, payload) => {
    content.applyServerStatusUpdate(payload);
  });

  account.bindAccountIpc();
  updates.bindUpdateIpc();
}

export async function hydrateRuntimeConfig(ctx) {
  const { ipcRenderer, state, content } = ctx;

  try {
    const result = await ipcRenderer.invoke('get-runtime-config');
    if (result && result.success && result.runtime) {
      state.runtimeConfig = result.runtime;
      state.runtimeConnected = result.runtime.connected === true;
      if (result.runtime.launcherUi && typeof result.runtime.launcherUi === 'object') {
        state.launcherConfig = {
          ...(state.launcherConfig && typeof state.launcherConfig === 'object' ? state.launcherConfig : {}),
          ...result.runtime.launcherUi
        };
        content.renderHeroSlides();
        content.renderNews();
      }
    }
  } catch (error) {
    console.error('Error hydrating runtime config:', error.message);
  }
}

export async function initializeLauncher(ctx) {
  const { stateUi, updates, account, content } = ctx;

  ctx.setFinishInit(() => {
    account.loadSavedAccount();
    stateUi.checkGameInstalled().then(() => account.updateAccountPanel());
    content.fetchLauncherConfig(content.renderHeroSlides, content.renderNews);
  });

  await stateUi.hydrateLauncherState();
  await hydrateRuntimeConfig(ctx);
  await updates.checkForUpdates();
}

export function startServerStatusPolling(ctx) {
  const { content, setManagedTimeout } = ctx;
  const serverCheckInterval = setInterval(() => content.checkServerStatus(), 10000);
  setManagedTimeout(() => content.checkServerStatus(), 1000);
  return serverCheckInterval;
}
