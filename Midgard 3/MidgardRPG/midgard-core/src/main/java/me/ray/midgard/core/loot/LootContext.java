package me.ray.midgard.core.loot;

import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LootContext {

    private final MidgardProfile looter;
    private final Location location;
    private final double luck;

    public LootContext(MidgardProfile looter, Location location, double luck) {
        this.looter = looter;
        this.location = location;
        this.luck = luck;
    }

    public Optional<MidgardProfile> getLooter() {
        return Optional.ofNullable(looter);
    }
    
    public Optional<Player> getPlayer() {
        return looter != null ? Optional.ofNullable(looter.getPlayer()) : Optional.empty();
    }

    public Location getLocation() {
        return location;
    }

    public double getLuck() {
        return luck;
    }
    
    public static LootContext of(MidgardProfile profile, Location loc) {
        return new LootContext(profile, loc, 0);
    }
    
    public static LootContext of(Location loc) {
        return new LootContext(null, loc, 0);
    }
}
