(function initShopOffersDomain() {
  function createShopOffersDomain(ctx) {
    async function loadShopOffers(resetOffset) {
      if (resetOffset) ctx.setShopPageOffset(0);
      const q = ctx.$('shop-search').value.trim();
      const shopStatusFilter = ctx.getShopStatusFilter();
      const shopSortMode = ctx.getShopSortMode();
      const status = shopStatusFilter === 'active' || shopStatusFilter === 'inactive' ? shopStatusFilter : '';
      const sort = ['position', 'id', 'name', 'status', 'price'].includes(shopSortMode) ? shopSortMode : 'position';
      const params = `q=${encodeURIComponent(q)}&status=${encodeURIComponent(status)}&sort=${encodeURIComponent(sort)}&limit=${ctx.SHOP_PAGE_SIZE}&offset=${ctx.getShopPageOffset()}`;
      ctx.setBusy('shop-search-btn', true, 'BUSCANDO');
      ctx.renderSkeleton('shop-list', 6);
      try {
        const r = await fetch(`/api/shop/offers?${params}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) {
          ctx.$('shop-list').innerHTML = `<div class="empty-state">${ctx.esc(d.error)}</div>`;
          ctx.showToast(d.error || 'Falha ao carregar ofertas', 'error');
          return;
        }
        const offers = Array.isArray(d.offers) ? d.offers : [];
        ctx.setShopLastPageOffers(offers);
        ctx.setShopTotalOffers(Number(d.total) || 0);
        ctx.setShopSelectedOfferId(offers[0] ? offers[0].id : null);
        const hiddenInvalid = Number(d.inventoryFilter && d.inventoryFilter.hiddenInvalid) || 0;
        const filterNote = hiddenInvalid > 0 ? ` | ${hiddenInvalid} fora do GameData ocultas` : '';
        ctx.$('shop-stats').textContent = `${ctx.getShopTotalOffers()} ofertas | pacote atual: ${ctx.getShopBuilderOffers().length} itens${filterNote}`;
        const itemNames = await ctx.getItemNames();
        const resolveDisplayName = (itemKey) => {
          const base = ctx.normalizeItemBaseKey(itemKey);
          return itemNames[itemKey] || itemNames[base] || itemKey;
        };
        await ctx.enrichWeaponVisuals(offers.map((o) => ({ key: o.name, displayName: resolveDisplayName(o.name) })), false, 30);
        ctx.renderShopOfferList();
      } catch (e) {
        ctx.$('shop-list').innerHTML = `<div class="empty-state">Erro: ${ctx.esc(e.message)}</div>`;
        ctx.showToast(`Falha ao carregar ofertas: ${e.message}`, 'error');
      } finally {
        ctx.setBusy('shop-search-btn', false);
      }
    }

    function shopPage(dir) {
      ctx.setShopPageOffset(Math.max(0, ctx.getShopPageOffset() + dir * ctx.SHOP_PAGE_SIZE));
      loadShopOffers(false);
    }

    async function loadShopCatalog(resetOffset) {
      if (resetOffset) ctx.setShopCatalogOffset(0);
      const q = ctx.$('shop-catalog-search').value.trim();
      const params = `q=${encodeURIComponent(q)}&limit=${ctx.SHOP_CATALOG_PAGE_SIZE}&offset=${ctx.getShopCatalogOffset()}`;
      ctx.setBusy('shop-catalog-search-btn', true, 'BUSCANDO');
      ctx.renderSkeleton('shop-catalog-list', 6);
      try {
        const r = await fetch(`/api/shop/catalog?${params}`, { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) {
          ctx.$('shop-catalog-list').innerHTML = `<div class="empty-state">${ctx.esc(d.error)}</div>`;
          return;
        }
        const items = Array.isArray(d.items) ? d.items : [];
        ctx.setShopCatalogTotal(Number(d.total) || 0);
        ctx.renderShopCatalog(items);
      } catch (e) {
        ctx.$('shop-catalog-list').innerHTML = `<div class="empty-state">Erro: ${ctx.esc(e.message)}</div>`;
      } finally {
        ctx.setBusy('shop-catalog-search-btn', false);
      }
    }

    function shopCatalogPage(dir) {
      ctx.setShopCatalogOffset(Math.max(0, ctx.getShopCatalogOffset() + dir * ctx.SHOP_CATALOG_PAGE_SIZE));
      loadShopCatalog(false);
    }

    return {
      loadShopOffers,
      shopPage,
      loadShopCatalog,
      shopCatalogPage
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.shop = window.AdminPanelDomains.shop || {};
  window.AdminPanelDomains.shop.createShopOffersDomain = createShopOffersDomain;
})();
