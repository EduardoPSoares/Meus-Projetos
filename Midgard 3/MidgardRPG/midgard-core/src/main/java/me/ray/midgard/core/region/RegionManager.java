package me.ray.midgard.core.region;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gerencia a integração com sistemas de regiões (como WorldGuard, Lands).
 * Permite verificar em quais regiões um jogador está.
 */
public class RegionManager {

    private static volatile RegionManager instance;
    private final List<RegionProvider> providers = new ArrayList<>();

    /**
     * Obtém a instância única do RegionManager.
     *
     * @return Instância do RegionManager.
     */
    public static RegionManager getInstance() {
        if (instance == null) {
            instance = new RegionManager();
        }
        return instance;
    }

    private RegionManager() {
    }

    /**
     * Registra um provedor de regiões.
     *
     * @param provider Provedor de regiões.
     */
    public void registerProvider(RegionProvider provider) {
        this.providers.add(provider);
    }
    
    /**
     * Define o provedor de regiões (Substitui os existentes).
     *
     * @param provider Provedor de regiões.
     */
    public void setProvider(RegionProvider provider) {
        this.providers.clear();
        this.providers.add(provider);
    }

    /**
     * Obtém as regiões em uma determinada localização.
     *
     * @param location Localização a ser verificada.
     * @return Conjunto de IDs das regiões.
     */
    public Set<String> getRegions(Location location) {
        if (providers.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> allRegions = new HashSet<>();
        for (RegionProvider provider : providers) {
            allRegions.addAll(provider.getRegions(location));
        }
        return allRegions;
    }

    /**
     * Verifica se uma localização está dentro de uma região específica.
     *
     * @param location Localização a ser verificada.
     * @param regionId ID da região.
     * @return true se estiver na região, false caso contrário.
     */
    public boolean isInRegion(Location location, String regionId) {
        for (RegionProvider provider : providers) {
            if (provider.isInRegion(location, regionId)) {
                return true;
            }
        }
        return false;
    }
}
