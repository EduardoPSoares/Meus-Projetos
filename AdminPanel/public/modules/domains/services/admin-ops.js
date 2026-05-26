(function initServicesAdminOpsDomain() {
  function createServicesAdminOpsDomain(ctx) {
    async function loadMaintenance() {
      try {
        const r = await fetch('/api/maintenance', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        ctx.setMaintenanceEnabled(!!d.enabled);
        ctx.$('maint-toggle').classList.toggle('on', !!d.enabled);
        ctx.$('maint-status').textContent = d.enabled ? 'Ativado' : 'Desativado';
        ctx.$('maint-message').value = d.message || '';
      } catch {}
    }

    function toggleMaintenance() {
      const next = !ctx.getMaintenanceEnabled();
      ctx.setMaintenanceEnabled(next);
      const msg = ctx.$('maint-message').value.trim() || 'Servidor em manutencao. Tente novamente mais tarde.';
      ctx.$('maint-toggle').classList.toggle('on', next);
      ctx.$('maint-status').textContent = next ? 'Ativado' : 'Desativado';
      saveMaintenanceConfig(next, msg);
    }

    async function saveMaintenance() {
      const msg = ctx.$('maint-message').value.trim() || 'Servidor em manutencao. Tente novamente mais tarde.';
      await saveMaintenanceConfig(ctx.getMaintenanceEnabled(), msg);
    }

    async function saveMaintenanceConfig(enabled, message) {
      try {
        const r = await fetch('/api/maintenance/set', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ enabled, message })
        });
        const d = await r.json();
        const el = ctx.$('maint-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch {}
    }

    function applyQuickXP(multiplier) {
      const input = ctx.$('xp-multiplier');
      input.value = multiplier;
      input.setAttribute('data-current', multiplier);
    }

    async function loadXpMultiplier() {
      try {
        const r = await fetch('/api/xp', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        const input = ctx.$('xp-multiplier');
        const msgInput = ctx.$('xp-message');
        const durationInput = ctx.$('xp-duration-minutes');
        if (d.success && d.multiplier) {
          input.value = d.multiplier;
          input.setAttribute('data-current', d.multiplier);
          if (msgInput) msgInput.value = d.message || '';
          if (durationInput) durationInput.value = '';
          ctx.setXpEventState({ active: !!d.active, multiplier: Number(d.multiplier) || 1, message: d.message || '', startedAt: d.startedAt || null, expiresAt: d.expiresAt || null, temporary: !!d.temporary });
          ctx.updateXpEventStatusUi();
        } else {
          input.value = 1;
          input.setAttribute('data-current', 1);
          if (msgInput) msgInput.value = '';
          if (durationInput) durationInput.value = '';
          ctx.setXpEventState({ active: false, multiplier: 1, message: '', startedAt: null, expiresAt: null, temporary: false });
          ctx.updateXpEventStatusUi();
        }
      } catch (e) {
        console.warn('[XP] Falha ao carregar multiplicador:', e.message);
      }
    }

    async function setXP(event) {
      if (event) event.preventDefault();
      const input = ctx.$('xp-multiplier');
      const multiplier = ctx.parseStrictIntInput(input.value);
      const message = String((ctx.$('xp-message') && ctx.$('xp-message').value) || '').trim();
      const durationInput = ctx.$('xp-duration-minutes');
      const durationMinutes = durationInput && String(durationInput.value || '').trim() !== '' ? ctx.parseStrictIntInput(durationInput.value) : 0;
      const model = ctx.getPanelModel();
      const xpRange = (model.xp && model.xp.multiplier) || ctx.DEFAULT_PANEL_MODEL.xp.multiplier;
      if (multiplier === null || multiplier < xpRange.min || multiplier > xpRange.max) {
        ctx.showResult(`Multiplicador invalido (${xpRange.min} ~ ${xpRange.max})`, true);
        input.focus();
        return;
      }
      if (durationMinutes === null || durationMinutes < 0 || durationMinutes > 10080) {
        ctx.showResult('Duracao invalida (0 a 10080 minutos)', true);
        durationInput && durationInput.focus();
        return;
      }
      try {
        const r = await fetch('/api/xp', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ multiplier, message, duration_minutes: durationMinutes })
        });
        const d = await r.json();
        const el = ctx.$('xp-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
        if (d.success) {
          ctx.showToast('Evento XP aplicado', 'success');
          setTimeout(loadXpMultiplier, 200);
        }
      } catch (e) {
        ctx.showResult('Erro: ' + e.message, true);
      }
    }

    async function disableXPEvent() {
      try {
        ctx.setBusy('xp-disable-btn', true, 'DESATIVANDO...');
        const r = await fetch('/api/xp/disable', { method: 'POST', headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!r.ok || d.success === false) {
          ctx.showToast(d.error || 'Falha ao desativar evento XP', 'error');
          return;
        }
        ctx.showToast('Evento XP desativado', 'warn');
        await loadXpMultiplier();
      } catch (e) {
        ctx.showToast(`Erro ao desativar XP: ${e.message}`, 'error');
      } finally {
        ctx.setBusy('xp-disable-btn', false);
      }
    }

    async function loadAutoBroadcast() {
      try {
        const r = await fetch('/api/autobroadcast', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const cfg = d.config;
        ctx.setAutoBroadcastEnabled(!!cfg.enabled);
        ctx.$('ab-toggle').classList.toggle('on', !!cfg.enabled);
        ctx.$('ab-status').textContent = cfg.enabled ? 'Ativado' : 'Desativado';
        ctx.$('ab-interval').value = cfg.interval || 300;
        ctx.$('ab-message').value = cfg.message || '';
      } catch {}
    }

    function toggleAutoBroadcast() {
      const next = !ctx.getAutoBroadcastEnabled();
      ctx.setAutoBroadcastEnabled(next);
      ctx.$('ab-toggle').classList.toggle('on', next);
      ctx.$('ab-status').textContent = next ? 'Ativado' : 'Desativado';
    }

    async function saveAutoBroadcast() {
      const enabled = ctx.getAutoBroadcastEnabled();
      const interval = parseInt(ctx.$('ab-interval').value, 10) || 300;
      const message = ctx.$('ab-message').value.trim();
      try {
        const r = await fetch('/api/autobroadcast/set', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ enabled, interval, message })
        });
        const d = await r.json();
        const el = ctx.$('ab-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
        if (d.success) ctx.loadAchievementLists();
      } catch {}
    }

    async function createBackup() {
      const el = ctx.$('backup-result');
      el.textContent = 'Criando backup...';
      el.className = 'cmd-result';
      el.classList.remove('hidden');
      try {
        const r = await fetch('/api/backup', { method: 'POST', headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        if (d.success) listBackups();
      } catch (e) {
        el.textContent = 'Erro: ' + e.message;
        el.className = 'cmd-result error';
      }
    }

    async function listBackups() {
      try {
        const r = await fetch('/api/backup/list', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        const list = ctx.$('backup-list');
        if (!d.success || !d.backups || !d.backups.length) {
          list.innerHTML = '<div class="empty-state">Nenhum backup encontrado</div>';
          return;
        }
        list.innerHTML = d.backups.map(b => {
          const date = new Date(b.date).toLocaleString('pt-BR');
          const size = (b.size / 1048576).toFixed(2) + ' MB';
          return `<div class="ban-item" style="border-left-color:#2a6a9a"><span class="ban-nick">${ctx.esc(b.name)}</span><span class="ban-info">${size} - ${date}</span></div>`;
        }).join('');
      } catch {}
    }

    async function loadNotes() {
      const nick = ctx.$('ins-nick').value.trim();
      if (!nick) return;
      try {
        const r = await fetch(`/api/notes/get?nick=${encodeURIComponent(nick)}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (d.success) ctx.$('ins-notes').value = d.notes || '';
      } catch {}
    }

    async function saveNotes() {
      const nick = ctx.$('ins-nick').value.trim();
      const notes = ctx.$('ins-notes').value;
      if (!nick) return;
      try {
        const r = await fetch('/api/notes/set', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick, notes })
        });
        const d = await r.json();
        const el = ctx.$('notes-result');
        el.textContent = d.success ? 'Salvo!' : 'Erro: ' + (d.error || '');
        el.style.color = d.success ? '#4aaa4a' : '#c8371a';
        setTimeout(() => { el.textContent = ''; }, 3000);
      } catch {}
    }

    return {
      loadMaintenance,
      toggleMaintenance,
      saveMaintenance,
      saveMaintenanceConfig,
      applyQuickXP,
      loadXpMultiplier,
      setXP,
      disableXPEvent,
      loadAutoBroadcast,
      toggleAutoBroadcast,
      saveAutoBroadcast,
      createBackup,
      listBackups,
      loadNotes,
      saveNotes
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.services = window.AdminPanelDomains.services || {};
  window.AdminPanelDomains.services.createAdminOpsDomain = createServicesAdminOpsDomain;
})();
