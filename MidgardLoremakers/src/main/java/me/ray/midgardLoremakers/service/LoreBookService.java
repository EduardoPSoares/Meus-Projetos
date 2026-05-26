package me.ray.midgardLoremakers.service;

import me.ray.midgardLoremakers.config.PluginConfiguration;
import me.ray.midgardLoremakers.data.DatabaseManager;
import me.ray.midgardLoremakers.model.BookSnapshot;
import me.ray.midgardLoremakers.model.BookSnapshotSummary;
import me.ray.midgardLoremakers.model.BookCollaborator;
import me.ray.midgardLoremakers.model.LoreBook;
import me.ray.midgardLoremakers.model.LoreBookSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LoreBookService {

    private final DatabaseManager databaseManager;
    private final PluginConfiguration.BookLimits bookLimits;

    public LoreBookService(DatabaseManager databaseManager, PluginConfiguration.BookLimits bookLimits) {
        this.databaseManager = databaseManager;
        this.bookLimits = bookLimits;
    }

    public List<LoreBookSummary> listBooks(UUID ownerUuid) {
        List<LoreBookSummary> books = new ArrayList<>(databaseManager.listBooks(ownerUuid));
        books.addAll(databaseManager.listSharedBooks(ownerUuid));

        java.util.Set<Long> favoriteIds = databaseManager.listFavoriteBookIds(ownerUuid);
        if (!favoriteIds.isEmpty()) {
            books = books.stream()
                    .map(b -> favoriteIds.contains(b.id())
                            ? new LoreBookSummary(b.id(), b.title(), b.category(), b.tags(), b.pageCount(), b.createdAt(), b.updatedAt(), b.shared(), true)
                            : b)
                    .collect(java.util.stream.Collectors.toList());
        }

        return books;
    }

    public List<String> listCategories(UUID playerUuid) {
        return databaseManager.listCategories(playerUuid);
    }

    public String createCategory(UUID playerUuid, String category) {
        String normalizedCategory = category == null ? "" : category.trim().replaceAll("\\s+", " ");
        if (normalizedCategory.isEmpty()) {
            throw new ValidationException("Informe um nome valido para a categoria.");
        }
        if (normalizedCategory.length() > bookLimits.maxCategoryLength()) {
            throw new ValidationException("A categoria excede o limite de " + bookLimits.maxCategoryLength() + " caracteres.");
        }
        long now = System.currentTimeMillis();
        databaseManager.addCategory(playerUuid, normalizedCategory, now);
        return normalizedCategory;
    }

    public String renameCategory(UUID playerUuid, String oldCategory, String newCategory) {
        String normalizedOld = oldCategory == null ? "" : oldCategory.trim().replaceAll("\\s+", " ");
        String normalizedNew = newCategory == null ? "" : newCategory.trim().replaceAll("\\s+", " ");

        if (normalizedOld.isEmpty()) {
            throw new ValidationException("Categoria original invalida.");
        }
        if (normalizedNew.isEmpty()) {
            throw new ValidationException("Informe um nome valido para a categoria.");
        }
        if (normalizedNew.length() > bookLimits.maxCategoryLength()) {
            throw new ValidationException("A categoria excede o limite de " + bookLimits.maxCategoryLength() + " caracteres.");
        }
        if (normalizedOld.equalsIgnoreCase(normalizedNew)) {
            return normalizedOld;
        }

        boolean changed = databaseManager.renameCategory(playerUuid, normalizedOld, normalizedNew);
        if (!changed) {
            throw new NotFoundException("Categoria nao encontrada.");
        }
        return normalizedNew;
    }

    public void deleteCategory(UUID playerUuid, String category, String destinationCategory) {
        String normalizedCategory = category == null ? "" : category.trim().replaceAll("\\s+", " ");
        String normalizedDestination = destinationCategory == null ? "" : destinationCategory.trim().replaceAll("\\s+", " ");
        if (normalizedCategory.isEmpty()) {
            throw new ValidationException("Categoria invalida.");
        }
        if ("Sem categoria".equalsIgnoreCase(normalizedCategory)) {
            throw new ValidationException("A categoria padrao nao pode ser excluida.");
        }
        if (normalizedDestination.isEmpty()) {
            normalizedDestination = "Sem categoria";
        }
        if (normalizedDestination.length() > bookLimits.maxCategoryLength()) {
            throw new ValidationException("A categoria excede o limite de " + bookLimits.maxCategoryLength() + " caracteres.");
        }
        if (normalizedCategory.equalsIgnoreCase(normalizedDestination)) {
            throw new ValidationException("Escolha uma categoria de destino diferente da categoria excluida.");
        }

        long now = System.currentTimeMillis();
        boolean removed = databaseManager.deleteCategory(playerUuid, normalizedCategory, normalizedDestination, now);
        if (!removed) {
            throw new NotFoundException("Categoria nao encontrada.");
        }
    }

    public Optional<LoreBook> findBook(UUID playerUuid, long bookId) {
        Optional<LoreBook> book = databaseManager.findBook(playerUuid, bookId);
        if (book.isPresent()) return book;

        Optional<LoreBook> sharedBook = databaseManager.findBookById(bookId);
        if (sharedBook.isPresent() && databaseManager.isCollaborator(bookId, playerUuid)) {
            return sharedBook;
        }

        return Optional.empty();
    }

    public LoreBook saveBook(UUID playerUuid, Long bookId, String title, String category, List<String> tags, List<String> pages) {
        List<String> normalizedPages = normalizePages(pages);
        String normalizedTitle = normalizeTitle(title);
        String normalizedCategory = normalizeCategory(category);
        List<String> normalizedTags = normalizeTags(tags);
        long now = System.currentTimeMillis();

        if (bookId == null || bookId <= 0) {
            if (databaseManager.countBooks(playerUuid) >= bookLimits.maxBooksPerPlayer()) {
                throw new ValidationException("Voce atingiu o limite de livros permitidos.");
            }
            databaseManager.addCategory(playerUuid, normalizedCategory, now);
            return databaseManager.createBook(playerUuid, normalizedTitle, normalizedCategory, normalizedTags, normalizedPages, now);
        }

        Optional<LoreBook> existing = databaseManager.findBook(playerUuid, bookId);
        if (existing.isPresent()) {
            LoreBook old = existing.get();
            databaseManager.createSnapshot(playerUuid, bookId, old.title(), old.category(), old.pages(), now);
            databaseManager.addCategory(playerUuid, normalizedCategory, now);
            return databaseManager.updateBook(playerUuid, bookId, normalizedTitle, normalizedCategory, normalizedTags, normalizedPages, now)
                    .orElseThrow(() -> new NotFoundException("O livro informado nao existe mais."));
        }

        Optional<LoreBook> sharedBook = databaseManager.findBookById(bookId);
        if (sharedBook.isPresent() && databaseManager.isCollaborator(bookId, playerUuid)) {
            LoreBook old = sharedBook.get();
            UUID ownerUuid = old.ownerUuid();
            databaseManager.createSnapshot(ownerUuid, bookId, old.title(), old.category(), old.pages(), now);
            databaseManager.addCategory(ownerUuid, normalizedCategory, now);
            return databaseManager.updateBook(ownerUuid, bookId, normalizedTitle, normalizedCategory, normalizedTags, normalizedPages, now)
                    .orElseThrow(() -> new NotFoundException("O livro informado nao existe mais."));
        }

        throw new NotFoundException("O livro informado nao existe mais.");
    }

    public void deleteBook(UUID ownerUuid, long bookId) {
        if (!databaseManager.deleteBook(ownerUuid, bookId)) {
            throw new NotFoundException("O livro informado nao existe mais.");
        }
        databaseManager.deleteSnapshotsForBook(ownerUuid, bookId);
        databaseManager.deleteCollaboratorsForBook(bookId);
        databaseManager.deleteFavoritesForBook(bookId);
    }

    public boolean toggleFavorite(UUID playerUuid, long bookId) {
        if (findBook(playerUuid, bookId).isEmpty()) {
            throw new NotFoundException("Livro nao encontrado.");
        }
        if (databaseManager.isFavorite(playerUuid, bookId)) {
            databaseManager.removeFavorite(playerUuid, bookId);
            return false;
        } else {
            long now = System.currentTimeMillis();
            databaseManager.addFavorite(playerUuid, bookId, now);
            return true;
        }
    }

    public List<BookSnapshotSummary> listSnapshots(UUID playerUuid, long bookId) {
        LoreBook book = findBook(playerUuid, bookId)
                .orElseThrow(() -> new NotFoundException("Livro nao encontrado."));
        return databaseManager.listSnapshots(book.ownerUuid(), bookId);
    }

    public BookSnapshot previewSnapshot(UUID playerUuid, long bookId, long snapshotId) {
        LoreBook book = findBook(playerUuid, bookId)
                .orElseThrow(() -> new NotFoundException("Livro nao encontrado."));
        return databaseManager.findSnapshot(book.ownerUuid(), snapshotId)
                .orElseThrow(() -> new NotFoundException("Versao nao encontrada."));
    }

    public LoreBook restoreSnapshot(UUID playerUuid, long snapshotId) {
        BookSnapshot snapshot = databaseManager.findSnapshotById(snapshotId)
                .orElseThrow(() -> new NotFoundException("Versao nao encontrada."));

        Optional<LoreBook> book = findBook(playerUuid, snapshot.bookId());
        if (book.isEmpty()) {
            throw new NotFoundException("Versao nao encontrada.");
        }

        long now = System.currentTimeMillis();
        UUID ownerUuid = book.get().ownerUuid();
        List<String> currentTags = book.get().tags();
        return databaseManager.updateBook(ownerUuid, snapshot.bookId(), snapshot.title(), snapshot.category(), currentTags, snapshot.pages(), now)
                .orElseThrow(() -> new NotFoundException("O livro original nao existe mais."));
    }

    public LoreBook duplicateBook(UUID ownerUuid, long bookId) {
        if (databaseManager.countBooks(ownerUuid) >= bookLimits.maxBooksPerPlayer()) {
            throw new ValidationException("Voce atingiu o limite de livros permitidos.");
        }

        long now = System.currentTimeMillis();
        return databaseManager.duplicateBook(ownerUuid, bookId, now)
                .orElseThrow(() -> new NotFoundException("O livro informado nao existe mais."));
    }

    public List<BookCollaborator> listCollaborators(UUID ownerUuid, long bookId) {
        if (databaseManager.findBook(ownerUuid, bookId).isEmpty()) {
            throw new NotFoundException("Livro nao encontrado.");
        }
        return databaseManager.listCollaborators(bookId);
    }

    public void addCollaborator(UUID ownerUuid, long bookId, UUID collaboratorUuid, String collaboratorName) {
        if (databaseManager.findBook(ownerUuid, bookId).isEmpty()) {
            throw new NotFoundException("Livro nao encontrado.");
        }
        if (ownerUuid.equals(collaboratorUuid)) {
            throw new ValidationException("Voce nao pode se adicionar como colaborador.");
        }
        long now = System.currentTimeMillis();
        try {
            databaseManager.addCollaborator(bookId, collaboratorUuid, collaboratorName, now);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(exception.getMessage());
        }
    }

    public void removeCollaborator(UUID ownerUuid, long bookId, UUID collaboratorUuid) {
        if (databaseManager.findBook(ownerUuid, bookId).isEmpty()) {
            throw new NotFoundException("Livro nao encontrado.");
        }
        if (!databaseManager.removeCollaborator(bookId, collaboratorUuid)) {
            throw new NotFoundException("Colaborador nao encontrado.");
        }
    }

    public boolean isOwner(UUID playerUuid, long bookId) {
        return databaseManager.findBook(playerUuid, bookId).isPresent();
    }

    public PluginConfiguration.BookLimits bookLimits() {
        return bookLimits;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null) continue;
            String trimmed = tag.trim().toLowerCase().replaceAll("\\s+", " ");
            if (trimmed.isEmpty() || trimmed.length() > 20) continue;
            if (!normalized.contains(trimmed) && normalized.size() < 10) {
                normalized.add(trimmed);
            }
        }

        return List.copyOf(normalized);
    }

    private String normalizeTitle(String title) {
        String normalizedTitle = title == null ? "" : title.trim();

        if (normalizedTitle.isEmpty()) {
            throw new ValidationException("Todo livro precisa de um titulo.");
        }
        if (normalizedTitle.length() > bookLimits.maxTitleLength()) {
            throw new ValidationException("O titulo excede o limite de " + bookLimits.maxTitleLength() + " caracteres.");
        }

        return normalizedTitle;
    }

    private String normalizeCategory(String category) {
        String normalizedCategory = category == null ? "" : category.trim().replaceAll("\\s+", " ");

        if (normalizedCategory.isEmpty()) {
            normalizedCategory = "Sem categoria";
        }
        if (normalizedCategory.length() > bookLimits.maxCategoryLength()) {
            throw new ValidationException("A categoria excede o limite de " + bookLimits.maxCategoryLength() + " caracteres.");
        }

        return normalizedCategory;
    }

    private List<String> normalizePages(List<String> pages) {
        if (pages == null || pages.isEmpty()) {
            throw new ValidationException("Todo livro precisa de pelo menos uma pagina.");
        }
        if (pages.size() > bookLimits.maxPagesPerBook()) {
            throw new ValidationException("Este livro excede o limite de " + bookLimits.maxPagesPerBook() + " paginas.");
        }

        List<String> normalizedPages = new ArrayList<>();
        for (String page : pages) {
            String normalizedPage = page == null ? "" : page.replace("\r\n", "\n");
            if (normalizedPage.length() > bookLimits.maxCharactersPerPage()) {
                throw new ValidationException("Uma das paginas excede o limite de " + bookLimits.maxCharactersPerPage() + " caracteres.");
            }
            normalizedPages.add(normalizedPage);
        }

        return List.copyOf(normalizedPages);
    }
}
