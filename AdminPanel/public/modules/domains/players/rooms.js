(function initPlayersRoomsDomain() {
  function createPlayersRoomsDomain(ctx) {
    async function loadGameRooms() {
      try {
        const r = await fetch('/api/gamerooms', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const list = ctx.$('gamerooms-list');
        const stats = ctx.$('gameroom-stats');
        if (!d.rooms || !d.rooms.length) {
          stats.textContent = '0 salas ativas';
          list.innerHTML = '<div class="empty-state">Nenhuma sala ativa</div>';
          return;
        }
        stats.textContent = `${d.rooms.length} sala(s) ativa(s)`;
        list.innerHTML = d.rooms.map(rm => {
          const players = rm.players.map(p => `<span style="color:${p.team === 0 ? '#4a8aba' : '#c8371a'}">${ctx.esc(p.nickname)}${p.status === 1 ? ' (em jogo)' : ''}</span>`).join(', ');
          const typeNames = { 8: 'PvE', 16: 'PvP', 32: 'PvP (Ranked)' };
          const type = typeNames[rm.room_type] || rm.room_type;
          const status = rm.session_status === 0 ? '<span style="color:#4aaa4a">Aguardando</span>' : '<span style="color:#c8a01a">Em jogo</span>';
          return `<div class="player-item" style="border-left-color:#4a8aba;flex-wrap:wrap"><span class="ban-nick">${ctx.esc(rm.name || 'Sem nome')}</span><span class="ban-info">${type} - ${rm.player_count}/${rm.max_players} jogadores - ${status}</span><span class="ban-info" style="flex-basis:100%;margin-top:2px">${players || 'nenhum jogador'}</span></div>`;
        }).join('');
      } catch {}
    }

    function autoRefreshGameRooms() {
      if (ctx.getGameroomInterval()) {
        clearInterval(ctx.getGameroomInterval());
        ctx.setGameroomInterval(null);
        return;
      }
      loadGameRooms();
      ctx.setGameroomInterval(setInterval(loadGameRooms, 5000));
    }

    return { loadGameRooms, autoRefreshGameRooms };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.players = window.AdminPanelDomains.players || {};
  window.AdminPanelDomains.players.createPlayersRoomsDomain = createPlayersRoomsDomain;
})();
