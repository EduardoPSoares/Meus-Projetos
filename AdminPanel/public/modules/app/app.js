let token = 'local-no-auth';

function updateAdminToken(nextToken, persist = true) {
  const normalized = String(nextToken || '').trim();
  token = normalized || 'local-no-auth';
  if (!persist) return;
  try {
    if (normalized) localStorage.setItem('admin_auth_token', normalized);
    else localStorage.removeItem('admin_auth_token');
  } catch {}
}

window.setAdminToken = function setAdminTokenFromConsole(nextToken) { updateAdminToken(nextToken, true); return token; };

(function bootstrapAdminAuthToken() {
  try {
    const qp = new URLSearchParams(window.location.search);
    const queryToken = String(qp.get('token') || '').trim();
    const storedToken = String(localStorage.getItem('admin_auth_token') || '').trim();
    if (queryToken) {
      updateAdminToken(queryToken, true);
      return;
    }
    if (storedToken) {
      updateAdminToken(storedToken, false);
      return;
    }
  } catch {}
  updateAdminToken('local-no-auth', false);
})();

(function patchApiFetchAuthHeader() {
  if (!window.fetch || typeof window.fetch !== 'function') return;
  const originalFetch = window.fetch.bind(window);
  window.fetch = function patchedFetch(input, init) {
    const requestInit = init ? { ...init } : {};
    const rawUrl = typeof input === 'string' ? input : (input && input.url ? String(input.url) : '');
    const isApiRoute = rawUrl.startsWith('/api/') || rawUrl.includes('/api/');
    const isPublicApiRoute = rawUrl.startsWith('/api/public/');
    if (isApiRoute && !isPublicApiRoute) {
      const headers = new Headers(requestInit.headers || (input && input.headers) || undefined);
      if (!headers.has('X-Auth-Token') && !headers.has('x-auth-token')) {
        headers.set('X-Auth-Token', token);
      }
      requestInit.headers = headers;
    }
    return originalFetch(input, requestInit);
  };
})();;
const DEFAULT_PANEL_MODEL = Object.freeze({
  nick: { minLen: 3, maxLen: 24 },
  command: {
    limits: {
      addcry: { perCmd: 100000000, minTotal: 0, maxTotal: 2147483647 },
      addcrown: { perCmd: 100000000, minTotal: 0, maxTotal: 2147483647 },
      addvp: { perCmd: 100000000, minTotal: 0, maxTotal: 2147483647 },
      addxp: { perCmd: 10000000, minTotal: 0, maxTotal: 2147483647 },
      addgm: { minRank: 1, maxRank: 90 }
    }
  },
  item: {
    minLen: 2,
    maxLen: 80,
    quantity: { min: 1, max: 999 },
    expirationHours: { min: 0, max: 8760 }
  },
  achievement: { progress: { min: 0, max: 1000000 } },
  xp: { multiplier: { min: 1, max: 9999 } }
});

const SHOP_PAGE_SIZE = 50;
const SHOP_CATALOG_PAGE_SIZE = 60;

let monitorInterval = null;
let logStream = null;

const $ = id => document.getElementById(id);
const qs = (s, p) => (p||document).querySelector(s);

let selectedCmd = 'addcry';
let panelModel = null;
let toastCounter = 0;
let patchSelectionMode = 'files';
let publishProgressTimer = null;
let launcherPublishProgressTimer = null;
let gameSyncProgressTimer = null;
let launcherSyncProgressTimer = null;
let xpEventUiTimer = null;
let xpEventSyncTimer = null;
let itemHoverPreviewEl = null;
let xpEventState = {
  active: false,
  multiplier: 1,
  message: '',
  startedAt: null,
  expiresAt: null,
  temporary: false
};
let panelRuntimeState = {
  server: null,
  services: null,
  recentLog: null
};
let globalServiceActionState = null;
let loadedConfigObject = null;
const uiBaseDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.ui
  && typeof window.AdminPanelDomains.ui.createUiBaseDomain === 'function'
  ? window.AdminPanelDomains.ui.createUiBaseDomain({
    $,
    qs,
    switchTab: (tab) => switchTab(tab),
    getToastCounter: () => toastCounter,
    setToastCounter: (value) => { toastCounter = value; },
    getGlobalServiceActionState: () => globalServiceActionState,
    setGlobalServiceActionState: (value) => { globalServiceActionState = value; },
    getPanelRuntimeState: () => panelRuntimeState,
    setPanelRuntimeState: (value) => { panelRuntimeState = value; }
  })
  : null;
const servicesAdminOpsDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.services
  && typeof window.AdminPanelDomains.services.createAdminOpsDomain === 'function'
  ? window.AdminPanelDomains.services.createAdminOpsDomain({
    $,
    esc: (v) => esc(v),
    getToken: () => token,
    setBusy: (t,b,l) => setBusy(t,b,l),
    showToast: (m,t) => showToast(m,t),
    showResult: (m,e) => showResult(m,e),
    parseStrictIntInput: (v) => parseStrictIntInput(v),
    getPanelModel: () => getPanelModel(),
    DEFAULT_PANEL_MODEL,
    updateXpEventStatusUi: () => updateXpEventStatusUi(),
    getMaintenanceEnabled: () => maintenanceEnabled,
    setMaintenanceEnabled: (v) => { maintenanceEnabled = v; },
    getXpEventState: () => xpEventState,
    setXpEventState: (v) => { xpEventState = v; },
    getAutoBroadcastEnabled: () => autoBroadcastEnabled,
    setAutoBroadcastEnabled: (v) => { autoBroadcastEnabled = v; },
    loadAchievementLists: () => loadAchievementLists()
  })
  : null;
const servicesConfigDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.services
  && typeof window.AdminPanelDomains.services.createConfigDomain === 'function'
  ? window.AdminPanelDomains.services.createConfigDomain({
    $,
    getToken: () => token,
    getLoadedConfigObject: () => loadedConfigObject,
    setLoadedConfigObject: (v) => { loadedConfigObject = v; }
  })
  : null;
const servicesAntiCheatDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.services
  && typeof window.AdminPanelDomains.services.createAntiCheatDomain === 'function'
  ? window.AdminPanelDomains.services.createAntiCheatDomain({
    $,
    getToken: () => token,
    showResult: (m,e) => showResult(m,e)
  })
  : null;
const servicesPerformanceDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.services
  && typeof window.AdminPanelDomains.services.createPerformanceDomain === 'function'
  ? window.AdminPanelDomains.services.createPerformanceDomain({
    $,
    getToken: () => token,
    getPerfInterval: () => perfInterval,
    setPerfInterval: (v) => { perfInterval = v; }
  })
  : null;
const servicesMonitoringDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.services
  && typeof window.AdminPanelDomains.services.createServicesMonitoringDomain === 'function'
  ? window.AdminPanelDomains.services.createServicesMonitoringDomain({
    $,
    esc,
    setText,
    setTone,
    setBusy,
    showToast,
    setGlobalServiceAction,
    updatePanelAlerts,
    updateServiceSummary,
    updateGlobalServiceActionProgress,
    markServiceCardPending,
    fetchPlayers,
    getToken: () => token,
    getMonitorInterval: () => monitorInterval,
    setMonitorInterval: (value) => { monitorInterval = value; },
    getLogStream: () => logStream,
    setLogStream: (value) => { logStream = value; },
    getPanelRuntimeState: () => panelRuntimeState,
    setPanelRuntimeState: (value) => { panelRuntimeState = value; }
  })
  : null;
const servicesRuntimeConfigDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.services
  && typeof window.AdminPanelDomains.services.createRuntimeConfigDomain === 'function'
  ? window.AdminPanelDomains.services.createRuntimeConfigDomain({
    $,
    getToken: () => token
  })
  : null;
const playersRoomsDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.players
  && typeof window.AdminPanelDomains.players.createPlayersRoomsDomain === 'function'
  ? window.AdminPanelDomains.players.createPlayersRoomsDomain({
    $,
    esc: (v) => esc(v),
    getToken: () => token,
    getGameroomInterval: () => gameroomInterval,
    setGameroomInterval: (v) => { gameroomInterval = v; }
  })
  : null;
const playersCommandsDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.players
  && typeof window.AdminPanelDomains.players.createPlayersCommandsDomain === 'function'
  ? window.AdminPanelDomains.players.createPlayersCommandsDomain({
    $,
    num: (value) => num(value),
    showResult: (msg, isError) => showResult(msg, isError),
    parseStrictIntInput: (value) => parseStrictIntInput(value),
    getToken: () => token,
    getSelectedCmd: () => selectedCmd,
    getPanelModel: () => getPanelModel(),
    getCommandLimit: (cmd) => getCommandLimit(cmd),
    DEFAULT_PANEL_MODEL,
    getLookupTimer: () => lookupTimer,
    setLookupTimer: (value) => { lookupTimer = value; }
  })
  : null;
const playersModerationDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.players
  && typeof window.AdminPanelDomains.players.createPlayersModerationDomain === 'function'
  ? window.AdminPanelDomains.players.createPlayersModerationDomain({
    $,
    esc: (v) => esc(v),
    num: (v) => num(v),
    getToken: () => token,
    getChatLogsInterval: () => chatLogsInterval,
    setChatLogsInterval: (v) => { chatLogsInterval = v; }
  })
  : null;
const playersInspectDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.players
  && typeof window.AdminPanelDomains.players.createPlayersInspectDomain === 'function'
  ? window.AdminPanelDomains.players.createPlayersInspectDomain({
    $,
    esc: (v) => esc(v),
    getToken: () => token,
    getItemNames: () => itemNames,
    setItemNames: (v) => { itemNames = v; },
    loadNotes: () => loadNotes()
  })
  : null;
const playersItemOpsDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.players
  && typeof window.AdminPanelDomains.players.createPlayersItemOpsDomain === 'function'
  ? window.AdminPanelDomains.players.createPlayersItemOpsDomain({
    $,
    esc: (v) => esc(v),
    getToken: () => token,
    parseStrictIntInput: (v) => parseStrictIntInput(v),
    getPanelModel: () => getPanelModel(),
    DEFAULT_PANEL_MODEL,
    loadItemNames: () => loadItemNames(),
    getItemNames: () => itemNames,
    setItemNames: (v) => { itemNames = v; },
    getItemSuggestionsLoaded: () => itemSuggestionsLoaded,
    setItemSuggestionsLoaded: (v) => { itemSuggestionsLoaded = v; },
    weaponVisualImage: (k) => weaponVisualImage(k),
    weaponVisualTitle: (k, f) => weaponVisualTitle(k, f),
    enrichWeaponVisuals: (items, noNetwork, maxNetwork) => enrichWeaponVisuals(items, noNetwork, maxNetwork)
  })
  : null;
const playersAchievementsDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.players
  && typeof window.AdminPanelDomains.players.createPlayersAchievementsDomain === 'function'
  ? window.AdminPanelDomains.players.createPlayersAchievementsDomain({
    $,
    esc: (v) => esc(v),
    showToast: (m,t) => showToast(m,t),
    confirmDanger: (m) => confirmDanger(m),
    parseStrictIntInput: (v) => parseStrictIntInput(v),
    getPanelModel: () => getPanelModel(),
    DEFAULT_PANEL_MODEL,
    getToken: () => token,
    getAchievementImageMap: () => achievementImageMap,
    setAchievementImageMap: (v) => { achievementImageMap = v; },
    getAchievementBadgePool: () => achievementBadgePool,
    setAchievementBadgePool: (v) => { achievementBadgePool = v; },
    getAchCatalogOffset: () => achCatalogOffset,
    setAchCatalogOffset: (v) => { achCatalogOffset = v; },
    getAchCatalogLimit: () => achCatalogLimit,
    setAchCatalogLimit: (v) => { achCatalogLimit = v; },
    getAchCatalogTotal: () => achCatalogTotal,
    setAchCatalogTotal: (v) => { achCatalogTotal = v; }
  })
  : null;
const shopOffersDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.shop
  && typeof window.AdminPanelDomains.shop.createShopOffersDomain === 'function'
  ? window.AdminPanelDomains.shop.createShopOffersDomain({
    $,
    esc: (v) => esc(v),
    setBusy: (target, busy, label) => setBusy(target, busy, label),
    renderSkeleton: (id, count) => renderSkeleton(id, count),
    showToast: (msg, type) => showToast(msg, type),
    getToken: () => token,
    SHOP_PAGE_SIZE,
    SHOP_CATALOG_PAGE_SIZE,
    getShopStatusFilter: () => shopStatusFilter,
    getShopSortMode: () => shopSortMode,
    getShopPageOffset: () => shopPageOffset,
    setShopPageOffset: (v) => { shopPageOffset = v; },
    getShopCatalogOffset: () => shopCatalogOffset,
    setShopCatalogOffset: (v) => { shopCatalogOffset = v; },
    getShopTotalOffers: () => shopTotalOffers,
    setShopTotalOffers: (v) => { shopTotalOffers = v; },
    setShopLastPageOffers: (v) => { shopLastPageOffers = v; },
    setShopSelectedOfferId: (v) => { shopSelectedOfferId = v; },
    getShopBuilderOffers: () => getShopBuilderOffers(),
    getItemNames: () => getItemNames(),
    normalizeItemBaseKey: (v) => normalizeItemBaseKey(v),
    enrichWeaponVisuals: (items, noNetwork, maxNetwork) => enrichWeaponVisuals(items, noNetwork, maxNetwork),
    renderShopOfferList: () => renderShopOfferList(),
    renderShopCatalog: (items) => renderShopCatalog(items),
    setShopCatalogTotal: (v) => { shopCatalogTotal = v; }
  })
  : null;
const shopPackagesDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.shop
  && typeof window.AdminPanelDomains.shop.createShopPackagesDomain === 'function'
  ? window.AdminPanelDomains.shop.createShopPackagesDomain({
    $,
    esc: (v) => esc(v),
    showToast: (msg, type) => showToast(msg, type),
    getToken: () => token,
    parseStrictIntInput: (v) => parseStrictIntInput(v),
    confirmDanger: (msg) => confirmDanger(msg),
    loadShopOffers: (reset) => loadShopOffers(reset),
    getShopPackagesCache: () => shopPackagesCache,
    setShopPackagesCache: (v) => { shopPackagesCache = v; },
    getShopRotationCache: () => shopRotationCache,
    setShopRotationCache: (v) => { shopRotationCache = v; },
    getShopPendingSelectPackageId: () => shopPendingSelectPackageId,
    setShopPendingSelectPackageId: (v) => { shopPendingSelectPackageId = v; },
    getShopBuilderOffers: () => getShopBuilderOffers(),
    setShopBuilderOffers: (offers) => setShopBuilderOffers(offers)
  })
  : null;
const rewardsSurvivalDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.rewards
  && typeof window.AdminPanelDomains.rewards.createRewardsSurvivalDomain === 'function'
  ? window.AdminPanelDomains.rewards.createRewardsSurvivalDomain({
    $,
    esc: (v) => esc(v),
    num: (v) => num(v),
    showToast: (m, t) => showToast(m, t),
    setBusy: (target, busy, label) => setBusy(target, busy, label),
    renderSkeleton: (id, count) => renderSkeleton(id, count),
    getToken: () => token,
    getCurrentSurvivalRow: () => getCurrentSurvivalRow(),
    getSurvivalRewardsCache: () => survivalRewardsCache,
    setSurvivalRewardsCache: (v) => { survivalRewardsCache = v; },
    getSurvivalRewardsConfig: () => survivalRewardsConfig,
    setSurvivalRewardsConfig: (v) => { survivalRewardsConfig = v; },
    renderSurvivalRewardItemsDatalist: (items) => renderSurvivalRewardItemsDatalist(items),
    survivalMissionDisplayName: (row) => survivalMissionDisplayName(row),
    survivalMissionDifficulty: (row) => survivalMissionDifficulty(row),
    survivalMissionImage: (row) => survivalMissionImage(row),
    survivalRewardItems: (row) => survivalRewardItems(row),
    survivalRewardItemDisplay: (item) => survivalRewardItemDisplay(item),
    survivalRewardMoney: (row, c) => survivalRewardMoney(row, c),
    survivalEscAttr: (v) => survivalEscAttr(v),
    survivalRewardItemImage: (item) => survivalRewardItemImage(item),
    renderSurvivalMissionPreview: (row, draft) => renderSurvivalMissionPreview(row, draft),
    useSurvivalRewardAsDraft: (idx) => useSurvivalRewardAsDraft(idx)
  })
  : null;
const rewardsSurvivalActionsDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.rewards
  && typeof window.AdminPanelDomains.rewards.createRewardsSurvivalActionsDomain === 'function'
  ? window.AdminPanelDomains.rewards.createRewardsSurvivalActionsDomain({
    $,
    showToast: (m, t) => showToast(m, t),
    setBusy: (target, busy, label) => setBusy(target, busy, label),
    confirmDanger: (m) => confirmDanger(m),
    getToken: () => token,
    loadSurvivalRewards: (force) => loadSurvivalRewards(force),
    setSurvivalResult: (m, err) => setSurvivalResult(m, err),
    getSurvivalRewardsCache: () => survivalRewardsCache,
    useSurvivalRewardAsDraft: (idx) => useSurvivalRewardAsDraft(idx),
    getSurvivalRewardItemsDirty: () => survivalRewardItemsDirty,
    getSurvivalSelectedRewardItems: () => survivalSelectedRewardItems,
    getGlobalRewardPanelOpen: () => globalRewardPanelOpen,
    setGlobalRewardPanelOpen: (v) => { globalRewardPanelOpen = v; }
  })
  : null;
const launcherCdnDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.launcher
  && typeof window.AdminPanelDomains.launcher.createLauncherCdnDomain === 'function'
  ? window.AdminPanelDomains.launcher.createLauncherCdnDomain({
    $,
    getToken: () => token,
    setStatusTone: (el, tone) => setStatusTone(el, tone),
    loadPatchHistory: () => loadPatchHistory(),
    loadLauncherPatchHistory: () => loadLauncherPatchHistory(),
    loadGameRefInfo: () => loadGameRefInfo(),
    getLauncherSyncProgressTimer: () => launcherSyncProgressTimer,
    setLauncherSyncProgressTimer: (v) => { launcherSyncProgressTimer = v; },
    getGameSyncProgressTimer: () => gameSyncProgressTimer,
    setGameSyncProgressTimer: (v) => { gameSyncProgressTimer = v; }
  })
  : null;
const launcherContentDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.launcher
  && typeof window.AdminPanelDomains.launcher.createLauncherContentDomain === 'function'
  ? window.AdminPanelDomains.launcher.createLauncherContentDomain({
    $,
    esc: (v) => esc(v),
    getLauncherConfig: () => launcherConfig,
    setLauncherConfig: (v) => { launcherConfig = v; },
    getCurrentSlideIdx: () => currentSlideIdx,
    setCurrentSlideIdx: (v) => { currentSlideIdx = v; },
    getCurrentNewsIdx: () => currentNewsIdx,
    setCurrentNewsIdx: (v) => { currentNewsIdx = v; },
    setLauncherCounterText: (text) => setLauncherCounterText(text)
  })
  : null;
const launcherSyncDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.launcher
  && typeof window.AdminPanelDomains.launcher.createLauncherSyncDomain === 'function'
  ? window.AdminPanelDomains.launcher.createLauncherSyncDomain({
    $,
    getToken: () => token,
    setBusy: (t,b,l) => setBusy(t,b,l),
    setStatusTone: (el,tone) => setStatusTone(el,tone),
    showToast: (m,t) => showToast(m,t),
    showPatchResult: (m,e) => showPatchResult(m,e),
    showLauncherVersionResult: (m,e) => showLauncherVersionResult(m,e),
    startDevSyncProgressPolling: (kind) => startDevSyncProgressPolling(kind),
    pollDevSyncProgress: (kind,force) => pollDevSyncProgress(kind,force),
    clearDevSyncProgressTimer: (kind) => clearDevSyncProgressTimer(kind),
    loadLauncherRefInfo: () => loadLauncherRefInfo()
  })
  : null;
const launcherPublishDomain = window.AdminPanelDomains
  && window.AdminPanelDomains.launcher
  && typeof window.AdminPanelDomains.launcher.createLauncherPublishDomain === 'function'
  ? window.AdminPanelDomains.launcher.createLauncherPublishDomain({
    $,
    getToken: () => token,
    setBusy: (t,b,l) => setBusy(t,b,l),
    showToast: (m,t) => showToast(m,t),
    formatPatchSize: (v) => formatPatchSize(v),
    showPatchResult: (m,e) => showPatchResult(m,e),
    getPatchSelectionMode: () => patchSelectionMode,
    setPatchSelectionMode: (v) => { patchSelectionMode = v; },
    getPublishProgressTimer: () => publishProgressTimer,
    setPublishProgressTimer: (v) => { publishProgressTimer = v; },
    getLauncherPublishProgressTimer: () => launcherPublishProgressTimer,
    setLauncherPublishProgressTimer: (v) => { launcherPublishProgressTimer = v; },
    loadGameRefInfo: () => loadGameRefInfo(),
    loadVersions: () => loadVersions(),
    loadPatchHistory: () => loadPatchHistory(),
    getLauncherConfig: () => launcherConfig,
    getLauncherResultEl: () => getLauncherResultEl(),
    loadLauncherVersions: () => loadLauncherVersions(),
    loadLauncherPatchHistory: () => loadLauncherPatchHistory(),
    loadLauncherRefInfo: () => loadLauncherRefInfo()
  })
  : null;

function showToast(message, type = 'success', timeout = 3200) {
  if (uiBaseDomain) return uiBaseDomain.showToast(message, type, timeout);
  const stack = $('toast-stack');
  if (!stack || !message) return;
  const id = `toast-${Date.now()}-${toastCounter++}`;
  const el = document.createElement('div');
  el.className = `toast toast-${type === 'error' ? 'error' : type === 'warn' ? 'warn' : 'success'}`;
  el.id = id;
  el.textContent = message;
  stack.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  setTimeout(() => {
    el.classList.remove('show');
    setTimeout(() => el.remove(), 220);
  }, timeout);
}

function setStatusTone(el, tone) {
  if (uiBaseDomain) return uiBaseDomain.setStatusTone(el, tone);
  if (!el) return;
  el.classList.remove('path-status-info', 'path-status-ok', 'path-status-warn', 'path-status-danger');
  el.classList.add(`path-status-${tone || 'info'}`);
}

function setBusy(target, busy, label) {
  if (uiBaseDomain) return uiBaseDomain.setBusy(target, busy, label);
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

function setGlobalActionBusy(actionName, busy, label) {
  if (uiBaseDomain) return uiBaseDomain.setGlobalActionBusy(actionName, busy, label);
  document.querySelectorAll(`[data-global-action="${actionName}"]`).forEach(el => setBusy(el, busy, label));
}

function markServiceCardPending(id, action) {
  if (uiBaseDomain) return uiBaseDomain.markServiceCardPending(id, action);
  const card = document.querySelector(`.service-card[data-service-id="${id}"]`);
  if (!card) return;
  card.classList.add('service-pending');
  card.dataset.pendingAction = action;
  const status = card.querySelector('.sc-status');
  if (status) {
    status.textContent = action === 'stop' ? 'STOPPING' : action === 'restart' ? 'RESTARTING' : 'STARTING';
    status.classList.remove('running', 'stopped');
    status.classList.add('pending');
  }
}

function setGlobalActionFeedback(message, tone) {
  if (uiBaseDomain) return uiBaseDomain.setGlobalActionFeedback(message, tone);
  const el = $('global-action-feedback');
  if (!el) return;
  if (!message) {
    el.textContent = '';
    el.classList.add('hidden');
    el.classList.remove('running', 'success', 'warn');
    return;
  }
  el.textContent = message;
  el.classList.remove('hidden', 'running', 'success', 'warn');
  el.classList.add(tone || 'running');
}

function setGlobalServiceAction(type) {
  if (uiBaseDomain) return uiBaseDomain.setGlobalServiceAction(type);
  if (!type) {
    globalServiceActionState = null;
    setGlobalActionBusy('start-all', false);
    setGlobalActionBusy('stop-all', false);
    setGlobalActionFeedback('');
    document.body.classList.remove('global-service-pending');
    return;
  }

  globalServiceActionState = { type, startedAt: Date.now(), doneNotified: false };
  const starting = type === 'start';
  setGlobalActionBusy('start-all', true, starting ? 'INICIANDO...' : 'INICIAR TUDO');
  setGlobalActionBusy('stop-all', true, starting ? 'PARAR TUDO' : 'PARANDO...');
  setGlobalActionFeedback(starting ? 'Inicializando servicos...' : 'Parando servicos...', 'running');
  document.body.classList.add('global-service-pending');
}

function updateGlobalServiceActionProgress(services) {
  if (uiBaseDomain) return uiBaseDomain.updateGlobalServiceActionProgress(services);
  if (!globalServiceActionState || !services) return;
  const allAvailable = Object.values(services).filter(s => s.available !== false);
  const values = globalServiceActionState.type === 'start'
    ? allAvailable.filter(s => !s.onDemand)
    : allAvailable;
  const total = values.length;
  const ready = values.filter(s => !!s.ready).length;

  if (globalServiceActionState.type === 'start') {
    setGlobalActionFeedback(`Inicializando servicos... ${ready}/${total} prontos`, 'running');
    if (total > 0 && ready === total) {
      setGlobalActionFeedback('Todos os servicos estao prontos.', 'success');
      if (!globalServiceActionState.doneNotified) {
        globalServiceActionState.doneNotified = true;
        showToast('Todos os servicos estao online', 'success', 3000);
      }
      setTimeout(() => setGlobalServiceAction(null), 1400);
    }
    return;
  }

  const stillRunning = ready;
  setGlobalActionFeedback(`Parando servicos... ${stillRunning}/${total} ainda ativos`, 'warn');
  if (stillRunning === 0) {
    setGlobalActionFeedback('Todos os servicos foram parados.', 'success');
    if (!globalServiceActionState.doneNotified) {
      globalServiceActionState.doneNotified = true;
      showToast('Todos os servicos foram parados', 'warn', 3000);
    }
    setTimeout(() => setGlobalServiceAction(null), 1400);
  }
}

function renderPanelState(activeTab) {
  if (uiBaseDomain) return uiBaseDomain.renderPanelState(activeTab);
  document.querySelectorAll('.nav-group').forEach(group => {
    group.classList.toggle('active-group', !!group.querySelector(`.tab[data-tab="${activeTab}"]`));
  });
  const active = document.querySelector(`.tab[data-tab="${activeTab}"]`);
  const pageName = active ? active.textContent.trim() : activeTab;
  document.body.dataset.activeTab = activeTab || 'overview';
  document.querySelectorAll('.tab-content').forEach(panel => {
    if (panel.classList.contains('active')) panel.dataset.pageTitle = pageName;
  });
}

function initDesktopUx() {
  if (uiBaseDomain) return uiBaseDomain.initDesktopUx();
  document.querySelectorAll('.nav-group-label').forEach(label => {
    label.setAttribute('role', 'button');
    label.setAttribute('tabindex', '0');
    const toggle = () => {
      const group = label.closest('.nav-group');
      if (!group || group.classList.contains('active-group')) return;
      group.classList.toggle('collapsed');
    };
    label.addEventListener('click', toggle);
    label.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        toggle();
      }
    });
  });

  const search = $('nav-search');
  if (search) {
    search.addEventListener('input', () => filterNavigation(search.value));
    search.addEventListener('keydown', e => {
      if (e.key === 'Enter') {
        const first = document.querySelector('.tab.nav-match, .tab:not(.nav-filtered)');
        if (first && first.dataset.tab) {
          switchTab(first.dataset.tab);
          search.blur();
        }
      } else if (e.key === 'Escape') {
        search.value = '';
        filterNavigation('');
        search.blur();
      }
    });
  }

  window.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      const el = $('nav-search');
      if (el) {
        e.preventDefault();
        el.focus();
        el.select();
      }
    }
  });
}

function filterNavigation(raw) {
  if (uiBaseDomain) return uiBaseDomain.filterNavigation(raw);
  const query = String(raw || '').trim().toLowerCase();
  document.querySelectorAll('.nav-group').forEach(group => {
    let hits = 0;
    group.querySelectorAll('.tab').forEach(tab => {
      const text = tab.textContent.trim().toLowerCase();
      const match = !query || text.includes(query) || String(tab.dataset.tab || '').includes(query);
      tab.classList.toggle('nav-filtered', !match);
      tab.classList.toggle('nav-match', !!query && match);
      if (match) hits++;
    });
    group.classList.toggle('nav-group-filtered', !!query && hits === 0);
    if (query && hits > 0) group.classList.remove('collapsed');
  });
}

function setText(id, value) {
  if (uiBaseDomain) return uiBaseDomain.setText(id, value);
  const el = $(id);
  if (el) el.textContent = value;
}

function setTone(id, tone) {
  if (uiBaseDomain) return uiBaseDomain.setTone(id, tone);
  const el = $(id);
  if (!el) return;
  el.classList.remove('tone-ok', 'tone-warn', 'tone-bad', 'tone-info');
  el.classList.add(`tone-${tone || 'info'}`);
}

function updatePanelAlerts() {
  if (uiBaseDomain) return uiBaseDomain.updatePanelAlerts();
  const wrap = $('panel-alerts');
  if (!wrap) return;
  const alerts = [];
  const server = panelRuntimeState.server;
  const services = panelRuntimeState.services;

  if (server) {
    if (server.status !== 'online') alerts.push({ tone: 'bad', text: 'Servidor principal offline.' });
    if (server.database !== 'connected') alerts.push({ tone: 'bad', text: 'MongoDB/API nao esta conectado.' });
  }

  if (services) {
    const values = Object.values(services);
    const missing = values.filter(s => s.available === false);
    const stopped = values.filter(s => s.available !== false && !s.ready && !s.onDemand);
    if (missing.length) alerts.push({ tone: 'bad', text: `${missing.length} servico(s) com arquivo/caminho ausente.` });
    if (stopped.length) alerts.push({ tone: 'warn', text: `${stopped.length} servico(s) parados ou nao prontos.` });
  }

  if (panelRuntimeState.recentLog && panelRuntimeState.recentLog.level === 'error') {
    alerts.push({ tone: 'bad', text: `Erro recente: ${String(panelRuntimeState.recentLog.msg || '').slice(0, 110)}` });
  }

  if (!alerts.length) alerts.push({ tone: 'ok', text: 'Nenhum alerta critico no momento.' });
  wrap.innerHTML = alerts.slice(0, 4).map(a => `<div class="panel-alert ${a.tone}">${esc(a.text)}</div>`).join('');
}

function updateServiceSummary(services) {
  if (uiBaseDomain) return uiBaseDomain.updateServiceSummary(services);
  panelRuntimeState.services = services || {};
  const values = Object.values(panelRuntimeState.services);
  const coreValues = values.filter(s => s.available !== false && !s.onDemand);
  const total = coreValues.length;
  const ready = coreValues.filter(s => s.ready).length;
  const missing = values.filter(s => s.available === false).length;
  const stopped = coreValues.filter(s => !s.ready).length;
  setText('ov-services-status', total ? `${ready}/${total}` : '---');
  setText('ov-services-sub', total ? `${stopped} parados / ${missing} ausentes` : 'Sem dados');
  setTone('ov-services-status', missing || stopped ? 'warn' : 'ok');

  const pve = panelRuntimeState.services.dedicated_pve;
  const pvp = panelRuntimeState.services.dedicated_pvp;
  const dedicatedReady = [pve, pvp].filter(s => s && s.ready).length;
  const dedicatedTotal = [pve, pvp].filter(Boolean).length;
  if (dedicatedTotal > 0) {
    setText('ov-dedicated-status', `${dedicatedReady}/${dedicatedTotal}`);
    if ((pve && pve.onDemand) || (pvp && pvp.onDemand)) {
      setText('ov-dedicated-sub', 'Modo dinamico: abre ao criar sala');
      setTone('ov-dedicated-status', 'info');
    } else {
      setText('ov-dedicated-sub', `PvE ${pve && pve.ready ? 'OK' : 'OFF'} / PvP ${pvp && pvp.ready ? 'OK' : 'OFF'}`);
      setTone('ov-dedicated-status', dedicatedReady === dedicatedTotal ? 'ok' : 'warn');
    }
  } else {
    setText('ov-dedicated-status', '---');
    setText('ov-dedicated-sub', 'Sem dedicado fixo');
    setTone('ov-dedicated-status', 'info');
  }

  const health = missing || stopped ? 'ATENCAO' : 'OK';
  setText('nav-health', `Sistema: ${health}`);
  setTone('nav-health', missing ? 'bad' : stopped ? 'warn' : 'ok');
  updatePanelAlerts();
}

function renderSkeleton(targetId, count = 4) {
  if (uiBaseDomain) return uiBaseDomain.renderSkeleton(targetId, count);
  const el = $(targetId);
  if (!el) return;
  el.innerHTML = Array.from({ length: count }, () => '<div class="skeleton-line"></div>').join('');
}

function ensureItemHoverPreview() {
  if (itemHoverPreviewEl) return itemHoverPreviewEl;
  const wrap = document.createElement('div');
  wrap.className = 'item-hover-preview';
  wrap.id = 'item-hover-preview';
  wrap.innerHTML = '<img alt="preview" />';
  document.body.appendChild(wrap);
  itemHoverPreviewEl = wrap;
  return wrap;
}

function isHoverPreviewImageTarget(el) {
  if (!el || el.tagName !== 'IMG') return false;
  return el.matches('.survival-item-thumb, .survival-selected-item-thumb, .survival-preview-reward img, .survival-card-reward img, .iri-thumb, .shop-item-image, .item-thumb');
}

function moveItemHoverPreview(x, y) {
  const wrap = ensureItemHoverPreview();
  const gap = 16;
  const rect = wrap.getBoundingClientRect();
  let left = x + gap;
  let top = y + gap;
  if (left + rect.width > window.innerWidth - 8) left = x - rect.width - gap;
  if (top + rect.height > window.innerHeight - 8) top = y - rect.height - gap;
  wrap.style.left = `${Math.max(8, left)}px`;
  wrap.style.top = `${Math.max(8, top)}px`;
}

function showItemHoverPreview(imgEl, x, y) {
  const src = (imgEl && imgEl.getAttribute('src')) || '';
  if (!src) return;
  const wrap = ensureItemHoverPreview();
  const previewImg = wrap.querySelector('img');
  if (!previewImg) return;
  previewImg.src = src;
  wrap.classList.add('show');
  moveItemHoverPreview(x, y);
}

function hideItemHoverPreview() {
  if (!itemHoverPreviewEl) return;
  itemHoverPreviewEl.classList.remove('show');
}

function initItemHoverPreview() {
  ensureItemHoverPreview();
  document.addEventListener('mousemove', (e) => {
    if (!itemHoverPreviewEl || !itemHoverPreviewEl.classList.contains('show')) return;
    moveItemHoverPreview(e.clientX, e.clientY);
  });
  document.addEventListener('mouseover', (e) => {
    const target = e.target;
    if (!isHoverPreviewImageTarget(target)) return;
    showItemHoverPreview(target, e.clientX, e.clientY);
  });
  document.addEventListener('mouseout', (e) => {
    const target = e.target;
    if (!isHoverPreviewImageTarget(target)) return;
    hideItemHoverPreview();
  });
  window.addEventListener('blur', hideItemHoverPreview);
  document.addEventListener('scroll', hideItemHoverPreview, true);
}

function formatDurationMs(ms) {
  if (uiBaseDomain) return uiBaseDomain.formatDurationMs(ms);
  const totalSec = Math.max(0, Math.floor((Number(ms) || 0) / 1000));
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
  return `${m}m ${String(s).padStart(2, '0')}s`;
}

function updateXpEventStatusUi() {
  const pill = $('xp-event-status');
  const disableBtn = $('xp-disable-btn');
  if (!pill || !disableBtn) return;
  if (!xpEventState.active || Number(xpEventState.multiplier || 1) <= 1) {
    pill.textContent = 'XP Event: inativo';
    pill.classList.remove('tone-ok', 'tone-warn', 'tone-bad');
    pill.classList.add('tone-info');
    disableBtn.disabled = true;
    return;
  }
  disableBtn.disabled = false;
  const now = Date.now();
  const elapsed = xpEventState.startedAt ? formatDurationMs(now - Number(xpEventState.startedAt)) : '0m 00s';
  if (xpEventState.temporary && xpEventState.expiresAt) {
    const remainMs = Number(xpEventState.expiresAt) - now;
    if (remainMs <= 0) {
      pill.textContent = `XP ${xpEventState.multiplier}x expirando...`; 
      pill.classList.remove('tone-info', 'tone-ok', 'tone-bad');
      pill.classList.add('tone-warn');
      return;
    }
    const remain = formatDurationMs(remainMs);
    pill.textContent = `XP ${xpEventState.multiplier}x ativo | ${elapsed} | resta ${remain}`;
    pill.classList.remove('tone-info', 'tone-warn', 'tone-bad');
    pill.classList.add('tone-ok');
    return;
  }
  pill.textContent = `XP ${xpEventState.multiplier}x ativo | ${elapsed} | permanente`;
  pill.classList.remove('tone-info', 'tone-warn', 'tone-bad');
  pill.classList.add('tone-ok');
}

function confirmDanger(message) {
  if (uiBaseDomain) return uiBaseDomain.confirmDanger(message);
  return window.confirm(message);
}


function getPanelModel() {
  return panelModel || DEFAULT_PANEL_MODEL;
}

function getCommandLimit(cmd) {
  const model = getPanelModel();
  return (model.command && model.command.limits && model.command.limits[cmd]) || null;
}

function updateCommandAmountUI() {
  const amountGroup = $('amount-group');
  const amountInput = $('cmd-amount');
  const hidden = selectedCmd === 'kick' || selectedCmd === 'addcm';
  amountGroup.classList.toggle('hidden', hidden);
  if (hidden) return;

  const lim = getCommandLimit(selectedCmd);
  if (selectedCmd === 'addgm') {
    const minRank = lim && lim.minRank ? lim.minRank : 1;
    const maxRank = lim && lim.maxRank ? lim.maxRank : 90;
    amountInput.min = String(minRank);
    amountInput.max = String(maxRank);
    amountInput.placeholder = `${minRank} ~ ${maxRank}`;
    return;
  }

  const maxPerCmd = lim && lim.perCmd ? lim.perCmd : 100000000;
  amountInput.min = '1';
  amountInput.max = String(maxPerCmd);
  amountInput.placeholder = `1 ~ ${maxPerCmd.toLocaleString('pt-BR')}`;
}

function applyModelToInputs() {
  const model = getPanelModel();

  const nickMax = (model.nick && model.nick.maxLen) || DEFAULT_PANEL_MODEL.nick.maxLen;
  $('cmd-nick').maxLength = nickMax;
  $('item-nick').maxLength = nickMax;
  $('removeitem-nick').maxLength = nickMax;
  $('ach-nick').maxLength = nickMax;

  const itemQty = (model.item && model.item.quantity) || DEFAULT_PANEL_MODEL.item.quantity;
  $('item-qty').min = String(itemQty.min);
  $('item-qty').max = String(itemQty.max);

  const itemExp = (model.item && model.item.expirationHours) || DEFAULT_PANEL_MODEL.item.expirationHours;
  $('item-exp').min = String(itemExp.min);
  $('item-exp').max = String(itemExp.max);

  const achProgress = (model.achievement && model.achievement.progress) || DEFAULT_PANEL_MODEL.achievement.progress;
  $('ach-progress').min = String(achProgress.min);
  $('ach-progress').max = String(achProgress.max);

  const xpMult = (model.xp && model.xp.multiplier) || DEFAULT_PANEL_MODEL.xp.multiplier;
  $('xp-multiplier').min = String(xpMult.min);
  $('xp-multiplier').max = String(xpMult.max);

  updateCommandAmountUI();
}

async function loadPanelModel() {
  try {
    const r = await fetch('/api/model', { headers: { 'X-Auth-Token': token } });
    const d = await r.json();
    if (d && d.success && d.model) panelModel = d.model;
  } catch {}
  applyModelToInputs();
}

const weaponVisualCache = {};

function normalizeItemBaseKey(name) {
  return String(name || '').toLowerCase().replace(/_(shop|default|game|bronze|silver|gold|diamond|premium)$/i, '');
}

function getLocalWeaponThumb(itemKey) {
  const base = normalizeItemBaseKey(itemKey);
  return `/api/weapons/thumb?name=${encodeURIComponent(base)}`;
}

function itemCategory(name) {
  const n = (name || '').toLowerCase();
  if (n.includes('box')||n.includes('case')||n.includes('container')||n.includes('caixa')||n.startsWith('key_')) return 'box';
  if (['ar','smg','shg','sr','mg','hmg','pt','kn'].some(x=>n.startsWith(x))||n.includes('gl')) return 'weapon';
  if (n.includes('helmet')||n.includes('vest')||n.includes('hands')||n.includes('shoes')) return 'armor';
  if (n.includes('skin')||n.includes('camo')||n.includes('fbs')||n.includes('set12')||n.includes('bra')||n.includes('cartel')||n.includes('carbon')) return 'skin';
  if (n.includes('booster')||n.includes('consum')||n.includes('voucher')||n.includes('xp_')||n.includes('vp_')||n.includes('crown_')||n.includes('credit')) return 'consumable';
  if (n.includes('bundle')||n.includes('kit')||n.includes('pack')) return 'bundle';
  return 'other';
}

function weaponVisualImage(itemKey) {
  const base = normalizeItemBaseKey(itemKey);
  const fromCache = weaponVisualCache[base];
  if (fromCache && fromCache.image) return fromCache.image;
  return getLocalWeaponThumb(base);
}

function fallbackItemImage(img, name) {
  const cat = itemCategory(name);
  const catFallback = cat === 'box'
    ? '/img/weapons/wiki_all/icons_randombox_skins.png'
    : `/img/weapons/_${cat === 'other' ? 'default' : cat}.png`;
  if (img.src !== catFallback && !img.src.includes('/_')) {
    img.src = catFallback;
  } else {
    img.src = '/img/weapons/_default.png';
  }
}

function weaponVisualTitle(itemKey, fallbackName) {
  const base = normalizeItemBaseKey(itemKey);
  const fromCache = weaponVisualCache[base];
  if (fromCache && fromCache.wikiName) return fromCache.wikiName;
  return fallbackName || base;
}

async function enrichWeaponVisuals(items, noNetwork, maxNetwork) {
  try {
    const uniq = [];
    const seen = new Set();
    (items || []).forEach(row => {
      const key = normalizeItemBaseKey(row && row.key);
      if (!key || seen.has(key)) return;
      seen.add(key);
      uniq.push({ key, displayName: row.displayName || row.name || key });
    });
    if (!uniq.length) return {};

    const out = {};
    const chunkSize = 100;
    let remainingBudget = (maxNetwork == null) ? null : Number(maxNetwork);

    for (let i = 0; i < uniq.length; i += chunkSize) {
      const chunk = uniq.slice(i, i + chunkSize);
      const chunkBudget = remainingBudget == null ? undefined : Math.max(0, remainingBudget);
      const r = await fetch('/api/weapons/enrich', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Auth-Token': token },
        body: JSON.stringify({
          items: chunk,
          noNetwork: !!noNetwork || chunkBudget === 0,
          maxNetwork: chunkBudget,
          concurrency: 10
        })
      });
      const d = await r.json();
      if (!d.success || !d.items) continue;
      Object.keys(d.items).forEach(k => {
        weaponVisualCache[k] = d.items[k];
        out[k] = d.items[k];
      });
      if (remainingBudget != null) {
        remainingBudget -= chunk.length;
        if (remainingBudget < 0) remainingBudget = 0;
      }
    }

    return out;
  } catch {
    return {};
  }
}

function initAppDomBindings() {
  $('btn-exec').addEventListener('click', execCommand);
  $('cmd-nick').addEventListener('keydown', e => { if (e.key === 'Enter') execCommand(); });
  $('cmd-nick').addEventListener('input', lookupProfile);
  $('log-filter').addEventListener('change', loadAllLogs);
  $('btn-gen-token').addEventListener('click', generateToken);
  $('btn-ins-load').addEventListener('click', loadFullProfile);
  $('ins-nick').addEventListener('keydown', e => { if (e.key === 'Enter') loadFullProfile(); });
  const patchFileInput = $('patch-file-input');
  const patchFolderInput = $('patch-folder-input');
  if (patchFileInput) {
    patchFileInput.addEventListener('change', () => {
      patchSelectionMode = 'files';
      if (patchFolderInput) patchFolderInput.value = '';
      renderPatchSelectionSummary();
    });
  }
  if (patchFolderInput) {
    patchFolderInput.addEventListener('change', () => {
      patchSelectionMode = 'folder';
      if (patchFileInput) patchFileInput.value = '';
      renderPatchSelectionSummary();
    });
  }
  document.querySelectorAll('.tab').forEach(t => {
    t.addEventListener('click', () => switchTab(t.dataset.tab));
  });
  document.querySelectorAll('.cmd-opt').forEach(el => {
    el.addEventListener('click', () => {
      document.querySelectorAll('.cmd-opt').forEach(o => o.classList.remove('selected'));
      el.classList.add('selected');
      selectedCmd = el.dataset.cmd;
      updateCommandAmountUI();
    });
  });
  document.querySelector('.cmd-opt[data-cmd="addcry"]').classList.add('selected');

  const globalItemSel = $('global-reward-item-select');
  if (globalItemSel) globalItemSel.addEventListener('change', () => syncRewardItemFromSelect('global-reward-item-select', 'global-reward-item'));
  const survivalItemSel = $('survival-reward-item-select');
  if (survivalItemSel) survivalItemSel.addEventListener('change', () => syncRewardItemFromSelect('survival-reward-item-select', 'survival-reward-item'));
  $('survival-type-select')?.addEventListener('change', loadSelectedSurvivalType);
  $('survival-item-search')?.addEventListener('input', renderSurvivalItemSearch);
  document.querySelector('.cmd-opt[data-cmd="addcry"]').classList.add('selected');
  applyModelToInputs();
  initDesktopUx();
  renderPanelState('overview');
  document.body.classList.add('panel-authed');
  $('dashboard').classList.remove('hidden');
  if (xpEventUiTimer) clearInterval(xpEventUiTimer);
  xpEventUiTimer = setInterval(updateXpEventStatusUi, 1000);
  initItemHoverPreview();
  initDashboard();
}

async function login() {
  const pwd = $('login-password').value;
  if (!pwd) return;
  try {
    const r = await fetch(`/api/login?password=${encodeURIComponent(pwd)}`);
    const d = await r.json();
    if (d.success) {
      token = d.token;
      document.body.classList.add('panel-authed');
      $('login-screen').classList.add('hidden');
      $('dashboard').classList.remove('hidden');
      initDashboard();
    } else { alert('Senha incorreta'); }
  } catch { alert('Erro ao conectar'); }
}

function initDashboard() {
  loadPanelModel();
  startMonitor();
  connectLogStream();
  loadServices();
  loadAcConfig();
  loadConfig();
  loadRuntimeConfigEditor();
  loadXpMultiplier();
  if (xpEventSyncTimer) clearInterval(xpEventSyncTimer);
  xpEventSyncTimer = setInterval(loadXpMultiplier, 15000);
}

function switchTab(tab) {
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
  document.querySelectorAll('.tab-content').forEach(t => t.classList.toggle('active', t.id === `tab-${tab}`));
  renderPanelState(tab);
  if (tab === 'services') loadServices();
  if (tab === 'anticheat') loadAcConfig();
  if (tab === 'config') loadConfig();
  if (tab === 'config') loadRuntimeConfigEditor();
  if (tab === 'overview') loadXpMultiplier();
  if (tab === 'shop') {
    switchShopSubtab(currentShopSubtab || 'offers');
    loadShopPackagesAndRotation();
    if (!shopLastPageOffers.length) loadShopOffers(true);
  }
  if (tab === 'survival-rewards') loadSurvivalRewards(false);
  if (tab === 'launcher') {
    loadLauncherConfig();
    showLauncherCdnPanel('reference');
    loadLauncherVersions();
    loadLauncherRefInfo();
    loadLauncherPatchHistory();
  }
  if (tab === 'launcher-news') loadLauncherConfig();
  if (tab === 'gamefiles') {
    showGameFilesPanel('reference');
    loadGameRefInfo();
    loadVersions();
  }
}

function showGameFilesPanel(panel) {
  const next = panel || 'reference';
  document.querySelectorAll('.gamefiles-subpanel').forEach(el => {
    el.classList.toggle('active', el.id === `gamefiles-panel-${next}`);
  });
  document.querySelectorAll('[data-gamefiles-subtab]').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.gamefilesSubtab === next);
  });
  if (next === 'reference') loadGameRefInfo();
  if (next === 'patches') loadPatchHistory();
  if (next === 'publish') {
    loadGameRefInfo();
    loadVersions();
    pollPublishProgress(true);
  }
}

function showPublishPanel() {
  showGameFilesPanel('publish');
}

// ─── Monitor ───────────────────────────────────────────────────────────
async function fetchServerInfo() {
  return servicesMonitoringDomain && servicesMonitoringDomain.fetchServerInfo
    ? servicesMonitoringDomain.fetchServerInfo()
    : undefined;
}

async function fetchPlayers() {
  try {
    const r = await fetch('/api/players', { headers: { 'X-Auth-Token': token } });
    const d = await r.json();
    if (!d.success) return;
    const list = $('ov-players-list');
    if (!d.players || d.players.length === 0) {
      list.innerHTML = '<div class="empty-state">Nenhum jogador online</div>';
      return;
    }
    list.innerHTML = d.players.map(p =>
      `<div class="player-item"><span class="name">${esc(p.nickname || p.username)}</span><span class="info">${esc(p.ip || '---')}</span></div>`
    ).join('');
  } catch {}
}

function startMonitor() {
  return servicesMonitoringDomain && servicesMonitoringDomain.startMonitor
    ? servicesMonitoringDomain.startMonitor()
    : undefined;
}

// ─── Services ──────────────────────────────────────────────────────────
async function loadServices() {
  return servicesMonitoringDomain && servicesMonitoringDomain.loadServices
    ? servicesMonitoringDomain.loadServices()
    : undefined;
}

async function svcAction(id, action, btnEl) {
  return servicesMonitoringDomain && servicesMonitoringDomain.svcAction
    ? servicesMonitoringDomain.svcAction(id, action, btnEl)
    : undefined;
}

async function startAll() {
  return servicesMonitoringDomain && servicesMonitoringDomain.startAll
    ? servicesMonitoringDomain.startAll()
    : undefined;
}
async function stopAll() {
  return servicesMonitoringDomain && servicesMonitoringDomain.stopAll
    ? servicesMonitoringDomain.stopAll()
    : undefined;
}

// ─── Logs ──────────────────────────────────────────────────────────────
function connectLogStream() {
  return servicesMonitoringDomain && servicesMonitoringDomain.connectLogStream
    ? servicesMonitoringDomain.connectLogStream()
    : undefined;
}

async function loadInitialLogs() {
  return servicesMonitoringDomain && servicesMonitoringDomain.loadInitialLogs
    ? servicesMonitoringDomain.loadInitialLogs()
    : undefined;
}

async function loadAllLogs() {
  return servicesMonitoringDomain && servicesMonitoringDomain.loadAllLogs
    ? servicesMonitoringDomain.loadAllLogs()
    : undefined;
}

function isSessionDebugLog(msg) {
  return servicesMonitoringDomain && servicesMonitoringDomain.isSessionDebugLog
    ? servicesMonitoringDomain.isSessionDebugLog(msg)
    : false;
}

function addLogEntry(entry) {
  return servicesMonitoringDomain && servicesMonitoringDomain.addLogEntry
    ? servicesMonitoringDomain.addLogEntry(entry)
    : undefined;
}

function copyLogs() {
  return servicesMonitoringDomain && servicesMonitoringDomain.copyLogs
    ? servicesMonitoringDomain.copyLogs()
    : undefined;
}

function clearLogs() {
  return servicesMonitoringDomain && servicesMonitoringDomain.clearLogs
    ? servicesMonitoringDomain.clearLogs()
    : undefined;
}

// ─── Anti-Cheat ────────────────────────────────────────────────────────
async function loadAcConfig() {
  return servicesAntiCheatDomain && servicesAntiCheatDomain.loadAcConfig
    ? servicesAntiCheatDomain.loadAcConfig()
    : undefined;
}

async function toggleAc(el) {
  return servicesAntiCheatDomain && servicesAntiCheatDomain.toggleAc
    ? servicesAntiCheatDomain.toggleAc(el)
    : undefined;
}

async function saveAcRaw() {
  return servicesAntiCheatDomain && servicesAntiCheatDomain.saveAcRaw
    ? servicesAntiCheatDomain.saveAcRaw()
    : undefined;
}

// ─── Commands ──────────────────────────────────────────────────────────
let lookupTimer = null;
async function lookupProfile() {
  return playersCommandsDomain && playersCommandsDomain.lookupProfile
    ? playersCommandsDomain.lookupProfile()
    : undefined;
}

function num(n) { if (uiBaseDomain) return uiBaseDomain.num(n); return (n || 0).toLocaleString('pt-BR'); }

function parseStrictIntInput(value) {
  if (uiBaseDomain) return uiBaseDomain.parseStrictIntInput(value);
  const s = String(value || '').trim();
  if (!/^-?\d+$/.test(s)) return null;
  const n = Number(s);
  if (!Number.isSafeInteger(n)) return null;
  return n;
}

async function execCommand() {
  return playersCommandsDomain && playersCommandsDomain.execCommand
    ? playersCommandsDomain.execCommand()
    : undefined;
}

async function generateToken() {
  return playersCommandsDomain && playersCommandsDomain.generateToken
    ? playersCommandsDomain.generateToken()
    : undefined;
}

function showResult(msg, isError) {
  if (uiBaseDomain) return uiBaseDomain.showResult(msg, isError);
  const el = $('cmd-result');
  el.textContent = msg;
  el.className = 'cmd-result' + (isError ? ' error' : '');
  el.classList.remove('hidden');
  showToast(msg, isError ? 'error' : 'success');
  setTimeout(() => el.classList.add('hidden'), 5000);
}

// ─── Inspect ────────────────────────────────────────────────────────────
const CLASS_NAMES = ['Rifleman','Heavy','Recon','Medic','Engineer'];
const CLASS_ICONS = ['classiconrifleman','classiconheavy','classiconsniper','classiconmedic','classiconengineer'];
let itemNames = null;
async function loadItemNames() {
  return playersInspectDomain && playersInspectDomain.loadItemNames
    ? playersInspectDomain.loadItemNames()
    : undefined;
}
function itemDisplayName(name) {
  return playersInspectDomain && playersInspectDomain.itemDisplayName
    ? playersInspectDomain.itemDisplayName(name)
    : String(name || "");
}
const SLOT_PT = ['Fuzileiro','Heavy','Atirador','Medico','Engenheiro','Pistola','Corpo a corpo','Equipamento','Capacete','Colete','Luvas','Botas','Paraquedas','DogTags','Mochila','C4','Badge','Marca','Listra','Pele','Graffiti','Avatar','Spray','Roupa','Contrato','Receita','Acessorio','Material','FuzileiroVIP','HeavyVIP','AtiradorVIP','MedicoVIP','EngenheiroVIP','PistolaVIP','CorpoVIP','EquipVIP','CapaceteVIP','ColeteVIP','LuvasVIP','BotasVIP','ParaquedasVIP','DogTagsVIP','MochilaVIP','BadgeVIP','MarcaVIP','ListraVIP','ArmaCraft','ArmaCraftVIP','Especial'];
const STAT_PT = {
  player_online_time:'Tempo online', player_max_session_time:'Sessao maxima', player_ammo_restored:'Municao restaurada',
  player_climb_coops:'Escaladas', player_repair:'Reparos', player_heal:'Cura',
  player_resurrected_by_coin:'Ressuscitado (moeda)', player_climb_assists:'Auxilio escalada',
  player_resurrect_made:'Ressuscitou', player_gained_money:'Dinheiro ganho', player_damage:'Dano',
  player_max_damage:'Dano maximo', player_resurrected_by_medic:'Ressuscitado (medico)',
  player_kills_ai:'Inimigos (IA)', player_kills_player:'Jogadores', player_kill_streak:'Sequencia',
  player_kills_melee:'Corpo a corpo', player_kills_claymore:'Claymore', player_deaths:'Mortes',
  player_sessions_left:'Sessoes abandonadas', player_sessions_lost_connection:'Desconectou',
  player_sessions_kicked:'Expulso', player_shots:'Disparos', player_hits:'Acertos',
  player_headshots:'Headshots', player_playtime:'Tempo jogado', player_sessions_won:'Vitorias',
  player_sessions_lost:'Derrotas', player_sessions_draw:'Empates', player_wpn_usage:'Uso de arma'
};
const MODE_PT = { PVP:'PvP', PVE:'PvE', '':'-' };
const CLASS_PT = { Rifleman:'Fuzileiro', Heavy:'Heavy', Recon:'Atirador', Medic:'Medico', Engineer:'Engenheiro', '':'-' };
function fmtBanner(v) {
  return playersInspectDomain && playersInspectDomain.fmtBanner
    ? playersInspectDomain.fmtBanner(v)
    : (v === "4294967295" ? "Nenhum" : esc(v));
}

async function loadFullProfile() {
  return playersInspectDomain && playersInspectDomain.loadFullProfile
    ? playersInspectDomain.loadFullProfile()
    : undefined;
}

// ─── Maintenance Mode ─────────────────────────────────────────────────────
let maintenanceEnabled = false;

async function loadMaintenance() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.loadMaintenance
    ? servicesAdminOpsDomain.loadMaintenance()
    : undefined;
}

function toggleMaintenance() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.toggleMaintenance
    ? servicesAdminOpsDomain.toggleMaintenance()
    : undefined;
}

async function saveMaintenance() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.saveMaintenance
    ? servicesAdminOpsDomain.saveMaintenance()
    : undefined;
}

async function saveMaintenanceConfig(enabled, message) {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.saveMaintenanceConfig
    ? servicesAdminOpsDomain.saveMaintenanceConfig(enabled, message)
    : undefined;
}

// ─── XP Multiplier (PvE) ────────────────────────────────────────────────────
function applyQuickXP(multiplier) {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.applyQuickXP
    ? servicesAdminOpsDomain.applyQuickXP(multiplier)
    : undefined;
}

async function loadXpMultiplier() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.loadXpMultiplier
    ? servicesAdminOpsDomain.loadXpMultiplier()
    : undefined;
}

async function setXP(event) {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.setXP
    ? servicesAdminOpsDomain.setXP(event)
    : undefined;
}

async function disableXPEvent() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.disableXPEvent
    ? servicesAdminOpsDomain.disableXPEvent()
    : undefined;
}

// ─── Auto Broadcast ────────────────────────────────────────────────────────
let autoBroadcastEnabled = false;

async function loadAutoBroadcast() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.loadAutoBroadcast
    ? servicesAdminOpsDomain.loadAutoBroadcast()
    : undefined;
}

function toggleAutoBroadcast() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.toggleAutoBroadcast
    ? servicesAdminOpsDomain.toggleAutoBroadcast()
    : undefined;
}

async function saveAutoBroadcast() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.saveAutoBroadcast
    ? servicesAdminOpsDomain.saveAutoBroadcast()
    : undefined;
}

// ─── Backup ────────────────────────────────────────────────────────────────
async function createBackup() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.createBackup
    ? servicesAdminOpsDomain.createBackup()
    : undefined;
}

async function listBackups() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.listBackups
    ? servicesAdminOpsDomain.listBackups()
    : undefined;
}

// ─── Config ────────────────────────────────────────────────────────────
async function loadConfig() {
  return servicesConfigDomain && servicesConfigDomain.loadConfig
    ? servicesConfigDomain.loadConfig()
    : undefined;
}

function objectToKeyValueLines(obj) {
  return servicesConfigDomain && servicesConfigDomain.objectToKeyValueLines
    ? servicesConfigDomain.objectToKeyValueLines(obj)
    : '';
}

function keyValueLinesToObject(text) {
  return servicesConfigDomain && servicesConfigDomain.keyValueLinesToObject
    ? servicesConfigDomain.keyValueLinesToObject(text)
    : {};
}

function fillConfigBlocks(cfg) {
  return servicesConfigDomain && servicesConfigDomain.fillConfigBlocks
    ? servicesConfigDomain.fillConfigBlocks(cfg)
    : undefined;
}

function parseDedicatedPortMapLines(text) {
  return servicesConfigDomain && servicesConfigDomain.parseDedicatedPortMapLines
    ? servicesConfigDomain.parseDedicatedPortMapLines(text)
    : {};
}

function buildConfigFromBlocks() {
  return servicesConfigDomain && servicesConfigDomain.buildConfigFromBlocks
    ? servicesConfigDomain.buildConfigFromBlocks()
    : {};
}

function syncConfigBlocksToEditor() {
  return servicesConfigDomain && servicesConfigDomain.syncConfigBlocksToEditor
    ? servicesConfigDomain.syncConfigBlocksToEditor()
    : undefined;
}

async function saveConfig() {
  return servicesConfigDomain && servicesConfigDomain.saveConfig
    ? servicesConfigDomain.saveConfig()
    : undefined;
}

async function loadPanelPaths() {
  return servicesConfigDomain && servicesConfigDomain.loadPanelPaths
    ? servicesConfigDomain.loadPanelPaths()
    : undefined;
}

async function savePanelPaths() {
  return servicesConfigDomain && servicesConfigDomain.savePanelPaths
    ? servicesConfigDomain.savePanelPaths()
    : undefined;
}

function esc(s) { if (uiBaseDomain) return uiBaseDomain.esc(s); return String(s).replace(/[<>&]/g, c => ({ '<':'&lt;', '>':'&gt;', '&':'&amp;' })[c]); }

// ─── Notes ────────────────────────────────────────────────────────────────
async function loadNotes() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.loadNotes
    ? servicesAdminOpsDomain.loadNotes()
    : undefined;
}

async function saveNotes() {
  return servicesAdminOpsDomain && servicesAdminOpsDomain.saveNotes
    ? servicesAdminOpsDomain.saveNotes()
    : undefined;
}

// ─── Clans ────────────────────────────────────────────────────────────────
async function loadClans() {
  return playersModerationDomain && playersModerationDomain.loadClans
    ? playersModerationDomain.loadClans()
    : undefined;
}

function showCreateClan() {
  return playersModerationDomain && playersModerationDomain.showCreateClan
    ? playersModerationDomain.showCreateClan()
    : undefined;
}

async function createClan() {
  return playersModerationDomain && playersModerationDomain.createClan
    ? playersModerationDomain.createClan()
    : undefined;
}

async function renameClan(oldName) {
  return playersModerationDomain && playersModerationDomain.renameClan
    ? playersModerationDomain.renameClan(oldName)
    : undefined;
}

function showClanResult(msg, isError) {
  return playersModerationDomain && playersModerationDomain.showClanResult
    ? playersModerationDomain.showClanResult(msg, isError)
    : undefined;
}

// ─── Game Rooms ───────────────────────────────────────────────────────────
let gameroomInterval = null;

async function loadGameRooms() {
  return playersRoomsDomain && playersRoomsDomain.loadGameRooms
    ? playersRoomsDomain.loadGameRooms()
    : undefined;
}

function autoRefreshGameRooms() {
  return playersRoomsDomain && playersRoomsDomain.autoRefreshGameRooms
    ? playersRoomsDomain.autoRefreshGameRooms()
    : undefined;
}

// ─── Achievements ─────────────────────────────────────────────────────────
let achLoadTimer = null;
let achievementImageMap = null;
let achievementBadgePool = null;
let achCatalogOffset = 0;
let achCatalogLimit = 80;
let achCatalogTotal = 0;

async function loadAchievementImageMap() {
  if (playersAchievementsDomain) return playersAchievementsDomain.loadAchievementImageMap();
  if (achievementImageMap) return achievementImageMap;
  achievementImageMap = {};
  achievementBadgePool = [];
  try {
    const r = await fetch('/wiki-allimages-index.json', { cache: 'no-store' });
    const d = await r.json();
    const byName = d && d.byName ? d.byName : {};
    Object.keys(byName).forEach(fileName => {
      const lower = String(fileName || '').toLowerCase();
      const path = byName[fileName];
      if (!path || (!lower.startsWith('challenge_') && !lower.startsWith('achievement_'))) return;
      if (lower.startsWith('challenge_badge_') && lower.endsWith('.png')) achievementBadgePool.push(path);
      const m = lower.match(/(?:^|_)(\d{1,4})(?:\.[a-z0-9]+)$/i);
      if (!m) return;
      const id = String(Number(m[1]));
      if (!id || id === '0') return;
      const current = achievementImageMap[id];
      const score =
        lower.includes('challenge_badge_') ? 100 :
        lower.includes('achievement_') ? 80 :
        lower.includes('challenge_mark_') ? 60 :
        lower.includes('challenge_strip_') ? 40 : 10;
      if (!current || score > current.score) achievementImageMap[id] = { path, score };
    });
    achievementBadgePool = Array.from(new Set(achievementBadgePool)).sort();
  } catch {}
  return achievementImageMap;
}

function readRuntimeEditorPayload() {
  return servicesRuntimeConfigDomain && servicesRuntimeConfigDomain.readRuntimeEditorPayload
    ? servicesRuntimeConfigDomain.readRuntimeEditorPayload()
    : {};
}

function validateRuntimeEditorPayload(payload) {
  return servicesRuntimeConfigDomain && servicesRuntimeConfigDomain.validateRuntimeEditorPayload
    ? servicesRuntimeConfigDomain.validateRuntimeEditorPayload(payload)
    : { ok: false, error: 'Runtime config domain indisponivel' };
}

function fillRuntimeEditor(runtime) {
  return servicesRuntimeConfigDomain && servicesRuntimeConfigDomain.fillRuntimeEditor
    ? servicesRuntimeConfigDomain.fillRuntimeEditor(runtime)
    : undefined;
}

function showRuntimeConfigResult(message, isError = false) {
  return servicesRuntimeConfigDomain && servicesRuntimeConfigDomain.showRuntimeConfigResult
    ? servicesRuntimeConfigDomain.showRuntimeConfigResult(message, isError)
    : undefined;
}

async function loadRuntimeConfigEditor() {
  return servicesRuntimeConfigDomain && servicesRuntimeConfigDomain.loadRuntimeConfigEditor
    ? servicesRuntimeConfigDomain.loadRuntimeConfigEditor()
    : undefined;
}

async function publishRuntimeConfig(mode = 'snapshot') {
  return servicesRuntimeConfigDomain && servicesRuntimeConfigDomain.publishRuntimeConfig
    ? servicesRuntimeConfigDomain.publishRuntimeConfig(mode)
    : undefined;
}

function pickAchievement(id, progress) {
  return playersAchievementsDomain && playersAchievementsDomain.pickAchievement
    ? playersAchievementsDomain.pickAchievement(id, progress)
    : undefined;
}

function renderAchievementCatalogPager() {
  return playersAchievementsDomain && playersAchievementsDomain.renderAchievementCatalogPager
    ? playersAchievementsDomain.renderAchievementCatalogPager()
    : undefined;
}

function achCatalogPrev() {
  return playersAchievementsDomain && playersAchievementsDomain.achCatalogPrev
    ? playersAchievementsDomain.achCatalogPrev()
    : undefined;
}

function achCatalogNext() {
  return playersAchievementsDomain && playersAchievementsDomain.achCatalogNext
    ? playersAchievementsDomain.achCatalogNext()
    : undefined;
}

async function grantAchievementFromCatalog(id) {
  return playersAchievementsDomain && playersAchievementsDomain.grantAchievementFromCatalog
    ? playersAchievementsDomain.grantAchievementFromCatalog(id)
    : undefined;
}

function achievementImageCandidates(id) {
  return playersAchievementsDomain && playersAchievementsDomain.achievementImageCandidates
    ? playersAchievementsDomain.achievementImageCandidates(id)
    : ['/img/weapons/_default.png'];
}

function achievementIconToImage(iconName) {
  return playersAchievementsDomain && playersAchievementsDomain.achievementIconToImage
    ? playersAchievementsDomain.achievementIconToImage(iconName)
    : '';
}

function achievementRowImageCandidates(rowOrId) {
  return playersAchievementsDomain && playersAchievementsDomain.achievementRowImageCandidates
    ? playersAchievementsDomain.achievementRowImageCandidates(rowOrId)
    : ['/img/weapons/_default.png'];
}

function titleCaseWords(text) {
  return playersAchievementsDomain && playersAchievementsDomain.titleCaseWords
    ? playersAchievementsDomain.titleCaseWords(text)
    : String(text || '');
}

function humanizeAchievementLabel(entry) {
  return playersAchievementsDomain && playersAchievementsDomain.humanizeAchievementLabel
    ? playersAchievementsDomain.humanizeAchievementLabel(entry)
    : 'Conquista';
}

function fallbackAchievementImage(img) {
  return playersAchievementsDomain && playersAchievementsDomain.fallbackAchievementImage
    ? playersAchievementsDomain.fallbackAchievementImage(img)
    : undefined;
}

async function loadAchievementLists() {
  return playersAchievementsDomain && playersAchievementsDomain.loadAchievementLists
    ? playersAchievementsDomain.loadAchievementLists()
    : undefined;
}

async function giveAchievement() {
  return playersAchievementsDomain && playersAchievementsDomain.giveAchievement
    ? playersAchievementsDomain.giveAchievement()
    : undefined;
}

async function removeAchievement() {
  return playersAchievementsDomain && playersAchievementsDomain.removeAchievement
    ? playersAchievementsDomain.removeAchievement()
    : undefined;
}

// ─── Remove Item ──────────────────────────────────────────────────────────
async function removeItem() {
  return playersItemOpsDomain && playersItemOpsDomain.removeItem
    ? playersItemOpsDomain.removeItem()
    : undefined;
}

// ─── Broadcast ──────────────────────────────────────────────────────────
async function sendBroadcast() {
  return playersItemOpsDomain && playersItemOpsDomain.sendBroadcast
    ? playersItemOpsDomain.sendBroadcast()
    : undefined;
}

async function sendNotification() {
  return playersItemOpsDomain && playersItemOpsDomain.sendNotification
    ? playersItemOpsDomain.sendNotification()
    : undefined;
}

// ─── Bans ────────────────────────────────────────────────────────────────
async function loadBans() {
  return playersModerationDomain && playersModerationDomain.loadBans
    ? playersModerationDomain.loadBans()
    : undefined;
}

async function banPlayer() {
  return playersModerationDomain && playersModerationDomain.banPlayer
    ? playersModerationDomain.banPlayer()
    : undefined;
}

async function unbanPlayer() {
  return playersModerationDomain && playersModerationDomain.unbanPlayer
    ? playersModerationDomain.unbanPlayer()
    : undefined;
}

async function quickUnban(nick) {
  return playersModerationDomain && playersModerationDomain.quickUnban
    ? playersModerationDomain.quickUnban(nick)
    : undefined;
}

async function loadBanHistory() {
  return playersModerationDomain && playersModerationDomain.loadBanHistory
    ? playersModerationDomain.loadBanHistory()
    : undefined;
}



// ─── IP Bans ──────────────────────────────────────────────────────────────
async function loadIpBans() {
  return playersModerationDomain && playersModerationDomain.loadIpBans
    ? playersModerationDomain.loadIpBans()
    : undefined;
}

async function banIp() {
  return playersModerationDomain && playersModerationDomain.banIp
    ? playersModerationDomain.banIp()
    : undefined;
}

async function unbanIp(ip) {
  return playersModerationDomain && playersModerationDomain.unbanIp
    ? playersModerationDomain.unbanIp(ip)
    : undefined;
}

async function kickByIp() {
  return playersModerationDomain && playersModerationDomain.kickByIp
    ? playersModerationDomain.kickByIp()
    : undefined;
}

// ─── Items ───────────────────────────────────────────────────────────────
let itemSuggestionsLoaded = false;

function isGiveItemCandidateKey(key) {
  return playersItemOpsDomain && playersItemOpsDomain.isGiveItemCandidateKey
    ? playersItemOpsDomain.isGiveItemCandidateKey(key)
    : false;
}

async function loadItemSuggestions() {
  return playersItemOpsDomain && playersItemOpsDomain.loadItemSuggestions
    ? playersItemOpsDomain.loadItemSuggestions()
    : undefined;
}

function setGiveItemName(value) {
  return playersItemOpsDomain && playersItemOpsDomain.setGiveItemName
    ? playersItemOpsDomain.setGiveItemName(value)
    : undefined;
}

function renderGiveItemPreview() {
  return playersItemOpsDomain && playersItemOpsDomain.renderGiveItemPreview
    ? playersItemOpsDomain.renderGiveItemPreview()
    : undefined;
}

function hideGiveItemSuggestions() {
  return playersItemOpsDomain && playersItemOpsDomain.hideGiveItemSuggestions
    ? playersItemOpsDomain.hideGiveItemSuggestions()
    : undefined;
}

async function renderGiveItemSuggestions() {
  return playersItemOpsDomain && playersItemOpsDomain.renderGiveItemSuggestions
    ? playersItemOpsDomain.renderGiveItemSuggestions()
    : undefined;
}

let itemSearchTimer = null;
async function searchItems() {
  return playersItemOpsDomain && playersItemOpsDomain.searchItems
    ? playersItemOpsDomain.searchItems()
    : undefined;
}

async function giveItem() {
  return playersItemOpsDomain && playersItemOpsDomain.giveItem
    ? playersItemOpsDomain.giveItem()
    : undefined;
}

// --- Shop Editor ------------------------------------------------------------
let shopPageOffset = 0;
let shopLastPageOffers = [];
let shopTotalOffers = 0;
let shopStatusFilter = 'all';
let shopSortMode = 'position';
let shopSelectedOfferId = null;
let currentShopSubtab = 'offers';
const shopPackageBuilderMap = new Map();
let shopPackagesCache = [];
let shopRotationCache = null;
let shopPendingSelectPackageId = '';
let shopCatalogOffset = 0;
let shopCatalogTotal = 0;
let survivalRewardsCache = [];
let survivalRewardsConfig = null;
let survivalRewardItemSuggestions = [];
let survivalSelectedRewardItems = [];
let survivalRewardItemsDirty = false;
let survivalItemFilter = 'box';

function setShopPackageResult(message, isError) {
  return shopPackagesDomain && shopPackagesDomain.setShopPackageResult
    ? shopPackagesDomain.setShopPackageResult(message, isError)
    : undefined;
}

function setSurvivalResult(message, isError) {
  return rewardsSurvivalDomain && rewardsSurvivalDomain.setSurvivalResult
    ? rewardsSurvivalDomain.setSurvivalResult(message, isError)
    : undefined;
}

function updateSurvivalSelectedCount() {
  return rewardsSurvivalDomain && rewardsSurvivalDomain.updateSurvivalSelectedCount
    ? rewardsSurvivalDomain.updateSurvivalSelectedCount()
    : undefined;
}

function survivalRewardMoney(row, currency) {
  return Number(row && row.rewards && row.rewards.money && row.rewards.money[currency]) || 0;
}

function survivalFirstItem(row) {
  const items = row && row.rewards && Array.isArray(row.rewards.items) ? row.rewards.items : [];
  return items[0] || null;
}

function survivalRewardItems(row) {
  return row && row.rewards && Array.isArray(row.rewards.items) ? row.rewards.items : [];
}

function survivalThresholdValue(row, type, medal) {
  return row && row.missions && row.missions.sample && row.missions.sample.thresholds &&
    row.missions.sample.thresholds[type] ? (row.missions.sample.thresholds[type][medal] || '') : '';
}

function survivalEscAttr(value) {
  return esc(value).replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function renderSurvivalRewardItemsDatalist(items) {
  const list = $('survival-reward-items');
  if (!list) return;
  const safeItems = Array.isArray(items) ? items : [];
  survivalRewardItemSuggestions = safeItems;
  list.innerHTML = safeItems.map(item => {
    const label = item.displayName && item.displayName !== item.key ? ` label="${survivalEscAttr(item.displayName)}"` : '';
    return `<option value="${survivalEscAttr(item.key)}"${label}></option>`;
  }).join('');
  populateRewardItemSelects(safeItems);
  renderSurvivalItemSearch();
}

function populateRewardItemSelects(items) {
  const safeItems = Array.isArray(items) ? items : [];
  ['global-reward-item-select', 'survival-reward-item-select'].forEach(id => {
    const sel = $(id);
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = '<option value="">Nenhum (pular item)</option>';
    safeItems.forEach(item => {
      const opt = document.createElement('option');
      opt.value = item.key;
      opt.textContent = item.displayName || item.key;
      sel.appendChild(opt);
    });
    if (current) sel.value = current;
  });
}

function syncRewardItemFromSelect(selectId, hiddenId) {
  const sel = $(selectId);
  const hidden = $(hiddenId);
  if (sel && hidden) {
    hidden.value = sel.value || '';
  }
}

function normalizeSurvivalItemKey(value) {
  return String(value || '').trim().toLowerCase();
}

function findSurvivalRewardSuggestion(name) {
  const key = normalizeSurvivalItemKey(name);
  return survivalRewardItemSuggestions.find(item => normalizeSurvivalItemKey(item.key) === key) || null;
}

function survivalRewardItemKind(item) {
  const key = normalizeSurvivalItemKey(item && (item.key || item.name || item.item || ''));
  const displayName = String(item && item.displayName || '').toLowerCase();
  const category = String(item && item.category || '').toLowerCase();
  const haystack = `${key} ${displayName} ${category}`;
  return haystack.includes('random_box') ||
    haystack.includes('randombox') ||
    haystack.includes('warbox') ||
    haystack.includes('warcase') ||
    haystack.includes('box') ||
    haystack.includes('caixa')
    ? 'WARBOX'
    : 'ITEM';
}

function survivalRewardItemImage(item) {
  const key = normalizeSurvivalItemKey(item && (item.key || item.name || item.item || ''));
  const suggestion = findSurvivalRewardSuggestion(key);
  return (item && item.image) || (suggestion && suggestion.image) || weaponVisualImage(key);
}

function survivalRewardItemDisplay(item) {
  const key = normalizeSurvivalItemKey(item && (item.key || item.name || item.item || ''));
  const suggestion = findSurvivalRewardSuggestion(key);
  return {
    name: key,
    displayName: item && item.displayName || (suggestion && suggestion.displayName) || key,
    category: item && item.category || (suggestion && suggestion.category) || '',
    image: item && item.image || (suggestion && suggestion.image) || '',
    amount: item && item.amount,
    expiration: item && item.expiration,
    durability: item && item.durability
  };
}

function survivalMissionDifficulty(row) {
  const sample = row && row.missions && row.missions.sample ? row.missions.sample : {};
  const text = `${row && row.missionType || ''} ${sample.difficulty || ''} ${sample.relativePath || ''}`.toLowerCase();
  if (text.includes('hard') || text.includes('pro')) return 'Dificil';
  if (text.includes('easy')) return 'Facil';
  if (text.includes('normal')) return 'Normal';
  if (text.includes('survival')) return 'Sobrevivencia';
  return sample.difficulty || 'Missao';
}

function survivalMissionDifficultyCode(row) {
  const label = survivalMissionDifficulty(row).toLowerCase();
  if (label.includes('dificil')) return 'h';
  if (label.includes('facil')) return 'e';
  return 'n';
}

function survivalMissionDisplayName(row) {
  if (!row) return 'Nenhum mapa aberto';
  const type = String(row.missionType || '').toLowerCase();
  const difficulty = survivalMissionDifficulty(row);
  const names = [
    [/^anubis.*2$/, 'Blecaute'],
    [/^anubis/, 'Anubis'],
    [/^campaignsection1$/, 'Ponta de Lanca'],
    [/^campaignsection2$/, 'Emboscada'],
    [/^campaignsection3$/, 'Zenite'],
    [/^campaignsections$/, 'Maratona de Sobrevivencia'],
    [/^chernobyl/, 'Pripyat'],
    [/^icebreaker/, 'Quebra-gelo'],
    [/^japan/, 'Sol Nascente'],
    [/^mars/, 'Marte'],
    [/^survivalmission$/, 'Sobrevivencia China'],
    [/^volcano/, 'Terremoto'],
    [/^zombietower/, 'Black Shark'],
    [/^zombie/, 'Horda Ciborgue']
  ];
  const found = names.find(([re]) => re.test(type));
  if (found) {
    return ['Maratona de Sobrevivencia', 'Sobrevivencia China'].includes(found[1])
      ? found[1]
      : `${found[1]} - ${difficulty}`;
  }
  const sample = row.missions && row.missions.sample ? row.missions.sample : {};
  const cleaned = String(sample.name || row.missionType || '')
    .replace(/^Recompensa\s+(Especial\s+)?da\s+Miss[a�]o\s+(Especial\s+)?/i, '')
    .replace(/^Recompensa\s+especial\s+da\s+miss[a�]o\s+/i, '')
    .replace(/^Caixa\s+Aleat[o�]ria\s+de\s+Recompensa\s+Especial/i, 'Sobrevivencia')
    .trim();
  return cleaned || row.missionType || 'Mapa';
}

function survivalMissionImage(row) {
  const sample = row && row.missions && row.missions.sample ? row.missions.sample : {};
  const type = String(row && row.missionType || '').toLowerCase();
  const text = `${type} ${sample.name || ''} ${sample.releaseMission || ''} ${sample.relativePath || ''}`.toLowerCase();
  const code = survivalMissionDifficultyCode(row);
  if (type === 'campaignsection1') return '/img/weapons/wiki_all/ct_snow1_hard.png';
  if (type === 'campaignsection2') return '/img/weapons/wiki_all/ct_snow2_hard.png';
  if (type === 'campaignsection3') return '/img/weapons/wiki_all/ct_snow3_hard.png';
  if (type === 'campaignsections') return '/img/weapons/wiki_all/ct_survival_snow.png';
  if (type.includes('chernobyl')) return `/img/weapons/wiki_all/chernobyl_${code === 'e' ? 'easy' : code === 'h' ? 'hard' : 'normal'}.png`;
  if (type.includes('icebreaker')) return `/img/weapons/wiki_all/ct_snow_boss_${code === 'e' ? 'easy' : code === 'h' ? 'hard' : 'normal'}.png`;
  if (type.includes('japan')) return `/img/weapons/wiki_all/japan_${code}.png`;
  if (type.includes('mars')) return `/img/weapons/wiki_all/mars_${code}.png`;
  if (type === 'survivalmission') return '/img/weapons/wiki_all/china_survival11.jpg';
  if (type.includes('volcano')) return `/img/weapons/wiki_all/volcano_${code}.png`;
  if (type.includes('zombietower')) return '/img/weapons/wiki_all/blachshark12.jpg';
  if (type.includes('zombie')) return `/img/weapons/wiki_all/zsd1j_${code}.png`;
  if (type.includes('anubis') && type.endsWith('2')) return `/img/weapons/wiki_all/anubis_escape_${code}.png`;
  if (text.includes('anubis')) return `/img/weapons/wiki_all/na_anubis_${code}.png`;
  if (text.includes('swarm')) return '/img/weapons/wiki_all/swarm_act1.jpg';
  if (text.includes('arachnid')) return `/img/weapons/wiki_all/arachnid_marathon_${code}.png`;
  if (text.includes('zsd2')) return `/img/weapons/wiki_all/zsd2j_${code}.png`;
  if (text.includes('zsd') || text.includes('zombie')) return '/img/weapons/wiki_all/zsd1j_map.jpg';
  if (text.includes('tower')) return '/img/weapons/wiki_all/dst_towers.jpg';
  if (text.includes('snow')) return '/img/weapons/wiki_all/ct_survival_snow.png';
  if (code === 'h') return '/img/weapons/wiki_all/ct_survival_new_hard.png';
  if (code === 'e') return '/img/weapons/wiki_all/ct_survival_new_easy.png';
  if (text.includes('normal')) return '/img/weapons/wiki_all/ct_survival_new_normal.png';
  return '/img/weapons/wiki_all/mission.png';
}

function getCurrentSurvivalRow() {
  const selectedType = $('survival-type-select') ? $('survival-type-select').value : '';
  return survivalRewardsCache.find(row => row.missionType === selectedType) || null;
}

function renderSurvivalMissionPreview(row, draftItems) {
  const preview = $('survival-mission-preview');
  if (!preview) return;
  if (!row) {
    preview.innerHTML = `
      <img class="survival-mission-preview-img" src="/img/weapons/wiki_all/mission.png" alt="" onerror="this.src='/img/weapons/_default.png'" />
      <div class="survival-mission-preview-main">
        <div class="survival-mission-preview-title">Nenhum mapa aberto</div>
        <div class="survival-mission-preview-sub">Selecione um mapa na lista para ver a recompensa atual.</div>
        <div class="survival-mission-preview-rewards"><span class="survival-chip blue">Aguardando selecao</span></div>
      </div>`;
    return;
  }
  const sample = row.missions && row.missions.sample ? row.missions.sample : {};
  const missionCount = row.missions && row.missions.count ? row.missions.count : 1;
  const items = Array.isArray(draftItems) ? draftItems : survivalRewardItems(row);
  const visibleItems = items.slice(0, 4).map(survivalRewardItemDisplay);
  const money = [
    survivalRewardMoney(row, 'game_money') ? `<span class="survival-chip">GP ${num(survivalRewardMoney(row, 'game_money'))}</span>` : '',
    survivalRewardMoney(row, 'cry_money') ? `<span class="survival-chip blue">Cash ${num(survivalRewardMoney(row, 'cry_money'))}</span>` : '',
    survivalRewardMoney(row, 'crown_money') ? `<span class="survival-chip warn">Coroas ${num(survivalRewardMoney(row, 'crown_money'))}</span>` : ''
  ].filter(Boolean);
  const rewardHtml = visibleItems.map(item => `
    <span class="survival-preview-reward">
      <img src="${survivalEscAttr(survivalRewardItemImage(item))}" alt="" onerror="fallbackItemImage(this, '${survivalEscAttr(item.name)}')" />
      <span>${esc(item.displayName || item.name)}</span>
    </span>`).join('');
  preview.innerHTML = `
    <img class="survival-mission-preview-img" src="${survivalEscAttr(survivalMissionImage(row))}" alt="" onerror="this.src='/img/weapons/wiki_all/mission.png'" />
    <div class="survival-mission-preview-main">
      <div class="survival-mission-preview-title">${esc(survivalMissionDisplayName(row))}</div>
      <div class="survival-mission-preview-sub">${esc(survivalMissionDifficulty(row))} - ${missionCount} ${missionCount === 1 ? 'mapa' : 'mapas'}</div>
      <div class="survival-mission-preview-rewards">
        ${rewardHtml || '<span class="survival-chip red">Sem item final</span>'}
        ${money.join('')}
      </div>
    </div>`;
}

function normalizeSurvivalSelectedItem(raw) {
  const name = normalizeSurvivalItemKey(raw && (raw.name || raw.key || raw.item || ''));
  if (!name) return null;
  const suggestion = findSurvivalRewardSuggestion(name);
  return {
    name,
    displayName: raw.displayName || (suggestion && suggestion.displayName) || name,
    category: raw.category || (suggestion && suggestion.category) || '',
    image: raw.image || (suggestion && suggestion.image) || '',
    amount: raw.amount === undefined || raw.amount === null ? '' : String(raw.amount),
    expiration: raw.expiration === undefined || raw.expiration === null ? '' : String(raw.expiration),
    durability: raw.durability === undefined || raw.durability === null ? '' : String(raw.durability)
  };
}

function setSurvivalSelectedRewardItems(items, dirty) {
  survivalSelectedRewardItems = (Array.isArray(items) ? items : [])
    .map(normalizeSurvivalSelectedItem)
    .filter(Boolean);
  survivalRewardItemsDirty = !!dirty;
  renderSurvivalSelectedItems();
}

function renderSurvivalSelectedItems() {
  const list = $('survival-selected-items');
  const count = $('survival-selected-items-count');
  if (count) count.textContent = `${survivalSelectedRewardItems.length} ${survivalSelectedRewardItems.length === 1 ? 'item' : 'itens'}`;
  if (!list) return;
  if (!survivalSelectedRewardItems.length) {
    list.innerHTML = '<div class="empty-state">Clique em um item acima para adicionar</div>';
    renderSurvivalMissionPreview(getCurrentSurvivalRow(), []);
    return;
  }
  list.innerHTML = survivalSelectedRewardItems.map((rawItem, idx) => {
    const item = survivalRewardItemDisplay(rawItem);
    return `
    <div class="survival-selected-item-row">
      <img class="survival-selected-item-thumb" src="${survivalEscAttr(survivalRewardItemImage(item))}" alt="" onerror="fallbackItemImage(this, '${survivalEscAttr(item.name)}')" />
      <div class="survival-selected-item-main">
        <div class="survival-selected-item-name">${esc(item.displayName || item.name)}</div>
        <div class="survival-selected-item-key">${esc(item.name)}</div>
      </div>
      <label class="survival-mini-field">Qtd
        <input class="form-input" type="number" min="0" value="${survivalEscAttr(rawItem.amount)}" oninput="updateSurvivalRewardItem(${idx}, 'amount', this.value)" />
      </label>
      <label class="survival-mini-field">Duracao
        <input class="form-input" type="text" value="${survivalEscAttr(rawItem.expiration)}" placeholder="30d" oninput="updateSurvivalRewardItem(${idx}, 'expiration', this.value)" />
      </label>
      <label class="survival-mini-field">Durab
        <input class="form-input" type="number" min="0" value="${survivalEscAttr(rawItem.durability)}" oninput="updateSurvivalRewardItem(${idx}, 'durability', this.value)" />
      </label>
      <button class="sc-btn shop-btn shop-btn-danger survival-remove-item" type="button" onclick="removeSurvivalRewardItem(${idx})">REMOVER</button>
    </div>
  `;
  }).join('');
  renderSurvivalMissionPreview(getCurrentSurvivalRow(), survivalSelectedRewardItems);
}

function updateSurvivalRewardItem(index, field, value) {
  if (!survivalSelectedRewardItems[index]) return;
  survivalSelectedRewardItems[index][field] = String(value || '').trim();
  survivalRewardItemsDirty = true;
  renderSurvivalMissionPreview(getCurrentSurvivalRow(), survivalSelectedRewardItems);
}

function removeSurvivalRewardItem(index) {
  if (index < 0 || index >= survivalSelectedRewardItems.length) return;
  survivalSelectedRewardItems.splice(index, 1);
  survivalRewardItemsDirty = true;
  renderSurvivalSelectedItems();
}

function addSurvivalRewardItem(name) {
  const normalized = normalizeSurvivalItemKey(name);
  if (!normalized) return;
  const existing = survivalSelectedRewardItems.find(item => item.name === normalized);
  if (existing) {
    const current = parseStrictIntInput(existing.amount);
    existing.amount = String((current === null ? 1 : current) + 1);
  } else {
    const suggestion = findSurvivalRewardSuggestion(normalized);
    survivalSelectedRewardItems.push({
      name: normalized,
      displayName: suggestion && suggestion.displayName || normalized,
      category: suggestion && suggestion.category || '',
      image: suggestion && suggestion.image || '',
      amount: '1',
      expiration: '',
      durability: ''
    });
  }
  survivalRewardItemsDirty = true;
  renderSurvivalSelectedItems();
  setSurvivalResult(`Item adicionado: ${normalized}`, false);
}

function setSurvivalItemFilter(filter) {
  survivalItemFilter = ['box', 'item', 'all'].includes(filter) ? filter : 'box';
  document.querySelectorAll('[data-survival-item-filter]').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.survivalItemFilter === survivalItemFilter);
  });
  renderSurvivalItemSearch();
}

function renderSurvivalItemSearch() {
  const list = $('survival-item-results');
  if (!list) return;
  const q = String($('survival-item-search') && $('survival-item-search').value || '').trim().toLowerCase();
  const source = survivalRewardItemSuggestions.filter(item => {
    const kind = survivalRewardItemKind(item);
    if (survivalItemFilter === 'box' && kind !== 'WARBOX') return false;
    if (survivalItemFilter === 'item' && kind === 'WARBOX') return false;
    if (!q) return true;
    return String(item.key || '').toLowerCase().includes(q) ||
      String(item.displayName || '').toLowerCase().includes(q) ||
      String(item.category || '').toLowerCase().includes(q);
  }).slice(0, 48);
  if (!source.length) {
    list.innerHTML = '<div class="empty-state">Nenhum item encontrado</div>';
    return;
  }
  list.innerHTML = source.map(item => {
    const kind = survivalRewardItemKind(item);
    const encodedKey = encodeURIComponent(item.key || '');
    return `<button class="survival-item-result" type="button" onclick="addSurvivalRewardItem(decodeURIComponent('${encodedKey}'))">
      <img class="survival-item-thumb" src="${survivalEscAttr(survivalRewardItemImage(item))}" alt="" onerror="fallbackItemImage(this, '${survivalEscAttr(item.key)}')" />
      <span class="survival-item-kind">${kind}</span>
      <span class="survival-item-name">${esc(item.displayName || item.key)}</span>
      <span class="survival-item-key">${esc(item.key)}</span>
      <span class="survival-item-add">Adicionar</span>
    </button>`;
  }).join('');
}

function renderSurvivalRewards() {
  return rewardsSurvivalDomain && rewardsSurvivalDomain.renderSurvivalRewards
    ? rewardsSurvivalDomain.renderSurvivalRewards()
    : undefined;
}

async function loadSurvivalRewards(force) {
  return rewardsSurvivalDomain && rewardsSurvivalDomain.loadSurvivalRewards
    ? rewardsSurvivalDomain.loadSurvivalRewards(force)
    : undefined;
}

function selectVisibleSurvivalRewards(checked) {
  document.querySelectorAll('#survival-rewards-list .survival-reward-check').forEach(input => {
    input.checked = !!checked;
  });
  updateSurvivalSelectedCount();
}

function populateSurvivalTypeDropdown() {
  return rewardsSurvivalDomain && rewardsSurvivalDomain.populateSurvivalTypeDropdown
    ? rewardsSurvivalDomain.populateSurvivalTypeDropdown()
    : undefined;
}

function loadSelectedSurvivalType() {
  return rewardsSurvivalDomain && rewardsSurvivalDomain.loadSelectedSurvivalType
    ? rewardsSurvivalDomain.loadSelectedSurvivalType()
    : undefined;
}

async function saveSelectedSurvivalType() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.saveSelectedSurvivalType
    ? rewardsSurvivalActionsDomain.saveSelectedSurvivalType()
    : undefined;
}

function getSelectedSurvivalMissionTypes() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.getSelectedSurvivalMissionTypes
    ? rewardsSurvivalActionsDomain.getSelectedSurvivalMissionTypes()
    : [];
}

function setSurvivalInput(id, value) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.setSurvivalInput
    ? rewardsSurvivalActionsDomain.setSurvivalInput(id, value)
    : undefined;
}

function formatSecondsAsMinutes(value) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.formatSecondsAsMinutes
    ? rewardsSurvivalActionsDomain.formatSecondsAsMinutes(value)
    : "";
}

function minutesToSecondsString(value) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.minutesToSecondsString
    ? rewardsSurvivalActionsDomain.minutesToSecondsString(value)
    : "";
}

function useSurvivalRewardAsDraft(index) {
  const row = survivalRewardsCache[index];
  if (!row) return;
  const sample = row.missions && row.missions.sample ? row.missions.sample : null;
  const pools = sample && sample.pools ? sample.pools : {};
  const crownCfg = row.crown || {};
  const items = survivalRewardItems(row);
  setSurvivalSelectedRewardItems(items, false);
  setSurvivalInput('survival-reward-item', '');
  setSurvivalInput('survival-reward-item-amount', '');
  setSurvivalInput('survival-reward-item-expiration', '');
  setSurvivalInput('survival-reward-item-durability', '');
  setSurvivalInput('survival-reward-gp', survivalRewardMoney(row, 'game_money') || '');
  setSurvivalInput('survival-reward-cash', survivalRewardMoney(row, 'cry_money') || '');
  setSurvivalInput('survival-reward-crown', survivalRewardMoney(row, 'crown_money') || '');
  setSurvivalInput('survival-money-multiplier', row.multipliers && row.multipliers.gp || '');
  setSurvivalInput('survival-xp-multiplier', row.multipliers && row.multipliers.xp || '');
  setSurvivalInput('survival-cash-multiplier', row.multipliers && row.multipliers.cash || '');
  setSurvivalInput('survival-crown-bronze', crownCfg.bronze || '');
  setSurvivalInput('survival-crown-silver', crownCfg.silver || '');
  setSurvivalInput('survival-crown-gold', crownCfg.gold || '');
  setSurvivalInput('survival-win-pool', pools.win || '');
  setSurvivalInput('survival-lose-pool', pools.lose || '');
  setSurvivalInput('survival-draw-pool', pools.draw || '');
  setSurvivalInput('survival-score-pool', pools.score || '');
  setSurvivalInput('survival-reward-pool', sample && sample.rewardPools && sample.rewardPools.max !== null ? sample.rewardPools.max : '');
  setSurvivalInput('survival-bonus-pool', row.bonusPool || '');
  setSurvivalInput('survival-score-bronze', survivalThresholdValue(row, 'score', 'bronze'));
  setSurvivalInput('survival-score-silver', survivalThresholdValue(row, 'score', 'silver'));
  setSurvivalInput('survival-score-gold', survivalThresholdValue(row, 'score', 'gold'));
  setSurvivalInput('survival-time-bronze', formatSecondsAsMinutes(survivalThresholdValue(row, 'time', 'bronze')));
  setSurvivalInput('survival-time-silver', formatSecondsAsMinutes(survivalThresholdValue(row, 'time', 'silver')));
  setSurvivalInput('survival-time-gold', formatSecondsAsMinutes(survivalThresholdValue(row, 'time', 'gold')));
  const sel = $('survival-type-select');
  if (sel) sel.value = row.missionType;
  renderSurvivalMissionPreview(row, survivalSelectedRewardItems);
  renderSurvivalRewards();
  updateSurvivalSelectedCount();
  setSurvivalResult(`Valores carregados de ${survivalMissionDisplayName(row)}`, false);
}

function collectSurvivalRewardFields() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.collectSurvivalRewardFields
    ? rewardsSurvivalActionsDomain.collectSurvivalRewardFields()
    : {};
}

async function saveSurvivalRewards(applyAll) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.saveSurvivalRewards
    ? rewardsSurvivalActionsDomain.saveSurvivalRewards(applyAll)
    : undefined;
}

let globalRewardPanelOpen = true;

function toggleGlobalRewardPanel() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.toggleGlobalRewardPanel
    ? rewardsSurvivalActionsDomain.toggleGlobalRewardPanel()
    : undefined;
}

function setGlobalInput(id, value) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.setGlobalInput
    ? rewardsSurvivalActionsDomain.setGlobalInput(id, value)
    : undefined;
}

function collectGlobalRewardFields() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.collectGlobalRewardFields
    ? rewardsSurvivalActionsDomain.collectGlobalRewardFields()
    : {};
}

function clearGlobalFields() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.clearGlobalFields
    ? rewardsSurvivalActionsDomain.clearGlobalFields()
    : undefined;
}

function applyGlobalPreset(tier) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.applyGlobalPreset
    ? rewardsSurvivalActionsDomain.applyGlobalPreset(tier)
    : undefined;
}

function setGlobalResult(message, isError) {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.setGlobalResult
    ? rewardsSurvivalActionsDomain.setGlobalResult(message, isError)
    : undefined;
}

async function applyGlobalReward() {
  return rewardsSurvivalActionsDomain && rewardsSurvivalActionsDomain.applyGlobalReward
    ? rewardsSurvivalActionsDomain.applyGlobalReward()
    : undefined;
}

function clampShopInt(n, min, max) {
  return Math.max(min, Math.min(max, n));
}

function normalizeShopOfferForPackage(raw, fallbackIndex) {
  if (!raw || typeof raw !== 'object') return null;
  const fallbackId = Number.isSafeInteger(fallbackIndex) ? (fallbackIndex + 1) : 1;
  const idParsed = parseStrictIntInput(raw.id);
  const id = idParsed === null ? fallbackId : idParsed;
  if (!Number.isSafeInteger(id)) return null;
  const name = String(raw.name || '').trim().toLowerCase();
  if (!/^[a-z0-9_]{2,120}$/i.test(name)) return null;

  const toBoundedInt = (v, min, max, fallback) => {
    const parsed = parseStrictIntInput(v);
    if (parsed === null) return fallback;
    return clampShopInt(parsed, min, max);
  };

  const gamePrice = toBoundedInt(raw.game_price, 0, 2147483647, 0);
  const cryPrice = toBoundedInt(raw.cry_price, 0, 2147483647, 0);
  const crownPrice = toBoundedInt(raw.crown_price, 0, 2147483647, 0);
  const durability = toBoundedInt(raw.durabilityPoints, 0, 1000000, 0);
  const quantity = toBoundedInt(raw.quantity, 0, 999999, 0);

  let offerStatus = String(raw.offer_status || 'NORMAL').trim();
  const validStatus = new Set(['NORMAL', 'NEW', 'HOT', 'enabled', 'limited']);
  if (!validStatus.has(offerStatus)) offerStatus = 'NORMAL';

  const clean = Object.assign({}, raw);
  clean.id = id;
  clean.name = name;
  clean.game_price = gamePrice;
  clean.cry_price = cryPrice;
  clean.crown_price = crownPrice;
  clean.offer_status = offerStatus;
  clean.durabilityPoints = durability;
  clean.quantity = quantity;
  clean.game_price_origin = toBoundedInt(raw.game_price_origin, 0, 2147483647, gamePrice);
  clean.cry_price_origin = toBoundedInt(raw.cry_price_origin, 0, 2147483647, cryPrice);
  clean.crown_price_origin = toBoundedInt(raw.crown_price_origin, 0, 2147483647, crownPrice);
  clean.repair_cost = toBoundedInt(raw.repair_cost, 0, 2147483647, 0);
  clean.supplier_id = toBoundedInt(raw.supplier_id, 0, 999999, 1);
  clean.rank = toBoundedInt(raw.rank, 0, 90, 0);
  clean.discount = toBoundedInt(raw.discount, 0, 2147483647, 0);
  clean.sorting_index = toBoundedInt(raw.sorting_index, 0, 2147483647, 0);
  ['key_item_name', 'expirationTime', 'item_category_override'].forEach(field => {
    clean[field] = String(raw[field] || '').trim();
  });
  return clean;
}

function shopBuilderOfferKey(offer) {
  return `${offer.id}|${offer.name}`;
}

function getShopBuilderOffers() {
  const out = Array.from(shopPackageBuilderMap.values());
  out.sort((a, b) => (a.id - b.id) || a.name.localeCompare(b.name));
  return out;
}

function renderShopBuilderList() {
  const listEl = $('shop-builder-list');
  const statsEl = $('shop-builder-stats');
  const offers = getShopBuilderOffers();
  if (statsEl) statsEl.textContent = `${offers.length} itens no pacote em montagem`;
  if (!listEl) return;
  if (!offers.length) {
    listEl.innerHTML = '<div class="empty-state">Adicione itens da tabela para montar o pacote</div>';
    return;
  }
  const itemNames = window._itemNamesCache || {};
  listEl.innerHTML = offers.map((o, idx) => {
    const base = normalizeItemBaseKey(o.name);
    const displayName = itemNames[o.name] || itemNames[base] || o.name;
    return `<div class="shop-builder-item">
      <div class="shop-builder-item-main">
        <strong title="${esc(displayName)}">${esc(displayName)}</strong>
        <span>${esc(o.name)} | ID ${o.id} | G:${o.game_price} VP:${o.cry_price} C:${o.crown_price}</span>
      </div>
      <button class="sc-btn shop-btn shop-btn-deactivate shop-builder-remove" onclick="removeShopBuilderOffer(${idx})">REM</button>
    </div>`;
  }).join('');
}

function removeShopBuilderOffer(index) {
  const offers = getShopBuilderOffers();
  const target = offers[index];
  if (!target) return;
  shopPackageBuilderMap.delete(shopBuilderOfferKey(target));
  renderShopBuilderList();
}

function setShopBuilderOffers(rawOffers) {
  shopPackageBuilderMap.clear();
  const offers = Array.isArray(rawOffers) ? rawOffers : [];
  offers.forEach((raw, idx) => {
    const clean = normalizeShopOfferForPackage(raw, idx);
    if (!clean) return;
    shopPackageBuilderMap.set(shopBuilderOfferKey(clean), clean);
  });
  renderShopBuilderList();
}

function readShopOfferFromRow(rowIndex) {
  const source = shopLastPageOffers[rowIndex];
  if (!source) return null;
  if (String(source.id) === String(shopSelectedOfferId)) {
    const draft = readSelectedShopOfferDraft();
    if (draft) return normalizeShopOfferForPackage(draft, rowIndex);
  }
  return normalizeShopOfferForPackage(source, rowIndex);
}

function addShopOfferFromList(rowIndex) {
  const clean = readShopOfferFromRow(rowIndex);
  if (!clean) {
    setShopPackageResult('Nao foi possivel adicionar o offer ao pacote', true);
    return;
  }
  const key = shopBuilderOfferKey(clean);
  const already = shopPackageBuilderMap.has(key);
  shopPackageBuilderMap.set(key, clean);
  renderShopBuilderList();
  setShopPackageResult(already ? `Item ${clean.name} ja estava no pacote` : `Item ${clean.name} adicionado ao pacote`, false);
}

function addCurrentShopPageToPackage() {
  let added = 0;
  for (let i = 0; i < shopLastPageOffers.length; i += 1) {
    const clean = readShopOfferFromRow(i);
    if (!clean) continue;
    const key = shopBuilderOfferKey(clean);
    const already = shopPackageBuilderMap.has(key);
    shopPackageBuilderMap.set(key, clean);
    if (!already) added += 1;
  }
  renderShopBuilderList();
  setShopPackageResult(`${added} itens adicionados da pagina atual`, false);
}

function clearShopPackageBuilder() {
  shopPackageBuilderMap.clear();
  renderShopBuilderList();
  setShopPackageResult('Pacote em montagem limpo', false);
}

function switchShopSubtab(name) {
  const next = ['offers', 'packages', 'rotation', 'catalog'].includes(name) ? name : 'offers';
  currentShopSubtab = next;
  document.querySelectorAll('.shop-subtab').forEach(btn => btn.classList.toggle('active', btn.dataset.shopSubtab === next));
  document.querySelectorAll('.shop-subpanel').forEach(panel => panel.classList.toggle('active', panel.id === `shop-subpanel-${next}`));
  if (next === 'packages' || next === 'rotation') loadShopPackagesAndRotation();
  if (next === 'catalog' && !$('shop-catalog-list')?.dataset.loaded) loadShopCatalog(true);
}

function isShopOfferActive(o) {
  const status = String(o && o.offer_status || '').toLowerCase();
  return !status || status === 'normal' || status === 'enabled' || status === 'active' || status === 'new' || status === 'hot';
}

function shopOfferStatusLabel(o) {
  const status = String(o && o.offer_status || '').trim();
  if (!status) return 'NORMAL';
  return status;
}

function getShopOfferById(id) {
  return shopLastPageOffers.find(o => String(o.id) === String(id)) || null;
}

function updateShopPagingUI() {
  const from = shopTotalOffers ? shopPageOffset + 1 : 0;
  const to = Math.min(shopPageOffset + shopLastPageOffers.length, shopTotalOffers);
  const page = Math.floor(shopPageOffset / SHOP_PAGE_SIZE) + 1;
  const pages = Math.max(1, Math.ceil(shopTotalOffers / SHOP_PAGE_SIZE));
  if ($('shop-offers-count')) $('shop-offers-count').textContent = `${shopTotalOffers} ofertas (${from}-${to})`;
  if ($('shop-page-label')) $('shop-page-label').textContent = `Pagina ${page}/${pages}`;
  if ($('shop-prev-page')) $('shop-prev-page').disabled = shopPageOffset <= 0;
  if ($('shop-next-page')) $('shop-next-page').disabled = shopPageOffset + SHOP_PAGE_SIZE >= shopTotalOffers;
}

function priceChip(label, value, cls) {
  return `<span class="price-chip ${cls}"><b>${label}</b>${num(Number(value) || 0)}</span>`;
}

function renderShopOfferList() {
  const list = $('shop-list');
  if (!list) return;
  updateShopPagingUI();
  if (!shopLastPageOffers.length) {
    list.innerHTML = '<div class="empty-state">Nenhuma oferta encontrada</div>';
    renderShopOfferEditor(null);
    return;
  }
  if (!getShopOfferById(shopSelectedOfferId)) shopSelectedOfferId = shopLastPageOffers[0].id;
  list.innerHTML = shopLastPageOffers.map((o, idx) => {
    const displayName = shopOfferDisplayName(o);
    const visualName = weaponVisualTitle(o.name, displayName);
    const visualImg = weaponVisualImage(o.name);
    const active = isShopOfferActive(o);
    const selected = String(o.id) === String(shopSelectedOfferId);
    return `<div class="shop-offer-row ${selected ? 'selected' : ''}" data-offer-id="${esc(o.id)}">
      <button class="shop-offer-main" type="button" onclick="selectShopOffer(${idx})">
        <img class="shop-item-thumb" src="${esc(visualImg)}" onerror="fallbackItemImage(this, '${esc(o.name)}')" />
        <span class="shop-item-meta">
          <span class="shop-item-name">${esc(visualName)}</span>
          <span class="shop-item-key">#${esc(o.id)} � ordem ${esc(Number(o.sorting_index) || idx + 1)} � ${esc(o.name)}</span>
        </span>
      </button>
      <div class="shop-offer-prices">
        ${priceChip('G', o.game_price, 'game')}
        ${priceChip('VP', o.cry_price, 'vp')}
        ${priceChip('C', o.crown_price, 'crown')}
      </div>
      <span class="shop-status-pill ${active ? 'active' : 'inactive'}">${esc(shopOfferStatusLabel(o))}</span>
      <div class="shop-row-actions">
        <button class="sc-btn shop-btn shop-btn-save" type="button" onclick="selectShopOffer(${idx})">EDITAR</button>
        <button class="sc-btn shop-btn ${active ? 'shop-btn-deactivate' : 'shop-btn-activate'}" type="button" onclick="toggleShopOffer(${o.id}, '${active ? 'limited' : 'enabled'}')">${active ? 'DESAT.' : 'ATIVAR'}</button>
        <button class="sc-btn shop-btn shop-btn-package" type="button" onclick="addShopOfferFromList(${idx})">+ PAC</button>
      </div>
    </div>`;
  }).join('');
  renderShopOfferEditor(getShopOfferById(shopSelectedOfferId));
}

function shopOfferDisplayName(offer) {
  const itemNames = window._itemNamesCache || {};
  const base = normalizeItemBaseKey(offer && offer.name);
  return itemNames[offer && offer.name] || itemNames[base] || (offer && offer.name) || '';
}

function selectShopOffer(index) {
  const offer = shopLastPageOffers[index];
  if (!offer) return;
  shopSelectedOfferId = offer.id;
  renderShopOfferList();
}

function readSelectedShopOfferDraft() {
  const offer = getShopOfferById(shopSelectedOfferId);
  if (!offer) return null;
  const gp = parseStrictIntInput($('shop-edit-gp')?.value);
  const cp = parseStrictIntInput($('shop-edit-cp')?.value);
  const crp = parseStrictIntInput($('shop-edit-crp')?.value);
  const orderEl = $('shop-edit-order');
  const order = orderEl ? parseStrictIntInput(orderEl.value) : (parseStrictIntInput(offer.sorting_index) || 0);
  if (gp === null || cp === null || crp === null || order === null) return null;
  return Object.assign({}, offer, {
    game_price: gp,
    cry_price: cp,
    crown_price: crp,
    sorting_index: order,
    offer_status: $('shop-edit-status')?.value || offer.offer_status || 'NORMAL'
  });
}

function renderShopOfferEditor(offer) {
  const el = $('shop-offer-editor');
  if (!el) return;
  if (!offer) {
    el.innerHTML = '<div class="empty-state">Selecione uma oferta para editar preco, status ou adicionar ao pacote.</div>';
    return;
  }
  const displayName = shopOfferDisplayName(offer);
  const visualName = weaponVisualTitle(offer.name, displayName);
  const visualImg = weaponVisualImage(offer.name);
  const active = isShopOfferActive(offer);
  const status = shopOfferStatusLabel(offer);
  el.innerHTML = `<div class="shop-editor-card">
    <div class="shop-editor-visual">
      <img src="${esc(visualImg)}" onerror="fallbackItemImage(this, '${esc(offer && offer.name || '')}')" />
      <div>
        <div class="shop-editor-title">${esc(visualName)}</div>
        <div class="shop-editor-key">#${esc(offer.id)} � ${esc(offer.name)}</div>
      </div>
    </div>
    <div class="shop-edit-grid">
      <div class="form-group">
        <label class="form-label">Ouro</label>
        <input type="number" class="form-input" id="shop-edit-gp" value="${Number(offer.game_price) || 0}" min="0" />
      </div>
      <div class="form-group">
        <label class="form-label">VP</label>
        <input type="number" class="form-input" id="shop-edit-cp" value="${Number(offer.cry_price) || 0}" min="0" />
      </div>
      <div class="form-group">
        <label class="form-label">Coroas</label>
        <input type="number" class="form-input" id="shop-edit-crp" value="${Number(offer.crown_price) || 0}" min="0" />
      </div>
      <div class="form-group">
        <label class="form-label">Ordem</label>
        <input type="number" class="form-input" id="shop-edit-order" value="${Number(offer.sorting_index) || Number(offer.id) || 0}" min="0" />
      </div>
      <div class="form-group">
        <label class="form-label">Status</label>
        <select class="form-input" id="shop-edit-status">
          <option value="NORMAL"${status === 'NORMAL' ? ' selected' : ''}>NORMAL</option>
          <option value="NEW"${status === 'NEW' ? ' selected' : ''}>NEW</option>
          <option value="HOT"${status === 'HOT' ? ' selected' : ''}>HOT</option>
          <option value="limited"${!active ? ' selected' : ''}>Inativo</option>
        </select>
      </div>
    </div>
    <div class="shop-editor-actions">
      <button class="sc-btn shop-btn shop-btn-save" id="shop-save-selected" type="button" onclick="updateShopOffer(${offer.id})">SALVAR</button>
      <button class="sc-btn shop-btn ${active ? 'shop-btn-deactivate' : 'shop-btn-activate'}" type="button" onclick="toggleShopOffer(${offer.id}, '${active ? 'limited' : 'enabled'}')">${active ? 'DESATIVAR' : 'ATIVAR'}</button>
      <button class="sc-btn shop-btn shop-btn-package" type="button" onclick="addShopSelectedOfferToPackage()">ADD PACOTE</button>
    </div>
  </div>`;
}

function addShopSelectedOfferToPackage() {
  const idx = shopLastPageOffers.findIndex(o => String(o.id) === String(shopSelectedOfferId));
  if (idx >= 0) addShopOfferFromList(idx);
}

function renderShopPackagesSelect() {
  return shopPackagesDomain && shopPackagesDomain.renderShopPackagesSelect
    ? shopPackagesDomain.renderShopPackagesSelect()
    : undefined;
}

function renderSelectedShopPackageMeta() {
  return shopPackagesDomain && shopPackagesDomain.renderSelectedShopPackageMeta
    ? shopPackagesDomain.renderSelectedShopPackageMeta()
    : undefined;
}

function renderShopRotationMeta() {
  return shopPackagesDomain && shopPackagesDomain.renderShopRotationMeta
    ? shopPackagesDomain.renderShopRotationMeta()
    : undefined;
}

function applyRotationToInputs() {
  return shopPackagesDomain && shopPackagesDomain.applyRotationToInputs
    ? shopPackagesDomain.applyRotationToInputs()
    : undefined;
}

async function loadShopPackagesAndRotation() {
  return shopPackagesDomain && shopPackagesDomain.loadShopPackagesAndRotation
    ? shopPackagesDomain.loadShopPackagesAndRotation()
    : undefined;
}

async function saveShopPackage(updateExisting) {
  return shopPackagesDomain && shopPackagesDomain.saveShopPackage
    ? shopPackagesDomain.saveShopPackage(updateExisting)
    : undefined;
}

function getSelectedShopPackageId() {
  return shopPackagesDomain && shopPackagesDomain.getSelectedShopPackageId
    ? shopPackagesDomain.getSelectedShopPackageId()
    : "";
}

async function loadSelectedShopPackage() {
  return shopPackagesDomain && shopPackagesDomain.loadSelectedShopPackage
    ? shopPackagesDomain.loadSelectedShopPackage()
    : undefined;
}

async function applySelectedShopPackage(mode) {
  return shopPackagesDomain && shopPackagesDomain.applySelectedShopPackage
    ? shopPackagesDomain.applySelectedShopPackage(mode)
    : undefined;
}

async function deleteSelectedShopPackage() {
  return shopPackagesDomain && shopPackagesDomain.deleteSelectedShopPackage
    ? shopPackagesDomain.deleteSelectedShopPackage()
    : undefined;
}

function getSelectedRotationPackageIds() {
  return shopPackagesDomain && shopPackagesDomain.getSelectedRotationPackageIds
    ? shopPackagesDomain.getSelectedRotationPackageIds()
    : [];
}

async function saveShopRotation(runNow) {
  return shopPackagesDomain && shopPackagesDomain.saveShopRotation
    ? shopPackagesDomain.saveShopRotation(runNow)
    : undefined;
}

async function runShopRotationNow() {
  return shopPackagesDomain && shopPackagesDomain.runShopRotationNow
    ? shopPackagesDomain.runShopRotationNow()
    : undefined;
}

async function loadShopOffers(resetOffset) {
  return shopOffersDomain && shopOffersDomain.loadShopOffers
    ? shopOffersDomain.loadShopOffers(resetOffset)
    : undefined;
}

function shopPage(dir) {
  return shopOffersDomain && shopOffersDomain.shopPage
    ? shopOffersDomain.shopPage(dir)
    : undefined;
}

async function updateShopOffer(id) {
  shopSelectedOfferId = id;
  const draft = readSelectedShopOfferDraft();
  const gp = draft ? parseStrictIntInput(draft.game_price) : null;
  const cp = draft ? parseStrictIntInput(draft.cry_price) : null;
  const crp = draft ? parseStrictIntInput(draft.crown_price) : null;
  const order = draft ? parseStrictIntInput(draft.sorting_index) : null;
  if (gp === null || cp === null || crp === null || order === null) {
    setShopPackageResult('Precos ou ordem invalidos para este offer', true);
    return;
  }
  setBusy('shop-save-selected', true, 'SALVANDO');
  try {
    const r = await fetch('/api/shop/offer/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Auth-Token': token },
      body: JSON.stringify({ id: Number(id), game_price: gp, cry_price: cp, crown_price: crp, sorting_index: order, offer_status: draft.offer_status }),
    });
    const d = await r.json();
    if (d.success) {
      setShopPackageResult('Offer atualizado', false);
      const idx = shopLastPageOffers.findIndex(o => String(o.id) === String(id));
      if (idx >= 0) {
        shopLastPageOffers[idx] = Object.assign({}, shopLastPageOffers[idx], draft);
        renderShopOfferList();
        flashShopOffer(id);
      }
    } else {
      setShopPackageResult(d.error || 'Falha ao atualizar offer', true);
    }
  } catch (e) {
    setShopPackageResult(`Falha ao atualizar offer: ${e.message}`, true);
  } finally {
    setBusy('shop-save-selected', false);
  }
}

async function toggleShopOffer(id, newStatus) {
  try {
    const r = await fetch('/api/shop/offer/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Auth-Token': token },
      body: JSON.stringify({ id: Number(id), offer_status: newStatus }),
    });
    const d = await r.json();
    if (d.success) {
      setShopPackageResult('Status do offer atualizado', false);
      const idx = shopLastPageOffers.findIndex(o => String(o.id) === String(id));
      if (idx >= 0) {
        shopLastPageOffers[idx] = Object.assign({}, shopLastPageOffers[idx], { offer_status: newStatus === 'enabled' ? 'NORMAL' : 'limited' });
        renderShopOfferList();
        flashShopOffer(id);
      }
    } else {
      setShopPackageResult(d.error || 'Falha ao atualizar status', true);
    }
  } catch (e) {
    setShopPackageResult(`Falha ao atualizar status: ${e.message}`, true);
  }
}

async function regenerateShopCache() {
  setBusy('shop-regenerate-btn', true, 'CACHE');
  try {
    const r = await fetch('/api/shop/regenerate', { headers: { 'X-Auth-Token': token } });
    const d = await r.json();
    $('shop-stats').textContent = 'Cache sendo regenerado... (inicie o servico Cache Shop)';
    setShopPackageResult(d.message || 'Solicitacao de regeneracao enviada', !d.success);
  } catch (e) {
    setShopPackageResult(`Falha ao regenerar cache: ${e.message}`, true);
  } finally {
    setBusy('shop-regenerate-btn', false);
  }
}

async function clearActiveShop() {
  if (!confirmDanger('Isso vai remover TODAS as ofertas da loja ativa. Pacotes salvos nao serao apagados. Continuar?')) return;
  const typed = window.prompt('Para confirmar, digite EXCLUIR');
  if (String(typed || '').trim().toUpperCase() !== 'EXCLUIR') {
    setShopPackageResult('Limpeza da loja cancelada', true);
    return;
  }
  setBusy('shop-clear-btn', true, 'EXCLUINDO');
  try {
    const r = await fetch('/api/shop/clear', {
      method: 'POST',
      headers: { 'X-Auth-Token': token }
    });
    const d = await r.json();
    if (!d.success) {
      setShopPackageResult(d.error || 'Falha ao limpar loja', true);
      return;
    }
    shopLastPageOffers = [];
    shopTotalOffers = 0;
    shopSelectedOfferId = null;
    shopPageOffset = 0;
    renderShopOfferList();
    $('shop-stats').textContent = '0 ofertas | pacote atual: ' + getShopBuilderOffers().length + ' itens';
    setShopPackageResult(d.message || 'Loja limpa', false);
  } catch (e) {
    setShopPackageResult(`Falha ao limpar loja: ${e.message}`, true);
  } finally {
    setBusy('shop-clear-btn', false);
  }
}

function flashShopOffer(id) {
  const row = document.querySelector(`.shop-offer-row[data-offer-id="${CSS.escape(String(id))}"]`);
  if (!row) return;
  row.classList.add('just-updated');
  setTimeout(() => row.classList.remove('just-updated'), 1200);
}

async function loadShopCatalog(resetOffset) {
  return shopOffersDomain && shopOffersDomain.loadShopCatalog
    ? shopOffersDomain.loadShopCatalog(resetOffset)
    : undefined;
}

function renderShopCatalog(items) {
  const list = $('shop-catalog-list');
  if (!list) return;
  const from = shopCatalogTotal ? shopCatalogOffset + 1 : 0;
  const to = Math.min(shopCatalogOffset + items.length, shopCatalogTotal);
  const page = Math.floor(shopCatalogOffset / SHOP_CATALOG_PAGE_SIZE) + 1;
  const pages = Math.max(1, Math.ceil(shopCatalogTotal / SHOP_CATALOG_PAGE_SIZE));
  if ($('shop-catalog-count')) $('shop-catalog-count').textContent = `${shopCatalogTotal} itens (${from}-${to})`;
  if ($('shop-catalog-page-label')) $('shop-catalog-page-label').textContent = `Pagina ${page}/${pages}`;
  if ($('shop-catalog-prev')) $('shop-catalog-prev').disabled = shopCatalogOffset <= 0;
  if ($('shop-catalog-next')) $('shop-catalog-next').disabled = shopCatalogOffset + SHOP_CATALOG_PAGE_SIZE >= shopCatalogTotal;
  if (!items.length) {
    list.innerHTML = '<div class="empty-state">Nenhum item encontrado no catalogo</div>';
    return;
  }
  list.innerHTML = items.map(item => {
    const img = (item.visual && item.visual.image) || item.image || '/img/weapons/_default.png';
    const name = item.wikiName || item.displayName || item.key;
    return `<div class="shop-catalog-card">
      <img src="${esc(img)}" onerror="fallbackItemImage(this, '${esc(item.key)}')" />
      <div class="shop-catalog-main">
        <div class="shop-item-name">${esc(name)}</div>
        <div class="shop-item-key">${esc(item.key)} � ${esc(item.type || 'other')}</div>
      </div>
      <span class="shop-status-pill ${item.inShop ? 'active' : 'inactive'}">${item.inShop ? 'Na loja' : 'Fora'}</span>
    </div>`;
  }).join('');
}

function shopCatalogPage(dir) {
  return shopOffersDomain && shopOffersDomain.shopCatalogPage
    ? shopOffersDomain.shopCatalogPage(dir)
    : undefined;
}

async function getItemNames() {
  if (window._itemNamesCache) return window._itemNamesCache;
  try {
    const r = await fetch('/api/weapons/names', { headers: { 'X-Auth-Token': token } });
    const d = await r.json();
    window._itemNamesCache = d.success ? (d.names || {}) : {};
    return window._itemNamesCache;
  } catch { return {}; }
}

// ─── Search ──────────────────────────────────────────────────────────────
async function searchPlayers() {
  return playersModerationDomain && playersModerationDomain.searchPlayers
    ? playersModerationDomain.searchPlayers()
    : undefined;
}

// ─── Chat Logs ────────────────────────────────────────────────────────────
let chatLogsInterval = null;

async function loadChatLogs() {
  return playersModerationDomain && playersModerationDomain.loadChatLogs
    ? playersModerationDomain.loadChatLogs()
    : undefined;
}

function autoRefreshChatLogs() {
  return playersModerationDomain && playersModerationDomain.autoRefreshChatLogs
    ? playersModerationDomain.autoRefreshChatLogs()
    : undefined;
}

// ─── Performance ─────────────────────────────────────────────────────────
let perfInterval = null;
async function loadPerfStats() {
  return servicesPerformanceDomain && servicesPerformanceDomain.loadPerfStats
    ? servicesPerformanceDomain.loadPerfStats()
    : undefined;
}

async function loadPerfCharts() {
  return servicesPerformanceDomain && servicesPerformanceDomain.loadPerfCharts
    ? servicesPerformanceDomain.loadPerfCharts()
    : undefined;
}

function drawChart(canvasId, data, getVal, label, color, unit) {
  return servicesPerformanceDomain && servicesPerformanceDomain.drawChart
    ? servicesPerformanceDomain.drawChart(canvasId, data, getVal, label, color, unit)
    : undefined;
}

function initAppSecondaryBindings() {
  // Item search debounce
  $('item-search')?.addEventListener('input', () => {
    clearTimeout(itemSearchTimer);
    itemSearchTimer = setTimeout(searchItems, 300);
  });
  // Item suggestions load on focus
  $('item-name')?.addEventListener('focus', () => { loadItemSuggestions(); renderGiveItemSuggestions(); });
  $('item-name')?.addEventListener('input', () => {
    renderGiveItemPreview();
    renderGiveItemSuggestions();
  });
  $('item-name')?.addEventListener('blur', () => {
    setTimeout(hideGiveItemSuggestions, 120);
  });
  document.addEventListener('click', e => {
    const wrap = e.target && e.target.closest ? e.target.closest('.item-picker-wrap') : null;
    if (!wrap) hideGiveItemSuggestions();
  });
  // Chat Logs tab
  document.querySelector('.tab[data-tab="chatlogs"]')?.addEventListener('click', () => {
    if (chatLogsInterval) { clearInterval(chatLogsInterval); chatLogsInterval = null; }
  });
  $('chatlog-nick')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadChatLogs(); });
  // Performance tab
  if (servicesPerformanceDomain) servicesPerformanceDomain.bindPerformanceTab();
  else {
    document.querySelector('.tab[data-tab="performance"]')?.addEventListener('click', () => {
      loadPerfStats();
      loadPerfCharts();
      if (perfInterval) clearInterval(perfInterval);
      perfInterval = setInterval(loadPerfStats, 5000);
    });
  }
  // Ban tab
  document.querySelector('.tab[data-tab="bans"]')?.addEventListener('click', () => { setTimeout(loadBans, 100); setTimeout(loadIpBans, 200); });
  // Achievements tab
  document.querySelector('.tab[data-tab="achievements"]')?.addEventListener('click', () => setTimeout(loadAchievementLists, 80));
  $('ach-nick')?.addEventListener('input', () => {
    if (achLoadTimer) clearTimeout(achLoadTimer);
    achCatalogOffset = 0;
    achLoadTimer = setTimeout(loadAchievementLists, 280);
  });
  // Clans tab
  document.querySelector('.tab[data-tab="clans"]')?.addEventListener('click', () => setTimeout(loadClans, 100));
  // Game Rooms tab
  document.querySelector('.tab[data-tab="gamerooms"]')?.addEventListener('click', () => { if (gameroomInterval) { clearInterval(gameroomInterval); gameroomInterval = null; } });
  // Maintenance tab
  document.querySelector('.tab[data-tab="maintenance"]')?.addEventListener('click', () => setTimeout(loadMaintenance, 100));
  // Auto Broadcast tab
  document.querySelector('.tab[data-tab="autobroadcast"]')?.addEventListener('click', () => setTimeout(loadAutoBroadcast, 100));
  // Backup tab
  document.querySelector('.tab[data-tab="backup"]')?.addEventListener('click', () => setTimeout(listBackups, 100));
  // Shop tab
  $('shop-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadShopOffers(true); });
  $('shop-sort-mode')?.addEventListener('change', e => {
    shopSortMode = e.target.value || 'position';
    loadShopOffers(true);
  });
  document.querySelectorAll('#shop-status-filter .seg-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      shopStatusFilter = btn.dataset.status || 'all';
      document.querySelectorAll('#shop-status-filter .seg-btn').forEach(b => b.classList.toggle('active', b === btn));
      loadShopOffers(true);
    });
  });
  $('shop-packages-select')?.addEventListener('change', renderSelectedShopPackageMeta);
  $('shop-catalog-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadShopCatalog(true); });
  $('shop-catalog-type')?.addEventListener('change', () => loadShopCatalog(true));
  $('shop-catalog-shop')?.addEventListener('change', () => loadShopCatalog(true));
  $('survival-reward-filter')?.addEventListener('input', renderSurvivalRewards);
  renderShopBuilderList();
  // Search on enter
  $('search-term')?.addEventListener('keydown', e => { if (e.key === 'Enter') searchPlayers(); });
  $('ban-nick')?.addEventListener('keydown', e => { if (e.key === 'Enter') banPlayer(); });
  $('item-nick')?.addEventListener('keydown', e => { if (e.key === 'Enter') giveItem(); });
  $('bcast-msg')?.addEventListener('keydown', e => { if (e.key === 'Enter' && e.ctrlKey) sendBroadcast(); });
  $('notif-msg')?.addEventListener('keydown', e => { if (e.key === 'Enter' && e.ctrlKey) sendNotification(); });
  // Launcher tabs
  document.querySelector('.tab[data-tab="launcher"]')?.addEventListener('click', () => setTimeout(loadLauncherConfig, 100));
  document.querySelector('.tab[data-tab="launcher-news"]')?.addEventListener('click', () => setTimeout(loadLauncherConfig, 100));
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    initAppDomBindings();
    initAppSecondaryBindings();
  });
} else {
  initAppDomBindings();
  initAppSecondaryBindings();
}

// ─── Launcher Tab ──────────────────────────────────────────────────────────
let launcherConfig = { slides: [], news: [] };
let currentSlideIdx = -1;
let currentNewsIdx = -1;

function getLauncherResultEl() { return launcherCdnDomain && launcherCdnDomain.getLauncherResultEl ? launcherCdnDomain.getLauncherResultEl() : null; }
function setLauncherCounterText(text) { return launcherCdnDomain && launcherCdnDomain.setLauncherCounterText ? launcherCdnDomain.setLauncherCounterText(text) : undefined; }

async function loadLauncherConfig() { return launcherContentDomain && launcherContentDomain.loadLauncherConfig ? launcherContentDomain.loadLauncherConfig() : undefined; }
function renderHeroPreview() { return launcherContentDomain && launcherContentDomain.renderHeroPreview ? launcherContentDomain.renderHeroPreview() : undefined; }
function showSlide(idx) { return launcherContentDomain && launcherContentDomain.showSlide ? launcherContentDomain.showSlide(idx) : undefined; }
function prevSlide() { return launcherContentDomain && launcherContentDomain.prevSlide ? launcherContentDomain.prevSlide() : undefined; }
function nextSlide() { return launcherContentDomain && launcherContentDomain.nextSlide ? launcherContentDomain.nextSlide() : undefined; }
function selectSlide(idx) { return launcherContentDomain && launcherContentDomain.selectSlide ? launcherContentDomain.selectSlide(idx) : undefined; }
function syncSlideEditor() { return launcherContentDomain && launcherContentDomain.syncSlideEditor ? launcherContentDomain.syncSlideEditor() : undefined; }
function deleteCurrentSlide() { return launcherContentDomain && launcherContentDomain.deleteCurrentSlide ? launcherContentDomain.deleteCurrentSlide() : undefined; }
async function uploadSlideImage(input) { return launcherContentDomain && launcherContentDomain.uploadSlideImage ? launcherContentDomain.uploadSlideImage(input) : undefined; }
function addSlide() { return launcherContentDomain && launcherContentDomain.addSlide ? launcherContentDomain.addSlide() : undefined; }
function renderNewsPreview() { return launcherContentDomain && launcherContentDomain.renderNewsPreview ? launcherContentDomain.renderNewsPreview() : undefined; }
function selectNews(idx) { return launcherContentDomain && launcherContentDomain.selectNews ? launcherContentDomain.selectNews(idx) : undefined; }
function syncNewsEditor() { return launcherContentDomain && launcherContentDomain.syncNewsEditor ? launcherContentDomain.syncNewsEditor() : undefined; }
function deleteCurrentNews() { return launcherContentDomain && launcherContentDomain.deleteCurrentNews ? launcherContentDomain.deleteCurrentNews() : undefined; }
function addNews() { return launcherContentDomain && launcherContentDomain.addNews ? launcherContentDomain.addNews() : undefined; }

async function loadPatchHistory() {
  try {
    const r = await fetch('/api/public/game-update-history');
    const d = await r.json();
    const list = $('patch-history-list');
    if (!list) return;
    const updates = Array.isArray(d && d.updates) ? d.updates : [];
    if (!updates.length) {
      list.innerHTML = '<div class="patch-history-empty">Nenhuma atualiza??o carregada</div>';
      return;
    }
    list.innerHTML = updates.slice(0, 100).map((u) => {
      const version = esc(u.version || '--');
      const when = u.updatedAt ? new Date(u.updatedAt).toLocaleString('pt-BR') : '--';
      const notes = esc(u.notes || 'Sem notas');
      const changed = Number(u.changed_count || u.uploaded_count || 0).toLocaleString('pt-BR');
      const removed = Number(u.removed_count || 0).toLocaleString('pt-BR');
      return '<div class="patch-history-item">' +
        '<div class="patch-history-top"><strong>v' + version + '</strong><span>' + esc(when) + '</span></div>' +
        '<div class="patch-history-meta">Alterados: ' + changed + ' | Removidos: ' + removed + '</div>' +
        '<div class="patch-history-notes">' + notes + '</div>' +
      '</div>';
    }).join('');
  } catch {
    const list = $('patch-history-list');
    if (list) list.innerHTML = '<div class="patch-history-empty">Falha ao carregar hist?rico</div>';
  }
}

async function loadLauncherPatchHistory() {
  try {
    const r = await fetch('/api/public/launcher-update-history');
    const d = await r.json();
    const list = $('launcher-patch-history-list');
    if (!list) return;
    const updates = Array.isArray(d && d.updates) ? d.updates : [];
    if (!updates.length) {
      list.innerHTML = '<div class="patch-history-empty">Nenhuma atualiza??o carregada</div>';
      return;
    }
    list.innerHTML = updates.slice(0, 100).map((u) => {
      const version = esc(u.version || '--');
      const when = u.updatedAt ? new Date(u.updatedAt).toLocaleString('pt-BR') : '--';
      const notes = esc(u.notes || 'Sem notas');
      const changed = Number(u.changed_count || u.uploaded_count || 0).toLocaleString('pt-BR');
      const removed = Number(u.removed_count || 0).toLocaleString('pt-BR');
      return '<div class="patch-history-item">' +
        '<div class="patch-history-top"><strong>v' + version + '</strong><span>' + esc(when) + '</span></div>' +
        '<div class="patch-history-meta">Alterados: ' + changed + ' | Removidos: ' + removed + '</div>' +
        '<div class="patch-history-notes">' + notes + '</div>' +
      '</div>';
    }).join('');
  } catch {
    const list = $('launcher-patch-history-list');
    if (list) list.innerHTML = '<div class="patch-history-empty">Falha ao carregar hist?rico</div>';
  }
}

async function loadVersions() { return launcherCdnDomain && launcherCdnDomain.loadVersions ? launcherCdnDomain.loadVersions() : undefined; }
function showLauncherCdnPanel(panel) {
  const next = panel || 'reference';
  document.querySelectorAll('#launcher-cdn-content .gamefiles-subpanel').forEach(el => {
    el.classList.toggle('active', el.id === `launcher-cdn-panel-${next}`);
  });
  document.querySelectorAll('[data-launcher-subtab]').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.launcherSubtab === next);
  });
  if (next === 'reference') loadLauncherRefInfo();
  if (next === 'patches') loadLauncherPatchHistory();
}
async function loadLauncherVersions() { return launcherCdnDomain && launcherCdnDomain.loadLauncherVersions ? launcherCdnDomain.loadLauncherVersions() : undefined; }
async function saveLauncherVersion() { return launcherCdnDomain && launcherCdnDomain.saveLauncherVersion ? launcherCdnDomain.saveLauncherVersion() : undefined; }
function showLauncherVersionResult(msg, isError) { return launcherCdnDomain && launcherCdnDomain.showLauncherVersionResult ? launcherCdnDomain.showLauncherVersionResult(msg, isError) : undefined; }
async function loadLauncherRefInfo() { return launcherCdnDomain && launcherCdnDomain.loadLauncherRefInfo ? launcherCdnDomain.loadLauncherRefInfo() : undefined; }
function clearDevSyncProgressTimer(kind) { return launcherCdnDomain && launcherCdnDomain.clearDevSyncProgressTimer ? launcherCdnDomain.clearDevSyncProgressTimer(kind) : undefined; }
function renderDevSyncProgress(kind, progress = {}) { return launcherCdnDomain && launcherCdnDomain.renderDevSyncProgress ? launcherCdnDomain.renderDevSyncProgress(kind, progress) : undefined; }
async function pollDevSyncProgress(kind, force = false) { return launcherCdnDomain && launcherCdnDomain.pollDevSyncProgress ? launcherCdnDomain.pollDevSyncProgress(kind, force) : undefined; }
function startDevSyncProgressPolling(kind) { return launcherCdnDomain && launcherCdnDomain.startDevSyncProgressPolling ? launcherCdnDomain.startDevSyncProgressPolling(kind) : undefined; }

async function browseLauncherSourceDir() { return launcherSyncDomain && launcherSyncDomain.browseLauncherSourceDir ? launcherSyncDomain.browseLauncherSourceDir() : undefined; }
async function syncLauncherFromCdn() { return launcherSyncDomain && launcherSyncDomain.syncLauncherFromCdn ? launcherSyncDomain.syncLauncherFromCdn() : undefined; }
async function saveLauncherSourceDir() { return launcherSyncDomain && launcherSyncDomain.saveLauncherSourceDir ? launcherSyncDomain.saveLauncherSourceDir() : undefined; }
async function loadGameRefInfo() { return launcherSyncDomain && launcherSyncDomain.loadGameRefInfo ? launcherSyncDomain.loadGameRefInfo() : undefined; }
async function syncGameFromCdn() { return launcherSyncDomain && launcherSyncDomain.syncGameFromCdn ? launcherSyncDomain.syncGameFromCdn() : undefined; }
async function browseGameSourceDir() { return launcherSyncDomain && launcherSyncDomain.browseGameSourceDir ? launcherSyncDomain.browseGameSourceDir() : undefined; }
async function saveGameSourceDir() { return launcherSyncDomain && launcherSyncDomain.saveGameSourceDir ? launcherSyncDomain.saveGameSourceDir() : undefined; }

function clearPublishProgressTimer() { return launcherPublishDomain && launcherPublishDomain.clearPublishProgressTimer ? launcherPublishDomain.clearPublishProgressTimer() : undefined; }
function renderPublishProgress(progress = {}) { return launcherPublishDomain && launcherPublishDomain.renderPublishProgress ? launcherPublishDomain.renderPublishProgress(progress) : undefined; }
async function pollPublishProgress(force = false) { return launcherPublishDomain && launcherPublishDomain.pollPublishProgress ? launcherPublishDomain.pollPublishProgress(force) : undefined; }
function startPublishProgressPolling() { return launcherPublishDomain && launcherPublishDomain.startPublishProgressPolling ? launcherPublishDomain.startPublishProgressPolling() : undefined; }
function clearLauncherPublishProgressTimer() { return launcherPublishDomain && launcherPublishDomain.clearLauncherPublishProgressTimer ? launcherPublishDomain.clearLauncherPublishProgressTimer() : undefined; }
function renderLauncherPublishProgress(progress = {}) { return launcherPublishDomain && launcherPublishDomain.renderLauncherPublishProgress ? launcherPublishDomain.renderLauncherPublishProgress(progress) : undefined; }
async function pollLauncherPublishProgress(force = false) { return launcherPublishDomain && launcherPublishDomain.pollLauncherPublishProgress ? launcherPublishDomain.pollLauncherPublishProgress(force) : undefined; }
function startLauncherPublishProgressPolling() { return launcherPublishDomain && launcherPublishDomain.startLauncherPublishProgressPolling ? launcherPublishDomain.startLauncherPublishProgressPolling() : undefined; }
async function selectAndPublishGameFolder() { return launcherPublishDomain && launcherPublishDomain.selectAndPublishGameFolder ? launcherPublishDomain.selectAndPublishGameFolder() : undefined; }
async function publishGameFolder(sourceDir, keepBusy = false) { return launcherPublishDomain && launcherPublishDomain.publishGameFolder ? launcherPublishDomain.publishGameFolder(sourceDir, keepBusy) : undefined; }
function getSelectedPatchInput() { return launcherPublishDomain && launcherPublishDomain.getSelectedPatchInput ? launcherPublishDomain.getSelectedPatchInput() : null; }
function openPatchFilePicker() { return launcherPublishDomain && launcherPublishDomain.openPatchFilePicker ? launcherPublishDomain.openPatchFilePicker() : undefined; }
function openPatchFolderPicker() { return launcherPublishDomain && launcherPublishDomain.openPatchFolderPicker ? launcherPublishDomain.openPatchFolderPicker() : undefined; }
function renderPatchSelectionSummary() { return launcherPublishDomain && launcherPublishDomain.renderPatchSelectionSummary ? launcherPublishDomain.renderPatchSelectionSummary() : undefined; }
function readPatchFileAsBase64(file) { return launcherPublishDomain && launcherPublishDomain.readPatchFileAsBase64 ? launcherPublishDomain.readPatchFileAsBase64(file) : Promise.resolve(''); }
async function uploadPatch() { return launcherPublishDomain && launcherPublishDomain.uploadPatch ? launcherPublishDomain.uploadPatch() : undefined; }
function showPatchResult(msg, isError) {
  const el = $('patch-result');
  if (!el) return;
  el.textContent = msg;
  el.className = 'cmd-result' + (isError ? ' error' : '');
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 6000);
}
async function saveLauncherConfig() { return launcherPublishDomain && launcherPublishDomain.saveLauncherConfig ? launcherPublishDomain.saveLauncherConfig() : undefined; }
async function publishLauncherToCdn() { return launcherPublishDomain && launcherPublishDomain.publishLauncherToCdn ? launcherPublishDomain.publishLauncherToCdn() : undefined; }
