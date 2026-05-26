const ALL_CATEGORIES = "__all__";
const FAVORITES_FILTER = "__favorites__";
const SESSION_STORAGE_KEY = "midgard_loremaker_session";
const DRAFT_STORAGE_KEY_PREFIX = "midgard_loremaker_draft_v1";
const ROUTES = {
    dashboard: "/panel/dashboard",
    library: "/panel/library",
    editor: "/panel/editor"
};

const CATEGORY_PALETTE = [
    { bg: "#ede9fe", text: "#6d28d9", border: "#c4b5fd" },
    { bg: "#dbeafe", text: "#1d4ed8", border: "#93c5fd" },
    { bg: "#d1fae5", text: "#047857", border: "#6ee7b7" },
    { bg: "#fef3c7", text: "#b45309", border: "#fcd34d" },
    { bg: "#fee2e2", text: "#b91c1c", border: "#fca5a5" },
    { bg: "#fce7f3", text: "#be185d", border: "#f9a8d4" },
    { bg: "#e0f2fe", text: "#0369a1", border: "#7dd3fc" },
    { bg: "#ecfccb", text: "#4d7c0f", border: "#bef264" }
];

const bootstrapPayload = readBootstrapPayload();

const state = {
    session: null,
    books: [],
    categories: [],
    selectedBookSummaryId: null,
    selectedBookId: null,
    editor: createEmptyDraft(),
    dirty: false,
    activeCategory: ALL_CATEGORIES,
    searchTerm: "",
    activePageIndex: 0,
    view: "dashboard",
    previewPageIndex: 0,
    filterFavorites: false,
    filterTags: [],
    editorIsOwner: true,
    categoryOperation: {
        loading: false,
        message: ""
    }
};

const elements = {
    authPanel: document.getElementById("auth-panel"),
    sessionPlayer: document.getElementById("session-player"),
    sessionExpiry: document.getElementById("session-expiry"),
    metricBooks: document.getElementById("metric-books"),
    metricPages: document.getElementById("metric-pages"),
    metricCategories: document.getElementById("metric-categories"),
    metricFavorites: document.getElementById("metric-favorites"),
    metricShared: document.getElementById("metric-shared"),
    topbarKicker: document.getElementById("topbar-kicker"),
    topbarTitle: document.getElementById("topbar-title"),
    topbarSubtitle: document.getElementById("topbar-subtitle"),
    globalSearchInput: document.getElementById("global-search-input"),
    navDashboard: document.getElementById("nav-dashboard"),
    navLibrary: document.getElementById("nav-library"),
    navEditor: document.getElementById("nav-editor"),
    sidebarNewBookButton: document.getElementById("sidebar-new-book-button"),
    sidebarCreateCategoryButton: document.getElementById("sidebar-create-category-button"),
    sidebarCategoryCount: document.getElementById("sidebar-category-count"),
    sidebarCategoryList: document.getElementById("sidebar-category-list"),
    dashboardNewBookButton: document.getElementById("dashboard-new-book-button"),
    dashboardOpenLibraryButton: document.getElementById("dashboard-open-library-button"),
    dashboardOpenLibraryButtonSecondary: document.getElementById("dashboard-open-library-button-secondary"),
    dashboardOpenEditorButton: document.getElementById("dashboard-open-editor-button"),
    dashboardRecentList: document.getElementById("dashboard-recent-list"),
    dashboardCategoryGrid: document.getElementById("dashboard-category-grid"),
    libraryHeading: document.getElementById("library-heading"),
    librarySubtitle: document.getElementById("library-subtitle"),
    libraryNewBookButton: document.getElementById("library-new-book-button"),
    libraryCreateCategoryButton: document.getElementById("library-create-category-button"),
    categoryManagerCreateButton: document.getElementById("category-manager-create-button"),
    categoryManagerSubtitle: document.getElementById("category-manager-subtitle"),
    categoryManagerList: document.getElementById("category-manager-list"),
    libraryClearFiltersButton: document.getElementById("library-clear-filters-button"),
    libraryCategoryChips: document.getElementById("library-category-chips"),
    libraryBookGrid: document.getElementById("library-book-grid"),
    libraryDetailEmpty: document.getElementById("library-detail-empty"),
    libraryDetailContent: document.getElementById("library-detail-content"),
    detailTitle: document.getElementById("detail-title"),
    detailCategory: document.getElementById("detail-category"),
    detailPages: document.getElementById("detail-pages"),
    detailUpdated: document.getElementById("detail-updated"),
    detailSummary: document.getElementById("detail-summary"),
    detailOpenEditorButton: document.getElementById("detail-open-editor-button"),
    detailFilterCategoryButton: document.getElementById("detail-filter-category-button"),
    detailDuplicateBookButton: document.getElementById("detail-duplicate-book-button"),
    editorBookHeading: document.getElementById("editor-book-heading"),
    editorAddPageButton: document.getElementById("editor-add-page-button"),
    pageThumbnailList: document.getElementById("page-thumbnail-list"),
    documentTitle: document.getElementById("document-title"),
    editorSaveBookButton: document.getElementById("editor-save-book-button"),
    editorDeleteBookButton: document.getElementById("editor-delete-book-button"),
    editorFavoriteButton: document.getElementById("editor-favorite-button"),
    sheetBookLabel: document.getElementById("sheet-book-label"),
    sheetPageLabel: document.getElementById("sheet-page-label"),
    sheetCharCount: document.getElementById("sheet-char-count"),
    editorPageTextarea: document.getElementById("editor-page-textarea"),
    editorTitleInput: document.getElementById("editor-title-input"),
    editorCategoryInput: document.getElementById("editor-category-input"),
    editorAuthorInput: document.getElementById("editor-author-input"),
    editorLoreInput: document.getElementById("editor-lore-input"),
    editorDisplayColorSelect: document.getElementById("editor-display-color-select"),
    editorGlowCheckbox: document.getElementById("editor-glow-checkbox"),
    categorySuggestions: document.getElementById("category-suggestions"),
    editorTagsList: document.getElementById("editor-tags-list"),
    editorTagInput: document.getElementById("editor-tag-input"),
    editorStatus: document.getElementById("editor-status"),
    editorPageStat: document.getElementById("editor-page-stat"),
    editorCategoryStat: document.getElementById("editor-category-stat"),
    editorPageUpButton: document.getElementById("editor-page-up-button"),
    editorPageDownButton: document.getElementById("editor-page-down-button"),
    editorPageRemoveButton: document.getElementById("editor-page-remove-button"),
    snapshotList: document.getElementById("snapshot-list"),
    collaboratorSection: document.getElementById("collaborator-section"),
    collaboratorAdd: document.getElementById("collaborator-add"),
    collaboratorNameInput: document.getElementById("collaborator-name-input"),
    collaboratorAddButton: document.getElementById("collaborator-add-button"),
    collaboratorList: document.getElementById("collaborator-list"),
    viewDashboard: document.getElementById("view-dashboard"),
    viewLibrary: document.getElementById("view-library"),
    viewEditor: document.getElementById("view-editor"),
    toastContainer: document.getElementById("toast-container"),
    confirmModal: document.getElementById("confirm-modal"),
    confirmModalTitle: document.getElementById("confirm-modal-title"),
    confirmModalMessage: document.getElementById("confirm-modal-message"),
    confirmModalCancel: document.getElementById("confirm-modal-cancel"),
    confirmModalOk: document.getElementById("confirm-modal-ok"),
    createCategoryModal: document.getElementById("create-category-modal"),
    createCategoryTitle: document.getElementById("create-category-title"),
    createCategoryDescription: document.getElementById("create-category-description"),
    createCategoryInput: document.getElementById("create-category-input"),
    createCategoryError: document.getElementById("create-category-error"),
    createCategoryCancel: document.getElementById("create-category-cancel"),
    createCategoryOk: document.getElementById("create-category-ok"),
    deleteCategoryModal: document.getElementById("delete-category-modal"),
    deleteCategoryTitle: document.getElementById("delete-category-title"),
    deleteCategoryDescription: document.getElementById("delete-category-description"),
    deleteCategoryTargetSelect: document.getElementById("delete-category-target-select"),
    deleteCategoryError: document.getElementById("delete-category-error"),
    deleteCategoryCancel: document.getElementById("delete-category-cancel"),
    deleteCategoryOk: document.getElementById("delete-category-ok"),
    contextMenu: document.getElementById("context-menu"),
    contextMenuItems: document.getElementById("context-menu-items"),
    themeToggle: document.getElementById("theme-toggle"),
    editorExportBookButton: document.getElementById("editor-export-book-button"),
    mcPreviewTitle: document.getElementById("mc-preview-title"),
    mcPreviewText: document.getElementById("mc-preview-text"),
    mcPreviewPageNumber: document.getElementById("mc-preview-page-number"),
    mcPreviewNavLabel: document.getElementById("mc-preview-nav-label"),
    mcPreviewPrev: document.getElementById("mc-preview-prev"),
    mcPreviewNext: document.getElementById("mc-preview-next"),
    libraryFilterPills: document.getElementById("library-filter-pills"),
    editorRoleBadge: document.getElementById("editor-role-badge"),
    detailRoleBadge: document.getElementById("detail-role-badge"),
    snapshotPreviewModal: document.getElementById("snapshot-preview-modal"),
    snapshotPreviewTitle: document.getElementById("snapshot-preview-title"),
    snapshotPreviewDate: document.getElementById("snapshot-preview-date"),
    snapshotPreviewCategory: document.getElementById("snapshot-preview-category"),
    snapshotPreviewPages: document.getElementById("snapshot-preview-pages"),
    snapshotPreviewContent: document.getElementById("snapshot-preview-content"),
    snapshotPreviewClose: document.getElementById("snapshot-preview-close"),
    snapshotPreviewRestore: document.getElementById("snapshot-preview-restore"),
    mobileMenuToggle: document.getElementById("mobile-menu-toggle"),
    mobileOverlay: document.getElementById("mobile-overlay"),
    sidebar: document.getElementById("sidebar"),
    autosaveIndicator: document.getElementById("autosave-indicator"),
    autosaveLabel: document.getElementById("autosave-label"),
    shortcutsModal: document.getElementById("shortcuts-modal"),
    shortcutsModalClose: document.getElementById("shortcuts-modal-close")
};

initTheme();
initMobileMenu();
bindEvents();
bootstrap();

/* ===== MOBILE MENU ===== */

function initMobileMenu() {
    const mq = window.matchMedia("(max-width: 1100px)");
    function onBreakpoint(e) {
        elements.mobileMenuToggle.classList.toggle("hidden", !e.matches);
        if (!e.matches) closeMobileMenu();
    }
    mq.addEventListener("change", onBreakpoint);
    onBreakpoint(mq);
}

function openMobileMenu() {
    elements.sidebar.classList.add("sidebar-open");
    elements.mobileOverlay.classList.remove("hidden");
    elements.mobileMenuToggle.setAttribute("aria-expanded", "true");
    elements.mobileMenuToggle.querySelector(".hamburger-icon").classList.add("hidden");
    elements.mobileMenuToggle.querySelector(".close-icon").classList.remove("hidden");
}

function closeMobileMenu() {
    elements.sidebar.classList.remove("sidebar-open");
    elements.mobileOverlay.classList.add("hidden");
    elements.mobileMenuToggle.setAttribute("aria-expanded", "false");
    elements.mobileMenuToggle.querySelector(".hamburger-icon").classList.remove("hidden");
    elements.mobileMenuToggle.querySelector(".close-icon").classList.add("hidden");
}

function toggleMobileMenu() {
    const isOpen = elements.sidebar.classList.contains("sidebar-open");
    isOpen ? closeMobileMenu() : openMobileMenu();
}

/* ===== FOCUS TRAP ===== */

function trapFocus(container) {
    const focusable = container.querySelectorAll(
        'button:not([disabled]):not([tabindex="-1"]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    if (focusable.length === 0) return () => {};
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    function onKeydown(e) {
        if (e.key !== "Tab") return;
        if (e.shiftKey) {
            if (document.activeElement === first) { e.preventDefault(); last.focus(); }
        } else {
            if (document.activeElement === last) { e.preventDefault(); first.focus(); }
        }
    }
    container.addEventListener("keydown", onKeydown);
    first.focus();
    return () => container.removeEventListener("keydown", onKeydown);
}

/* ===== AUTO-SAVE ===== */

let autoSaveTimer = null;
const AUTO_SAVE_DELAY = 3000;

function scheduleAutoSave() {
    if (autoSaveTimer) clearTimeout(autoSaveTimer);
    if (!state.editor.id || !state.dirty) return;
    autoSaveTimer = setTimeout(async () => {
        autoSaveTimer = null;
        if (!state.dirty || !state.editor.id || state.view !== "editor") return;
        showAutoSaveIndicator("saving");
        try {
            await saveCurrentBook();
            showAutoSaveIndicator("saved");
        } catch (error) {
            showAutoSaveIndicator("hidden");
        }
    }, AUTO_SAVE_DELAY);
}

function showAutoSaveIndicator(status) {
    if (status === "saving") {
        elements.autosaveIndicator.classList.remove("hidden", "saved");
        elements.autosaveLabel.textContent = "Salvando...";
    } else if (status === "saved") {
        elements.autosaveIndicator.classList.remove("hidden");
        elements.autosaveIndicator.classList.add("saved");
        elements.autosaveLabel.textContent = "Salvo";
        setTimeout(() => {
            elements.autosaveIndicator.classList.add("hidden");
        }, 2500);
    } else {
        elements.autosaveIndicator.classList.add("hidden");
    }
}

/* ===== THEME ===== */

function initTheme() {
    const saved = localStorage.getItem("loremaker_theme");
    if (saved === "dark" || (!saved && window.matchMedia("(prefers-color-scheme: dark)").matches)) {
        document.documentElement.setAttribute("data-theme", "dark");
    }
}

function toggleTheme() {
    const isDark = document.documentElement.getAttribute("data-theme") === "dark";
    const next = isDark ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", next);
    localStorage.setItem("loremaker_theme", next);
}

/* ===== SHORTCUTS MODAL ===== */

function toggleShortcutsModal() {
    if (elements.shortcutsModal.classList.contains("hidden")) {
        elements.shortcutsModal.classList.remove("hidden");
        trapFocus(elements.shortcutsModal);
    } else {
        closeShortcutsModal();
    }
}

function closeShortcutsModal() {
    elements.shortcutsModal.classList.add("hidden");
}

/* ===== PAGE TRANSITION ===== */

function switchPageWithTransition(targetIndex) {
    if (targetIndex < 0 || targetIndex >= state.editor.pages.length) return;
    elements.editorPageTextarea.classList.add("page-fade");
    setTimeout(() => {
        state.activePageIndex = targetIndex;
        syncPreviewToSystemPage(targetIndex);
        renderEditor();
        elements.editorPageTextarea.classList.remove("page-fade");
        focusPageEditor();
    }, 120);
}

/* ===== TOAST & MODAL ===== */

function withButtonLock(button, asyncFn) {
    return async function(...args) {
        if (button && button.disabled) return;
        if (button) button.disabled = true;
        try {
            await asyncFn.apply(this, args);
        } finally {
            if (button) button.disabled = false;
        }
    };
}

function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    elements.toastContainer.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add("toast-visible"));
    setTimeout(() => {
        toast.classList.remove("toast-visible");
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function showConfirm(message, title = "Confirmar") {
    return new Promise((resolve) => {
        elements.confirmModalTitle.textContent = title;
        elements.confirmModalMessage.textContent = message;
        elements.confirmModal.classList.remove("hidden");
        const releaseTrap = trapFocus(elements.confirmModal);
        const cleanup = (result) => {
            elements.confirmModal.classList.add("hidden");
            elements.confirmModalOk.removeEventListener("click", onOk);
            elements.confirmModalCancel.removeEventListener("click", onCancel);
            elements.confirmModal.removeEventListener("keydown", onKeydown);
            releaseTrap();
            resolve(result);
        };
        const onOk = () => cleanup(true);
        const onCancel = () => cleanup(false);
        const onKeydown = (e) => { if (e.key === "Escape") { e.preventDefault(); onCancel(); } };
        elements.confirmModalOk.addEventListener("click", onOk);
        elements.confirmModalCancel.addEventListener("click", onCancel);
        elements.confirmModal.addEventListener("keydown", onKeydown);
    });
}

function showCreateCategoryModal(maxLength = 32, options = {}) {
    return new Promise((resolve) => {
        elements.createCategoryTitle.textContent = options.title || "Nova categoria";
        elements.createCategoryDescription.textContent = options.description || "A categoria sera criada separadamente e podera ser usada em qualquer livro depois.";
        elements.createCategoryOk.textContent = options.okLabel || "Criar e continuar";
        elements.createCategoryInput.maxLength = maxLength;
        elements.createCategoryInput.value = options.initialValue || "";
        elements.createCategoryError.textContent = "";
        elements.createCategoryError.classList.add("hidden");
        elements.createCategoryInput.classList.remove("input-error");
        elements.createCategoryModal.classList.remove("hidden");

        const cleanup = (result) => {
            elements.createCategoryModal.classList.add("hidden");
            elements.createCategoryOk.removeEventListener("click", onOk);
            elements.createCategoryCancel.removeEventListener("click", onCancel);
            elements.createCategoryModal.removeEventListener("click", onBackdropClick);
            elements.createCategoryInput.removeEventListener("keydown", onKeydown);
            elements.createCategoryInput.removeEventListener("input", onInput);
            resolve(result);
        };

        const showValidationError = (message) => {
            elements.createCategoryError.textContent = message;
            elements.createCategoryError.classList.remove("hidden");
            elements.createCategoryInput.classList.add("input-error");
            elements.createCategoryInput.focus();
        };

        const onOk = () => {
            const category = elements.createCategoryInput.value.trim().replace(/\s+/g, " ");
            if (!category) {
                showValidationError("Informe um nome valido para a categoria.");
                return;
            }
            if (category.length > maxLength) {
                showValidationError(`A categoria deve ter no maximo ${maxLength} caracteres.`);
                return;
            }
            cleanup(category);
        };

        const onCancel = () => cleanup(null);
        const onKeydown = (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                onOk();
            } else if (event.key === "Escape") {
                event.preventDefault();
                onCancel();
            }
        };
        const onBackdropClick = (event) => {
            if (event.target === elements.createCategoryModal) {
                onCancel();
            }
        };

        const onInput = () => {
            elements.createCategoryError.textContent = "";
            elements.createCategoryError.classList.add("hidden");
            elements.createCategoryInput.classList.remove("input-error");
        };

        elements.createCategoryOk.addEventListener("click", onOk);
        elements.createCategoryCancel.addEventListener("click", onCancel);
        elements.createCategoryModal.addEventListener("click", onBackdropClick);
        elements.createCategoryInput.addEventListener("keydown", onKeydown);
        elements.createCategoryInput.addEventListener("input", onInput);

        requestAnimationFrame(() => {
            elements.createCategoryInput.focus();
            elements.createCategoryInput.select();
        });
    });
}

function showDeleteCategoryModal(categoryName) {
    return new Promise((resolve) => {
        const normalizedName = normalizedCategory(categoryName);
        if (normalizedName === "Sem categoria") {
            resolve(null);
            return;
        }

        elements.deleteCategoryTitle.textContent = `Excluir "${categoryName}"`;
        elements.deleteCategoryDescription.textContent = `Escolha para onde os livros desta categoria devem ser movidos.`;
        elements.deleteCategoryError.textContent = "";
        elements.deleteCategoryError.classList.add("hidden");

        const otherCategories = getAllCategoryStats()
            .map((c) => c.name)
            .filter((n) => n !== categoryName);

        elements.deleteCategoryTargetSelect.innerHTML = "";
        const defaultOption = document.createElement("option");
        defaultOption.value = "Sem categoria";
        defaultOption.textContent = "Sem categoria";
        elements.deleteCategoryTargetSelect.appendChild(defaultOption);

        otherCategories.forEach((name) => {
            if (name === "Sem categoria") return;
            const option = document.createElement("option");
            option.value = name;
            option.textContent = name;
            elements.deleteCategoryTargetSelect.appendChild(option);
        });

        elements.deleteCategoryModal.classList.remove("hidden");

        const cleanup = (result) => {
            elements.deleteCategoryModal.classList.add("hidden");
            elements.deleteCategoryOk.removeEventListener("click", onOk);
            elements.deleteCategoryCancel.removeEventListener("click", onCancel);
            elements.deleteCategoryModal.removeEventListener("click", onBackdropClick);
            elements.deleteCategoryTargetSelect.removeEventListener("keydown", onKeydown);
            resolve(result);
        };

        const onOk = () => {
            const target = normalizedCategory(elements.deleteCategoryTargetSelect.value);
            if (target === normalizedName) {
                elements.deleteCategoryError.textContent = "Escolha uma categoria de destino diferente da categoria excluida.";
                elements.deleteCategoryError.classList.remove("hidden");
                elements.deleteCategoryTargetSelect.focus();
                return;
            }
            cleanup(target);
        };
        const onCancel = () => cleanup(null);
        const onKeydown = (event) => {
            if (event.key === "Escape") {
                event.preventDefault();
                onCancel();
            }
        };
        const onBackdropClick = (event) => {
            if (event.target === elements.deleteCategoryModal) {
                onCancel();
            }
        };

        elements.deleteCategoryOk.addEventListener("click", onOk);
        elements.deleteCategoryCancel.addEventListener("click", onCancel);
        elements.deleteCategoryModal.addEventListener("click", onBackdropClick);
        elements.deleteCategoryTargetSelect.addEventListener("keydown", onKeydown);

        requestAnimationFrame(() => elements.deleteCategoryTargetSelect.focus());
    });
}

/* ===== CATEGORY COLORS ===== */

function getCategoryColor(categoryName) {
    let hash = 0;
    for (let i = 0; i < categoryName.length; i++) {
        hash = categoryName.charCodeAt(i) + ((hash << 5) - hash);
    }
    return CATEGORY_PALETTE[Math.abs(hash) % CATEGORY_PALETTE.length];
}

/* ===== BOOTSTRAP ===== */

function readBootstrapPayload() {
    const bootstrapElement = document.getElementById("loremaker-bootstrap");
    if (!bootstrapElement) {
        console.warn("[LoreMaker] Elemento bootstrap nao encontrado no DOM.");
        return { session: null };
    }

    try {
        const raw = bootstrapElement.textContent || "{}";
        const parsed = JSON.parse(raw);
        console.log("[LoreMaker] Bootstrap payload:", parsed?.session ? "sessao presente" : "sem sessao");
        return parsed && typeof parsed === "object" ? parsed : { session: null };
    } catch (error) {
        console.error("[LoreMaker] Falha ao ler bootstrap JSON:", error);
        return { session: null };
    }
}

function createEmptyDraft(category = "") {
    return {
        id: null,
        title: "",
        category,
        author: "",
        exportLore: "",
        exportDisplayColor: "",
        exportGlow: false,
        tags: [],
        favorite: false,
        pages: [""],
        createdAt: null,
        updatedAt: null
    };
}

async function bootstrap() {
    const entryToken = readEntryToken();
    syncRouteFromLocation();
    renderApp();

    try {
        state.session = bootstrapPayload.session || await restoreSession(entryToken);
    } catch (error) {
        console.error("[LoreMaker] Falha na autenticacao:", error);
        if (entryToken) {
            stripTokenFromUrl();
        }
        setAuthenticated(false, error.message);
        return;
    }

    console.log("[LoreMaker] Sessao autenticada:", state.session.playerName, "sessionId:", state.session.sessionId ? "presente" : "ausente");
    rememberWorkspaceSession(state.session.sessionId);
    setAuthenticated(true);
    applySessionData();

    if (entryToken) {
        stripTokenFromUrl();
    }

    normalizeRoute(true);

    try {
        await refreshBooks();
        await hydrateRouteSelection();
        await maybeRestorePersistedDraft();
    } catch (error) {
        console.error("[LoreMaker] Falha ao carregar dados:", error);
        showToast(error.message || "Falha ao carregar livros.", "error");
        renderConnectionError();
    }

    normalizeRoute(true);
    renderApp();
}

/* ===== EVENTS ===== */

function bindEvents() {
    window.addEventListener("popstate", async () => {
        syncRouteFromLocation();
        try {
            await hydrateRouteSelection();
        } catch (error) {
            if (error?.message) {
                showToast(error.message, "error");
            }
        }
        renderApp();
    });

    window.addEventListener("beforeunload", (event) => {
        persistEditorDraftIfPossible();
        if (!state.dirty) {
            return;
        }
        event.preventDefault();
        event.returnValue = "";
    });

    document.addEventListener("keydown", async (event) => {
        const mod = event.ctrlKey || event.metaKey;

        if (mod && event.key.toLowerCase() === "s") {
            event.preventDefault();
            if (state.view === "editor") await saveCurrentBook();
            return;
        }

        if (mod && event.key === "?") {
            event.preventDefault();
            toggleShortcutsModal();
            return;
        }

        if (state.view !== "editor" || !elements.editorPageTextarea.matches(":focus")) return;

        if (mod && event.key.toLowerCase() === "b") { event.preventDefault(); insertFormatCode("l"); return; }
        if (mod && event.key.toLowerCase() === "i") { event.preventDefault(); insertFormatCode("o"); return; }
        if (mod && event.key.toLowerCase() === "u") { event.preventDefault(); insertFormatCode("n"); return; }
        if (mod && event.shiftKey && event.key.toLowerCase() === "x") { event.preventDefault(); insertFormatCode("m"); return; }

        if (mod && event.key === "ArrowUp") {
            event.preventDefault();
            moveActivePage(-1);
            return;
        }
        if (mod && event.key === "ArrowDown") {
            event.preventDefault();
            moveActivePage(1);
            return;
        }

        if (event.key === "PageUp" && !mod) {
            event.preventDefault();
            if (state.activePageIndex > 0) {
                switchPageWithTransition(state.activePageIndex - 1);
            }
            return;
        }
        if (event.key === "PageDown" && !mod) {
            event.preventDefault();
            if (state.activePageIndex < state.editor.pages.length - 1) {
                switchPageWithTransition(state.activePageIndex + 1);
            }
            return;
        }
    });

    document.addEventListener("click", (event) => {
        if (elements.contextMenu.classList.contains("hidden")) {
            return;
        }
        if (!elements.contextMenu.contains(event.target)) {
            hideContextMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            hideContextMenu();
        }
    });

    window.addEventListener("resize", hideContextMenu);
    window.addEventListener("scroll", hideContextMenu, true);

    elements.mobileMenuToggle.addEventListener("click", toggleMobileMenu);
    elements.mobileOverlay.addEventListener("click", closeMobileMenu);

    elements.navDashboard.addEventListener("click", () => { closeMobileMenu(); navigate("dashboard"); });
    elements.navLibrary.addEventListener("click", () => { closeMobileMenu(); navigate("library"); });
    elements.navEditor.addEventListener("click", async () => {
        closeMobileMenu();
        if (state.selectedBookId) {
            await openBookInEditor(state.selectedBookId);
            return;
        }
        navigate("editor");
    });

    elements.sidebarNewBookButton.addEventListener("click", () => { closeMobileMenu(); startNewBook(); });
    elements.sidebarCreateCategoryButton.addEventListener("click", () => { closeMobileMenu(); createCategoryFlow(); });
    elements.dashboardNewBookButton.addEventListener("click", () => startNewBook());
    elements.libraryNewBookButton.addEventListener("click", () => startNewBook());
    elements.libraryCreateCategoryButton.addEventListener("click", createCategoryFlow);
    elements.categoryManagerCreateButton.addEventListener("click", createCategoryFlow);

    elements.dashboardOpenLibraryButton.addEventListener("click", () => navigate("library"));
    elements.dashboardOpenLibraryButtonSecondary.addEventListener("click", () => navigate("library"));
    elements.dashboardOpenEditorButton.addEventListener("click", async () => {
        if (state.selectedBookSummaryId) {
            await openBookInEditor(state.selectedBookSummaryId);
            return;
        }
        startNewBook();
    });

    elements.libraryClearFiltersButton.addEventListener("click", () => {
        state.activeCategory = ALL_CATEGORIES;
        state.searchTerm = "";
        state.filterFavorites = false;
        state.filterTags = [];
        renderApp();
    });

    elements.detailOpenEditorButton.addEventListener("click", async () => {
        if (!state.selectedBookSummaryId) {
            return;
        }
        await openBookInEditor(state.selectedBookSummaryId);
    });

    elements.detailFilterCategoryButton.addEventListener("click", () => {
        const selected = getSelectedBookSummary();
        if (!selected) {
            return;
        }
        selectCategory(selected.category, true);
    });

    elements.detailDuplicateBookButton.addEventListener("click", withButtonLock(elements.detailDuplicateBookButton, async () => {
        const selected = getSelectedBookSummary();
        if (!selected) return;
        await duplicateBook(selected.id);
    }));

    elements.collaboratorAddButton.addEventListener("click", async () => {
        const name = elements.collaboratorNameInput.value.trim();
        if (!name || !state.editor.id) return;
        await addCollaborator(state.editor.id, name);
    });

    elements.collaboratorNameInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") elements.collaboratorAddButton.click();
    });

    elements.globalSearchInput.addEventListener("input", (event) => {
        state.searchTerm = event.target.value.trim();
        syncVisibleSelection();
        renderApp();
    });

    elements.globalSearchInput.addEventListener("keydown", (event) => {
        if (event.key !== "Enter" || state.view === "library") {
            return;
        }
        navigate("library");
    });

    elements.editorTitleInput.addEventListener("input", (event) => {
        state.editor.title = event.target.value;
        setDirty(true);
        renderEditor();
    });

    elements.editorCategoryInput.addEventListener("input", (event) => {
        state.editor.category = event.target.value;
        setDirty(true);
        renderEditor();
    });

    elements.editorAuthorInput.addEventListener("input", (event) => {
        state.editor.author = event.target.value;
        persistEditorDraftIfPossible();
    });

    elements.editorLoreInput.addEventListener("input", (event) => {
        state.editor.exportLore = event.target.value;
        persistEditorDraftIfPossible();
    });

    elements.editorDisplayColorSelect.addEventListener("change", (event) => {
        state.editor.exportDisplayColor = event.target.value;
        persistEditorDraftIfPossible();
    });

    elements.editorGlowCheckbox.addEventListener("change", (event) => {
        state.editor.exportGlow = event.target.checked;
        persistEditorDraftIfPossible();
    });

    elements.editorTagInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            const tag = elements.editorTagInput.value.trim().toLowerCase();
            if (tag && !state.editor.tags.includes(tag) && state.editor.tags.length < 10) {
                state.editor.tags.push(tag);
                elements.editorTagInput.value = "";
                setDirty(true);
                renderTags();
            }
        }
    });

    elements.editorPageTextarea.addEventListener("input", (event) => {
        ensureEditorHasPage();
        const text = event.target.value;
        const maxChars = state.session?.limits?.maxCharactersPerPage || 1200;
        const maxPages = state.session?.limits?.maxPagesPerBook || 50;

        if (text.length <= maxChars) {
            state.editor.pages[state.activePageIndex] = text;
            syncPreviewToSystemPage(state.activePageIndex);
            setDirty(true);
            renderEditor();
            return;
        }

        state.editor.pages[state.activePageIndex] = text.substring(0, maxChars);
        let overflow = text.substring(maxChars);
        let target = state.activePageIndex + 1;

        while (overflow.length > 0) {
            if (target >= state.editor.pages.length) {
                if (state.editor.pages.length >= maxPages) {
                    overflow = "";
                    break;
                }
                state.editor.pages.splice(target, 0, "");
            }
            const merged = overflow + state.editor.pages[target];
            if (merged.length <= maxChars) {
                state.editor.pages[target] = merged;
                overflow = "";
            } else {
                state.editor.pages[target] = merged.substring(0, maxChars);
                overflow = merged.substring(maxChars);
                target++;
            }
        }

        const cursorPos = text.length - maxChars;
        state.activePageIndex = Math.min(state.activePageIndex + 1, state.editor.pages.length - 1);
        syncPreviewToSystemPage(state.activePageIndex);
        setDirty(true);
        renderEditor();

        requestAnimationFrame(() => {
            elements.editorPageTextarea.focus();
            const pos = Math.min(cursorPos, (state.editor.pages[state.activePageIndex] || "").length);
            elements.editorPageTextarea.setSelectionRange(pos, pos);
        });
    });

    elements.editorAddPageButton.addEventListener("click", addPage);
    elements.editorSaveBookButton.addEventListener("click", withButtonLock(elements.editorSaveBookButton, saveCurrentBook));
    elements.editorDeleteBookButton.addEventListener("click", withButtonLock(elements.editorDeleteBookButton, deleteCurrentBook));
    elements.editorFavoriteButton.addEventListener("click", withButtonLock(elements.editorFavoriteButton, toggleFavorite));
    elements.editorPageUpButton.addEventListener("click", () => moveActivePage(-1));
    elements.editorPageDownButton.addEventListener("click", () => moveActivePage(1));

    document.querySelectorAll(".color-swatch, .format-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            const code = btn.getAttribute("data-code");
            if (code) insertFormatCode(code);
        });
    });
    elements.editorPageRemoveButton.addEventListener("click", removeActivePage);

    elements.themeToggle.addEventListener("click", toggleTheme);
    elements.editorExportBookButton.addEventListener("click", withButtonLock(elements.editorExportBookButton, exportCurrentBook));
    elements.shortcutsModalClose.addEventListener("click", () => closeShortcutsModal());
    elements.shortcutsModal.addEventListener("click", (e) => { if (e.target === elements.shortcutsModal) closeShortcutsModal(); });

    elements.mcPreviewPrev.addEventListener("click", () => {
        if (state.previewPageIndex > 0) {
            state.previewPageIndex--;
            renderMinecraftPreview();
        }
    });
    elements.mcPreviewNext.addEventListener("click", () => {
        const total = buildPreviewPages().length;
        if (state.previewPageIndex < total - 1) {
            state.previewPageIndex++;
            renderMinecraftPreview();
        }
    });
}

/* ===== TOKEN & SESSION ===== */

function stripTokenFromUrl() {
    const url = new URL(window.location.href);
    if (!url.searchParams.has("token")) {
        return;
    }
    url.searchParams.delete("token");
    window.history.replaceState({}, document.title, buildRelativeUrl(url.pathname, url.searchParams));
}

function readEntryToken() {
    const url = new URL(window.location.href);
    const token = url.searchParams.get("token");
    return token && token.trim() ? token.trim() : null;
}

function rememberWorkspaceSession(sessionId) {
    if (!sessionId) {
        return;
    }
    try {
        window.sessionStorage.setItem(SESSION_STORAGE_KEY, sessionId);
    } catch (error) {
        console.warn("Nao foi possivel memorizar a sessao do workspace.", error);
    }
}

function getRememberedWorkspaceSession() {
    try {
        return window.sessionStorage.getItem(SESSION_STORAGE_KEY);
    } catch (error) {
        return null;
    }
}

function clearRememberedWorkspaceSession() {
    try {
        window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
    } catch (error) {
        // Ignora quando o navegador restringe storage.
    }
}

/* ===== ROUTING ===== */

function syncRouteFromLocation() {
    const normalizedPath = normalizePath(window.location.pathname);
    const params = new URLSearchParams(window.location.search);

    if (normalizedPath.endsWith("/library")) {
        state.view = "library";
    } else if (normalizedPath.endsWith("/editor")) {
        state.view = "editor";
    } else {
        state.view = "dashboard";
    }

    const rawBookId = params.get("book");
    if (rawBookId == null || rawBookId === "") {
        if (state.view !== "editor") {
            state.selectedBookId = null;
        }
        return;
    }

    const parsedBookId = Number(rawBookId);
    if (Number.isFinite(parsedBookId) && parsedBookId > 0) {
        state.selectedBookId = parsedBookId;
        state.selectedBookSummaryId = parsedBookId;
    } else {
        state.selectedBookId = null;
    }
}

async function navigate(view, options = {}) {
    if (!(await canLeaveCurrentContext())) {
        return false;
    }

    applyViewState(view, options.bookId ?? null);
    normalizeRoute(false);
    renderApp();
    return true;
}

function applyViewState(view, bookId) {
    state.view = view;
    if (view === "editor") {
        state.selectedBookId = bookId || state.editor.id || null;
        if (state.selectedBookId != null) {
            state.selectedBookSummaryId = state.selectedBookId;
        }
        return;
    }

    state.selectedBookId = null;
}

function normalizeRoute(replace) {
    const target = buildRouteUrl(state.view, state.view === "editor" ? state.selectedBookId || state.editor.id : null);
    const current = buildRelativeUrl(window.location.pathname, new URLSearchParams(window.location.search));
    if (target === current) {
        updateDocumentTitle();
        return;
    }

    const action = replace ? "replaceState" : "pushState";
    window.history[action]({}, document.title, target);
    updateDocumentTitle();
}

function buildRouteUrl(view, bookId) {
    const route = ROUTES[view] || ROUTES.dashboard;
    const params = new URLSearchParams();
    if (view === "editor" && bookId) {
        params.set("book", String(bookId));
    }
    return buildRelativeUrl(route, params);
}

function buildRelativeUrl(pathname, params) {
    const search = params.toString();
    return `${normalizePath(pathname)}${search ? `?${search}` : ""}`;
}

function normalizePath(pathname) {
    const trimmed = pathname.replace(/\/+$/, "");
    return trimmed === "" ? "/" : trimmed;
}

async function hydrateRouteSelection() {
    if (state.view !== "editor" || !state.selectedBookId) {
        syncVisibleSelection();
        return;
    }

    if (state.editor.id === state.selectedBookId) {
        return;
    }

    await loadBook(state.selectedBookId);
}

async function restoreSession(entryToken) {
    try {
        const session = await api("/api/session", { suppressUnauthorizedOverlay: true });
        console.log("[LoreMaker] Sessao restaurada via cookie/header.");
        return session;
    } catch (error) {
        console.log("[LoreMaker] Cookie/header falhou:", error.message);
        if (!entryToken) {
            throw error;
        }
    }

    console.log("[LoreMaker] Tentando autenticar com token de entrada...");
    return api(`/api/session?token=${encodeURIComponent(entryToken)}`, {
        suppressUnauthorizedOverlay: true
    });
}

function applySessionData() {
    elements.editorTitleInput.maxLength = state.session.limits.maxTitleLength;
    elements.editorCategoryInput.maxLength = state.session.limits.maxCategoryLength;
    elements.sessionPlayer.textContent = state.session.playerName;
    elements.sessionExpiry.textContent = "Sessao ativa";
}

function setAuthenticated(authenticated, message) {
    elements.authPanel.classList.toggle("hidden", authenticated);
    if (authenticated) {
        return;
    }

    clearRememberedWorkspaceSession();
    elements.sessionPlayer.textContent = "Acesso necessario";
    elements.sessionExpiry.textContent = message || "Volte ao Minecraft e use /loremaker para abrir um novo acesso.";
}

/* ===== BOOK OPERATIONS ===== */

async function refreshBooks(preferredBookId = state.selectedBookSummaryId) {
    state.books = await api("/api/books");
    state.categories = await api("/api/categories");
    updateCategorySuggestions();

    if (preferredBookId && state.books.some((book) => book.id === preferredBookId)) {
        state.selectedBookSummaryId = preferredBookId;
    } else if (state.selectedBookSummaryId && !state.books.some((book) => book.id === state.selectedBookSummaryId)) {
        state.selectedBookSummaryId = null;
    }

    const availableCategoryNames = new Set([
        ...state.categories.map((category) => normalizedCategory(category)),
        ...state.books.map((book) => normalizedCategory(book.category))
    ]);

    if (state.activeCategory !== ALL_CATEGORIES && !availableCategoryNames.has(state.activeCategory)) {
        state.activeCategory = ALL_CATEGORIES;
    }
    if (state.filterFavorites && !state.books.some(b => b.favorite)) {
        state.filterFavorites = false;
    }

    syncVisibleSelection();
    renderApp();
}

function syncVisibleSelection() {
    const visibleBooks = getVisibleBooks();
    if (visibleBooks.length === 0) {
        state.selectedBookSummaryId = null;
        return;
    }

    if (!visibleBooks.some((book) => book.id === state.selectedBookSummaryId)) {
        state.selectedBookSummaryId = visibleBooks[0].id;
    }
}

async function openBookInEditor(bookId) {
    if (!bookId || !(await canLeaveCurrentContext())) {
        return;
    }

    await loadBook(bookId);
    state.view = "editor";
    state.selectedBookId = bookId;
    state.selectedBookSummaryId = bookId;
    normalizeRoute(false);
    renderApp();
    renderSnapshots();
    renderCollaborators();
    focusPageEditor();
}

async function loadBook(bookId) {
    const book = await api(`/api/books/${bookId}`);
    const summary = state.books.find(b => b.id === bookId);
    state.editor = {
        id: book.id,
        title: book.title,
        category: book.category,
        author: "",
        exportLore: "",
        exportDisplayColor: "",
        exportGlow: false,
        tags: Array.isArray(book.tags) ? [...book.tags] : [],
        favorite: summary ? summary.favorite : false,
        pages: Array.isArray(book.pages) && book.pages.length > 0 ? [...book.pages] : [""],
        createdAt: book.createdAt,
        updatedAt: book.updatedAt
    };
    state.activePageIndex = 0;
    state.previewPageIndex = 0;
    state.selectedBookId = book.id;
    state.selectedBookSummaryId = book.id;
    setDirty(false);
}

async function startNewBook() {
    await startNewBookWithCategory(prefillCategory());
}

async function startNewBookWithCategory(category) {
    if (!(await canLeaveCurrentContext())) {
        return false;
    }

    state.editor = createEmptyDraft(category || "");
    state.activePageIndex = 0;
    state.previewPageIndex = 0;
    state.selectedBookId = null;
    setDirty(false);
    state.view = "editor";
    normalizeRoute(false);
    renderApp();
    focusTitleEditor();
    return true;
}

async function createCategoryFlow() {
    const maxLength = state.session?.limits?.maxCategoryLength || 32;
    const category = await showCreateCategoryModal(maxLength);
    if (category == null) {
        return;
    }

    try {
        showToast("Criando categoria...", "info");
        const created = await api("/api/categories", {
            method: "POST",
            body: JSON.stringify({ name: category })
        });
        const normalized = normalizedCategory(created.name || category);
        state.categories = await api("/api/categories");
        state.activeCategory = normalized;
        syncVisibleSelection();
        renderApp();
        showToast(`Categoria \"${normalized}\" criada.`, "success");
    } catch (error) {
        showToast(error.message, "error");
    }
}

/* ===== RENDER ===== */

function renderApp() {
    updateDocumentTitle();
    renderTopbar();
    renderNavigation();
    renderSidebarCategories();
    renderDashboard();
    renderLibrary();
    renderEditor();
    updateMetrics();
    toggleViews();
}

function renderTopbar() {
    const currentSelection = getSelectedBookSummary();
    const descriptors = {
        dashboard: {
            kicker: "PAINEL",
            title: "Studio editorial",
            subtitle: "Acompanhe seus volumes, entre nas estantes certas e volte para o editor a partir do painel principal."
        },
        library: {
            kicker: "BIBLIOTECA",
            title: state.activeCategory === ALL_CATEGORIES ? "Catalogo completo" : `Estante: ${state.activeCategory}`,
            subtitle: `${getVisibleBooks().length} manuscrito(s) encontrado(s). Selecione um volume para ver os detalhes.`
        },
        editor: {
            kicker: "EDITOR",
            title: state.editor.id ? (state.editor.title || "Livro sem titulo") : "Novo manuscrito",
            subtitle: state.editor.id
                ? "Edite pagina por pagina com trilha lateral e painel de propriedades."
                : "Monte o manuscrito pagina por pagina e salve quando o volume estiver pronto."
        }
    };

    const descriptor = descriptors[state.view];
    elements.topbarKicker.textContent = descriptor.kicker;
    elements.topbarTitle.textContent = descriptor.title;
    elements.topbarSubtitle.textContent = descriptor.subtitle;
    elements.globalSearchInput.value = state.searchTerm;

    if (state.view === "dashboard" && currentSelection) {
        elements.topbarSubtitle.textContent = `Livro em foco: ${currentSelection.title}, atualizado em ${formatDate(currentSelection.updatedAt)}.`;
    }
}

function renderNavigation() {
    elements.navDashboard.classList.toggle("active", state.view === "dashboard");
    elements.navLibrary.classList.toggle("active", state.view === "library");
    elements.navEditor.classList.toggle("active", state.view === "editor");
    elements.navDashboard.setAttribute("aria-current", state.view === "dashboard" ? "page" : "false");
    elements.navLibrary.setAttribute("aria-current", state.view === "library" ? "page" : "false");
    elements.navEditor.setAttribute("aria-current", state.view === "editor" ? "page" : "false");
}

function renderSidebarCategories() {
    elements.sidebarCategoryList.innerHTML = "";
    const categories = getAllCategoryStats();
    elements.sidebarCategoryCount.textContent = String(categories.length);
    elements.sidebarCategoryList.appendChild(createCategoryButton("Todos os livros", ALL_CATEGORIES, state.books.length));

    const favCount = state.books.filter(b => b.favorite).length;
    if (favCount > 0) {
        const favBtn = document.createElement("button");
        favBtn.type = "button";
        favBtn.className = `cat-btn${state.filterFavorites ? " active" : ""}`;
        favBtn.innerHTML = `<span>\u2605 Favoritos</span><span class="cat-count">${favCount}</span>`;
        favBtn.addEventListener("click", () => {
            state.filterFavorites = !state.filterFavorites;
            syncVisibleSelection();
            renderApp();
        });
        elements.sidebarCategoryList.appendChild(favBtn);
    }

    categories.forEach((category) => {
        elements.sidebarCategoryList.appendChild(createCategoryButton(category.name, category.name, category.count));
    });
}

function renderDashboard() {
    renderRecentBooks();
    renderDashboardCategories();
}

function renderRecentBooks() {
    elements.dashboardRecentList.innerHTML = "";
    const recentBooks = getVisibleBooks()
        .slice()
        .sort((left, right) => right.updatedAt - left.updatedAt)
        .slice(0, 4);

    if (recentBooks.length === 0) {
        appendEmptyState(elements.dashboardRecentList, "Nenhum livro recente. Crie o primeiro volume para preencher este painel.");
        return;
    }

    recentBooks.forEach((book) => {
        const color = getCategoryColor(normalizedCategory(book.category));
        const item = document.createElement("button");
        item.type = "button";
        item.className = "recent-item";
        item.innerHTML = `
            <div class="recent-icon" style="background:${color.bg};color:${color.text}">${escapeHtml(book.title.charAt(0).toUpperCase())}</div>
            <div class="recent-info">
                <strong>${escapeHtml(book.title)}</strong>
                <span>${escapeHtml(normalizedCategory(book.category))} · ${book.pageCount} pag · ${formatDate(book.updatedAt)}</span>
            </div>
        `;
        item.addEventListener("click", async () => {
            await openBookInEditor(book.id);
        });
        elements.dashboardRecentList.appendChild(item);
    });
}

function renderDashboardCategories() {
    elements.dashboardCategoryGrid.innerHTML = "";
    const categories = getAllCategoryStats();

    if (categories.length === 0) {
        appendEmptyState(elements.dashboardCategoryGrid, "As categorias vao surgir aqui conforme a biblioteca receber os primeiros livros.");
        return;
    }

    categories.forEach((category) => {
        const color = getCategoryColor(category.name);
        const booksInCat = state.books.filter(b => normalizedCategory(b.category) === category.name);
        const lastUpdated = booksInCat.length > 0
            ? formatDate(Math.max(...booksInCat.map(b => b.updatedAt || 0)))
            : null;
        const card = document.createElement("button");
        card.type = "button";
        card.className = "category-card";
        card.innerHTML = `
            <span class="cat-dot" style="background:${color.text}"></span>
            <span class="cat-label">${escapeHtml(category.name)}</span>
            <span class="cat-num">${category.count}${lastUpdated ? ` · ${lastUpdated}` : ""}</span>
        `;
        card.addEventListener("click", () => {
            selectCategory(category.name, true);
        });
        elements.dashboardCategoryGrid.appendChild(card);
    });
}

function renderLibrary() {
    renderLibraryHeader();
    renderLibraryChips();
    renderFilterPills();
    renderCategoryManager();
    renderLibraryBooks();
    renderLibraryDetail();
}

function renderLibraryHeader() {
    const visibleBooks = getVisibleBooks();
    elements.libraryHeading.textContent = state.activeCategory === ALL_CATEGORIES ? "Todos os livros" : state.activeCategory;
    elements.librarySubtitle.textContent = visibleBooks.length === 0
        ? "Nenhum manuscrito encontrado."
        : `${visibleBooks.length} livro(s)${state.searchTerm ? ` para "${state.searchTerm}"` : ""}`;
    elements.libraryClearFiltersButton.disabled = state.activeCategory === ALL_CATEGORIES && state.searchTerm === "" && !state.filterFavorites && state.filterTags.length === 0;
}

function renderLibraryChips() {
    elements.libraryCategoryChips.innerHTML = "";
    elements.libraryCategoryChips.appendChild(createLibraryChip("Todos", ALL_CATEGORIES, state.books.length));

    const favCount = state.books.filter(b => b.favorite).length;
    if (favCount > 0) {
        const favChip = document.createElement("button");
        favChip.type = "button";
        favChip.className = `chip chip-favorite${state.filterFavorites ? " active" : ""}`;
        favChip.innerHTML = `<span>\u2605 Favoritos</span><span class="chip-count">${favCount}</span>`;
        favChip.addEventListener("click", () => {
            state.filterFavorites = !state.filterFavorites;
            syncVisibleSelection();
            renderLibrary();
            renderSidebarCategories();
            renderTopbar();
        });
        elements.libraryCategoryChips.appendChild(favChip);
    }

    getAllCategoryStats().forEach((category) => {
        elements.libraryCategoryChips.appendChild(createLibraryChip(category.name, category.name, category.count));
    });
}

function renderFilterPills() {
    const hasFilters = state.filterFavorites || state.filterTags.length > 0;
    elements.libraryFilterPills.classList.toggle("hidden", !hasFilters);
    if (!hasFilters) return;

    elements.libraryFilterPills.innerHTML = "";

    if (state.filterFavorites) {
        const pill = document.createElement("button");
        pill.type = "button";
        pill.className = "filter-pill";
        pill.innerHTML = `\u2605 Favoritos <span class="filter-pill-remove">&times;</span>`;
        pill.addEventListener("click", () => {
            state.filterFavorites = false;
            syncVisibleSelection();
            renderApp();
        });
        elements.libraryFilterPills.appendChild(pill);
    }

    state.filterTags.forEach(tag => {
        const pill = document.createElement("button");
        pill.type = "button";
        pill.className = "filter-pill";
        pill.innerHTML = `#${escapeHtml(tag)} <span class="filter-pill-remove">&times;</span>`;
        pill.addEventListener("click", () => toggleTagFilter(tag));
        elements.libraryFilterPills.appendChild(pill);
    });

    if (state.filterFavorites && state.filterTags.length > 0 || state.filterTags.length > 1) {
        const clearPill = document.createElement("button");
        clearPill.type = "button";
        clearPill.className = "filter-pill filter-pill-clear";
        clearPill.textContent = "Limpar filtros";
        clearPill.addEventListener("click", () => {
            state.filterFavorites = false;
            state.filterTags = [];
            syncVisibleSelection();
            renderApp();
        });
        elements.libraryFilterPills.appendChild(clearPill);
    }
}

function toggleTagFilter(tag) {
    const index = state.filterTags.indexOf(tag);
    if (index >= 0) {
        state.filterTags.splice(index, 1);
    } else {
        state.filterTags.push(tag);
    }
    syncVisibleSelection();
    renderApp();
}

function renderCategoryManager() {
    const categories = getAllCategoryStats();
    elements.categoryManagerSubtitle.textContent = categories.length === 0
        ? "Nenhuma categoria criada ainda."
        : `${categories.length} categoria(s) registrada(s).`;

    elements.categoryManagerList.innerHTML = "";

    if (categories.length === 0) {
        appendEmptyState(elements.categoryManagerList, "Crie a primeira categoria para organizar seus livros.");
        return;
    }

    categories.forEach((category) => {
        const color = getCategoryColor(category.name);
        const row = document.createElement("div");
        row.className = "category-manager-row";
        row.innerHTML = `
            <span class="category-manager-dot" style="background:${color.text}"></span>
            <span class="category-manager-name">${escapeHtml(category.name)}</span>
            <span class="category-manager-count">${category.count} livro(s)</span>
        `;

        const actions = document.createElement("div");
        actions.className = "category-manager-actions";

        const renameBtn = document.createElement("button");
        renameBtn.type = "button";
        renameBtn.className = "btn btn-sm btn-ghost";
        renameBtn.textContent = "Renomear";
        renameBtn.addEventListener("click", () => renameCategoryFlow(category.name));

        const deleteBtn = document.createElement("button");
        deleteBtn.type = "button";
        deleteBtn.className = "btn btn-sm btn-danger-ghost";
        deleteBtn.textContent = "Excluir";
        deleteBtn.addEventListener("click", () => deleteCategoryFlow(category.name));

        const filterBtn = document.createElement("button");
        filterBtn.type = "button";
        filterBtn.className = "btn btn-sm btn-ghost";
        filterBtn.textContent = "Filtrar";
        filterBtn.addEventListener("click", () => selectCategory(category.name, false));

        actions.appendChild(filterBtn);
        actions.appendChild(renameBtn);
        actions.appendChild(deleteBtn);
        row.appendChild(actions);
        elements.categoryManagerList.appendChild(row);
    });
}

function renderLibraryBooks() {
    elements.libraryBookGrid.innerHTML = "";
    const visibleBooks = getVisibleBooks()
        .slice()
        .sort((left, right) => right.updatedAt - left.updatedAt);

    if (visibleBooks.length === 0) {
        appendEmptyState(
            elements.libraryBookGrid,
            state.books.length === 0
                ? "A biblioteca esta vazia. Crie um livro novo para inaugurar a estante."
                : "Nenhum volume corresponde ao filtro atual. Troque a busca ou escolha outra categoria."
        );
        return;
    }

    visibleBooks.forEach((book) => {
        const catName = normalizedCategory(book.category);
        const color = getCategoryColor(catName);
        const card = document.createElement("button");
        card.type = "button";
        card.className = `book-card${state.selectedBookSummaryId === book.id ? " active" : ""}`;

        const roleBadge = book.shared
            ? '<span class="role-badge role-badge-collaborator">Colaborador</span>'
            : '';
        const tagHtml = book.tags && book.tags.length > 0
            ? `<div class="book-card-meta">${book.tags.map(t => `<span class="tag-chip tag-clickable${state.filterTags.includes(t) ? ' tag-active' : ''}" data-tag="${escapeHtml(t)}">${escapeHtml(t)}</span>`).join(' ')}</div>`
            : '';

        card.innerHTML = `
            <div class="book-card-header">
                <strong>${escapeHtml(book.title)}${roleBadge}</strong>
                <span class="book-badge" style="background:${color.bg};color:${color.text}">${escapeHtml(catName)}</span>
                ${book.favorite ? '<svg class="card-favorite-star" viewBox="0 0 20 20" fill="currentColor"><path d="M10 2l2.39 4.84L17.3 7.8l-3.65 3.56.86 5.02L10 13.9l-4.51 2.48.86-5.02L2.7 7.8l4.91-.96L10 2z"/></svg>' : ''}
            </div>
            <div class="book-card-meta">
                <span>${book.pageCount} pagina(s)</span>
                <span>${formatDate(book.updatedAt)}</span>
            </div>
            ${tagHtml}
        `;

        card.querySelectorAll('.tag-chip').forEach(chip => {
            chip.addEventListener('click', (e) => {
                e.stopPropagation();
                const tag = chip.getAttribute('data-tag');
                if (tag) toggleTagFilter(tag);
            });
        });

        card.addEventListener("click", async () => {
            state.selectedBookSummaryId = book.id;
            await openBookInEditor(book.id);
        });
        card.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            openBookContextMenu(book, event.clientX, event.clientY);
        });
        elements.libraryBookGrid.appendChild(card);
    });
}

function renderLibraryDetail() {
    const selected = getSelectedBookSummary();
    const hasSelection = Boolean(selected);
    elements.libraryDetailEmpty.classList.toggle("hidden", hasSelection);
    elements.libraryDetailContent.classList.toggle("hidden", !hasSelection);
    elements.detailOpenEditorButton.disabled = !hasSelection;
    elements.detailFilterCategoryButton.disabled = !hasSelection;
    elements.detailDuplicateBookButton.disabled = !hasSelection;

    if (!selected) {
        return;
    }

    const catName = normalizedCategory(selected.category);
    const color = getCategoryColor(catName);
    elements.detailTitle.textContent = selected.title;
    elements.detailCategory.textContent = catName;

    if (selected.shared) {
        elements.detailRoleBadge.textContent = "Colaborador";
        elements.detailRoleBadge.className = "role-badge role-badge-collaborator";
        elements.detailRoleBadge.classList.remove("hidden");
        elements.detailSummary.textContent = `Livro compartilhado com voce. Abra no editor para editar cada pagina separadamente.`;
    } else {
        elements.detailRoleBadge.classList.add("hidden");
        elements.detailSummary.textContent = `Volume na estante "${catName}" com ${selected.pageCount} pagina(s). Abra no editor para editar cada pagina separadamente.`;
    }
    elements.detailCategory.style.background = color.bg;
    elements.detailCategory.style.color = color.text;
    elements.detailPages.textContent = String(selected.pageCount);
    elements.detailUpdated.textContent = formatDate(selected.updatedAt);
}

function renderEditor() {
    ensureEditorHasPage();
    renderPageThumbnails();
    renderDocumentSheet();
    renderEditorInspector();
    renderMinecraftPreview();
}

function ensureEditorHasPage() {
    if (!Array.isArray(state.editor.pages) || state.editor.pages.length === 0) {
        state.editor.pages = [""];
    }
    if (state.activePageIndex < 0 || state.activePageIndex >= state.editor.pages.length) {
        state.activePageIndex = 0;
    }
}

function renderPageThumbnails() {
    elements.pageThumbnailList.innerHTML = "";
    elements.editorBookHeading.textContent = state.editor.title || "Novo livro";

    if (state.editor.id) {
        if (isCurrentBookShared()) {
            elements.editorRoleBadge.textContent = "Colaborador";
            elements.editorRoleBadge.className = "role-badge role-badge-collaborator";
            elements.editorRoleBadge.classList.remove("hidden");
        } else if (!state.editorIsOwner) {
            elements.editorRoleBadge.textContent = "Colaborador";
            elements.editorRoleBadge.className = "role-badge role-badge-collaborator";
            elements.editorRoleBadge.classList.remove("hidden");
        } else {
            elements.editorRoleBadge.textContent = "Dono";
            elements.editorRoleBadge.className = "role-badge role-badge-owner";
            elements.editorRoleBadge.classList.remove("hidden");
        }
    } else {
        elements.editorRoleBadge.classList.add("hidden");
    }

    state.editor.pages.forEach((page, index) => {
        const thumb = document.createElement("button");
        thumb.type = "button";
        thumb.className = `page-thumb${index === state.activePageIndex ? " active" : ""}`;
        thumb.draggable = true;
        thumb.setAttribute("data-page-index", index);
        thumb.innerHTML = `
            <strong>Pagina ${index + 1}</strong>
            <p>${escapeHtml(truncate(page.trim() || "Pagina vazia.", 84))}</p>
            <small>${page.length} caractere(s)</small>
        `;
        thumb.addEventListener("click", () => {
            switchPageWithTransition(index);
        });
        thumb.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            openPageContextMenu(index, event.clientX, event.clientY);
        });

        thumb.addEventListener("dragstart", (e) => {
            state._dragPageIndex = index;
            thumb.classList.add("dragging");
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", String(index));
        });
        thumb.addEventListener("dragend", () => {
            thumb.classList.remove("dragging");
            clearDragIndicators();
            state._dragPageIndex = null;
        });
        thumb.addEventListener("dragover", (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = "move";
            clearDragIndicators();
            const rect = thumb.getBoundingClientRect();
            const midY = rect.top + rect.height / 2;
            if (e.clientY < midY) {
                thumb.classList.add("drag-over-top");
            } else {
                thumb.classList.add("drag-over-bottom");
            }
        });
        thumb.addEventListener("dragleave", () => {
            thumb.classList.remove("drag-over-top", "drag-over-bottom");
        });
        thumb.addEventListener("drop", (e) => {
            e.preventDefault();
            clearDragIndicators();
            const fromIndex = state._dragPageIndex;
            if (fromIndex == null || fromIndex === index) return;
            const rect = thumb.getBoundingClientRect();
            const midY = rect.top + rect.height / 2;
            let toIndex = e.clientY < midY ? index : index + 1;
            if (fromIndex < toIndex) toIndex--;
            if (fromIndex === toIndex) return;
            const [moved] = state.editor.pages.splice(fromIndex, 1);
            state.editor.pages.splice(toIndex, 0, moved);
            state.activePageIndex = toIndex;
            setDirty(true);
            renderEditor();
        });

        elements.pageThumbnailList.appendChild(thumb);
    });
}

function clearDragIndicators() {
    elements.pageThumbnailList.querySelectorAll(".page-thumb").forEach(t => {
        t.classList.remove("drag-over-top", "drag-over-bottom");
    });
}

function showContextMenu(x, y, items) {
    const validItems = (items || []).filter((item) => item === "separator" || (item && typeof item.label === "string"));
    if (validItems.length === 0) {
        hideContextMenu();
        return;
    }

    elements.contextMenuItems.innerHTML = "";
    validItems.forEach((item) => {
        if (item === "separator") {
            const sep = document.createElement("div");
            sep.className = "context-menu-separator";
            elements.contextMenuItems.appendChild(sep);
            return;
        }

        const button = document.createElement("button");
        button.type = "button";
        button.className = `context-menu-item${item.danger ? " danger" : ""}`;
        button.textContent = item.label;
        button.disabled = !!item.disabled;
        button.addEventListener("click", async () => {
            hideContextMenu();
            if (item.disabled || typeof item.action !== "function") {
                return;
            }
            await item.action();
        });
        elements.contextMenuItems.appendChild(button);
    });

    elements.contextMenu.classList.remove("hidden");
    elements.contextMenu.style.left = `${Math.max(8, x)}px`;
    elements.contextMenu.style.top = `${Math.max(8, y)}px`;

    requestAnimationFrame(() => {
        const rect = elements.contextMenu.getBoundingClientRect();
        const maxLeft = window.innerWidth - rect.width - 8;
        const maxTop = window.innerHeight - rect.height - 8;
        elements.contextMenu.style.left = `${Math.max(8, Math.min(x, maxLeft))}px`;
        elements.contextMenu.style.top = `${Math.max(8, Math.min(y, maxTop))}px`;
        const firstItem = elements.contextMenu.querySelector(".context-menu-item:not([disabled])");
        if (firstItem) firstItem.focus();
    });

    state._contextMenuKeyHandler = (e) => {
        const items = [...elements.contextMenuItems.querySelectorAll(".context-menu-item:not([disabled])")];
        if (items.length === 0) return;
        const idx = items.indexOf(document.activeElement);
        if (e.key === "ArrowDown") { e.preventDefault(); items[(idx + 1) % items.length].focus(); }
        else if (e.key === "ArrowUp") { e.preventDefault(); items[(idx - 1 + items.length) % items.length].focus(); }
        else if (e.key === "Home") { e.preventDefault(); items[0].focus(); }
        else if (e.key === "End") { e.preventDefault(); items[items.length - 1].focus(); }
        else if (e.key === "Tab") { e.preventDefault(); hideContextMenu(); }
    };
    elements.contextMenu.addEventListener("keydown", state._contextMenuKeyHandler);
}

function hideContextMenu() {
    elements.contextMenu.classList.add("hidden");
    elements.contextMenuItems.innerHTML = "";
    if (state._contextMenuKeyHandler) {
        elements.contextMenu.removeEventListener("keydown", state._contextMenuKeyHandler);
        state._contextMenuKeyHandler = null;
    }
}

function openPageContextMenu(pageIndex, x, y) {
    const maxPages = state.session?.limits?.maxPagesPerBook || 50;
    const canDuplicate = state.editor.pages.length < maxPages;

    showContextMenu(x, y, [
        {
            label: "Abrir pagina",
            action: async () => {
                state.activePageIndex = pageIndex;
                syncPreviewToSystemPage(pageIndex);
                renderEditor();
                focusPageEditor();
            }
        },
        {
            label: "Duplicar pagina",
            disabled: !canDuplicate,
            action: async () => {
                const content = state.editor.pages[pageIndex] || "";
                state.editor.pages.splice(pageIndex + 1, 0, content);
                state.activePageIndex = pageIndex + 1;
                syncPreviewToSystemPage(state.activePageIndex);
                setDirty(true);
                renderEditor();
            }
        },
        "separator",
        {
            label: "Mover para cima",
            disabled: pageIndex <= 0,
            action: async () => {
                state.activePageIndex = pageIndex;
                moveActivePage(-1);
            }
        },
        {
            label: "Mover para baixo",
            disabled: pageIndex >= state.editor.pages.length - 1,
            action: async () => {
                state.activePageIndex = pageIndex;
                moveActivePage(1);
            }
        },
        "separator",
        {
            label: "Excluir pagina",
            danger: true,
            disabled: state.editor.pages.length <= 1,
            action: async () => {
                state.activePageIndex = pageIndex;
                await removeActivePage();
            }
        }
    ]);
}

function openBookContextMenu(book, x, y) {
    showContextMenu(x, y, [
        {
            label: "Abrir no editor",
            action: async () => {
                await openBookInEditor(book.id);
            }
        },
        {
            label: "Duplicar livro",
            action: async () => {
                await duplicateBook(book.id);
            }
        },
        "separator",
        {
            label: book.favorite ? "Remover dos favoritos" : "Adicionar aos favoritos",
            action: async () => {
                const result = await api(`/api/books/${book.id}/favorite`, { method: "POST" });
                const summary = state.books.find((entry) => entry.id === book.id);
                if (summary) {
                    summary.favorite = !!result.favorite;
                }
                if (state.editor.id === book.id) {
                    state.editor.favorite = !!result.favorite;
                    renderEditorInspector();
                }
                renderLibrary();
                renderSidebarCategories();
                showToast(result.favorite ? "Adicionado aos favoritos." : "Removido dos favoritos.", "info");
            }
        },
        "separator",
        {
            label: "Excluir livro",
            danger: true,
            disabled: !!book.shared,
            action: async () => {
                const confirmed = await showConfirm(
                    `Excluir permanentemente o livro \"${book.title || "sem titulo"}\"? Esta acao nao pode ser desfeita.`,
                    "Excluir livro"
                );
                if (!confirmed) {
                    return;
                }
                await api(`/api/books/${book.id}`, { method: "DELETE" });
                await refreshBooks();
                showToast("Livro excluido.", "info");
            }
        }
    ]);
}

function openCategoryContextMenu(categoryName, x, y) {
    const isDefaultCategory = normalizedCategory(categoryName) === "Sem categoria";
    showContextMenu(x, y, [
        {
            label: "Filtrar categoria",
            action: async () => {
                await selectCategory(categoryName, false);
            }
        },
        "separator",
        {
            label: "Renomear categoria",
            action: () => renameCategoryFlow(categoryName)
        },
        ...(!isDefaultCategory ? [{
            label: "Excluir categoria",
            danger: true,
            action: () => deleteCategoryFlow(categoryName)
        }] : [])
    ]);
}

async function renameCategoryFlow(categoryName) {
    const maxLength = state.session?.limits?.maxCategoryLength || 32;
    const newName = await showCreateCategoryModal(maxLength, {
        title: "Renomear categoria",
        description: `Insira o novo nome para a categoria "${categoryName}".`,
        okLabel: "Renomear",
        initialValue: categoryName
    });
    if (newName == null || newName === categoryName) {
        return;
    }
    try {
        showToast("Renomeando categoria...", "info");
        await api(`/api/categories/${encodeURIComponent(categoryName)}`, {
            method: "PATCH",
            body: JSON.stringify({ newName })
        });
        state.categories = await api("/api/categories");
        await refreshBooks();
        if (state.activeCategory === categoryName) {
            state.activeCategory = normalizedCategory(newName);
        }
        syncVisibleSelection();
        renderApp();
        showToast(`Categoria renomeada para "${newName}".`, "success");
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function deleteCategoryFlow(categoryName) {
    if (normalizedCategory(categoryName) === "Sem categoria") {
        showToast("A categoria padrao nao pode ser excluida.", "error");
        return;
    }

    const target = await showDeleteCategoryModal(categoryName);
    if (target == null) {
        return;
    }
    try {
        showToast("Excluindo categoria...", "info");
        await api(`/api/categories/${encodeURIComponent(categoryName)}?target=${encodeURIComponent(target)}`, {
            method: "DELETE"
        });
        state.categories = await api("/api/categories");
        await refreshBooks();
        if (state.activeCategory === categoryName) {
            state.activeCategory = ALL_CATEGORIES;
        }
        syncVisibleSelection();
        renderApp();
        showToast(`Categoria "${categoryName}" excluida. Livros movidos para "${target}".`, "info");
    } catch (error) {
        showToast(error.message, "error");
    }
}

function renderDocumentSheet() {
    const currentPage = state.editor.pages[state.activePageIndex] || "";
    const maxCharacters = state.session?.limits.maxCharactersPerPage || 1200;
    elements.documentTitle.textContent = state.editor.title || "Pagina atual";
    elements.sheetBookLabel.textContent = (state.editor.title || "Livro novo").toUpperCase();
    elements.sheetPageLabel.textContent = `Pagina ${state.activePageIndex + 1} de ${state.editor.pages.length}`;
    elements.sheetCharCount.textContent = `${currentPage.length}/${maxCharacters}`;
    elements.editorPageTextarea.value = currentPage;
}

function renderTags() {
    const tags = state.editor.tags || [];
    if (tags.length === 0) {
        elements.editorTagsList.innerHTML = '';
        return;
    }
    elements.editorTagsList.innerHTML = tags.map((tag, i) =>
        `<span class="tag-pill">${escapeHtml(tag)}<button type="button" onclick="removeTag(${i})" class="tag-remove">&times;</button></span>`
    ).join('');
}

function removeTag(index) {
    state.editor.tags.splice(index, 1);
    setDirty(true);
    renderTags();
}

function renderEditorInspector() {
    const maxPages = state.session?.limits.maxPagesPerBook || 50;
    const categoryLabel = normalizedCategory(state.editor.category);
    const updatedLabel = state.editor.updatedAt ? formatDate(state.editor.updatedAt) : null;

    elements.editorTitleInput.value = state.editor.title;
    elements.editorCategoryInput.value = state.editor.category || "";
    elements.editorAuthorInput.value = state.editor.author || "";
    elements.editorLoreInput.value = state.editor.exportLore || "";
    elements.editorDisplayColorSelect.value = state.editor.exportDisplayColor || "";
    elements.editorGlowCheckbox.checked = !!state.editor.exportGlow;
    renderTags();
    elements.editorPageStat.textContent = `${state.editor.pages.length}/${maxPages}`;
    elements.editorCategoryStat.textContent = categoryLabel;
    elements.editorDeleteBookButton.disabled = !state.editor.id || isCurrentBookShared();
    elements.editorFavoriteButton.disabled = !state.editor.id;
    elements.editorFavoriteButton.classList.toggle("is-favorite", !!state.editor.favorite);
    elements.editorAddPageButton.disabled = state.editor.pages.length >= maxPages;
    elements.editorPageRemoveButton.disabled = state.editor.pages.length <= 1;
    elements.editorPageUpButton.disabled = state.activePageIndex === 0;
    elements.editorPageDownButton.disabled = state.activePageIndex >= state.editor.pages.length - 1;

    if (state.dirty) {
        elements.editorStatus.textContent = "Alteracoes pendentes. Salve para gravar o livro.";
    } else if (updatedLabel) {
        elements.editorStatus.textContent = `Salvo em ${updatedLabel}. Categoria: ${categoryLabel}.`;
    } else {
        elements.editorStatus.textContent = "Livro novo, ainda nao salvo.";
    }
}

/* ===== FORMATTING ===== */

const MC_COLOR_MAP = {
    "0": "#000000", "1": "#0000AA", "2": "#00AA00", "3": "#00AAAA",
    "4": "#AA0000", "5": "#AA00AA", "6": "#FFAA00", "7": "#AAAAAA",
    "8": "#555555", "9": "#5555FF", "a": "#55FF55", "b": "#55FFFF",
    "c": "#FF5555", "d": "#FF55FF", "e": "#FFFF55", "f": "#FFFFFF"
};

function insertFormatCode(code) {
    const ta = elements.editorPageTextarea;
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const text = ta.value;
    const formatStr = "\u00A7" + code;

    let newText;
    let cursorPos;

    if (start !== end) {
        const selected = text.substring(start, end);
        if (code === "r") {
            newText = text.substring(0, start) + selected + formatStr + text.substring(end);
            cursorPos = end + formatStr.length;
        } else {
            newText = text.substring(0, start) + formatStr + selected + "\u00A7r" + text.substring(end);
            cursorPos = start + formatStr.length + selected.length + 2;
        }
    } else {
        newText = text.substring(0, start) + formatStr + text.substring(end);
        cursorPos = start + formatStr.length;
    }

    ta.value = newText;
    ta.setSelectionRange(cursorPos, cursorPos);
    ta.focus();

    ensureEditorHasPage();
    state.editor.pages[state.activePageIndex] = ta.value;
    setDirty(true);
    renderEditor();
}

function convertMarkdownToSectionCodes(text) {
    return text
        .replace(/\*\*(.+?)\*\*/g, '\u00A7l$1\u00A7r')
        .replace(/\*(.+?)\*/g, '\u00A7o$1\u00A7r')
        .replace(/~~(.+?)~~/g, '\u00A7m$1\u00A7r')
        .replace(/__(.+?)__/g, '\u00A7n$1\u00A7r');
}

function renderFormattedText(rawText) {
    const converted = convertMarkdownToSectionCodes(rawText);
    const escaped = escapeHtml(converted);
    const parts = escaped.split(/\u00A7([0-9a-fk-or])/gi);
    if (parts.length === 1) return escaped;

    let html = "";
    let color = null;
    let bold = false;
    let italic = false;
    let underline = false;
    let strikethrough = false;

    function openSpan(text) {
        if (!text) return "";
        const styles = [];
        if (color) styles.push("color:" + color);
        if (bold) styles.push("font-weight:bold");
        if (italic) styles.push("font-style:italic");
        const decorations = [];
        if (underline) decorations.push("underline");
        if (strikethrough) decorations.push("line-through");
        if (decorations.length > 0) styles.push("text-decoration:" + decorations.join(" "));
        if (styles.length === 0) return text;
        return `<span style="${styles.join(";")}">${text}</span>`;
    }

    html += openSpan(parts[0]);

    for (let i = 1; i < parts.length; i += 2) {
        const code = (parts[i] || "").toLowerCase();
        const text = parts[i + 1] || "";

        if (MC_COLOR_MAP[code]) {
            color = MC_COLOR_MAP[code];
            bold = false; italic = false; underline = false; strikethrough = false;
        } else if (code === "l") { bold = true; }
        else if (code === "o") { italic = true; }
        else if (code === "n") { underline = true; }
        else if (code === "m") { strikethrough = true; }
        else if (code === "r") { color = null; bold = false; italic = false; underline = false; strikethrough = false; }

        html += openSpan(text);
    }

    return html;
}

function syncPreviewToSystemPage(systemPageIndex) {
    state.previewPageIndex = systemPageIndex;
}

function buildPreviewPages() {
    const pages = state.editor.pages;
    if (pages.length === 0) {
        return [{ systemPage: 0, text: "" }];
    }
    return pages.map((text, i) => ({ systemPage: i, text: text || "" }));
}

function renderMinecraftPreview() {
    const visualPages = buildPreviewPages();
    const totalVisual = visualPages.length;

    if (state.previewPageIndex >= totalVisual) {
        state.previewPageIndex = Math.max(0, totalVisual - 1);
    }

    const current = visualPages[state.previewPageIndex] || { text: "", systemPage: 0 };
    const bookTitle = state.editor.title || "Sem titulo";

    elements.mcPreviewTitle.textContent = bookTitle;
    elements.mcPreviewText.innerHTML = current.text ? renderFormattedText(current.text) : "...";
    elements.mcPreviewPageNumber.textContent = `Pag. ${state.previewPageIndex + 1} / ${totalVisual}`;
    elements.mcPreviewNavLabel.textContent = `${state.previewPageIndex + 1} / ${totalVisual}`;
    elements.mcPreviewPrev.disabled = state.previewPageIndex <= 0;
    elements.mcPreviewNext.disabled = state.previewPageIndex >= totalVisual - 1;

    elements.editorExportBookButton.disabled = !state.editor.id;
}

async function renderSnapshots() {
    if (!state.editor.id) {
        elements.snapshotList.innerHTML = '<p class="snapshot-empty">Salve o livro para gerar versoes.</p>';
        return;
    }

    try {
        const snapshots = await api(`/api/books/${state.editor.id}/history`);
        if (snapshots.length === 0) {
            elements.snapshotList.innerHTML = '<p class="snapshot-empty">Nenhuma versao anterior.</p>';
            return;
        }

        elements.snapshotList.innerHTML = snapshots.map(s => {
            const date = formatDate(s.snapshotAt);
            const safeTitle = escapeHtml(s.title);
            return `<div class="snapshot-item">
                <div class="snapshot-info">
                    <strong>${safeTitle}</strong>
                    <span>${date} &middot; ${s.pageCount} pag.</span>
                </div>
                <div class="snapshot-actions">
                    <button class="btn btn-ghost btn-sm" onclick="previewSnapshotFlow(${s.id})" type="button">Visualizar</button>
                    <button class="btn btn-ghost btn-sm" onclick="restoreSnapshot(${s.id})" type="button">Restaurar</button>
                </div>
            </div>`;
        }).join('');
    } catch (error) {
        elements.snapshotList.innerHTML = '<p class="snapshot-empty">Falha ao carregar historico.</p>';
    }
}

async function previewSnapshotFlow(snapshotId) {
    if (!state.editor.id) return;

    try {
        const snapshot = await api(`/api/books/${state.editor.id}/history/${snapshotId}`);
        elements.snapshotPreviewTitle.textContent = snapshot.title || "Versao anterior";
        elements.snapshotPreviewDate.textContent = formatDate(snapshot.snapshotAt);
        elements.snapshotPreviewCategory.textContent = normalizedCategory(snapshot.category);
        elements.snapshotPreviewPages.textContent = `${(snapshot.pages || []).length} pagina(s)`;

        elements.snapshotPreviewContent.innerHTML = "";
        (snapshot.pages || []).forEach((page, index) => {
            const pageDiv = document.createElement("div");
            pageDiv.className = "snapshot-preview-page";
            pageDiv.innerHTML = `
                <div class="snapshot-preview-page-title">Pagina ${index + 1}</div>
                <div class="snapshot-preview-page-text">${renderFormattedText(page || "")}</div>
            `;
            elements.snapshotPreviewContent.appendChild(pageDiv);
        });

        elements.snapshotPreviewModal.classList.remove("hidden");

        const cleanup = () => {
            elements.snapshotPreviewModal.classList.add("hidden");
            elements.snapshotPreviewClose.removeEventListener("click", onClose);
            elements.snapshotPreviewRestore.removeEventListener("click", onRestore);
            elements.snapshotPreviewModal.removeEventListener("click", onBackdrop);
        };

        const onClose = () => cleanup();
        const onRestore = async () => {
            cleanup();
            await restoreSnapshot(snapshotId);
        };
        const onBackdrop = (e) => {
            if (e.target === elements.snapshotPreviewModal) cleanup();
        };

        elements.snapshotPreviewClose.addEventListener("click", onClose);
        elements.snapshotPreviewRestore.addEventListener("click", onRestore);
        elements.snapshotPreviewModal.addEventListener("click", onBackdrop);
    } catch (error) {
        showToast(error.message || "Falha ao carregar preview.", "error");
    }
}

async function restoreSnapshot(snapshotId) {
    const confirmed = await showConfirm(
        "Restaurar esta versao? O conteudo atual sera substituido (mas ficara salvo no historico).",
        "Restaurar versao"
    );
    if (!confirmed) return;

    try {
        const restored = await api(`/api/books/${state.editor.id}/restore/${snapshotId}`, { method: "POST" });
        state.editor = {
            id: restored.id,
            title: restored.title,
            category: restored.category,
            author: state.editor.author || "",
            exportLore: state.editor.exportLore || "",
            exportDisplayColor: state.editor.exportDisplayColor || "",
            exportGlow: !!state.editor.exportGlow,
            tags: Array.isArray(restored.tags) ? [...restored.tags] : [],
            favorite: state.editor.favorite || false,
            pages: Array.isArray(restored.pages) && restored.pages.length > 0 ? [...restored.pages] : [""],
            createdAt: restored.createdAt,
            updatedAt: restored.updatedAt
        };
        state.activePageIndex = 0;
        state.previewPageIndex = 0;
        setDirty(false);
        await refreshBooks(restored.id);
        renderApp();
        renderSnapshots();
        renderCollaborators();
        showToast("Versao restaurada com sucesso!", "success");
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function renderCollaborators() {
    if (!state.editor.id) {
        elements.collaboratorAdd.classList.add("hidden");
        elements.collaboratorList.innerHTML = '<p class="collab-empty">Salve o livro para gerenciar colaboradores.</p>';
        return;
    }

    try {
        const data = await api(`/api/books/${state.editor.id}/collaborators`);
        const isOwner = data.isOwner;
        const collaborators = data.collaborators || [];

        state.editorIsOwner = isOwner;
        elements.collaboratorAdd.classList.toggle("hidden", !isOwner);

        if (!isOwner) {
            elements.collaboratorList.innerHTML = '<p class="collab-empty">Somente o dono pode gerenciar colaboradores.</p>';
            return;
        }

        if (collaborators.length === 0) {
            elements.collaboratorList.innerHTML = '<p class="collab-empty">Nenhum colaborador.</p>';
            return;
        }

        elements.collaboratorList.innerHTML = collaborators.map(c => {
            const safeName = escapeHtml(c.collaboratorName);
            return `<div class="collab-item">
                <span>${safeName}</span>
                <button class="btn btn-ghost btn-sm" onclick="removeCollaborator('${c.collaboratorUuid}')" type="button" title="Remover">&times;</button>
            </div>`;
        }).join('');
    } catch (error) {
        elements.collaboratorList.innerHTML = '<p class="collab-empty">Falha ao carregar colaboradores.</p>';
    }
}

async function addCollaborator(bookId, playerName) {
    try {
        const result = await api(`/api/books/${bookId}/collaborators`, {
            method: "POST",
            body: JSON.stringify({ playerName })
        });
        showToast(result.message || "Colaborador adicionado!", "success");
        elements.collaboratorNameInput.value = "";
        renderCollaborators();
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function removeCollaborator(collaboratorUuid) {
    if (!state.editor.id) return;
    const confirmed = await showConfirm("Remover este colaborador?", "Remover colaborador");
    if (!confirmed) return;

    try {
        await api(`/api/books/${state.editor.id}/collaborators/${collaboratorUuid}`, { method: "DELETE" });
        showToast("Colaborador removido.", "success");
        renderCollaborators();
    } catch (error) {
        showToast(error.message, "error");
    }
}

function updateMetrics() {
    elements.metricBooks.textContent = String(state.books.length);
    elements.metricPages.textContent = String(state.books.reduce((sum, book) => sum + (book.pageCount || 0), 0));
    elements.metricCategories.textContent = String(getAllCategoryStats().length);
    elements.metricFavorites.textContent = String(state.books.filter(b => b.favorite).length);
    elements.metricShared.textContent = String(state.books.filter(b => b.shared).length);
}

function toggleViews() {
    elements.viewDashboard.classList.toggle("hidden", state.view !== "dashboard");
    elements.viewLibrary.classList.toggle("hidden", state.view !== "library");
    elements.viewEditor.classList.toggle("hidden", state.view !== "editor");
}

/* ===== SAVE / DELETE / PAGES ===== */

async function saveCurrentBook() {
    try {
        const savedBook = await api("/api/books", {
            method: "POST",
            body: JSON.stringify({
                id: state.editor.id,
                title: state.editor.title,
                category: state.editor.category,
                tags: state.editor.tags,
                pages: state.editor.pages
            })
        });

        state.editor = {
            id: savedBook.id,
            title: savedBook.title,
            category: savedBook.category,
            author: state.editor.author || "",
            exportLore: state.editor.exportLore || "",
            exportDisplayColor: state.editor.exportDisplayColor || "",
            exportGlow: !!state.editor.exportGlow,
            tags: Array.isArray(savedBook.tags) ? [...savedBook.tags] : [],
            pages: Array.isArray(savedBook.pages) && savedBook.pages.length > 0 ? [...savedBook.pages] : [""],
            createdAt: savedBook.createdAt,
            updatedAt: savedBook.updatedAt
        };
        state.selectedBookId = savedBook.id;
        state.selectedBookSummaryId = savedBook.id;
        setDirty(false);
        await refreshBooks(savedBook.id);
        state.view = "editor";
        normalizeRoute(true);
        renderApp();
        renderSnapshots();
        renderCollaborators();
        showToast("Livro salvo com sucesso!", "success");
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function deleteCurrentBook() {
    if (!state.editor.id) {
        return;
    }

    const confirmed = await showConfirm(
        `Excluir permanentemente o livro "${state.editor.title || "sem titulo"}"? Esta acao nao pode ser desfeita.`,
        "Excluir livro"
    );
    if (!confirmed) {
        return;
    }

    try {
        await api(`/api/books/${state.editor.id}`, { method: "DELETE" });
        state.editor = createEmptyDraft(prefillCategory());
        state.activePageIndex = 0;
        state.selectedBookId = null;
        setDirty(false);
        await refreshBooks();
        state.view = "library";
        normalizeRoute(true);
        renderApp();
        showToast("Livro excluido.", "info");
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function toggleFavorite() {
    if (!state.editor.id) return;
    try {
        const result = await api(`/api/books/${state.editor.id}/favorite`, { method: "POST" });
        state.editor.favorite = result.favorite;
        const summary = state.books.find(b => b.id === state.editor.id);
        if (summary) summary.favorite = result.favorite;
        renderEditorInspector();
        renderLibrary();
        showToast(result.favorite ? "Adicionado aos favoritos." : "Removido dos favoritos.", "info");
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function duplicateBook(bookId) {
    try {
        const duplicated = await api(`/api/books/${bookId}/duplicate`, { method: "POST" });
        await refreshBooks(duplicated.id);
        state.selectedBookSummaryId = duplicated.id;
        renderApp();
        showToast("Livro duplicado com sucesso!", "success");
    } catch (error) {
        showToast(error.message, "error");
    }
}

async function exportCurrentBook() {
    if (!state.editor.id) {
        showToast("Salve o livro antes de exportar.", "error");
        return;
    }

    if (state.dirty) {
        const confirmSave = await showConfirm("O livro tem alteracoes nao salvas. Deseja salvar antes de exportar?", "Salvar antes de exportar");
        if (confirmSave) {
            await saveCurrentBook();
        }
    }

    try {
        const body = {};
        if (state.editor.author && state.editor.author.trim()) {
            body.author = state.editor.author.trim();
        }
        const lore = (state.editor.exportLore || "").trim();
        if (lore) body.lore = lore;
        const displayColor = state.editor.exportDisplayColor || "";
        if (displayColor) body.displayColor = displayColor;
        if (state.editor.exportGlow) body.glow = true;
        const result = await api(`/api/books/${state.editor.id}/export`, { method: "POST", body: JSON.stringify(body) });
        showToast(result.message || "Livro exportado para o inventario!", "success");
    } catch (error) {
        showToast(error.message, "error");
    }
}

function addPage() {
    const maxPages = state.session?.limits.maxPagesPerBook || 50;
    if (state.editor.pages.length >= maxPages) {
        showToast(`Limite de ${maxPages} pagina(s) por livro atingido.`, "error");
        return;
    }

    state.editor.pages.push("");
    state.activePageIndex = state.editor.pages.length - 1;
    setDirty(true);
    renderEditor();
    focusPageEditor();
}

function moveActivePage(direction) {
    const targetIndex = state.activePageIndex + direction;
    if (targetIndex < 0 || targetIndex >= state.editor.pages.length) {
        return;
    }

    const nextPages = [...state.editor.pages];
    [nextPages[state.activePageIndex], nextPages[targetIndex]] = [nextPages[targetIndex], nextPages[state.activePageIndex]];
    state.editor.pages = nextPages;
    state.activePageIndex = targetIndex;
    setDirty(true);
    renderEditor();
}

async function removeActivePage() {
    if (state.editor.pages.length <= 1) {
        return;
    }

    const currentContent = state.editor.pages[state.activePageIndex]?.trim();
    if (currentContent) {
        const confirmed = await showConfirm("A pagina ativa tem conteudo. Deseja excluir mesmo assim?", "Excluir pagina");
        if (!confirmed) {
            return;
        }
    }

    state.editor.pages.splice(state.activePageIndex, 1);
    state.activePageIndex = Math.max(0, Math.min(state.activePageIndex, state.editor.pages.length - 1));
    setDirty(true);
    renderEditor();
}

/* ===== CATEGORY HELPERS ===== */

async function selectCategory(category, navigateToLibrary) {
    if (navigateToLibrary && !(await canLeaveCurrentContext())) {
        return;
    }

    state.activeCategory = normalizedCategory(category);
    syncVisibleSelection();

    if (navigateToLibrary) {
        state.view = "library";
        normalizeRoute(false);
    }

    renderApp();
}

function createCategoryButton(label, value, count) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `cat-btn${state.activeCategory === value ? " active" : ""}`;
    button.innerHTML = `
        <span>${escapeHtml(label)}</span>
        <span class="cat-count">${count}</span>
    `;
    button.addEventListener("click", () => {
        selectCategory(value, true);
    });
    if (value !== ALL_CATEGORIES && value !== FAVORITES_FILTER) {
        button.addEventListener("contextmenu", (e) => {
            e.preventDefault();
            openCategoryContextMenu(value, e.clientX, e.clientY);
        });
    }
    return button;
}

function createLibraryChip(label, value, count) {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = `chip${state.activeCategory === value ? " active" : ""}`;
    chip.innerHTML = `<span>${escapeHtml(label)}</span><span class="chip-count">${count}</span>`;
    chip.addEventListener("click", () => {
        state.activeCategory = value;
        syncVisibleSelection();
        renderLibrary();
        renderSidebarCategories();
        renderTopbar();
    });
    if (value !== ALL_CATEGORIES && value !== FAVORITES_FILTER) {
        chip.addEventListener("contextmenu", (e) => {
            e.preventDefault();
            openCategoryContextMenu(value, e.clientX, e.clientY);
        });
    }
    return chip;
}

/* ===== QUERY HELPERS ===== */

function getVisibleBooks() {
    const normalizedSearch = state.searchTerm.toLowerCase();
    return state.books.filter((book) => {
        const categoryName = normalizedCategory(book.category);
        const matchesCategory = state.activeCategory === ALL_CATEGORIES
            || categoryName === state.activeCategory;
        const matchesFavorites = !state.filterFavorites || book.favorite;
        const matchesTags = state.filterTags.length === 0
            || state.filterTags.every(t => book.tags && book.tags.includes(t));
        const matchesSearch = normalizedSearch === ""
            || book.title.toLowerCase().includes(normalizedSearch)
            || categoryName.toLowerCase().includes(normalizedSearch)
            || (book.tags && book.tags.some(t => t.toLowerCase().includes(normalizedSearch)));
        return matchesCategory && matchesFavorites && matchesTags && matchesSearch;
    });
}

function getCategoryStats(books) {
    const counts = new Map();
    books.forEach((book) => {
        const categoryName = normalizedCategory(book.category);
        counts.set(categoryName, (counts.get(categoryName) || 0) + 1);
    });

    return [...counts.entries()]
        .map(([name, count]) => ({ name, count }))
        .sort((left, right) => {
            if (right.count !== left.count) {
                return right.count - left.count;
            }
            return left.name.localeCompare(right.name, "pt-BR");
        });
}

function getAllCategoryStats() {
    const counts = new Map();

    (state.categories || []).forEach((category) => {
        const categoryName = normalizedCategory(category);
        if (!counts.has(categoryName)) {
            counts.set(categoryName, 0);
        }
    });

    state.books.forEach((book) => {
        const categoryName = normalizedCategory(book.category);
        counts.set(categoryName, (counts.get(categoryName) || 0) + 1);
    });

    return [...counts.entries()]
        .map(([name, count]) => ({ name, count }))
        .sort((left, right) => {
            if (right.count !== left.count) {
                return right.count - left.count;
            }
            return left.name.localeCompare(right.name, "pt-BR");
        });
}

function getSelectedBookSummary() {
    if (!state.selectedBookSummaryId) {
        return null;
    }
    return state.books.find((book) => book.id === state.selectedBookSummaryId) || null;
}

function isCurrentBookShared() {
    if (!state.editor.id) return false;
    const summary = state.books.find(b => b.id === state.editor.id);
    return summary ? summary.shared === true : false;
}

function updateCategorySuggestions() {
    elements.categorySuggestions.innerHTML = "";
    getAllCategoryStats().forEach((category) => {
        const option = document.createElement("option");
        option.value = category.name;
        elements.categorySuggestions.appendChild(option);
    });
}

function prefillCategory() {
    return state.activeCategory === ALL_CATEGORIES || state.activeCategory === FAVORITES_FILTER ? "" : state.activeCategory;
}

function getDraftStorageKey() {
    const playerName = (state.session?.playerName || "unknown").toLowerCase().replace(/[^a-z0-9_\-]/g, "_");
    return `${DRAFT_STORAGE_KEY_PREFIX}:${playerName}`;
}

function hasMeaningfulDraftContent(editor) {
    if (!editor) return false;
    if ((editor.title || "").trim()) return true;
    if ((editor.category || "").trim()) return true;
    if ((editor.author || "").trim()) return true;
    if ((editor.exportLore || "").trim()) return true;
    if ((editor.exportDisplayColor || "").trim()) return true;
    if (editor.exportGlow) return true;
    if (Array.isArray(editor.tags) && editor.tags.length > 0) return true;
    return Array.isArray(editor.pages) && editor.pages.some((page) => (page || "").trim().length > 0);
}

function persistEditorDraftIfPossible() {
    if (!state.session) return;
    if (state.view !== "editor") return;
    if (!hasMeaningfulDraftContent(state.editor)) {
        clearPersistedEditorDraft();
        return;
    }

    const payload = {
        version: 1,
        savedAt: Date.now(),
        activePageIndex: state.activePageIndex,
        previewPageIndex: state.previewPageIndex,
        editor: {
            id: state.editor.id,
            title: state.editor.title || "",
            category: state.editor.category || "",
            author: state.editor.author || "",
            exportLore: state.editor.exportLore || "",
            exportDisplayColor: state.editor.exportDisplayColor || "",
            exportGlow: !!state.editor.exportGlow,
            tags: Array.isArray(state.editor.tags) ? [...state.editor.tags] : [],
            favorite: !!state.editor.favorite,
            pages: Array.isArray(state.editor.pages) && state.editor.pages.length > 0 ? [...state.editor.pages] : [""],
            createdAt: state.editor.createdAt || null,
            updatedAt: state.editor.updatedAt || null
        }
    };

    try {
        localStorage.setItem(getDraftStorageKey(), JSON.stringify(payload));
    } catch (error) {
        console.warn("Nao foi possivel persistir rascunho local.", error);
    }
}

function readPersistedEditorDraft() {
    if (!state.session) return null;
    try {
        const raw = localStorage.getItem(getDraftStorageKey());
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== "object" || !parsed.editor) return null;
        if (!hasMeaningfulDraftContent(parsed.editor)) return null;
        return parsed;
    } catch (error) {
        console.warn("Nao foi possivel ler rascunho local.", error);
        return null;
    }
}

function clearPersistedEditorDraft() {
    if (!state.session) return;
    try {
        localStorage.removeItem(getDraftStorageKey());
    } catch (error) {
        // Ignora quando storage estiver indisponivel.
    }
}

async function maybeRestorePersistedDraft() {
    const persisted = readPersistedEditorDraft();
    if (!persisted) return;

    const savedAtLabel = persisted.savedAt ? formatDate(persisted.savedAt) : "recentemente";
    const shouldRestore = await showConfirm(
        `Encontramos um rascunho local salvo em ${savedAtLabel}. Deseja restaurar esse conteudo?`,
        "Restaurar rascunho"
    );

    if (!shouldRestore) {
        return;
    }

    const restoredEditor = persisted.editor;
    state.editor = {
        id: restoredEditor.id ?? null,
        title: restoredEditor.title || "",
        category: restoredEditor.category || "",
        author: restoredEditor.author || "",
        exportLore: restoredEditor.exportLore || "",
        exportDisplayColor: restoredEditor.exportDisplayColor || "",
        exportGlow: !!restoredEditor.exportGlow,
        tags: Array.isArray(restoredEditor.tags) ? [...restoredEditor.tags] : [],
        favorite: !!restoredEditor.favorite,
        pages: Array.isArray(restoredEditor.pages) && restoredEditor.pages.length > 0 ? [...restoredEditor.pages] : [""],
        createdAt: restoredEditor.createdAt ?? null,
        updatedAt: restoredEditor.updatedAt ?? null
    };

    state.activePageIndex = Math.max(0, Math.min(Number(persisted.activePageIndex) || 0, state.editor.pages.length - 1));
    state.previewPageIndex = Math.max(0, Number(persisted.previewPageIndex) || 0);
    state.selectedBookId = state.editor.id || null;
    state.selectedBookSummaryId = state.editor.id || state.selectedBookSummaryId;
    state.view = "editor";
    setDirty(true);
    normalizeRoute(true);
    renderApp();
    if (state.editor.id) {
        renderSnapshots();
        renderCollaborators();
    }
    showToast("Rascunho local restaurado.", "success");
}

async function canLeaveCurrentContext() {
    if (!state.dirty || state.view !== "editor") {
        return true;
    }
    return showConfirm("Existem alteracoes nao salvas. Deseja descartar e trocar de tela?", "Alteracoes pendentes");
}

/* ===== API ===== */

async function api(path, options = {}) {
    const {
        suppressUnauthorizedOverlay = false,
        ...fetchOptions
    } = options;

    const headers = new Headers(fetchOptions.headers || {});
    const rememberedSessionId = getRememberedWorkspaceSession();
    if (rememberedSessionId && !headers.has("X-Midgard-Session")) {
        headers.set("X-Midgard-Session", rememberedSessionId);
    }
    if (fetchOptions.body) {
        headers.set("Content-Type", "application/json; charset=UTF-8");
    }

    const response = await fetch(path, {
        ...fetchOptions,
        headers,
        credentials: "same-origin"
    });

    if (response.status === 204) {
        return null;
    }

    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        if (response.status === 401) {
            clearRememberedWorkspaceSession();
        }
        if (response.status === 401 && !suppressUnauthorizedOverlay) {
            setAuthenticated(false, payload.error || "Sessao invalida ou ausente.");
        }
        throw new Error(payload.error || "Falha ao processar a requisicao.");
    }

    return payload;
}

/* ===== UTILITIES ===== */

function setDirty(dirty) {
    state.dirty = dirty;
    if (dirty) {
        persistEditorDraftIfPossible();
        scheduleAutoSave();
    } else {
        clearPersistedEditorDraft();
        if (autoSaveTimer) { clearTimeout(autoSaveTimer); autoSaveTimer = null; }
    }
    updateDocumentTitle();
}

function updateDocumentTitle() {
    const labels = {
        dashboard: "Painel",
        library: "Biblioteca",
        editor: state.editor.title || "Editor"
    };
    document.title = `${state.dirty ? "* " : ""}Midgard LoreMaker - ${labels[state.view]}`;
}

function normalizedCategory(value) {
    return value && value.trim() ? value.trim() : "Sem categoria";
}

function appendEmptyState(container, message) {
    const empty = document.createElement("div");
    empty.className = "empty-placeholder";
    empty.textContent = message;
    container.appendChild(empty);
}

function renderConnectionError() {
    const targets = [elements.libraryBookGrid, elements.dashboardRecentList, elements.dashboardCategoryGrid];
    targets.forEach(container => {
        container.innerHTML = "";
        const errorDiv = document.createElement("div");
        errorDiv.className = "empty-placeholder error-state";
        errorDiv.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="32" height="32" style="margin-bottom:8px;color:var(--danger)">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <p style="margin-bottom:12px">Falha ao carregar dados. Verifique sua conexao.</p>
        `;
        const retryBtn = document.createElement("button");
        retryBtn.type = "button";
        retryBtn.className = "btn btn-sm btn-primary";
        retryBtn.textContent = "Tentar novamente";
        retryBtn.addEventListener("click", async () => {
            retryBtn.disabled = true;
            retryBtn.textContent = "Carregando...";
            try {
                await refreshBooks();
                renderApp();
                showToast("Dados carregados com sucesso!", "success");
            } catch (e) {
                showToast(e.message || "Falha ao reconectar.", "error");
                retryBtn.disabled = false;
                retryBtn.textContent = "Tentar novamente";
            }
        });
        errorDiv.appendChild(retryBtn);
        container.appendChild(errorDiv);
    });
}

function truncate(value, maxLength) {
    if (value.length <= maxLength) {
        return value;
    }
    return `${value.slice(0, Math.max(0, maxLength - 3))}...`;
}

function formatDate(timestamp) {
    if (!timestamp) {
        return "-";
    }
    return new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(new Date(timestamp));
}

function focusTitleEditor() {
    window.requestAnimationFrame(() => {
        elements.editorTitleInput.focus();
        elements.editorTitleInput.select();
    });
}

function focusPageEditor() {
    window.requestAnimationFrame(() => {
        elements.editorPageTextarea.focus();
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
