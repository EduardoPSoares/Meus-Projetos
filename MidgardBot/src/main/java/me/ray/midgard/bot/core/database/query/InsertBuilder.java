package me.ray.midgard.bot.core.database.query;

import me.ray.midgard.bot.core.database.Database;

import java.util.*;

public class InsertBuilder {

    private final Database database;
    private final String table;
    private final List<String> columns = new ArrayList<>();
    private final List<Object> values = new ArrayList<>();
    private boolean orReplace = false;
    private boolean orIgnore = false;

    public InsertBuilder(Database database, String table) {
        this.database = database;
        this.table = table;
    }

    public InsertBuilder set(String column, Object value) {
        columns.add(column);
        values.add(value);
        return this;
    }

    public InsertBuilder values(Map<String, Object> data) {
        for (var entry : data.entrySet()) {
            columns.add(entry.getKey());
            values.add(entry.getValue());
        }
        return this;
    }

    public InsertBuilder orReplace() {
        this.orReplace = true;
        this.orIgnore = false;
        return this;
    }

    public InsertBuilder orIgnore() {
        this.orIgnore = true;
        this.orReplace = false;
        return this;
    }

    public int execute() {
        StringBuilder sql = new StringBuilder();
        if (orReplace) {
            if (database.isMysql()) {
                sql.append("REPLACE INTO ");
            } else {
                sql.append("INSERT OR REPLACE INTO ");
            }
        } else if (orIgnore) {
            if (database.isMysql()) {
                sql.append("INSERT IGNORE INTO ");
            } else {
                sql.append("INSERT OR IGNORE INTO ");
            }
        } else {
            sql.append("INSERT INTO ");
        }

        sql.append(table).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");

        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < columns.size(); i++) {
            placeholders.add("?");
        }
        sql.append(placeholders);
        sql.append(")");

        return database.update(sql.toString(), values.toArray());
    }

    public int[] executeBatch(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return new int[0];

        // Use columns from first row
        Map<String, Object> first = rows.get(0);
        List<String> cols = new ArrayList<>(first.keySet());

        StringBuilder sql = new StringBuilder();
        if (orReplace) {
            if (database.isMysql()) {
                sql.append("REPLACE INTO ");
            } else {
                sql.append("INSERT OR REPLACE INTO ");
            }
        } else if (orIgnore) {
            if (database.isMysql()) {
                sql.append("INSERT IGNORE INTO ");
            } else {
                sql.append("INSERT OR IGNORE INTO ");
            }
        } else {
            sql.append("INSERT INTO ");
        }

        sql.append(table).append(" (");
        sql.append(String.join(", ", cols));
        sql.append(") VALUES (");

        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < cols.size(); i++) {
            placeholders.add("?");
        }
        sql.append(placeholders).append(")");

        List<Object[]> paramSets = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object[] params = new Object[cols.size()];
            for (int i = 0; i < cols.size(); i++) {
                params[i] = row.get(cols.get(i));
            }
            paramSets.add(params);
        }

        return database.executeBatch(sql.toString(), paramSets);
    }
}
