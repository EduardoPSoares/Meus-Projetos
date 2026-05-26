package me.ray.midgard.core.script.actions;

import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.script.Action;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundAction implements Action {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    @SuppressWarnings({"deprecation", "removal"})
    public SoundAction(String soundData) {
        String[] parts = soundData.split(" ");
        String soundName = parts[0].toLowerCase();
        
        // Tenta buscar pelo registry primeiro (suporta custom/novos sons)
        Sound foundSound = null;
        try {
            foundSound = Bukkit.getRegistry(Sound.class).get(NamespacedKey.minecraft(soundName));
        } catch (Exception e) {
            // Registry não disponível, tentando fallback via Enum
        }

        // Fallback para Enum se não encontrar (ou se o registry falhar)
        if (foundSound == null) {
            try {
                foundSound = Sound.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                 // Deixa null, será tratado ou causará erro depois
            }
        }
        
        if (foundSound == null) {
             throw new IllegalArgumentException("Sound not found: " + parts[0]);
        }
        
        this.sound = foundSound;
        this.volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        this.pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
    }

    @Override
    public void execute(Player player) {
        MidgardLogger.debug(DebugCategory.SCRIPT, 
            "Executando som §e%s§f para §e%s§f (v:§b%.1f§f, p:§b%.1f§f)", 
            sound.toString(), player.getName(), volume, pitch);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
