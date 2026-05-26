package me.ray.midgard.loader.gui;

import io.lumine.mythic.core.mobs.ActiveMob;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.Attribute;
import me.ray.midgard.core.attribute.AttributeRegistry;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.integration.MythicMobsIntegration;
import me.ray.midgard.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MobDebugGui extends BaseGui {

    private final LivingEntity target;
    private final LanguageManager lang;
    private int page = 0;
    private static final int[] ATTRIBUTE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public MobDebugGui(Player player, LivingEntity target) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("loader.gui.mob_debug.title")
                .replace("<name>", (target.customName() != null ? me.ray.midgard.core.text.MessageUtils.serialize(target.customName()) : target.getName())));
        this.target = target;
        this.lang = MidgardCore.getLanguageManager();
    }

    @Override
    public void initializeItems() {
        // Fill background with dark glass panes for a modern look
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        ItemStack accent = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).setName(" ").build();
        
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        // Accent borders (top and bottom rows)
        int[] accentSlots = {0, 8, 45, 53, 1, 7, 46, 52};
        for (int slot : accentSlots) {
            inventory.setItem(slot, accent);
        }

        updateHeader();
        updateAttributes();
        updateNavigation();
        updateDebugItem();
    }

    private void updateDebugItem() {
        ItemBuilder debugItem = new ItemBuilder(Material.COMMAND_BLOCK)
                .setName("<gradient:#a855f7:#ec4899>⚙ Info de Debug</gradient>");
        
        debugItem.addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━");
        debugItem.addLore("<gray>Tamanho do Registry: <white>" + AttributeRegistry.getInstance().getAll().size());
        debugItem.addLore("<gray>Tags do Mob: <white>" + target.getScoreboardTags().size());
        debugItem.addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━");
        debugItem.addLore("");
        
        boolean hasMidgardTags = false;
        for (String tag : target.getScoreboardTags()) {
            if (tag.startsWith("midgard.")) {
                debugItem.addLore("<green>✔ <gray>" + tag);
                hasMidgardTags = true;
            }
        }
        
        if (!hasMidgardTags) {
            debugItem.addLore("<red>✘ <gray>Nenhuma tag midgard encontrada!");
            debugItem.addLore("<dark_gray>Verifique o console para logs.");
        }
        
        inventory.setItem(8, debugItem.build());
    }

    private void updateHeader() {
        String name = (target.customName() != null ? me.ray.midgard.core.text.MessageUtils.serialize(target.customName()) : target.getName());
        String type = target.getType().name();
        String health = String.format("%.1f", target.getHealth());
        var maxHealthAttr = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        String maxHealth = String.format("%.1f", maxHealthAttr != null ? maxHealthAttr.getValue() : 0.0);
        
        ItemBuilder headerItem;

        if (MythicMobsIntegration.isMythicMob(target)) {
            Optional<ActiveMob> mobOpt = MythicMobsIntegration.getActiveMob(target);
            if (mobOpt.isPresent()) {
                ActiveMob mob = mobOpt.get();
                headerItem = new ItemBuilder(Material.WITHER_SKELETON_SKULL)
                        .setName(lang.getRawMessage("loader.gui.mob_debug.mythic-info.name"));

                for (String line : lang.getStringList("loader.gui.mob_debug.mythic-info.lore")) {
                    headerItem.addLore(line
                            .replace("<internal_name>", mob.getType().getInternalName())
                            .replace("<display_name>", mob.getDisplayName())
                            .replace("<level>", String.valueOf(mob.getLevel()))
                            .replace("<faction>", (mob.getFaction() != null ? mob.getFaction() : "None"))
                            .replace("<stance>", mob.getStance())
                            .replace("<health>", health)
                            .replace("<max_health>", maxHealth));
                }
            } else {
                headerItem = new ItemBuilder(Material.SKELETON_SKULL)
                        .setName("<gradient:#ff6b6b:#ffa07a>" + name + "</gradient>")
                        .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                        .addLore("<gray>Tipo: <white>" + type)
                        .addLore("<gray>Vida: <red>" + health + "<dark_gray>/<gray>" + maxHealth + " <red>❤")
                        .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━");
            }
        } else {
            headerItem = new ItemBuilder(Material.PLAYER_HEAD)
                    .setName(lang.getRawMessage("loader.gui.mob_debug.basic-info.name"));

            for (String line : lang.getStringList("loader.gui.mob_debug.basic-info.lore")) {
                headerItem.addLore(line
                        .replace("<name>", name)
                        .replace("<type>", type)
                        .replace("<health>", health)
                        .replace("<max_health>", maxHealth)
                        .replace("<uuid>", target.getUniqueId().toString()));
            }
        }

        inventory.setItem(4, headerItem.build());
    }

    private void updateAttributes() {
        Map<String, Double> mobAttributes = MythicMobsIntegration.getAttributes(target);
        List<Attribute> allAttributes = new ArrayList<>(AttributeRegistry.getInstance().getAll());
        
        // Debug logs
        System.out.println("[MidgardDebug] Target: " + target.getName());
        System.out.println("[MidgardDebug] Registry Size: " + allAttributes.size());
        System.out.println("[MidgardDebug] Mob Tags: " + target.getScoreboardTags());
        System.out.println("[MidgardDebug] Detected Attributes: " + mobAttributes);
        
        // Sort attributes by Category then Name
        allAttributes.sort(Comparator.comparingInt((Attribute a) -> getCategoryWeight(a.getId()))
                .thenComparing(Attribute::getName));

        List<Attribute> activeAttributes = new ArrayList<>();
        for (Attribute attr : allAttributes) {
            // Show attribute if it is present in the mob's data (even if 0), OR if it's not 0 (from other sources if any)
            if (mobAttributes.containsKey(attr.getId())) {
                activeAttributes.add(attr);
            }
        }

        int startIndex = page * ATTRIBUTE_SLOTS.length;
        
        if (activeAttributes.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName("<red><bold>✘ Nenhum Atributo Encontrado</bold>")
                    .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                    .addLore("<gray>Este mob não possui atributos Midgard.")
                    .addLore("<gray>Verifique se a mecânica")
                    .addLore("<yellow>'midgard-set-attribute'<gray> está funcionando.")
                    .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                    .build());
            return;
        }
        
        for (int i = 0; i < ATTRIBUTE_SLOTS.length; i++) {
            int slot = ATTRIBUTE_SLOTS[i];
            int attrIndex = startIndex + i;

            if (attrIndex < activeAttributes.size()) {
                Attribute attr = activeAttributes.get(attrIndex);
                double value = mobAttributes.get(attr.getId());

                Material iconMat = getMaterialForAttribute(attr);
                
                // Fetch localized name and lore
                String langKey = "core.attributes." + attr.getId();
                String displayName = lang.getRawMessage(langKey + ".name");
                if (displayName.contains("Key not found")) displayName = "<gradient:#a855f7:#ec4899>✦ " + attr.getName() + "</gradient>";
                
                List<String> description = lang.getStringList(langKey + ".lore");
                
                String suffix = attr.getFormat().contains("%") ? "<gray>%" : "";
                String valueColor = value > 0 ? "<green>" : (value < 0 ? "<red>" : "<gray>");
                
                ItemBuilder attrItem = new ItemBuilder(iconMat)
                        .setName(displayName)
                        .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                        .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                        .addLore("<gray>ID: <dark_gray>" + attr.getId())
                        .addLore("<gray>Valor: " + valueColor + String.format("%.1f", value) + suffix)
                        .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                        .addLore("");
                
                for (String line : description) {
                    attrItem.addLore(line);
                }

                inventory.setItem(slot, attrItem.build());
            } else {
                inventory.setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build());
            }
        }
    }

    private void updateNavigation() {
        // Previous Page
        if (page > 0) {
            inventory.setItem(48, new ItemBuilder(Material.ARROW)
                    .setName("<gradient:#a855f7:#ec4899>◀ Página Anterior</gradient>")
                    .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                    .addLore("<gray>Clique para voltar")
                    .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                    .build());
        } else {
            inventory.setItem(48, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build());
        }
        
        // Botão Fechar (sempre no centro)
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .setName("<red><bold>✘ Fechar</bold>")
                .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                .addLore("<gray>Clique para fechar este menu")
                .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                .build());

        // Next Page
        Map<String, Double> mobAttributes = MythicMobsIntegration.getAttributes(target);
        long activeCount = AttributeRegistry.getInstance().getAll().stream()
                .filter(attr -> mobAttributes.containsKey(attr.getId()))
                .count();
        
        if ((page + 1) * ATTRIBUTE_SLOTS.length < activeCount) {
            inventory.setItem(50, new ItemBuilder(Material.ARROW)
                    .setName("<gradient:#a855f7:#ec4899>Próxima Página ▶</gradient>")
                    .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                    .addLore("<gray>Clique para continuar")
                    .addLore("<dark_gray>━━━━━━━━━━━━━━━━━━━━━")
                    .build());
        } else {
             inventory.setItem(50, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build());
        }
    }

    private Material getMaterialForAttribute(Attribute attr) {
        if (attr.getIcon() != null && !attr.getIcon().isEmpty()) {
            Material mat = Material.matchMaterial(attr.getIcon());
            if (mat != null) return mat;
        }
        
        String id = attr.getId().toLowerCase();
        if (id.contains("health")) return Material.APPLE;
        if (id.contains("mana")) return Material.LAPIS_LAZULI;
        if (id.contains("stamina")) return Material.FEATHER;
        if (id.contains("fire")) return Material.BLAZE_POWDER;
        if (id.contains("ice")) return Material.SNOWBALL;
        if (id.contains("light")) return Material.GLOWSTONE_DUST;
        if (id.contains("darkness")) return Material.COAL;
        if (id.contains("divine")) return Material.GOLD_NUGGET;
        if (id.contains("defense")) return Material.IRON_CHESTPLATE;
        if (id.contains("damage")) return Material.IRON_SWORD;
        if (id.contains("strength")) return Material.IRON_AXE;
        if (id.contains("intelligence")) return Material.ENCHANTED_BOOK;
        if (id.contains("dexterity")) return Material.BOW;
        if (id.contains("critical")) return Material.REDSTONE;
        if (id.contains("dodge")) return Material.SUGAR;
        if (id.contains("block")) return Material.SHIELD;
        if (id.contains("speed")) return Material.LEATHER_BOOTS;
        if (id.contains("luck")) return Material.EMERALD;
        if (id.contains("cooldown")) return Material.CLOCK;
        
        return Material.PAPER;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;
        
        int slot = event.getSlot();
        
        // Close button
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        
        // Previous page
        if (slot == 48 && page > 0) {
            page--;
            initializeItems();
            return;
        }
        
        // Next page
        if (slot == 50 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            page++;
            initializeItems();
        }
    }

    private int getCategoryWeight(String id) {
        id = id.toLowerCase();
        // 1. Vitals (Health, Mana, Stamina)
        if (id.startsWith("max_") || id.endsWith("_regen") || id.endsWith("_regen_amp")) return 1;
        
        // 2. Primary Stats
        if (id.equals("strength") || id.equals("intelligence") || id.equals("dexterity") || id.equals("defense")) return 2;
        
        // 3. Offensive Stats
        if (id.contains("damage") || id.contains("critical") || id.contains("penetration") || id.contains("accuracy") || id.contains("attack_speed")) {
            // Separate Elemental Damage to group 5
            if (isElemental(id)) return 5;
            return 3;
        }
        
        // 4. Defensive Stats
        if (id.contains("armor") || id.contains("resistance") || id.contains("block") || id.contains("dodge") || id.contains("parry") || id.contains("reduction") || id.contains("thorns")) {
            // Separate Elemental Defense to group 5
            if (isElemental(id)) return 5;
            return 4;
        }
        
        // 5. Elemental (handled above mostly, but catch remaining)
        if (isElemental(id)) return 5;
        
        // 6. Utility/Other
        return 6;
    }

    private boolean isElemental(String id) {
        return id.contains("fire") || id.contains("ice") || id.contains("light") || id.contains("darkness") || id.contains("divine");
    }
}
