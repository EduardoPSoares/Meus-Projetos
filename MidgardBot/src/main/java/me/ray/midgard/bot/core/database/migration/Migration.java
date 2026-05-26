package me.ray.midgard.bot.core.database.migration;

import me.ray.midgard.bot.core.database.Database;

public abstract class Migration {

    public abstract int getVersion();

    public abstract String getDescription();

    public abstract void up(Database database);

    public void down(Database database) {
        // Optional: implement rollback
    }

    @Override
    public String toString() {
        return "Migration v" + getVersion() + ": " + getDescription();
    }
}
