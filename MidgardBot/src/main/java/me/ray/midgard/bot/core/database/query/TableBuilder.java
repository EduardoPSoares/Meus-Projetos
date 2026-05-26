package me.ray.midgard.bot.core.database.query;

import me.ray.midgard.bot.core.database.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TableBuilder {

    private final Database database;
    private final String table;
    private final List<String> columns = new ArrayList<>();
    private final List<String> constraints = new ArrayList<>();
    private boolean ifNotExists = true;

    public TableBuilder(Database database, String table) {
        this.database = database;
        this.table = table;
    }

    public TableBuilder ifNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
        return this;
    }

    // ==================== Column Types ====================

    public TableBuilder column(String name, String type) {
        columns.add(name + " " + type);
        return this;
    }

    public TableBuilder integer(String name) {
        return column(name, "INTEGER");
    }

    public TableBuilder integer(String name, boolean notNull) {
        return column(name, "INTEGER" + (notNull ? " NOT NULL" : ""));
    }

    public TableBuilder integerDefault(String name, int defaultValue) {
        return column(name, "INTEGER NOT NULL DEFAULT " + defaultValue);
    }

    public TableBuilder bigint(String name) {
        return column(name, "BIGINT");
    }

    public TableBuilder bigintDefault(String name, long defaultValue) {
        return column(name, "BIGINT NOT NULL DEFAULT " + defaultValue);
    }

    public TableBuilder text(String name) {
        return column(name, "TEXT");
    }

    public TableBuilder text(String name, boolean notNull) {
        return column(name, "TEXT" + (notNull ? " NOT NULL" : ""));
    }

    public TableBuilder textDefault(String name, String defaultValue) {
        return column(name, "TEXT NOT NULL DEFAULT '" + defaultValue.replace("'", "''") + "'");
    }

    public TableBuilder real(String name) {
        return column(name, "REAL");
    }

    public TableBuilder realDefault(String name, double defaultValue) {
        return column(name, "REAL NOT NULL DEFAULT " + defaultValue);
    }

    public TableBuilder bool(String name) {
        return column(name, "BOOLEAN NOT NULL DEFAULT 0");
    }

    public TableBuilder boolDefault(String name, boolean defaultValue) {
        return column(name, "BOOLEAN NOT NULL DEFAULT " + (defaultValue ? 1 : 0));
    }

    public TableBuilder blob(String name) {
        return column(name, "BLOB");
    }

    public TableBuilder timestamp(String name) {
        return column(name, "TIMESTAMP");
    }

    public TableBuilder timestampDefault(String name) {
        return column(name, "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
    }

    // ==================== Special Columns ====================

    public TableBuilder primaryKey(String name) {
        return column(name, "INTEGER PRIMARY KEY AUTOINCREMENT");
    }

    public TableBuilder textPrimaryKey(String name) {
        return column(name, "TEXT PRIMARY KEY");
    }

    // ==================== Constraints ====================

    public TableBuilder unique(String... columns) {
        constraints.add("UNIQUE (" + String.join(", ", columns) + ")");
        return this;
    }

    public TableBuilder primaryKey(String... columns) {
        constraints.add("PRIMARY KEY (" + String.join(", ", columns) + ")");
        return this;
    }

    public TableBuilder foreignKey(String column, String refTable, String refColumn) {
        constraints.add("FOREIGN KEY (" + column + ") REFERENCES " + refTable + "(" + refColumn + ") ON DELETE CASCADE");
        return this;
    }

    public TableBuilder foreignKey(String column, String refTable, String refColumn, String onDelete) {
        constraints.add("FOREIGN KEY (" + column + ") REFERENCES " + refTable + "(" + refColumn + ") ON DELETE " + onDelete);
        return this;
    }

    public TableBuilder check(String expression) {
        constraints.add("CHECK (" + expression + ")");
        return this;
    }

    // ==================== Index ====================

    public TableBuilder index(String name, String... columns) {
        // Executed after table creation
        database.execute("CREATE INDEX IF NOT EXISTS " + name + " ON " + table + " (" + String.join(", ", columns) + ")");
        return this;
    }

    public TableBuilder uniqueIndex(String name, String... columns) {
        database.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + name + " ON " + table + " (" + String.join(", ", columns) + ")");
        return this;
    }

    // ==================== Execute ====================

    public void execute() {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(table).append(" (\n");

        StringJoiner sj = new StringJoiner(",\n  ", "  ", "");
        for (String col : columns) {
            sj.add(col);
        }
        for (String constraint : constraints) {
            sj.add(constraint);
        }
        sql.append(sj);
        sql.append("\n)");

        database.execute(sql.toString());
    }
}
