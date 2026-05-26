(function initUiBaseDomain() {
  function createUiBaseDomain(ctx) {
    function showToast(message, type = 'success', timeout = 3200) {
      const stack = ctx.$('toast-stack');
      if (!stack || !message) return;
      const toastCounter = ctx.getToastCounter();
      const id = `toast-${Date.now()}-${toastCounter}`;
      ctx.setToastCounter(toastCounter + 1);
      const el = document.createElement('div');
      el.className = `toast toast-${type === 'error' ? 'error' : type === 'warn' ? 'warn' : 'success'}`;
      el.id = id;
      el.textContent = message;
      stack.appendChild(el);
      requestAnimationFrame(() => el.classList.add('show'));
      setTimeout(() => {
        el.classList.remove('show');
        setTimeout(() => el.remove(), 220);
      }, timeout);
    }

    function setStatusTone(el, tone) {
      if (!el) return;
      el.classList.remove('path-status-info', 'path-status-ok', 'path-status-warn', 'path-status-danger');
      el.classList.add(`path-status-${tone || 'info'}`);
    }

    function setBusy(target, busy, label) {
      const el = typeof target === 'string' ? ctx.$(target) : target;
      if (!el) return;
      if (busy) {
        if (!el.dataset.idleText) el.dataset.idleText = el.textContent;
        el.disabled = true;
        el.classList.add('is-busy');
        if (label) el.textContent = label;
      } else {
        el.disabled = false;
        el.classList.remove('is-busy');
        if (el.dataset.idleText) el.textContent = el.dataset.idleText;
      }
    }

    function setGlobalActionBusy(actionName, busy, label) {
      document.querySelectorAll(`[data-global-action="${actionName}"]`).forEach((el) => setBusy(el, busy, label));
    }

    function markServiceCardPending(id, action) {
      const card = document.querySelector(`.service-card[data-service-id="${id}"]`);
      if (!card) return;
      card.classList.add('service-pending');
      card.dataset.pendingAction = action;
      const status = card.querySelector('.sc-status');
      if (status) {
        status.textContent = action === 'stop' ? 'STOPPING' : action === 'restart' ? 'RESTARTING' : 'STARTING';
        status.classList.remove('running', 'stopped');
        status.classList.add('pending');
      }
    }

    function setGlobalActionFeedback(message, tone) {
      const el = ctx.$('global-action-feedback');
      if (!el) return;
      if (!message) {
        el.textContent = '';
        el.classList.add('hidden');
        el.classList.remove('running', 'success', 'warn');
        return;
      }
      el.textContent = message;
      el.classList.remove('hidden', 'running', 'success', 'warn');
      el.classList.add(tone || 'running');
    }

    function setGlobalServiceAction(type) {
      if (!type) {
        ctx.setGlobalServiceActionState(null);
        setGlobalActionBusy('start-all', false);
        setGlobalActionBusy('stop-all', false);
        setGlobalActionFeedback('');
        document.body.classList.remove('global-service-pending');
        return;
      }

      ctx.setGlobalServiceActionState({ type, startedAt: Date.now(), doneNotified: false });
      const starting = type === 'start';
      setGlobalActionBusy('start-all', true, starting ? 'INICIANDO...' : 'INICIAR TUDO');
      setGlobalActionBusy('stop-all', true, starting ? 'PARAR TUDO' : 'PARANDO...');
      setGlobalActionFeedback(starting ? 'Inicializando servicos...' : 'Parando servicos...', 'running');
      document.body.classList.add('global-service-pending');
    }

    function updateGlobalServiceActionProgress(services) {
      const globalServiceActionState = ctx.getGlobalServiceActionState();
      if (!globalServiceActionState || !services) return;
      const allAvailable = Object.values(services).filter((s) => s.available !== false);
      const values = globalServiceActionState.type === 'start'
        ? allAvailable.filter((s) => !s.onDemand)
        : allAvailable;
      const total = values.length;
      const ready = values.filter((s) => !!s.ready).length;

      if (globalServiceActionState.type === 'start') {
        setGlobalActionFeedback(`Inicializando servicos... ${ready}/${total} prontos`, 'running');
        if (total > 0 && ready === total) {
          setGlobalActionFeedback('Todos os servicos estao prontos.', 'success');
          if (!globalServiceActionState.doneNotified) {
            globalServiceActionState.doneNotified = true;
            showToast('Todos os servicos estao online', 'success', 3000);
          }
          setTimeout(() => setGlobalServiceAction(null), 1400);
        }
        return;
      }

      const stillRunning = ready;
      setGlobalActionFeedback(`Parando servicos... ${stillRunning}/${total} ainda ativos`, 'warn');
      if (stillRunning === 0) {
        setGlobalActionFeedback('Todos os servicos foram parados.', 'success');
        if (!globalServiceActionState.doneNotified) {
          globalServiceActionState.doneNotified = true;
          showToast('Todos os servicos foram parados', 'warn', 3000);
        }
        setTimeout(() => setGlobalServiceAction(null), 1400);
      }
    }

    function renderPanelState(activeTab) {
      document.querySelectorAll('.nav-group').forEach((group) => {
        group.classList.toggle('active-group', !!group.querySelector(`.tab[data-tab="${activeTab}"]`));
      });
      const active = document.querySelector(`.tab[data-tab="${activeTab}"]`);
      const pageName = active ? active.textContent.trim() : activeTab;
      document.body.dataset.activeTab = activeTab || 'overview';
      document.querySelectorAll('.tab-content').forEach((panel) => {
        if (panel.classList.contains('active')) panel.dataset.pageTitle = pageName;
      });
    }

    function filterNavigation(raw) {
      const query = String(raw || '').trim().toLowerCase();
      document.querySelectorAll('.nav-group').forEach((group) => {
        let hits = 0;
        group.querySelectorAll('.tab').forEach((tab) => {
          const text = tab.textContent.trim().toLowerCase();
          const match = !query || text.includes(query) || String(tab.dataset.tab || '').includes(query);
          tab.classList.toggle('nav-filtered', !match);
          tab.classList.toggle('nav-match', !!query && match);
          if (match) hits++;
        });
        group.classList.toggle('nav-group-filtered', !!query && hits === 0);
        if (query && hits > 0) group.classList.remove('collapsed');
      });
    }

    function initDesktopUx() {
      document.querySelectorAll('.nav-group-label').forEach((label) => {
        label.setAttribute('role', 'button');
        label.setAttribute('tabindex', '0');
        const toggle = () => {
          const group = label.closest('.nav-group');
          if (!group || group.classList.contains('active-group')) return;
          group.classList.toggle('collapsed');
        };
        label.addEventListener('click', toggle);
        label.addEventListener('keydown', (e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            toggle();
          }
        });
      });

      const search = ctx.$('nav-search');
      if (search) {
        search.addEventListener('input', () => filterNavigation(search.value));
        search.addEventListener('keydown', (e) => {
          if (e.key === 'Enter') {
            const first = document.querySelector('.tab.nav-match, .tab:not(.nav-filtered)');
            if (first && first.dataset.tab) {
              ctx.switchTab(first.dataset.tab);
              search.blur();
            }
          } else if (e.key === 'Escape') {
            search.value = '';
            filterNavigation('');
            search.blur();
          }
        });
      }

      window.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
          const el = ctx.$('nav-search');
          if (el) {
            e.preventDefault();
            el.focus();
            el.select();
          }
        }
      });
    }

    function setText(id, value) {
      const el = ctx.$(id);
      if (el) el.textContent = value;
    }

    function setTone(id, tone) {
      const el = ctx.$(id);
      if (!el) return;
      el.classList.remove('tone-ok', 'tone-warn', 'tone-bad', 'tone-info');
      el.classList.add(`tone-${tone || 'info'}`);
    }

    function updatePanelAlerts() {
      const wrap = ctx.$('panel-alerts');
      if (!wrap) return;
      const alerts = [];
      const panelRuntimeState = ctx.getPanelRuntimeState();
      const server = panelRuntimeState.server;
      const services = panelRuntimeState.services;

      if (server) {
        if (server.status !== 'online') alerts.push({ tone: 'bad', text: 'Servidor principal offline.' });
        if (server.database !== 'connected') alerts.push({ tone: 'bad', text: 'MongoDB/API nao esta conectado.' });
      }

      if (services) {
        const values = Object.values(services);
        const missing = values.filter((s) => s.available === false);
        const stopped = values.filter((s) => s.available !== false && !s.ready && !s.onDemand);
        if (missing.length) alerts.push({ tone: 'bad', text: `${missing.length} servico(s) com arquivo/caminho ausente.` });
        if (stopped.length) alerts.push({ tone: 'warn', text: `${stopped.length} servico(s) parados ou nao prontos.` });
      }

      const recentLog = panelRuntimeState.recentLog;
      if (recentLog && recentLog.level === 'error') {
        alerts.push({ tone: 'bad', text: `Erro recente (${recentLog.id || 'servico'}): ${String(recentLog.msg || '').slice(0, 72)}` });
      }

      if (!alerts.length) {
        wrap.innerHTML = '<div class="panel-alert ok">Sistema estavel. Nenhum alerta critico.</div>';
        return;
      }

      wrap.innerHTML = alerts.slice(0, 4).map((a) => `<div class="panel-alert ${a.tone}">${ctx.esc(a.text)}</div>`).join('');
    }

    function updateServiceSummary(services) {
      const values = Object.values(services || {});
      const available = values.filter((s) => s.available !== false && !s.onDemand);
      const ready = available.filter((s) => s.ready);
      const missing = values.filter((s) => s.available === false);
      setText('ov-svc-running', `${ready.length}/${available.length}`);
      setText('ov-svc-down', String(Math.max(0, available.length - ready.length)));
      setText('ov-svc-missing', String(missing.length));
      setTone('ov-svc-running', ready.length === available.length ? 'ok' : (ready.length > 0 ? 'warn' : 'bad'));
      setTone('ov-svc-down', (available.length - ready.length) > 0 ? 'warn' : 'ok');
      setTone('ov-svc-missing', missing.length > 0 ? 'bad' : 'ok');

      const panelRuntimeState = ctx.getPanelRuntimeState();
      panelRuntimeState.services = services;
      ctx.setPanelRuntimeState(panelRuntimeState);
      updatePanelAlerts();
    }

    function renderSkeleton(targetId, count = 4) {
      const el = ctx.$(targetId);
      if (!el) return;
      el.innerHTML = Array.from({ length: count }).map(() => '<div class="skel-line"></div>').join('');
    }

    function formatDurationMs(ms) {
      const total = Math.max(0, Math.floor((Number(ms) || 0) / 1000));
      const h = Math.floor(total / 3600);
      const m = Math.floor((total % 3600) / 60);
      const s = total % 60;
      if (h > 0) return `${h}h ${m}m`;
      if (m > 0) return `${m}m ${s}s`;
      return `${s}s`;
    }

    function confirmDanger(message) {
      return window.confirm(message || 'Tem certeza?');
    }

    function num(n) { return (n || 0).toLocaleString('pt-BR'); }

    function parseStrictIntInput(value) {
      if (value === null || value === undefined) return null;
      const text = String(value).trim();
      if (!text || !/^-?\d+$/.test(text)) return null;
      const parsed = Number.parseInt(text, 10);
      return Number.isFinite(parsed) ? parsed : null;
    }

    function showResult(msg, isError) {
      const el = ctx.$('cmd-result');
      if (!el) return;
      el.textContent = msg;
      el.className = 'cmd-result' + (isError ? ' error' : '');
      el.classList.remove('hidden');
      setTimeout(() => el.classList.add('hidden'), 5000);
    }

    function esc(s) { return String(s).replace(/[<>&]/g, (c) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;' }[c])); }

    return {
      showToast,
      setStatusTone,
      setBusy,
      setGlobalActionBusy,
      markServiceCardPending,
      setGlobalActionFeedback,
      setGlobalServiceAction,
      updateGlobalServiceActionProgress,
      renderPanelState,
      initDesktopUx,
      filterNavigation,
      setText,
      setTone,
      updatePanelAlerts,
      updateServiceSummary,
      renderSkeleton,
      formatDurationMs,
      confirmDanger,
      num,
      parseStrictIntInput,
      showResult,
      esc
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.ui = window.AdminPanelDomains.ui || {};
  window.AdminPanelDomains.ui.createUiBaseDomain = createUiBaseDomain;
})();
