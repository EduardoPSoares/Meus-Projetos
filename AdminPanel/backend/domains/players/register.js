function registerPlayersRoutes(context = {}) {
  const prefixes = ['/api/profile/', '/api/achievements/', '/api/clan/'];
  const exact = new Set([
    '/api/players', '/api/command', '/api/generateToken', '/api/broadcast', '/api/notification',
    '/api/ban', '/api/unban', '/api/bans', '/api/giveitem', '/api/search', '/api/playerhistory',
    '/api/banhistory', '/api/chatlogs', '/api/notes/get', '/api/notes/set', '/api/bannedips',
    '/api/banip', '/api/unbanip', '/api/kickbyip', '/api/clans', '/api/removeitem', '/api/gamerooms'
  ]);
  return [async (req, res, route) => {
    if (!exact.has(route.pathname) && !prefixes.some(p => route.pathname.startsWith(p))) return false;
    if (route.pathname === '/api/players') {
      try {
        const raw = await context.callApi('getplayers', `${context.XMPP_API}/getplayers`);
        if (!raw) {
          context.json(res, { success: false, error: 'API offline' }, 503);
          return true;
        }
        try {
          const data = JSON.parse(raw);
          const basePlayers = Array.isArray(data.players) ? data.players : [];
          const count = basePlayers.length;

          const usernames = [...new Set(basePlayers
            .map(p => String((p && p.nickname) || '').trim())
            .filter(Boolean))];

          let nickByUsername = new Map();
          if (usernames.length) {
            try {
              await context.withMongo(async (db) => {
                const docs = await db.collection('profiles')
                  .find({ username: { $in: usernames } }, { projection: { username: 1, nick: 1 } })
                  .toArray();
                nickByUsername = new Map(
                  docs.map(d => [String(d.username || '').trim(), String(d.nick || '').trim()])
                );
              });
            } catch {}
          }

          const players = basePlayers.map((p) => {
            const username = String((p && p.nickname) || '').trim();
            const nick = nickByUsername.get(username) || username;
            return {
              ...p,
              username,
              nickname: nick
            };
          });

          context.json(res, { success: true, players, online: data.code === 0 ? count : 0 });
        } catch {
          context.json(res, { success: false, error: 'Erro ao parsear' });
        }
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/playerhistory') {
      const url = new URL(req.url, 'http://localhost');
      const period = parseInt(url.searchParams.get('period')) || 3600;
      const cutoff = Date.now() - period * 1000;
      const filtered = (context.statsHistory || []).filter(e => e.time > cutoff && e.online !== undefined).map(e => ({ time: e.time, online: e.online }));
      context.json(res, { success: true, history: filtered, period });
      return true;
    }

    if (route.pathname === '/api/profile/full') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const nick = url.searchParams.get('nick');
        if (!nick) return context.json(res, { success: false, error: 'nick obrigatorio' });
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          const items = (profile.items || []).map(i => ({
            id: i.id, name: i.name, slot: i.slot, equipped: i.equipped,
            permanent: i.permanent, attached_to: i.attached_to, config: i.config,
            buy_time_utc: i.buy_time_utc, expiration_time_utc: i.expiration_time_utc || null,
            seconds_left: i.seconds_left || null, durability_points: i.durability_points || null,
            total_durability_points: i.total_durability_points || null, quantity: i.quantity || null,
            repair_cost: i.repair_cost || null
          }));
          const data = {
            basic: {
              id: profile._id, username: profile.username, nick: profile.nick,
              gender: profile.gender, rank: profile.rank || 1,
              experience: profile.experience || 0,
              game_money: profile.game_money || 0, crown_money: profile.crown_money || 0,
              cry_money: profile.cry_money || 0, current_class: profile.current_class || 0,
              clan_name: profile.clan_name || '', clan_points: profile.clan_points || 0,
              head: profile.head || '', status: profile.status, location: profile.location || '',
              last_seen: profile.last_seen_date || 0
            },
            items,
            expired_items: (profile.expired_items || []).map(i => ({ id: i.id, name: i.name })),
            stats: (profile.stats || []).filter(s => s.stat && s.Value !== undefined).map(s => ({
              stat: s.stat, value: s.Value, mode: s.mode || '', class: s.class || ''
            })),
            achievements: (profile.achievements || []).map(a => ({
              id: a.achievement_id, progress: a.progress, completion_time: a.completion_time || 0
            })),
            classes_unlocked: profile.classes_unlocked || [],
            missions_unlocked: profile.missions_unlocked || [],
            tutorials_passed: profile.tutorials_passed || [],
            contracts: profile.contracts || {},
            friends: profile.friends || [],
            persistent_settings: profile.persistent_settings || {},
            mute: profile.mute || { time: 0, reason: '' },
            banner: { badge: profile.banner_badge, mark: profile.banner_mark, stripe: profile.banner_stripe }
          };
          context.json(res, { success: true, profile: data });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/profile/lookup') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const nick = url.searchParams.get('nick');
        if (!nick) return context.json(res, { success: false, error: 'nick obrigatorio' });
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick }, { projection: { game_money: 1, cry_money: 1, crown_money: 1, experience: 1, rank: 1, _id: 0 } });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          context.json(res, { success: true, profile });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/bans') {
      try {
        await context.withMongo(async (db) => {
          const accounts = await db.collection('accounts').find({ ban: { $exists: true } }).toArray();
          const profileIds = accounts.filter(a => a.ban).map(a => a._id);
          const profiles = profileIds.length
            ? await db.collection('profiles').find({ username: { $in: profileIds.map(String) } }, { projection: { nick: 1, username: 1 } }).toArray()
            : [];
          const nickMap = {};
          profiles.forEach(p => { nickMap[p.username] = p.nick; });
          const bans = accounts.filter(a => a.ban).map(a => ({
            username: a._id,
            nick: nickMap[String(a._id)] || '?',
            expires: a.ban.expires,
            cause: a.ban.cause,
            reason: a.ban.reason || '',
            expired: a.ban.expires * 1000 < Date.now(),
          }));
          context.json(res, { success: true, bans });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/generateToken') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const id = url.searchParams.get('id') || '';
        const tokenVal = url.searchParams.get('token') || '';
        const time = url.searchParams.get('time') || '120000';
        let banError = '';
        try {
          const banData = await context.withMongo(async (db) => {
            const account = await db.collection('accounts').findOne({ _id: Number(id) }, { projection: { ban: 1 } });
            if (account && account.ban && account.ban.expires) {
              const now = Math.floor(Date.now() / 1000);
              if (account.ban.expires > now) {
                const msg = account.ban.reason || 'Banido do servidor';
                const b64 = Buffer.from(msg, 'utf8').toString('base64').replace(/=/g, '*').replace(/\//g, '|');
                return `&error_code=${account.ban.cause || 14}&error_message=${encodeURIComponent(b64)}&error_unbantime=${account.ban.expires}`;
              }
            }
            return '';
          });
          banError = banData || '';
        } catch {}

        const targetUrl = `${context.XMPP_API}/settoken?id=${encodeURIComponent(id)}&token=${encodeURIComponent(tokenVal)}&time=${time}${banError}`;
        const raw = await context.callApi('settoken', targetUrl);
        let ok = false;
        if (raw) try { ok = JSON.parse(raw).code === 0; } catch {}
        context.json(res, { success: ok, message: ok ? (banError ? 'Token negado â€” conta banida' : 'Token gerado') : 'Falha ao gerar token' });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/broadcast') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { message } = body;
        if (!message) return context.json(res, { success: false, error: 'Mensagem obrigatoria' });
        const encoded = encodeURIComponent(message);
        const raw = await context.callApi('broadcast', `${context.XMPP_API}/broadcast?message=${encoded}`);
        let sent = 0;
        if (raw) try { sent = JSON.parse(raw).sent || 0; } catch {}
        if (sent <= 0) return context.json(res, { success: false, error: 'Nenhum GameClient online para receber a mensagem' });
        context.json(res, { success: true, message: `Mensagem enviada para ${sent} jogador(es)` });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/notification') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { message, type } = body;
        if (!message) return context.json(res, { success: false, error: 'Mensagem obrigatoria' });
        const encoded = encodeURIComponent(message);
        const notifType = parseInt(type) || 8;
        const raw = await context.callApi('notifyall', `${context.XMPP_API}/notifyall?message=${encoded}&type=${notifType}`);
        let sent = 0;
        if (raw) try { sent = JSON.parse(raw).sent || 0; } catch {}
        context.json(res, { success: true, message: `Notificacao enviada para ${sent} jogador(es)` });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/ban') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick, reason, duration } = body;
        if (!nick) return context.json(res, { success: false, error: 'Nick obrigatorio' });
        const dur = parseInt(duration) || 315360000;
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          const expires = Math.floor(Date.now() / 1000) + dur;
          const banReason = reason || 'Banido pelo painel';
          await db.collection('accounts').updateOne(
            { _id: Number(profile.username) },
            { $set: { ban: { expires, cause: 14, reason: banReason } } },
            { upsert: true }
          );
          await db.collection('ban_history').insertOne({
            type: 'ban', nick, username: Number(profile.username),
            reason: banReason, duration: dur, expires,
            admin: 'Painel', timestamp: Date.now()
          });
          await context.callApi('kick', `${context.XMPP_API}/kick?username=${encodeURIComponent(profile.username)}`);
          context.writeBanCache();
          context.json(res, { success: true, message: `${nick} banido` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/unban') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick } = body;
        if (!nick) return context.json(res, { success: false, error: 'Nick obrigatorio' });
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          await db.collection('accounts').updateOne({ _id: Number(profile.username) }, { $unset: { ban: '' } });
          await db.collection('ban_history').insertOne({
            type: 'unban', nick, username: Number(profile.username),
            reason: 'Desbanido pelo painel', duration: null, expires: null,
            admin: 'Painel', timestamp: Date.now()
          });
          context.writeBanCache();
          context.json(res, { success: true, message: `${nick} desbanido` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/giveitem') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick, item_name, quantity, durability, expiration_hours } = body;
        const nickValidation = context.validateNickInput(nick);
        if (!nickValidation.ok) return context.json(res, { success: false, error: nickValidation.error });
        const itemValidation = context.validateItemNameInput(item_name);
        if (!itemValidation.ok) return context.json(res, { success: false, error: itemValidation.error });
        const forcedShopItemName = `${itemValidation.baseKey}_shop`;

        const qtyValidation = context.validateBoundedInt(
          quantity === undefined || quantity === null || String(quantity).trim() === '' ? 1 : quantity,
          context.PANEL_MODEL.item.quantity,
          'Quantidade'
        );
        if (!qtyValidation.ok) return context.json(res, { success: false, error: qtyValidation.error });

        const durabilityValidation = context.validateBoundedInt(
          durability === undefined || durability === null || String(durability).trim() === '' ? 0 : durability,
          context.PANEL_MODEL.item.durability,
          'Durabilidade'
        );
        if (!durabilityValidation.ok) return context.json(res, { success: false, error: durabilityValidation.error });

        const expirationValidation = context.validateBoundedInt(
          expiration_hours === undefined || expiration_hours === null || String(expiration_hours).trim() === '' ? 0 : expiration_hours,
          context.PANEL_MODEL.item.expirationHours,
          'Duracao (horas)'
        );
        if (!expirationValidation.ok) return context.json(res, { success: false, error: expirationValidation.error });

        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick: nickValidation.value });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });

          const qty = qtyValidation.value;
          const dur = durabilityValidation.value;
          const expHours = expirationValidation.value;

          const item = { name: forcedShopItemName };
          if (dur > 0) item.durabilityPoints = dur;
          if (expHours > 0) item.expirationTime = expHours * 3600;
          if (qty > 1) item.quantity = qty;

          const currentRaw = profile.remote_give && typeof profile.remote_give === 'object' ? profile.remote_give : {};
          const currentItems = Array.isArray(currentRaw.items) ? currentRaw.items.slice() : [];
          if (currentItems.length >= context.PANEL_MODEL.item.maxPendingRemoteGiveItems) {
            return context.json(res, {
              success: false,
              error: `Fila de entrega cheia (${context.PANEL_MODEL.item.maxPendingRemoteGiveItems}). Aguarde as entregas pendentes.`
            });
          }
          currentItems.push(item);
          const current = {
            ...currentRaw,
            items: currentItems,
            achievements: Array.isArray(currentRaw.achievements) ? currentRaw.achievements : [],
          };

          await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { remote_give: current } });
          context.json(res, { success: true, message: `${itemValidation.displayName} (${forcedShopItemName}) sera entregue a ${nickValidation.value} em alguns segundos` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/search') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const q = url.searchParams.get('q');
        const type = url.searchParams.get('type') || 'nick';
        if (!q || q.length < 2) return context.json(res, { success: false, error: 'Minimo 2 caracteres' });
        await context.withMongo(async (db) => {
          let results = [];
          if (type === 'nick') {
            const profiles = await db.collection('profiles').find(
              { nick: { $regex: q, $options: 'i' } },
              { projection: { nick: 1, username: 1, rank: 1, game_money: 1, cry_money: 1, experience: 1, status: 1, clan_name: 1, current_class: 1 } }
            ).limit(30).toArray();
            results = profiles.map(p => ({
              nick: p.nick, username: p.username, id: p._id,
              rank: p.rank || 1, game_money: p.game_money || 0,
              cry_money: p.cry_money || 0, experience: p.experience || 0,
              status: p.status, clan_name: p.clan_name || '', current_class: p.current_class || 0
            }));
          } else if (type === 'id') {
            const numId = parseInt(q);
            if (isNaN(numId)) return context.json(res, { success: false, error: 'ID invalido' });
            const account = await db.collection('accounts').findOne({ _id: numId });
            if (account) {
              const profile = await db.collection('profiles').findOne(
                { username: String(numId) },
                { projection: { nick: 1, username: 1, rank: 1, game_money: 1, cry_money: 1, experience: 1, status: 1, clan_name: 1, current_class: 1 } }
              );
              results = profile ? [{
                nick: profile.nick, username: profile.username, id: profile._id,
                rank: profile.rank || 1, game_money: profile.game_money || 0,
                cry_money: profile.cry_money || 0, experience: profile.experience || 0,
                status: profile.status, clan_name: profile.clan_name || '', current_class: profile.current_class || 0
              }] : [];
            }
          }
          context.json(res, { success: true, results });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/command') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { cmd, nick, amount } = body;
        const cmdValidation = context.validateCommandName(cmd);
        if (!cmdValidation.ok) return context.json(res, { success: false, error: cmdValidation.error });
        const nickValidation = context.validateNickInput(nick);
        if (!nickValidation.ok) return context.json(res, { success: false, error: nickValidation.error });

        const cmdName = cmdValidation.value;
        const nickName = nickValidation.value;
        const result = await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick: nickName });
          if (!profile) return { error: 'Jogador nao encontrado' };
          switch (cmdName) {
            case 'addcry': {
              const parsed = context.validatePositiveDelta(amount, context.LIMITS.game_money, 'Valor de ouro');
              if (!parsed.ok) return { error: parsed.error };
              const current = context.getProfileInt(profile, 'game_money', 0);
              if (!current.ok) return { error: current.error };
              const gm = current.value + parsed.value;
              if (gm < context.LIMITS.game_money.min || gm > context.LIMITS.game_money.max) return { error: `Total excede limite (${context.LIMITS.game_money.min.toLocaleString('pt-BR')} ~ ${context.LIMITS.game_money.max.toLocaleString('pt-BR')})` };
              await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { game_money: gm } });
              await context.callApi('setprofile(game_money)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickName)}&field=game_money&value=${gm}`);
              return { success: true, message: `${parsed.value} ouro adicionado para ${nickName} (total: ${gm.toLocaleString('pt-BR')})` };
            }
            case 'addcrown': {
              const parsed = context.validatePositiveDelta(amount, context.LIMITS.crown_money, 'Valor de coroas');
              if (!parsed.ok) return { error: parsed.error };
              const current = context.getProfileInt(profile, 'crown_money', 0);
              if (!current.ok) return { error: current.error };
              const cm = current.value + parsed.value;
              if (cm < context.LIMITS.crown_money.min || cm > context.LIMITS.crown_money.max) return { error: `Total excede limite (${context.LIMITS.crown_money.min.toLocaleString('pt-BR')} ~ ${context.LIMITS.crown_money.max.toLocaleString('pt-BR')})` };
              await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { crown_money: cm } });
              await context.callApi('setprofile(crown_money)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickName)}&field=crown_money&value=${cm}`);
              return { success: true, message: `${parsed.value} coroas adicionado para ${nickName} (total: ${cm.toLocaleString('pt-BR')})` };
            }
            case 'addvp': {
              const parsed = context.validatePositiveDelta(amount, context.LIMITS.cry_money, 'Valor de VP');
              if (!parsed.ok) return { error: parsed.error };
              const current = context.getProfileInt(profile, 'cry_money', 0);
              if (!current.ok) return { error: current.error };
              const vp = current.value + parsed.value;
              if (vp < context.LIMITS.cry_money.min || vp > context.LIMITS.cry_money.max) return { error: `Total excede limite (${context.LIMITS.cry_money.min.toLocaleString('pt-BR')} ~ ${context.LIMITS.cry_money.max.toLocaleString('pt-BR')})` };
              await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { cry_money: vp } });
              await context.callApi('setprofile(cry_money)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickName)}&field=cry_money&value=${vp}`);
              return { success: true, message: `${parsed.value} VP adicionado para ${nickName} (total: ${vp.toLocaleString('pt-BR')})` };
            }
            case 'addxp': {
              const parsed = context.validatePositiveDelta(amount, context.LIMITS.experience, 'Valor de XP');
              if (!parsed.ok) return { error: parsed.error };
              const current = context.getProfileInt(profile, 'experience', 0);
              if (!current.ok) return { error: current.error };
              const xp = current.value + parsed.value;
              const maxXp = context.getMaxAllowedExp();
              if (xp > maxXp) return { error: `XP maximo para rank ${context.LIMITS.rank.max}: ${maxXp.toLocaleString('pt-BR')}` };
              await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { experience: xp } });
              await context.callApi('setprofile(experience)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickName)}&field=experience&value=${xp}`);
              return { success: true, message: `${parsed.value} XP adicionado para ${nickName} (total: ${xp.toLocaleString('pt-BR')})` };
            }
            case 'addgm': {
              const rankInfo = context.getProfileInt(profile, 'rank', context.LIMITS.rank.min);
              if (!rankInfo.ok) return { error: rankInfo.error };
              let newRank;
              if (amount !== undefined && amount !== null && String(amount).trim() !== '') {
                const parsedRank = context.validateBoundedInt(amount, { min: context.LIMITS.rank.min, max: context.LIMITS.rank.max }, 'Rank');
                if (!parsedRank.ok) return { error: parsedRank.error };
                newRank = parsedRank.value;
              } else {
                if (rankInfo.value >= context.LIMITS.rank.max) return { error: `Rank maximo ja atingido (${context.LIMITS.rank.max})` };
                newRank = rankInfo.value + 1;
              }
              const newXp = context.getExpForRank(newRank);
              await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { rank: newRank, experience: newXp } });
              await context.callApi('setprofile(rank)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickName)}&field=rank&value=${newRank}`);
              await context.callApi('setprofile(experience)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickName)}&field=experience&value=${newXp}`);
              return { success: true, message: `${nickName} rank definido para ${newRank}` };
            }
            case 'kick': {
              const raw = await context.callApi('kick', `${context.XMPP_API}/kick?username=${encodeURIComponent(profile.username)}`);
              let kicked = false;
              if (raw) try { kicked = JSON.parse(raw).code === 0; } catch {}
              return kicked ? { success: true, message: `${nickName} foi kickado` } : { success: false, message: `${nickName} nao encontrado online` };
            }
            case 'addcm': {
              await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { clan_name: nickName, clan_rank: 1 } });
              return { success: true, message: `Clan criado para ${nickName}` };
            }
            default:
              return { error: `Comando desconhecido: ${cmdName}` };
          }
        });
        if (result.error) return context.json(res, { success: false, error: result.error });
        context.json(res, { success: true, message: result.message });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/banhistory') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const limit = Math.min(parseInt(url.searchParams.get('limit')) || 100, 1000);
        const offset = parseInt(url.searchParams.get('offset')) || 0;
        const searchNick = url.searchParams.get('nick') || '';
        const type = url.searchParams.get('type') || 'all';
        await context.withMongo(async (db) => {
          const filter = {};
          if (searchNick) filter.nick = { $regex: searchNick, $options: 'i' };
          if (type !== 'all') filter.type = type;
          const total = await db.collection('ban_history').countDocuments(filter);
          const history = await db.collection('ban_history').find(filter).sort({ timestamp: -1 }).skip(offset).limit(limit).toArray();
          context.json(res, { success: true, history, total, offset, limit });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/chatlogs') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const type = url.searchParams.get('type') || 'all';
        const nick = url.searchParams.get('nick') || '';
        const limit = Math.min(parseInt(url.searchParams.get('limit')) || 200, 1000);
        const offset = parseInt(url.searchParams.get('offset')) || 0;
        await context.withMongo(async (db) => {
          const filter = {};
          if (type !== 'all') filter.type = type;
          if (nick) {
            filter.$or = [
              { nick: { $regex: nick, $options: 'i' } },
              { from_username: { $regex: nick, $options: 'i' } },
              { to_nick: { $regex: nick, $options: 'i' } }
            ];
          }
          const total = await db.collection('chat_logs').countDocuments(filter);
          const logs = await db.collection('chat_logs').find(filter).sort({ time: -1 }).skip(offset).limit(limit).toArray();
          context.json(res, { success: true, logs, total, offset, limit });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/notes/get') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const nick = url.searchParams.get('nick');
        if (!nick) {
          context.json(res, { success: false, error: 'nick obrigatorio' });
          return true;
        }
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick }, { projection: { internal_notes: 1 } });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          context.json(res, { success: true, notes: profile.internal_notes || '' });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/notes/set') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick, notes } = body;
        if (!nick) {
          context.json(res, { success: false, error: 'nick obrigatorio' });
          return true;
        }
        await context.withMongo(async (db) => {
          const result = await db.collection('profiles').updateOne({ nick }, { $set: { internal_notes: notes || '' } });
          if (result.matchedCount === 0) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          context.json(res, { success: true, message: 'Notas salvas' });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/bannedips') {
      try {
        await context.withMongo(async (db) => {
          const docs = await db.collection('ip_bans').find({}).sort({ createdAt: -1 }).toArray();
          const now = Date.now() / 1000;
          const active = docs.filter(d => d.expires > now).map(d => ({ ip: d.ip, reason: d.reason || '', expires: d.expires, createdBy: d.createdBy || 'Painel', createdAt: d.createdAt || 0, expired: false }));
          const expired = docs.filter(d => d.expires <= now).map(d => ({ ip: d.ip, reason: d.reason || '', expires: d.expires, createdBy: d.createdBy || 'Painel', createdAt: d.createdAt || 0, expired: true }));
          context.json(res, { success: true, active, expired });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/banip') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { ip, reason, duration } = body;
        if (!ip) {
          context.json(res, { success: false, error: 'IP obrigatorio' });
          return true;
        }
        const dur = parseInt(duration) || 315360000;
        const expires = Math.floor(Date.now() / 1000) + dur;
        await context.withMongo(async (db) => {
          await db.collection('ip_bans').insertOne({
            ip,
            reason: reason || 'IP banido pelo painel',
            expires,
            createdBy: 'Painel',
            createdAt: Date.now()
          });
          await context.writeIpBanCache();
          await context.callApi('kickbyip', `${context.XMPP_API}/kickbyip?ip=${encodeURIComponent(ip)}`);
          context.json(res, { success: true, message: `IP ${ip} banido` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/unbanip') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { ip } = body;
        if (!ip) {
          context.json(res, { success: false, error: 'IP obrigatorio' });
          return true;
        }
        await context.withMongo(async (db) => {
          await db.collection('ip_bans').deleteMany({ ip });
          await context.writeIpBanCache();
          context.json(res, { success: true, message: `IP ${ip} desbanido` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/kickbyip') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { ip } = body;
        if (!ip) {
          context.json(res, { success: false, error: 'IP obrigatorio' });
          return true;
        }
        const raw = await context.callApi('kickbyip', `${context.XMPP_API}/kickbyip?ip=${encodeURIComponent(ip)}`);
        let kicked = 0;
        if (raw) try { kicked = JSON.parse(raw).kicked || 0; } catch {}
        context.json(res, { success: true, message: `${kicked} conexao(oes) desconectada(s) do IP ${ip}` });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/clans') {
      try {
        const url = new URL(req.url, 'http://localhost');
        const search = url.searchParams.get('q') || '';
        await context.withMongo(async (db) => {
          const filter = search ? { name: { $regex: search, $options: 'i' } } : {};
          const clans = await db.collection('clans').find(filter).project({ name: 1, leader_name: 1, members: 1, points: 1, description: 1, date: 1 }).sort({ name: 1 }).limit(100).toArray();
          const result = clans.map(c => ({
            id: c._id,
            name: c.name,
            leader: c.leader_name || '',
            members: c.members ? c.members.length : 0,
            membersList: (c.members || []).map(m => ({ nickname: m.nickname, role: m.role, profile_id: m.profile_id })),
            points: c.points || 0,
            description: c.description || '',
            date: c.date || 0
          }));
          context.json(res, { success: true, clans: result });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/clan/create') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { name, leader_nick } = body;
        if (!name || !leader_nick) {
          context.json(res, { success: false, error: 'name e leader_nick obrigatorios' });
          return true;
        }
        await context.withMongo(async (db) => {
          const existing = await db.collection('clans').findOne({ name });
          if (existing) return context.json(res, { success: false, error: 'Clan ja existe' });
          const profile = await db.collection('profiles').findOne({ nick: leader_nick });
          if (!profile) return context.json(res, { success: false, error: 'Lider nao encontrado' });
          await db.collection('clans').insertOne({
            name,
            leader_name: leader_nick,
            leader_id: profile._id,
            members: [{ nickname: leader_nick, role: 'leader', profile_id: profile._id }],
            points: 0,
            description: '',
            date: Date.now()
          });
          await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { clan_name: name, clan_rank: 1 } });
          context.json(res, { success: true, message: `Clan ${name} criado com lider ${leader_nick}` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/clan/rename') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { oldName, newName } = body;
        if (!oldName || !newName) {
          context.json(res, { success: false, error: 'oldName e newName obrigatorios' });
          return true;
        }
        await context.withMongo(async (db) => {
          const existing = await db.collection('clans').findOne({ name: newName });
          if (existing) return context.json(res, { success: false, error: 'Nome ja em uso' });
          const clan = await db.collection('clans').findOne({ name: oldName });
          if (!clan) return context.json(res, { success: false, error: 'Clan nao encontrado' });
          await db.collection('clans').updateOne({ _id: clan._id }, { $set: { name: newName } });
          if (clan.members) {
            for (const m of clan.members) {
              await db.collection('profiles').updateOne({ _id: m.profile_id }, { $set: { clan_name: newName } });
            }
          }
          context.json(res, { success: true, message: `Clan renomeado para ${newName}` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/clan/kick') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { clan_name, member_nick } = body;
        if (!clan_name || !member_nick) {
          context.json(res, { success: false, error: 'clan_name e member_nick obrigatorios' });
          return true;
        }
        await context.withMongo(async (db) => {
          const clan = await db.collection('clans').findOne({ name: clan_name });
          if (!clan) return context.json(res, { success: false, error: 'Clan nao encontrado' });
          const idx = (clan.members || []).findIndex(m => m.nickname === member_nick);
          if (idx === -1) return context.json(res, { success: false, error: 'Membro nao encontrado no clan' });
          clan.members.splice(idx, 1);
          await db.collection('clans').updateOne({ _id: clan._id }, { $set: { members: clan.members } });
          await db.collection('profiles').updateOne({ nick: member_nick }, { $unset: { clan_name: '', clan_rank: '' } });
          await context.callApi('kick', `${context.XMPP_API}/kick?username=${encodeURIComponent(member_nick)}`);
          context.json(res, { success: true, message: `${member_nick} removido do clan ${clan_name}` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/removeitem') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick, item_name, item_id } = body;
        const nickValidation = context.validateNickInput(nick);
        if (!nickValidation.ok) return context.json(res, { success: false, error: nickValidation.error });

        const hasItemId = item_id !== undefined && item_id !== null && String(item_id).trim() !== '';
        if (!hasItemId && !context.asTrimmedString(item_name)) {
          return context.json(res, { success: false, error: 'nick e item_name ou item_id obrigatorios' });
        }

        let itemIdValidation = null;
        let itemNameValidation = null;
        if (hasItemId) {
          itemIdValidation = context.validateBoundedInt(item_id, { min: 1, max: 2147483647 }, 'ID do item');
          if (!itemIdValidation.ok) return context.json(res, { success: false, error: itemIdValidation.error });
        } else {
          itemNameValidation = context.validateFreeItemToken(item_name);
          if (!itemNameValidation.ok) return context.json(res, { success: false, error: itemNameValidation.error });
        }

        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick: nickValidation.value });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          const items = Array.isArray(profile.items) ? profile.items.slice() : [];
          let removed = [];
          if (itemIdValidation) {
            const idNum = itemIdValidation.value;
            const idx = items.findIndex(i => context.parseStrictInt(i && i.id) === idNum);
            if (idx >= 0) removed = items.splice(idx, 1);
          } else {
            const needle = itemNameValidation.value;
            const idx = items.findIndex(i => context.normalizeItemName(i && i.name) === needle);
            if (idx >= 0) removed = items.splice(idx, 1);
          }
          if (removed.length === 0) return context.json(res, { success: false, error: 'Item nao encontrado no inventario' });
          await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { items } });
          await context.callApi('setprofile(items)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickValidation.value)}&field=items&value=changed`);
          context.json(res, { success: true, message: `Item removido de ${nickValidation.value}` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/achievements/list') {
      try {
        const queryUrl = new URL(req.url, 'http://localhost');
        const nickRaw = context.asTrimmedString(queryUrl.searchParams.get('nick') || '');
        const offsetParsed = context.parseStrictInt(queryUrl.searchParams.get('offset'));
        const limitParsed = context.parseStrictInt(queryUrl.searchParams.get('limit'));
        const offset = offsetParsed === null ? 0 : Math.max(0, offsetParsed);
        const limit = limitParsed === null ? 80 : Math.max(1, Math.min(300, limitParsed));
        await context.withMongo(async (db) => {
          const catalogAgg = await db.collection('profiles').aggregate([
            { $project: { achievements: 1 } },
            { $unwind: '$achievements' },
            { $project: { _id: 0, id: '$achievements.achievement_id' } },
            { $match: { id: { $ne: null } } },
            { $project: { idStr: { $toString: '$id' } } },
            { $match: { idStr: { $regex: /^[0-9]{1,6}$/ } } },
            { $group: { _id: '$idStr' } },
            { $sort: { _id: 1 } }
          ]).toArray();
          const fromProfiles = catalogAgg.map(x => ({ id: context.asTrimmedString(x._id), icon: '', name: '', source: 'profiles' })).filter(x => x.id);
          const fromWiki = context.loadAchievementCatalogFromWikiIndex().map(id => ({ id, icon: '', name: '', source: 'wiki_numeric' }));
          const fromWikiNumericIcon = context.loadAchievementNumericEntriesFromWikiIndex();
          const fromUnlockItems = context.loadAchievementCatalogFromUnlockItems();
          const fromWikiVisual = context.loadAchievementVisualCatalogFromWikiIndex();
          const mergedMap = new Map();
          [...fromProfiles, ...fromWiki, ...fromWikiNumericIcon, ...fromUnlockItems, ...fromWikiVisual].forEach(row => {
            const id = context.asTrimmedString(row && row.id);
            if (!id) return;
            const prev = mergedMap.get(id);
            const icon = context.asTrimmedString(row && row.icon);
            const name = context.asTrimmedString(row && row.name);
            const source = context.asTrimmedString(row && row.source);
            if (!prev) mergedMap.set(id, { id, icon, name, source });
            else {
              if (!prev.icon && icon) prev.icon = icon;
              if (!prev.name && name) prev.name = name;
            }
          });
          const mergedAll = Array.from(mergedMap.values()).sort((a, b) => {
            const na = Number(a.id);
            const nb = Number(b.id);
            if (Number.isFinite(na) && Number.isFinite(nb)) return na - nb;
            if (Number.isFinite(na) && !Number.isFinite(nb)) return -1;
            if (!Number.isFinite(na) && Number.isFinite(nb)) return 1;
            return String(a.id).localeCompare(String(b.id));
          });
          const total = mergedAll.length;
          const catalog = mergedAll.slice(offset, offset + limit);
          let player = [];
          if (nickRaw) {
            const profile = await db.collection('profiles').findOne({ nick: nickRaw }, { projection: { achievements: 1, _id: 0 } });
            player = (profile && Array.isArray(profile.achievements) ? profile.achievements : [])
              .map(a => ({
                id: context.asTrimmedString(a && a.achievement_id),
                progress: context.parseStrictInt(a && a.progress) || 0,
                completion_time: context.parseStrictInt(a && a.completion_time) || 0
              }))
              .filter(a => a.id)
              .sort((a, b) => a.id.localeCompare(b.id));
          }
          context.json(res, { success: true, nick: nickRaw, player, catalog, total, offset, limit });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/achievements/give') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick, achievement_id, progress } = body;
        const nickValidation = context.validateNickInput(nick);
        if (!nickValidation.ok) return context.json(res, { success: false, error: nickValidation.error });
        const achievementValidation = context.validateAchievementId(achievement_id);
        if (!achievementValidation.ok) return context.json(res, { success: false, error: achievementValidation.error });
        let progressValidation = null;
        if (progress !== undefined && progress !== null && String(progress).trim() !== '') {
          progressValidation = context.validateBoundedInt(progress, context.PANEL_MODEL.achievement.progress, 'Progresso');
          if (!progressValidation.ok) return context.json(res, { success: false, error: progressValidation.error });
        }
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick: nickValidation.value });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          const achievements = Array.isArray(profile.achievements) ? profile.achievements.slice() : [];
          const achievementId = achievementValidation.value;
          const existing = achievements.find(a => context.asTrimmedString(a.achievement_id) === achievementId);
          if (existing) {
            const base = context.parseStrictInt(existing.progress);
            const computed = progressValidation ? progressValidation.value : ((base !== null ? base : 0) + 1);
            if (computed < context.PANEL_MODEL.achievement.progress.min || computed > context.PANEL_MODEL.achievement.progress.max) {
              return context.json(res, { success: false, error: `Progresso deve estar entre ${context.PANEL_MODEL.achievement.progress.min} e ${context.PANEL_MODEL.achievement.progress.max}` });
            }
            existing.progress = computed;
            const completionTime = context.parseStrictInt(existing.completion_time);
            const nowSec = Math.floor(Date.now() / 1000);
            existing.completion_time = completionTime === null || completionTime < 0 ? (computed > 0 ? nowSec : 0) : completionTime;
          } else {
            const computed = progressValidation ? progressValidation.value : 1;
            achievements.push({ achievement_id: achievementId, progress: computed, completion_time: computed > 0 ? Math.floor(Date.now() / 1000) : 0 });
          }
          await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { achievements } });
          await context.callApi('setprofile(achievements)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickValidation.value)}&field=achievements&value=changed`);
          context.json(res, { success: true, message: `Achievement ${achievementId} concedido a ${nickValidation.value}` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/achievements/remove') {
      if (route.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const { nick, achievement_id } = body;
        const nickValidation = context.validateNickInput(nick);
        if (!nickValidation.ok) return context.json(res, { success: false, error: nickValidation.error });
        const achievementValidation = context.validateAchievementId(achievement_id);
        if (!achievementValidation.ok) return context.json(res, { success: false, error: achievementValidation.error });
        await context.withMongo(async (db) => {
          const profile = await db.collection('profiles').findOne({ nick: nickValidation.value });
          if (!profile) return context.json(res, { success: false, error: 'Jogador nao encontrado' });
          const achievementId = achievementValidation.value;
          const achievements = (Array.isArray(profile.achievements) ? profile.achievements : []).filter(a => context.asTrimmedString(a.achievement_id) !== achievementId);
          await db.collection('profiles').updateOne({ _id: profile._id }, { $set: { achievements } });
          await context.callApi('setprofile(achievements)', `http://127.0.0.1:8080/setprofile?nick=${encodeURIComponent(nickValidation.value)}&field=achievements&value=changed`);
          context.json(res, { success: true, message: `Achievement ${achievementId} removido de ${nickValidation.value}` });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/gamerooms') {
      try {
        await context.withMongo(async (db) => {
          const docs = await db.collection('cache').find({ _id: { $regex: /^gamerooms_/ } }).toArray();
          const rooms = [];
          docs.forEach(d => { if (d.data) d.data.forEach(r => rooms.push(r)); });
          rooms.sort((a, b) => b.player_count - a.player_count);
          context.json(res, { success: true, rooms });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    return false;
  }];
}

module.exports = { registerPlayersRoutes };
