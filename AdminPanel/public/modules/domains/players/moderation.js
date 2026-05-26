(function initPlayersModerationDomain() {
  function createPlayersModerationDomain(ctx) {
    function showClanResult(msg, isError) {
      const el = ctx.$('clan-result');
      if (!el) return;
      el.textContent = msg;
      el.className = 'cmd-result' + (isError ? ' error' : '');
      el.classList.remove('hidden');
      setTimeout(() => el.classList.add('hidden'), 5000);
    }

    async function loadBans() {
      try {
        const r = await fetch('/api/bans', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) { ctx.$('bans-list').innerHTML = '<div class="empty-state">Erro ao carregar</div>'; return; }
        const list = ctx.$('bans-list');
        if (!d.bans || !d.bans.length) {
          list.innerHTML = '<div class="empty-state">Nenhum banimento encontrado</div>';
          return;
        }
        list.innerHTML = d.bans.map(b => {
          const cls = b.expired ? 'ban-expired' : 'ban-ok';
          const label = b.expired ? 'Expirado' : 'Ativo';
          const expires = new Date(b.expires * 1000).toLocaleString('pt-BR');
          const motivo = b.reason ? ` - ${ctx.esc(b.reason)}` : '';
          return `<div class="ban-item"><span><span class="ban-nick">${ctx.esc(b.nick)}</span> <span class="ban-info">#${b.username}${motivo} - expira: ${expires}</span></span><span><span class="${cls}">${label}</span> <button class="ban-btn-small" onclick="quickUnban('${ctx.esc(b.nick)}')">DESBANIR</button></span></div>`;
        }).join('');
      } catch {}
    }

    async function banPlayer() {
      const nick = ctx.$('ban-nick').value.trim();
      const duration = ctx.$('ban-duration').value;
      const reason = ctx.$('ban-reason').value.trim();
      if (!nick) return;
      try {
        const r = await fetch('/api/ban', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick, reason, duration: parseInt(duration, 10) })
        });
        const d = await r.json();
        const el = ctx.$('ban-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
        if (d.success) { loadBans(); ctx.$('ban-nick').value = ''; }
      } catch (e) { alert('Erro: ' + e.message); }
    }

    async function unbanPlayer() {
      const nick = ctx.$('ban-nick').value.trim();
      if (!nick) return;
      try {
        const r = await fetch('/api/unban', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick })
        });
        const d = await r.json();
        const el = ctx.$('ban-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
        if (d.success) { loadBans(); ctx.$('ban-nick').value = ''; }
      } catch (e) { alert('Erro: ' + e.message); }
    }

    async function quickUnban(nick) {
      try {
        await fetch('/api/unban', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick })
        });
        loadBans();
      } catch {}
    }

    async function loadBanHistory() {
      const container = ctx.$('ban-history-container');
      const list = ctx.$('ban-history-list');
      const visible = !container.classList.contains('hidden');
      if (!visible) {
        container.classList.remove('hidden');
        const nick = ctx.$('banhist-nick')?.value?.trim() || '';
        const type = ctx.$('banhist-type')?.value || 'all';
        try {
          const params = `limit=200&nick=${encodeURIComponent(nick)}&type=${type}`;
          const r = await fetch(`/api/banhistory?${params}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
          const d = await r.json();
          if (!d.success || !d.history || !d.history.length) {
            list.innerHTML = '<div class="empty-state">Nenhum registro</div>';
            return;
          }
          let html = `<div style="font-family:'Share Tech Mono',monospace;font-size:9px;color:#3a5a3a;padding:4px 8px">Total: ${d.total} registros</div>`;
          html += `<table class="ins-table"><tr><th>Tipo</th><th>Nick</th><th>#ID</th><th>Motivo</th><th>Admin</th><th>Data</th></tr>`;
          d.history.forEach(h => {
            const date = new Date(h.timestamp).toLocaleString('pt-BR');
            const icon = h.type === 'ban' ? '<span style="color:#c8371a">BAN</span>' : '<span style="color:#4aaa4a">UNBAN</span>';
            html += `<tr><td>${icon}</td><td class="ban-nick">${ctx.esc(h.nick)}</td><td>#${h.username}</td><td>${ctx.esc(h.reason || '-')}</td><td>${ctx.esc(h.admin || '-')}</td><td>${date}</td></tr>`;
          });
          html += `</table>`;
          list.innerHTML = html;
        } catch { list.innerHTML = '<div class="empty-state">Erro ao carregar</div>'; }
      } else {
        container.classList.add('hidden');
      }
    }

    async function loadIpBans() {
      try {
        const r = await fetch('/api/bannedips', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) { ctx.$('ipbans-list').innerHTML = '<div class="empty-state">Erro ao carregar</div>'; return; }
        const list = ctx.$('ipbans-list');
        const all = [...(d.active || []), ...(d.expired || [])];
        if (!all.length) { list.innerHTML = '<div class="empty-state">Nenhum IP banido</div>'; return; }
        list.innerHTML = all.map(b => {
          const cls = b.expired ? 'ban-expired' : 'ban-ok';
          const label = b.expired ? 'Expirado' : 'Ativo';
          const expires = new Date(b.expires * 1000).toLocaleString('pt-BR');
          return `<div class="ban-item"><span><span class="ban-nick">${ctx.esc(b.ip)}</span> <span class="ban-info">${ctx.esc(b.reason)} - expira: ${expires}</span></span><span><span class="${cls}">${label}</span> <button class="ban-btn-small" onclick="unbanIp('${ctx.esc(b.ip)}')">DESBANIR</button></span></div>`;
        }).join('');
      } catch {}
    }

    async function banIp() {
      const ip = ctx.$('banip-ip').value.trim();
      const duration = ctx.$('banip-duration').value;
      const reason = ctx.$('banip-reason').value.trim();
      if (!ip) return;
      try {
        const r = await fetch('/api/banip', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ ip, reason, duration: parseInt(duration, 10) })
        });
        const d = await r.json();
        const el = ctx.$('ipban-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
        if (d.success) { loadIpBans(); ctx.$('banip-ip').value = ''; }
      } catch {}
    }

    async function unbanIp(ip) {
      try {
        await fetch('/api/unbanip', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ ip })
        });
        loadIpBans();
      } catch {}
    }

    async function kickByIp() {
      const ip = ctx.$('banip-ip').value.trim();
      if (!ip) return;
      try {
        const r = await fetch('/api/kickbyip', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ ip })
        });
        const d = await r.json();
        const el = ctx.$('ipban-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch {}
    }

    async function loadClans() {
      const q = ctx.$('clan-search').value.trim();
      try {
        const r = await fetch(`/api/clans?q=${encodeURIComponent(q)}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) { ctx.$('clans-list').innerHTML = `<div class="empty-state">${ctx.esc(d.error)}</div>`; return; }
        const list = ctx.$('clans-list');
        if (!d.clans || !d.clans.length) { list.innerHTML = '<div class="empty-state">Nenhum clan encontrado</div>'; return; }
        list.innerHTML = d.clans.map(c => {
          const members = c.membersList ? c.membersList.map(m => `${ctx.esc(m.nickname)} (${ctx.esc(m.role)})`).join(', ') : '';
          return `<div class="ins-section" style="margin-bottom:6px"><div class="ins-section-title" style="color:#8a6aba">${ctx.esc(c.name)} <span style="color:#4a7a4a;font-size:9px">${c.members} membros - ${c.points} pontos</span><span style="font-size:9px;color:#3a5a3a"><button class="sc-btn" style="height:20px;font-size:8px;padding:0 6px" onclick="renameClan('${ctx.esc(c.name)}')">RENOMEAR</button></span></div><div style="padding:8px;font-family:'Share Tech Mono',monospace;font-size:10px;color:#5a8a5a"><div>Lider: ${ctx.esc(c.leader)}</div><div>Membros: ${members || 'nenhum'}</div><div>Descricao: ${ctx.esc(c.description || '-')}</div></div></div>`;
        }).join('');
      } catch {}
    }

    function showCreateClan() {
      const form = ctx.$('create-clan-form');
      form.classList.toggle('hidden');
    }

    async function createClan() {
      const name = ctx.$('clan-new-name').value.trim();
      const leader = ctx.$('clan-new-leader').value.trim();
      if (!name || !leader) return;
      try {
        const r = await fetch('/api/clan/create', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ name, leader_nick: leader })
        });
        const d = await r.json();
        showClanResult(d.message || d.error, !d.success);
        if (d.success) { ctx.$('create-clan-form').classList.add('hidden'); loadClans(); }
      } catch {}
    }

    async function renameClan(oldName) {
      const newName = prompt(`Novo nome para o clan "${oldName}":`);
      if (!newName || newName === oldName) return;
      try {
        const r = await fetch('/api/clan/rename', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ oldName, newName })
        });
        const d = await r.json();
        showClanResult(d.message || d.error, !d.success);
        if (d.success) loadClans();
      } catch {}
    }

    async function searchPlayers() {
      const q = ctx.$('search-term').value.trim();
      const type = ctx.$('search-type').value;
      if (q.length < 2) return;
      try {
        const r = await fetch(`/api/search?q=${encodeURIComponent(q)}&type=${type}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        const container = ctx.$('search-results');
        if (!d.success) { container.innerHTML = `<div class="empty-state">${ctx.esc(d.error)}</div>`; return; }
        if (!d.results || !d.results.length) { container.innerHTML = '<div class="empty-state">Nenhum jogador encontrado</div>'; return; }
        container.innerHTML = `<table class="search-table"><tr><th>Nick</th><th>ID</th><th>Rank</th><th>Ouro</th><th>VP</th><th>XP</th><th>Status</th><th>Cla</th></tr>${d.results.map(p => `<tr><td>${ctx.esc(p.nick)}</td><td>#${p.id}</td><td>${p.rank}</td><td class="pi-value gold">${ctx.num(p.game_money)}</td><td class="pi-value vp">${ctx.num(p.cry_money)}</td><td class="pi-value xp">${ctx.num(p.experience)}</td><td class="${p.status === 9 ? 'st-online' : 'st-offline'}">${p.status === 9 ? 'Online' : 'Offline'}</td><td>${ctx.esc(p.clan_name || '-')}</td></tr>`).join('')}</table>`;
      } catch {}
    }

    async function loadChatLogs() {
      const type = ctx.$('chatlog-type').value;
      const nick = ctx.$('chatlog-nick').value.trim();
      const params = `type=${type}&nick=${encodeURIComponent(nick)}&limit=200`;
      try {
        const r = await fetch(`/api/chatlogs?${params}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const list = ctx.$('chatlog-list');
        const stats = ctx.$('chatlog-stats');
        stats.textContent = `${d.total} mensagens encontradas (exibindo ${d.logs.length})`;
        if (!d.logs.length) {
          list.innerHTML = '<div class="empty-state">Nenhuma mensagem encontrada</div>';
          return;
        }
        list.innerHTML = d.logs.map(l => {
          const time = new Date(l.time).toLocaleString('pt-BR');
          const icon = l.type === 'groupchat' ? '#' : '@';
          let who = '';
          if (l.type === 'groupchat') {
            who = `<span class="ban-nick">${ctx.esc(l.nick || l.username)}</span> <span class="ban-info">em ${ctx.esc(l.channel || '?')}</span>`;
          } else {
            who = `<span class="ban-nick">${ctx.esc(l.nick || l.from_username)}</span> <span class="ban-info">-> ${ctx.esc(l.to_nick)}</span>`;
          }
          const color = l.type === 'groupchat' ? '#4a8aba' : '#c8a01a';
          return `<div class="player-item" style="border-left-color:${color}"><span>${icon} ${who}</span><span class="ban-info" style="flex:1;padding:0 8px;word-break:break-word">${ctx.esc(l.message)}</span><span class="ban-info">${time}</span></div>`;
        }).join('');
      } catch {}
    }

    function autoRefreshChatLogs() {
      if (ctx.getChatLogsInterval()) {
        clearInterval(ctx.getChatLogsInterval());
        ctx.setChatLogsInterval(null);
        return;
      }
      loadChatLogs();
      ctx.setChatLogsInterval(setInterval(loadChatLogs, 10000));
    }

    return {
      loadBans,
      banPlayer,
      unbanPlayer,
      quickUnban,
      loadBanHistory,
      loadIpBans,
      banIp,
      unbanIp,
      kickByIp,
      loadClans,
      showCreateClan,
      createClan,
      renameClan,
      showClanResult,
      searchPlayers,
      loadChatLogs,
      autoRefreshChatLogs
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.players = window.AdminPanelDomains.players || {};
  window.AdminPanelDomains.players.createPlayersModerationDomain = createPlayersModerationDomain;
})();
