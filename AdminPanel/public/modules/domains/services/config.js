(function initServicesConfigDomain() {
  function createServicesConfigDomain(ctx) {
    async function loadConfig() {
      try {
        const r = await fetch('/api/config', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (d.success) {
          const cfg = d.config && typeof d.config === 'object' ? d.config : {};
          ctx.setLoadedConfigObject(cfg);
          ctx.$('config-editor').value = JSON.stringify(cfg, null, 4);
          fillConfigBlocks(cfg);
        }
      } catch {}
      await loadPanelPaths();
    }

    async function loadPanelPaths() {
      try {
        const r = await fetch('/api/paths', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const p = d.paths || {};
        if (ctx.$('cfg-path-server-root')) ctx.$('cfg-path-server-root').value = String(p.serverRoot || '');
        if (ctx.$('cfg-path-game-dir')) ctx.$('cfg-path-game-dir').value = String(p.gameDir || '');
        if (ctx.$('cfg-path-public-dir')) ctx.$('cfg-path-public-dir').value = String(p.publicDir || '');
      } catch {}
    }

    function objectToKeyValueLines(obj) {
      if (!obj || typeof obj !== 'object') return '';
      return Object.entries(obj).map(([k, v]) => `${k}=${v}`).join('\n');
    }

    function keyValueLinesToObject(text) {
      const out = {};
      const lines = String(text || '').split(/\r?\n/);
      for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line || line.startsWith('#')) continue;
        const idx = line.indexOf('=');
        if (idx < 1) continue;
        const key = line.slice(0, idx).trim();
        const value = line.slice(idx + 1).trim();
        if (!key) continue;
        out[key] = value;
      }
      return out;
    }

    function fillConfigBlocks(cfg) {
      const safe = cfg && typeof cfg === 'object' ? cfg : {};
      const ms = safe.masterserver && typeof safe.masterserver === 'object' ? safe.masterserver : {};
      if (ctx.$('cfg-mongodb')) ctx.$('cfg-mongodb').value = String(safe.mongodb || '');
      if (ctx.$('cfg-master-host')) ctx.$('cfg-master-host').value = String(ms.host || '');
      if (ctx.$('cfg-master-port')) ctx.$('cfg-master-port').value = String(ms.port || '');
      if (ctx.$('cfg-master-domain')) ctx.$('cfg-master-domain').value = String(ms.domain || '');
      if (ctx.$('cfg-master-username')) ctx.$('cfg-master-username').value = String(ms.username || '');
      if (ctx.$('cfg-master-password')) ctx.$('cfg-master-password').value = String(ms.password || '');
      if (ctx.$('cfg-dedicated-hosts')) ctx.$('cfg-dedicated-hosts').value = objectToKeyValueLines(safe.dedicated_hosts || {});
      if (ctx.$('cfg-dedicated-public-hosts')) ctx.$('cfg-dedicated-public-hosts').value = objectToKeyValueLines(safe.dedicated_public_hosts || {});
      if (ctx.$('cfg-dedicated-public-ports')) ctx.$('cfg-dedicated-public-ports').value = objectToKeyValueLines(safe.dedicated_public_ports || {});
      if (ctx.$('cfg-dedicated-port-map')) {
        const map = safe.dedicated_port_map && typeof safe.dedicated_port_map === 'object' ? safe.dedicated_port_map : {};
        ctx.$('cfg-dedicated-port-map').value = Object.entries(map)
          .map(([room, v]) => `${room}=${Number((v && v.base) || 0)},${Number((v && v.count) || 0)}`)
          .join('\n');
      }
    }

    function parseDedicatedPortMapLines(text) {
      const out = {};
      const lines = String(text || '').split(/\r?\n/);
      for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line || line.startsWith('#')) continue;
        const idx = line.indexOf('=');
        if (idx < 1) continue;
        const key = line.slice(0, idx).trim();
        const data = line.slice(idx + 1).trim();
        const parts = data.split(',').map(v => v.trim());
        const base = Number.parseInt(parts[0] || '', 10);
        const count = Number.parseInt(parts[1] || '1', 10);
        if (!key || !Number.isFinite(base) || base < 1 || !Number.isFinite(count) || count < 1) continue;
        out[key] = { base, count };
      }
      return out;
    }

    function buildConfigFromBlocks() {
      const loaded = ctx.getLoadedConfigObject();
      const base = loaded && typeof loaded === 'object' ? JSON.parse(JSON.stringify(loaded)) : {};
      const master = base.masterserver && typeof base.masterserver === 'object' ? base.masterserver : {};
      base.mongodb = String((ctx.$('cfg-mongodb') && ctx.$('cfg-mongodb').value) || '').trim();
      base.masterserver = {
        ...master,
        host: String((ctx.$('cfg-master-host') && ctx.$('cfg-master-host').value) || '').trim(),
        port: String((ctx.$('cfg-master-port') && ctx.$('cfg-master-port').value) || '').trim(),
        domain: String((ctx.$('cfg-master-domain') && ctx.$('cfg-master-domain').value) || '').trim(),
        username: String((ctx.$('cfg-master-username') && ctx.$('cfg-master-username').value) || '').trim(),
        password: String((ctx.$('cfg-master-password') && ctx.$('cfg-master-password').value) || '').trim()
      };

      if (!base.mongodb) throw new Error('MongoDB URL obrigatoria');
      if (!base.masterserver.host) throw new Error('Master Host obrigatorio');
      if (!base.masterserver.port) throw new Error('Master Port obrigatorio');
      if (!base.masterserver.domain) throw new Error('Master Domain obrigatorio');
      if (!base.masterserver.username) throw new Error('Master Username obrigatorio');
      if (!base.masterserver.password) throw new Error('Master Password obrigatorio');

      base.dedicated_hosts = keyValueLinesToObject((ctx.$('cfg-dedicated-hosts') && ctx.$('cfg-dedicated-hosts').value) || '');
      base.dedicated_public_hosts = keyValueLinesToObject((ctx.$('cfg-dedicated-public-hosts') && ctx.$('cfg-dedicated-public-hosts').value) || '');
      base.dedicated_public_ports = keyValueLinesToObject((ctx.$('cfg-dedicated-public-ports') && ctx.$('cfg-dedicated-public-ports').value) || '');
      base.dedicated_port_map = parseDedicatedPortMapLines((ctx.$('cfg-dedicated-port-map') && ctx.$('cfg-dedicated-port-map').value) || '');
      return base;
    }

    function syncConfigBlocksToEditor() {
      try {
        buildConfigFromBlocks();
        const el = ctx.$('config-result');
        el.textContent = 'Blocos validados com sucesso.';
        el.className = 'cmd-result';
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 2500);
      } catch (e) {
        const el = ctx.$('config-result');
        el.textContent = 'Erro ao sincronizar: ' + e.message;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
      }
    }

    async function saveConfig() {
      try {
        const cfg = buildConfigFromBlocks();
        ctx.$('config-editor').value = JSON.stringify(cfg, null, 4);
        const r = await fetch('/api/config/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ config: cfg })
        });
        const d = await r.json();
        const el = ctx.$('config-result');
        el.textContent = d.message || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 3000);
        if (d.success) ctx.setLoadedConfigObject(cfg);
      } catch (e) {
        const el = ctx.$('config-result');
        el.textContent = 'Falha ao salvar config: ' + e.message;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
      }
    }

    async function savePanelPaths() {
      try {
        const payload = {
          serverRoot: String((ctx.$('cfg-path-server-root') && ctx.$('cfg-path-server-root').value) || '').trim(),
          gameDir: String((ctx.$('cfg-path-game-dir') && ctx.$('cfg-path-game-dir').value) || '').trim(),
          publicDir: String((ctx.$('cfg-path-public-dir') && ctx.$('cfg-path-public-dir').value) || '').trim()
        };
        const r = await fetch('/api/paths/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ paths: payload })
        });
        const d = await r.json();
        const el = ctx.$('paths-result');
        if (!el) return;
        el.textContent = d.message || (d.success ? 'Paths salvos' : (d.error || 'Erro ao salvar paths'));
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
      } catch (e) {
        const el = ctx.$('paths-result');
        if (!el) return;
        el.textContent = 'Falha ao salvar paths: ' + e.message;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
      }
    }

    return {
      loadConfig,
      objectToKeyValueLines,
      keyValueLinesToObject,
      fillConfigBlocks,
      parseDedicatedPortMapLines,
      buildConfigFromBlocks,
      syncConfigBlocksToEditor,
      saveConfig,
      loadPanelPaths,
      savePanelPaths
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.services = window.AdminPanelDomains.services || {};
  window.AdminPanelDomains.services.createConfigDomain = createServicesConfigDomain;
})();
