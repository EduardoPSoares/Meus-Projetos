(function initShopPackagesDomain() {
  function createShopPackagesDomain(ctx) {
    function setShopPackageResult(message, isError) {
      const el = ctx.$('shop-package-result');
      if (!el) return;
      if (!message) {
        el.classList.add('hidden');
        return;
      }
      el.textContent = message;
      el.className = `cmd-result${isError ? ' error' : ''}`;
      el.classList.remove('hidden');
      ctx.showToast(message, isError ? 'error' : 'success');
    }

    function renderShopPackagesSelect() {
      const shopPackagesCache = ctx.getShopPackagesCache();
      const shopRotationCache = ctx.getShopRotationCache();
      const selectEl = ctx.$('shop-packages-select');
      const rotationEl = ctx.$('shop-rotation-packages');
      const selectedBefore = ctx.getShopPendingSelectPackageId() || (selectEl ? selectEl.value : '');
      const rotationSelected = new Set(shopRotationCache && Array.isArray(shopRotationCache.packageIds) ? shopRotationCache.packageIds : []);

      if (selectEl) {
        if (!shopPackagesCache.length) {
          selectEl.innerHTML = '';
        } else {
          selectEl.innerHTML = shopPackagesCache.map((p) => {
            const updated = p.updatedAt ? new Date(p.updatedAt).toLocaleDateString('pt-BR') : '--';
            const invalid = Number(p.removedInvalidCount) || 0;
            const label = `${p.name} (${p.offersCount}${invalid ? `, ${invalid} fora do jogo` : ''}) [${updated}]`;
            return `<option value="${ctx.esc(p.id)}">${ctx.esc(label)}</option>`;
          }).join('');
          if (selectedBefore && shopPackagesCache.some((p) => p.id === selectedBefore)) {
            selectEl.value = selectedBefore;
          }
          if (!selectEl.value && shopPackagesCache[0]) selectEl.value = shopPackagesCache[0].id;
        }
      }

      if (rotationEl) {
        rotationEl.innerHTML = shopPackagesCache.map((p) => {
          const selected = rotationSelected.has(p.id) ? ' selected' : '';
          return `<option value="${ctx.esc(p.id)}"${selected}>${ctx.esc(`${p.name} (${p.offersCount})`)}</option>`;
        }).join('');
      }

      ctx.setShopPendingSelectPackageId('');
      renderSelectedShopPackageMeta();
    }

    function renderSelectedShopPackageMeta() {
      const shopPackagesCache = ctx.getShopPackagesCache();
      const el = ctx.$('shop-package-selected-meta');
      if (!el) return;
      const selectEl = ctx.$('shop-packages-select');
      const id = selectEl ? String(selectEl.value || '') : '';
      if (!id) {
        el.textContent = 'Nenhum pacote selecionado';
        return;
      }
      const pkg = shopPackagesCache.find((p) => p.id === id);
      if (!pkg) {
        el.textContent = 'Pacote nao encontrado';
        return;
      }
      const updatedAt = pkg.updatedAt ? new Date(pkg.updatedAt).toLocaleString('pt-BR') : '--';
      const invalid = Number(pkg.removedInvalidCount) || 0;
      el.textContent = `${pkg.name} | ${pkg.offersCount} itens${invalid ? ` | ${invalid} fora do jogo ocultos` : ''} | atualizado em ${updatedAt}`;
    }

    function renderShopRotationMeta() {
      const cfg = ctx.getShopRotationCache() || {};
      const el = ctx.$('shop-rotation-meta');
      if (!el) return;
      const enabled = !!cfg.enabled;
      const count = Array.isArray(cfg.packageIds) ? cfg.packageIds.length : 0;
      const interval = Number(cfg.intervalMinutes) || 60;
      const nextAt = cfg.nextRunAt ? new Date(cfg.nextRunAt).toLocaleString('pt-BR') : '--';
      const lastAt = cfg.lastAppliedAt ? new Date(cfg.lastAppliedAt).toLocaleString('pt-BR') : '--';
      const lastPkg = cfg.lastAppliedPackageId || '--';
      el.textContent = enabled
        ? `Ativo | ${count} pacotes | intervalo ${interval} min | proxima: ${nextAt} | ultimo pacote: ${lastPkg} em ${lastAt}`
        : 'Rotacao desativada';
    }

    function applyRotationToInputs() {
      const cfg = ctx.getShopRotationCache() || {};
      if (ctx.$('shop-rotation-enabled')) ctx.$('shop-rotation-enabled').checked = !!cfg.enabled;
      if (ctx.$('shop-rotation-interval')) ctx.$('shop-rotation-interval').value = String(Number(cfg.intervalMinutes) || 60);
      if (ctx.$('shop-rotation-packages') && Array.isArray(cfg.packageIds)) {
        const selected = new Set(cfg.packageIds);
        Array.from(ctx.$('shop-rotation-packages').options).forEach((opt) => {
          opt.selected = selected.has(opt.value);
        });
      }
      renderShopRotationMeta();
    }

    async function loadShopPackagesAndRotation() {
      try {
        const r = await fetch('/api/shop/rotation', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) throw new Error(d.error || 'Falha ao carregar pacotes');
        ctx.setShopPackagesCache(Array.isArray(d.packages) ? d.packages : []);
        ctx.setShopRotationCache(d.rotation || null);
        renderShopPackagesSelect();
        applyRotationToInputs();
      } catch (e) {
        setShopPackageResult(`Falha ao carregar pacotes/rotacao: ${e.message}`, true);
      }
    }

    function getSelectedShopPackageId() {
      return String(ctx.$('shop-packages-select') && ctx.$('shop-packages-select').value || '').trim();
    }

    async function saveShopPackage(updateExisting) {
      const name = String(ctx.$('shop-package-name') && ctx.$('shop-package-name').value || '').trim();
      const description = String(ctx.$('shop-package-desc') && ctx.$('shop-package-desc').value || '').trim();
      const offers = ctx.getShopBuilderOffers();
      if (name.length < 3 || name.length > 64) {
        setShopPackageResult('Nome do pacote deve ter entre 3 e 64 caracteres', true);
        return;
      }
      if (!offers.length) {
        setShopPackageResult('Adicione ao menos 1 item ao pacote', true);
        return;
      }
      const body = { name, description, offers };
      if (updateExisting) {
        const selectedId = String(ctx.$('shop-packages-select') && ctx.$('shop-packages-select').value || '').trim();
        if (!selectedId) {
          setShopPackageResult('Selecione um pacote para atualizar', true);
          return;
        }
        body.id = selectedId;
      }
      try {
        const r = await fetch('/api/shop/packages/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify(body)
        });
        const d = await r.json();
        if (!d.success) {
          setShopPackageResult(d.error || 'Erro ao salvar pacote', true);
          return;
        }
        ctx.setShopPendingSelectPackageId(d.package && d.package.id ? d.package.id : '');
        await loadShopPackagesAndRotation();
        setShopPackageResult(d.message || 'Pacote salvo', false);
      } catch (e) {
        setShopPackageResult(`Falha ao salvar pacote: ${e.message}`, true);
      }
    }

    async function loadSelectedShopPackage() {
      const id = getSelectedShopPackageId();
      if (!id) {
        setShopPackageResult('Selecione um pacote', true);
        return;
      }
      try {
        const r = await fetch(`/api/shop/packages/get?id=${encodeURIComponent(id)}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success || !d.package) {
          setShopPackageResult(d.error || 'Pacote nao encontrado', true);
          return;
        }
        if (ctx.$('shop-package-name')) ctx.$('shop-package-name').value = d.package.name || '';
        if (ctx.$('shop-package-desc')) ctx.$('shop-package-desc').value = d.package.description || '';
        ctx.setShopBuilderOffers(d.package.offers || []);
        setShopPackageResult(`Pacote ${d.package.name} carregado para edicao`, false);
      } catch (e) {
        setShopPackageResult(`Falha ao carregar pacote: ${e.message}`, true);
      }
    }

    async function applySelectedShopPackage(mode) {
      const id = getSelectedShopPackageId();
      if (!id) {
        setShopPackageResult('Selecione um pacote para aplicar', true);
        return;
      }
      const applyMode = mode === 'merge' ? 'merge' : 'replace';
      try {
        const r = await fetch('/api/shop/packages/apply', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ id, mode: applyMode, regenerate: true })
        });
        const d = await r.json();
        if (!d.success) {
          setShopPackageResult(d.error || 'Falha ao aplicar pacote', true);
          return;
        }
        setShopPackageResult(`${d.message || 'Pacote aplicado'} (${applyMode})`, false);
        await ctx.loadShopOffers(false);
        await loadShopPackagesAndRotation();
      } catch (e) {
        setShopPackageResult(`Erro ao aplicar pacote: ${e.message}`, true);
      }
    }

    async function deleteSelectedShopPackage() {
      const id = getSelectedShopPackageId();
      if (!id) {
        setShopPackageResult('Selecione um pacote para excluir', true);
        return;
      }
      const pkg = (ctx.getShopPackagesCache() || []).find((p) => p.id === id);
      const pkgName = pkg ? pkg.name : id;
      if (!ctx.confirmDanger(`Excluir pacote "${pkgName}"?`)) return;
      try {
        const r = await fetch('/api/shop/packages/delete', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ id })
        });
        const d = await r.json();
        if (!d.success) {
          setShopPackageResult(d.error || 'Falha ao excluir pacote', true);
          return;
        }
        ctx.setShopPendingSelectPackageId('');
        await loadShopPackagesAndRotation();
        setShopPackageResult(d.message || 'Pacote removido', false);
      } catch (e) {
        setShopPackageResult(`Falha ao excluir pacote: ${e.message}`, true);
      }
    }

    function getSelectedRotationPackageIds() {
      const selectEl = ctx.$('shop-rotation-packages');
      if (!selectEl) return [];
      return Array.from(selectEl.options).filter((opt) => opt.selected).map((opt) => opt.value);
    }

    async function saveShopRotation(runNow) {
      const enabled = !!(ctx.$('shop-rotation-enabled') && ctx.$('shop-rotation-enabled').checked);
      const intervalRaw = ctx.$('shop-rotation-interval') ? ctx.$('shop-rotation-interval').value : '60';
      const intervalMinutes = ctx.parseStrictIntInput(intervalRaw);
      if (intervalMinutes === null || intervalMinutes < 5 || intervalMinutes > 10080) {
        setShopPackageResult('Intervalo invalido (5 a 10080 minutos)', true);
        return;
      }
      const packageIds = getSelectedRotationPackageIds();
      if (enabled && !packageIds.length) {
        setShopPackageResult('Selecione ao menos 1 pacote para ativar a rotacao', true);
        return;
      }
      try {
        const r = await fetch('/api/shop/rotation/set', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': ctx.getToken() },
          body: JSON.stringify({ enabled, intervalMinutes, packageIds, runNow: !!runNow })
        });
        const d = await r.json();
        if (!d.success) {
          setShopPackageResult(d.error || 'Falha ao salvar rotacao', true);
          return;
        }
        ctx.setShopRotationCache(d.rotation || ctx.getShopRotationCache());
        applyRotationToInputs();
        setShopPackageResult(d.message || 'Rotacao salva', false);
        if (runNow && enabled) await runShopRotationNow();
      } catch (e) {
        setShopPackageResult(`Falha ao salvar rotacao: ${e.message}`, true);
      }
    }

    async function runShopRotationNow() {
      try {
        const r = await fetch('/api/shop/rotation/run', {
          method: 'POST',
          headers: { 'X-Auth-Token': ctx.getToken() }
        });
        const d = await r.json();
        if (!d.success) {
          setShopPackageResult(d.error || 'Falha ao executar rotacao', true);
          return;
        }
        ctx.setShopRotationCache(d.rotation || ctx.getShopRotationCache());
        applyRotationToInputs();
        setShopPackageResult(d.message || 'Rotacao executada', false);
        await ctx.loadShopOffers(false);
      } catch (e) {
        setShopPackageResult(`Falha ao executar rotacao: ${e.message}`, true);
      }
    }

    return {
      setShopPackageResult,
      renderShopPackagesSelect,
      renderSelectedShopPackageMeta,
      renderShopRotationMeta,
      applyRotationToInputs,
      loadShopPackagesAndRotation,
      saveShopPackage,
      getSelectedShopPackageId,
      loadSelectedShopPackage,
      applySelectedShopPackage,
      deleteSelectedShopPackage,
      getSelectedRotationPackageIds,
      saveShopRotation,
      runShopRotationNow
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.shop = window.AdminPanelDomains.shop || {};
  window.AdminPanelDomains.shop.createShopPackagesDomain = createShopPackagesDomain;
})();
