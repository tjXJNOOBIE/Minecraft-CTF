package dev.tjxjnoobie.ctf.game.flag;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * DTO for flag configuration data.
 */
public final class FlagConfigData {

    // == Stored configuration values ==
    private final Location baseLocation;
    private final Location indicatorLocation;
    private final Material material;

    // == Lifecycle ==
    /**
     * Constructs a FlagConfigData instance.
     *
     * @param baseLocation Location used for flag/base placement or fallback logic.
     * @param indicatorLocation Location used for flag/base placement or fallback logic.
     * @param material Bukkit type used by this operation.
     */
    public FlagConfigData(Location baseLocation, Location indicatorLocation, Material material) {
        // Capture the immutable flag configuration.
        this.baseLocation = baseLocation;
        this.indicatorLocation = indicatorLocation;
        this.material = material;
    }

    // == Getters ==
    public Location getBaseLocation() {
        // Return the configured base location.
        return baseLocation;
    }

    public Location getIndicatorLocation() {
        // Return the configured indicator location.
        return indicatorLocation;
    }

    public Material getMaterial() {
        // Return the configured flag material.
        return material;
    }
}
