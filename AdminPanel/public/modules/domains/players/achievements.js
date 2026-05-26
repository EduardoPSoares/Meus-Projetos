(function initPlayersAchievementsDomain() {
  function createPlayersAchievementsDomain(ctx) {
    async function loadAchievementImageMap() {
      if (ctx.getAchievementImageMap()) return ctx.getAchievementImageMap();
      ctx.setAchievementImageMap({});
      ctx.setAchievementBadgePool([]);
      try {
        const r = await fetch('/wiki-allimages-index.json', { cache: 'no-store' });
        const d = await r.json();
        const byName = d && d.byName ? d.byName : {};
        const map = ctx.getAchievementImageMap();
        const pool = [];
        Object.keys(byName).forEach(fileName => {
          const lower = String(fileName || '').toLowerCase();
          const path = byName[fileName];
          if (!path || (!lower.startsWith('challenge_') && !lower.startsWith('achievement_'))) return;
          if (lower.startsWith('challenge_badge_') && lower.endsWith('.png')) pool.push(path);
          const m = lower.match(/(?:^|_)(\d{1,4})(?:\.[a-z0-9]+)$/i);
          if (!m) return;
          const id = String(Number(m[1]));
          if (!id || id === '0') return;
          const current = map[id];
          const score = lower.includes('challenge_badge_') ? 100 : lower.includes('achievement_') ? 80 : lower.includes('challenge_mark_') ? 60 : lower.includes('challenge_strip_') ? 40 : 10;
          if (!current || score > current.score) map[id] = { path, score };
        });
        ctx.setAchievementBadgePool(Array.from(new Set(pool)).sort());
      } catch {}
      return ctx.getAchievementImageMap();
    }

    function pickAchievement(id, progress) {
      if (ctx.$('ach-id')) ctx.$('ach-id').value = id || '';
      if (ctx.$('ach-progress') && Number.isFinite(Number(progress))) ctx.$('ach-progress').value = String(progress);
    }

    function renderAchievementCatalogPager() {
      const el = ctx.$('ach-catalog-page');
      if (!el) return;
      const page = Math.floor(ctx.getAchCatalogOffset() / ctx.getAchCatalogLimit()) + 1;
      const pages = Math.max(1, Math.ceil((ctx.getAchCatalogTotal() || 0) / ctx.getAchCatalogLimit()));
      el.textContent = `Pagina ${page}/${pages} - ${ctx.getAchCatalogTotal() || 0} conquistas`;
    }

    function achCatalogPrev() {
      ctx.setAchCatalogOffset(Math.max(0, ctx.getAchCatalogOffset() - ctx.getAchCatalogLimit()));
      loadAchievementLists();
    }

    function achCatalogNext() {
      if (ctx.getAchCatalogOffset() + ctx.getAchCatalogLimit() >= ctx.getAchCatalogTotal()) return;
      ctx.setAchCatalogOffset(ctx.getAchCatalogOffset() + ctx.getAchCatalogLimit());
      loadAchievementLists();
    }

    async function grantAchievementFromCatalog(id) {
      const nick = String((ctx.$('ach-nick') && ctx.$('ach-nick').value) || '').trim();
      if (!nick) {
        ctx.showToast('Digite o nickname antes de conceder', 'warn');
        ctx.$('ach-nick')?.focus();
        return;
      }
      pickAchievement(id, Number((ctx.$('ach-progress') && ctx.$('ach-progress').value) || 1));
      const progress = ctx.parseStrictIntInput((ctx.$('ach-progress') && ctx.$('ach-progress').value) || '1') || 1;
      if (!ctx.confirmDanger(`Conceder conquista ${id} para ${nick} com progresso ${progress}?`)) return;
      try {
        const r = await fetch('/api/achievements/give', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick, achievement_id: id, progress })
        });
        const d = await r.json();
        const el = ctx.$('ach-result');
        if (el) {
          el.textContent = d.message || d.error || 'Erro';
          el.className = 'cmd-result' + (d.success ? '' : ' error');
          el.classList.remove('hidden');
          setTimeout(() => el.classList.add('hidden'), 5000);
        }
        if (d.success) {
          ctx.showToast('Conquista enviada ao player', 'success');
          loadAchievementLists();
        }
      } catch (e) {
        ctx.showToast(`Erro: ${e.message}`, 'error');
      }
    }

    function achievementImageCandidates(id) {
      const clean = String(id || '').trim().toLowerCase();
      const numeric = String(Number(clean));
      const map = ctx.getAchievementImageMap();
      const pool = ctx.getAchievementBadgePool();
      const mapped = map && map[numeric] ? map[numeric].path : '';
      const parsedNum = Number(clean);
      const poolPick = Array.isArray(pool) && pool.length && Number.isFinite(parsedNum) ? pool[Math.abs(parsedNum) % pool.length] : '';
      if (!clean) return ['/img/weapons/_default.png'];
      return [mapped, poolPick, `/img/weapons/wiki_all/challenge_badge_${clean}.png`, `/img/weapons/wiki_all/achievement_${clean}.png`, `/img/weapons/wiki_all/achievement_${clean}_shop.png`, '/img/weapons/_default.png'].filter(Boolean);
    }

    function achievementIconToImage(iconName) {
      const icon = String(iconName || '').trim().toLowerCase();
      if (!icon) return '';
      return `/img/weapons/wiki_all/${icon}.png`;
    }

    function achievementRowImageCandidates(rowOrId) {
      if (rowOrId && typeof rowOrId === 'object') {
        const id = String(rowOrId.id || '');
        const iconImg = achievementIconToImage(rowOrId.icon);
        return [iconImg, ...achievementImageCandidates(id)].filter(Boolean);
      }
      return achievementImageCandidates(String(rowOrId || ''));
    }

    function titleCaseWords(text) {
      return String(text || '').split(' ').filter(Boolean).map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');
    }

    function humanizeAchievementLabel(entry) {
      const rawName = entry && entry.name ? String(entry.name).trim() : '';
      const id = entry && entry.id ? String(entry.id).trim() : '';
      const source = entry && entry.source ? String(entry.source) : '';
      const icon = entry && entry.icon ? String(entry.icon) : '';
      if (rawName && !rawName.startsWith('@') && /[A-Za-z]/.test(rawName) && /\s/.test(rawName)) return rawName;
      const iconLabel = String(icon || '').replace(/^challenge_(badge|mark|strip)_/i, '$1_').replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim();
      if (/^\d+$/.test(id) && iconLabel) {
        let fromIcon = titleCaseWords(iconLabel);
        if (/^Badge\b/i.test(fromIcon)) fromIcon = fromIcon.replace(/^Badge\b/i, 'Insignia');
        if (/^Mark\b/i.test(fromIcon)) fromIcon = fromIcon.replace(/^Mark\b/i, 'Marca');
        if (/^Strip\b/i.test(fromIcon)) fromIcon = fromIcon.replace(/^Strip\b/i, 'Faixa');
        return fromIcon;
      }
      const baseToken = (rawName || icon || id).replace(/^@+/, '').replace(/^ui_/, '').replace(/^challenge_(badge|mark|strip)_/i, '$1_').replace(/^achievement_/, '').replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim();
      if (!baseToken) return 'Conquista';
      if (/^\d+$/.test(baseToken)) return `Conquista ${baseToken}`;
      let label = titleCaseWords(baseToken);
      if (/^Badge\b/i.test(label)) label = label.replace(/^Badge\b/i, 'Insignia');
      if (/^Mark\b/i.test(label)) label = label.replace(/^Mark\b/i, 'Marca');
      if (/^Strip\b/i.test(label)) label = label.replace(/^Strip\b/i, 'Faixa');
      if (source === 'wiki_visual' && /^(Insignia|Marca|Faixa)$/i.test(label)) return `${label} Especial`;
      return label || (id ? `Conquista ${id}` : 'Conquista');
    }

    function fallbackAchievementImage(img) {
      if (!img) return;
      const list = String(img.dataset.fallbacks || '').split('|').filter(Boolean);
      if (!list.length) {
        img.onerror = null;
        img.src = '/img/weapons/_default.png';
        return;
      }
      const next = list.shift();
      img.dataset.fallbacks = list.join('|');
      img.src = next;
    }

    async function loadAchievementLists() {
      await loadAchievementImageMap();
      const nick = String((ctx.$('ach-nick') && ctx.$('ach-nick').value) || '').trim();
      const playerWrap = ctx.$('ach-player-list');
      const catalogWrap = ctx.$('ach-catalog-list');
      try {
        const r = await fetch(`/api/achievements/list?nick=${encodeURIComponent(nick)}&offset=${ctx.getAchCatalogOffset()}&limit=${ctx.getAchCatalogLimit()}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) {
          if (playerWrap) playerWrap.innerHTML = `<div class="empty-state">${ctx.esc(d.error || 'Erro')}</div>`;
          if (catalogWrap) catalogWrap.innerHTML = `<div class="empty-state">${ctx.esc(d.error || 'Erro')}</div>`;
          return;
        }
        const player = Array.isArray(d.player) ? d.player : [];
        const catalog = Array.isArray(d.catalog) ? d.catalog : [];
        ctx.setAchCatalogTotal(Number(d.total || 0));
        if (ctx.getAchCatalogOffset() >= ctx.getAchCatalogTotal() && ctx.getAchCatalogOffset() > 0) {
          ctx.setAchCatalogOffset(Math.max(0, ctx.getAchCatalogOffset() - ctx.getAchCatalogLimit()));
          return loadAchievementLists();
        }
        renderAchievementCatalogPager();

        if (playerWrap) {
          if (!nick) playerWrap.innerHTML = '<div class="empty-state">Digite um nickname para carregar</div>';
          else if (!player.length) playerWrap.innerHTML = '<div class="empty-state">Jogador sem conquistas</div>';
          else playerWrap.innerHTML = player.map(a => {
            const imgs = achievementImageCandidates(a.id);
            const first = imgs.shift();
            return `<div class="item-result-item" onclick="pickAchievement('${ctx.esc(a.id)}', ${Number(a.progress) || 1})"><img class="iri-thumb" src="${ctx.esc(first)}" data-fallbacks="${ctx.esc(imgs.join('|'))}" onerror="fallbackAchievementImage(this)" /><span class="iri-text"><span class="iri-key">${ctx.esc(a.id)}</span><span class="iri-name">Progresso: ${Number(a.progress) || 0}</span></span></div>`;
          }).join('');
        }

        if (catalogWrap) {
          if (!catalog.length) catalogWrap.innerHTML = '<div class="empty-state">Nenhuma conquista encontrada</div>';
          else catalogWrap.innerHTML = catalog.map(entry => {
            const id = typeof entry === 'string' ? entry : String(entry && entry.id || '');
            const displayName = typeof entry === 'object' ? humanizeAchievementLabel(entry) : 'Conquista';
            const imgs = achievementRowImageCandidates(entry);
            const first = imgs.shift();
            return `<div class="item-result-item ach-catalog-row" onclick="grantAchievementFromCatalog('${ctx.esc(id)}')"><img class="iri-thumb" src="${ctx.esc(first)}" data-fallbacks="${ctx.esc(imgs.join('|'))}" onerror="fallbackAchievementImage(this)" /><span class="iri-text"><span class="iri-key">${ctx.esc(id)}</span><span class="iri-name">${ctx.esc(displayName)}</span></span></div>`;
          }).join('');
        }
      } catch (e) {
        if (playerWrap) playerWrap.innerHTML = `<div class="empty-state">Erro: ${ctx.esc(e.message)}</div>`;
        if (catalogWrap) catalogWrap.innerHTML = `<div class="empty-state">Erro: ${ctx.esc(e.message)}</div>`;
      }
    }

    async function giveAchievement() {
      const nick = ctx.$('ach-nick').value.trim();
      const achievement_id = ctx.$('ach-id').value.trim();
      const progressRaw = ctx.$('ach-progress').value || '1';
      const el = ctx.$('ach-result');
      const model = ctx.getPanelModel();
      const nickMin = (model.nick && model.nick.minLen) || ctx.DEFAULT_PANEL_MODEL.nick.minLen;
      const nickMax = (model.nick && model.nick.maxLen) || ctx.DEFAULT_PANEL_MODEL.nick.maxLen;
      if (!nick || nick.length < nickMin || nick.length > nickMax) {
        el.textContent = `Nick deve ter entre ${nickMin} e ${nickMax} caracteres`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      if (!achievement_id || !/^[a-z0-9_:/.-]+$/i.test(achievement_id)) {
        el.textContent = 'achievement_id invalido';
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      const progress = ctx.parseStrictIntInput(progressRaw);
      const prgRange = (model.achievement && model.achievement.progress) || ctx.DEFAULT_PANEL_MODEL.achievement.progress;
      if (progress === null || progress < prgRange.min || progress > prgRange.max) {
        el.textContent = `Progresso deve estar entre ${prgRange.min} e ${prgRange.max}`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      try {
        const r = await fetch('/api/achievements/give', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick, achievement_id, progress })
        });
        const d = await r.json();
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
        if (d.success) loadAchievementLists();
      } catch {}
    }

    async function removeAchievement() {
      const nick = ctx.$('ach-nick').value.trim();
      const achievement_id = ctx.$('ach-id').value.trim();
      const el = ctx.$('ach-result');
      const model = ctx.getPanelModel();
      const nickMin = (model.nick && model.nick.minLen) || ctx.DEFAULT_PANEL_MODEL.nick.minLen;
      const nickMax = (model.nick && model.nick.maxLen) || ctx.DEFAULT_PANEL_MODEL.nick.maxLen;
      if (!nick || nick.length < nickMin || nick.length > nickMax) {
        el.textContent = `Nick deve ter entre ${nickMin} e ${nickMax} caracteres`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      if (!achievement_id || !/^[a-z0-9_:/.-]+$/i.test(achievement_id)) {
        el.textContent = 'achievement_id invalido';
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      try {
        const r = await fetch('/api/achievements/remove', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick, achievement_id })
        });
        const d = await r.json();
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch {}
    }

    return {
      loadAchievementImageMap,
      pickAchievement,
      renderAchievementCatalogPager,
      achCatalogPrev,
      achCatalogNext,
      grantAchievementFromCatalog,
      achievementImageCandidates,
      achievementIconToImage,
      achievementRowImageCandidates,
      titleCaseWords,
      humanizeAchievementLabel,
      fallbackAchievementImage,
      loadAchievementLists,
      giveAchievement,
      removeAchievement
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.players = window.AdminPanelDomains.players || {};
  window.AdminPanelDomains.players.createPlayersAchievementsDomain = createPlayersAchievementsDomain;
})();
