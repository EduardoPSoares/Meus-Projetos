package me.ray.midgardLoremakers.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.ray.midgardLoremakers.model.AuthenticatedSession;
import me.ray.midgardLoremakers.model.BookCollaborator;
import me.ray.midgardLoremakers.model.BookSnapshot;
import me.ray.midgardLoremakers.model.BookSnapshotSummary;
import me.ray.midgardLoremakers.model.LoreBook;
import me.ray.midgardLoremakers.model.LoreBookSummary;
import me.ray.midgardLoremakers.model.WebPanelSession;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DatabaseManager {

    private static final Type PAGES_TYPE = new TypeToken<List<String>>() { }.getType();

    private final Path databasePath;
    private final Gson gson;
    private Connection sharedConnection;

    public DatabaseManager(Path databasePath, Gson gson) {
        this.databasePath = databasePath;
        this.gson = gson;
    }

    public void initialize() throws Exception {
        Files.createDirectories(databasePath.getParent());
        Class.forName("org.sqlite.JDBC");

        sharedConnection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        try (Statement pragma = sharedConnection.createStatement()) {
            pragma.execute("PRAGMA journal_mode = WAL");
            pragma.execute("PRAGMA foreign_keys = ON");
            pragma.execute("PRAGMA busy_timeout = 5000");
        }

        try (Statement statement = sharedConnection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS access_tokens (
                        token_hash TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL,
                        last_used_at INTEGER NOT NULL
                    )
                    """);
            ensureColumnExists(sharedConnection, "access_tokens", "consumed_at", "INTEGER");
            clearLegacyAccessTokenValues(sharedConnection);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS web_sessions (
                        session_hash TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        last_seen_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS lore_books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner_uuid TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'Sem categoria',
                        page_count INTEGER NOT NULL,
                        pages_json TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
            ensureColumnExists(sharedConnection, "lore_books", "category", "TEXT NOT NULL DEFAULT 'Sem categoria'");
            ensureColumnExists(sharedConnection, "lore_books", "tags_json", "TEXT NOT NULL DEFAULT '[]'");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_access_tokens_player_uuid ON access_tokens(player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_access_tokens_expires_at ON access_tokens(expires_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_web_sessions_player_uuid ON web_sessions(player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_web_sessions_last_seen_at ON web_sessions(last_seen_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_lore_books_owner_uuid ON lore_books(owner_uuid)");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS book_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL REFERENCES lore_books(id) ON DELETE CASCADE,
                        owner_uuid TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        page_count INTEGER NOT NULL,
                        pages_json TEXT NOT NULL,
                        snapshot_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_snapshots_book_id ON book_snapshots(book_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_snapshots_owner_uuid ON book_snapshots(owner_uuid)");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS book_collaborators (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL REFERENCES lore_books(id) ON DELETE CASCADE,
                        collaborator_uuid TEXT NOT NULL,
                        collaborator_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        UNIQUE(book_id, collaborator_uuid)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_collab_book ON book_collaborators(book_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_collab_player ON book_collaborators(collaborator_uuid)");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS book_favorites (
                        player_uuid TEXT NOT NULL,
                        book_id INTEGER NOT NULL REFERENCES lore_books(id) ON DELETE CASCADE,
                        created_at INTEGER NOT NULL,
                        PRIMARY KEY(player_uuid, book_id)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_favorites_player ON book_favorites(player_uuid)");

            migrateBookRelationsForForeignKeys(sharedConnection);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS book_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_book_categories_owner_name ON book_categories(player_uuid, name COLLATE NOCASE)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_categories_owner ON book_categories(player_uuid)");

            statement.execute("""
                    INSERT OR IGNORE INTO book_categories(player_uuid, name, created_at)
                    SELECT owner_uuid, category, updated_at
                    FROM lore_books
                    WHERE category IS NOT NULL AND TRIM(category) <> ''
                    """);
        }
    }

    public void purgeExpiredTokens(long now) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM access_tokens WHERE expires_at < ?")) {
            statement.setLong(1, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao limpar tokens expirados", exception);
        }
    }

    public void purgeIdleWebSessions(long minimumLastSeenAt) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM web_sessions WHERE last_seen_at < ?")) {
            statement.setLong(1, minimumLastSeenAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao limpar sessoes web inativas", exception);
        }
    }

    public void storeAccessToken(UUID playerUuid, String playerName, String tokenHash, long createdAt, long expiresAt) {
        String sql = """
                INSERT INTO access_tokens(token_hash, player_uuid, player_name, created_at, expires_at, last_used_at, consumed_at)
                VALUES(?, ?, ?, ?, ?, ?, NULL)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            statement.setString(2, playerUuid.toString());
            statement.setString(3, playerName);
            statement.setLong(4, createdAt);
            statement.setLong(5, expiresAt);
            statement.setLong(6, createdAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao salvar token de acesso", exception);
        }
    }

    public Optional<AuthenticatedSession> consumeAccessToken(String tokenHash, long now) {
        String sql = """
                SELECT player_uuid, player_name, expires_at, consumed_at
                FROM access_tokens
                WHERE token_hash = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);

            UUID playerUuid;
            String playerName;
            long expiresAt;
            Object consumedAt;

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
                playerName = resultSet.getString("player_name");
                expiresAt = resultSet.getLong("expires_at");
                consumedAt = resultSet.getObject("consumed_at");
            }

            if (expiresAt < now) {
                deleteToken(connection, tokenHash);
                return Optional.empty();
            }
            if (consumedAt != null) {
                return Optional.empty();
            }

            if (!markTokenConsumed(connection, tokenHash, now)) {
                return Optional.empty();
            }

            return Optional.of(new AuthenticatedSession(
                    playerUuid,
                    playerName,
                    expiresAt
            ));
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao consumir token de acesso", exception);
        }
    }

    public void storeWebSession(UUID playerUuid, String playerName, String sessionHash, long createdAt) {
        String sql = """
                INSERT INTO web_sessions(session_hash, player_uuid, player_name, created_at, last_seen_at)
                VALUES(?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sessionHash);
            statement.setString(2, playerUuid.toString());
            statement.setString(3, playerName);
            statement.setLong(4, createdAt);
            statement.setLong(5, createdAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao salvar sessao web", exception);
        }
    }

    public Optional<WebPanelSession> findWebSession(String sessionHash, long now, long minimumLastSeenAt) {
        String sql = """
                SELECT player_uuid, player_name, created_at, last_seen_at
                FROM web_sessions
                WHERE session_hash = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sessionHash);

            UUID playerUuid;
            String playerName;
            long createdAt;
            long lastSeenAt;

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
                playerName = resultSet.getString("player_name");
                createdAt = resultSet.getLong("created_at");
                lastSeenAt = resultSet.getLong("last_seen_at");
            }

            if (lastSeenAt < minimumLastSeenAt) {
                deleteWebSession(connection, sessionHash);
                return Optional.empty();
            }

            updateWebSessionLastSeen(connection, sessionHash, now);
            return Optional.of(new WebPanelSession(
                    playerUuid,
                    playerName,
                    createdAt,
                    now
            ));
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao localizar sessao web", exception);
        }
    }

    public int countBooks(UUID ownerUuid) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM lore_books WHERE owner_uuid = ?")) {
            statement.setString(1, ownerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao contar livros", exception);
        }
    }

    public List<LoreBookSummary> listBooks(UUID ownerUuid) {
        List<LoreBookSummary> books = new ArrayList<>();
        String sql = """
                SELECT id, title, category, tags_json, page_count, created_at, updated_at
                FROM lore_books
                WHERE owner_uuid = ?
                ORDER BY updated_at DESC, id DESC
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    List<String> tags = gson.fromJson(resultSet.getString("tags_json"), PAGES_TYPE);
                    books.add(new LoreBookSummary(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("category"),
                            tags == null ? List.of() : List.copyOf(tags),
                            resultSet.getInt("page_count"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("updated_at"),
                            false,
                            false
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao listar livros", exception);
        }

        return books;
    }

    public Optional<LoreBook> findBook(UUID ownerUuid, long bookId) {
        String sql = """
                SELECT id, title, category, tags_json, pages_json, created_at, updated_at
                FROM lore_books
                WHERE owner_uuid = ? AND id = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            statement.setLong(2, bookId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapBook(ownerUuid, resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao carregar livro", exception);
        }
    }

    public LoreBook createBook(UUID ownerUuid, String title, String category, List<String> tags, List<String> pages, long now) {
        String sql = """
                INSERT INTO lore_books(owner_uuid, title, category, tags_json, page_count, pages_json, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, ownerUuid.toString());
            statement.setString(2, title);
            statement.setString(3, category);
            statement.setString(4, gson.toJson(tags));
            statement.setInt(5, pages.size());
            statement.setString(6, gson.toJson(pages));
            statement.setLong(7, now);
            statement.setLong(8, now);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return new LoreBook(id, ownerUuid, title, category, List.copyOf(tags), List.copyOf(pages), now, now);
                }
            }

            throw new IllegalStateException("Livro criado sem chave gerada");
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao criar livro", exception);
        }
    }

    public Optional<LoreBook> updateBook(UUID ownerUuid, long bookId, String title, String category, List<String> tags, List<String> pages, long now) {
        String sql = """
                UPDATE lore_books
                SET title = ?, category = ?, tags_json = ?, page_count = ?, pages_json = ?, updated_at = ?
                WHERE owner_uuid = ? AND id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, category);
            statement.setString(3, gson.toJson(tags));
            statement.setInt(4, pages.size());
            statement.setString(5, gson.toJson(pages));
            statement.setLong(6, now);
            statement.setString(7, ownerUuid.toString());
            statement.setLong(8, bookId);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao atualizar livro", exception);
        }

        return findBook(ownerUuid, bookId);
    }

    public boolean deleteBook(UUID ownerUuid, long bookId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM lore_books WHERE owner_uuid = ? AND id = ?")) {
            statement.setString(1, ownerUuid.toString());
            statement.setLong(2, bookId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao remover livro", exception);
        }
    }

    public Optional<LoreBook> duplicateBook(UUID ownerUuid, long bookId, long now) {
        Optional<LoreBook> original = findBook(ownerUuid, bookId);
        if (original.isEmpty()) {
            return Optional.empty();
        }

        LoreBook book = original.get();
        String duplicatedTitle = book.title() + " (copia)";
        return Optional.of(createBook(ownerUuid, duplicatedTitle, book.category(), book.tags(), book.pages(), now));
    }

    public void createSnapshot(UUID ownerUuid, long bookId, String title, String category, List<String> pages, long snapshotAt) {
        String sql = """
                INSERT INTO book_snapshots(book_id, owner_uuid, title, category, page_count, pages_json, snapshot_at)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setString(2, ownerUuid.toString());
            statement.setString(3, title);
            statement.setString(4, category);
            statement.setInt(5, pages.size());
            statement.setString(6, gson.toJson(pages));
            statement.setLong(7, snapshotAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao criar snapshot", exception);
        }

        pruneSnapshots(ownerUuid, bookId, 20);
    }

    public List<BookSnapshotSummary> listSnapshots(UUID ownerUuid, long bookId) {
        List<BookSnapshotSummary> snapshots = new ArrayList<>();
        String sql = """
                SELECT id, title, page_count, snapshot_at
                FROM book_snapshots
                WHERE owner_uuid = ? AND book_id = ?
                ORDER BY snapshot_at DESC
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            statement.setLong(2, bookId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    snapshots.add(new BookSnapshotSummary(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getInt("page_count"),
                            resultSet.getLong("snapshot_at")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao listar snapshots", exception);
        }

        return snapshots;
    }

    public Optional<BookSnapshot> findSnapshot(UUID ownerUuid, long snapshotId) {
        String sql = """
                SELECT id, book_id, title, category, pages_json, snapshot_at
                FROM book_snapshots
                WHERE owner_uuid = ? AND id = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            statement.setLong(2, snapshotId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                List<String> pages = gson.fromJson(resultSet.getString("pages_json"), PAGES_TYPE);
                return Optional.of(new BookSnapshot(
                        resultSet.getLong("id"),
                        resultSet.getLong("book_id"),
                        resultSet.getString("title"),
                        resultSet.getString("category"),
                        pages == null ? List.of() : List.copyOf(pages),
                        resultSet.getLong("snapshot_at")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao carregar snapshot", exception);
        }
    }

    public Optional<BookSnapshot> findSnapshotById(long snapshotId) {
        String sql = """
                SELECT id, book_id, title, category, pages_json, snapshot_at
                FROM book_snapshots
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, snapshotId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                List<String> pages = gson.fromJson(resultSet.getString("pages_json"), PAGES_TYPE);
                return Optional.of(new BookSnapshot(
                        resultSet.getLong("id"),
                        resultSet.getLong("book_id"),
                        resultSet.getString("title"),
                        resultSet.getString("category"),
                        pages == null ? List.of() : List.copyOf(pages),
                        resultSet.getLong("snapshot_at")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao carregar snapshot por ID", exception);
        }
    }

    public void deleteSnapshotsForBook(UUID ownerUuid, long bookId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM book_snapshots WHERE owner_uuid = ? AND book_id = ?")) {
            statement.setString(1, ownerUuid.toString());
            statement.setLong(2, bookId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao remover snapshots", exception);
        }
    }

    // ===== COLLABORATOR METHODS =====

    public Optional<LoreBook> findBookById(long bookId) {
        String sql = """
                SELECT id, owner_uuid, title, category, tags_json, pages_json, created_at, updated_at
                FROM lore_books
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UUID ownerUuid = UUID.fromString(resultSet.getString("owner_uuid"));
                return Optional.of(mapBook(ownerUuid, resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao carregar livro por ID", exception);
        }
    }

    public boolean isCollaborator(long bookId, UUID playerUuid) {
        String sql = "SELECT 1 FROM book_collaborators WHERE book_id = ? AND collaborator_uuid = ? LIMIT 1";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setString(2, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao verificar colaborador", exception);
        }
    }

    public void addCollaborator(long bookId, UUID collaboratorUuid, String collaboratorName, long now) {
        String sql = "INSERT INTO book_collaborators(book_id, collaborator_uuid, collaborator_name, created_at) VALUES(?, ?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setString(2, collaboratorUuid.toString());
            statement.setString(3, collaboratorName);
            statement.setLong(4, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("UNIQUE")) {
                throw new IllegalArgumentException("Jogador ja e colaborador deste livro.");
            }
            throw new IllegalStateException("Falha ao adicionar colaborador", exception);
        }
    }

    public boolean removeCollaborator(long bookId, UUID collaboratorUuid) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM book_collaborators WHERE book_id = ? AND collaborator_uuid = ?")) {
            statement.setLong(1, bookId);
            statement.setString(2, collaboratorUuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao remover colaborador", exception);
        }
    }

    public List<BookCollaborator> listCollaborators(long bookId) {
        List<BookCollaborator> collaborators = new ArrayList<>();
        String sql = "SELECT id, book_id, collaborator_uuid, collaborator_name, created_at FROM book_collaborators WHERE book_id = ? ORDER BY created_at ASC";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    collaborators.add(new BookCollaborator(
                            resultSet.getLong("id"),
                            resultSet.getLong("book_id"),
                            UUID.fromString(resultSet.getString("collaborator_uuid")),
                            resultSet.getString("collaborator_name"),
                            resultSet.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao listar colaboradores", exception);
        }

        return collaborators;
    }

    public List<LoreBookSummary> listSharedBooks(UUID collaboratorUuid) {
        List<LoreBookSummary> books = new ArrayList<>();
        String sql = """
                SELECT b.id, b.title, b.category, b.tags_json, b.page_count, b.created_at, b.updated_at
                FROM lore_books b
                INNER JOIN book_collaborators c ON c.book_id = b.id
                WHERE c.collaborator_uuid = ?
                ORDER BY b.updated_at DESC
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, collaboratorUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    List<String> tags = gson.fromJson(resultSet.getString("tags_json"), PAGES_TYPE);
                    books.add(new LoreBookSummary(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("category"),
                            tags == null ? List.of() : List.copyOf(tags),
                            resultSet.getInt("page_count"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("updated_at"),
                            true,
                            false
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao listar livros compartilhados", exception);
        }

        return books;
    }

    public void deleteCollaboratorsForBook(long bookId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM book_collaborators WHERE book_id = ?")) {
            statement.setLong(1, bookId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao remover colaboradores do livro", exception);
        }
    }

    // ===== FAVORITES METHODS =====

    public boolean isFavorite(UUID playerUuid, long bookId) {
        String sql = "SELECT 1 FROM book_favorites WHERE player_uuid = ? AND book_id = ? LIMIT 1";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setLong(2, bookId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao verificar favorito", exception);
        }
    }

    public void addFavorite(UUID playerUuid, long bookId, long now) {
        String sql = "INSERT OR IGNORE INTO book_favorites(player_uuid, book_id, created_at) VALUES(?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setLong(2, bookId);
            statement.setLong(3, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao adicionar favorito", exception);
        }
    }

    public void removeFavorite(UUID playerUuid, long bookId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM book_favorites WHERE player_uuid = ? AND book_id = ?")) {
            statement.setString(1, playerUuid.toString());
            statement.setLong(2, bookId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao remover favorito", exception);
        }
    }

    public java.util.Set<Long> listFavoriteBookIds(UUID playerUuid) {
        java.util.Set<Long> favorites = new java.util.HashSet<>();
        String sql = "SELECT book_id FROM book_favorites WHERE player_uuid = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    favorites.add(resultSet.getLong("book_id"));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao listar favoritos", exception);
        }

        return favorites;
    }

    public void deleteFavoritesForBook(long bookId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM book_favorites WHERE book_id = ?")) {
            statement.setLong(1, bookId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao remover favoritos do livro", exception);
        }
    }

    public List<String> listCategories(UUID playerUuid) {
        List<String> categories = new ArrayList<>();
        String sql = """
                SELECT name
                FROM (
                    SELECT name FROM book_categories WHERE player_uuid = ?
                    UNION
                    SELECT category AS name FROM lore_books WHERE owner_uuid = ?
                )
                WHERE name IS NOT NULL AND TRIM(name) <> ''
                ORDER BY name COLLATE NOCASE ASC
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(resultSet.getString("name"));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao listar categorias", exception);
        }

        return categories;
    }

    public void addCategory(UUID playerUuid, String name, long now) {
        String sql = "INSERT OR IGNORE INTO book_categories(player_uuid, name, created_at) VALUES(?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, name);
            statement.setLong(3, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao criar categoria", exception);
        }
    }

    public boolean renameCategory(UUID playerUuid, String oldName, String newName) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int movedBooks;
                try (PreparedStatement booksStatement = connection.prepareStatement("""
                        UPDATE lore_books
                        SET category = ?
                        WHERE owner_uuid = ? AND category = ?
                        """)) {
                    booksStatement.setString(1, newName);
                    booksStatement.setString(2, playerUuid.toString());
                    booksStatement.setString(3, oldName);
                    movedBooks = booksStatement.executeUpdate();
                }

                try (PreparedStatement upsertStatement = connection.prepareStatement("""
                        INSERT OR IGNORE INTO book_categories(player_uuid, name, created_at)
                        VALUES(?, ?, ?)
                        """)) {
                    upsertStatement.setString(1, playerUuid.toString());
                    upsertStatement.setString(2, newName);
                    upsertStatement.setLong(3, System.currentTimeMillis());
                    upsertStatement.executeUpdate();
                }

                int renamedRows;
                try (PreparedStatement renameStatement = connection.prepareStatement("""
                        UPDATE book_categories
                        SET name = ?
                        WHERE player_uuid = ? AND LOWER(name) = LOWER(?)
                        """)) {
                    renameStatement.setString(1, newName);
                    renameStatement.setString(2, playerUuid.toString());
                    renameStatement.setString(3, oldName);
                    renamedRows = renameStatement.executeUpdate();
                }

                if (renamedRows == 0) {
                    try (PreparedStatement deleteDuplicate = connection.prepareStatement("""
                            DELETE FROM book_categories
                            WHERE player_uuid = ? AND LOWER(name) = LOWER(?)
                            """)) {
                        deleteDuplicate.setString(1, playerUuid.toString());
                        deleteDuplicate.setString(2, oldName);
                        deleteDuplicate.executeUpdate();
                    }
                }

                connection.commit();
                return renamedRows > 0 || movedBooks > 0;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao renomear categoria", exception);
        }
    }

    public boolean deleteCategory(UUID playerUuid, String categoryName, String destinationCategory, long now) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int movedBooks;
                try (PreparedStatement booksStatement = connection.prepareStatement("""
                        UPDATE lore_books
                        SET category = ?
                        WHERE owner_uuid = ? AND LOWER(category) = LOWER(?)
                        """)) {
                    booksStatement.setString(1, destinationCategory);
                    booksStatement.setString(2, playerUuid.toString());
                    booksStatement.setString(3, categoryName);
                    movedBooks = booksStatement.executeUpdate();
                }

                if (movedBooks > 0) {
                    try (PreparedStatement ensureDestinationCategory = connection.prepareStatement("""
                            INSERT OR IGNORE INTO book_categories(player_uuid, name, created_at)
                            VALUES(?, ?, ?)
                            """)) {
                        ensureDestinationCategory.setString(1, playerUuid.toString());
                        ensureDestinationCategory.setString(2, destinationCategory);
                        ensureDestinationCategory.setLong(3, now);
                        ensureDestinationCategory.executeUpdate();
                    }
                }

                int deletedRows;
                try (PreparedStatement deleteCategory = connection.prepareStatement("""
                        DELETE FROM book_categories
                        WHERE player_uuid = ? AND LOWER(name) = LOWER(?)
                        """)) {
                    deleteCategory.setString(1, playerUuid.toString());
                    deleteCategory.setString(2, categoryName);
                    deletedRows = deleteCategory.executeUpdate();
                }

                connection.commit();
                return deletedRows > 0 || movedBooks > 0;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao excluir categoria", exception);
        }
    }

    private void pruneSnapshots(UUID ownerUuid, long bookId, int maxSnapshots) {
        String sql = """
                DELETE FROM book_snapshots
                WHERE owner_uuid = ? AND book_id = ? AND id NOT IN (
                    SELECT id FROM book_snapshots
                    WHERE owner_uuid = ? AND book_id = ?
                    ORDER BY snapshot_at DESC
                    LIMIT ?
                )
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            statement.setLong(2, bookId);
            statement.setString(3, ownerUuid.toString());
            statement.setLong(4, bookId);
            statement.setInt(5, maxSnapshots);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao limpar snapshots antigos", exception);
        }
    }

    private boolean markTokenConsumed(Connection connection, String tokenHash, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE access_tokens
                SET consumed_at = ?, last_used_at = ?
                WHERE token_hash = ? AND consumed_at IS NULL
                """)) {
            statement.setLong(1, now);
            statement.setLong(2, now);
            statement.setString(3, tokenHash);
            return statement.executeUpdate() > 0;
        }
    }

    private void deleteToken(Connection connection, String tokenHash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM access_tokens WHERE token_hash = ?")) {
            statement.setString(1, tokenHash);
            statement.executeUpdate();
        }
    }

    private void deleteWebSession(Connection connection, String sessionHash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM web_sessions WHERE session_hash = ?")) {
            statement.setString(1, sessionHash);
            statement.executeUpdate();
        }
    }

    private void updateWebSessionLastSeen(Connection connection, String sessionHash, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE web_sessions SET last_seen_at = ? WHERE session_hash = ?")) {
            statement.setLong(1, now);
            statement.setString(2, sessionHash);
            statement.executeUpdate();
        }
    }

    private LoreBook mapBook(UUID ownerUuid, ResultSet resultSet) throws SQLException {
        List<String> pages = gson.fromJson(resultSet.getString("pages_json"), PAGES_TYPE);
        List<String> tags = gson.fromJson(resultSet.getString("tags_json"), PAGES_TYPE);
        return new LoreBook(
                resultSet.getLong("id"),
                ownerUuid,
                resultSet.getString("title"),
                resultSet.getString("category"),
                tags == null ? List.of() : List.copyOf(tags),
                pages == null ? List.of() : List.copyOf(pages),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }

    private void ensureColumnExists(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*") || !columnName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Nome de tabela ou coluna invalido");
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void clearLegacyAccessTokenValues(Connection connection) throws SQLException {
        if (!columnExists(connection, "access_tokens", "token_value")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("UPDATE access_tokens SET token_value = NULL WHERE token_value IS NOT NULL")) {
            statement.executeUpdate();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*") || !columnName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Nome de tabela ou coluna invalido");
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void migrateBookRelationsForForeignKeys(Connection connection) throws SQLException {
        if (!hasBookForeignKey(connection, "book_snapshots")) {
            migrateBookSnapshotsTable(connection);
        }
        if (!hasBookForeignKey(connection, "book_collaborators")) {
            migrateBookCollaboratorsTable(connection);
        }
        if (!hasBookForeignKey(connection, "book_favorites")) {
            migrateBookFavoritesTable(connection);
        }
    }

    private boolean hasBookForeignKey(Connection connection, String tableName) throws SQLException {
        if (!tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Nome de tabela invalido");
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(" + tableName + ")")) {
            while (resultSet.next()) {
                String referencedTable = resultSet.getString("table");
                String fromColumn = resultSet.getString("from");
                if ("lore_books".equalsIgnoreCase(referencedTable) && "book_id".equalsIgnoreCase(fromColumn)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void migrateBookSnapshotsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE book_snapshots RENAME TO book_snapshots_legacy");
            statement.execute("""
                    CREATE TABLE book_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL REFERENCES lore_books(id) ON DELETE CASCADE,
                        owner_uuid TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        page_count INTEGER NOT NULL,
                        pages_json TEXT NOT NULL,
                        snapshot_at INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO book_snapshots(id, book_id, owner_uuid, title, category, page_count, pages_json, snapshot_at)
                    SELECT s.id, s.book_id, s.owner_uuid, s.title, s.category, s.page_count, s.pages_json, s.snapshot_at
                    FROM book_snapshots_legacy s
                    INNER JOIN lore_books b ON b.id = s.book_id
                    """);
            statement.execute("DROP TABLE book_snapshots_legacy");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_snapshots_book_id ON book_snapshots(book_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_snapshots_owner_uuid ON book_snapshots(owner_uuid)");
        }
    }

    private void migrateBookCollaboratorsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE book_collaborators RENAME TO book_collaborators_legacy");
            statement.execute("""
                    CREATE TABLE book_collaborators (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL REFERENCES lore_books(id) ON DELETE CASCADE,
                        collaborator_uuid TEXT NOT NULL,
                        collaborator_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        UNIQUE(book_id, collaborator_uuid)
                    )
                    """);
            statement.execute("""
                    INSERT INTO book_collaborators(id, book_id, collaborator_uuid, collaborator_name, created_at)
                    SELECT c.id, c.book_id, c.collaborator_uuid, c.collaborator_name, c.created_at
                    FROM book_collaborators_legacy c
                    INNER JOIN lore_books b ON b.id = c.book_id
                    """);
            statement.execute("DROP TABLE book_collaborators_legacy");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_collab_book ON book_collaborators(book_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_collab_player ON book_collaborators(collaborator_uuid)");
        }
    }

    private void migrateBookFavoritesTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE book_favorites RENAME TO book_favorites_legacy");
            statement.execute("""
                    CREATE TABLE book_favorites (
                        player_uuid TEXT NOT NULL,
                        book_id INTEGER NOT NULL REFERENCES lore_books(id) ON DELETE CASCADE,
                        created_at INTEGER NOT NULL,
                        PRIMARY KEY(player_uuid, book_id)
                    )
                    """);
            statement.execute("""
                    INSERT INTO book_favorites(player_uuid, book_id, created_at)
                    SELECT f.player_uuid, f.book_id, f.created_at
                    FROM book_favorites_legacy f
                    INNER JOIN lore_books b ON b.id = f.book_id
                    """);
            statement.execute("DROP TABLE book_favorites_legacy");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_book_favorites_player ON book_favorites(player_uuid)");
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public synchronized void close() {
        if (sharedConnection != null) {
            try {
                sharedConnection.close();
            } catch (SQLException ignored) {
            }
            sharedConnection = null;
        }
    }
}
