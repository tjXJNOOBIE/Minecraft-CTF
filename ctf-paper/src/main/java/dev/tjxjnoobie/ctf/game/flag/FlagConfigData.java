package dev.tjxjnoobie.ctf.game.flag;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * DTO for flag configuration data.
 */
public final class FlagConfigData {
    private final Location baseLocation;
    private final Location indicatorLocation;
    private final Material material;

    public FlagConfigData(Location baseLocation, Location indicatorLocation, Material material) {
        this.baseLocation = baseLocation;
        this.indicatorLocation = indicatorLocation;
        this.material = material;
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public Location getIndicatorLocation() {
        return indicatorLocation;
    }

    public Material getMaterial() {
        return material;
    }
}
