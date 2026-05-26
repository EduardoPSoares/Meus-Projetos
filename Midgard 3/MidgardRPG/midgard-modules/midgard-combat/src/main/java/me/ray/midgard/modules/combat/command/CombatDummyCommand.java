package me.ray.midgard.modules.combat.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CombatDummyCommand extends MidgardCommand {

    private static final List<String> DAMAGE_TYPES = Arrays.asList(
            "ATTACK_DAMAGE", "WEAPON_DAMAGE", "PHYSICAL_DAMAGE", "MAGIC_DAMAGE",
            "PROJECTILE_DAMAGE", "SKILL_DAMAGE", "UNDEAD_DAMAGE",
            "FIRE_DAMAGE", "ICE_DAMAGE", "LIGHT_DAMAGE", "DARKNESS_DAMAGE", "DIVINE_DAMAGE",
            "EARTH_DAMAGE", "THUNDER_DAMAGE", "WATER_DAMAGE", "AIR_DAMAGE"
    );

    public CombatDummyCommand() {
        super("dummy", "midgard.admin.dummy", true);
    }

    @Override
    public String getDescription() {
        me.ray.midgard.modules.combat.CombatModule module = me.ray.midgard.modules.combat.CombatModule.getInstance();
        return module != null ? module.getMessage("command.dummy_description") : "Cria e gerencia bonecos de treino";
    }

    @Override
    public String getUsage() {
        me.ray.midgard.modules.combat.CombatModule module = me.ray.midgard.modules.combat.CombatModule.getInstance();
        return module != null ? module.getMessage("command.dummy_usage") : "/rpg admin dummy [spawn|clear|indicator]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("core.error.player_only"));
            return;
        }

        if (args.length < 1) {
            MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.usage"));
            return;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            int count = 0;
            for (Entity entity : ((Player)sender).getWorld().getEntities()) {
                if (entity.getScoreboardTags().contains("midgard_dummy")) {
                    entity.remove();
                    count++;
                }
            }
            MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.removed").replace("%count%", String.valueOf(count)));
            return;
        }

        if (args[0].equalsIgnoreCase("indicator")) {
            if (me.ray.midgard.modules.combat.CombatManager.getInstance() != null) {
                // me.ray.midgard.modules.combat.DamageIndicatorManager dim = me.ray.midgard.modules.combat.CombatManager.getInstance().getIndicatorManager();
                // if (dim != null) {
                //    dim.spawnCustomIndicator(((Player)sender), "TEST 123", "<red>");
                //    MessageUtils.send(sender, "<green>Indicador de teste enviado!");
                // } else {
                //    MessageUtils.send(sender, "<red>IndicatorManager is null!");
                // }
                
                // TESTE NMS
                try {
                    me.ray.midgard.nms.api.NMSHandler nms = MidgardCore.getNMSHandler();
                    if (nms != null) {
                        nms.spawnDamageIndicatorPacket(me.ray.midgard.modules.combat.CombatModule.getInstance().getPlugin(), (Player)sender, ((Player)sender).getLocation().add(0, 2, 0), "<red>TEST NMS PACKET", 60, 0x50000000, true, true);
                        MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.nms_test"));
                    } else {
                        MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.nms_null"));
                    }
                } catch (Exception e) {
                    me.ray.midgard.core.debug.MidgardLogger.error("Erro ao testar pacote NMS do dummy", e);
                    MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.nms_error").replace("%error%", e.getMessage()));
                }
            }
            return;
        }

        if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.usage"));
                return;
            }

            String type = args[1].toUpperCase();
            if (!type.endsWith("_DAMAGE")) {
                type += "_DAMAGE";
            }

            Player player = (Player) sender;
            Location loc = player.getLocation();

            try {
                String shortType = type.replace("_DAMAGE", "");
                String nameTemplate = me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.display_name");
                String displayName = nameTemplate.replace("%type%", shortType);

                if (!DAMAGE_TYPES.contains(type)) {
                     MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.spawn.warn_unknown").replace("%type%", type));
                }

                Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
                zombie.setAI(false);
                zombie.customName(MessageUtils.parse(displayName));
                zombie.setCustomNameVisible(true);
                zombie.setCanPickupItems(false);
                zombie.setAdult();

                // Infinite Health & Stats
                double maxHealth = 1024.0; // Max allowed by default Spigot config
                org.bukkit.attribute.AttributeInstance healthAttr = zombie.getAttribute(Attribute.MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.setBaseValue(maxHealth);
                    zombie.setHealth(maxHealth);
                }

                org.bukkit.attribute.AttributeInstance kbAttr = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                if (kbAttr != null) {
                    kbAttr.setBaseValue(1.0);
                }
                
                // Add tags for detection
                zombie.addScoreboardTag("midgard_dummy");
                zombie.addScoreboardTag("midgard.damage." + type);
                // Add element tag for victim element detection in ElementalDamageCalculator
                String elementName = type.replace("_DAMAGE", "").toLowerCase();
                zombie.addScoreboardTag("element_" + elementName);
                
                MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.spawn.success").replace("%type%", type));

            } catch (Exception e) {
                MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.spawn.error").replace("%error%", e.getMessage()));
                me.ray.midgard.core.debug.MidgardLogger.error("Erro ao criar dummy", e);
            }
            return;
        }

        MessageUtils.send(sender, me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("dummy.usage"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "clear", "indicator"), new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            List<String> types = new ArrayList<>();
            for (String type : DAMAGE_TYPES) {
                types.add(type.replace("_DAMAGE", "").toLowerCase());
            }
            return StringUtil.copyPartialMatches(args[1], types, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
