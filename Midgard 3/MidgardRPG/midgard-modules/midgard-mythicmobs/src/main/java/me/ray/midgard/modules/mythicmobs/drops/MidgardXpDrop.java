package me.ray.midgard.modules.mythicmobs.drops;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.combat.CombatModule;
import org.bukkit.entity.Player;

public class MidgardXpDrop implements IItemDrop {

    private final double xpAmount;

    public MidgardXpDrop(MythicLineConfig config) {
        this.xpAmount = config.getDouble(new String[]{"amount", "a", "xp"}, 0);
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata metadata, double amount) {
        // amount here is the number of drops rolled by MythicMobs (?)
        // usually 1.
        
        if (metadata.getCause().isPresent()) {
             org.bukkit.entity.Entity entity = BukkitAdapter.adapt(metadata.getCause().get());
             if (entity instanceof Player) {
                 Player player = (Player) entity;
                 
                 if (CombatModule.getInstance() != null && CombatModule.getInstance().getLevelManager() != null) {
                     // Add XP
                     CombatModule.getInstance().getLevelManager().addExperience(player, xpAmount);
                     
                     // Send Message (imitating LevelListener behavior)
                     String xpMsg = CombatModule.getInstance().getMessage("progression.xp_gained");
                     if (xpMsg != null) {
                         String mobName = CombatModule.getInstance().getMessage("mob.unknown_name");
                     if (mobName == null || mobName.isEmpty()) {
                         mobName = "Unknown";
                     }
                         if (metadata.getDropper().isPresent()) {
                             mobName = metadata.getDropper().get().getName();
                         }
                         
                         xpMsg = xpMsg.replace("%xp%", String.format("%.1f", xpAmount))
                                      .replace("%mob%", mobName)
                                      .replace("%level%", "1"); // We don't have level here easily without Mob instance
                                      
                         MessageUtils.send(player, xpMsg);
                     }
                 }
             }
        }
        return null; // No physical item dropped
    }
}
