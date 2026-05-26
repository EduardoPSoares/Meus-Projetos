export function formatTime(seconds) {
  if (!seconds || seconds <= 0 || !isFinite(seconds)) return '--';
  if (seconds < 60) return `${seconds}s`;
  if (seconds < 3600) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  }
  const hours = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  return `${hours}h ${mins}m`;
}

export function formatBytes(bytes) {
  const value = Number(bytes || 0);
  if (!Number.isFinite(value) || value <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = value;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit++;
  }
  const decimals = unit >= 2 ? 2 : 0;
  return `${size.toFixed(decimals)} ${units[unit]}`;
}

export function formatSpeed(bytesPerSecond) {
  const value = Number(bytesPerSecond || 0);
  if (!Number.isFinite(value) || value <= 0) return '--';
  return `${formatBytes(value)}/s`;
}

export function shortenPathMiddle(value, max = 54) {
  const text = String(value || '').trim();
  if (!text || text.length <= max) return text;
  const keep = Math.max(10, Math.floor((max - 3) / 2));
  return `${text.slice(0, keep)}...${text.slice(text.length - keep)}`;
}

export function parseDownloadStatusMessage(message) {
  const text = String(message || '').trim();
  const downloading = text.match(/^BAIXANDO\s+(\d+)\/(\d+):\s+(.+)$/i);
  if (downloading) {
    const current = Number(downloading[1] || 0);
    const total = Number(downloading[2] || 0);
    const file = String(downloading[3] || '').trim();
    return {
      text,
      desc: `Baixando arquivo ${current}/${total}`,
      status: `● Arquivo ${current}/${total}: ${shortenPathMiddle(file, 52)}`,
      transferLabel: `${current}/${total} • ${file}`
    };
  }
  return {
    text,
    desc: text,
    status: `● ${text}`,
    transferLabel: ''
  };
}

export function formatUptime(seconds) {
  if (!seconds || seconds <= 0) return '--';
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

export function escHtml(s) {
  return String(s).replace(/[<>&]/g, (c) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;' }[c]));
}
