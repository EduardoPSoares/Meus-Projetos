package me.ray.midgard.core.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.region.RegionProvider;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class WorldGuardIntegration implements RegionProvider {

    public WorldGuardIntegration() {
    }

    @Override
    public Set<String> getRegions(Location location) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            return set.getRegions().stream()
                    .map(ProtectedRegion::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            MidgardLogger.error("Erro ao consultar regiões do WorldGuard", e);
            return new HashSet<>();
        }
    }
}
