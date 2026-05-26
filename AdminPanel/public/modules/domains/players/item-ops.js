(function initPlayersItemOpsDomain() {
  function createPlayersItemOpsDomain(ctx) {
    let lastPreviewVisualKey = '';
    async function removeItem() {
      const nick = ctx.$('removeitem-nick').value.trim();
      const item = ctx.$('removeitem-name').value.trim();
      const el = ctx.$('removeitem-result');
      const model = ctx.getPanelModel();
      const nickMin = (model.nick && model.nick.minLen) || ctx.DEFAULT_PANEL_MODEL.nick.minLen;
      const nickMax = (model.nick && model.nick.maxLen) || ctx.DEFAULT_PANEL_MODEL.nick.maxLen;
      if (!nick || nick.length < nickMin || nick.length > nickMax) {
        el.textContent = `Nick deve ter entre ${nickMin} e ${nickMax} caracteres`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      if (!item) {
        el.textContent = 'Informe nome ou ID do item';
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }

      const body = { nick };
      const itemId = ctx.parseStrictIntInput(item);
      if (itemId !== null) {
        if (itemId < 1 || itemId > 2147483647) {
          el.textContent = 'ID do item fora do limite permitido';
          el.className = 'cmd-result error';
          el.classList.remove('hidden');
          return;
        }
        body.item_id = itemId;
      } else {
        const normalized = item.toLowerCase();
        if (!/^[a-z0-9_]+$/i.test(normalized)) {
          el.textContent = 'Nome do item invalido';
          el.className = 'cmd-result error';
          el.classList.remove('hidden');
          return;
        }
        body.item_name = normalized;
      }
      try {
        const r = await fetch('/api/removeitem', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(body)
        });
        const d = await r.json();
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch {}
    }

    async function sendBroadcast() {
      const msg = ctx.$('bcast-msg').value.trim();
      if (!msg) return;
      try {
        const r = await fetch('/api/broadcast', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ message: msg })
        });
        const d = await r.json();
        const el = ctx.$('bcast-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch (e) { alert('Erro: ' + e.message); }
    }

    async function sendNotification() {
      const msg = ctx.$('notif-msg').value.trim();
      if (!msg) return;
      try {
        const r = await fetch('/api/notification', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ message: msg })
        });
        const d = await r.json();
        const el = ctx.$('bcast-result');
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch (e) { alert('Erro: ' + e.message); }
    }

    function isGiveItemCandidateKey(key) {
      const k = String(key || '').toLowerCase();
      if (!k) return false;
      if (k.startsWith('achievement_') || k.startsWith('achievementunlock_') || k.startsWith('challenge_')) return false;
      if (k.includes('unlock_aw_') || k.includes('badge_') || k.includes('mark_')) return false;
      if (/^(ar|sr|smg|shg|mg|hmg|pt|kn)\d+/i.test(k)) return true;
      if (k.includes('random_box') || k.includes('warbox') || k.includes('box')) return true;
      if (k.includes('weapon') || k.includes('skin') || k.includes('armor') || k.includes('consumable')) return true;
      return false;
    }

    async function loadItemSuggestions() {
      if (ctx.getItemSuggestionsLoaded()) return;
      await ctx.loadItemNames();
      const itemNames = ctx.getItemNames();
      if (!itemNames) return;
      ctx.setItemSuggestionsLoaded(true);
      const datalist = ctx.$('item-suggest');
      const keys = Object.keys(itemNames).filter(isGiveItemCandidateKey).sort();
      datalist.innerHTML = keys.slice(0, 300).map(k => `<option value="${ctx.esc(k)}">${ctx.esc(itemNames[k])}</option>`).join('');
    }

    async function ensureGiveItemVisual(key) {
      const norm = String(key || '').trim().toLowerCase();
      if (!norm) return;
      const names = ctx.getItemNames() || {};
      try {
        await ctx.enrichWeaponVisuals([{ key: norm, displayName: names[norm] || norm }], false, 2);
      } catch {}
    }

    async function setGiveItemName(value) {
      const input = ctx.$('item-name');
      if (!input) return;
      const norm = String(value || '').toLowerCase();
      input.value = norm;
      await ensureGiveItemVisual(norm);
      renderGiveItemPreview();
      hideGiveItemSuggestions();
    }

    function renderGiveItemPreview() {
      const host = ctx.$('item-picked-preview');
      const input = ctx.$('item-name');
      const itemNames = ctx.getItemNames();
      if (!host || !input) return;
      const key = String(input.value || '').trim().toLowerCase();
      if (!key) {
        lastPreviewVisualKey = '';
        host.classList.add('hidden');
        host.innerHTML = '';
        return;
      }
      if (key !== lastPreviewVisualKey) {
        lastPreviewVisualKey = key;
        ensureGiveItemVisual(key).then(() => {
          if (lastPreviewVisualKey === key) renderGiveItemPreview();
        });
      }
      const title = ctx.weaponVisualTitle(key, itemNames && itemNames[key] ? itemNames[key] : key);
      host.innerHTML = `<img class="iri-thumb" src="${ctx.esc(ctx.weaponVisualImage(key))}" onerror="fallbackItemImage(this, '${ctx.esc(key)}')" /><span class="iri-text"><span class="iri-key">${ctx.esc(key)}</span><span class="iri-name">${ctx.esc(title)}</span></span>`;
      host.classList.remove('hidden');
    }

    function hideGiveItemSuggestions() {
      const box = ctx.$('item-name-suggestions');
      if (!box) return;
      box.classList.add('hidden');
      box.innerHTML = '';
    }

    async function renderGiveItemSuggestions() {
      const input = ctx.$('item-name');
      const box = ctx.$('item-name-suggestions');
      const itemNames = ctx.getItemNames();
      if (!input || !box) return;
      const q = String(input.value || '').trim().toLowerCase();
      if (q.length < 2) { hideGiveItemSuggestions(); return; }
      await ctx.loadItemNames();
      const names = ctx.getItemNames();
      if (!names) { hideGiveItemSuggestions(); return; }
      const keys = Object.keys(names)
        .filter(isGiveItemCandidateKey)
        .filter(k => k.includes(q) || String(names[k] || '').toLowerCase().includes(q))
        .slice(0, 40);
      if (!keys.length) { hideGiveItemSuggestions(); return; }
      await ctx.enrichWeaponVisuals(keys.slice(0, 20).map(k => ({ key: k, displayName: names[k] })), false, 10);
      box.innerHTML = keys.map(k => `<div class="item-result-item" onclick="setGiveItemName('${ctx.esc(k)}')"><img class="iri-thumb" src="${ctx.esc(ctx.weaponVisualImage(k))}" onerror="fallbackItemImage(this, '${ctx.esc(k)}')" /><span class="iri-text"><span class="iri-key">${ctx.esc(k)}</span><span class="iri-name">${ctx.esc(ctx.weaponVisualTitle(k, names[k]))}</span></span></div>`).join('');
      box.classList.remove('hidden');
    }

    async function searchItems() {
      const q = ctx.$('item-search').value.trim().toLowerCase();
      const container = ctx.$('item-search-results');
      if (q.length < 2) { container.innerHTML = ''; return; }
      await ctx.loadItemNames();
      const itemNames = ctx.getItemNames();
      if (!itemNames) { container.innerHTML = '<div class="empty-state">Nenhum item carregado</div>'; return; }
      const matches = Object.keys(itemNames)
        .filter(isGiveItemCandidateKey)
        .filter(k => k.includes(q) || (itemNames[k] || '').toLowerCase().includes(q))
        .slice(0, 100);
      if (!matches.length) { container.innerHTML = '<div class="empty-state">Nenhum item encontrado</div>'; return; }
      await ctx.enrichWeaponVisuals(matches.slice(0, 40).map(k => ({ key: k, displayName: itemNames[k] })), false, 12);
      container.innerHTML = matches.map(k => `<div class="item-result-item" onclick="setGiveItemName('${ctx.esc(k)}')"><img class="iri-thumb" src="${ctx.esc(ctx.weaponVisualImage(k))}" onerror="fallbackItemImage(this, '${ctx.esc(k)}')" /><span class="iri-text"><span class="iri-key">${ctx.esc(k)}</span> - <span class="iri-name">${ctx.esc(ctx.weaponVisualTitle(k, itemNames[k]))}</span></span></div>`).join('');
    }

    async function giveItem() {
      const nick = ctx.$('item-nick').value.trim();
      const itemNameRaw = ctx.$('item-name').value.trim();
      const qtyRaw = ctx.$('item-qty').value || '1';
      const expRaw = ctx.$('item-exp').value || '0';
      const el = ctx.$('item-result');
      const model = ctx.getPanelModel();
      const nickMin = (model.nick && model.nick.minLen) || ctx.DEFAULT_PANEL_MODEL.nick.minLen;
      const nickMax = (model.nick && model.nick.maxLen) || ctx.DEFAULT_PANEL_MODEL.nick.maxLen;
      if (!nick) { el.textContent = 'Digite o nickname'; el.className = 'cmd-result error'; el.classList.remove('hidden'); return; }
      if (nick.length < nickMin || nick.length > nickMax) {
        el.textContent = `Nick deve ter entre ${nickMin} e ${nickMax} caracteres`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      const itemName = itemNameRaw.toLowerCase();
      if (!itemName) { el.textContent = 'Digite o nome do item'; el.className = 'cmd-result error'; el.classList.remove('hidden'); return; }
      if (!/^[a-z0-9_]+$/i.test(itemName)) {
        el.textContent = 'Nome do item invalido';
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }

      const itemLenMin = (model.item && model.item.minLen) || ctx.DEFAULT_PANEL_MODEL.item.minLen;
      const itemLenMax = (model.item && model.item.maxLen) || ctx.DEFAULT_PANEL_MODEL.item.maxLen;
      if (itemName.length < itemLenMin || itemName.length > itemLenMax) {
        el.textContent = `Nome do item deve ter entre ${itemLenMin} e ${itemLenMax} caracteres`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }

      await ctx.loadItemNames();
      const itemNames = ctx.getItemNames();
      const baseItem = itemName.replace(/_(shop|default|game|bronze|silver|gold|diamond|premium)$/i, '');
      const hasLocalCatalog = itemNames && Object.keys(itemNames).length > 0;
      // Do not hard-block by local catalog only: some valid live IDs are present
      // in assets/wiki_all but may not exist in the local item-name cache yet.
      if (hasLocalCatalog && !itemNames[itemName] && !itemNames[baseItem]) {
        console.warn('[giveitem] Item not found in local catalog, proceeding:', itemName);
      }

      const qty = ctx.parseStrictIntInput(qtyRaw);
      const exp = ctx.parseStrictIntInput(expRaw);
      const qtyRange = (model.item && model.item.quantity) || ctx.DEFAULT_PANEL_MODEL.item.quantity;
      const expRange = (model.item && model.item.expirationHours) || ctx.DEFAULT_PANEL_MODEL.item.expirationHours;

      if (qty === null || qty < qtyRange.min || qty > qtyRange.max) {
        el.textContent = `Quantidade deve estar entre ${qtyRange.min} e ${qtyRange.max}`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }
      if (exp === null || exp < expRange.min || exp > expRange.max) {
        el.textContent = `Duracao deve estar entre ${expRange.min} e ${expRange.max} horas`;
        el.className = 'cmd-result error';
        el.classList.remove('hidden');
        return;
      }

      try {
        const r = await fetch('/api/giveitem', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ nick, item_name: itemName, quantity: qty, expiration_hours: exp })
        });
        const d = await r.json();
        el.textContent = d.message || d.error || 'Erro';
        el.className = 'cmd-result' + (d.success ? '' : ' error');
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 5000);
      } catch (e) { alert('Erro: ' + e.message); }
    }

    return {
      removeItem,
      sendBroadcast,
      sendNotification,
      isGiveItemCandidateKey,
      loadItemSuggestions,
      setGiveItemName,
      renderGiveItemPreview,
      hideGiveItemSuggestions,
      renderGiveItemSuggestions,
      searchItems,
      giveItem
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.players = window.AdminPanelDomains.players || {};
  window.AdminPanelDomains.players.createPlayersItemOpsDomain = createPlayersItemOpsDomain;
})();
