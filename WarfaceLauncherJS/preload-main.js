const { contextBridge, ipcRenderer } = require('electron');

const invokeChannels = new Set([
  'get-launcher-state',
  'set-launcher-state',
  'get-runtime-config',
  'check-game-installed',
  'open-account-window',
  'cancel-download',
  'check-game-update',
  'launch-game',
  'check-server-status',
  'fetch-server-info',
  'fetch-launcher-config',
  'sync-game-manifest',
  'open-discord-invite'
]);

const sendChannels = new Set([
  'window-minimize',
  'window-maximize',
  'window-close'
]);

const receiveChannels = new Set([
  'check-installation',
  'account-created',
  'account-logged-in',
  'download-progress',
  'download-status',
  'launcher-update-start',
  'launcher-update-progress',
  'launcher-update-status',
  'launcher-update-error',
  'runtime-config-updated',
  'runtime-config-connection',
  'server-status-update'
]);

contextBridge.exposeInMainWorld('launcherAPI', {
  invoke(channel, payload) {
    if (!invokeChannels.has(channel)) {
      return Promise.reject(new Error(`IPC invoke bloqueado: ${channel}`));
    }
    return ipcRenderer.invoke(channel, payload);
  },
  send(channel, payload) {
    if (!sendChannels.has(channel)) return;
    ipcRenderer.send(channel, payload);
  },
  on(channel, callback) {
    if (!receiveChannels.has(channel) || typeof callback !== 'function') {
      return () => {};
    }
    const wrapped = (event, ...args) => callback(event, ...args);
    ipcRenderer.on(channel, wrapped);
    return () => ipcRenderer.removeListener(channel, wrapped);
  }
});
