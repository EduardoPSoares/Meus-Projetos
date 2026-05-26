function parseVersionParts(version) {
  const clean = String(version || '').trim().replace(/^v/i, '');
  if (!clean) return [0];
  const parts = clean.split('.').map((part) => {
    const n = Number.parseInt(String(part).replace(/[^0-9].*$/, ''), 10);
    return Number.isFinite(n) && n >= 0 ? n : 0;
  });
  return parts.length ? parts : [0];
}

function compareVersions(a, b) {
  const pa = parseVersionParts(a);
  const pb = parseVersionParts(b);
  const len = Math.max(pa.length, pb.length);
  for (let i = 0; i < len; i++) {
    const va = pa[i] || 0;
    const vb = pb[i] || 0;
    if (va > vb) return 1;
    if (va < vb) return -1;
  }
  return 0;
}

function withCacheBuster(urlString) {
  const url = new URL(String(urlString || ''));
  url.searchParams.set('_ts', String(Date.now()));
  return url.toString();
}

function isLocalNetworkHost(hostname) {
  const host = String(hostname || '').toLowerCase();
  return host === 'localhost' || host === '127.0.0.1' || host === '::1';
}

function requireSafeRemoteUrl(urlString, label, options = {}) {
  let url;
  try {
    url = new URL(String(urlString || ''));
  } catch {
    throw new Error(`${label} invalida`);
  }

  const allowLocalHttp = options.allowLocalHttp !== false && isLocalNetworkHost(url.hostname);
  if (url.protocol !== 'https:' && !(allowLocalHttp && url.protocol === 'http:')) {
    throw new Error(`${label} deve usar HTTPS`);
  }
  if (url.username || url.password) {
    throw new Error(`${label} nao pode conter credenciais`);
  }
  return url;
}

function sanitizeTempVersion(value) {
  return String(value || '0.0.0').replace(/[^0-9A-Za-z._-]/g, '_').slice(0, 40) || '0.0.0';
}

module.exports = {
  parseVersionParts,
  compareVersions,
  withCacheBuster,
  isLocalNetworkHost,
  requireSafeRemoteUrl,
  sanitizeTempVersion
};
