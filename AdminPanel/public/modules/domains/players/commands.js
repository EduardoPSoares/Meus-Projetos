(function initPlayersCommandsDomain() {
  function createPlayersCommandsDomain(ctx) {
    async function lookupProfile() {
      clearTimeout(ctx.getLookupTimer());
      ctx.setLookupTimer(setTimeout(async () => {
        const nick = ctx.$('cmd-nick').value.trim();
        const info = ctx.$('profile-info');
        if (!nick) { info.classList.add('hidden'); return; }
        try {
          const r = await fetch(`/api/profile/lookup?nick=${encodeURIComponent(nick)}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
          const d = await r.json();
          if (d.success) {
            const p = d.profile;
            info.innerHTML =
              `<div class="pi-item"><span class="pi-label">Ouro</span><span class="pi-value gold">${ctx.num(p.game_money)}</span></div>` +
              `<div class="pi-item"><span class="pi-label">Coroas</span><span class="pi-value crown">${ctx.num(p.crown_money)}</span></div>` +
              `<div class="pi-item"><span class="pi-label">VP</span><span class="pi-value vp">${ctx.num(p.cry_money)}</span></div>` +
              `<div class="pi-item"><span class="pi-label">XP</span><span class="pi-value xp">${ctx.num(p.experience)}</span></div>` +
              `<div class="pi-item"><span class="pi-label">Rank</span><span class="pi-value">${p.rank}</span></div>`;
            info.classList.remove('hidden');
          } else {
            info.classList.add('hidden');
          }
        } catch { info.classList.add('hidden'); }
      }, 400));
    }

    async function execCommand() {
      const cmd = ctx.getSelectedCmd();
      const nick = ctx.$('cmd-nick').value.trim();
      const amountRaw = ctx.$('cmd-amount').value.trim();
      const model = ctx.getPanelModel();
      const nickMin = (model.nick && model.nick.minLen) || ctx.DEFAULT_PANEL_MODEL.nick.minLen;
      const nickMax = (model.nick && model.nick.maxLen) || ctx.DEFAULT_PANEL_MODEL.nick.maxLen;
      if (!nick) { ctx.showResult('Digite um nickname', true); return; }
      if (nick.length < nickMin || nick.length > nickMax) {
        ctx.showResult(`Nick deve ter entre ${nickMin} e ${nickMax} caracteres`, true);
        return;
      }

      const requiresAmount = cmd === 'addcry' || cmd === 'addcrown' || cmd === 'addvp' || cmd === 'addxp';
      if (requiresAmount && !amountRaw) {
        ctx.showResult('Digite um valor', true);
        return;
      }

      const body = { cmd, nick };
      if (amountRaw) {
        const val = ctx.parseStrictIntInput(amountRaw);
        if (val === null) { ctx.showResult('Valor invalido', true); return; }

        const lim = ctx.getCommandLimit(cmd);
        if (cmd === 'addgm') {
          const minRank = lim && lim.minRank ? lim.minRank : 1;
          const maxRank = lim && lim.maxRank ? lim.maxRank : 90;
          if (val < minRank || val > maxRank) {
            ctx.showResult(`Rank deve estar entre ${minRank} e ${maxRank}`, true);
            return;
          }
        } else if (cmd !== 'kick' && cmd !== 'addcm') {
          if (val <= 0) { ctx.showResult('Valor deve ser positivo', true); return; }
          const maxPerCmd = lim && lim.perCmd ? lim.perCmd : 100000000;
          if (val > maxPerCmd) {
            ctx.showResult(`Valor maximo por comando: ${maxPerCmd.toLocaleString('pt-BR')}`, true);
            return;
          }
        }

        body.amount = val;
      }

      try {
        const r = await fetch('/api/command', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(body)
        });
        const d = await r.json();
        ctx.showResult(d.message || d.error || 'Erro', !d.success);
        if (d.success) lookupProfile();
      } catch (e) { ctx.showResult('Erro: ' + e.message, true); }
    }

    async function generateToken() {
      const id = ctx.$('token-id').value.trim();
      const val = ctx.$('token-val').value.trim();
      if (!id || !val) { ctx.showResult('Preencha ID e Token', true); return; }
      try {
        const r = await fetch(`/api/generateToken?id=${encodeURIComponent(id)}&token=${encodeURIComponent(val)}`, {
          headers: { 'X-Auth-Token': ctx.getToken() }
        });
        const d = await r.json();
        ctx.showResult(d.message || 'Erro', !d.success);
      } catch (e) { ctx.showResult('Erro: ' + e.message, true); }
    }

    return {
      lookupProfile,
      execCommand,
      generateToken
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.players = window.AdminPanelDomains.players || {};
  window.AdminPanelDomains.players.createPlayersCommandsDomain = createPlayersCommandsDomain;
})();
