function registerRewardsRoutes(context = {}) {
  const prefixes = ['/api/survival-rewards'];
  return [async (req, res, route) => {
    if (!prefixes.some(p => route.pathname.startsWith(p))) return false;
    if (route.pathname === '/api/survival-rewards') {
      if (route.method !== 'GET') {
        context.json(res, { success: false, error: 'Use GET' }, 405);
        return true;
      }
      try {
        const config = context.getSurvivalRewardConfig();
        context.json(res, { success: true, config });
      } catch (e) {
        context.log('REWARDS', `load error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/survival-rewards/save') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const result = context.applySurvivalRewardChanges(body);
        context.json(res, {
          success: true,
          message: result.dryRun
            ? `Validacao OK: ${result.missionTypeCount} tipos e ${result.missionFileCount} XMLs de missao`
            : `Recompensas atualizadas: ${result.missionTypeCount} tipos, ${result.changedFiles.length} arquivos alterados`,
          result
        });
      } catch (e) {
        context.log('REWARDS', `save error: ${e.message}`);
        context.json(res, { success: false, error: e.message }, 400);
      }
      return true;
    }

    return false;
  }];
}

module.exports = { registerRewardsRoutes };
