(function initPlayersInspectDomain() {
  function createPlayersInspectDomain(ctx) {
    const CLASS_NAMES = ['Rifleman','Heavy','Recon','Medic','Engineer'];
    const CLASS_ICONS = ['classiconrifleman','classiconheavy','classiconsniper','classiconmedic','classiconengineer'];
    const SLOT_PT = ['Fuzileiro','Heavy','Atirador','Medico','Engenheiro','Pistola','Corpo a corpo','Equipamento','Capacete','Colete','Luvas','Botas','Paraquedas','DogTags','Mochila','C4','Badge','Marca','Listra','Pele','Graffiti','Avatar','Spray','Roupa','Contrato','Receita','Acessorio','Material','FuzileiroVIP','HeavyVIP','AtiradorVIP','MedicoVIP','EngenheiroVIP','PistolaVIP','CorpoVIP','EquipVIP','CapaceteVIP','ColeteVIP','LuvasVIP','BotasVIP','ParaquedasVIP','DogTagsVIP','MochilaVIP','BadgeVIP','MarcaVIP','ListraVIP','ArmaCraft','ArmaCraftVIP','Especial'];
    const STAT_PT = {
      player_online_time:'Tempo online', player_max_session_time:'Sessao maxima', player_ammo_restored:'Municao restaurada',
      player_climb_coops:'Escaladas', player_repair:'Reparos', player_heal:'Cura',
      player_resurrected_by_coin:'Ressuscitado (moeda)', player_climb_assists:'Auxilio escalada',
      player_resurrect_made:'Ressuscitou', player_gained_money:'Dinheiro ganho', player_damage:'Dano',
      player_max_damage:'Dano maximo', player_resurrected_by_medic:'Ressuscitado (medico)',
      player_kills_ai:'Inimigos (IA)', player_kills_player:'Jogadores', player_kill_streak:'Sequencia',
      player_kills_melee:'Corpo a corpo', player_kills_claymore:'Claymore', player_deaths:'Mortes',
      player_sessions_left:'Sessoes abandonadas', player_sessions_lost_connection:'Desconectou',
      player_sessions_kicked:'Expulso', player_shots:'Disparos', player_hits:'Acertos',
      player_headshots:'Headshots', player_playtime:'Tempo jogado', player_sessions_won:'Vitorias',
      player_sessions_lost:'Derrotas', player_sessions_draw:'Empates', player_wpn_usage:'Uso de arma'
    };
    const MODE_PT = { PVP:'PvP', PVE:'PvE', '':'-' };
    const CLASS_PT = { Rifleman:'Fuzileiro', Heavy:'Heavy', Recon:'Atirador', Medic:'Medico', Engineer:'Engenheiro', '':'-' };

    async function loadItemNames() {
      if (ctx.getItemNames()) return;
      try {
        const r = await fetch('/api/weapons/names', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        ctx.setItemNames(d.success ? (d.names || {}) : {});
      } catch { ctx.setItemNames({}); }
    }

    function itemDisplayName(name) {
      const itemNames = ctx.getItemNames();
      if (!itemNames) return name;
      const s = String(name || '').toLowerCase();
      const parts = s.split('_');
      const base = parts[0];
      return itemNames[base] || itemNames[s] || itemNames[parts.slice(0,2).join('_')] || name;
    }

    function fmtBanner(v) { return v === '4294967295' ? 'Nenhum' : ctx.esc(v); }

    async function loadFullProfile() {
      const nick = ctx.$('ins-nick').value.trim();
      if (!nick) return;
      const container = ctx.$('ins-result');
      container.innerHTML = '<div class="empty-state">Carregando...</div>';
      await loadItemNames();
      try {
        const r = await fetch(`/api/profile/full?nick=${encodeURIComponent(nick)}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) { container.innerHTML = `<div class="cmd-result error">${ctx.esc(d.error)}</div>`; return; }
        const p = d.profile;
        let html = '';

        const clsIdx = p.basic.current_class;
        const classIcon = CLASS_ICONS[clsIdx] || CLASS_ICONS[0];
        html += `<div class="ins-section"><div class="ins-section-title">Dados Basicos</div><div class="ins-hero">`;
        html += `<div class="ins-avatar"><img src="img/${classIcon}.png" class="ins-class-icon" onerror="this.style.display='none'" /><div class="ins-rank-badge"><img src="img/rank.png" class="ins-rank-icon" onerror="this.style.display='none'" /><span>${p.basic.rank}</span></div></div>`;
        html += `<div class="ins-hero-info"><div class="ins-nick">${ctx.esc(p.basic.nick)}</div><div class="ins-username">${ctx.esc(p.basic.username)} #${p.basic.id}</div>`;
        const moneyItems = [
          { key:'game_money', label:'Ouro', file:'game_money' },
          { key:'crown_money', label:'Coroas', file:'crown_money' },
          { key:'cry_money', label:'VP', file:'cry_money' }
        ];
        html += `<div class="ins-money-row">`;
        moneyItems.forEach(m => {
          const val = p.basic[m.key] || 0;
          html += `<span class="ins-money-item"><img src="img/${m.file}.png" class="ins-money-icon" onerror="this.style.display='none'" />${val.toLocaleString('pt-BR')}</span>`;
        });
        html += `</div></div></div>`;
        html += `<div class="ins-hero-meta">`;
        html += `<span>Classe: ${CLASS_NAMES[clsIdx] || clsIdx}</span><span>XP: ${p.basic.experience.toLocaleString('pt-BR')}</span>`;
        html += `<span>Cla: ${ctx.esc(p.basic.clan_name || '(nenhum)')}</span><span>${p.basic.status === 9 ? 'Online' : 'Offline'}</span>`;
        if (p.basic.last_seen) html += `<span>Ultima vez: ${new Date(p.basic.last_seen).toLocaleString('pt-BR')}</span>`;
        html += `</div></div>`;

        if (p.items && p.items.length) {
          html += `<div class="ins-section"><div class="ins-section-title" onclick="this.nextElementSibling.classList.toggle('ins-hide')">Itens (${p.items.length}) <span class="ins-toggle">v</span></div><div class="ins-table-wrap">`;
          html += `<table class="ins-table"><tr><th>Nome</th><th>Slot</th><th>Equipado</th><th>Dur.</th><th>Expira</th></tr>`;
          p.items.slice(0, 200).forEach(i => {
            const eqList = [];
            if (i.equipped) {
              for (let bit = 0; bit < SLOT_PT.length; bit++) {
                if (i.equipped & (1 << bit)) eqList.push(SLOT_PT[bit] || `slot${bit}`);
              }
            }
            const eq = eqList.length ? eqList.join(', ') : 'Nao';
            const exp = i.expiration_time_utc ? new Date(i.expiration_time_utc).toLocaleDateString('pt-BR') : '-';
            const dur = i.total_durability_points ? `${i.durability_points}/${i.total_durability_points}` : '-';
            html += `<tr><td title="${ctx.esc(i.name)}">${ctx.esc(itemDisplayName(i.name))}</td><td>${i.slot}</td><td class="${i.equipped ? 'ins-equipped' : 'ins-unequipped'}">${eq}</td><td>${dur}</td><td>${exp}</td></tr>`;
          });
          if (p.items.length > 200) html += `<tr><td colspan="5">... e mais ${p.items.length - 200} itens</td></tr>`;
          html += `</table></div></div>`;
        }

        if (p.expired_items && p.expired_items.length) {
          html += `<div class="ins-section"><div class="ins-section-title" onclick="this.nextElementSibling.classList.toggle('ins-hide')">Itens Expirados (${p.expired_items.length}) <span class="ins-toggle">v</span></div><div class="ins-table-wrap">`;
          html += `<table class="ins-table"><tr><th>Nome</th></tr>`;
          p.expired_items.slice(0, 50).forEach(i => { html += `<tr><td title="${ctx.esc(i.name)}">${ctx.esc(itemDisplayName(i.name))}</td></tr>`; });
          html += `</table></div></div>`;
        }

        if (p.stats && p.stats.length) {
          const seen = new Set();
          const unique = p.stats.filter(s => {
            const key = `${s.stat}_${s.mode || ''}_${s.class || ''}`;
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
          });
          html += `<div class="ins-section"><div class="ins-section-title" onclick="this.nextElementSibling.classList.toggle('ins-hide')">Estatisticas (${unique.length}) <span class="ins-toggle">v</span></div><div class="ins-table-wrap">`;
          html += `<table class="ins-table"><tr><th>Estatistica</th><th>Valor</th><th>Modo</th><th>Classe</th></tr>`;
          unique.forEach(s => {
            const label = STAT_PT[s.stat] || s.stat;
            const mode = MODE_PT[s.mode] || s.mode || '-';
            const cls = CLASS_PT[s.class] || s.class || '-';
            html += `<tr><td>${ctx.esc(label)}</td><td>${s.value.toLocaleString('pt-BR')}</td><td>${mode}</td><td>${cls}</td></tr>`;
          });
          html += `</table></div></div>`;
        }

        if (p.achievements && p.achievements.length) {
          html += `<div class="ins-section"><div class="ins-section-title" onclick="this.nextElementSibling.classList.toggle('ins-hide')">Conquistas (${p.achievements.length}) <span class="ins-toggle">v</span></div><div class="ins-table-wrap">`;
          html += `<table class="ins-table"><tr><th>ID</th><th>Progresso</th><th>Completado</th></tr>`;
          p.achievements.forEach(a => {
            const completed = a.completion_time ? new Date(a.completion_time).toLocaleDateString('pt-BR') : '-';
            html += `<tr><td>${a.id}</td><td>${a.progress}</td><td>${completed}</td></tr>`;
          });
          html += `</table></div></div>`;
        }

        html += `<div class="ins-section"><div class="ins-section-title" onclick="this.nextElementSibling.classList.toggle('ins-hide')">Outras Informacoes <span class="ins-toggle">v</span></div><div style="padding:8px">`;
        html += `<div class="ins-field"><span class="ins-label">Classes</span><span class="ins-value">${p.classes_unlocked.map(c => CLASS_NAMES[c] || c).join(', ') || 'nenhuma'}</span></div>`;
        html += `<div class="ins-field"><span class="ins-label">Missoes</span><span class="ins-value">${ctx.esc(p.missions_unlocked.join(', ') || 'nenhuma')}</span></div>`;
        html += `<div class="ins-field"><span class="ins-label">Tutoriais</span><span class="ins-value">${p.tutorials_passed.length}</span></div>`;
        html += `<div class="ins-field"><span class="ins-label">Amigos</span><span class="ins-value">${p.friends.length}</span></div>`;
        html += `<div class="ins-field"><span class="ins-label">Banner</span><span class="ins-value">Badge: ${fmtBanner(p.banner.badge)} | Marca: ${fmtBanner(p.banner.mark)} | Listra: ${fmtBanner(p.banner.stripe)}</span></div>`;
        if (p.mute.time) html += `<div class="ins-field"><span class="ins-label">Silenciado</span><span class="ins-value">ate ${new Date(p.mute.time).toLocaleString('pt-BR')} - ${ctx.esc(p.mute.reason)}</span></div>`;
        html += `</div></div>`;

        container.innerHTML = html;
        ctx.loadNotes();
      } catch (e) {
        container.innerHTML = `<div class="cmd-result error">Erro: ${ctx.esc(e.message)}</div>`;
      }
    }

    return { loadItemNames, itemDisplayName, fmtBanner, loadFullProfile };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.players = window.AdminPanelDomains.players || {};
  window.AdminPanelDomains.players.createPlayersInspectDomain = createPlayersInspectDomain;
})();
