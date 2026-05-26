const { contextBridge, ipcRenderer } = require('electron');

const invokeChannels = new Set([
  'login-account',
  'register-account'
]);

const sendChannels = new Set([
  'account-window-minimize',
  'account-window-close',
  'account-created',
  'account-logged-in'
]);

contextBridge.exposeInMainWorld('accountAPI', {
  invoke(channel, payload) {
    if (!invokeChannels.has(channel)) {
      return Promise.reject(new Error(`IPC invoke bloqueado: ${channel}`));
    }
    return ipcRenderer.invoke(channel, payload);
  },
  send(channel, payload) {
    if (!sendChannels.has(channel)) return;
    ipcRenderer.send(channel, payload);
  }
});
