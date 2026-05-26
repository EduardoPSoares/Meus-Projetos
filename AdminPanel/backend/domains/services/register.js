function registerServicesRoutes(context = {}) {
  const handlers = [];

  handlers.push(async (req, res, route) => {
    if (route.pathname === '/api/runtime-config' && route.method === 'GET') {
      try {
        context.json(res, {
          success: true,
          runtime: context.readRuntimeConfigPayload(),
          ws: { path: context.RUNTIME_WS_PATH, schemaVersion: context.RUNTIME_SCHEMA_VERSION }
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }
    return false;
  });

  handlers.push(async (req, res, route) => {
    if (route.pathname === '/api/runtime-config/publish' && route.method === 'POST') {
      try {
        const body = await context.parseBody(req);
        const payload = body && typeof body.payload === 'object' ? body.payload : body;
        const mode = String((body && body.mode) || 'snapshot').toLowerCase() === 'patch' ? 'patch' : 'snapshot';
        const saved = context.writeRuntimeConfigPayload(payload);
        const envelope = context.buildRuntimeEnvelope(mode, saved);
        context.broadcastRuntimeEnvelope(envelope);
        context.json(res, { success: true, mode, envelope, clients: context.getRuntimeClientCount() });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }
    return false;
  });

  handlers.push(async (req, res, route) => {
    if (route.pathname === '/api/services' && route.method === 'GET') {
      const status = {};
      for (const k of Object.keys(context.services || {})) status[k] = await context.getServiceStatus(k);
      context.json(res, { success: true, services: status });
      return true;
    }

    if (route.pathname === '/api/service/start' && route.method === 'GET') {
      const url = new URL(req.url, 'http://localhost');
      const id = url.searchParams.get('id');
      if (!id || !context.services[id]) {
        context.json(res, { success: false, error: 'Servico invalido' });
        return true;
      }
      const ok = await context.startService(id);
      context.json(res, {
        success: ok,
        message: ok ? `${context.services[id].name} iniciado` : `Falha ao iniciar ${context.services[id].name}. Veja os logs do servico.`
      }, ok ? 200 : 500);
      return true;
    }

    if (route.pathname === '/api/service/stop' && route.method === 'GET') {
      const url = new URL(req.url, 'http://localhost');
      const id = url.searchParams.get('id');
      if (!id || !context.services[id]) {
        context.json(res, { success: false, error: 'Servico invalido' });
        return true;
      }
      context.stopService(id);
      context.json(res, { success: true, message: `${context.services[id].name} parado` });
      return true;
    }

    if (route.pathname === '/api/service/restart' && route.method === 'GET') {
      const url = new URL(req.url, 'http://localhost');
      const id = url.searchParams.get('id');
      if (!id || !context.services[id]) {
        context.json(res, { success: false, error: 'Servico invalido' });
        return true;
      }
      context.restartService(id);
      context.json(res, { success: true, message: `${context.services[id].name} reiniciando` });
      return true;
    }

    if (route.pathname === '/api/services/startAll') {
      setImmediate(() => {
        context.startAllServices().catch(e => context.log('SVC', `startAll error: ${e.message}`));
      });
      context.json(res, { success: true, message: 'Iniciando servicos em ordem (com 1 dedicado PvE + 1 PvP fixos)' });
      return true;
    }

    if (route.pathname === '/api/services/stopAll') {
      const result = context.stopAllServices({ hard: true });
      context.json(res, {
        success: true,
        message: result.killed.length
          ? `Parando todos os servicos. Hard-stop removeu ${result.killed.length} processo(s) restante(s).`
          : 'Parando todos os servicos. Nenhum processo restante encontrado no hard-stop.',
        killedPids: result.killed
      });
      return true;
    }

    if (route.pathname === '/api/logs') {
      const url = new URL(req.url, 'http://localhost');
      const id = url.searchParams.get('id');
      if (id === 'admin') return context.json(res, { success: true, logs: context.adminLogs }), true;
      if (id && context.services[id]) return context.json(res, { success: true, logs: context.services[id].logs }), true;
      const allLogs = {};
      Object.keys(context.services || {}).forEach(k => { allLogs[k] = context.services[k].logs; });
      allLogs.admin = context.adminLogs;
      context.json(res, { success: true, logs: allLogs });
      return true;
    }

    if (route.pathname === '/api/logs/stream') {
      res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Access-Control-Allow-Origin': '*'
      });
      context.sseClients.push(res);
      req.on('close', () => {
        const i = context.sseClients.indexOf(res);
        if (i >= 0) context.sseClients.splice(i, 1);
      });
      return true;
    }

    if (route.pathname === '/api/anticheat') {
      context.json(res, { success: true, config: context.getAcConfig() });
      return true;
    }

    if (route.pathname === '/api/anticheat/set') {
      try {
        const body = await context.parseBody(req);
        const { flag, enabled } = body;
        if (!flag) return context.json(res, { success: false, error: 'Flag obrigatoria' }), true;
        const ok = context.setAcFlag(flag, enabled);
        context.json(res, { success: ok, config: context.getAcConfig() });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/serverinfo') {
      try {
        const raw = await context.callApi('getonline', `${context.XMPP_API}/getonline`);
        const apiOnline = raw !== null;
        let online = 0;
        if (raw) try { online = JSON.parse(raw).online || 0; } catch {}
        let dbOnline = false;
        let playersCount = 0;
        try {
          await context.withMongo(async (db) => {
            dbOnline = true;
            playersCount = await db.collection('profiles').countDocuments();
          });
        } catch {}
        const srv = {};
        for (const k of Object.keys(context.services || {})) srv[k] = await context.getServiceStatus(k);
        const status = apiOnline && dbOnline && srv.xmpp && srv.xmpp.ready ? 'online' : 'offline';
        context.json(res, {
          success: true,
          server: {
            name: 'Warface DEV20',
            version: '1.22400.5519.45100',
            status,
            online: apiOnline ? online : 0,
            players: playersCount,
            database: dbOnline ? 'connected' : 'offline',
            xmppApi: apiOnline ? 'connected' : 'offline',
            uptime: context.process.uptime(),
            services: srv
          }
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/stats') {
      const cpus = context.os.cpus();
      const cpuLoad = cpus.reduce((s, c) => {
        const total = Object.values(c.times).reduce((a, b) => a + b, 0);
        const idle = c.times.idle;
        return s + (1 - idle / total);
      }, 0) / cpus.length * 100;
      context.json(res, {
        success: true,
        stats: {
          cpu: Math.round(cpuLoad * 10) / 10,
          cpus: cpus.length,
          memory: {
            total: context.os.totalmem(),
            free: context.os.freemem(),
            used: context.os.totalmem() - context.os.freemem(),
          },
          uptime: context.process.uptime(),
          hostname: context.os.hostname(),
          platform: context.os.platform(),
          arch: context.os.arch(),
        }
      });
      return true;
    }

    if (route.pathname === '/api/stats/history') {
      const url = new URL(req.url, 'http://localhost');
      const period = parseInt(url.searchParams.get('period')) || 3600;
      const cutoff = Date.now() - period * 1000;
      const filtered = (context.statsHistory || []).filter(e => e.time > cutoff);
      context.json(res, { success: true, stats: filtered, period });
      return true;
    }

    if (route.pathname === '/api/maintenance') {
      try {
        await context.withMongo(async (db) => {
          const doc = await db.collection('config').findOne({ _id: 'maintenance' });
          context.json(res, {
            success: true,
            enabled: doc ? doc.enabled : false,
            message: doc ? doc.message : 'Servidor em manutencao. Tente novamente mais tarde.'
          });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/maintenance/set') {
      if (route.method !== 'POST') return context.json(res, { success: false, error: 'Use POST' }, 405), true;
      try {
        const body = await context.parseBody(req);
        const { enabled, message } = body;
        await context.withMongo(async (db) => {
          await db.collection('config').updateOne(
            { _id: 'maintenance' },
            { $set: { enabled: !!enabled, message: message || 'Servidor em manutencao. Tente novamente mais tarde.', updatedAt: Date.now() } },
            { upsert: true }
          );
          try {
            const postData = JSON.stringify({ enabled: !!enabled, message: message || 'Servidor em manutencao. Tente novamente mais tarde.' });
            const reqUp = context.http.request({
              hostname: '127.0.0.1',
              port: 8080,
              method: 'POST',
              path: '/updatemaintenance',
              headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData) }
            });
            reqUp.write(postData);
            reqUp.end();
          } catch {}
          context.json(res, { success: true, message: enabled ? 'Modo manutencao ativado' : 'Modo manutencao desativado' });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/xp/disable') {
      if (route.method !== 'POST') return context.json(res, { success: false, error: 'Method not allowed' }, 405), true;
      try {
        await context.withMongo(async (db) => {
          await context.disableXpEvent(db);
          context.json(res, { success: true, message: 'Evento XP desativado' });
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/xp') {
      if (route.method === 'POST') {
        try {
          const body = await context.parseBody(req);
          const { multiplier } = body;
          const message = context.asTrimmedString(body.message || '');
          const durationMinutesRaw = body.duration_minutes;
          const multiplierValidation = context.validateBoundedInt(multiplier, context.PANEL_MODEL.xp.multiplier, 'Multiplicador');
          if (!multiplierValidation.ok) return context.json(res, { success: false, error: multiplierValidation.error }), true;
          const rate = multiplierValidation.value;
          const durationMinutes = durationMinutesRaw === undefined || durationMinutesRaw === null || String(durationMinutesRaw).trim() === '' ? 0 : context.parseStrictInt(durationMinutesRaw);
          if (durationMinutes === null || durationMinutes < 0 || durationMinutes > 10080) return context.json(res, { success: false, error: 'Duracao invalida (0 a 10080 minutos)' }), true;
          await context.withMongo(async (db) => {
            await db.collection('config').updateOne({ _id: 'xp_multiplier' }, { $set: { multiplier: rate } }, { upsert: true });
            const dynamicInfo = message || `Evento XP ${rate}x ativo`;
            const dynamicEnabled = rate > 1;
            const startedAt = dynamicEnabled ? Date.now() : null;
            const expiresAt = dynamicEnabled && durationMinutes > 0 ? startedAt + (durationMinutes * 60 * 1000) : null;
            await db.collection('cache').updateOne(
              { _id: 'dynamic_multipliers' },
              { $set: { data: { enabled: dynamicEnabled, multiplier: rate, info: dynamicInfo, startedAt, expiresAt }, hash: Date.now(), updatedAt: Date.now() } },
              { upsert: true }
            );
            try { await context.callApi('setxp', `${context.XMPP_API}/setxp?rate=${rate}`); } catch {}
            const msg = dynamicEnabled ? `Multiplicador de XP definido para ${rate}x${durationMinutes > 0 ? ` por ${durationMinutes} minuto(s)` : ''}${message ? ' com mensagem de evento' : ''}` : 'XP resetado para 1x (evento desativado)';
            context.json(res, { success: true, message: msg });
          });
        } catch (e) {
          context.json(res, { success: false, error: e.message }, 500);
        }
        return true;
      }
      if (route.method === 'GET') {
        try {
          await context.withMongo(async (db) => {
            const now = Date.now();
            const doc = await db.collection('config').findOne({ _id: 'xp_multiplier' });
            const parsedMultiplier = context.parseStrictInt(doc && doc.multiplier);
            const dynamicDoc = await db.collection('cache').findOne({ _id: 'dynamic_multipliers' });
            const multiplier = parsedMultiplier !== null && parsedMultiplier >= context.PANEL_MODEL.xp.multiplier.min && parsedMultiplier <= context.PANEL_MODEL.xp.multiplier.max ? parsedMultiplier : 1;
            const state = context.normalizeXpEventDoc(dynamicDoc);
            if (state.enabled && state.temporary && state.expiresAt && now >= state.expiresAt) {
              await context.disableXpEvent(db);
              return context.json(res, { success: true, multiplier: 1, message: '', active: false, startedAt: null, expiresAt: null, elapsedMs: 0, remainingMs: null, temporary: false });
            }
            context.json(res, {
              success: true,
              multiplier,
              message: state.info,
              active: state.enabled,
              startedAt: state.startedAt || null,
              expiresAt: state.expiresAt || null,
              elapsedMs: state.elapsedMs,
              remainingMs: state.remainingMs,
              temporary: state.temporary
            });
          });
        } catch (e) {
          context.json(res, { success: false, error: e.message }, 500);
        }
        return true;
      }
      return context.json(res, { success: false, error: 'Method not allowed' }, 405), true;
    }

    if (route.pathname === '/api/config') {
      try {
        const data = context.fs.readFileSync(context.MASTER_DIR + '/config.json', 'utf8');
        context.json(res, { success: true, config: JSON.parse(data) });
      } catch {
        context.json(res, { success: false, error: 'Nao foi possivel ler config' });
      }
      return true;
    }

    if (route.pathname === '/api/config/save') {
      try {
        const body = await context.parseBody(req);
        if (!body.config) return context.json(res, { success: false, error: 'config obrigatorio' }), true;
        context.fs.writeFileSync(context.MASTER_DIR + '/config.json', JSON.stringify(body.config, null, 4), 'utf8');
        context.json(res, { success: true, message: 'Configuracao salva' });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/paths') {
      try {
        const pathsFile = context.path.join(__dirname, '..', '..', 'app', 'paths.local.json');
        let data = {};
        try {
          if (context.fs.existsSync(pathsFile)) {
            data = JSON.parse(context.fs.readFileSync(pathsFile, 'utf8').replace(/^\uFEFF/, ''));
          }
        } catch {}
        context.json(res, {
          success: true,
          paths: {
            serverRoot: context.asTrimmedString(data.serverRoot),
            gameDir: context.asTrimmedString(data.gameDir),
            publicDir: context.asTrimmedString(data.publicDir)
          },
          pathsFile,
          hint: 'Reinicie o AdminPanel para aplicar os novos caminhos'
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/paths/save') {
      if (route.method !== 'POST') return context.json(res, { success: false, error: 'Use POST' }, 405), true;
      try {
        const body = await context.parseBody(req);
        const incoming = body && typeof body.paths === 'object' ? body.paths : body;
        const payload = {
          serverRoot: context.asTrimmedString(incoming && incoming.serverRoot),
          gameDir: context.asTrimmedString(incoming && incoming.gameDir),
          publicDir: context.asTrimmedString(incoming && incoming.publicDir)
        };

        if (!payload.serverRoot && !payload.gameDir && !payload.publicDir) {
          return context.json(res, { success: false, error: 'Informe ao menos um caminho' }, 400), true;
        }

        const pathsFile = context.path.join(__dirname, '..', '..', 'app', 'paths.local.json');
        context.fs.writeFileSync(pathsFile, JSON.stringify(payload, null, 2), 'utf8');
        context.json(res, {
          success: true,
          message: 'Caminhos salvos. Reinicie o AdminPanel para aplicar.',
          paths: payload,
          pathsFile,
          restartRequired: true
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/backup/list') {
      try {
        const backupDir = context.path.join(__dirname, '..', '..', 'app', 'backups');
        if (!context.fs.existsSync(backupDir)) return context.json(res, { success: true, backups: [] }), true;
        const items = context.fs.readdirSync(backupDir, { withFileTypes: true });
        const backups = items.filter(i => i.isDirectory()).map(d => {
          const dirPath = context.path.join(backupDir, d.name);
          const stats = context.fs.statSync(dirPath);
          return { name: d.name, size: context.getDirSize(dirPath), date: stats.mtimeMs };
        }).sort((a, b) => b.date - a.date);
        context.json(res, { success: true, backups });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/autobroadcast') {
      try {
        const dataPath = context.path.join(__dirname, '..', '..', 'app', 'autobroadcast.json');
        let config = { enabled: false, interval: 300, message: '', nextRun: 0 };
        try { config = JSON.parse(context.fs.readFileSync(dataPath, 'utf8')); } catch {}
        context.json(res, { success: true, config });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/autobroadcast/set') {
      if (route.method !== 'POST') return context.json(res, { success: false, error: 'Use POST' }, 405), true;
      try {
        const body = await context.parseBody(req);
        const { enabled, interval, message } = body;
        const dataPath = context.path.join(__dirname, '..', '..', 'app', 'autobroadcast.json');
        const config = {
          enabled: !!enabled,
          interval: Math.max(30, parseInt(interval) || 300),
          message: message || '',
          nextRun: Date.now() + (parseInt(interval) || 300) * 1000
        };
        context.fs.writeFileSync(dataPath, JSON.stringify(config, null, 2), 'utf8');
        if (context.broadcastTimers.main) {
          clearInterval(context.broadcastTimers.main);
          context.broadcastTimers.main = null;
        }
        if (config.enabled && config.message) {
          context.broadcastTimers.main = setInterval(async () => {
            try {
              const encoded = encodeURIComponent(config.message);
              await context.callApi('autobroadcast', `${context.XMPP_API}/broadcast?message=${encoded}`);
            } catch {}
          }, config.interval * 1000);
        }
        context.json(res, { success: true, message: 'Configuracao salva', config });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/backup') {
      try {
        const backupDir = context.path.join(__dirname, '..', '..', 'app', 'backups');
        if (!context.fs.existsSync(backupDir)) context.fs.mkdirSync(backupDir, { recursive: true });
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const backupName = `warface-backup-${timestamp}`;
        const backupPath = context.path.join(backupDir, backupName);
        const mongoBin = context.path.join(context.ROOT, 'MongoDb');
        const mongodumpPath = context.path.join(mongoBin, 'mongodump.exe');

        if (context.fs.existsSync(mongodumpPath)) {
          const { spawn } = require('child_process');
          await new Promise((resolve, reject) => {
            const proc = spawn(mongodumpPath, ['--out', backupPath, '--db', 'warface'], { cwd: mongoBin, stdio: ['ignore', 'pipe', 'pipe'] });
            let out = '';
            proc.stdout.on('data', d => out += d.toString());
            proc.stderr.on('data', d => out += d.toString());
            proc.on('close', code => code === 0 ? resolve() : reject(new Error(`mongodump exit code ${code}: ${out.substring(0, 200)}`)));
            proc.on('error', reject);
          });
          context.json(res, { success: true, message: `Backup criado: ${backupName}`, path: backupPath });
        } else {
          await context.withMongo(async (db) => {
            const collections = await db.listCollections().toArray();
            const dir = backupPath;
            context.fs.mkdirSync(dir, { recursive: true });
            for (const col of collections) {
              const docs = await db.collection(col.name).find({}).toArray();
              context.fs.writeFileSync(context.path.join(dir, `${col.name}.json`), JSON.stringify(docs, null, 2), 'utf8');
            }
            context.json(res, { success: true, message: `Backup JSON criado com ${collections.length} colecoes`, path: dir });
          });
        }
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    const exact = new Set([
      '/api/services', '/api/service/start', '/api/service/stop', '/api/service/restart',
      '/api/services/startAll', '/api/services/stopAll', '/api/logs', '/api/logs/stream',
      '/api/anticheat', '/api/anticheat/set', '/api/serverinfo', '/api/stats', '/api/stats/history',
      '/api/maintenance', '/api/maintenance/set', '/api/autobroadcast', '/api/autobroadcast/set',
      '/api/xp', '/api/xp/disable', '/api/backup', '/api/backup/list',
      '/api/config', '/api/config/save', '/api/paths', '/api/paths/save'
    ]);
    if (!exact.has(route.pathname)) return false;
    return false;
  });

  return handlers;
}

module.exports = { registerServicesRoutes };
