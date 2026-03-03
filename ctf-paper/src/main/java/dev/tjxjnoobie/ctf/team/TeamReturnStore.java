package dev.tjxjnoobie.ctf.team;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

/**
 * Stores and loads per-team return capture points from config.
 */
public final class TeamReturnStore {
    private final SpawnConfigHandler spawnConfigHandler;

    public TeamReturnStore(SpawnConfigHandler spawnConfigHandler) {
        this.spawnConfigHandler = spawnConfigHandler;
    }

    /**
     * Appends a new return point for a team.
     */
    public boolean addReturnPoint(String teamKey, Location location) {
        if (teamKey == null || location == null || location.getWorld() == null) {
            return false;
        }

        if (spawnConfigHandler == null) {
            return false;
        }
        return spawnConfigHandler.addReturnPoint(teamKey, location);
    }

    /**
     * Returns all configured return points for a team.
     */
    public List<Location> getReturnPoints(String teamKey) {
        if (spawnConfigHandler == null) {
            return new ArrayList<>();
        }
        return spawnConfigHandler.getReturnPoints(teamKey);
    }
}

