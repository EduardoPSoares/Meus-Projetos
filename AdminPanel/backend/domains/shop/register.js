function registerShopRoutes(context = {}) {
  const prefixes = ['/api/shop/'];
  const exact = new Set(['/api/arsenal/catalog']);
  return [async (req, res, route) => {
    if (!exact.has(route.pathname) && !prefixes.some(p => route.pathname.startsWith(p))) return false;
    if (route.pathname === '/api/shop/packages') {
      if (route.method !== 'GET') {
        context.json(res, { success: false, error: 'Use GET' }, 405);
        return true;
      }
      try {
        const store = context.loadShopPackagesStore();
        const packages = store.packages
          .map(context.summarizeShopPackage)
          .sort((a, b) => (Number(b.updatedAt) || 0) - (Number(a.updatedAt) || 0));
        context.json(res, { success: true, packages });
      } catch (e) {
        context.log('SHOPPKG', `list error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/packages/get') {
      if (route.method !== 'GET') {
        context.json(res, { success: false, error: 'Use GET' }, 405);
        return true;
      }
      try {
        const url = new URL(req.url, 'http://localhost');
        const id = context.asTrimmedString(url.searchParams.get('id'));
        if (!id) {
          context.json(res, { success: false, error: 'id obrigatorio' }, 400);
          return true;
        }
        const store = context.loadShopPackagesStore();
        const pkg = store.packages.find(p => context.asTrimmedString(p.id) === id);
        if (!pkg) {
          context.json(res, { success: false, error: 'Pacote nao encontrado' }, 404);
          return true;
        }
        const offers = context.sanitizePackageOffers(pkg.offers);
        context.json(res, {
          success: true,
          package: {
            id: context.asTrimmedString(pkg.id),
            name: context.asTrimmedString(pkg.name),
            description: context.asTrimmedString(pkg.description),
            createdAt: Number(pkg.createdAt) || Date.now(),
            updatedAt: Number(pkg.updatedAt) || Date.now(),
            offers
          }
        });
      } catch (e) {
        context.log('SHOPPKG', `get error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/packages/save') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const name = context.sanitizePackageName(body && body.name);
        if (!name) {
          context.json(res, { success: false, error: 'Nome do pacote invalido (3-64 caracteres)' }, 400);
          return true;
        }
        const description = context.sanitizePackageDescription(body && body.description);
        const expirationWarnings = [];
        const offers = context.sanitizePackageOffers(body && body.offers, { warnings: expirationWarnings });
        if (!offers.length) {
          context.json(res, { success: false, error: 'Pacote sem ofertas validas' }, 400);
          return true;
        }
        if (offers.length > 6000) {
          context.json(res, { success: false, error: 'Pacote muito grande (max 6000 offers)' }, 400);
          return true;
        }
        const now = Date.now();
        const inputId = context.asTrimmedString(body && body.id);
        const store = context.loadShopPackagesStore();
        const existingIdx = inputId ? store.packages.findIndex(p => context.asTrimmedString(p.id) === inputId) : -1;

        if (existingIdx >= 0) {
          const prev = store.packages[existingIdx];
          store.packages[existingIdx] = {
            id: context.asTrimmedString(prev.id),
            name,
            description,
            offers,
            createdAt: Number(prev.createdAt) || now,
            updatedAt: now
          };
        } else {
          const id = context.makeShopPackageId(name);
          store.packages.push({ id, name, description, offers, createdAt: now, updatedAt: now });
        }
        context.saveShopPackagesStore(store);
        const saved = existingIdx >= 0 ? store.packages[existingIdx] : store.packages[store.packages.length - 1];
        const currentRot = context.loadShopRotationConfig(store);
        context.saveShopRotationConfig(currentRot, store);
        const baseMessage = existingIdx >= 0 ? 'Pacote atualizado' : 'Pacote criado';
        const warningMessage = expirationWarnings.length
          ? ` | ${expirationWarnings.length} oferta(s) com tempo invalido foram convertidas para permanente`
          : '';
        context.json(res, {
          success: true,
          package: context.summarizeShopPackage(saved),
          message: baseMessage + warningMessage,
          expirationWarningsCount: expirationWarnings.length,
          expirationWarnings: expirationWarnings.slice(0, 20)
        });
      } catch (e) {
        context.log('SHOPPKG', `save error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/packages/delete') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const id = context.asTrimmedString(body && body.id);
        if (!id) {
          context.json(res, { success: false, error: 'id obrigatorio' }, 400);
          return true;
        }
        const store = context.loadShopPackagesStore();
        const before = store.packages.length;
        store.packages = store.packages.filter(p => context.asTrimmedString(p.id) !== id);
        if (store.packages.length === before) {
          context.json(res, { success: false, error: 'Pacote nao encontrado' }, 404);
          return true;
        }
        context.saveShopPackagesStore(store);
        const rot = context.loadShopRotationConfig(store);
        context.saveShopRotationConfig(rot, store);
        context.json(res, { success: true, message: 'Pacote removido' });
      } catch (e) {
        context.log('SHOPPKG', `delete error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/packages/apply') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const id = context.asTrimmedString(body && body.id);
        if (!id) {
          context.json(res, { success: false, error: 'id obrigatorio' }, 400);
          return true;
        }
        const mode = context.asTrimmedString(body && body.mode) === 'merge' ? 'merge' : 'replace';
        const regenerate = body && Object.prototype.hasOwnProperty.call(body, 'regenerate') ? !!body.regenerate : true;
        const result = await context.applyShopPackageById(id, mode, regenerate, 'manual');
        context.json(res, { success: true, message: 'Pacote aplicado', result });
      } catch (e) {
        context.log('SHOPPKG', `apply error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/rotation') {
      if (route.method !== 'GET') {
        context.json(res, { success: false, error: 'Use GET' }, 405);
        return true;
      }
      try {
        const packagesStore = context.loadShopPackagesStore();
        const cfg = context.loadShopRotationConfig(packagesStore);
        const packages = packagesStore.packages
          .map(context.summarizeShopPackage)
          .sort((a, b) => (Number(b.updatedAt) || 0) - (Number(a.updatedAt) || 0));
        context.json(res, { success: true, rotation: cfg, packages });
      } catch (e) {
        context.log('SHOPROT', `get error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/rotation/set') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const packagesStore = context.loadShopPackagesStore();
        let cfg = context.loadShopRotationConfig(packagesStore);

        if (Object.prototype.hasOwnProperty.call(body || {}, 'enabled')) cfg.enabled = !!body.enabled;
        if (Object.prototype.hasOwnProperty.call(body || {}, 'intervalMinutes')) {
          const intervalParsed = context.parseStrictInt(body.intervalMinutes);
          if (intervalParsed === null) {
            context.json(res, { success: false, error: 'intervalMinutes invalido' }, 400);
            return true;
          }
          cfg.intervalMinutes = context.clampInt(intervalParsed, 5, 10080);
        }
        if (Array.isArray(body && body.packageIds)) cfg.packageIds = body.packageIds;
        if (Object.prototype.hasOwnProperty.call(body || {}, 'currentIndex')) {
          const currentIndexParsed = context.parseStrictInt(body.currentIndex);
          if (currentIndexParsed !== null) cfg.currentIndex = currentIndexParsed;
        }

        cfg = context.normalizeShopRotationConfig(cfg, packagesStore);
        if (cfg.packageIds.length) {
          cfg.currentIndex = ((cfg.currentIndex % cfg.packageIds.length) + cfg.packageIds.length) % cfg.packageIds.length;
        } else {
          cfg.currentIndex = 0;
        }

        if (!cfg.enabled) {
          cfg.nextRunAt = 0;
        } else if (!cfg.packageIds.length) {
          context.json(res, { success: false, error: 'Selecione ao menos 1 pacote para rotacao' }, 400);
          return true;
        } else {
          const runNow = !!(body && body.runNow);
          const now = Date.now();
          cfg.nextRunAt = runNow ? now : now + cfg.intervalMinutes * 60 * 1000;
        }

        const saved = context.saveShopRotationConfig(cfg, packagesStore);
        context.json(res, { success: true, rotation: saved, message: 'Rotacao atualizada' });
      } catch (e) {
        context.log('SHOPROT', `set error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/rotation/run') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const applied = await context.processShopRotationTick(true);
        const packagesStore = context.loadShopPackagesStore();
        const cfg = context.loadShopRotationConfig(packagesStore);
        if (!applied) {
          context.json(res, {
            success: false,
            error: cfg.enabled ? 'Nenhuma troca executada agora' : 'Rotacao desativada'
          }, 400);
          return true;
        }
        context.json(res, { success: true, message: 'Rotacao executada', rotation: cfg });
      } catch (e) {
        context.log('SHOPROT', `run error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/regenerate') {
      context.json(res, {
        success: false,
        error: 'Rebuild automatico desativado: Cache Shop sobrescreve a loja ativa com os XML. Use Servicos > Cache Shop apenas quando quiser reconstruir a loja a partir dos XML.'
      }, 400);
      return true;
    }

    if (route.pathname === '/api/shop/offers') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const search = url.searchParams.get('q') || '';
        const status = context.asTrimmedString(url.searchParams.get('status')).toLowerCase();
        const sortMode = context.asTrimmedString(url.searchParams.get('sort')).toLowerCase() || 'position';
        const sortDir = context.asTrimmedString(url.searchParams.get('dir')).toLowerCase() || 'asc';
        const limit = Math.min(parseInt(url.searchParams.get('limit')) || 500, 5000);
        const offset = parseInt(url.searchParams.get('offset')) || 0;
        await context.withMongo(async (db) => {
          const cache = await db.collection('cache').findOne({ _id: 'shop' });
          if (!cache || !Array.isArray(cache.data)) return context.json(res, { success: false, error: 'Shop cache not found' });
          let offers = cache.data.slice();
          const gameFilter = context.filterShopOffersToGameItems(offers);
          if (gameFilter.inventoryAvailable && gameFilter.removed.length) {
            await context.backupCurrentShopCache(db, 'active-shop-game-inventory-sync');
            await db.collection('cache').updateOne(
              { _id: 'shop' },
              {
                $set: {
                  data: gameFilter.offers,
                  hash: Math.floor(Date.now() / 1000),
                  updatedAt: Date.now(),
                  last_game_inventory_sync_at: Date.now()
                }
              }
            );
          }
          offers = gameFilter.offers;
          if (status === 'active') offers = offers.filter(o => context.isShopOfferActiveStatus(o && o.offer_status));
          else if (status === 'inactive') offers = offers.filter(o => !context.isShopOfferActiveStatus(o && o.offer_status));
          if (search) {
            const s = search.toLowerCase();
            offers = offers.filter(o => String(o.name).toLowerCase().includes(s) || String(o.id).includes(s));
          }
          offers = context.sortShopOffersForPanel(offers, sortMode, sortDir);
          const total = offers.length;
          const page = offers.slice(offset, offset + limit);
          context.json(res, {
            success: true,
            offers: page,
            total,
            offset,
            limit,
            hash: cache.hash,
            status: status || 'all',
            sort: sortMode,
            dir: sortDir,
            inventoryFilter: {
              available: gameFilter.inventoryAvailable,
              hiddenInvalid: gameFilter.removed.length
            }
          });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/arsenal/catalog') {
      if (route.method !== 'GET') return context.json(res, { success: false, error: 'Use GET' }, 405), true;
      try {
        const url = new URL(req.url, 'http://localhost');
        const q = context.asTrimmedString(url.searchParams.get('q')).toLowerCase();
        const typeRaw = context.asTrimmedString(url.searchParams.get('type')).toLowerCase();
        const shopRaw = context.asTrimmedString(url.searchParams.get('shop')).toLowerCase();
        const type = (typeRaw === 'weapon' || typeRaw === 'equipment' || typeRaw === 'other') ? typeRaw : 'all';
        const shop = (shopRaw === 'in' || shopRaw === 'out') ? shopRaw : 'both';
        const limitParsed = context.parseStrictInt(url.searchParams.get('limit'));
        const offsetParsed = context.parseStrictInt(url.searchParams.get('offset'));
        const limit = limitParsed === null ? 2500 : context.clampInt(limitParsed, 50, 10000);
        const offset = offsetParsed === null ? 0 : Math.max(0, offsetParsed);
        let shopInfo = { set: new Set(), hash: 0, available: true };
        try {
          shopInfo = await context.withMongo(async db => {
            const info = await context.getCurrentShopOfferNameSet(db);
            info.available = true;
            return info;
          });
        } catch (e) {
          shopInfo = { set: new Set(), hash: 0, available: false, error: e.message };
        }
        let items = context.buildArsenalCatalog(shopInfo.set);
        if (type !== 'all') items = items.filter(item => item.type === type);
        if (shop === 'in') items = items.filter(item => item.inShop);
        else if (shop === 'out') items = items.filter(item => !item.inShop);
        if (q) {
          items = items.filter(item =>
            item.key.includes(q) ||
            String(item.displayName || '').toLowerCase().includes(q) ||
            String(item.wikiName || '').toLowerCase().includes(q)
          );
        }
        const total = items.length;
        const page = items.slice(offset, offset + limit);
        context.json(res, { success: true, items: page, total, offset, limit, shopHash: shopInfo.hash, shopAvailable: !!shopInfo.available });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/offer/update') {
      if (route.method !== 'POST') return context.json(res, { success: false, error: 'Use POST' }, 405), true;
      try {
        const body = await context.parseBody(req);
        const { id, game_price, cry_price, crown_price, offer_status, durabilityPoints, quantity, sorting_index, expirationTime } = body;
        if (id === undefined) return context.json(res, { success: false, error: 'ID do offer obrigatorio' });
        await context.withMongo(async (db) => {
          const cache = await db.collection('cache').findOne({ _id: 'shop' });
          if (!cache || !cache.data) return context.json(res, { success: false, error: 'Shop cache not found' });
          const idx = cache.data.findIndex(o => o.id === parseInt(id) || o.id === id);
          if (idx === -1) return context.json(res, { success: false, error: 'Offer nao encontrado' });
          const offer = cache.data[idx] && typeof cache.data[idx] === 'object' ? cache.data[idx] : {};
          const priceMax = 2147483647;
          const qtyMax = 999999;
          const durabilityMax = 1000000;
          const applyOptionalInt = (rawValue, fieldName, min, max) => {
            if (rawValue === undefined) return null;
            const parsed = context.parseStrictInt(rawValue);
            if (parsed === null) throw new Error(`${fieldName} invalido`);
            if (parsed < min || parsed > max) throw new Error(`${fieldName} deve estar entre ${min} e ${max}`);
            return parsed;
          };
          const nextGamePrice = applyOptionalInt(game_price, 'Preco ouro', 0, priceMax);
          const nextCryPrice = applyOptionalInt(cry_price, 'Preco VP', 0, priceMax);
          const nextCrownPrice = applyOptionalInt(crown_price, 'Preco coroas', 0, priceMax);
          const nextDurability = applyOptionalInt(durabilityPoints, 'Durabilidade', 0, durabilityMax);
          const nextQuantity = applyOptionalInt(quantity, 'Quantidade', 0, qtyMax);
          const nextSortingIndex = applyOptionalInt(sorting_index, 'Ordem', 0, priceMax);
          if (nextGamePrice !== null) offer.game_price = nextGamePrice;
          if (nextCryPrice !== null) offer.cry_price = nextCryPrice;
          if (nextCrownPrice !== null) offer.crown_price = nextCrownPrice;
          if (nextDurability !== null) offer.durabilityPoints = nextDurability;
          if (nextQuantity !== null) offer.quantity = nextQuantity;
          if (nextSortingIndex !== null) offer.sorting_index = nextSortingIndex;
          if (expirationTime !== undefined) {
            const normalizedExpiration = context.normalizeShopExpirationTime(expirationTime);
            if (normalizedExpiration === null) throw new Error('Tempo invalido. Use: 30d, 7d, 3h');
            offer.expirationTime = normalizedExpiration;
          }
          if (offer_status !== undefined) {
            const statusRaw = context.asTrimmedString(offer_status);
            const allowedStatus = new Set(['NORMAL', 'NEW', 'HOT', 'enabled', 'limited']);
            if (!statusRaw || !allowedStatus.has(statusRaw)) throw new Error('Status de oferta invalido');
            offer.offer_status = statusRaw;
          }

          const offerType = context.inferArsenalType ? context.inferArsenalType(offer.name) : 'other';
          const hasCrownPrice = Number(offer.crown_price || 0) > 0;
          if (offerType === 'equipment' || !hasCrownPrice) {
            offer.expirationTime = '';
          }

          await context.backupCurrentShopCache(db, `offer-update-${id}`);
          cache.data[idx] = offer;
          await db.collection('cache').updateOne({ _id: 'shop' }, { $set: { data: cache.data, hash: Math.floor(Date.now() / 1000) } });
          context.json(res, { success: true, message: `Offer ${id} atualizado` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/shop/clear') {
      if (route.method !== 'POST') return context.json(res, { success: false, error: 'Use POST' }, 405), true;
      try {
        await context.withMongo(async (db) => {
          const cache = await db.collection('cache').findOne({ _id: 'shop' });
          if (!cache || !Array.isArray(cache.data)) throw new Error('Shop cache not found');
          await context.backupCurrentShopCache(db, 'clear-active-shop');
          const removed = cache.data.length;
          await db.collection('cache').updateOne(
            { _id: 'shop' },
            {
              $set: {
                data: [],
                hash: Math.floor(Date.now() / 1000),
                updatedAt: Date.now(),
                last_shop_clear_at: Date.now()
              },
              $unset: {
                last_shop_package_id: '',
                last_shop_package_name: ''
              }
            }
          );
          context.json(res, { success: true, message: `Loja limpa (${removed} ofertas removidas)`, removed });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    return false;
  }];
}

module.exports = { registerShopRoutes };
