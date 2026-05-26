(function initRewardsSurvivalActionsDomain() {
  function createRewardsSurvivalActionsDomain(ctx) {
    async function saveSelectedSurvivalType() {
      const sel = ctx.$('survival-type-select');
      if (!sel || !sel.value) {
        ctx.setSurvivalResult('Selecione um tipo no dropdown', true);
        return;
      }
      const fields = collectSurvivalRewardFields();
      if (!Object.keys(fields).length) {
        ctx.setSurvivalResult('Preencha ao menos um campo para aplicar', true);
        return;
      }
      ctx.setBusy('survival-apply-selected-btn', true, 'APLICANDO');
      try {
        const missionType = sel.value;
        const body = { scope: 'selected', missionTypes: [missionType], fields };
        const r = await fetch('/api/survival-rewards/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(body)
        });
        const d = await r.json();
        if (!d.success) throw new Error(d.error || 'Falha ao salvar');
        await ctx.loadSurvivalRewards(true);
        const idx = ctx.getSurvivalRewardsCache().findIndex((row) => row.missionType === missionType);
        if (idx >= 0) ctx.useSurvivalRewardAsDraft(idx);
        ctx.setSurvivalResult(`${missionType}: ${d.message || 'Atualizado!'}`, false);
        ctx.showToast(`${missionType} atualizado com sucesso!`);
      } catch (e) {
        ctx.setSurvivalResult(e.message, true);
        ctx.showToast(`Erro: ${e.message}`, 'error');
      } finally {
        ctx.setBusy('survival-apply-selected-btn', false);
      }
    }

    function getSelectedSurvivalMissionTypes() {
      const checked = Array.from(document.querySelectorAll('.survival-reward-check:checked')).map((input) => input.value).filter(Boolean);
      if (checked.length) return checked;
      const selected = ctx.$('survival-type-select') ? ctx.$('survival-type-select').value : '';
      return selected ? [selected] : [];
    }

    function setSurvivalInput(id, value) {
      const el = ctx.$(id);
      if (el) el.value = value === undefined || value === null ? '' : String(value);
    }

    function formatSecondsAsMinutes(value) {
      const n = Number(value);
      if (!Number.isFinite(n) || n < 0) return '';
      const minutes = n / 60;
      return Number.isInteger(minutes) ? String(minutes) : String(Math.round(minutes * 10) / 10);
    }

    function minutesToSecondsString(value) {
      const n = Number(String(value || '').replace(',', '.'));
      if (!Number.isFinite(n) || n < 0) return '';
      return String(Math.round(n * 60));
    }

    function collectSurvivalRewardFields() {
      const map = {
        rewardItemName: 'survival-reward-item',
        rewardItemAmount: 'survival-reward-item-amount',
        rewardItemExpiration: 'survival-reward-item-expiration',
        rewardItemDurability: 'survival-reward-item-durability',
        gpAmount: 'survival-reward-gp',
        cashAmount: 'survival-reward-cash',
        crownAmount: 'survival-reward-crown',
        moneyMultiplier: 'survival-money-multiplier',
        xpMultiplier: 'survival-xp-multiplier',
        cashMultiplier: 'survival-cash-multiplier',
        crownBronze: 'survival-crown-bronze',
        crownSilver: 'survival-crown-silver',
        crownGold: 'survival-crown-gold',
        winPool: 'survival-win-pool',
        losePool: 'survival-lose-pool',
        drawPool: 'survival-draw-pool',
        scorePool: 'survival-score-pool',
        rewardPoolValue: 'survival-reward-pool',
        bonusPool: 'survival-bonus-pool',
        scoreBronze: 'survival-score-bronze',
        scoreSilver: 'survival-score-silver',
        scoreGold: 'survival-score-gold',
        timeBronze: 'survival-time-bronze',
        timeSilver: 'survival-time-silver',
        timeGold: 'survival-time-gold'
      };
      const fields = {};
      Object.entries(map).forEach(([key, id]) => {
        const el = ctx.$(id);
        const value = el ? String(el.value || '').trim() : '';
        if (value !== '') fields[key] = value;
      });
      ['timeBronze', 'timeSilver', 'timeGold'].forEach((key) => {
        if (Object.prototype.hasOwnProperty.call(fields, key)) {
          const sec = minutesToSecondsString(fields[key]);
          if (sec !== '') fields[key] = sec;
          else delete fields[key];
        }
      });
      if (ctx.$('survival-selected-items')) {
        delete fields.rewardItemName;
        delete fields.rewardItemAmount;
        delete fields.rewardItemExpiration;
        delete fields.rewardItemDurability;
        if (ctx.getSurvivalRewardItemsDirty()) {
          fields.rewardItems = ctx.getSurvivalSelectedRewardItems().map((item) => ({
            name: item.name,
            amount: String(item.amount || '').trim(),
            expiration: String(item.expiration || '').trim(),
            durability: String(item.durability || '').trim()
          }));
        }
      }
      return fields;
    }

    async function saveSurvivalRewards(applyAll) {
      const fields = collectSurvivalRewardFields();
      if (!Object.keys(fields).length) {
        ctx.setSurvivalResult('Preencha ao menos um campo para aplicar', true);
        return;
      }
      const missionTypes = applyAll ? [] : getSelectedSurvivalMissionTypes();
      if (!applyAll && !missionTypes.length) {
        ctx.setSurvivalResult('Selecione ao menos um tipo de sobrevivencia', true);
        return;
      }
      if (applyAll && !ctx.confirmDanger('Aplicar estes valores em todos os tipos de sobrevivencia/op especial?')) return;
      const buttonId = applyAll ? 'survival-apply-all-btn' : 'survival-apply-selected-btn';
      ctx.setBusy(buttonId, true, 'APLICANDO');
      try {
        const body = { scope: applyAll ? 'all' : 'selected', missionTypes, fields };
        const r = await fetch('/api/survival-rewards/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(body)
        });
        const d = await r.json();
        if (!d.success) throw new Error(d.error || 'Falha ao salvar recompensas');
        ctx.setSurvivalResult(d.message || 'Recompensas atualizadas', false);
        ctx.showToast(d.message || 'Recompensas atualizadas');
        await ctx.loadSurvivalRewards(true);
      } catch (e) {
        ctx.setSurvivalResult(e.message, true);
        ctx.showToast(`Erro nas recompensas: ${e.message}`, 'error');
      } finally {
        ctx.setBusy(buttonId, false);
      }
    }

    function toggleGlobalRewardPanel() {
      ctx.setGlobalRewardPanelOpen(!ctx.getGlobalRewardPanelOpen());
      const body = ctx.$('survival-global-body');
      const icon = ctx.$('global-toggle-icon');
      if (body) body.classList.toggle('collapsed', !ctx.getGlobalRewardPanelOpen());
      if (icon) icon.classList.toggle('collapsed', !ctx.getGlobalRewardPanelOpen());
    }

    function setGlobalInput(id, value) {
      const el = ctx.$(id);
      if (el) el.value = value === undefined || value === null ? '' : String(value);
    }

    function collectGlobalRewardFields() {
      const map = {
        rewardItemName: 'global-reward-item',
        rewardItemAmount: 'global-reward-item-amount',
        rewardItemExpiration: 'global-reward-item-expiration',
        gpAmount: 'global-reward-gp',
        cashAmount: 'global-reward-cash',
        crownAmount: 'global-reward-crown',
        moneyMultiplier: 'global-money-multiplier',
        xpMultiplier: 'global-xp-multiplier',
        cashMultiplier: 'global-cash-multiplier',
        crownBronze: 'global-crown-bronze',
        crownSilver: 'global-crown-silver',
        crownGold: 'global-crown-gold',
        winPool: 'global-win-pool',
        losePool: 'global-lose-pool',
        drawPool: 'global-draw-pool',
        scorePool: 'global-score-pool',
        rewardPoolValue: 'global-reward-pool',
        bonusPool: 'global-bonus-pool',
        scoreBronze: 'global-score-bronze',
        scoreSilver: 'global-score-silver',
        scoreGold: 'global-score-gold',
        timeBronze: 'global-time-bronze',
        timeSilver: 'global-time-silver',
        timeGold: 'global-time-gold'
      };
      const fields = {};
      Object.entries(map).forEach(([key, id]) => {
        const el = ctx.$(id);
        const value = el ? String(el.value || '').trim() : '';
        if (value !== '') fields[key] = value;
      });
      ['timeBronze', 'timeSilver', 'timeGold'].forEach((key) => {
        if (Object.prototype.hasOwnProperty.call(fields, key)) {
          const sec = minutesToSecondsString(fields[key]);
          if (sec !== '') fields[key] = sec;
          else delete fields[key];
        }
      });
      return fields;
    }

    function clearGlobalFields() {
      [
        'global-reward-item', 'global-reward-item-amount', 'global-reward-item-expiration',
        'global-reward-gp', 'global-reward-cash', 'global-reward-crown',
        'global-money-multiplier', 'global-xp-multiplier', 'global-cash-multiplier',
        'global-crown-bronze', 'global-crown-silver', 'global-crown-gold',
        'global-win-pool', 'global-lose-pool', 'global-draw-pool', 'global-score-pool',
        'global-reward-pool', 'global-bonus-pool',
        'global-score-bronze', 'global-score-silver', 'global-score-gold',
        'global-time-bronze', 'global-time-silver', 'global-time-gold'
      ].forEach((id) => { const el = ctx.$(id); if (el) el.value = ''; });
    }

    function setGlobalResult(message, isError) {
      const el = ctx.$('global-rewards-result');
      if (!el) return;
      if (!message) { el.classList.add('hidden'); return; }
      el.textContent = message;
      el.className = `cmd-result${isError ? ' error' : ''}`;
      el.classList.remove('hidden');
    }

    function applyGlobalPreset(tier) {
      const presets = {
        low: { gp: 500, cash: 50, crown: 10, gpMult: '0.4', xpMult: '1.0', cashMult: '0.5', crownBronze: 5, crownSilver: 15, crownGold: 30, winPool: 50, losePool: 10, scorePool: 0, bonusPool: 200, scoreBronze: 500, scoreSilver: 1000, scoreGold: 2000, timeBronze: 600, timeSilver: 400, timeGold: 240 },
        medium: { gp: 1500, cash: 150, crown: 30, gpMult: '0.8', xpMult: '1.5', cashMult: '1.0', crownBronze: 15, crownSilver: 40, crownGold: 75, winPool: 100, losePool: 25, scorePool: 0, bonusPool: 400, scoreBronze: 1000, scoreSilver: 2000, scoreGold: 4000, timeBronze: 500, timeSilver: 320, timeGold: 200 },
        high: { gp: 5000, cash: 500, crown: 80, gpMult: '1.5', xpMult: '2.5', cashMult: '2.0', crownBronze: 30, crownSilver: 80, crownGold: 150, winPool: 200, losePool: 50, scorePool: 0, bonusPool: 800, scoreBronze: 2000, scoreSilver: 4000, scoreGold: 8000, timeBronze: 400, timeSilver: 260, timeGold: 160 },
        extreme: { gp: 15000, cash: 1500, crown: 200, gpMult: '3.0', xpMult: '5.0', cashMult: '4.0', crownBronze: 60, crownSilver: 150, crownGold: 300, winPool: 500, losePool: 100, scorePool: 0, bonusPool: 2000, scoreBronze: 5000, scoreSilver: 10000, scoreGold: 20000, timeBronze: 300, timeSilver: 200, timeGold: 120 }
      };
      const p = presets[tier];
      if (!p) return;
      setGlobalInput('global-reward-gp', p.gp);
      setGlobalInput('global-reward-cash', p.cash);
      setGlobalInput('global-reward-crown', p.crown);
      setGlobalInput('global-money-multiplier', p.gpMult);
      setGlobalInput('global-xp-multiplier', p.xpMult);
      setGlobalInput('global-cash-multiplier', p.cashMult);
      setGlobalInput('global-crown-bronze', p.crownBronze);
      setGlobalInput('global-crown-silver', p.crownSilver);
      setGlobalInput('global-crown-gold', p.crownGold);
      setGlobalInput('global-win-pool', p.winPool);
      setGlobalInput('global-lose-pool', p.losePool);
      setGlobalInput('global-score-pool', p.scorePool);
      setGlobalInput('global-bonus-pool', p.bonusPool);
      setGlobalInput('global-score-bronze', p.scoreBronze);
      setGlobalInput('global-score-silver', p.scoreSilver);
      setGlobalInput('global-score-gold', p.scoreGold);
      setGlobalInput('global-time-bronze', formatSecondsAsMinutes(p.timeBronze));
      setGlobalInput('global-time-silver', formatSecondsAsMinutes(p.timeSilver));
      setGlobalInput('global-time-gold', formatSecondsAsMinutes(p.timeGold));
      setGlobalResult(`Preset ${tier.toUpperCase()} carregado - revise e clique em APLICAR`, false);
    }

    async function applyGlobalReward() {
      const fields = collectGlobalRewardFields();
      if (!Object.keys(fields).length) {
        setGlobalResult('Preencha ao menos um campo para aplicar', true);
        return;
      }
      if (!ctx.confirmDanger('Aplicar estes valores em TODOS os tipos de sobrevivencia? Esta acao nao pode ser desfeita.')) return;
      ctx.setBusy('global-apply-btn', true, 'APLICANDO...');
      try {
        const body = { scope: 'all', missionTypes: [], fields };
        const r = await fetch('/api/survival-rewards/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(body)
        });
        const d = await r.json();
        if (!d.success) throw new Error(d.error || 'Falha ao salvar recompensas');
        setGlobalResult(d.message || 'Recompensas globais aplicadas com sucesso!', false);
        ctx.showToast('Recompensas globais aplicadas em todos os sobrevivencias!');
        await ctx.loadSurvivalRewards(true);
      } catch (e) {
        setGlobalResult(e.message, true);
        ctx.showToast(`Erro: ${e.message}`, 'error');
      } finally {
        ctx.setBusy('global-apply-btn', false);
      }
    }

    return {
      saveSelectedSurvivalType,
      getSelectedSurvivalMissionTypes,
      setSurvivalInput,
      formatSecondsAsMinutes,
      minutesToSecondsString,
      collectSurvivalRewardFields,
      saveSurvivalRewards,
      toggleGlobalRewardPanel,
      setGlobalInput,
      collectGlobalRewardFields,
      clearGlobalFields,
      applyGlobalPreset,
      setGlobalResult,
      applyGlobalReward
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.rewards = window.AdminPanelDomains.rewards || {};
  window.AdminPanelDomains.rewards.createRewardsSurvivalActionsDomain = createRewardsSurvivalActionsDomain;
})();
