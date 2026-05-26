import { createServerStatusModule } from './content-server-status.js';
import { createHeroSlidesModule } from './content-hero.js';
import { createNewsModule } from './content-news.js';

export function createContentModule(ctx) {
  const { ipcRenderer, state } = ctx;

  const serverStatus = createServerStatusModule(ctx);
  const heroSlides = createHeroSlidesModule(ctx);
  const news = createNewsModule(ctx);

  async function fetchLauncherConfig(renderHeroSlides, renderNews) {
    try {
      if (state.runtimeConfig && state.runtimeConfig.launcherUi && typeof state.runtimeConfig.launcherUi === 'object') {
        state.launcherConfig = state.runtimeConfig.launcherUi;
        renderHeroSlides();
        renderNews();
        return;
      }
      const result = await ipcRenderer.invoke('fetch-launcher-config');
      if (result && result.success && result.config) {
        state.launcherConfig = result.config;
        renderHeroSlides();
        renderNews();
      }
    } catch (error) {
      console.error('Error fetching launcher config:', error);
    }
  }

  return {
    checkServerStatus: serverStatus.checkServerStatus,
    applyServerStatusUpdate: serverStatus.applyServerStatusUpdate,
    fetchLauncherConfig,
    showSlide: heroSlides.showSlide,
    renderHeroSlides: heroSlides.renderHeroSlides,
    renderNews: news.renderNews
  };
}
