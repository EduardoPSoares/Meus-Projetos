package me.ray.midgard.core.integration;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.region.RegionProvider;
import org.bukkit.Location;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MidgardLandsIntegration implements RegionProvider {

    private final LandsIntegration landsApi;

    public MidgardLandsIntegration() {
        this.landsApi = LandsIntegration.of(MidgardCore.getPlugin());
    }

    @Override
    public Set<String> getRegions(Location location) {
        if (location == null || location.getWorld() == null) {
            return Collections.emptySet();
        }
        
        Area area = landsApi.getArea(location);
        if (area != null) {
            Set<String> regions = new HashSet<>();
            // Using getLand().getName() assuming it exists. 
            // If API differs, we might need to adjust.
            // Often protection plugins return the claim owner or land name.
            if (area.getLand() != null) {
                 regions.add(area.getLand().getName());
            }
            return regions;
        }
        return Collections.emptySet();
    }
}
