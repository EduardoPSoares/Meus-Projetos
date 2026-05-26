function registerCdnRoutes(context = {}) {
  const exact = new Set(['/api/weapons/names', '/api/weapons/enrich']);
  return [async (req, res, route) => {
    if (!exact.has(route.pathname)) return false;
    if (route.pathname === '/api/weapons/names') {
      const names = context.loadItemNames();
      context.json(res, { success: true, names });
      return true;
    }
    if (route.pathname === '/api/weapons/enrich') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const items = Array.isArray(body.items) ? body.items : [];
        const noNetwork = !!body.noNetwork;
        const maxNetworkParsed = context.parseStrictInt(body.maxNetwork);
        const maxNetwork = maxNetworkParsed === null ? 24 : Math.max(0, Math.min(500, maxNetworkParsed));
        const concurrencyParsed = context.parseStrictInt(body.concurrency);
        const concurrency = concurrencyParsed === null ? 8 : Math.max(1, Math.min(16, concurrencyParsed));
        if (!items.length) {
          context.json(res, { success: true, items: {} });
          return true;
        }
        if (items.length > 120) {
          context.json(res, { success: false, error: 'Maximo 120 itens por lote' }, 400);
          return true;
        }
        const prepared = [];
        const seen = new Set();
        for (const row of items) {
          const keyRaw = context.asTrimmedString(row && (row.key || row.name || row.item));
          if (!keyRaw) continue;
          const key = context.normalizeItemKeyToken(keyRaw.replace(context.ITEM_VARIANT_SUFFIX_RE, ''));
          if (!key || seen.has(key)) continue;
          seen.add(key);
          const displayName = context.asTrimmedString(row && (row.displayName || row.label || row.name || keyRaw));
          prepared.push({ key, displayName });
        }
        const out = {};
        let cursor = 0;
        const workerCount = Math.min(concurrency, prepared.length || 1);
        const workers = Array.from({ length: workerCount }, async () => {
          while (true) {
            const idx = cursor++;
            if (idx >= prepared.length) break;
            const row = prepared[idx];
            const allowNetwork = !noNetwork && idx < maxNetwork;
            const info = await context.resolveWeaponVisual(row.key, row.displayName, { noNetwork: !allowNetwork });
            out[row.key] = info;
          }
        });
        await Promise.all(workers);
        context.json(res, { success: true, items: out });
      } catch (e) {
        context.log('WIKI', `enrich error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }
    return false;
  }];
}

module.exports = { registerCdnRoutes };
