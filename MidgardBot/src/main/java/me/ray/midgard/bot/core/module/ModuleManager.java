package me.ray.midgard.bot.core.module;

import me.ray.midgard.bot.MidgardBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ModuleManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);

    private final MidgardBot bot;
    private final Map<String, BotModule> modules = new LinkedHashMap<>();

    public ModuleManager(MidgardBot bot) {
        this.bot = bot;
    }

    public void registerModule(BotModule module) {
        module.initialize(bot);
        modules.put(module.getName(), module);
        logger.info("Registered module: {} v{}", module.getName(), module.getVersion());
    }

    public void registerModules(BotModule... modules) {
        for (BotModule module : modules) {
            registerModule(module);
        }
    }

    public void enableAll() {
        // Enable in dependency order
        Set<String> enabled = new HashSet<>();
        for (BotModule module : modules.values()) {
            enableWithDependencies(module, enabled, new HashSet<>());
        }
    }

    private void enableWithDependencies(BotModule module, Set<String> enabled, Set<String> visiting) {
        if (enabled.contains(module.getName())) return;

        if (visiting.contains(module.getName())) {
            logger.error("Circular dependency detected for module: {}", module.getName());
            return;
        }

        visiting.add(module.getName());

        for (String dep : module.getDependencies()) {
            BotModule depModule = modules.get(dep);
            if (depModule == null) {
                logger.error("Missing dependency '{}' for module '{}'", dep, module.getName());
                return;
            }
            enableWithDependencies(depModule, enabled, visiting);
        }

        try {
            module.enable();
            enabled.add(module.getName());
        } catch (Exception e) {
            logger.error("Failed to enable module: {}", module.getName(), e);
        }
    }

    public void disableAll() {
        // Disable in reverse order
        List<BotModule> reversed = new ArrayList<>(modules.values());
        Collections.reverse(reversed);
        for (BotModule module : reversed) {
            if (module.isEnabled()) {
                try {
                    module.disable();
                } catch (Exception e) {
                    logger.error("Failed to disable module: {}", module.getName(), e);
                }
            }
        }
    }

    public void enableModule(String name) {
        BotModule module = modules.get(name);
        if (module != null && !module.isEnabled()) {
            enableWithDependencies(module, new HashSet<>(), new HashSet<>());
        }
    }

    public void disableModule(String name) {
        BotModule module = modules.get(name);
        if (module != null && module.isEnabled()) {
            // Check if other enabled modules depend on this one
            for (BotModule other : modules.values()) {
                if (other.isEnabled() && Arrays.asList(other.getDependencies()).contains(name)) {
                    logger.warn("Cannot disable '{}': module '{}' depends on it", name, other.getName());
                    return;
                }
            }
            module.disable();
        }
    }

    public BotModule getModule(String name) {
        return modules.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends BotModule> T getModule(String name, Class<T> type) {
        BotModule module = modules.get(name);
        if (module != null && type.isInstance(module)) {
            return (T) module;
        }
        return null;
    }

    public Collection<BotModule> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public List<BotModule> getEnabledModules() {
        List<BotModule> result = new ArrayList<>();
        for (BotModule module : modules.values()) {
            if (module.isEnabled()) result.add(module);
        }
        return result;
    }

    public int getModuleCount() {
        return modules.size();
    }

    public int getEnabledCount() {
        return (int) modules.values().stream().filter(BotModule::isEnabled).count();
    }
}
