(function initServicesAntiCheatDomain() {
  function createServicesAntiCheatDomain(ctx) {
    async function loadAcConfig() {
      try {
        const r = await fetch('/api/anticheat', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const cfg = d.config;
        ctx.$('ac-editor').value = cfg.raw;
        const grid = ctx.$('ac-grid');
        grid.innerHTML = '';
        const items = [
          { flag: 'checkCertificate', label: 'Verificar Certificado', desc: 'Valida certificado TLS do servidor' },
          { flag: 'useProtect', label: 'Protocolo Protect', desc: 'Usa protocolo protegido entre cliente/servidor' },
          { flag: 'cvarHash', label: 'CVAR Hash Validation', desc: 'Valida hash dos CVARs do cliente' },
          { flag: 'antiCheatHash', label: 'Anti-Cheat Hash', desc: 'Valida hash dos executaveis do jogo' },
          { flag: 'releaseBuild', label: 'Release Build', desc: 'Modo release (desativa debug)' },
          { flag: 'consoleRestricted', label: 'Console Restrito', desc: 'Impede acesso ao console de desenvolvedor' },
          { flag: 'deactivateConsole', label: 'Desativar Console', desc: 'Esconde o console do jogo' }
        ];
        items.forEach(item => {
          const div = document.createElement('div');
          div.className = 'ac-item';
          const on = cfg[item.flag] === true;
          div.innerHTML = `<div><div class="ac-label">${item.label}</div><div class="ac-desc">${item.desc}</div></div><div class="toggle ${on ? 'on' : ''}" data-flag="${item.flag}" onclick="toggleAc(this)"></div>`;
          grid.appendChild(div);
        });
      } catch {}
    }

    async function toggleAc(el) {
      const flag = el.dataset.flag;
      const enabled = !el.classList.contains('on');
      try {
        await fetch('/api/anticheat/set', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ flag, enabled })
        });
        el.classList.toggle('on', enabled);
      } catch {}
    }

    async function saveAcRaw() {
      try {
        const content = ctx.$('ac-editor').value;
        await fetch('/api/anticheat/set', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ flag: 'raw', enabled: true, rawContent: content })
        });
        ctx.showResult('online.cfg salvo!', false);
      } catch {
        ctx.showResult('Erro ao salvar', true);
      }
    }

    return { loadAcConfig, toggleAc, saveAcRaw };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.services = window.AdminPanelDomains.services || {};
  window.AdminPanelDomains.services.createAntiCheatDomain = createServicesAntiCheatDomain;
})();
