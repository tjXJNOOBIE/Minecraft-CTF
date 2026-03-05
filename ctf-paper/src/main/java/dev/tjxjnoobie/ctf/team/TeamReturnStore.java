package dev.tjxjnoobie.ctf.team;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

/**
 * Stores and loads per-team return capture points from config.
 */
public final class TeamReturnStore {
    private final SpawnConfigHandler spawnConfigHandler;

    // == Lifecycle ==
    /**
     * Constructs a TeamReturnStore instance.
     *
     * @param spawnConfigHandler Dependency responsible for spawn config handler.
     */
    public TeamReturnStore(SpawnConfigHandler spawnConfigHandler) {
        // Capture spawn config handler.
        this.spawnConfigHandler = spawnConfigHandler;
    }

    // == Getters ==
    public List<Location> getReturnPoints(String teamKey) {
        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return new ArrayList<>();
        }
        // Delegate reading return points to config handler.
        return spawnConfigHandler.getReturnPoints(teamKey);
    }

    // == Utilities ==
    /**
     * Returns the result of addReturnPoint.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @param location World location used by this operation.
     * @return Result produced by this method.
     */
    public boolean addReturnPoint(String teamKey, Location location) {
        boolean conditionResult1 = teamKey == null || location == null || location.getWorld() == null;
        // Guard: short-circuit when teamKey == null || location == null || location.getWorld() == null.
        if (conditionResult1) {
            return false;
        }

        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return false;
        }
        // Delegate persistence to config handler.
        return spawnConfigHandler.addReturnPoint(teamKey, location);
    }

    /**
     * Removes the nearest return point to the provided location.
     *
     * @param reference Reference location used to choose the nearest return point.
     * @return Team key for the removed point, or {@code null} when none exist.
     */
    public String removeNearestReturnPoint(Location reference) {
        if (reference == null || reference.getWorld() == null || spawnConfigHandler == null) {
            return null;
        }
        return spawnConfigHandler.removeNearestReturnPoint(reference);
    }
}

