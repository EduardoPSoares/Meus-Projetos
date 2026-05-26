export function createHeroSlidesModule(ctx) {
  const { state } = ctx;

  function showSlide(index) {
    const slides = state.launcherConfig?.slides || [];
    if (index < 0 || index >= slides.length) return;
    state.currentSlide = index;
    const slide = slides[index];

    const tagEl = document.getElementById('hero-tag');
    const titleEl = document.getElementById('hero-title');
    const descEl = document.getElementById('hero-desc');
    const bgEl = document.getElementById('slide-bg');

    if (tagEl) tagEl.textContent = slide.tag || '';
    if (titleEl) titleEl.innerHTML = slide.title || '';
    if (descEl) descEl.textContent = slide.desc || '';

    document.querySelectorAll('.ndot').forEach((dot, i) => {
      dot.classList.toggle('active', i === index);
    });

    if (bgEl && slide.image) {
      bgEl.style.backgroundImage = `url(launcher-images/${slide.image})`;
      bgEl.style.backgroundSize = 'cover';
      bgEl.style.backgroundPosition = 'center';
      bgEl.style.display = 'block';
    } else if (bgEl) {
      bgEl.style.backgroundImage = '';
      bgEl.style.display = 'none';
    }
  }

  function renderHeroSlides() {
    const slides = state.launcherConfig?.slides || [];
    if (!slides.length) return;

    showSlide(0);

    const dotsContainer = document.getElementById('news-dots');
    if (!dotsContainer) return;
    dotsContainer.innerHTML = slides.map((s, i) =>
      `<div class="ndot ${i === 0 ? 'active' : ''}" data-index="${i}"></div>`
    ).join('');

    dotsContainer.querySelectorAll('.ndot').forEach((dot) => {
      dot.addEventListener('click', () => {
        const index = parseInt(dot.dataset.index, 10);
        showSlide(index);
      });
    });
  }

  return {
    showSlide,
    renderHeroSlides
  };
}
