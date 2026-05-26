(function initServicesRuntimeConfigDomain() {
  function createServicesRuntimeConfigDomain(ctx) {
    function readRuntimeEditorPayload() {
      const versionRaw = ctx.$('rt-config-version') ? ctx.$('rt-config-version').value.trim() : '';
      const portRaw = ctx.$('rt-server-port') ? ctx.$('rt-server-port').value.trim() : '';
      const port = parseInt(portRaw, 10);
      return {
        configVersion: versionRaw || String(Date.now()),
        server: {
          host: (ctx.$('rt-server-host') && ctx.$('rt-server-host').value.trim()) || 'warface',
          ip: (ctx.$('rt-server-ip') && ctx.$('rt-server-ip').value.trim()) || '127.0.0.1',
          port: Number.isFinite(port) && port >= 1 && port <= 65535 ? port : 1050
        },
        links: {
          discordInviteUrl: (ctx.$('rt-discord-url') && ctx.$('rt-discord-url').value.trim()) || 'https://discord.gg/YOUR_INVITE'
        },
        urls: {
          gameVersionUrl: (ctx.$('rt-url-game-version') && ctx.$('rt-url-game-version').value.trim()) || '',
          gameManifestUrl: (ctx.$('rt-url-game-manifest') && ctx.$('rt-url-game-manifest').value.trim()) || '',
          launcherConfigUrl: (ctx.$('rt-url-launcher-config') && ctx.$('rt-url-launcher-config').value.trim()) || ''
        }
      };
    }

    function validateRuntimeEditorPayload(payload) {
      if (!payload || typeof payload !== 'object') return { ok: false, error: 'Payload runtime invalido' };
      const server = payload.server && typeof payload.server === 'object' ? payload.server : {};
      const links = payload.links && typeof payload.links === 'object' ? payload.links : {};
      const urls = payload.urls && typeof payload.urls === 'object' ? payload.urls : {};
      const host = String(server.host || '').trim();
      const ip = String(server.ip || '').trim();
      const port = Number(server.port);
      if (!host || host.length > 120) return { ok: false, error: 'Servidor Host invalido' };
      if (!ip || ip.length > 255) return { ok: false, error: 'Servidor IP/Hostname invalido' };
      if (!Number.isInteger(port) || port < 1 || port > 65535) return { ok: false, error: 'Porta invalida (1-65535)' };

      const checkUrl = (value, label, required = false) => {
        const raw = String(value || '').trim();
        if (!raw) return required ? `${label} obrigatoria` : '';
        try {
          const parsed = new URL(raw);
          const isLocal = parsed.hostname === '127.0.0.1' || parsed.hostname === 'localhost' || parsed.hostname === '::1';
          if (parsed.protocol !== 'https:' && !(isLocal && parsed.protocol === 'http:')) return `${label} deve usar HTTPS (ou HTTP local)`;
          if (parsed.username || parsed.password) return `${label} nao pode conter credenciais`;
          return '';
        } catch { return `${label} invalida`; }
      };

      const discordError = checkUrl(links.discordInviteUrl, 'Discord Invite URL', true);
      if (discordError) return { ok: false, error: discordError };
      const gameVersionError = checkUrl(urls.gameVersionUrl, 'URL Game Version', true);
      if (gameVersionError) return { ok: false, error: gameVersionError };
      const gameManifestError = checkUrl(urls.gameManifestUrl, 'URL Game Manifest', true);
      if (gameManifestError) return { ok: false, error: gameManifestError };
      const launcherConfigError = checkUrl(urls.launcherConfigUrl, 'URL Launcher Config', true);
      if (launcherConfigError) return { ok: false, error: launcherConfigError };
      return { ok: true };
    }

    function fillRuntimeEditor(runtime) {
      if (!runtime || typeof runtime !== 'object') return;
      const server = runtime.server || {};
      const links = runtime.links || {};
      const urls = runtime.urls || {};
      if (ctx.$('rt-config-version')) ctx.$('rt-config-version').value = String(runtime.configVersion || '');
      if (ctx.$('rt-server-host')) ctx.$('rt-server-host').value = String(server.host || '');
      if (ctx.$('rt-server-ip')) ctx.$('rt-server-ip').value = String(server.ip || '');
      if (ctx.$('rt-server-port')) ctx.$('rt-server-port').value = String(server.port || '');
      if (ctx.$('rt-discord-url')) ctx.$('rt-discord-url').value = String(links.discordInviteUrl || '');
      if (ctx.$('rt-url-game-version')) ctx.$('rt-url-game-version').value = String(urls.gameVersionUrl || '');
      if (ctx.$('rt-url-game-manifest')) ctx.$('rt-url-game-manifest').value = String(urls.gameManifestUrl || '');
      if (ctx.$('rt-url-launcher-config')) ctx.$('rt-url-launcher-config').value = String(urls.launcherConfigUrl || '');
    }

    function showRuntimeConfigResult(message, isError = false) {
      const el = ctx.$('runtime-config-result');
      if (!el) return;
      el.textContent = message || '';
      el.className = 'cmd-result' + (isError ? ' error' : '');
      el.classList.remove('hidden');
      setTimeout(() => el.classList.add('hidden'), isError ? 5000 : 3000);
    }

    async function loadRuntimeConfigEditor() {
      try {
        const r = await fetch('/api/runtime-config', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success || !d.runtime) return showRuntimeConfigResult(d.error || 'Falha ao carregar runtime config', true);
        fillRuntimeEditor(d.runtime);
      } catch (e) {
        showRuntimeConfigResult('Erro ao carregar runtime config: ' + e.message, true);
      }
    }

    async function publishRuntimeConfig(mode = 'snapshot') {
      try {
        const publishMode = String(mode || 'snapshot').toLowerCase() === 'patch' ? 'patch' : 'snapshot';
        const payload = readRuntimeEditorPayload();
        const validation = validateRuntimeEditorPayload(payload);
        if (!validation.ok) return showRuntimeConfigResult(validation.error || 'Runtime config invalida', true);
        const r = await fetch('/api/runtime-config/publish', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ mode: publishMode, payload })
        });
        const d = await r.json();
        if (!d.success) return showRuntimeConfigResult(d.error || 'Falha ao publicar runtime config', true);
        showRuntimeConfigResult(`Runtime ${publishMode} publicado (v${payload.configVersion}) para ${d.clients || 0} cliente(s).`);
        await loadRuntimeConfigEditor();
      } catch (e) {
        showRuntimeConfigResult('Erro ao publicar runtime config: ' + e.message, true);
      }
    }

    return {
      readRuntimeEditorPayload,
      validateRuntimeEditorPayload,
      fillRuntimeEditor,
      showRuntimeConfigResult,
      loadRuntimeConfigEditor,
      publishRuntimeConfig
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.services = window.AdminPanelDomains.services || {};
  window.AdminPanelDomains.services.createRuntimeConfigDomain = createServicesRuntimeConfigDomain;
})();
