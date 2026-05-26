import { escHtml } from './helpers.js';

export function createNewsModule(ctx) {
  const { state } = ctx;

  function renderNews() {
    const newsList = (state.launcherConfig?.news || []).slice(0, 3);
    const container = document.getElementById('news-container');
    if (!container) return;

    if (!newsList.length) {
      container.innerHTML = '';
      return;
    }

    container.innerHTML = newsList.map((n) => {
      const badgeHtml = n.badge ? `<span class="news-badge badge-${n.badge}">${n.badge.toUpperCase()}</span>` : '';
      return `
      <div class="news-item ${n.featured ? 'featured' : ''}">
        <div class="news-date">${escHtml(n.date || '')}</div>
        <div class="news-title">${badgeHtml} ${escHtml(n.title || '')}</div>
      </div>
    `;
    }).join('');
  }

  return {
    renderNews
  };
}
