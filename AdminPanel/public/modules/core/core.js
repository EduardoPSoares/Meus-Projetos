(function initAdminPanelCore() {
  const state = {
    toastCounter: 0,
    globalServiceActionState: null
  };

  function $(id) {
    return document.getElementById(id);
  }

  function qs(selector, parent) {
    return (parent || document).querySelector(selector);
  }

  function esc(value) {
    return String(value).replace(/[<>&]/g, (c) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;' }[c]));
  }

  function num(value) {
    return (value || 0).toLocaleString('pt-BR');
  }

  function parseStrictIntInput(value) {
    const text = String(value || '').trim();
    if (!text) return null;
    if (!/^-?\d+$/.test(text)) return null;
    const parsed = Number.parseInt(text, 10);
    return Number.isFinite(parsed) ? parsed : null;
  }

  function setStatusTone(el, tone) {
    if (!el) return;
    el.classList.remove('path-status-info', 'path-status-ok', 'path-status-warn', 'path-status-danger');
    el.classList.add(`path-status-${tone || 'info'}`);
  }

  function setBusy(target, busy, label) {
    const el = typeof target === 'string' ? $(target) : target;
    if (!el) return;
    if (busy) {
      if (!el.dataset.idleText) el.dataset.idleText = el.textContent;
      el.disabled = true;
      el.classList.add('is-busy');
      if (label) el.textContent = label;
    } else {
      el.disabled = false;
      el.classList.remove('is-busy');
      if (el.dataset.idleText) el.textContent = el.dataset.idleText;
    }
  }

  function showToast(message, type, timeout) {
    const stack = $('toast-stack');
    if (!stack || !message) return;

    const safeType = type === 'error' ? 'error' : type === 'warn' ? 'warn' : 'success';
    const safeTimeout = Number.isFinite(Number(timeout)) ? Number(timeout) : 3200;

    const id = `toast-${Date.now()}-${state.toastCounter++}`;
    const el = document.createElement('div');
    el.className = `toast toast-${safeType}`;
    el.id = id;
    el.textContent = message;
    stack.appendChild(el);

    requestAnimationFrame(() => el.classList.add('show'));
    setTimeout(() => {
      el.classList.remove('show');
      setTimeout(() => el.remove(), 220);
    }, safeTimeout);
  }

  function formatDurationMs(ms) {
    const n = Number(ms || 0);
    if (!Number.isFinite(n) || n <= 0) return '--';
    const total = Math.floor(n / 1000);
    const h = Math.floor(total / 3600);
    const m = Math.floor((total % 3600) / 60);
    const s = total % 60;
    if (h > 0) return `${h}h ${m}m`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
  }

  window.AdminPanelCore = {
    state,
    $, qs, esc, num,
    parseStrictIntInput,
    setStatusTone,
    setBusy,
    showToast,
    formatDurationMs
  };
})();
