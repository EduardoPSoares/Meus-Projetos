package me.ray.midgard.bot.core.database.repository;

import me.ray.midgard.bot.core.database.Database;
import me.ray.midgard.bot.core.database.RowMapper;
import me.ray.midgard.bot.core.database.query.InsertBuilder;
import me.ray.midgard.bot.core.database.query.QueryBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class Repository<T, ID> {

    protected final Database database;
    protected final String table;
    protected final String primaryKey;

    protected Repository(Database database, String table, String primaryKey) {
        this.database = database;
        this.table = table;
        this.primaryKey = primaryKey;
    }

    protected Repository(Database database, String table) {
        this(database, table, "id");
    }

    // ==================== Abstract ====================

    protected abstract RowMapper<T> getMapper();

    protected abstract Map<String, Object> toMap(T entity);

    // ==================== CRUD ====================

    public Optional<T> findById(ID id) {
        return query().where(primaryKey, id).firstOptional(getMapper());
    }

    public List<T> findAll() {
        return query().get(getMapper());
    }

    public List<T> findAll(int limit, int offset) {
        return query().limit(limit).offset(offset).get(getMapper());
    }

    public T save(T entity) {
        Map<String, Object> data = toMap(entity);
        insert().values(data).orReplace().execute();
        return entity;
    }

    public void saveAll(List<T> entities) {
        if (entities.isEmpty()) return;
        List<Map<String, Object>> rows = entities.stream().map(this::toMap).toList();
        insert().orReplace().executeBatch(rows);
    }

    public int deleteById(ID id) {
        return query().where(primaryKey, id).delete();
    }

    public int deleteAll() {
        return database.update("DELETE FROM " + table);
    }

    public boolean existsById(ID id) {
        return query().where(primaryKey, id).exists();
    }

    public long count() {
        return query().count();
    }

    // ==================== Async CRUD ====================

    public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return database.queryOptionalAsync(
                "SELECT * FROM " + table + " WHERE " + primaryKey + " = ?",
                getMapper(), id
        );
    }

    public CompletableFuture<List<T>> findAllAsync() {
        return database.queryListAsync("SELECT * FROM " + table, getMapper());
    }

    public CompletableFuture<Integer> deleteByIdAsync(ID id) {
        return database.updateAsync(
                "DELETE FROM " + table + " WHERE " + primaryKey + " = ?", id
        );
    }

    // ==================== Builders ====================

    protected QueryBuilder query() {
        return new QueryBuilder(database, table);
    }

    protected InsertBuilder insert() {
        return new InsertBuilder(database, table);
    }

    // ==================== Utility ====================

    public List<T> findByColumn(String column, Object value) {
        return query().where(column, value).get(getMapper());
    }

    public Optional<T> findOneByColumn(String column, Object value) {
        return query().where(column, value).firstOptional(getMapper());
    }

    public int deleteByColumn(String column, Object value) {
        return query().where(column, value).delete();
    }

    public int updateColumn(ID id, String column, Object value) {
        return query().where(primaryKey, id).update(column, value);
    }

    public int updateColumns(ID id, Map<String, Object> values) {
        return query().where(primaryKey, id).update(values);
    }

    public long countByColumn(String column, Object value) {
        return query().where(column, value).count();
    }
}
