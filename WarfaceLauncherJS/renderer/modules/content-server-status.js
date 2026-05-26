import { formatUptime } from './helpers.js';

export function createServerStatusModule(ctx) {
  const { ipcRenderer, state, requiredGameUpdateRef } = ctx;

  function renderStatus(online, info = {}, ping = null) {
    const indicator = document.getElementById('server-indicator');
    const statusText = document.getElementById('server-status');
    const pingDisplay = document.getElementById('ping-display');
    const jogadoresEl = document.getElementById('stat-jogadores');
    const partidasEl = document.getElementById('stat-partidas');
    const versaoEl = document.getElementById('stat-versao');
    const uptimeEl = document.getElementById('stat-uptime');

    if (online) {
      if (indicator) indicator.className = 'stat-indicator si-online';
      if (statusText) { statusText.textContent = 'ONLINE'; statusText.className = 'stat-val'; }
    } else {
      if (indicator) indicator.className = 'stat-indicator si-offline';
      if (statusText) { statusText.textContent = 'OFFLINE'; statusText.className = 'stat-val red'; }
    }

    if (pingDisplay) {
      pingDisplay.textContent = Number.isFinite(ping) && ping >= 0 ? `${Math.floor(ping)}ms` : '--ms';
    }

    const onlineCount = Math.max(0, Number(info.online || 0));
    const localGameVersion = localStorage.getItem('wf_game_version') || '';
    const requiredGameUpdate = requiredGameUpdateRef.get();
    const publishedVersion = (requiredGameUpdate && requiredGameUpdate.latestVersion)
      ? String(requiredGameUpdate.latestVersion)
      : String(localGameVersion || '');

    if (jogadoresEl) jogadoresEl.textContent = onlineCount.toLocaleString();
    if (partidasEl) partidasEl.textContent = Math.max(0, Math.floor(onlineCount / 2));
    if (versaoEl) versaoEl.textContent = publishedVersion || String(info.version || '--');
    if (uptimeEl) uptimeEl.textContent = formatUptime(Number(info.uptime || 0));
  }

  async function checkServerStatus() {
    try {
      const [statusResult, infoResult] = await Promise.allSettled([
        ipcRenderer.invoke('check-server-status'),
        ipcRenderer.invoke('fetch-server-info')
      ]);

      const tcpOnline = statusResult.status === 'fulfilled' && statusResult.value && statusResult.value.online;
      const apiOnline = infoResult.status === 'fulfilled'
        && infoResult.value
        && infoResult.value.success
        && (
          String(infoResult.value.status || '').toLowerCase() === 'online'
          || Number(infoResult.value.players || 0) > 0
          || Number(infoResult.value.online || 0) > 0
        );
      const effectiveOnline = Boolean(tcpOnline || apiOnline);
      const info = (infoResult.status === 'fulfilled' && infoResult.value && infoResult.value.success) ? infoResult.value : {};
      const ping = tcpOnline ? Number(statusResult.value.ping || -1) : -1;
      renderStatus(effectiveOnline, info, ping);
    } catch (error) {
      console.error('Error checking server status:', error);
    }
  }

  function applyServerStatusUpdate(payload) {
    const info = payload && typeof payload === 'object' ? payload : {};
    const hasOnlineText = String(info.status || '').toLowerCase() === 'online';
    const onlineByCount = Number(info.online || 0) > 0 || Number(info.players || 0) > 0;
    const online = hasOnlineText || onlineByCount || info.online === true;
    renderStatus(online, info, null);
  }

  return {
    checkServerStatus,
    applyServerStatusUpdate
  };
}
