(function initLauncherCdnDomain() {
  function createLauncherCdnDomain(ctx) {
    function setLauncherCounterText(text) {
      const a = ctx.$('lpr-counter');
      const b = ctx.$('lpr-counter-news');
      if (a) a.textContent = text;
      if (b) b.textContent = text;
    }

    function getLauncherResultEl() {
      const systemTab = ctx.$('tab-launcher');
      if (systemTab && systemTab.classList.contains('active')) return ctx.$('launcher-result-system');
      const newsTab = ctx.$('tab-launcher-news');
      if (newsTab && newsTab.classList.contains('active')) return ctx.$('launcher-result-news');
      return ctx.$('launcher-result-system') || ctx.$('launcher-result-news');
    }

    function formatPatchSize(bytes) {
      const n = Number(bytes || 0);
      if (!Number.isFinite(n) || n <= 0) return '0 B';
      const units = ['B', 'KB', 'MB', 'GB', 'TB'];
      let size = n;
      let idx = 0;
      while (size >= 1024 && idx < units.length - 1) {
        size /= 1024;
        idx++;
      }
      return `${size.toFixed(idx >= 2 ? 2 : 0)} ${units[idx]}`;
    }

    function formatUpdateDate(value) {
      if (!value) return '--';
      const d = new Date(value);
      if (Number.isNaN(d.getTime())) return '--';
      return d.toLocaleString('pt-BR');
    }

    function showVersionResult(msg, isError) {
      const el = ctx.$('version-result');
      if (!el) return;
      el.textContent = msg;
      el.className = 'cmd-result' + (isError ? ' error' : '');
      el.classList.remove('hidden');
      setTimeout(() => el.classList.add('hidden'), 3500);
    }

    function showLauncherVersionResult(msg, isError) {
      const el = ctx.$('launcher-version-result');
      if (!el) return;
      el.textContent = msg;
      el.className = 'cmd-result' + (isError ? ' error' : '');
      el.classList.remove('hidden');
      setTimeout(() => el.classList.add('hidden'), 3500);
    }

    async function loadVersions() {
      try {
        const gr = await fetch('/api/public/game-version');
        const gv = await gr.json();
        if (gv && gv.version) {
          if (ctx.$('game-version-input')) ctx.$('game-version-input').value = gv.version;
          if (ctx.$('game-manifest-url')) ctx.$('game-manifest-url').value = gv.manifest_url || '';
          if (ctx.$('game-cdn-base-url')) ctx.$('game-cdn-base-url').value = gv.base_url || '';
          if (ctx.$('patch-base-url')) ctx.$('patch-base-url').value = gv.base_url || '';
          const notesInput = ctx.$('game-version-notes');
          if (notesInput) notesInput.value = gv.notes || '';
          const cur = ctx.$('cur-game-version');
          if (cur) cur.textContent = 'v' + gv.version;
          if (ctx.$('cur-game-file-count')) ctx.$('cur-game-file-count').textContent = (gv.file_count || 0).toLocaleString('pt-BR');
          if (ctx.$('cur-game-total-size')) ctx.$('cur-game-total-size').textContent = formatPatchSize(gv.total_size || 0);
          if (ctx.$('cur-game-cdn-status')) ctx.$('cur-game-cdn-status').textContent = gv.base_url && gv.base_url.startsWith('https://') ? 'R2 ONLINE' : 'LOCAL';
          if (ctx.$('publish-version-input') && !ctx.$('publish-version-input').value) ctx.$('publish-version-input').value = gv.version;
          const patchUrl = ctx.$('cur-game-patch-url');
          if (patchUrl) patchUrl.textContent = gv.manifest_url ? `Manifest: ${gv.manifest_url}` : 'Manifest não publicado';
          const required = ctx.$('cur-game-required');
          if (required) required.textContent = gv.required === false ? 'OPCIONAL' : 'OBRIGATÓRIO';
        }
      } catch {}
      ctx.loadPatchHistory();
    }

    async function loadLauncherVersions() {
      try {
        const r = await fetch('/api/public/launcher-version');
        const v = await r.json();
        if (!v || !v.version) return;
        if (ctx.$('launcher-version-input')) ctx.$('launcher-version-input').value = v.version;
        if (ctx.$('launcher-cdn-base-url')) ctx.$('launcher-cdn-base-url').value = v.base_url || '';
        if (ctx.$('launcher-version-notes')) ctx.$('launcher-version-notes').value = v.notes || '';
        if (ctx.$('launcher-publish-version-input')) ctx.$('launcher-publish-version-input').value = v.version;
        if (ctx.$('cur-launcher-version')) ctx.$('cur-launcher-version').textContent = 'v' + v.version;
        if (ctx.$('cur-launcher-required')) ctx.$('cur-launcher-required').textContent = v.required === false ? 'OPCIONAL' : 'OBRIGATÓRIO';
        if (ctx.$('cur-launcher-file-count')) ctx.$('cur-launcher-file-count').textContent = (v.file_count || 0).toLocaleString('pt-BR');
        if (ctx.$('cur-launcher-total-size')) ctx.$('cur-launcher-total-size').textContent = formatPatchSize(v.total_size || 0);
        if (ctx.$('cur-launcher-cdn-status')) ctx.$('cur-launcher-cdn-status').textContent = v.base_url && v.base_url.startsWith('https://') ? 'R2 ONLINE' : 'LOCAL';
        if (ctx.$('cur-launcher-patch-url')) ctx.$('cur-launcher-patch-url').textContent = v.manifest_url ? `Manifest: ${v.manifest_url}` : 'Manifest não publicado';
      } catch {}
      if (ctx.$('launcher-version-input') && !ctx.$('launcher-version-input').value) ctx.$('launcher-version-input').value = '1.0.0';
      if (ctx.$('launcher-publish-version-input') && !ctx.$('launcher-publish-version-input').value) ctx.$('launcher-publish-version-input').value = '1.0.0';
    }

    async function saveLauncherVersion() {
      const version = ctx.$('launcher-version-input') ? ctx.$('launcher-version-input').value.trim() : '';
      const baseUrl = ctx.$('launcher-cdn-base-url') ? ctx.$('launcher-cdn-base-url').value.trim() : '';
      const notes = ctx.$('launcher-version-notes') ? ctx.$('launcher-version-notes').value.trim() : '';
      if (!version) return showLauncherVersionResult('Digite uma versão.', true);
      try {
        const r = await fetch('/api/launcher/version/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ version, base_url: baseUrl, notes })
        });
        const d = await r.json();
        showLauncherVersionResult(d.success ? 'Versão do launcher salva com sucesso.' : 'Erro: ' + (d.error || ''), !d.success);
        if (d.success) {
          loadLauncherVersions();
          ctx.loadLauncherPatchHistory();
        }
      } catch (err) {
        showLauncherVersionResult('Erro: ' + err.message, true);
      }
    }

    async function loadLauncherRefInfo() {
      try {
        const r = await fetch('/api/launcher/ref-info', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        if (ctx.$('launcher-ref-path')) ctx.$('launcher-ref-path').textContent = d.path || '---';
        if (ctx.$('launcher-root-path-view')) ctx.$('launcher-root-path-view').textContent = d.sourcePath || 'Pasta do desenvolvedor não configurada';
        if (ctx.$('launcher-build-path-view')) ctx.$('launcher-build-path-view').textContent = d.sourcePath ? `${d.sourcePath}\\release` : 'release / dist / build';
        if (ctx.$('launcher-config-path-view')) ctx.$('launcher-config-path-view').textContent = d.sourcePath ? `${d.sourcePath}\\launcher-config.json` : 'launcher-config.json';
        if (ctx.$('launcher-images-path-view')) ctx.$('launcher-images-path-view').textContent = d.sourcePath ? `${d.sourcePath}\\launcher-images` : 'launcher-images';
        if (ctx.$('launcher-ref-count')) ctx.$('launcher-ref-count').textContent = (d.fileCount || 0).toLocaleString('pt-BR');
        if (ctx.$('launcher-ref-size')) ctx.$('launcher-ref-size').textContent = formatPatchSize(d.totalSize || 0);
        if (ctx.$('launcher-source-edit')) ctx.$('launcher-source-edit').value = d.sourcePath || '';
        if (ctx.$('launcher-ref-label')) ctx.$('launcher-ref-label').textContent = d.sourcePath ? `Pasta do desenvolvedor do launcher: ${d.sourcePath}` : 'Pasta do desenvolvedor do launcher: não configurada';
        ctx.setStatusTone(ctx.$('launcher-ref-label'), d.sourcePath ? 'info' : 'warn');
        if (ctx.$('launcher-dev-sync-label')) {
          const sync = d.devSync || {};
          ctx.$('launcher-dev-sync-label').textContent = sync.synced
            ? `Pasta do desenvolvedor sincronizada: OK (local ${sync.localVersion || '--'} / CDN ${sync.remoteVersion || '--'})`
            : `Pasta do desenvolvedor desatualizada: sincronize antes de publicar (local ${sync.localVersion || '--'} / CDN ${sync.remoteVersion || '--'})`;
          ctx.setStatusTone(ctx.$('launcher-dev-sync-label'), sync.synced ? 'ok' : 'danger');
        }
        if (ctx.$('launcher-cdn-base-url') && d.r2PublicBaseUrl && !ctx.$('launcher-cdn-base-url').value) ctx.$('launcher-cdn-base-url').value = d.r2PublicBaseUrl;
      } catch {}
    }

    function clearDevSyncProgressTimer(kind) {
      if (kind === 'launcher') {
        if (ctx.getLauncherSyncProgressTimer()) clearInterval(ctx.getLauncherSyncProgressTimer());
        ctx.setLauncherSyncProgressTimer(null);
        return;
      }
      if (ctx.getGameSyncProgressTimer()) clearInterval(ctx.getGameSyncProgressTimer());
      ctx.setGameSyncProgressTimer(null);
    }

    function renderDevSyncProgress(kind, progress = {}) {
      const prefix = kind === 'launcher' ? 'launcher-sync' : 'game-sync';
      const wrap = ctx.$(`${prefix}-progress-wrap`);
      if (!wrap) return;
      const percent = Math.max(0, Math.min(100, Math.round(Number(progress.percent || 0))));
      wrap.classList.remove('hidden');
      wrap.classList.toggle('error', progress.phase === 'error' || !!progress.error);
      if (ctx.$(`${prefix}-progress-label`)) ctx.$(`${prefix}-progress-label`).textContent = progress.message || 'Sincronizando com CDN';
      if (ctx.$(`${prefix}-progress-percent`)) ctx.$(`${prefix}-progress-percent`).textContent = percent + '%';
      if (ctx.$(`${prefix}-progress-fill`)) ctx.$(`${prefix}-progress-fill`).style.width = percent + '%';
      const current = progress.current_file ? ` - ${progress.current_file}` : '';
      if (ctx.$(`${prefix}-progress-phase`)) ctx.$(`${prefix}-progress-phase`).textContent = `${progress.phase || 'idle'}${current}`;
      const totalBytes = Number(progress.download_bytes_total || 0);
      const doneBytes = Number(progress.download_bytes_done || 0) + Number(progress.download_bytes_current || 0);
      const fileText = progress.total_files
        ? `${Number(progress.checked_files || 0).toLocaleString('pt-BR')}/${Number(progress.total_files || 0).toLocaleString('pt-BR')} verificados`
        : `${Number(progress.download_index || 0).toLocaleString('pt-BR')}/${Number(progress.download_total || 0).toLocaleString('pt-BR')} downloads`;
      if (ctx.$(`${prefix}-progress-bytes`)) ctx.$(`${prefix}-progress-bytes`).textContent = totalBytes ? `${formatPatchSize(doneBytes)} / ${formatPatchSize(totalBytes)}` : fileText;
    }

    async function pollDevSyncProgress(kind, force = false) {
      try {
        const endpoint = kind === 'launcher' ? '/api/launcher/sync-progress' : '/api/game/sync-progress';
        const d = await (await fetch(endpoint, { headers: { 'X-Auth-Token': ctx.getToken() } })).json();
        if (!d.success || !d.progress) return;
        if (force || d.progress.active || d.progress.done) renderDevSyncProgress(kind, d.progress);
        if (d.progress.done && !d.progress.active) {
          clearDevSyncProgressTimer(kind);
          if (kind === 'launcher') loadLauncherRefInfo();
          else ctx.loadGameRefInfo();
        }
      } catch {}
    }

    function startDevSyncProgressPolling(kind) {
      clearDevSyncProgressTimer(kind);
      renderDevSyncProgress(kind, { percent: 0, phase: 'start', message: 'Iniciando sincronização...' });
      if (kind === 'launcher') ctx.setLauncherSyncProgressTimer(setInterval(() => pollDevSyncProgress(kind, false), 700));
      else ctx.setGameSyncProgressTimer(setInterval(() => pollDevSyncProgress(kind, false), 700));
      pollDevSyncProgress(kind, true);
    }

    return {
      setLauncherCounterText,
      getLauncherResultEl,
      formatPatchSize,
      formatUpdateDate,
      showVersionResult,
      showLauncherVersionResult,
      loadVersions,
      loadLauncherVersions,
      saveLauncherVersion,
      loadLauncherRefInfo,
      clearDevSyncProgressTimer,
      renderDevSyncProgress,
      pollDevSyncProgress,
      startDevSyncProgressPolling
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.launcher = window.AdminPanelDomains.launcher || {};
  window.AdminPanelDomains.launcher.createLauncherCdnDomain = createLauncherCdnDomain;
})();
