(function bootstrapAdminPanel() {
  function loadScript(src) {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = src;
      script.async = false;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error(`Falha ao carregar script: ${src}`));
      document.head.appendChild(script);
    });
  }

  const domainScripts = [
    'modules/domains/state/index.js',
    'modules/domains/api/index.js',
    'modules/domains/ui/index.js',
    'modules/domains/ui/base.js',
    'modules/domains/services/index.js',
    'modules/domains/services/monitoring.js',
    'modules/domains/services/runtime-config.js',
    'modules/domains/services/admin-ops.js',
    'modules/domains/services/config.js',
    'modules/domains/services/anticheat.js',
    'modules/domains/services/performance.js',
    'modules/domains/players/index.js',
    'modules/domains/players/commands.js',
    'modules/domains/players/moderation.js',
    'modules/domains/players/achievements.js',
    'modules/domains/players/rooms.js',
    'modules/domains/players/item-ops.js',
    'modules/domains/players/inspect.js',
    'modules/domains/shop/index.js',
    'modules/domains/shop/offers.js',
    'modules/domains/shop/packages-rotation.js',
    'modules/domains/rewards/index.js',
    'modules/domains/rewards/survival.js',
    'modules/domains/rewards/survival-actions.js',
    'modules/domains/launcher/index.js',
    'modules/domains/launcher/cdn.js',
    'modules/domains/launcher/content.js',
    'modules/domains/launcher/sync.js',
    'modules/domains/launcher/publish.js'
  ];

  (async () => {
    try {
      await loadScript('modules/core/core.js');
      for (const src of domainScripts) await loadScript(src);
      await loadScript('modules/app/app.js');
    } catch (error) {
      console.error('[AdminPanel] bootstrap error:', error && error.message ? error.message : error);
      const root = document.getElementById('toast-stack') || document.body;
      const el = document.createElement('div');
      el.textContent = 'Falha ao iniciar o painel. Verifique os scripts de bootstrap.';
      el.style.cssText = 'position:fixed;bottom:16px;right:16px;background:#8f2c2c;color:#fff;padding:10px 12px;border-radius:8px;z-index:99999;font:600 12px/1.2 sans-serif;';
      root.appendChild(el);
    }
  })();
})();
