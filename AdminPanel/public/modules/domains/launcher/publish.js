(function initLauncherPublishDomain() {
  function createLauncherPublishDomain(ctx) {
    function clearPublishProgressTimer() {
      if (ctx.getPublishProgressTimer()) {
        clearInterval(ctx.getPublishProgressTimer());
        ctx.setPublishProgressTimer(null);
      }
    }

    function renderPublishProgress(progress = {}) {
      const wrap = ctx.$('publish-progress-wrap');
      if (!wrap) return;
      const percent = Math.max(0, Math.min(100, Math.round(Number(progress.percent || 0))));
      const label = ctx.$('publish-progress-label');
      const pct = ctx.$('publish-progress-percent');
      const fill = ctx.$('publish-progress-fill');
      const phase = ctx.$('publish-progress-phase');
      const bytes = ctx.$('publish-progress-bytes');

      wrap.classList.remove('hidden');
      wrap.classList.toggle('error', progress.phase === 'error' || !!progress.error);
      if (label) label.textContent = progress.message || 'Publicacao em andamento';
      if (pct) pct.textContent = percent + '%';
      if (fill) fill.style.width = percent + '%';

      const current = progress.current_file ? ` - ${progress.current_file}` : '';
      if (phase) phase.textContent = `${progress.phase || 'idle'}${current}`;

      const totalBytes = Number(progress.upload_bytes_total || 0);
      const doneBytes = Number(progress.upload_bytes_done || 0) + Number(progress.upload_bytes_current || 0);
      const fileText = progress.total_files
        ? `${Number(progress.hashed_files || 0).toLocaleString('pt-BR')}/${Number(progress.total_files || 0).toLocaleString('pt-BR')} arquivos`
        : '';
      const uploadText = totalBytes
        ? `${ctx.formatPatchSize(doneBytes)} / ${ctx.formatPatchSize(totalBytes)}`
        : `${Number(progress.upload_index || 0).toLocaleString('pt-BR')}/${Number(progress.upload_total || 0).toLocaleString('pt-BR')} envios`;
      if (bytes) bytes.textContent = totalBytes || progress.upload_total ? uploadText : fileText || '--';
    }

    async function pollPublishProgress(force = false) {
      if (!ctx.getToken()) return;
      try {
        const r = await fetch('/api/game/publish-progress', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success || !d.progress) return;
        if (force || d.progress.active || d.progress.done) renderPublishProgress(d.progress);
        if (d.progress.done && !d.progress.active) clearPublishProgressTimer();
      } catch {}
    }

    function startPublishProgressPolling() {
      clearPublishProgressTimer();
      renderPublishProgress({ percent: 0, phase: 'start', message: 'Iniciando publicação...' });
      ctx.setPublishProgressTimer(setInterval(() => pollPublishProgress(false), 700));
      pollPublishProgress(true);
    }

    function clearLauncherPublishProgressTimer() {
      if (ctx.getLauncherPublishProgressTimer()) {
        clearInterval(ctx.getLauncherPublishProgressTimer());
        ctx.setLauncherPublishProgressTimer(null);
      }
    }

    function renderLauncherPublishProgress(progress = {}) {
      const wrap = ctx.$('launcher-publish-progress-wrap');
      if (!wrap) return;
      const percent = Math.max(0, Math.min(100, Math.round(Number(progress.percent || 0))));
      const label = ctx.$('launcher-publish-progress-label');
      const pct = ctx.$('launcher-publish-progress-percent');
      const fill = ctx.$('launcher-publish-progress-fill');
      const phase = ctx.$('launcher-publish-progress-phase');
      const bytes = ctx.$('launcher-publish-progress-bytes');

      wrap.classList.remove('hidden');
      wrap.classList.toggle('error', progress.phase === 'error' || !!progress.error);
      if (label) label.textContent = progress.message || 'Publicação do launcher em andamento';
      if (pct) pct.textContent = percent + '%';
      if (fill) fill.style.width = percent + '%';

      const current = progress.current_file ? ` - ${progress.current_file}` : '';
      if (phase) phase.textContent = `${progress.phase || 'idle'}${current}`;

      const totalBytes = Number(progress.upload_bytes_total || 0);
      const doneBytes = Number(progress.upload_bytes_done || 0) + Number(progress.upload_bytes_current || 0);
      const fileText = progress.total_files
        ? `${Number(progress.hashed_files || 0).toLocaleString('pt-BR')}/${Number(progress.total_files || 0).toLocaleString('pt-BR')} arquivos`
        : '';
      const uploadText = totalBytes
        ? `${ctx.formatPatchSize(doneBytes)} / ${ctx.formatPatchSize(totalBytes)}`
        : `${Number(progress.upload_index || 0).toLocaleString('pt-BR')}/${Number(progress.upload_total || 0).toLocaleString('pt-BR')} envios`;
      if (bytes) bytes.textContent = totalBytes || progress.upload_total ? uploadText : fileText || '--';
    }

    async function pollLauncherPublishProgress(force = false) {
      if (!ctx.getToken()) return;
      try {
        const r = await fetch('/api/launcher/publish-progress', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success || !d.progress) return;
        if (force || d.progress.active || d.progress.done) renderLauncherPublishProgress(d.progress);
        if (d.progress.done && !d.progress.active) clearLauncherPublishProgressTimer();
      } catch {}
    }

    function startLauncherPublishProgressPolling() {
      clearLauncherPublishProgressTimer();
      renderLauncherPublishProgress({ percent: 0, phase: 'start', message: 'Iniciando publicação do launcher...' });
      ctx.setLauncherPublishProgressTimer(setInterval(() => pollLauncherPublishProgress(false), 700));
      pollLauncherPublishProgress(true);
    }

    async function selectAndPublishGameFolder() {
      const btn = ctx.$('btn-publish-folder');
      const progress = ctx.$('patch-progress');
      const summary = ctx.$('publish-folder-summary');
      const sourceDir = ctx.$('publish-source-dir') ? ctx.$('publish-source-dir').value.trim() : '';

      if (!sourceDir) {
        const message = 'Pasta do desenvolvedor do jogo não configurada. Configure e salve a pasta local antes de publicar.';
        ctx.showPatchResult(message, true);
        if (summary) summary.textContent = message;
        return;
      }

      ctx.setBusy(btn, true, 'PUBLICANDO...');
      if (progress) {
        progress.classList.remove('hidden');
        progress.textContent = 'Lendo a pasta configurada e calculando hashes...';
      }
      if (summary) summary.textContent = 'Usando a pasta configurada. O painel enviará ao R2 somente arquivos novos ou alterados.';

      try {
        await publishGameFolder(sourceDir, true);
      } catch (err) {
        ctx.showPatchResult('Erro: ' + err.message, true);
        if (summary) summary.textContent = 'Erro: ' + err.message;
        if (progress) progress.classList.add('hidden');
        ctx.setBusy(btn, false);
      }
    }

    async function publishGameFolder(sourceDir, keepBusy = false) {
      sourceDir = sourceDir || (ctx.$('publish-source-dir') ? ctx.$('publish-source-dir').value.trim() : '');
      const btn = ctx.$('btn-publish-folder');
      const progress = ctx.$('patch-progress');
      const summary = ctx.$('publish-folder-summary');

      if (!sourceDir) return ctx.showPatchResult('Informe a pasta local do client completo', true);

      if (!keepBusy) ctx.setBusy(btn, true, 'PUBLICANDO...');
      if (progress) {
        progress.classList.remove('hidden');
        progress.textContent = 'Calculando hashes e enviando somente arquivos alterados...';
      }
      if (summary) summary.textContent = 'Publicacao em andamento. O painel faz tudo sozinho; em clients grandes pode demorar.';
      startPublishProgressPolling();

      try {
        const r = await fetch('/api/game/publish-folder', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ source_dir: sourceDir })
        });
        const d = await r.json();
        if (!d.success) {
          await pollPublishProgress(true);
          clearPublishProgressTimer();
          renderPublishProgress({ phase: 'error', percent: 0, message: 'Erro: ' + (d.error || 'Falha ao publicar'), error: d.error || 'Falha ao publicar' });
          ctx.showPatchResult('Erro: ' + (d.error || ''), true);
          if (summary) summary.textContent = 'Falha ao publicar pasta.';
          return;
        }
        if (d.no_changes || d.cancelled) {
          const msg = d.message || 'Nenhum arquivo alterado. Envio cancelado.';
          renderPublishProgress({ phase: 'nochange', percent: 100, message: msg, total_files: d.file_count || 0, hashed_files: d.file_count || 0, upload_bytes_done: 0, upload_bytes_total: 0, uploaded_count: 0, skipped_existing_count: 0, removed_count: 0 });
          clearPublishProgressTimer();
          ctx.showPatchResult(msg, false);
          ctx.showToast('Nenhuma alteracao encontrada', 'warn');
          if (summary) summary.textContent = msg;
          ctx.loadGameRefInfo();
          ctx.loadVersions();
          ctx.loadPatchHistory();
          return;
        }

        const msg = `Atualizacao v${d.version || '--'} publicada. Enviados: ${d.uploaded_count || 0} arquivos (${ctx.formatPatchSize(d.uploaded_size)}). Removidos: ${d.removed_count || 0}. Total: ${d.file_count || 0}.`;
        renderPublishProgress({ phase: 'complete', percent: 100, message: msg, total_files: d.file_count || 0, hashed_files: d.file_count || 0, upload_bytes_done: d.uploaded_size || 0, upload_bytes_total: d.uploaded_size || 0, uploaded_count: d.uploaded_count || 0, skipped_existing_count: d.skipped_existing_count || 0, removed_count: d.removed_count || 0 });
        clearPublishProgressTimer();
        ctx.showPatchResult(msg, false);
        ctx.showToast(`R2 atualizado: ${d.uploaded_count || 0} arquivos`, 'success');
        if (summary) summary.textContent = msg;
        if (ctx.$('publish-notes-input')) ctx.$('publish-notes-input').value = '';
        ctx.loadGameRefInfo();
        ctx.loadVersions();
        ctx.loadPatchHistory();
      } catch (err) {
        await pollPublishProgress(true);
        clearPublishProgressTimer();
        renderPublishProgress({ phase: 'error', percent: 0, message: 'Erro: ' + err.message, error: err.message });
        ctx.showPatchResult('Erro: ' + err.message, true);
        if (summary) summary.textContent = 'Erro: ' + err.message;
      } finally {
        ctx.setBusy(btn, false);
        if (progress) progress.classList.add('hidden');
      }
    }

    function getSelectedPatchInput() {
      return ctx.getPatchSelectionMode() === 'folder' ? ctx.$('patch-folder-input') : ctx.$('patch-file-input');
    }

    function openPatchFilePicker() {
      ctx.setPatchSelectionMode('files');
      const input = ctx.$('patch-file-input');
      if (input) input.click();
    }

    function openPatchFolderPicker() {
      ctx.setPatchSelectionMode('folder');
      const input = ctx.$('patch-folder-input');
      if (input) input.click();
    }

    function renderPatchSelectionSummary() {
      const summary = ctx.$('patch-file-summary');
      const input = getSelectedPatchInput();
      if (!summary || !input) return;

      const files = Array.from(input.files || []);
      if (!files.length) {
        summary.textContent = 'Nenhum arquivo selecionado.';
        return;
      }

      const totalSize = files.reduce((sum, file) => sum + (file.size || 0), 0);
      const first = files[0].webkitRelativePath || files[0].name;
      const mode = ctx.getPatchSelectionMode() === 'folder' ? 'pasta' : 'arquivos';
      summary.textContent = `${files.length} ${mode} selecionados - ${ctx.formatPatchSize(totalSize)} - primeiro: ${first}`;
    }

    function readPatchFileAsBase64(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          const result = String(reader.result || '');
          const comma = result.indexOf(',');
          resolve(comma >= 0 ? result.slice(comma + 1) : result);
        };
        reader.onerror = () => reject(new Error(`Erro ao ler arquivo: ${file.name}`));
        reader.readAsDataURL(file);
      });
    }

    async function uploadPatch() {
      const fileInput = getSelectedPatchInput();
      const version = ctx.$('patch-version-input').value.trim();
      const notes = ctx.$('patch-notes-input') ? ctx.$('patch-notes-input').value.trim() : '';
      const baseUrl = ctx.$('patch-base-url') ? ctx.$('patch-base-url').value.trim() : '';
      const selectedFiles = Array.from((fileInput && fileInput.files) || []);

      if (!selectedFiles.length) return ctx.showPatchResult('Selecione os arquivos ou a pasta do patch', true);
      if (!version) return ctx.showPatchResult('Digite a nova versão.', true);

      const btn = ctx.$('btn-upload-patch');
      const progress = ctx.$('patch-progress');
      ctx.setBusy(btn, true, 'ENVIANDO...');
      progress.style.display = 'block';
      progress.textContent = 'Lendo arquivo...';

      try {
        const files = [];
        for (let i = 0; i < selectedFiles.length; i++) {
          const file = selectedFiles[i];
          const relPath = file.webkitRelativePath || file.name;
          progress.textContent = `Lendo ${i + 1}/${selectedFiles.length}: ${relPath}`;
          files.push({ name: file.name, path: relPath, size: file.size || 0, data: await readPatchFileAsBase64(file) });
        }
        const payload = { version, files, notes, base_url: baseUrl };
        progress.textContent = `Enviando ${files.length} arquivos...`;

        const r = await fetch('/api/game/upload-patch', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(payload)
        });
        const d = await r.json();

        if (d.success) {
          ctx.showPatchResult(`Atualização v${version} publicada. Manifest: ${d.manifest_url}`, false);
          ctx.showToast(`Manifest ${version} publicado`, 'success');
          if (ctx.$('patch-file-input')) ctx.$('patch-file-input').value = '';
          if (ctx.$('patch-folder-input')) ctx.$('patch-folder-input').value = '';
          renderPatchSelectionSummary();
          const notesInput = ctx.$('patch-notes-input');
          if (notesInput) notesInput.value = '';
          ctx.loadGameRefInfo();
          ctx.loadVersions();
          ctx.loadPatchHistory();
        } else {
          ctx.showPatchResult('Erro: ' + (d.error || ''), true);
        }
      } catch (err) {
        ctx.showPatchResult('Erro: ' + err.message, true);
      } finally {
        ctx.setBusy(btn, false);
        progress.style.display = 'none';
      }
    }

    async function saveLauncherConfig() {
      try {
        const r = await fetch('/api/launcher/config/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ config: ctx.getLauncherConfig() })
        });
        const d = await r.json();
        const el = ctx.getLauncherResultEl();
        el.textContent = d.success ? 'Configuracao salva!' : 'Erro: ' + (d.error || '');
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 3000);
      } catch (err) {
        const el = ctx.getLauncherResultEl();
        el.textContent = 'Erro: ' + err.message;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
      }
    }

    async function publishLauncherToCdn() {
      const el = ctx.getLauncherResultEl();
      try {
        ctx.setBusy('launcher-publish-btn', true, 'PUBLICANDO...');
        const version = ctx.$('launcher-publish-version-input') ? ctx.$('launcher-publish-version-input').value.trim() : '1.0.0';
        const notes = ctx.$('launcher-publish-notes-input') ? ctx.$('launcher-publish-notes-input').value.trim() : '';
        const baseUrl = ctx.$('launcher-cdn-base-url') ? ctx.$('launcher-cdn-base-url').value.trim() : '';
        const sourceDir = ctx.$('launcher-source-edit') ? ctx.$('launcher-source-edit').value.trim() : '';
        if (ctx.$('launcher-publish-summary')) ctx.$('launcher-publish-summary').textContent = 'Publicação em andamento. Acompanhe a barra de progresso.';
        startLauncherPublishProgressPolling();
        const r = await fetch('/api/launcher/publish', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ launcher_prefix: 'warface-launcher', version: version || '1.0.0', notes, public_base_url: baseUrl, source_dir: sourceDir })
        });
        const d = await r.json();
        if (!r.ok || d.success === false) {
          await pollLauncherPublishProgress(true);
          clearLauncherPublishProgressTimer();
          renderLauncherPublishProgress({ phase: 'error', percent: 0, message: 'Erro: ' + (d.error || 'Falha ao publicar launcher'), error: d.error || 'Falha ao publicar launcher' });
          el.textContent = 'Erro: ' + (d.error || 'Falha ao publicar launcher');
          el.className = 'cmd-result error';
          el.classList.remove('hidden');
          return;
        }
        const lines = [d.message || 'Launcher publicado'];
        if (d.rootUrl) lines.push(`Root: ${d.rootUrl}`);
        if (d.configUrl) lines.push(`Config: ${d.configUrl}`);
        el.textContent = lines.join(' | ');
        el.className = 'cmd-result';
        el.classList.remove('hidden');
        renderLauncherPublishProgress({ phase: 'complete', percent: 100, message: d.message || 'Launcher publicado', upload_index: d.uploaded || 0, upload_total: d.changed_count || d.uploaded || 0, removed_count: d.removed_count || d.deleted || 0 });
        clearLauncherPublishProgressTimer();
        ctx.showToast('Launcher publicado no CDN', 'success');
        if (ctx.$('launcher-publish-summary')) ctx.$('launcher-publish-summary').textContent = d.message || 'Launcher publicado';
        ctx.loadLauncherVersions();
        ctx.loadLauncherPatchHistory();
        ctx.loadLauncherRefInfo();
      } catch (err) {
        await pollLauncherPublishProgress(true);
        clearLauncherPublishProgressTimer();
        renderLauncherPublishProgress({ phase: 'error', percent: 0, message: 'Erro: ' + err.message, error: err.message });
        el.textContent = 'Erro: ' + err.message;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
      } finally {
        ctx.setBusy('launcher-publish-btn', false);
      }
    }

    return {
      clearPublishProgressTimer,
      renderPublishProgress,
      pollPublishProgress,
      startPublishProgressPolling,
      clearLauncherPublishProgressTimer,
      renderLauncherPublishProgress,
      pollLauncherPublishProgress,
      startLauncherPublishProgressPolling,
      selectAndPublishGameFolder,
      publishGameFolder,
      getSelectedPatchInput,
      openPatchFilePicker,
      openPatchFolderPicker,
      renderPatchSelectionSummary,
      readPatchFileAsBase64,
      uploadPatch,
      saveLauncherConfig,
      publishLauncherToCdn
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.launcher = window.AdminPanelDomains.launcher || {};
  window.AdminPanelDomains.launcher.createLauncherPublishDomain = createLauncherPublishDomain;
})();
