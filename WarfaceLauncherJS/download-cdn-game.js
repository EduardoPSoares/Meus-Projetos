const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');

let config = {};
try {
  config = require('./config');
} catch {
  config = {};
}

const DEFAULT_MANIFEST_URL = config.GAME_MANIFEST_URL || 'https://pub-5359fa4ead8743cbbc40f1166dd089f1.r2.dev/game-manifest.json';
const DEFAULT_GAME_DIR = config.GAME_PATH || 'C:\\WarfaceSurvivor';
const MAX_REDIRECTS = 5;

const args = parseArgs(process.argv.slice(2));
const manifestUrl = args.manifest || DEFAULT_MANIFEST_URL;
const gameDir = path.resolve(args.dir || DEFAULT_GAME_DIR);
const parallel = Math.max(1, Math.min(16, Number(args.parallel || 4) || 4));
const retriesPerFile = Math.max(1, Math.min(20, Number(args.retries || 6) || 6));
const retryPasses = Math.max(0, Math.min(10, Number(args['retry-passes'] || 4) || 4));
const force = args.force === true;

let cancelled = false;
let activeRequest = null;

process.on('SIGINT', () => {
  cancelled = true;
  if (activeRequest) {
    try { activeRequest.destroy(); } catch {}
  }
  console.log('\nDownload cancelado. Execute novamente para continuar pelos arquivos ja concluidos.');
  process.exit(130);
});

main().catch(error => {
  console.error(`\nERRO: ${error.message}`);
  process.exit(1);
});

async function main() {
  console.log('Warface CDN Downloader');
  console.log(`Manifest: ${manifestUrl}`);
  console.log(`Destino:  ${gameDir}`);
  console.log(`Paralelo: ${parallel}`);
  console.log(`Tentativas por arquivo: ${retriesPerFile}`);
  console.log(`Rodadas extras para falhas: ${retryPasses}`);
  console.log('');

  const manifest = await getJson(manifestUrl);
  if (!manifest || !Array.isArray(manifest.files)) {
    throw new Error('Manifest invalido ou sem lista de arquivos.');
  }
  if (!manifest.base_url) {
    throw new Error('Manifest sem base_url.');
  }

  fs.mkdirSync(gameDir, { recursive: true });

  const files = manifest.files
    .map(file => normalizeManifestFile(file))
    .filter(Boolean);

  if (!files.length) throw new Error('Nenhum arquivo valido no manifest.');

  const totalBytes = files.reduce((sum, file) => sum + Number(file.size || 0), 0);
  let alreadyBytes = 0;
  const pending = [];

  for (const file of files) {
    const targetPath = safeTargetPath(gameDir, file.path);
    file.targetPath = targetPath;
    if (!force && fs.existsSync(targetPath)) {
      const size = fs.statSync(targetPath).size;
      if (size === Number(file.size || 0)) {
        alreadyBytes += size;
        continue;
      }
    }
    pending.push(file);
  }

  console.log(`Versao: ${manifest.version || 'desconhecida'}`);
  console.log(`Arquivos no manifest: ${files.length}`);
  console.log(`Ja existentes: ${files.length - pending.length}`);
  console.log(`Para baixar: ${pending.length}`);
  console.log(`Tamanho total: ${formatBytes(totalBytes)}`);
  console.log('');

  if (args['dry-run'] === true) {
    console.log('Dry-run: manifest carregado com sucesso. Nenhum arquivo foi baixado.');
    return;
  }

  if (!pending.length) {
    console.log('Nada para baixar. O jogo ja parece completo pelo tamanho dos arquivos.');
    return;
  }

  let completedFiles = files.length - pending.length;
  let downloadedBytes = alreadyBytes;
  const failedFiles = [];
  const startTime = Date.now();
  const queue = pending.slice();

  const workers = Array.from({ length: parallel }, async () => {
    while (queue.length && !cancelled) {
      const file = queue.shift();
      const fileUrl = resolveFileUrl(manifest.base_url, file.path);
      try {
        const result = await downloadWithRetry(fileUrl, file.targetPath, file.size, retriesPerFile, progress => {
          renderProgress({
            completedFiles,
            totalFiles: files.length,
            downloadedBytes: downloadedBytes + progress.currentBytes,
            totalBytes,
            currentFile: file.path,
            startTime
          });
        });
        downloadedBytes += result.bytes;
        completedFiles++;
        renderProgress({ completedFiles, totalFiles: files.length, downloadedBytes, totalBytes, currentFile: file.path, startTime });
      } catch (error) {
        failedFiles.push({ file, url: fileUrl, error: error.message });
        console.log(`\nFalhou: ${file.path}`);
        console.log(`Motivo: ${error.message}`);
      }
    }
  });

  await Promise.all(workers);
  process.stdout.write('\n');

  for (let pass = 1; failedFiles.length && pass <= retryPasses && !cancelled; pass++) {
    const retryList = failedFiles.splice(0);
    console.log(`Tentando novamente arquivos que falharam (${pass}/${retryPasses}): ${retryList.length}`);

    for (const item of retryList) {
      const file = item.file;
      if (!force && fs.existsSync(file.targetPath) && fs.statSync(file.targetPath).size === Number(file.size || 0)) {
        completedFiles++;
        continue;
      }

      try {
        const result = await downloadWithRetry(item.url, file.targetPath, file.size, retriesPerFile, progress => {
          renderProgress({
            completedFiles,
            totalFiles: files.length,
            downloadedBytes: downloadedBytes + progress.currentBytes,
            totalBytes,
            currentFile: file.path,
            startTime
          });
        });
        downloadedBytes += result.bytes;
        completedFiles++;
        renderProgress({ completedFiles, totalFiles: files.length, downloadedBytes, totalBytes, currentFile: file.path, startTime });
      } catch (error) {
        failedFiles.push({ file, url: item.url, error: error.message });
        console.log(`\nAinda falhou: ${file.path}`);
        console.log(`Motivo: ${error.message}`);
      }
    }
    process.stdout.write('\n');
  }

  if (failedFiles.length) {
    const logFile = writeFailureLog(failedFiles);
    throw new Error(`${failedFiles.length} arquivo(s) falharam. Lista salva em: ${logFile}`);
  }

  console.log('Download concluido.');
  console.log(`Pasta do jogo: ${gameDir}`);
}

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--force') {
      out.force = true;
    } else if (arg.startsWith('--')) {
      const key = arg.slice(2);
      const next = argv[i + 1];
      if (next && !next.startsWith('--')) {
        out[key] = next;
        i++;
      } else {
        out[key] = true;
      }
    }
  }
  return out;
}

function getJson(urlString) {
  return new Promise((resolve, reject) => {
    requestBuffer(urlString, 0, (error, data) => {
      if (error) return reject(error);
      try {
        resolve(JSON.parse(data.toString('utf8').replace(/^\uFEFF/, '')));
      } catch {
        reject(new Error('Resposta JSON invalida.'));
      }
    });
  });
}

function requestBuffer(urlString, redirects, done) {
  if (redirects > MAX_REDIRECTS) return done(new Error('Muitos redirecionamentos.'));
  let url;
  try {
    url = new URL(urlString);
  } catch {
    return done(new Error(`URL invalida: ${urlString}`));
  }

  const transport = url.protocol === 'https:' ? https : http;
  const req = transport.get(url, response => {
    if ([301, 302, 307, 308].includes(response.statusCode)) {
      const next = response.headers.location ? new URL(response.headers.location, url).toString() : '';
      response.resume();
      if (!next) return done(new Error('Redirect sem destino.'));
      return requestBuffer(next, redirects + 1, done);
    }
    if (response.statusCode !== 200) {
      response.resume();
      return done(new Error(`HTTP ${response.statusCode}`));
    }
    const chunks = [];
    response.on('data', chunk => chunks.push(chunk));
    response.on('end', () => done(null, Buffer.concat(chunks)));
  });
  req.setTimeout(60000, () => req.destroy(new Error('Tempo esgotado.')));
  req.on('error', error => done(error));
}

function normalizeManifestFile(file) {
  const rel = normalizeManifestPath(file && file.path);
  const size = Number(file && file.size || 0);
  if (!rel || size < 0) return null;
  return { ...file, path: rel, size };
}

function normalizeManifestPath(value) {
  const raw = String(value || '').replace(/\\/g, '/').replace(/^[A-Za-z]:\//, '').replace(/^\/+/, '');
  if (!raw) return '';
  const normalized = path.posix.normalize(raw);
  if (!normalized || normalized === '.' || normalized.startsWith('../') || normalized.includes('/../')) return '';
  if (/[<>:"|?*\x00-\x1F]/.test(normalized)) return '';
  return normalized;
}

function safeTargetPath(root, relPath) {
  const target = path.resolve(root, ...relPath.split('/'));
  if (!target.startsWith(root + path.sep)) throw new Error(`Caminho fora do destino: ${relPath}`);
  return target;
}

function resolveFileUrl(baseUrl, relPath) {
  const base = String(baseUrl || '').endsWith('/') ? String(baseUrl) : `${baseUrl}/`;
  return new URL(relPath.split('/').map(encodeURIComponent).join('/'), base).toString();
}

async function downloadWithRetry(url, targetPath, expectedSize, attempts, onProgress) {
  let lastError = null;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await downloadFile(url, targetPath, expectedSize, onProgress);
    } catch (error) {
      lastError = error;
      if (attempt < attempts) await sleep(1500 * attempt);
    }
  }
  throw lastError;
}

function writeFailureLog(failedFiles) {
  const logFile = path.join(__dirname, 'download-cdn-failed.txt');
  const lines = [];
  lines.push(`Falhas em ${new Date().toISOString()}`);
  lines.push(`Destino: ${gameDir}`);
  lines.push('');
  for (const item of failedFiles) {
    lines.push(item.file.path);
    lines.push(`  URL: ${item.url}`);
    lines.push(`  Erro: ${item.error}`);
    lines.push('');
  }
  fs.writeFileSync(logFile, lines.join('\n'), 'utf8');
  return logFile;
}

function downloadFile(urlString, targetPath, expectedSize, onProgress) {
  return new Promise((resolve, reject) => {
    const tmpPath = `${targetPath}.download`;
    let fileStream = null;
    let received = 0;

    const cleanup = () => {
      activeRequest = null;
      try { if (fileStream) fileStream.close(); } catch {}
    };

    const start = (nextUrl, redirects) => {
      if (redirects > MAX_REDIRECTS) return reject(new Error('Muitos redirecionamentos.'));
      let url;
      try {
        url = new URL(nextUrl);
      } catch {
        return reject(new Error(`URL invalida: ${nextUrl}`));
      }

      const transport = url.protocol === 'https:' ? https : http;
      fs.mkdirSync(path.dirname(targetPath), { recursive: true });
      fileStream = fs.createWriteStream(tmpPath);

      const req = transport.get(url, response => {
        if ([301, 302, 307, 308].includes(response.statusCode)) {
          const redirectUrl = response.headers.location ? new URL(response.headers.location, url).toString() : '';
          response.resume();
          cleanup();
          try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
          if (!redirectUrl) return reject(new Error('Redirect sem destino.'));
          return start(redirectUrl, redirects + 1);
        }
        if (response.statusCode !== 200) {
          response.resume();
          cleanup();
          return reject(new Error(`HTTP ${response.statusCode}`));
        }

        response.on('data', chunk => {
          received += chunk.length;
          if (typeof onProgress === 'function') onProgress({ currentBytes: received });
        });
        response.pipe(fileStream);
      });

      activeRequest = req;
      req.setTimeout(120000, () => req.destroy(new Error('Tempo esgotado.')));
      req.on('error', error => {
        cleanup();
        try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
        reject(error);
      });

      fileStream.on('finish', () => {
        fileStream.close(() => {
          cleanup();
          try {
            const actualSize = fs.statSync(tmpPath).size;
            if (Number(expectedSize || 0) > 0 && actualSize !== Number(expectedSize)) {
              try { fs.unlinkSync(tmpPath); } catch {}
              reject(new Error(`Tamanho invalido: ${actualSize}/${expectedSize}`));
              return;
            }
            if (fs.existsSync(targetPath)) fs.unlinkSync(targetPath);
            fs.renameSync(tmpPath, targetPath);
            resolve({ bytes: actualSize });
          } catch (error) {
            reject(error);
          }
        });
      });
      fileStream.on('error', error => {
        cleanup();
        try { if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath); } catch {}
        reject(error);
      });
    };

    start(urlString, 0);
  });
}

function renderProgress(state) {
  const elapsed = Math.max(1, (Date.now() - state.startTime) / 1000);
  const speed = state.downloadedBytes / elapsed;
  const pct = state.totalBytes > 0 ? (state.downloadedBytes / state.totalBytes) * 100 : 0;
  const line = `${pct.toFixed(2)}% | ${state.completedFiles}/${state.totalFiles} | ${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)} | ${formatBytes(speed)}/s | ${state.currentFile}`;
  process.stdout.write(`\r${line.slice(0, 180).padEnd(180, ' ')}`);
}

function formatBytes(bytes) {
  const n = Number(bytes || 0);
  if (n >= 1024 ** 4) return `${(n / 1024 ** 4).toFixed(2)} TB`;
  if (n >= 1024 ** 3) return `${(n / 1024 ** 3).toFixed(2)} GB`;
  if (n >= 1024 ** 2) return `${(n / 1024 ** 2).toFixed(2)} MB`;
  if (n >= 1024) return `${(n / 1024).toFixed(2)} KB`;
  return `${n} B`;
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
