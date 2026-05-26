package me.ray.midgard.core.command;

import java.util.Collections;
import java.util.List;

/**
 * Metadados de um comando para exibição de ajuda e organização.
 */
public class CommandInfo {
    
    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final List<String> aliases;
    private final CommandCategory category;
    private final String module;
    
    private CommandInfo(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.usage = builder.usage;
        this.permission = builder.permission;
        this.aliases = builder.aliases != null ? builder.aliases : Collections.emptyList();
        this.category = builder.category;
        this.module = builder.module;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public String getPermission() { return permission; }
    public List<String> getAliases() { return aliases; }
    public CommandCategory getCategory() { return category; }
    public String getModule() { return module; }
    
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
        private String module = "core";
        
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
        
        public CommandInfo build() {
            return new CommandInfo(this);
        }
    }
}
