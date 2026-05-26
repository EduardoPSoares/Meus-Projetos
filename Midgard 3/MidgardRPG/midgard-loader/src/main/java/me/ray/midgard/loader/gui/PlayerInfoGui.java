package me.ray.midgard.loader.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatAttributes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PlayerInfoGui extends BaseGui {

    private final Player target;
    private final MidgardProfile profile;
    private static final DecimalFormat DF = new DecimalFormat("#,###.##");

    public PlayerInfoGui(Player player, Player target) {
        super(player, 6, "Informações: " + target.getName());
        this.target = target;
        this.profile = MidgardCore.getProfileManager().getProfile(target.getUniqueId());
    }

    @Override
    public void initializeItems() {
        if (profile == null) {
            inventory.setItem(4, new ItemBuilder(Material.BARRIER)
                    .setName("<red>Perfil não carregado")
                    .lore(java.util.Collections.singletonList("<gray>O perfil do jogador ainda não foi carregado."))
                    .build());
            return;
        }
        
        // Cabeçalho - Informações Básicas (Slot 4)
        String ipDisplay = "Oculto";
        if (player.hasPermission("midgard.admin.seeip") && target.getAddress() != null) {
            ipDisplay = target.getAddress().getHostString();
        }
        
        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                .setName("<gold><b>" + target.getName() + "</b></gold>")
                .lore(java.util.Arrays.asList(
                        "",
                        "<gray>UUID: <white>" + target.getUniqueId(),
                        "<gray>Ping: <green>" + target.getPing() + "ms",
                        "<gray>IP: <yellow>" + ipDisplay,
                        "<gray>Gamemode: <white>" + target.getGameMode().name(),
                        "<gray>Voo: " + (target.getAllowFlight() ? "<green>Sim" : "<red>Não")
                ))
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
        
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(target);
            head.setItemMeta(meta);
        }
        inventory.setItem(4, head);

        // Stats de Combate (Slot 20)
        CombatData combatData = profile.getData(CombatData.class);
        CoreAttributeData attrData = profile.getData(CoreAttributeData.class);
        List<String> combatLore = new ArrayList<>();
        combatLore.add("");
        if (combatData != null && attrData != null) {
            combatLore.add("<gray>Saúde: <red>" + DF.format(target.getHealth()) + "/" + DF.format(getAttributeValue(attrData, CombatAttributes.MAX_HEALTH)));
            combatLore.add("<gray>Mana: <blue>" + DF.format(combatData.getCurrentMana()) + "/" + DF.format(getAttributeValue(attrData, CombatAttributes.MAX_MANA)));
            combatLore.add("<gray>Stamina: <yellow>" + DF.format(combatData.getCurrentStamina()) + "/" + DF.format(getAttributeValue(attrData, CombatAttributes.MAX_STAMINA)));
            combatLore.add("");
            combatLore.add("<gray>Dano Físico: <red>" + DF.format(getAttributeValue(attrData, CombatAttributes.PHYSICAL_DAMAGE)));
            combatLore.add("<gray>Dano Mágico: <light_purple>" + DF.format(getAttributeValue(attrData, CombatAttributes.MAGIC_DAMAGE)));
            combatLore.add("<gray>Defesa: <green>" + DF.format(getAttributeValue(attrData, CombatAttributes.DEFENSE)));
            combatLore.add("<gray>Crítico: <gold>" + DF.format(getAttributeValue(attrData, CombatAttributes.CRITICAL_CHANCE)) + "%");
        } else {
            combatLore.add("<red>Dados de combate não encontrados.");
        }

        inventory.setItem(20, new ItemBuilder(Material.IRON_SWORD)
                .setName("<red><b>Atributos de Combate</b></red>")
                .lore(combatLore)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build());

        // Progresso e Classe (Slot 22)
        ClassData classData = profile.getData(ClassData.class);
        List<String> classLore = new ArrayList<>();
        classLore.add("");
        if (classData != null) {
            classLore.add("<gray>Classe: <gold>" + (classData.hasClass() ? classData.getClassName() : "Nenhuma"));
            classLore.add("<gray>Nível: <green>" + classData.getLevel());
            classLore.add("<gray>XP: <aqua>" + DF.format(classData.getExperience()));
            classLore.add("<gray>Pontos de Atributo: <yellow>" + classData.getAttributePoints());
            // classLore.add("<gray>Pontos de Habilidade: <light_purple>" + classData.getSkillPoints()); // Removido pois não encontrei o getter
        } else {
            classLore.add("<red>Dados de classe não encontrados.");
        }

        inventory.setItem(22, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName("<green><b>Progresso</b></green>")
                .lore(classLore)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build());

        // Localização (Slot 24)
        inventory.setItem(24, new ItemBuilder(Material.COMPASS)
                .setName("<yellow><b>Localização</b></yellow>")
                .lore(java.util.Arrays.asList(
                        "",
                        "<gray>Mundo: <white>" + target.getWorld().getName(),
                        "<gray>X: <white>" + DF.format(target.getLocation().getX()),
                        "<gray>Y: <white>" + DF.format(target.getLocation().getY()),
                        "<gray>Z: <white>" + DF.format(target.getLocation().getZ()),
                        "",
                        "<yellow>Clique para teleportar"
                ))
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build());

        // Ações - Linha Inferior
        
        // Ver Inventário (Slot 48)
        inventory.setItem(48, new ItemBuilder(Material.CHEST)
                .setName("<gold><b>Ver Inventário</b></gold>")
                .lore(java.util.Collections.singletonList("<gray>Clique para abrir o inventário do jogador."))
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build());

        // Ver Ender Chest (Slot 49)
        inventory.setItem(49, new ItemBuilder(Material.ENDER_CHEST)
                .setName("<light_purple><b>Ver Baú do Fim</b></light_purple>")
                .lore(java.util.Collections.singletonList("<gray>Clique para abrir o Ender Chest do jogador."))
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build());
        
        // Kickar (Slot 50)
        inventory.setItem(50, new ItemBuilder(Material.BARRIER)
                .setName("<red><b>Expulsar Jogador</b></red>")
                .lore(java.util.Collections.singletonList("<gray>Clique para expulsar o jogador do servidor."))
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build());
                
    }

    private double getAttributeValue(CoreAttributeData data, String key) {
        AttributeInstance instance = data.getInstance(key);
        return instance != null ? instance.getValue() : 0.0;
    }

    @Override
    public void onClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        e.setCancelled(true);
        int slot = e.getSlot();

        if (slot == 24) { // Teleport
            if (!player.hasPermission("midgard.admin.info.teleport")) {
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("core.general.no-permission"));
                return;
            }
            player.closeInventory();
            player.teleport(target);
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("loader.player_info.teleport_success", "%player%", target.getName()));
        } else if (slot == 48) { // Inventário
            if (!player.hasPermission("midgard.admin.info.inventory")) {
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("core.general.no-permission"));
                return;
            }
            player.openInventory(target.getInventory());
        } else if (slot == 49) { // Ender Chest
            if (!player.hasPermission("midgard.admin.info.enderchest")) {
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("core.general.no-permission"));
                return;
            }
            player.openInventory(target.getEnderChest());
        } else if (slot == 50) { // Kick
            if (!player.hasPermission("midgard.admin.info.kick")) {
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("core.general.no-permission"));
                return;
            }
            player.closeInventory();
            target.kick(me.ray.midgard.core.text.MessageUtils.parse(me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("loader.player_info.kick_reason")));
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("loader.player_info.kick_success", "%player%", target.getName()));
        }
    }
}
