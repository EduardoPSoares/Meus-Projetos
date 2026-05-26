(function initLauncherSyncDomain() {
  function createLauncherSyncDomain(ctx) {
    async function browseLauncherSourceDir() {
      const input = ctx.$('launcher-source-edit');
      const btn = ctx.$('btn-browse-launcher-source-dir');
      ctx.setBusy(btn, true, 'ABRINDO...');
      try {
        const r = await fetch('/api/launcher/browse-source-dir', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ default_path: input ? input.value.trim() : '' })
        });
        const d = await r.json();
        if (!d.success) {
          if (!d.cancelled) ctx.showLauncherVersionResult('Erro: ' + (d.error || 'Falha ao selecionar a pasta.'), true);
          return;
        }
        if (input) input.value = d.path || '';
        await saveLauncherSourceDir();
      } catch (err) {
        ctx.showLauncherVersionResult('Erro: ' + err.message, true);
      } finally {
        ctx.setBusy(btn, false);
      }
    }

    async function syncLauncherFromCdn() {
      const input = ctx.$('launcher-source-edit');
      const btn = ctx.$('btn-sync-launcher-cdn');
      const sourceDir = input ? input.value.trim() : '';
      if (!sourceDir) return ctx.showLauncherVersionResult('Selecione ou informe a pasta do launcher antes de sincronizar.', true);
      ctx.setBusy(btn, true, 'SINCRONIZANDO...');
      ctx.startDevSyncProgressPolling('launcher');
      let started = false;
      try {
        const r = await fetch('/api/launcher/sync-from-cdn', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ source_dir: sourceDir, launcher_prefix: 'warface-launcher' })
        });
        const d = await r.json();
        await ctx.pollDevSyncProgress('launcher', true);
        if (!d.success) return ctx.showLauncherVersionResult('Erro: ' + (d.error || 'Falha ao sincronizar o launcher.'), true);
        if (d.started) {
          started = true;
          ctx.showLauncherVersionResult('Sincronização do launcher iniciada. Acompanhe a barra de progresso.', false);
          return;
        }
        ctx.showLauncherVersionResult(`Launcher sincronizado com o CDN v${d.version}: ${d.downloaded || 0} baixados, ${d.removed || 0} removidos.`, false);
        ctx.loadLauncherRefInfo();
      } catch (err) {
        await ctx.pollDevSyncProgress('launcher', true);
        ctx.showLauncherVersionResult('Erro: ' + err.message, true);
      } finally {
        if (!started) ctx.clearDevSyncProgressTimer('launcher');
        ctx.setBusy(btn, false);
      }
    }

    async function saveLauncherSourceDir() {
      const input = ctx.$('launcher-source-edit');
      const btn = ctx.$('btn-save-launcher-source-dir');
      const sourceDir = input ? input.value.trim() : '';
      if (!sourceDir) return ctx.showLauncherVersionResult('Selecione ou informe a pasta do launcher antes de salvar.', true);
      ctx.setBusy(btn, true, 'SALVANDO...');
      try {
        const r = await fetch('/api/launcher/source-dir/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ source_dir: sourceDir })
        });
        const d = await r.json();
        if (!d.success) return ctx.showLauncherVersionResult('Erro: ' + (d.error || 'Falha ao salvar a pasta.'), true);
        if (input) input.value = d.path || sourceDir;
        ctx.showLauncherVersionResult('Pasta do launcher salva com sucesso.', false);
        ctx.loadLauncherRefInfo();
      } catch (err) {
        ctx.showLauncherVersionResult('Erro: ' + err.message, true);
      } finally {
        ctx.setBusy(btn, false);
      }
    }

    async function loadGameRefInfo() {
      try {
        const [refR, verR] = await Promise.all([
          fetch('/api/game/ref-info', { headers: { 'X-Auth-Token': ctx.getToken() } }),
          fetch('/api/public/game-version')
        ]);
        const ref = await refR.json();
        const ver = await verR.json();

        if (ref.success) {
          if (ctx.$('ref-dir-path')) ctx.$('ref-dir-path').textContent = ref.path;
          if (ctx.$('game-root-path-view')) ctx.$('game-root-path-view').textContent = ref.sourcePath || ref.path || 'Pasta não configurada';
          if (ctx.$('ref-file-count')) ctx.$('ref-file-count').textContent = (ref.fileCount || 0).toLocaleString('pt-BR');
          const sizeMB = ((ref.totalSize || 0) / 1024 / 1024);
          if (ctx.$('ref-dir-size')) ctx.$('ref-dir-size').textContent = sizeMB > 1024 ? (sizeMB / 1024).toFixed(2) + ' GB' : sizeMB.toFixed(0) + ' MB';
          if (ctx.$('publish-source-dir')) ctx.$('publish-source-dir').value = ref.sourcePath || '';
          if (ctx.$('publish-source-edit')) ctx.$('publish-source-edit').value = ref.sourcePath || '';
          if (ref.r2PublicBaseUrl) {
            if (ctx.$('patch-base-url') && !ctx.$('patch-base-url').value) ctx.$('patch-base-url').value = ref.r2PublicBaseUrl;
            if (ctx.$('game-cdn-base-url') && (!ctx.$('game-cdn-base-url').value || ctx.$('game-cdn-base-url').value.includes('/cdn/game/'))) ctx.$('game-cdn-base-url').value = ref.r2PublicBaseUrl;
          }
          if (ctx.$('publish-source-label')) {
            ctx.$('publish-source-label').textContent = ref.sourcePath ? `Pasta do desenvolvedor do jogo: ${ref.sourcePath}` : 'Pasta do desenvolvedor do jogo: não configurada';
            ctx.setStatusTone(ctx.$('publish-source-label'), ref.sourcePath ? 'info' : 'warn');
          }
          if (ctx.$('game-dev-sync-label')) {
            const sync = ref.devSync || {};
            ctx.$('game-dev-sync-label').textContent = sync.synced ? `Pasta do desenvolvedor sincronizada: OK (local ${sync.localVersion || '--'} / CDN ${sync.remoteVersion || '--'})` : `Pasta do desenvolvedor desatualizada: sincronize antes de publicar (local ${sync.localVersion || '--'} / CDN ${sync.remoteVersion || '--'})`;
            ctx.setStatusTone(ctx.$('game-dev-sync-label'), sync.synced ? 'ok' : 'danger');
          }
        }

        if (ver && ver.version) {
          const parts = ver.version.split('.').map(Number);
          parts[parts.length - 1] = (parts[parts.length - 1] || 0) + 1;
          if (ctx.$('patch-version-input')) ctx.$('patch-version-input').value = parts.join('.');
          if (ctx.$('publish-version-input')) ctx.$('publish-version-input').value = parts.join('.');
        }
        return;
      } catch {}
      if (ctx.$('ref-dir-path')) ctx.$('ref-dir-path').textContent = 'WarfaceSurvivor_Ref (no servidor)';
      if (ctx.$('ref-file-count')) ctx.$('ref-file-count').textContent = '-';
      if (ctx.$('ref-dir-size')) ctx.$('ref-dir-size').textContent = '-';
      if (ctx.$('publish-source-label')) ctx.$('publish-source-label').textContent = 'Pasta do desenvolvedor do jogo: erro ao carregar a configuração';
      ctx.setStatusTone(ctx.$('publish-source-label'), 'danger');
    }

    async function syncGameFromCdn() {
      const sourceDir = ctx.$('publish-source-edit') ? ctx.$('publish-source-edit').value.trim() : (ctx.$('publish-source-dir') ? ctx.$('publish-source-dir').value.trim() : '');
      const btn = ctx.$('btn-sync-game-cdn');
      const summary = ctx.$('publish-folder-summary');
      if (!sourceDir) return ctx.showPatchResult('Selecione ou informe a pasta raiz do client antes de sincronizar.', true);
      ctx.setBusy(btn, true, 'SINCRONIZANDO...');
      ctx.startDevSyncProgressPolling('game');
      let started = false;
      if (summary) summary.textContent = 'Sincronizando a pasta local com a versão publicada no CDN...';
      try {
        const r = await fetch('/api/game/sync-from-cdn', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ source_dir: sourceDir })
        });
        const d = await r.json();
        await ctx.pollDevSyncProgress('game', true);
        if (!d.success) {
          const message = 'Erro: ' + (d.error || 'Falha ao sincronizar o client.');
          ctx.showPatchResult(message, true);
          if (summary) summary.textContent = message;
          return;
        }
        if (d.started) {
          started = true;
          const message = 'Sincronização do jogo iniciada. Acompanhe a barra de progresso.';
          ctx.showPatchResult(message, false);
          if (summary) summary.textContent = message;
          return;
        }
        const message = `Client sincronizado com o CDN v${d.version}: ${d.downloaded || 0} baixados, ${d.removed || 0} removidos.`;
        ctx.showPatchResult(message, false);
        if (summary) summary.textContent = message;
        loadGameRefInfo();
      } catch (err) {
        await ctx.pollDevSyncProgress('game', true);
        const message = 'Erro: ' + err.message;
        ctx.showPatchResult(message, true);
        if (summary) summary.textContent = message;
      } finally {
        if (!started) ctx.clearDevSyncProgressTimer('game');
        ctx.setBusy(btn, false);
      }
    }

    async function browseGameSourceDir() {
      const input = ctx.$('publish-source-edit');
      const btn = ctx.$('btn-browse-source-dir');
      const summary = ctx.$('publish-folder-summary');
      ctx.setBusy(btn, true, 'ABRINDO...');
      try {
        const r = await fetch('/api/game/browse-source-dir', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ default_path: input ? input.value.trim() : '' })
        });
        const d = await r.json();
        if (!d.success) {
          if (!d.cancelled) {
            const message = 'Erro: ' + (d.error || 'Falha ao selecionar a pasta.');
            ctx.showPatchResult(message, true);
            if (summary) summary.textContent = message;
          }
          return;
        }
        if (input) input.value = d.path || '';
        await saveGameSourceDir();
      } catch (err) {
        const message = 'Erro: ' + err.message;
        ctx.showPatchResult(message, true);
        if (summary) summary.textContent = message;
      } finally {
        ctx.setBusy(btn, false);
      }
    }

    async function saveGameSourceDir() {
      const input = ctx.$('publish-source-edit');
      const btn = ctx.$('btn-save-source-dir');
      const summary = ctx.$('publish-folder-summary');
      const sourceDir = input ? input.value.trim() : '';
      if (!sourceDir) {
        const message = 'Selecione ou informe a pasta raiz do client antes de salvar.';
        ctx.showPatchResult(message, true);
        if (summary) summary.textContent = message;
        return;
      }

      ctx.setBusy(btn, true, 'SALVANDO...');
      try {
        const r = await fetch('/api/game/source-dir/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ source_dir: sourceDir })
        });
        const d = await r.json();
        if (!d.success) {
          const message = 'Erro: ' + (d.error || 'Falha ao salvar a pasta.');
          ctx.showPatchResult(message, true);
          if (summary) summary.textContent = message;
          return;
        }
        if (ctx.$('publish-source-dir')) ctx.$('publish-source-dir').value = d.path || sourceDir;
        if (input) input.value = d.path || sourceDir;
        if (ctx.$('publish-source-label')) ctx.$('publish-source-label').textContent = `Pasta do desenvolvedor do jogo: ${d.path || sourceDir}`;
        ctx.setStatusTone(ctx.$('publish-source-label'), 'info');
        const message = 'Pasta do desenvolvedor do jogo salva. As próximas publicações usarão esse caminho.';
        ctx.showPatchResult(message, false);
        ctx.showToast('Pasta do desenvolvedor salva', 'success');
        if (summary) summary.textContent = message;
        loadGameRefInfo();
      } catch (err) {
        const message = 'Erro: ' + err.message;
        ctx.showPatchResult(message, true);
        if (summary) summary.textContent = message;
      } finally {
        ctx.setBusy(btn, false);
      }
    }

    return {
      browseLauncherSourceDir,
      syncLauncherFromCdn,
      saveLauncherSourceDir,
      loadGameRefInfo,
      syncGameFromCdn,
      browseGameSourceDir,
      saveGameSourceDir
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.launcher = window.AdminPanelDomains.launcher || {};
  window.AdminPanelDomains.launcher.createLauncherSyncDomain = createLauncherSyncDomain;
})();
