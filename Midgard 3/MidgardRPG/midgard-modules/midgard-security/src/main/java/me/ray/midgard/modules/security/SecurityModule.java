package me.ray.midgard.modules.security;

import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;

public class SecurityModule extends RPGModule {

    public SecurityModule() {
        super("MidgardSecurity", ModulePriority.HIGH); // High priority for security checks
    }

    @Override
    public void onEnable() {
        // ConsoleUtils.success("MidgardSecurity habilitado! Modulo de seguranca carregado.");
    }

    @Override
    public void onDisable() {
        
    }
}
