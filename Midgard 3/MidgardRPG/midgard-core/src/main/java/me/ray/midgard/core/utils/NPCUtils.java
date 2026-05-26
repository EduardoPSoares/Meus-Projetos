package me.ray.midgard.core.utils;

import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import java.util.UUID;

public class NPCUtils {

    public static Npc createNPC(String name, EntityType type, Location location) {
        // UUID.randomUUID() is used as the creator UUID
        NpcData data = new NpcData(name, UUID.randomUUID(), location);
        data.setType(type);
        
        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
        FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
        npc.create();
        npc.spawnForAll();
        
        return npc;
    }

    public static void removeNPC(String name) {
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(name);
        if (npc != null) {
            FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
            npc.removeForAll();
        }
    }
    
    public static Npc getNPC(String name) {
        return FancyNpcsPlugin.get().getNpcManager().getNpc(name);
    }
}
