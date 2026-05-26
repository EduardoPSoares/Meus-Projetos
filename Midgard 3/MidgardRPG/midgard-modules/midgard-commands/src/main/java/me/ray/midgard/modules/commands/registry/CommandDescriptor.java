package me.ray.midgard.modules.commands.registry;

import me.ray.midgard.core.command.CommandCategory;

import java.util.Collections;
import java.util.List;

/**
 * Descritor de comando que contém todos os metadados de um comando registrado.
 * Usado para validação, documentação e gerenciamento centralizado.
 */
public class CommandDescriptor {

    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final List<String> aliases;
    private final CommandCategory category;
    private final String module;
    private final boolean playerOnly;
    private final boolean enabled;
    private final CommandSource source;

    private CommandDescriptor(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.usage = builder.usage;
        this.permission = builder.permission;
        this.aliases = builder.aliases != null ? List.copyOf(builder.aliases) : Collections.emptyList();
        this.category = builder.category;
        this.module = builder.module;
        this.playerOnly = builder.playerOnly;
        this.enabled = builder.enabled;
        this.source = builder.source;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public CommandCategory getCategory() {
        return category;
    }

    public String getModule() {
        return module;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CommandSource getSource() {
        return source;
    }

    /**
     * Origem do comando.
     */
    public enum CommandSource {
        /** Comando registrado via /rpg (UnifiedCommandManager) */
        RPG_UNIFIED,
        /** Comando registrado via /rpg admin (AdminCommand) */
        RPG_ADMIN,
        /** Comando standalone registrado diretamente no Bukkit */
        STANDALONE,
        /** Comando de terceiros (outros plugins) */
        EXTERNAL
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String description = "";
        private String usage = "";
        private String permission = null;
        private List<String> aliases = Collections.emptyList();
        private CommandCategory category = CommandCategory.PLAYER;
        private String module = "unknown";
        private boolean playerOnly = false;
        private boolean enabled = true;
        private CommandSource source = CommandSource.RPG_UNIFIED;

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder aliases(String... aliases) {
            this.aliases = List.of(aliases);
            return this;
        }

        public Builder category(CommandCategory category) {
            this.category = category;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder playerOnly(boolean playerOnly) {
            this.playerOnly = playerOnly;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder source(CommandSource source) {
            this.source = source;
            return this;
        }

        public CommandDescriptor build() {
            return new CommandDescriptor(this);
        }
    }

    @Override
    public String toString() {
        return "CommandDescriptor{" +
                "name='" + name + '\'' +
                ", module='" + module + '\'' +
                ", category=" + category +
                ", enabled=" + enabled +
                ", source=" + source +
                '}';
    }
}
