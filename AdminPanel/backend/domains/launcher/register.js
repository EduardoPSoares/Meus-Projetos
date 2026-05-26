function registerLauncherRoutes(context = {}) {
  const prefixes = ['/api/launcher/', '/api/game/'];
  const exact = new Set(['/api/launcher/publish', '/api/launcher/config/save']);

  function resolveSafeTarget(rootDir, relPath, label) {
    const rel = context.normalizePatchEntryPath(relPath);
    if (!rel) throw new Error(`${label}: caminho invalido (${relPath || 'vazio'})`);
    const root = context.path.resolve(rootDir);
    const target = context.path.resolve(root, ...rel.split('/'));
    if (target !== root && !target.startsWith(root + context.path.sep)) {
      throw new Error(`${label}: caminho fora da pasta permitida (${rel})`);
    }
    return { rel, root, target };
  }

  function resolveVpsGameMirrorDir() {
    const r2 = context.readR2Config();
    const candidates = [
      context.asTrimmedString(r2 && r2.sourceDir),
      context.asTrimmedString(context.GAME_DIR)
    ]
      .filter(Boolean)
      .map(value => context.path.resolve(value));

    if (!candidates.length) {
      throw new Error('Pasta do jogo na VPS nao configurada. Defina em /api/game/source-dir/save.');
    }

    for (const candidate of candidates) {
      if (context.fs.existsSync(candidate) && context.fs.statSync(candidate).isDirectory()) {
        return candidate;
      }
    }

    throw new Error(`Pasta do jogo na VPS nao encontrada: ${candidates[0]}`);
  }

  function mirrorGameDeltaToVps(changedFiles = [], removedFiles = []) {
    const mirrorRoot = resolveVpsGameMirrorDir();
    let mirroredCount = 0;
    let mirroredSize = 0;
    let removedCount = 0;

    for (const file of changedFiles) {
      const rel = context.normalizePatchEntryPath(file && file.path);
      if (!rel) continue;
      const sourceInfo = resolveSafeTarget(context.GAME_CDN_DIR, rel, 'Stage CDN');
      if (!context.fs.existsSync(sourceInfo.target) || !context.fs.statSync(sourceInfo.target).isFile()) {
        throw new Error(`Arquivo staged nao encontrado para espelhamento da VPS: ${rel}`);
      }
      const targetInfo = resolveSafeTarget(mirrorRoot, rel, 'Espelhamento VPS');
      context.fs.mkdirSync(context.path.dirname(targetInfo.target), { recursive: true });
      context.fs.copyFileSync(sourceInfo.target, targetInfo.target);
      mirroredCount += 1;
      mirroredSize += Number(file && file.size || context.fs.statSync(sourceInfo.target).size || 0);
    }

    for (const file of removedFiles) {
      const rel = context.normalizePatchEntryPath(file && file.path);
      if (!rel) continue;
      const targetInfo = resolveSafeTarget(mirrorRoot, rel, 'Espelhamento VPS');
      try {
        if (context.fs.existsSync(targetInfo.target)) {
          context.fs.unlinkSync(targetInfo.target);
          removedCount += 1;
        }
      } catch {}
    }

    return {
      mirror_root: mirrorRoot,
      mirrored_count: mirroredCount,
      mirrored_size: mirroredSize,
      removed_count: removedCount
    };
  }

  return [async (req, res, route) => {
    if (!exact.has(route.pathname) && !prefixes.some(p => route.pathname.startsWith(p))) return false;

    if (route.pathname === '/api/launcher/sync-progress') {
      context.json(res, { success: true, progress: context.getDevSyncProgress('launcher') });
      return true;
    }

    if (route.pathname === '/api/launcher/publish-progress') {
      context.json(res, { success: true, progress: context.getLauncherPublishProgress() });
      return true;
    }

    if (route.pathname === '/api/launcher/source-dir/save') {
      try {
        const body = await context.parseBody(req);
        const selected = context.writeLauncherSourceDir(body.source_dir || body.sourceDir || body.path);
        context.json(res, { success: true, path: selected });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 400);
      }
      return true;
    }

    if (route.pathname === '/api/launcher/config/save') {
      try {
        const body = await context.parseBody(req);
        const config = body.config || {};
        context.fs.writeFileSync(context.LAUNCHER_DATA_FILE, JSON.stringify(config, null, 2), 'utf8');
        context.json(res, { success: true });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/launcher/browse-source-dir') {
      try {
        const body = await context.parseBody(req);
        const selected = await context.openFolderDialog(
          body.default_path || body.defaultPath || context.readLauncherSourceDir(),
          'Selecione a pasta do Dev onde o launcher esta sendo modificado'
        );
        if (!selected) {
          context.json(res, { success: false, cancelled: true, error: 'Selecao cancelada' }, 400);
          return true;
        }
        context.json(res, { success: true, path: selected });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/launcher/version/save') {
      try {
        const body = await context.parseBody(req);
        const version = context.normalizeVersionLabel(body.version);
        if (!version) {
          context.json(res, { success: false, error: 'Versao obrigatoria ou invalida' }, 400);
          return true;
        }
        const previousManifest = context.readLauncherManifest(req);
        const baseUrl = context.normalizeBaseUrl(body.base_url)
          || context.normalizeBaseUrl(previousManifest.base_url)
          || context.normalizeBaseUrl(context.readR2Config().publicBaseUrl)
          || context.getBaseUrl(req);
        const files = context.scanLauncherManifestFilesSync();
        const manifest = context.writeLauncherManifest(req, {
          version,
          notes: body.notes,
          base_url: baseUrl,
          files,
          changed_files: [],
          removed_files: [],
          previous_version: previousManifest.version
        });
        const data = {
          version,
          update_mode: 'manifest',
          manifest_url: context.buildLauncherManifestUrl(manifest.base_url, req),
          base_url: manifest.base_url,
          required: true,
          notes: context.asTrimmedString(body.notes),
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          updatedAt: new Date().toISOString()
        };
        context.fs.writeFileSync(context.LAUNCHER_VERSION_FILE, JSON.stringify(data, null, 2), 'utf8');
        context.json(res, { success: true });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/launcher/ref-info') {
      try {
        const launcherSourceDir = context.readLauncherSourceDir();
        let fileCount = 0;
        let totalSize = 0;
        for (const file of context.scanLauncherPublishFiles(launcherSourceDir)) {
          try {
            const stat = context.fs.statSync(file.fullPath);
            fileCount += 1;
            totalSize += Number(stat.size || 0);
          } catch {}
        }
        const r2 = context.readR2Config();
        const remoteManifest = await context.readRemoteLauncherManifest(req, 'warface-launcher');
        context.json(res, {
          success: true,
          path: `${context.path.join(launcherSourceDir, 'launcher-config.json')} + ${context.path.join(launcherSourceDir, 'launcher-images')}`,
          fileCount,
          totalSize,
          sourcePath: launcherSourceDir,
          r2Enabled: !!r2.enabled && !r2.error,
          r2PublicBaseUrl: r2.publicBaseUrl || '',
          r2Prefix: 'warface-launcher',
          devSync: context.getDevPublishStateInfo(launcherSourceDir, 'launcher', remoteManifest)
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/launcher/sync-from-cdn') {
      try {
        if (context.getDevSyncProgress('launcher').active) {
          context.json(res, { success: false, error: 'Sincronizacao do launcher ja esta em andamento' }, 409);
          return true;
        }
        const body = await context.parseBody(req);
        const sourceDir = context.path.resolve(context.asTrimmedString(body.source_dir || body.sourceDir || context.readLauncherSourceDir()));
        if (!context.fs.existsSync(sourceDir) || !context.fs.statSync(sourceDir).isDirectory()) {
          context.json(res, { success: false, error: `Pasta do launcher nao encontrada: ${sourceDir}` }, 400);
          return true;
        }
        context.setDevSyncProgress('launcher', { active: true, done: false, phase: 'start', percent: 1, message: 'Iniciando sync do launcher', source_dir: sourceDir, version: '', error: '', startedAt: new Date().toISOString(), completedAt: null });
        const launcherPrefix = context.normalizeR2Key(body.launcher_prefix || body.launcherPrefix || 'warface-launcher') || 'warface-launcher';
        context.json(res, { success: true, started: true, path: sourceDir });
        setImmediate(async () => {
          try {
            const manifest = await context.readRemoteLauncherManifest(req, launcherPrefix);
            const releaseDir = context.path.join(sourceDir, 'release');
            const result = await context.syncLocalFolderFromManifest(releaseDir, manifest, { key: 'launcher', onProgress: progress => context.setDevSyncProgress('launcher', { ...progress, version: manifest.version || '' }) });
            context.writeDevPublishState(sourceDir, 'launcher', manifest);
            context.setDevSyncProgress('launcher', { active: false, done: true, phase: 'complete', percent: 100, message: `Launcher sincronizado v${result.version}`, completedAt: new Date().toISOString(), ...result });
          } catch (e) {
            context.setDevSyncProgress('launcher', { active: false, done: true, phase: 'error', message: `Erro: ${e.message}`, error: e.message, completedAt: new Date().toISOString() });
          }
        });
      } catch (e) {
        context.setDevSyncProgress('launcher', { active: false, done: true, phase: 'error', message: `Erro: ${e.message}`, error: e.message, completedAt: new Date().toISOString() });
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/sync-progress') {
      context.json(res, { success: true, progress: context.getDevSyncProgress('game') });
      return true;
    }

    if (route.pathname === '/api/game/version/save') {
      try {
        const body = await context.parseBody(req);
        const version = context.normalizeVersionLabel(body.version);
        if (!version) {
          context.json(res, { success: false, error: 'Versao obrigatoria ou invalida' }, 400);
          return true;
        }
        const previousManifest = context.readGameManifest(req);
        const baseUrl = context.normalizeBaseUrl(body.base_url)
          || context.normalizeBaseUrl(previousManifest.base_url)
          || context.getPublicGameCdnBase(req);
        const manifest = context.writeGameManifest(req, {
          version,
          notes: body.notes,
          base_url: baseUrl,
          changed_files: [],
          removed_files: [],
          previous_version: previousManifest.version
        });
        const data = {
          version,
          update_mode: 'manifest',
          manifest_url: context.getPublicManifestUrl(req),
          base_url: manifest.base_url,
          required: true,
          notes: context.asTrimmedString(body.notes),
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          updatedAt: new Date().toISOString()
        };
        context.fs.writeFileSync(context.GAME_VERSION_FILE, JSON.stringify(data, null, 2), 'utf8');
        context.log('VERSION', `game version saved: ${data.version}`);
        context.json(res, { success: true });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/ref-info') {
      try {
        let fileCount = 0;
        let totalSize = 0;
        const r2 = context.readR2Config();
        function walkDir(dir) {
          try {
            const items = context.fs.readdirSync(dir);
            for (const item of items) {
              const p = context.path.join(dir, item);
              try {
                const s = context.fs.statSync(p);
                if (s.isDirectory()) walkDir(p);
                else { fileCount++; totalSize += s.size; }
              } catch {}
            }
          } catch {}
        }
        if (context.fs.existsSync(context.GAME_REF_DIR)) walkDir(context.GAME_REF_DIR);
        const sourcePath = r2.sourceDir || context.GAME_DIR;
        const remoteManifest = await context.readRemoteGameManifest(req);
        context.json(res, {
          success: true,
          path: context.GAME_REF_DIR,
          fileCount,
          totalSize,
          sourcePath,
          r2Enabled: !!r2.enabled && !r2.error,
          r2PublicBaseUrl: r2.publicBaseUrl || '',
          r2Prefix: r2.prefix || '',
          devSync: context.getDevPublishStateInfo(sourcePath, 'game', remoteManifest)
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/sync-from-cdn') {
      try {
        if (context.getDevSyncProgress('game').active) {
          context.json(res, { success: false, error: 'Sincronizacao do jogo ja esta em andamento' }, 409);
          return true;
        }
        const body = await context.parseBody(req);
        const r2 = context.readR2Config();
        const sourceDir = context.path.resolve(context.asTrimmedString(body.source_dir || body.sourceDir || r2.sourceDir || context.GAME_DIR));
        if (!context.fs.existsSync(sourceDir) || !context.fs.statSync(sourceDir).isDirectory()) {
          context.json(res, { success: false, error: `Pasta do client nao encontrada: ${sourceDir}` }, 400);
          return true;
        }
        context.setDevSyncProgress('game', { active: true, done: false, phase: 'start', percent: 1, message: 'Iniciando sync do jogo', source_dir: sourceDir, version: '', error: '', startedAt: new Date().toISOString(), completedAt: null });
        context.json(res, { success: true, started: true, path: sourceDir });
        setImmediate(async () => {
          try {
            const manifest = await context.readRemoteGameManifest(req);
            const result = await context.syncLocalFolderFromManifest(sourceDir, manifest, {
              key: 'game',
              skip: context.shouldSkipGamePublishPath,
              onProgress: progress => context.setDevSyncProgress('game', { ...progress, version: manifest.version || '' })
            });
            context.setDevSyncProgress('game', { active: false, done: true, phase: 'complete', percent: 100, message: `Jogo sincronizado v${result.version}`, completedAt: new Date().toISOString(), ...result });
          } catch (e) {
            context.setDevSyncProgress('game', { active: false, done: true, phase: 'error', message: `Erro: ${e.message}`, error: e.message, completedAt: new Date().toISOString() });
          }
        });
      } catch (e) {
        context.setDevSyncProgress('game', { active: false, done: true, phase: 'error', message: `Erro: ${e.message}`, error: e.message, completedAt: new Date().toISOString() });
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/publish-progress') {
      context.json(res, { success: true, progress: context.getGamePublishProgress() });
      return true;
    }

    if (route.pathname === '/api/game/publish-cancel') {
      context.resetGamePublishProgress();
      context.log('R2', 'publish state reset');
      context.json(res, { success: true, message: 'Estado de publicacao resetado' });
      return true;
    }

    if (route.pathname === '/api/game/source-dir/save') {
      try {
        const body = await context.parseBody(req);
        const selected = context.writeR2SourceDir(body.source_dir || body.sourceDir || body.path);
        context.log('R2', `game source dir saved: ${selected}`);
        context.json(res, { success: true, path: selected });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 400);
      }
      return true;
    }

    if (route.pathname === '/api/game/select-folder') {
      try {
        const body = await context.parseBody(req);
        const r2 = context.readR2Config();
        const selected = context.path.resolve(body.default_path || body.defaultPath || r2.sourceDir || context.GAME_DIR);
        if (!context.fs.existsSync(selected) || !context.fs.statSync(selected).isDirectory()) {
          context.json(res, { success: false, error: 'Pasta configurada invalida' }, 400);
          return true;
        }
        context.json(res, { success: true, path: selected, configured: true });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/browse-source-dir') {
      try {
        const body = await context.parseBody(req);
        const r2 = context.readR2Config();
        const selected = await context.openGameFolderDialog(body.default_path || body.defaultPath || r2.sourceDir || context.GAME_DIR);
        if (!selected) {
          context.json(res, { success: false, cancelled: true, error: 'Selecao cancelada' }, 400);
          return true;
        }
        context.json(res, { success: true, path: selected });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/stage-files') {
      if (req.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      try {
        const body = await context.parseBody(req);
        const files = Array.isArray(body.files) ? body.files : [];
        if (!files.length) {
          context.json(res, { success: false, error: 'Nenhum arquivo enviado para stage' }, 400);
          return true;
        }

        let stagedCount = 0;
        let stagedSize = 0;
        let skippedExcludedCount = 0;
        const stagedPaths = [];

        for (const file of files) {
          const rel = context.normalizePatchEntryPath(file.path || file.webkitRelativePath || file.name);
          if (!rel || context.shouldSkipGamePublishPath(rel)) {
            skippedExcludedCount++;
            continue;
          }
          const data = String(file.data || '');
          const comma = data.indexOf(',');
          const base64 = comma >= 0 ? data.slice(comma + 1) : data;
          const buffer = Buffer.from(base64, 'base64');
          if (!buffer.length) continue;
          context.writePublishedGameFile(rel, buffer);
          stagedCount++;
          stagedSize += buffer.length;
          stagedPaths.push(rel);
        }

        if (!stagedCount) {
          context.json(res, {
            success: false,
            error: skippedExcludedCount
              ? 'Todos os arquivos enviados estao na lista de exclusao do CDN'
              : 'Nenhum arquivo valido para stage'
          }, 400);
          return true;
        }

        context.json(res, {
          success: true,
          staged_count: stagedCount,
          staged_size: stagedSize,
          skipped_excluded_count: skippedExcludedCount,
          files: stagedPaths
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/publish-staged') {
      if (req.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      let progressStarted = false;
      try {
        if (context.getGamePublishProgress().active) {
          context.json(res, { success: false, error: 'Ja existe uma publicacao em andamento' }, 409);
          return true;
        }

        const body = await context.parseBody(req);
        const currentVersion = context.readGameVersionData().version;
        const version = context.normalizeVersionLabel(body.version) || context.nextVersionLabel(currentVersion);
        const notes = context.asTrimmedString(body.notes) || 'Atualizacao publicada pelo SurvivorDevelopers';
        const r2 = context.requireR2Config();
        const baseUrl = context.normalizeBaseUrl(body.base_url) || r2.publicBaseUrl;
        const previousManifest = context.readGameManifest(req);

        const changedRaw = Array.isArray(body.changed_paths) ? body.changed_paths : (Array.isArray(body.changedPaths) ? body.changedPaths : []);
        const removedRaw = Array.isArray(body.removed_paths) ? body.removed_paths : (Array.isArray(body.removedPaths) ? body.removedPaths : []);

        const changedSet = new Set(
          changedRaw
            .map(p => context.normalizePatchEntryPath(p))
            .filter(p => p && !context.shouldSkipGamePublishPath(p))
            .map(p => p.toLowerCase())
        );
        const removedSet = new Set(
          removedRaw
            .map(p => context.normalizePatchEntryPath(p))
            .filter(p => p && !context.shouldSkipGamePublishPath(p))
            .map(p => p.toLowerCase())
        );

        if (!changedSet.size && !removedSet.size) {
          context.json(res, {
            success: true,
            cancelled: true,
            no_changes: true,
            message: 'Nenhuma alteracao recebida para publicar',
            version: currentVersion || '0.0.0'
          });
          return true;
        }

        progressStarted = true;
        context.setGamePublishProgress({
          active: true,
          done: false,
          phase: 'start',
          percent: 2,
          message: 'Preparando delta recebido do SurvivorDevelopers',
          version,
          changed_count: changedSet.size,
          removed_count: removedSet.size,
          upload_total: changedSet.size,
          upload_index: 0,
          upload_bytes_done: 0,
          upload_bytes_current: 0,
          upload_bytes_total: 0,
          error: '',
          startedAt: new Date().toISOString(),
          completedAt: null
        });

        for (const relLower of removedSet) {
          const rel = removedRaw
            .map(p => context.normalizePatchEntryPath(p))
            .find(p => p && p.toLowerCase() === relLower);
          if (rel) context.removePublishedGameFile(rel);
        }

        const sourceFiles = context.scanManifestFiles(context.GAME_CDN_DIR);
        const sourceMap = new Map();
        for (const file of sourceFiles) {
          const rel = context.normalizePatchEntryPath(file.path);
          if (!rel) continue;
          sourceMap.set(rel.toLowerCase(), file);
        }

        const changedFiles = [];
        for (const relLower of changedSet) {
          const file = sourceMap.get(relLower);
          if (file) changedFiles.push(file);
        }

        const removedFiles = [];
        for (const prevFile of Array.isArray(previousManifest.files) ? previousManifest.files : []) {
          const rel = context.normalizePatchEntryPath(prevFile && prevFile.path);
          if (!rel) continue;
          if (removedSet.has(rel.toLowerCase())) removedFiles.push(prevFile);
        }

        let uploadBytesTotal = changedFiles.reduce((sum, file) => sum + Number(file.size || 0), 0);
        let uploadBytesDone = 0;
        let uploadedCount = 0;

        context.setGamePublishProgress({
          phase: 'upload',
          percent: 35,
          message: `Publicando ${changedFiles.length} arquivo(s) alterado(s)`,
          total_files: sourceFiles.length,
          hashed_files: sourceFiles.length,
          changed_count: changedFiles.length,
          removed_count: removedFiles.length,
          upload_total: changedFiles.length,
          upload_bytes_total: uploadBytesTotal
        });

        const percentForUpload = (index, currentBytes) => {
          const ratio = uploadBytesTotal > 0
            ? Math.min(1, (uploadBytesDone + Number(currentBytes || 0)) / uploadBytesTotal)
            : (changedFiles.length ? Math.min(1, index / changedFiles.length) : 1);
          return 35 + ratio * 50;
        };

        for (let i = 0; i < changedFiles.length; i++) {
          const file = changedFiles[i];
          const rel = context.normalizePatchEntryPath(file.path);
          const sourceFile = context.path.resolve(context.GAME_CDN_DIR, ...rel.split('/'));
          context.setGamePublishProgress({
            phase: 'upload',
            percent: percentForUpload(i, 0),
            message: `Enviando ${i + 1}/${changedFiles.length}: ${rel}`,
            current_file: rel,
            upload_index: i + 1,
            upload_total: changedFiles.length,
            upload_bytes_done: uploadBytesDone,
            upload_bytes_current: 0
          });
          await context.retryR2(`upload ${rel}`, () => {
            let currentAttemptBytes = 0;
            return context.r2PutFile(r2, rel, sourceFile, file, {
              onUploadProgress: bytes => {
                currentAttemptBytes += Number(bytes || 0);
                context.setGamePublishProgress({
                  phase: 'upload',
                  percent: percentForUpload(i, currentAttemptBytes),
                  message: `Enviando ${i + 1}/${changedFiles.length}: ${rel}`,
                  current_file: rel,
                  upload_index: i + 1,
                  upload_total: changedFiles.length,
                  upload_bytes_done: uploadBytesDone,
                  upload_bytes_current: currentAttemptBytes
                });
              }
            });
          });
          uploadedCount++;
          uploadBytesDone += Number(file.size || 0);
        }

        let removedCount = 0;
        for (let i = 0; i < removedFiles.length; i++) {
          const rel = context.normalizePatchEntryPath(removedFiles[i].path);
          if (!rel) continue;
          context.setGamePublishProgress({
            phase: 'delete',
            percent: 88 + ((i + 1) / Math.max(1, removedFiles.length)) * 7,
            message: `Removendo ${i + 1}/${removedFiles.length}: ${rel}`,
            current_file: rel,
            removed_done: i + 1,
            removed_count: removedFiles.length
          });
          await context.retryR2(`delete ${rel}`, () => context.r2DeleteObject(r2, rel));
          removedCount++;
        }

        context.setGamePublishProgress({
          phase: 'mirror',
          percent: 95,
          message: 'Espelhando alteracoes do delta para a pasta do jogo na VPS',
          current_file: ''
        });
        const vpsMirror = mirrorGameDeltaToVps(changedFiles, removedFiles);

        const manifestUrl = new URL(context.r2ObjectKey(r2, 'game-manifest.json'), baseUrl).toString();
        const versionUrl = new URL(context.r2ObjectKey(r2, 'game-version.json'), baseUrl).toString();

        const manifest = context.writeGameManifest(req, {
          version,
          notes,
          base_url: baseUrl,
          files: sourceFiles,
          changed_files: changedFiles,
          removed_files: removedFiles,
          previous_version: previousManifest.version
        });

        const gvData = {
          version,
          update_mode: 'manifest',
          manifest_url: manifestUrl,
          base_url: manifest.base_url,
          required: true,
          notes,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          uploaded_count: uploadedCount,
          uploaded_size: uploadBytesDone,
          skipped_existing_count: 0,
          skipped_existing_size: 0,
          removed_count: removedCount,
          vps_mirror_dir: vpsMirror.mirror_root,
          vps_mirrored_count: vpsMirror.mirrored_count,
          vps_mirrored_size: vpsMirror.mirrored_size,
          vps_removed_count: vpsMirror.removed_count,
          source: 'survivor-developers-delta',
          updatedAt: new Date().toISOString()
        };
        context.fs.writeFileSync(context.GAME_VERSION_FILE, JSON.stringify(gvData, null, 2), 'utf8');

        const historyEntry = {
          version,
          updatedAt: gvData.updatedAt,
          notes,
          source_dir: 'survivor-developers',
          manifest_url: manifestUrl,
          game_version_url: versionUrl,
          base_url: manifest.base_url,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          changed_count: changedFiles.length,
          uploaded_count: uploadedCount,
          uploaded_size: uploadBytesDone,
          skipped_existing_count: 0,
          skipped_existing_size: 0,
          removed_count: removedCount,
          vps_mirror_dir: vpsMirror.mirror_root,
          vps_mirrored_count: vpsMirror.mirrored_count,
          vps_mirrored_size: vpsMirror.mirrored_size,
          vps_removed_count: vpsMirror.removed_count,
          copied_size: 0,
          status: 'published'
        };
        const history = context.appendGameUpdateHistory(historyEntry);

        context.setGamePublishProgress({ phase: 'metadata', percent: 96, message: 'Publicando game-manifest.json', current_file: 'game-manifest.json' });
        await context.retryR2('upload game-manifest.json', () => context.r2PutJson(r2, 'game-manifest.json', manifest));
        context.setGamePublishProgress({ phase: 'metadata', percent: 98, message: 'Publicando game-version.json', current_file: 'game-version.json' });
        await context.retryR2('upload game-version.json', () => context.r2PutJson(r2, 'game-version.json', gvData));
        context.setGamePublishProgress({ phase: 'metadata', percent: 99, message: 'Publicando game-update-history.json', current_file: 'game-update-history.json' });
        await context.retryR2('upload game-update-history.json', () => context.r2PutJson(r2, 'game-update-history.json', { updates: history }));

        context.setGamePublishProgress({
          active: false,
          done: true,
          phase: 'complete',
          percent: 100,
          message: `Atualizacao v${version} publicada`,
          current_file: '',
          uploaded_count: uploadedCount,
          removed_done: removedCount,
          completedAt: new Date().toISOString()
        });

        context.json(res, {
          success: true,
          version,
          source_dir: 'survivor-developers',
          game_version_url: versionUrl,
          manifest_url: manifestUrl,
          base_url: manifest.base_url,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          uploaded_count: uploadedCount,
          uploaded_size: uploadBytesDone,
          skipped_existing_count: 0,
          skipped_existing_size: 0,
          removed_count: removedCount,
          vps_mirror_dir: vpsMirror.mirror_root,
          vps_mirrored_count: vpsMirror.mirrored_count,
          vps_mirrored_size: vpsMirror.mirrored_size,
          vps_removed_count: vpsMirror.removed_count
        });
      } catch (e) {
        context.log('R2', `publish staged error: ${e.message}`);
        if (progressStarted) {
          context.setGamePublishProgress({
            active: false,
            done: true,
            phase: 'error',
            message: `Erro: ${e.message}`,
            error: e.message,
            completedAt: new Date().toISOString()
          });
        }
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/upload-patch') {
      try {
        const body = await context.parseBody(req);
        const version = context.normalizeVersionLabel(body.version);
        if (!version) return context.json(res, { success: false, error: 'Versao obrigatoria ou invalida' }, 400), true;

        let selectedFileCount = 0;
        let uploadedBytes = 0;
        let skippedExcludedCount = 0;
        if (!Array.isArray(body.files) || body.files.length === 0) {
          return context.json(res, { success: false, error: 'Selecione arquivos ou uma pasta para publicar' }, 400), true;
        }

        for (const file of body.files) {
          const rel = context.normalizePatchEntryPath(file.path || file.webkitRelativePath || file.name);
          if (context.shouldSkipGamePublishPath(rel)) {
            skippedExcludedCount++;
            continue;
          }
          const data = String(file.data || '');
          const comma = data.indexOf(',');
          const base64 = comma >= 0 ? data.slice(comma + 1) : data;
          const buffer = Buffer.from(base64, 'base64');
          context.writePublishedGameFile(rel, buffer);
          selectedFileCount++;
          uploadedBytes += buffer.length;
        }
        if (!selectedFileCount) {
          return context.json(res, {
            success: false,
            error: skippedExcludedCount ? 'Todos os arquivos selecionados estao na lista de exclusao do CDN' : 'Nenhum arquivo valido para publicar'
          }, 400), true;
        }

        const baseUrl = context.normalizeBaseUrl(body.base_url) || context.getPublicGameCdnBase(req);
        const previousManifest = context.readGameManifest(req);
        const selectedPaths = new Set(
          body.files
            .map(file => context.normalizePatchEntryPath(file.path || file.webkitRelativePath || file.name))
            .filter(rel => !context.shouldSkipGamePublishPath(rel))
            .filter(Boolean)
            .map(rel => rel.toLowerCase())
        );
        const manifestFiles = context.scanManifestFiles(context.GAME_CDN_DIR);
        const selectedManifestFiles = manifestFiles.filter(file => selectedPaths.has(context.normalizePatchEntryPath(file.path).toLowerCase()));
        const manifest = context.writeGameManifest(req, {
          version,
          notes: body.notes,
          base_url: baseUrl,
          files: manifestFiles,
          changed_files: selectedManifestFiles,
          removed_files: [],
          previous_version: previousManifest.version
        });
        const gvData = {
          version,
          update_mode: 'manifest',
          manifest_url: context.getPublicManifestUrl(req),
          base_url: manifest.base_url,
          required: true,
          notes: context.asTrimmedString(body.notes),
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          uploaded_count: selectedFileCount,
          uploaded_size: uploadedBytes,
          source: 'files',
          updatedAt: new Date().toISOString()
        };
        context.fs.writeFileSync(context.GAME_VERSION_FILE, JSON.stringify(gvData, null, 2), 'utf8');
        context.json(res, {
          success: true,
          version,
          manifest_url: gvData.manifest_url,
          base_url: manifest.base_url,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          uploaded_count: selectedFileCount,
          uploaded_size: uploadedBytes
        });
      } catch (e) {
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/launcher/publish') {
      if (req.method !== 'POST') {
        context.json(res, { success: false, error: 'Use POST' }, 405);
        return true;
      }
      let progressStarted = false;
      try {
        if (context.getLauncherPublishProgress().active) {
          context.json(res, { success: false, error: 'Ja existe uma publicacao do launcher em andamento' }, 409);
          return true;
        }

        const body = await context.parseBody(req);
        const r2 = context.requireR2Config();
        const launcherSourceDir = context.path.resolve(context.asTrimmedString(body.source_dir || body.sourceDir || context.readLauncherSourceDir()));
        if (!context.fs.existsSync(launcherSourceDir) || !context.fs.statSync(launcherSourceDir).isDirectory()) {
          context.json(res, { success: false, error: `Pasta do launcher nao encontrada: ${launcherSourceDir}` }, 400);
          return true;
        }
        const launcherPrefixRaw = context.asTrimmedString(body.launcher_prefix || body.launcherPrefix || 'warface-launcher');
        const launcherPrefix = context.normalizeR2Key(launcherPrefixRaw || 'warface-launcher');
        if (!launcherPrefix) {
          context.json(res, { success: false, error: 'launcher_prefix invalido' }, 400);
          return true;
        }

        const version = context.normalizeVersionLabel(body.version) || new Date().toISOString().slice(0, 19).replace(/[T:]/g, '.');
        progressStarted = true;
        context.setLauncherPublishProgress({
          active: true,
          done: false,
          phase: 'start',
          percent: 1,
          message: 'Preparando publicacao do launcher',
          version,
          source_dir: launcherSourceDir,
          current_file: '',
          total_files: 0,
          hashed_files: 0,
          changed_count: 0,
          uploaded_count: 0,
          skipped_existing_count: 0,
          removed_count: 0,
          removed_done: 0,
          upload_index: 0,
          upload_total: 0,
          upload_bytes_done: 0,
          upload_bytes_current: 0,
          upload_bytes_total: 0,
          error: '',
          startedAt: new Date().toISOString(),
          completedAt: null
        });

        context.setLauncherPublishProgress({ phase: 'remote', percent: 4, message: 'Lendo manifest atual do CDN', current_file: 'launcher-manifest.json' });
        const remoteManifest = await context.readRemoteLauncherManifest(req, launcherPrefix);
        try {
          context.assertDevPublishReady(launcherSourceDir, 'launcher', remoteManifest);
        } catch (e) {
          if (e.code === 'DEV_FOLDER_OUTDATED') {
            context.json(res, { success: false, sync_required: true, error: e.message, sync: e.info }, 409);
            return true;
          }
          throw e;
        }

        context.setLauncherPublishProgress({ phase: 'build', percent: 8, message: 'Gerando instalador do launcher', current_file: 'WarfaceSurvivorSetup.exe' });
        const buildResult = await context.runLauncherBuildIfNeeded(launcherSourceDir);
        if (buildResult && buildResult.built) context.log('LAUNCHER', `build concluido em ${launcherSourceDir}`);
        context.setLauncherPublishProgress({ phase: 'build', percent: 30, message: 'Build concluida. Calculando hashes...', current_file: '' });

        const publicBase = context.normalizePublicBaseForPrefix(context.normalizeBaseUrl(body.public_base_url || body.publicBaseUrl) || r2.publicBaseUrl, launcherPrefix);
        const rootUrl = new URL(context.r2ObjectKey(r2, `${launcherPrefix}/`), publicBase).toString();
        const notes = context.asTrimmedString(body.notes);

        const files = await context.scanLauncherManifestFiles(progress => {
          const total = Number(progress.total_files || 0);
          const done = Number(progress.hashed_files || 0);
          const ratio = total > 0 ? done / total : 0;
          context.setLauncherPublishProgress({
            phase: 'hash',
            percent: 30 + ratio * 25,
            message: `Calculando SHA-256: ${done}/${total}`,
            total_files: total,
            hashed_files: done,
            current_file: progress.current_file || ''
          });
        }, launcherSourceDir);
        if (!files.length) {
          context.json(res, { success: false, error: 'Nenhum arquivo do launcher encontrado para publicar' }, 400);
          return true;
        }

        let previousManifest = null;
        try {
          previousManifest = await context.retryR2('download launcher-manifest.json', () => context.r2GetJson(r2, `${launcherPrefix}/launcher-manifest.json`));
        } catch {}
        if (!previousManifest || !Array.isArray(previousManifest.files)) previousManifest = context.readLauncherManifest(req);

        const diff = context.getManifestDiff(previousManifest, files);
        const uploadBytesTotal = diff.changed.reduce((sum, file) => sum + Number(file.size || 0), 0);
        const uploadPercent = (doneBytes, currentBytes, doneFiles) => {
          const ratio = uploadBytesTotal > 0
            ? Math.min(1, (Number(doneBytes || 0) + Number(currentBytes || 0)) / uploadBytesTotal)
            : (diff.changed.length ? Math.min(1, Number(doneFiles || 0) / diff.changed.length) : 1);
          return 58 + ratio * 32;
        };
        context.setLauncherPublishProgress({
          phase: 'diff',
          percent: 56,
          message: `Comparacao pronta: ${diff.changed.length} alterados, ${diff.removed.length} removidos`,
          total_files: files.length,
          hashed_files: files.length,
          changed_count: diff.changed.length,
          removed_count: diff.removed.length,
          upload_total: diff.changed.length,
          upload_bytes_total: uploadBytesTotal
        });
        let uploaded = 0;
        let deleted = 0;
        let uploadedBytes = 0;

        for (let i = 0; i < diff.changed.length; i++) {
          const file = diff.changed[i];
          const rel = `${launcherPrefix}/${file.path}`;
          const source = files.find(x => context.normalizePatchEntryPath(x.path).toLowerCase() === context.normalizePatchEntryPath(file.path).toLowerCase());
          if (!source || !source.fullPath) continue;
          let currentBytes = 0;
          context.setLauncherPublishProgress({
            phase: 'upload',
            percent: uploadPercent(uploadedBytes, 0, i),
            message: `Enviando ${i + 1}/${diff.changed.length}: ${file.path}`,
            current_file: file.path,
            upload_index: i + 1,
            upload_total: diff.changed.length,
            upload_bytes_done: uploadedBytes,
            upload_bytes_current: 0,
            upload_bytes_total: uploadBytesTotal
          });
          await context.retryR2(`upload ${rel}`, () => context.r2PutFile(r2, rel, source.fullPath, source, {
            contentType: context.mimeTypeByExt(context.path.extname(file.path)),
            cacheControl: file.path.endsWith('.json') ? 'no-cache' : 'public, max-age=31536000, immutable',
            onUploadProgress: chunkSize => {
              currentBytes += Number(chunkSize || 0);
              context.setLauncherPublishProgress({
                phase: 'upload',
                percent: uploadPercent(uploadedBytes, currentBytes, i),
                message: `Enviando ${i + 1}/${diff.changed.length}: ${file.path}`,
                current_file: file.path,
                upload_index: i + 1,
                upload_total: diff.changed.length,
                upload_bytes_done: uploadedBytes,
                upload_bytes_current: currentBytes,
                upload_bytes_total: uploadBytesTotal
              });
            }
          }));
          uploaded += 1;
          uploadedBytes += Number(file.size || 0);
        }

        for (let i = 0; i < diff.removed.length; i++) {
          const file = diff.removed[i];
          const rel = `${launcherPrefix}/${file.path}`;
          try {
            context.setLauncherPublishProgress({
              phase: 'delete',
              percent: 90 + ((i + 1) / Math.max(1, diff.removed.length)) * 4,
              message: `Removendo ${i + 1}/${diff.removed.length}: ${file.path}`,
              current_file: file.path,
              removed_done: i + 1,
              removed_count: diff.removed.length
            });
            await context.retryR2(`delete ${rel}`, () => context.r2DeleteObject(r2, rel));
            deleted += 1;
          } catch {}
        }

        const manifestFiles = files.map(({ fullPath, ...rest }) => rest);
        const changedFiles = diff.changed.map(({ fullPath, ...rest }) => rest);
        const removedFiles = diff.removed.map(({ fullPath, ...rest }) => rest);
        const manifest = {
          version,
          previous_version: String(previousManifest.version || '0.0.0'),
          update_mode: 'manifest',
          required: true,
          base_url: rootUrl,
          notes,
          generated_at: new Date().toISOString(),
          file_count: manifestFiles.length,
          total_size: manifestFiles.reduce((sum, file) => sum + Number(file.size || 0), 0),
          changed_files: changedFiles,
          changed_count: changedFiles.length,
          removed_files: removedFiles,
          removed_count: removedFiles.length,
          files: manifestFiles
        };

        const versionData = {
          version,
          update_mode: 'manifest',
          manifest_url: new URL(context.r2ObjectKey(r2, `${launcherPrefix}/launcher-manifest.json`), publicBase).toString(),
          base_url: rootUrl,
          required: true,
          notes,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          changed_count: manifest.changed_count,
          removed_count: manifest.removed_count,
          updatedAt: new Date().toISOString()
        };

        context.appendLauncherUpdateHistory({
          version,
          previous_version: manifest.previous_version,
          generated_at: manifest.generated_at,
          notes,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          changed_count: manifest.changed_count,
          removed_count: manifest.removed_count
        });
        context.writeDevPublishState(launcherSourceDir, 'launcher', manifest);

        context.fs.writeFileSync(context.LAUNCHER_MANIFEST_FILE, JSON.stringify(manifest, null, 2), 'utf8');
        context.fs.writeFileSync(context.LAUNCHER_VERSION_FILE, JSON.stringify(versionData, null, 2), 'utf8');

        context.setLauncherPublishProgress({ phase: 'metadata', percent: 95, message: 'Publicando launcher-manifest.json', current_file: 'launcher-manifest.json' });
        await context.retryR2('upload launcher-manifest.json', () => context.r2PutJson(r2, `${launcherPrefix}/launcher-manifest.json`, manifest));
        context.setLauncherPublishProgress({ phase: 'metadata', percent: 97, message: 'Publicando launcher-version.json', current_file: 'launcher-version.json' });
        await context.retryR2('upload launcher-version.json', () => context.r2PutJson(r2, `${launcherPrefix}/launcher-version.json`, versionData));
        context.setLauncherPublishProgress({ phase: 'metadata', percent: 99, message: 'Publicando historico do launcher', current_file: 'launcher-update-history.json' });
        await context.retryR2('upload launcher-update-history.json', () => context.r2PutJson(r2, `${launcherPrefix}/launcher-update-history.json`, { updates: context.readLauncherUpdateHistory() }));

        context.setLauncherPublishProgress({
          active: false,
          done: true,
          phase: 'complete',
          percent: 100,
          message: `Launcher v${version} publicado`,
          current_file: '',
          uploaded_count: uploaded,
          removed_done: deleted,
          completedAt: new Date().toISOString()
        });

        const cfgUrl = new URL(context.r2ObjectKey(r2, `${launcherPrefix}/launcher-config.json`), publicBase).toString();
        context.json(res, {
          success: true,
          message: `Launcher publicado (${uploaded} alterado(s), ${deleted} removido(s))`,
          uploaded,
          deleted,
          changed_count: manifest.changed_count,
          removed_count: manifest.removed_count,
          version,
          manifestUrl: versionData.manifest_url,
          rootUrl,
          configUrl: cfgUrl,
          prefix: launcherPrefix
        });
      } catch (e) {
        if (progressStarted) {
          context.setLauncherPublishProgress({
            active: false,
            done: true,
            phase: 'error',
            percent: context.getLauncherPublishProgress().percent || 0,
            message: `Erro: ${e.message}`,
            error: e.message,
            completedAt: new Date().toISOString()
          });
        }
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    if (route.pathname === '/api/game/publish-folder') {
      let progressStarted = false;
      try {
        if (context.getGamePublishProgress().active) {
          context.json(res, { success: false, error: 'Ja existe uma publicacao em andamento' }, 409);
          return true;
        }

        const body = await context.parseBody(req);
        const currentVersion = context.readGameVersionData().version;
        const version = context.normalizeVersionLabel(body.version) || context.nextVersionLabel(currentVersion);

        const r2 = context.requireR2Config();
        const sourceDir = context.path.resolve(context.asTrimmedString(body.source_dir || body.sourceDir || r2.sourceDir || context.GAME_DIR));
        if (!context.fs.existsSync(sourceDir) || !context.fs.statSync(sourceDir).isDirectory()) {
          context.json(res, { success: false, error: `Pasta do client nao encontrada: ${sourceDir}` }, 400);
          return true;
        }

        const remoteManifest = await context.readRemoteGameManifest(req);
        try {
          context.assertDevPublishReady(sourceDir, 'game', remoteManifest);
        } catch (e) {
          if (e.code === 'DEV_FOLDER_OUTDATED') {
            context.json(res, { success: false, sync_required: true, error: e.message, sync: e.info }, 409);
            return true;
          }
          throw e;
        }

        progressStarted = true;
        context.setGamePublishProgress({
          active: true,
          done: false,
          phase: 'start',
          percent: 1,
          message: 'Preparando publicacao',
          version,
          source_dir: sourceDir,
          current_file: '',
          total_files: 0,
          hashed_files: 0,
          changed_count: 0,
          uploaded_count: 0,
          skipped_existing_count: 0,
          removed_count: 0,
          removed_done: 0,
          upload_index: 0,
          upload_total: 0,
          upload_bytes_done: 0,
          upload_bytes_current: 0,
          upload_bytes_total: 0,
          error: '',
          startedAt: new Date().toISOString(),
          completedAt: null
        });

        const previousManifest = context.readGameManifest(req);
        const baseUrl = context.normalizeBaseUrl(body.base_url) || r2.publicBaseUrl;
        const sourceFiles = await context.scanManifestFilesStream(sourceDir, progress => {
          if (progress.phase === 'scan') {
            context.setGamePublishProgress({
              phase: 'scan',
              percent: 3,
              message: `Localizando arquivos: ${progress.scanned_files || 0}`,
              total_files: progress.scanned_files || 0
            });
            return;
          }
          const total = Number(progress.total_files || 0);
          const done = Number(progress.hashed_files || 0);
          const ratio = total > 0 ? done / total : 0;
          context.setGamePublishProgress({
            phase: 'hash',
            percent: 5 + ratio * 30,
            message: `Calculando SHA-256: ${done}/${total}`,
            total_files: total,
            hashed_files: done,
            current_file: progress.current_file || ''
          });
        });
        if (!sourceFiles.length) throw new Error('Nenhum arquivo encontrado na pasta do client');
        const diff = context.getManifestDiff(previousManifest, sourceFiles);
        let uploadBytesTotal = diff.changed.reduce((sum, file) => sum + Number(file.size || 0), 0);
        const uploadPercent = (doneBytes, currentBytes, doneFiles) => {
          const ratio = uploadBytesTotal > 0
            ? Math.min(1, (Number(doneBytes || 0) + Number(currentBytes || 0)) / uploadBytesTotal)
            : (diff.changed.length ? Math.min(1, Number(doneFiles || 0) / diff.changed.length) : 1);
          return 40 + ratio * 48;
        };
        context.setGamePublishProgress({
          phase: 'diff',
          percent: 38,
          message: `Comparacao pronta: ${diff.changed.length} alterados, ${diff.removed.length} removidos`,
          total_files: sourceFiles.length,
          hashed_files: sourceFiles.length,
          changed_count: diff.changed.length,
          removed_count: diff.removed.length,
          upload_total: diff.changed.length,
          upload_bytes_total: uploadBytesTotal
        });

        if (body.verify_remote === true) {
          const changedKeys = new Set(diff.changed.map(file => context.normalizePatchEntryPath(file.path).toLowerCase()).filter(Boolean));
          const removedKeys = new Set(diff.removed.map(file => context.normalizePatchEntryPath(file.path).toLowerCase()).filter(Boolean));
          let repairedCount = 0;
          for (let i = 0; i < sourceFiles.length; i++) {
            const file = sourceFiles[i];
            const rel = context.normalizePatchEntryPath(file.path);
            const key = rel.toLowerCase();
            if (!rel || changedKeys.has(key) || removedKeys.has(key)) continue;

            if (i % 10 === 0 || i + 1 === sourceFiles.length) {
              context.setGamePublishProgress({
                phase: 'verify',
                percent: 38 + ((i + 1) / Math.max(1, sourceFiles.length)) * 2,
                message: `Verificando R2: ${i + 1}/${sourceFiles.length}`,
                current_file: rel
              });
            }

            try {
              const head = await context.r2HeadObject(r2, rel);
              const remoteSize = Number(head.headers && head.headers['content-length'] || 0);
              const remoteHash = String(head.headers && head.headers['x-amz-meta-sha256'] || '').toLowerCase();
              if (remoteSize === Number(file.size || 0) && remoteHash === String(file.hash || '').toLowerCase()) continue;
            } catch {}

            diff.changed.push({ ...file, path: rel });
            changedKeys.add(key);
            repairedCount++;
          }

          if (repairedCount) {
            uploadBytesTotal = diff.changed.reduce((sum, file) => sum + Number(file.size || 0), 0);
            context.setGamePublishProgress({
              phase: 'diff',
              percent: 40,
              message: `R2 divergente: ${repairedCount} arquivos serao reenviados`,
              changed_count: diff.changed.length,
              upload_total: diff.changed.length,
              upload_bytes_total: uploadBytesTotal
            });
          }
        }

        if (!diff.changed.length && !diff.removed.length) {
          context.setGamePublishProgress({
            active: false,
            done: true,
            phase: 'nochange',
            percent: 100,
            message: 'Nenhum arquivo alterado. Envio cancelado.',
            current_file: '',
            completedAt: new Date().toISOString()
          });
          context.log('R2', `publish cancelled ${version}: no changed files`);
          context.json(res, {
            success: true,
            cancelled: true,
            no_changes: true,
            message: 'Nenhum arquivo alterado. Envio cancelado.',
            version: currentVersion,
            source_dir: sourceDir,
            file_count: sourceFiles.length,
            total_size: sourceFiles.reduce((sum, file) => sum + Number(file.size || 0), 0),
            uploaded_count: 0,
            uploaded_size: 0,
            skipped_existing_count: 0,
            skipped_existing_size: 0,
            removed_count: 0
          });
          return true;
        }

        const copyLocal = body.copy_local === true;
        let copiedBytes = 0;
        if (copyLocal) {
          for (const file of diff.changed) {
            const sourceFile = context.path.resolve(sourceDir, ...file.path.split('/'));
            if (!sourceFile.startsWith(sourceDir + context.path.sep)) throw new Error(`Caminho fora da pasta do client: ${file.path}`);
            context.copyPublishedGameFile(file.path, sourceFile);
            copiedBytes += Number(file.size || 0);
          }
          for (const file of diff.removed) context.removePublishedGameFile(file.path);
        }

        let uploadedBytes = 0;
        let skippedCount = 0;
        let skippedBytes = 0;
        let uploadProgressBytes = 0;
        const resumeExisting = body.resume_existing !== false;
        for (let i = 0; i < diff.changed.length; i++) {
          const file = diff.changed[i];
          const sourceFile = context.path.resolve(sourceDir, ...file.path.split('/'));
          context.setGamePublishProgress({
            phase: 'upload',
            percent: uploadPercent(uploadProgressBytes, 0, i),
            message: `Verificando ${i + 1}/${diff.changed.length}: ${file.path}`,
            current_file: file.path,
            upload_index: i + 1,
            upload_total: diff.changed.length,
            upload_bytes_done: uploadProgressBytes,
            upload_bytes_current: 0,
            upload_bytes_total: uploadBytesTotal,
            uploaded_count: Math.max(0, i - skippedCount),
            skipped_existing_count: skippedCount
          });
          if (resumeExisting) {
            try {
              const head = await context.r2HeadObject(r2, file.path);
              const remoteSize = Number(head.headers && head.headers['content-length'] || 0);
              const remoteHash = String(head.headers && head.headers['x-amz-meta-sha256'] || '').toLowerCase();
              if (remoteSize === Number(file.size || 0) && remoteHash === String(file.hash).toLowerCase()) {
                skippedCount++;
                skippedBytes += Number(file.size || 0);
                uploadProgressBytes += Number(file.size || 0);
                context.log('R2', `skip ${i + 1}/${diff.changed.length}: ${file.path}`);
                context.setGamePublishProgress({
                  phase: 'upload',
                  percent: uploadPercent(uploadProgressBytes, 0, i + 1),
                  message: `Mantido no R2 ${i + 1}/${diff.changed.length}: ${file.path}`,
                  current_file: file.path,
                  upload_index: i + 1,
                  upload_bytes_done: uploadProgressBytes,
                  upload_bytes_current: 0,
                  skipped_existing_count: skippedCount
                });
                continue;
              }
            } catch {}
          }
          context.log('R2', `upload ${i + 1}/${diff.changed.length}: ${file.path}`);
          await context.retryR2(`upload ${file.path}`, () => {
            let currentAttemptBytes = 0;
            context.setGamePublishProgress({
              phase: 'upload',
              percent: uploadPercent(uploadProgressBytes, 0, i),
              message: `Enviando ${i + 1}/${diff.changed.length}: ${file.path}`,
              current_file: file.path,
              upload_index: i + 1,
              upload_bytes_done: uploadProgressBytes,
              upload_bytes_current: 0
            });
            return context.r2PutFile(r2, file.path, sourceFile, file, {
              onUploadProgress: bytes => {
                currentAttemptBytes += Number(bytes || 0);
                context.setGamePublishProgress({
                  phase: 'upload',
                  percent: uploadPercent(uploadProgressBytes, currentAttemptBytes, i),
                  message: `Enviando ${i + 1}/${diff.changed.length}: ${file.path}`,
                  current_file: file.path,
                  upload_index: i + 1,
                  upload_bytes_done: uploadProgressBytes,
                  upload_bytes_current: currentAttemptBytes
                });
              }
            });
          });
          uploadedBytes += Number(file.size || 0);
          uploadProgressBytes += Number(file.size || 0);
          context.setGamePublishProgress({
            phase: 'upload',
            percent: uploadPercent(uploadProgressBytes, 0, i + 1),
            message: `Enviado ${i + 1}/${diff.changed.length}: ${file.path}`,
            current_file: file.path,
            uploaded_count: Math.max(0, i + 1 - skippedCount),
            upload_bytes_done: uploadProgressBytes,
            upload_bytes_current: 0
          });
        }
        for (let i = 0; i < diff.removed.length; i++) {
          const file = diff.removed[i];
          context.setGamePublishProgress({
            phase: 'delete',
            percent: 88 + ((i + 1) / Math.max(1, diff.removed.length)) * 5,
            message: `Removendo do R2 ${i + 1}/${diff.removed.length}: ${file.path}`,
            current_file: file.path,
            removed_done: i + 1,
            removed_count: diff.removed.length
          });
          context.log('R2', `delete ${i + 1}/${diff.removed.length}: ${file.path}`);
          await context.retryR2(`delete ${file.path}`, () => context.r2DeleteObject(r2, file.path));
        }

        context.setGamePublishProgress({
          phase: 'mirror',
          percent: 94,
          message: 'Espelhando alteracoes para a pasta do jogo na VPS',
          current_file: ''
        });
        const vpsMirror = mirrorGameDeltaToVps(diff.changed, diff.removed);

        const manifestUrl = new URL(context.r2ObjectKey(r2, 'game-manifest.json'), baseUrl).toString();
        const versionUrl = new URL(context.r2ObjectKey(r2, 'game-version.json'), baseUrl).toString();
        const notes = context.asTrimmedString(body.notes) || 'Atualizacao publicada automaticamente pelo painel';
        const manifest = context.writeGameManifest(req, {
          version,
          notes,
          base_url: baseUrl,
          files: sourceFiles,
          changed_files: diff.changed,
          removed_files: diff.removed,
          previous_version: previousManifest.version
        });
        const gvData = {
          version,
          update_mode: 'manifest',
          manifest_url: manifestUrl,
          base_url: manifest.base_url,
          required: true,
          notes,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          uploaded_count: diff.changed.length - skippedCount,
          uploaded_size: uploadedBytes,
          skipped_existing_count: skippedCount,
          skipped_existing_size: skippedBytes,
          removed_count: diff.removed.length,
          vps_mirror_dir: vpsMirror.mirror_root,
          vps_mirrored_count: vpsMirror.mirrored_count,
          vps_mirrored_size: vpsMirror.mirrored_size,
          vps_removed_count: vpsMirror.removed_count,
          source: 'r2-folder',
          updatedAt: new Date().toISOString()
        };
        context.fs.writeFileSync(context.GAME_VERSION_FILE, JSON.stringify(gvData, null, 2), 'utf8');

        const historyEntry = {
          version,
          updatedAt: gvData.updatedAt,
          notes,
          source_dir: sourceDir,
          manifest_url: manifestUrl,
          game_version_url: versionUrl,
          base_url: manifest.base_url,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          changed_count: diff.changed.length,
          uploaded_count: diff.changed.length - skippedCount,
          uploaded_size: uploadedBytes,
          skipped_existing_count: skippedCount,
          skipped_existing_size: skippedBytes,
          removed_count: diff.removed.length,
          copied_size: copiedBytes,
          vps_mirror_dir: vpsMirror.mirror_root,
          vps_mirrored_count: vpsMirror.mirrored_count,
          vps_mirrored_size: vpsMirror.mirrored_size,
          vps_removed_count: vpsMirror.removed_count,
          status: 'published'
        };
        const history = context.appendGameUpdateHistory(historyEntry);
        context.writeDevPublishState(sourceDir, 'game', manifest);

        context.setGamePublishProgress({ phase: 'metadata', percent: 95, message: 'Publicando game-manifest.json', current_file: 'game-manifest.json' });
        await context.retryR2('upload game-manifest.json', () => context.r2PutJson(r2, 'game-manifest.json', manifest));
        context.setGamePublishProgress({ phase: 'metadata', percent: 97, message: 'Publicando game-version.json', current_file: 'game-version.json' });
        await context.retryR2('upload game-version.json', () => context.r2PutJson(r2, 'game-version.json', gvData));
        context.setGamePublishProgress({ phase: 'metadata', percent: 99, message: 'Publicando historico de atualizacoes', current_file: 'game-update-history.json' });
        await context.retryR2('upload game-update-history.json', () => context.r2PutJson(r2, 'game-update-history.json', { updates: history }));

        context.setGamePublishProgress({
          active: false,
          done: true,
          phase: 'complete',
          percent: 100,
          message: `Atualizacao v${version} publicada`,
          current_file: '',
          uploaded_count: diff.changed.length - skippedCount,
          skipped_existing_count: skippedCount,
          removed_done: diff.removed.length,
          completedAt: new Date().toISOString()
        });
        context.log('R2', `published ${version}: changed=${diff.changed.length}, removed=${diff.removed.length}, total=${manifest.file_count}`);
        context.json(res, {
          success: true,
          version,
          source_dir: sourceDir,
          game_version_url: versionUrl,
          manifest_url: manifestUrl,
          base_url: manifest.base_url,
          file_count: manifest.file_count,
          total_size: manifest.total_size,
          uploaded_count: diff.changed.length - skippedCount,
          uploaded_size: uploadedBytes,
          skipped_existing_count: skippedCount,
          skipped_existing_size: skippedBytes,
          copied_size: copiedBytes,
          removed_count: diff.removed.length,
          vps_mirror_dir: vpsMirror.mirror_root,
          vps_mirrored_count: vpsMirror.mirrored_count,
          vps_mirrored_size: vpsMirror.mirrored_size,
          vps_removed_count: vpsMirror.removed_count
        });
      } catch (e) {
        context.log('R2', `publish error: ${e.message}`);
        if (progressStarted) {
          context.setGamePublishProgress({
            active: false,
            done: true,
            phase: 'error',
            percent: context.getGamePublishProgress().percent || 0,
            message: `Erro: ${e.message}`,
            error: e.message,
            completedAt: new Date().toISOString()
          });
        }
        context.json(res, { success: false, error: e.message }, 500);
      }
      return true;
    }

    return false;
  }];
}

module.exports = { registerLauncherRoutes };
