(function initServicesMonitoringDomain() {
  function createServicesMonitoringDomain(ctx) {
    function getToken() {
      return ctx.getToken();
    }

    function setMonitorInterval(value) {
      ctx.setMonitorInterval(value);
    }

    function getMonitorInterval() {
      return ctx.getMonitorInterval();
    }

    function getLogStream() {
      return ctx.getLogStream();
    }

    function setLogStream(value) {
      ctx.setLogStream(value);
    }

    function pushPanelServer(server) {
      const next = ctx.getPanelRuntimeState();
      next.server = server;
      ctx.setPanelRuntimeState(next);
    }

    function pushPanelRecentLog(entry) {
      const next = ctx.getPanelRuntimeState();
      next.recentLog = entry;
      ctx.setPanelRuntimeState(next);
    }

    async function fetchServerInfo() {
      try {
        const r = await fetch('/api/serverinfo', { headers: { 'X-Auth-Token': getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const s = d.server;
        ctx.$('ov-srv-status').textContent = s.status.toUpperCase();
        ctx.$('ov-srv-status').style.color = s.status === 'online' ? '#4aaa4a' : '#c8371a';
        const up = Math.floor(s.uptime);
        const h = Math.floor(up / 3600);
        const m = Math.floor((up % 3600) / 60);
        const sec = up % 60;
        ctx.$('ov-srv-uptime').textContent = h > 0 ? `Uptime: ${h}h ${m}m` : m > 0 ? `Uptime: ${m}m ${sec}s` : `Uptime: ${sec}s`;
        ctx.$('ov-online').textContent = s.online;
        ctx.$('ov-total').textContent = `${s.players} contas`;
        ctx.setText('nav-online-mini', `Online: ${Number(s.online || 0).toLocaleString('pt-BR')}`);
        ctx.$('ov-db-status').textContent = s.database.toUpperCase();
        ctx.$('ov-db-status').style.color = s.database === 'connected' ? '#4aaa4a' : '#c8371a';
        if (ctx.$('ov-db-sub')) ctx.$('ov-db-sub').textContent = `MongoDB / API ${s.xmppApi || '---'}`;
        pushPanelServer(s);
        ctx.setTone('nav-online-mini', Number(s.online || 0) > 0 ? 'ok' : 'info');
        ctx.updatePanelAlerts();
      } catch {}
    }

    function startMonitor() {
      fetchServerInfo();
      ctx.fetchPlayers();
      if (getMonitorInterval()) clearInterval(getMonitorInterval());
      setMonitorInterval(setInterval(() => {
        fetchServerInfo();
        ctx.fetchPlayers();
      }, 5000));
    }

    async function loadServices() {
      try {
        const r = await fetch('/api/services', { headers: { 'X-Auth-Token': getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const grid = ctx.$('services-grid');
        grid.innerHTML = '';
        Object.keys(d.services).forEach((k) => {
          const s = d.services[k];
          const unavailable = s.available === false;
          const statusText = unavailable
            ? 'MISSING'
            : (s.ready ? 'READY' : (s.onDemand ? 'ON-DEMAND' : (s.running ? 'RUNNING' : 'STOPPED')));
          const statusClass = unavailable ? 'stopped' : (s.ready ? 'running' : (s.onDemand ? 'ondemand' : 'stopped'));
          const metaText = unavailable ? (s.unavailableReason || 'Servico indisponivel') : null;
          const portTokens = [];
          if (s.ports && Object.keys(s.ports).length) {
            portTokens.push(Object.keys(s.ports).map((p) => `${p}:${s.ports[p] ? 'OK' : 'OFF'}`).join(' '));
          }
          if (s.managedPid) {
            portTokens.push(`pid:${s.managedPid}`);
          } else if (s.managed) {
            portTokens.push('processo gerenciado');
          }
          if (Number(s.externalCount || 0) > 0) {
            portTokens.push(`${Number(s.externalCount).toLocaleString('pt-BR')} proc. externo(s)`);
          }
          if (s.onDemand && !s.running && !unavailable) portTokens.push('sob demanda (abre quando cria sala)');
          const portText = portTokens.length ? portTokens.join(' | ') : 'sem porta monitorada';
          const startDisabled = unavailable ? 'disabled' : '';
          const card = document.createElement('div');
          card.className = 'service-card';
          card.dataset.serviceId = k;
          card.style.borderLeftColor = s.color;
          card.innerHTML = `
            <div class="sc-header">
              <div class="sc-name">${s.name}</div>
              <div class="sc-status ${statusClass}">${statusText}</div>
            </div>
            <div class="sc-meta">${ctx.esc(metaText || portText)}</div>
            <div class="sc-actions">
              <button class="sc-btn" ${startDisabled} onclick="svcAction('${k}','start', this)">INICIAR</button>
              <button class="sc-btn stop" onclick="svcAction('${k}','stop', this)">PARAR</button>
              <button class="sc-btn" ${startDisabled} onclick="svcAction('${k}','restart', this)">REINICIAR</button>
            </div>`;
          grid.appendChild(card);
        });
        ctx.updateServiceSummary(d.services);
        ctx.updateGlobalServiceActionProgress(d.services);
      } catch {}
    }

    async function svcAction(id, action, btnEl) {
      const label = action === 'stop' ? 'PARANDO...' : action === 'restart' ? 'REINICIANDO...' : 'INICIANDO...';
      ctx.setBusy(btnEl, true, label);
      ctx.markServiceCardPending(id, action);
      try {
        const r = await fetch(`/api/service/${action}?id=${id}`, { headers: { 'X-Auth-Token': getToken() } });
        const d = await r.json().catch(() => ({}));
        if (!r.ok || d.success === false) {
          ctx.showToast(d.message || d.error || `Falha ao ${action} ${id}`, 'error');
          addLogEntry({
            id: 'admin',
            level: 'error',
            msg: d.message || d.error || `Falha ao executar ${action} em ${id}`,
            time: Date.now()
          });
        } else {
          ctx.showToast(`${id}: ${action} enviado`, 'success', 2200);
        }
        [600, 1500, 3200].forEach((ms) => setTimeout(loadServices, ms));
      } catch (e) {
        ctx.showToast(e.message || 'Falha na acao do servico', 'error');
        addLogEntry({ id: 'admin', level: 'error', msg: e.message || 'Falha na acao do servico', time: Date.now() });
      } finally {
        ctx.setBusy(btnEl, false);
      }
    }

    async function startAll() {
      ctx.setGlobalServiceAction('start');
      ctx.showToast('Inicializacao enviada. Subindo servicos + dedicados base.', 'warn', 2600);
      try {
        const r = await fetch('/api/services/startAll', { headers: { 'X-Auth-Token': getToken() } });
        if (!r.ok) throw new Error('resposta invalida do servidor');
        loadServices();
        [500, 1200, 2500, 5000, 10000, 20000, 35000].forEach((ms) => setTimeout(loadServices, ms));
        [1200, 4000, 12000, 25000, 40000].forEach((ms) => setTimeout(fetchServerInfo, ms));
        ctx.showToast('Comando INICIAR TUDO enviado', 'success');
      } catch (e) {
        ctx.setGlobalServiceAction(null);
        ctx.showToast(`Erro ao iniciar: ${e.message || 'falha de rede'}`, 'error');
      }
    }

    async function stopAll() {
      ctx.setGlobalServiceAction('stop');
      try {
        const r = await fetch('/api/services/stopAll', { headers: { 'X-Auth-Token': getToken() } });
        if (!r.ok) throw new Error('resposta invalida do servidor');
        loadServices();
        [500, 1200, 2500, 5000].forEach((ms) => setTimeout(loadServices, ms));
        setTimeout(fetchServerInfo, 1800);
        ctx.showToast('Comando PARAR TUDO enviado', 'warn');
      } catch (e) {
        ctx.setGlobalServiceAction(null);
        ctx.showToast(`Erro ao parar: ${e.message || 'falha de rede'}`, 'error');
      }
    }

    function connectLogStream() {
      const currentStream = getLogStream();
      if (currentStream) currentStream.close();
      const stream = new EventSource(`/api/logs/stream?token=${getToken()}`);
      setLogStream(stream);
      stream.onmessage = (e) => {
        try {
          const entry = JSON.parse(e.data);
          if (entry.id) addLogEntry(entry);
        } catch {}
      };
      stream.onerror = () => {};
      loadInitialLogs();
    }

    function isSessionDebugLog(msg) {
      const keywords = [
        '[SessionJoin]', '[MissionLoad]', '[StartSession]', '[GameroomAskServer]',
        '[Dedicated][setserver]', '[UserLogout][Dedicated]',
        'session_join', 'mission_load', 'mission_unload', 'startSession',
        'gameroom_askserver', 'setserver', 'dedicatedServerJid',
        'dedicatedServersObject', 'session.status', 'session_id'
      ];
      return keywords.some((kw) => msg.includes(kw));
    }

    function renderLogsFromPayload(logsPayload) {
      const viewer = ctx.$('log-viewer');
      viewer.innerHTML = '';
      const filter = ctx.$('log-filter').value;
      const allLogs = [];
      Object.keys(logsPayload || {}).forEach((id) => {
        (logsPayload[id] || []).forEach((l) => {
          if (filter === 'all') {
            allLogs.push(l);
          } else if (filter === 'session_debug') {
            if (isSessionDebugLog(l.msg)) allLogs.push(l);
          } else if (l.id === filter) {
            allLogs.push(l);
          }
        });
      });
      allLogs.sort((a, b) => a.time - b.time);
      allLogs.forEach((l) => addLogEntry(l));
      if (!allLogs.length) viewer.innerHTML = '<div class="empty-state">Nenhum log registrado</div>';
    }

    async function loadInitialLogs() {
      try {
        const r = await fetch('/api/logs', { headers: { 'X-Auth-Token': getToken() } });
        const d = await r.json();
        if (d.success) renderLogsFromPayload(d.logs || {});
      } catch {}
    }

    async function loadAllLogs() {
      try {
        const r = await fetch('/api/logs', { headers: { 'X-Auth-Token': getToken() } });
        const d = await r.json();
        if (d.success) renderLogsFromPayload(d.logs || {});
      } catch {}
    }

    function addLogEntry(entry) {
      const viewer = ctx.$('log-viewer');
      const filter = ctx.$('log-filter').value;

      if (filter === 'session_debug') {
        if (!isSessionDebugLog(entry.msg)) return;
      } else if (filter !== 'all' && entry.id !== filter) {
        return;
      }

      if (viewer.querySelector('.empty-state')) viewer.innerHTML = '';

      const div = document.createElement('div');
      const level = entry.level || 'stdout';
      div.className = `log-entry log-level-${level}` + (filter === 'session_debug' ? ' log-session-debug' : '');
      const time = new Date(entry.time).toLocaleTimeString();
      div.innerHTML = `<span class="ltime">[${time}]</span><span class="lservice">${ctx.esc(entry.id || 'log')}</span><span class="l${level}">${ctx.esc(entry.msg)}</span>`;
      viewer.appendChild(div);
      requestAnimationFrame(() => div.classList.add('log-entry-live'));
      if (level === 'error' || level === 'warn') {
        pushPanelRecentLog({ level, msg: entry.msg, id: entry.id });
        ctx.updatePanelAlerts();
      }

      const threshold = 50;
      if (viewer.scrollTop + viewer.clientHeight >= viewer.scrollHeight - threshold) {
        viewer.scrollTop = viewer.scrollHeight;
      }
    }

    function copyLogs() {
      const viewer = ctx.$('log-viewer');
      const entries = viewer.querySelectorAll('.log-entry');
      if (!entries.length) {
        ctx.showToast('Nenhum log para copiar', 'warn');
        return;
      }
      let text = '';
      entries.forEach((e) => { text += `${e.textContent}\n`; });
      navigator.clipboard.writeText(text).then(() => {
        ctx.showToast('Logs copiados para a area de transferencia', 'ok');
      }).catch(() => {
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        ctx.showToast('Logs copiados para a area de transferencia', 'ok');
      });
    }

    function clearLogs() {
      ctx.$('log-viewer').innerHTML = '<div class="empty-state">Logs limpos</div>';
      loadAllLogs();
    }

    return {
      fetchServerInfo,
      startMonitor,
      loadServices,
      svcAction,
      startAll,
      stopAll,
      connectLogStream,
      loadInitialLogs,
      loadAllLogs,
      addLogEntry,
      copyLogs,
      clearLogs,
      isSessionDebugLog
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.services = window.AdminPanelDomains.services || {};
  window.AdminPanelDomains.services.createServicesMonitoringDomain = createServicesMonitoringDomain;
})();
