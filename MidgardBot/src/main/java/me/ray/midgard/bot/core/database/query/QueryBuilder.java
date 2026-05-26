package me.ray.midgard.bot.core.database.query;

import me.ray.midgard.bot.core.database.Database;
import me.ray.midgard.bot.core.database.RowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class QueryBuilder {

    private final Database database;
    private final String table;

    private final List<String> selectColumns = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private final List<String> orderClauses = new ArrayList<>();
    private final List<String> joinClauses = new ArrayList<>();
    private String groupBy;
    private String having;
    private int limit = -1;
    private int offset = -1;
    private boolean distinct = false;

    public QueryBuilder(Database database, String table) {
        this.database = database;
        this.table = table;
    }

    // ==================== SELECT ====================

    public QueryBuilder select(String... columns) {
        for (String col : columns) {
            selectColumns.add(col);
        }
        return this;
    }

    public QueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    // ==================== WHERE ====================

    public QueryBuilder where(String column, Object value) {
        whereClauses.add(column + " = ?");
        whereParams.add(value);
        return this;
    }

    public QueryBuilder where(String column, String operator, Object value) {
        whereClauses.add(column + " " + operator + " ?");
        whereParams.add(value);
        return this;
    }

    public QueryBuilder whereNot(String column, Object value) {
        whereClauses.add(column + " != ?");
        whereParams.add(value);
        return this;
    }

    public QueryBuilder whereNull(String column) {
        whereClauses.add(column + " IS NULL");
        return this;
    }

    public QueryBuilder whereNotNull(String column) {
        whereClauses.add(column + " IS NOT NULL");
        return this;
    }

    public QueryBuilder whereLike(String column, String pattern) {
        whereClauses.add(column + " LIKE ?");
        whereParams.add(pattern);
        return this;
    }

    public QueryBuilder whereIn(String column, List<?> values) {
        if (values.isEmpty()) {
            whereClauses.add("1 = 0"); // Always false
            return this;
        }
        StringJoiner sj = new StringJoiner(", ", "(", ")");
        for (Object val : values) {
            sj.add("?");
            whereParams.add(val);
        }
        whereClauses.add(column + " IN " + sj);
        return this;
    }

    public QueryBuilder whereBetween(String column, Object low, Object high) {
        whereClauses.add(column + " BETWEEN ? AND ?");
        whereParams.add(low);
        whereParams.add(high);
        return this;
    }

    public QueryBuilder whereRaw(String rawClause, Object... params) {
        whereClauses.add(rawClause);
        for (Object p : params) {
            whereParams.add(p);
        }
        return this;
    }

    // ==================== ORDER, LIMIT, GROUP ====================

    public QueryBuilder orderBy(String column) {
        orderClauses.add(column + " ASC");
        return this;
    }

    public QueryBuilder orderByDesc(String column) {
        orderClauses.add(column + " DESC");
        return this;
    }

    public QueryBuilder orderBy(String column, String direction) {
        orderClauses.add(column + " " + direction.toUpperCase());
        return this;
    }

    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    public QueryBuilder groupBy(String column) {
        this.groupBy = column;
        return this;
    }

    public QueryBuilder having(String clause, Object... params) {
        this.having = clause;
        for (Object p : params) {
            whereParams.add(p);
        }
        return this;
    }

    // ==================== JOIN ====================

    public QueryBuilder join(String table, String condition) {
        joinClauses.add("JOIN " + table + " ON " + condition);
        return this;
    }

    public QueryBuilder leftJoin(String table, String condition) {
        joinClauses.add("LEFT JOIN " + table + " ON " + condition);
        return this;
    }

    // ==================== Execute Queries ====================

    public <T> List<T> get(RowMapper<T> mapper) {
        String sql = buildSelect();
        return database.queryList(sql, mapper, whereParams.toArray());
    }

    public <T> T first(RowMapper<T> mapper) {
        limit(1);
        String sql = buildSelect();
        return database.queryOne(sql, mapper, whereParams.toArray());
    }

    public <T> Optional<T> firstOptional(RowMapper<T> mapper) {
        return Optional.ofNullable(first(mapper));
    }

    public long count() {
        selectColumns.clear();
        selectColumns.add("COUNT(*)");
        String sql = buildSelect();
        return database.queryLong(sql, whereParams.toArray());
    }

    public long sum(String column) {
        selectColumns.clear();
        selectColumns.add("COALESCE(SUM(" + column + "), 0)");
        String sql = buildSelect();
        return database.queryLong(sql, whereParams.toArray());
    }

    public boolean exists() {
        String sql = "SELECT EXISTS(" + buildSelect() + ")";
        return database.queryInt(sql, whereParams.toArray()) == 1;
    }

    // ==================== Execute Modifications ====================

    public int delete() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(table);
        appendWhere(sql);
        return database.update(sql.toString(), whereParams.toArray());
    }

    public int update(String column, Object value) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(table)
                .append(" SET ").append(column).append(" = ?");
        List<Object> allParams = new ArrayList<>();
        allParams.add(value);
        appendWhere(sql);
        allParams.addAll(whereParams);
        return database.update(sql.toString(), allParams.toArray());
    }

    public int update(java.util.Map<String, Object> values) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");
        List<Object> allParams = new ArrayList<>();
        StringJoiner sj = new StringJoiner(", ");
        for (var entry : values.entrySet()) {
            sj.add(entry.getKey() + " = ?");
            allParams.add(entry.getValue());
        }
        sql.append(sj);
        appendWhere(sql);
        allParams.addAll(whereParams);
        return database.update(sql.toString(), allParams.toArray());
    }

    public int increment(String column, long amount) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(table)
                .append(" SET ").append(column).append(" = ").append(column).append(" + ?");
        List<Object> allParams = new ArrayList<>();
        allParams.add(amount);
        appendWhere(sql);
        allParams.addAll(whereParams);
        return database.update(sql.toString(), allParams.toArray());
    }

    public int decrement(String column, long amount) {
        return increment(column, -amount);
    }

    // ==================== SQL Building ====================

    private String buildSelect() {
        StringBuilder sql = new StringBuilder("SELECT ");
        if (distinct) sql.append("DISTINCT ");

        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }

        sql.append(" FROM ").append(table);

        for (String join : joinClauses) {
            sql.append(" ").append(join);
        }

        appendWhere(sql);

        if (groupBy != null) {
            sql.append(" GROUP BY ").append(groupBy);
        }

        if (having != null) {
            sql.append(" HAVING ").append(having);
        }

        if (!orderClauses.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderClauses));
        }

        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        if (offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }

        return sql.toString();
    }

    private void appendWhere(StringBuilder sql) {
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
    }
}
