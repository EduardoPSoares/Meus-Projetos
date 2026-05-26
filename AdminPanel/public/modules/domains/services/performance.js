(function initServicesPerformanceDomain() {
  function createServicesPerformanceDomain(ctx) {
    async function loadPerfStats() {
      try {
        const r = await fetch('/api/stats', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success) return;
        const s = d.stats;
        ctx.$('perf-cpu-val').textContent = s.cpu.toFixed(1) + '%';
        ctx.$('perf-cpu-sub').textContent = s.cpus + ' nucleos';
        const usedGB = (s.memory.used / 1073741824).toFixed(1);
        const totalGB = (s.memory.total / 1073741824).toFixed(1);
        const pct = (s.memory.used / s.memory.total * 100).toFixed(1);
        ctx.$('perf-ram-val').textContent = usedGB + ' / ' + totalGB + ' GB';
        ctx.$('perf-ram-sub').textContent = pct + '% usado';
        const up = Math.floor(s.uptime);
        const h = Math.floor(up / 3600), m = Math.floor((up % 3600) / 60), sec = Math.floor(up % 60);
        ctx.$('perf-uptime-val').textContent = h > 0 ? `${h}h ${m}m` : m > 0 ? `${m}m ${sec}s` : `${sec}s`;
        const si = await (await fetch('/api/serverinfo', { headers: { 'X-Auth-Token': ctx.getToken() } })).json();
        if (si.success) {
          ctx.$('perf-online-val').textContent = si.server.online;
          ctx.$('perf-online-sub').textContent = si.server.players + ' contas';
        }
      } catch {}
    }

    async function loadPerfCharts() {
      try {
        const r = await fetch('/api/stats/history?period=3600', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const d = await r.json();
        if (!d.success || !d.stats || !d.stats.length) return;
        const data = d.stats;
        drawChart('chart-cpu', data, e => e.cpu, 'CPU %', '#4aaa4a', '%');
        drawChart('chart-ram', data, e => Math.round(e.mem.used / 1073741824 * 100) / 100, 'RAM GB', '#4a8aba', ' GB');
        const pr = await fetch('/api/playerhistory?period=3600', { headers: { 'X-Auth-Token': ctx.getToken() } });
        const pd = await pr.json();
        if (pd.success && pd.history && pd.history.length) drawChart('chart-players', pd.history, e => e.online, 'Jogadores', '#c8a01a', '');
      } catch {}
    }

    function drawChart(canvasId, data, getVal, label, color, unit) {
      const canvas = ctx.$(canvasId);
      if (!canvas) return;
      const x = canvas.getContext('2d');
      const W = canvas.width, H = canvas.height;
      const PAD = { t: 18, r: 10, b: 22, l: 36 };
      const cw = W - PAD.l - PAD.r, ch = H - PAD.t - PAD.b;
      x.clearRect(0, 0, W, H);
      if (data.length < 2) { x.fillStyle = '#2a4a2a'; x.font = '10px Share Tech Mono'; x.textAlign = 'center'; x.fillText('Aguardando dados...', W / 2, H / 2); return; }
      const vals = data.map(getVal);
      const min = Math.min(...vals), max = Math.max(...vals);
      const range = max - min || 1;
      const xStep = cw / (data.length - 1);
      x.strokeStyle = '#0f180f'; x.lineWidth = 1;
      for (let i = 0; i < 4; i++) {
        const y = PAD.t + ch * i / 3;
        x.beginPath(); x.moveTo(PAD.l, y); x.lineTo(W - PAD.r, y); x.stroke();
        const val = max - range * i / 3;
        x.fillStyle = '#3a5a3a'; x.font = '9px Share Tech Mono'; x.textAlign = 'right';
        x.fillText(val.toFixed(1), PAD.l - 4, y + 3);
      }
      x.strokeStyle = color; x.lineWidth = 1.5;
      x.beginPath();
      data.forEach((e, i) => {
        const px = PAD.l + i * xStep;
        const py = PAD.t + ch - (getVal(e) - min) / range * ch;
        i === 0 ? x.moveTo(px, py) : x.lineTo(px, py);
      });
      x.stroke();
      x.fillStyle = '#2a4a2a'; x.font = '8px Share Tech Mono'; x.textAlign = 'center';
      const steps = Math.min(6, data.length);
      const stepIdx = Math.floor(data.length / steps);
      for (let i = 0; i < data.length; i += stepIdx) {
        const t = new Date(data[i].time);
        const px = PAD.l + i * xStep;
        x.fillText(t.getHours().toString().padStart(2, '0') + ':' + t.getMinutes().toString().padStart(2, '0'), px, H - 4);
      }
    }

    function stopPerfAutoRefresh() {
      if (ctx.getPerfInterval()) {
        clearInterval(ctx.getPerfInterval());
        ctx.setPerfInterval(null);
      }
    }

    function bindPerformanceTab() {
      document.querySelector('.tab[data-tab="performance"]')?.addEventListener('click', () => {
        loadPerfStats();
        loadPerfCharts();
        stopPerfAutoRefresh();
        ctx.setPerfInterval(setInterval(loadPerfStats, 5000));
      });
    }

    return { loadPerfStats, loadPerfCharts, drawChart, stopPerfAutoRefresh, bindPerformanceTab };
  }

  window.AdminPanelDomains = window.AdminPanelDomains || {};
  window.AdminPanelDomains.services = window.AdminPanelDomains.services || {};
  window.AdminPanelDomains.services.createPerformanceDomain = createServicesPerformanceDomain;
})();
