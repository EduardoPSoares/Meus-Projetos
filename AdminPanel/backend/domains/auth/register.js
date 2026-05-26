function registerAuthRoutes(context = {}) {
  const paths = new Set(['/api/model']);
  return [async (req, res, route) => {
    if (!paths.has(route.pathname)) return false;
    context.json(res, {
      success: true,
      model: {
        nick: context.PANEL_MODEL.nick,
        command: {
          allowed: context.PANEL_MODEL.command.allowed,
          limits: {
            addcry: { perCmd: context.LIMITS.game_money.perCmd, minTotal: context.LIMITS.game_money.min, maxTotal: context.LIMITS.game_money.max },
            addcrown: { perCmd: context.LIMITS.crown_money.perCmd, minTotal: context.LIMITS.crown_money.min, maxTotal: context.LIMITS.crown_money.max },
            addvp: { perCmd: context.LIMITS.cry_money.perCmd, minTotal: context.LIMITS.cry_money.min, maxTotal: context.LIMITS.cry_money.max },
            addxp: { perCmd: context.LIMITS.experience.perCmd, minTotal: context.LIMITS.experience.min, maxTotal: context.LIMITS.experience.max },
            addgm: { minRank: context.LIMITS.rank.min, maxRank: context.LIMITS.rank.max }
          }
        },
        item: {
          minLen: context.PANEL_MODEL.item.minLen,
          maxLen: context.PANEL_MODEL.item.maxLen,
          pattern: '^[a-z0-9_]+$',
          quantity: context.PANEL_MODEL.item.quantity,
          durability: context.PANEL_MODEL.item.durability,
          expirationHours: context.PANEL_MODEL.item.expirationHours,
          maxPendingRemoteGiveItems: context.PANEL_MODEL.item.maxPendingRemoteGiveItems
        },
        achievement: {
          minIdLen: context.PANEL_MODEL.achievement.minIdLen,
          maxIdLen: context.PANEL_MODEL.achievement.maxIdLen,
          idPattern: '^[a-z0-9_:/.-]+$',
          progress: context.PANEL_MODEL.achievement.progress
        },
        xp: context.PANEL_MODEL.xp
      }
    });
    return true;
  }];
}

module.exports = { registerAuthRoutes };
