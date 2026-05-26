(function initLauncherContentDomain() {
  function createLauncherContentDomain(ctx) {
    async function loadLauncherConfig() {
      try {
        const r = await fetch('/api/public/launcher-config');
        const d = await r.json();
        if (d.success) {
          ctx.setLauncherConfig(d.config || { slides: [], news: [] });
          renderHeroPreview();
          renderNewsPreview();
        }
      } catch {}
    }

    function renderHeroPreview() {
      const config = ctx.getLauncherConfig();
      const slides = config.slides || [];
      const dotsContainer = ctx.$('lpr-hero-dots');
      if (!dotsContainer) return;

      if (!slides.length) {
        dotsContainer.innerHTML = '';
        ctx.setLauncherCounterText('0 / 0');
        showSlide(-1);
        return;
      }

      dotsContainer.innerHTML = slides.map((s, i) =>
        `<div class="lpr-dot ${i === 0 ? 'active' : ''}" onclick="selectSlide(${i})"></div>`
      ).join('');

      ctx.setCurrentSlideIdx(0);
      ctx.setLauncherCounterText(`1 / ${slides.length}`);
      showSlide(0);
    }

    function showSlide(idx) {
      const tagEl = ctx.$('lpr-hero-tag');
      const titleEl = ctx.$('lpr-hero-title');
      const descEl = ctx.$('lpr-hero-desc');
      const bgEl = ctx.$('lpr-hero-bg');
      const counterEl = ctx.$('lpr-counter');
      const slides = (ctx.getLauncherConfig().slides || []);

      if (idx < 0 || idx >= slides.length) {
        if (tagEl) tagEl.textContent = '---';
        if (titleEl) titleEl.innerHTML = 'SEM<br><span>SLIDES</span>';
        if (descEl) descEl.textContent = 'Adicione um novo slide';
        if (bgEl) bgEl.style.display = 'none';
        if (counterEl) counterEl.textContent = '0 / 0';
        ctx.setLauncherCounterText('0 / 0');
        document.querySelectorAll('.lpr-dot').forEach(d => d.classList.remove('active'));
        return;
      }

      const slide = slides[idx];
      if (tagEl) tagEl.textContent = slide.tag || '';
      if (titleEl) titleEl.innerHTML = slide.title || 'Slide';
      if (descEl) descEl.textContent = slide.desc || '';
      if (counterEl) counterEl.textContent = `${idx + 1} / ${slides.length}`;
      ctx.setLauncherCounterText(`${idx + 1} / ${slides.length}`);

      if (bgEl && slide.image) {
        bgEl.style.backgroundImage = `url(/launcher-images/${slide.image})`;
        bgEl.style.display = 'block';
      } else if (bgEl) {
        bgEl.style.display = 'none';
      }

      document.querySelectorAll('.lpr-dot').forEach((d, i) => d.classList.toggle('active', i === idx));
    }

    function prevSlide() {
      const slides = (ctx.getLauncherConfig().slides || []);
      if (!slides.length) return;
      const idx = (ctx.getCurrentSlideIdx() - 1 + slides.length) % slides.length;
      selectSlide(idx);
    }

    function nextSlide() {
      const slides = (ctx.getLauncherConfig().slides || []);
      if (!slides.length) return;
      const idx = (ctx.getCurrentSlideIdx() + 1) % slides.length;
      selectSlide(idx);
    }

    function selectSlide(idx) {
      ctx.setCurrentSlideIdx(idx);
      showSlide(idx);
      document.querySelectorAll('.lpr-dot').forEach((d, i) => d.classList.toggle('active', i === idx));

      const editor = ctx.$('slide-editor');
      if (editor) editor.style.display = 'block';
      const slide = (ctx.getLauncherConfig().slides || [])[idx] || {};
      if (ctx.$('slide-edit-tag')) ctx.$('slide-edit-tag').value = slide.tag || '';
      if (ctx.$('slide-edit-title')) ctx.$('slide-edit-title').value = slide.title || '';
      if (ctx.$('slide-edit-desc')) ctx.$('slide-edit-desc').value = slide.desc || '';

      const imgPreview = ctx.$('slide-edit-img-preview');
      if (!imgPreview) return;
      if (slide.image) {
        imgPreview.src = `/launcher-images/${slide.image}`;
        imgPreview.style.display = 'block';
      } else {
        imgPreview.style.display = 'none';
      }
    }

    function syncSlideEditor() {
      const idx = ctx.getCurrentSlideIdx();
      if (idx < 0) return;
      const config = ctx.getLauncherConfig();
      const slide = config.slides && config.slides[idx];
      if (!slide) return;
      slide.tag = ctx.$('slide-edit-tag') ? ctx.$('slide-edit-tag').value : slide.tag;
      slide.title = ctx.$('slide-edit-title') ? ctx.$('slide-edit-title').value : slide.title;
      slide.desc = ctx.$('slide-edit-desc') ? ctx.$('slide-edit-desc').value : slide.desc;
      showSlide(idx);
    }

    function deleteCurrentSlide() {
      const idx = ctx.getCurrentSlideIdx();
      if (idx < 0) return;
      const config = ctx.getLauncherConfig();
      config.slides.splice(idx, 1);
      ctx.setCurrentSlideIdx(-1);
      if (ctx.$('slide-editor')) ctx.$('slide-editor').style.display = 'none';
      renderHeroPreview();
      if (config.slides.length > 0) selectSlide(0);
    }

    async function uploadSlideImage(input) {
      const idx = ctx.getCurrentSlideIdx();
      if (idx < 0) return;
      const file = input && input.files ? input.files[0] : null;
      if (!file) return;
      const reader = new FileReader();
      reader.onload = async (e) => {
        const base64 = String(e.target.result || '').split(',')[1] || '';
        const name = `slide_${Date.now()}`;
        try {
          const r = await fetch('/api/public/launcher-image', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, data: base64 })
          });
          const d = await r.json();
          if (!d.success) {
            alert('Erro ao enviar imagem');
            return;
          }
          const config = ctx.getLauncherConfig();
          if (config.slides && config.slides[idx]) config.slides[idx].image = d.fileName;
          const imgPreview = ctx.$('slide-edit-img-preview');
          if (imgPreview) {
            imgPreview.src = `/launcher-images/${d.fileName}`;
            imgPreview.style.display = 'block';
          }
          showSlide(idx);
        } catch (err) {
          alert('Erro ao enviar imagem: ' + err.message);
        }
      };
      reader.readAsDataURL(file);
    }

    function addSlide() {
      const config = ctx.getLauncherConfig();
      if (!config.slides) config.slides = [];
      config.slides.push({ tag: 'NOVO', title: 'Novo Slide<br><span>DESTAQUE</span>', desc: '// Descricao do slide', image: '' });
      renderHeroPreview();
      selectSlide(config.slides.length - 1);
    }

    function renderNewsPreview() {
      const container = ctx.$('news-preview');
      if (!container) return;
      const config = ctx.getLauncherConfig();
      const newsList = config.news || [];

      if (!newsList.length) {
        container.innerHTML = '<div class="empty-state">Nenhuma noticia</div>';
        if (ctx.$('news-editor')) ctx.$('news-editor').style.display = 'none';
        return;
      }

      container.innerHTML = newsList.map((n, i) => {
        const badgeHtml = n.badge ? `<span class="lpr-news-badge ${n.badge}">${n.badge.toUpperCase()}</span>` : '';
        return `<div class="lpr-news-item ${n.featured ? 'featured' : ''} ${i === ctx.getCurrentNewsIdx() ? 'selected' : ''}" onclick="selectNews(${i})"><span class="lpr-news-date">${ctx.esc(n.date || '')}</span>${badgeHtml}<span class="lpr-news-title">${ctx.esc(n.title || '')}</span></div>`;
      }).join('');
    }

    function selectNews(idx) {
      ctx.setCurrentNewsIdx(idx);
      const editor = ctx.$('news-editor');
      if (editor) editor.style.display = 'block';
      const news = (ctx.getLauncherConfig().news || [])[idx] || {};
      if (ctx.$('news-edit-date')) ctx.$('news-edit-date').value = news.date || '';
      if (ctx.$('news-edit-badge')) ctx.$('news-edit-badge').value = news.badge || '';
      if (ctx.$('news-edit-title')) ctx.$('news-edit-title').value = news.title || '';
      if (ctx.$('news-edit-featured')) ctx.$('news-edit-featured').checked = !!news.featured;
      renderNewsPreview();
    }

    function syncNewsEditor() {
      const idx = ctx.getCurrentNewsIdx();
      if (idx < 0) return;
      const config = ctx.getLauncherConfig();
      const news = config.news && config.news[idx];
      if (!news) return;
      if (ctx.$('news-edit-date')) news.date = ctx.$('news-edit-date').value;
      if (ctx.$('news-edit-badge')) news.badge = ctx.$('news-edit-badge').value;
      if (ctx.$('news-edit-title')) news.title = ctx.$('news-edit-title').value;
      if (ctx.$('news-edit-featured')) news.featured = ctx.$('news-edit-featured').checked;
      renderNewsPreview();
    }

    function deleteCurrentNews() {
      const idx = ctx.getCurrentNewsIdx();
      if (idx < 0) return;
      const config = ctx.getLauncherConfig();
      config.news.splice(idx, 1);
      ctx.setCurrentNewsIdx(-1);
      if (ctx.$('news-editor')) ctx.$('news-editor').style.display = 'none';
      renderNewsPreview();
      if (config.news.length > 0) selectNews(0);
    }

    function addNews() {
      const config = ctx.getLauncherConfig();
      if (!config.news) config.news = [];
      config.news.push({ date: '01 JAN 2026', title: 'Nova noticia', badge: 'new', featured: false });
      renderNewsPreview();
      selectNews(config.news.length - 1);
    }

    return {
      loadLauncherConfig,
      renderHeroPreview,
      showSlide,
      prevSlide,
      nextSlide,
      selectSlide,
      syncSlideEditor,
      deleteCurrentSlide,
      uploadSlideImage,
      addSlide,
      renderNewsPreview,
      selectNews,
      syncNewsEditor,
      deleteCurrentNews,
      addNews
    };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.launcher = window.AdminPanelDomains.launcher || {};
  window.AdminPanelDomains.launcher.createLauncherContentDomain = createLauncherContentDomain;
})();
