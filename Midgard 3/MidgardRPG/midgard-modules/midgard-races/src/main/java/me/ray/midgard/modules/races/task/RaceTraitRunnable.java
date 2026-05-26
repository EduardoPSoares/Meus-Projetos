package me.ray.midgard.modules.races.task;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.api.TraitTrigger;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.ConfiguredTrait;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RaceTraitRunnable implements Runnable {

    private final RacesModule module;
    private final Map<UUID, Boolean> lastDayState = new ConcurrentHashMap<>();

    public RaceTraitRunnable(RacesModule module) {
        this.module = module;
    }

    @Override
    public void run() {
        try {
        for (Player player : Bukkit.getOnlinePlayers()) {
            me.ray.midgard.core.utils.Task.sync(player, () -> {
                try {
                    if (!player.isOnline()) { return; }
                    MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
                    if (profile == null) { return; }

                    RaceData data = profile.getData(RaceData.class);
                    if (data == null || !data.hasRace()) { return; }

                    Race race = module.getRaceManager().getRace(data.getRaceId());
                    if (race == null) { return; }

                    // Detectar transição dia↔noite para reaplicar atributos condicionais
                    if (race.hasTimeAttributes()) {
                        boolean isDay = RacesModule.isDayTime(player.getWorld().getTime());
                        Boolean previous = lastDayState.put(player.getUniqueId(), isDay);
                        if (previous != null && previous != isDay) {
                            module.getAttributeListener().refreshAttributes(player);
                        }
                    }

                    if (race.getTraits() == null) { return; }

                    for (ConfiguredTrait ct : race.getTraits()) {
                    if (ct.getTrigger() == TraitTrigger.PASSIVE_TICK) {
                        if (data.getLevel() >= ct.getMinLevel()) {
                            
                            if (ct.isSelectable() && !data.hasMutation(ct.getId())) {
                                continue;
                            }
                            
                            if (!ct.getCondition().isMet(player)) {
                                continue;
                            }

                            ct.getTrait().execute(player, TraitTrigger.PASSIVE_TICK, new HashMap<>(), ct.getConfig());
                        }
                    }
                }
                } catch (Exception e) {
                    me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar passive tick para %s", player.getName(), e);
                }
            });
        }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro no RaceTraitRunnable", e);
        }
    }

    public void removePlayer(UUID uuid) {
        lastDayState.remove(uuid);
    }
}
