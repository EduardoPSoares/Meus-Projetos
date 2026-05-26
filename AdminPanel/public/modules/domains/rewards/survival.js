(function initRewardsSurvivalDomain() {
  function createRewardsSurvivalDomain(ctx) {
    function setSurvivalResult(message, isError) {
      const el = ctx.$('survival-rewards-result');
      if (!el) return;
      if (!message) {
        el.classList.add('hidden');
        return;
      }
      el.textContent = message;
      el.className = `cmd-result${isError ? ' error' : ''}`;
      el.classList.remove('hidden');
    }

    function updateSurvivalSelectedCount() {
      const selectedType = ctx.$('survival-type-select') ? ctx.$('survival-type-select').value : '';
      const selectedRow = ctx.getCurrentSurvivalRow();
      const selectedLabel = selectedRow ? ctx.survivalMissionDisplayName(selectedRow) : '';
      if (ctx.$('survival-selected-count')) ctx.$('survival-selected-count').textContent = selectedLabel || 'Nada selecionado';
      if (ctx.$('survival-current-badge')) ctx.$('survival-current-badge').textContent = selectedLabel || 'Selecione uma sobrevivencia';
      document.querySelectorAll('.survival-reward-row').forEach((row) => {
        row.classList.toggle('checked', row.dataset.missionType === selectedType);
      });
    }

    function populateSurvivalTypeDropdown() {
      const sel = ctx.$('survival-type-select');
      if (!sel) return;
      const current = sel.value;
      sel.innerHTML = '<option value="">Selecione o tipo...</option>';
      ctx.getSurvivalRewardsCache().forEach((row) => {
        const opt = document.createElement('option');
        opt.value = row.missionType;
        opt.textContent = ctx.survivalMissionDisplayName(row);
        sel.appendChild(opt);
      });
      if (current && ctx.getSurvivalRewardsCache().some((r) => r.missionType === current)) {
        sel.value = current;
      }
    }

    function renderSurvivalRewards() {
      const list = ctx.$('survival-rewards-list');
      if (!list) return;
      const q = String(ctx.$('survival-reward-filter') && ctx.$('survival-reward-filter').value || '').trim().toLowerCase();
      const selectedType = ctx.$('survival-type-select') ? ctx.$('survival-type-select').value : '';
      const rows = ctx.getSurvivalRewardsCache().filter((row) => {
        if (!q) return true;
        return String(row.missionType || '').toLowerCase().includes(q) ||
          String(row.rewardSet || '').toLowerCase().includes(q);
      });

      if (ctx.$('survival-rewards-count')) ctx.$('survival-rewards-count').textContent = `${rows.length}/${ctx.getSurvivalRewardsCache().length} tipos`;
      if (!rows.length) {
        list.innerHTML = '<div class="empty-state">Nenhuma recompensa encontrada</div>';
        updateSurvivalSelectedCount();
        return;
      }

      list.innerHTML = rows.map((row) => {
        const idx = ctx.getSurvivalRewardsCache().indexOf(row);
        const items = ctx.survivalRewardItems(row);
        const item = items[0] ? ctx.survivalRewardItemDisplay(items[0]) : null;
        const gp = ctx.survivalRewardMoney(row, 'game_money');
        const cash = ctx.survivalRewardMoney(row, 'cry_money');
        const crown = ctx.survivalRewardMoney(row, 'crown_money');
        const missionCount = row.missions && row.missions.count ? row.missions.count : 0;
        const finalBits = [];
        if (item) finalBits.push(`<span class="survival-card-reward"><img src="${ctx.survivalEscAttr(ctx.survivalRewardItemImage(item))}" alt="" onerror="fallbackItemImage(this, '${ctx.survivalEscAttr(item.name)}')" /><span>${ctx.esc(item.displayName || item.name)}${items.length > 1 ? ` +${items.length - 1}` : ''}</span></span>`);
        if (gp) finalBits.push(`<span class="survival-chip">GP ${ctx.num(gp)}</span>`);
        if (cash) finalBits.push(`<span class="survival-chip blue">Cash ${ctx.num(cash)}</span>`);
        if (crown) finalBits.push(`<span class="survival-chip warn">Coroas ${ctx.num(crown)}</span>`);
        if (!finalBits.length) finalBits.push('<span class="survival-chip red">Sem final</span>');
        return `<button class="survival-reward-row survival-mission-row${row.missionType === selectedType ? ' checked' : ''}" type="button" data-survival-row="${idx}" data-mission-type="${ctx.survivalEscAttr(row.missionType)}" onclick="useSurvivalRewardAsDraft(${idx})">
          <div class="survival-mission-art">
            <img src="${ctx.survivalEscAttr(ctx.survivalMissionImage(row))}" alt="" onerror="this.src='/img/weapons/wiki_all/mission.png'" />
            <span>${idx + 1}</span>
          </div>
          <div class="survival-mission-main">
            <div class="survival-reward-title">
              <span>${ctx.esc(ctx.survivalMissionDisplayName(row))}</span>
              <span class="survival-chip${row.missionType === selectedType ? '' : ' blue'}">${row.missionType === selectedType ? 'Selecionado' : 'Abrir'}</span>
            </div>
            <div class="survival-reward-set">${ctx.esc(ctx.survivalMissionDifficulty(row))} - ${missionCount || 1} ${missionCount === 1 ? 'mapa' : 'mapas'}</div>
            <div class="survival-reward-summary">${finalBits.join('')}</div>
          </div>
        </button>`;
      }).join('');
      updateSurvivalSelectedCount();
    }

    async function loadSurvivalRewards(force) {
      if (!force && ctx.getSurvivalRewardsCache().length) {
        renderSurvivalRewards();
        return;
      }
      ctx.setBusy('survival-refresh-btn', true, 'LENDO');
      ctx.renderSkeleton('survival-rewards-list', 6);
      try {
        const r = await fetch('/api/survival-rewards', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) throw new Error(d.error || 'Falha ao carregar recompensas');
        ctx.setSurvivalRewardsConfig(d.config || null);
        const cfg = ctx.getSurvivalRewardsConfig();
        ctx.setSurvivalRewardsCache(cfg && Array.isArray(cfg.rows) ? cfg.rows : []);
        ctx.renderSurvivalRewardItemsDatalist(cfg && cfg.itemSuggestions && cfg.itemSuggestions.items);
        populateSurvivalTypeDropdown();
        if (ctx.$('survival-rewards-status')) {
          ctx.$('survival-rewards-status').textContent = `${ctx.getSurvivalRewardsCache().length} mapas carregados. Escolha um mapa e clique nas warboxes para montar o final.`;
        }
        renderSurvivalRewards();
        ctx.renderSurvivalMissionPreview(ctx.getCurrentSurvivalRow());
        setSurvivalResult('', false);
      } catch (e) {
        if (ctx.$('survival-rewards-list')) ctx.$('survival-rewards-list').innerHTML = `<div class="empty-state">Erro: ${ctx.esc(e.message)}</div>`;
        setSurvivalResult(e.message, true);
        ctx.showToast(`Falha nas recompensas: ${e.message}`, 'error');
      } finally {
        ctx.setBusy('survival-refresh-btn', false);
      }
    }

    function loadSelectedSurvivalType() {
      const sel = ctx.$('survival-type-select');
      if (!sel || !sel.value) {
        setSurvivalResult('Selecione um tipo no dropdown', true);
        return;
      }
      const idx = ctx.getSurvivalRewardsCache().findIndex((r) => r.missionType === sel.value);
      if (idx < 0) {
        setSurvivalResult('Tipo nao encontrado', true);
        return;
      }
      ctx.useSurvivalRewardAsDraft(idx);
    }

    return {
      setSurvivalResult,
      updateSurvivalSelectedCount,
      populateSurvivalTypeDropdown,
      renderSurvivalRewards,
      loadSurvivalRewards,
      loadSelectedSurvivalType
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.rewards = window.AdminPanelDomains.rewards || {};
  window.AdminPanelDomains.rewards.createRewardsSurvivalDomain = createRewardsSurvivalDomain;
})();
