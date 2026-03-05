package dev.tjxjnoobie.ctf.team;

import java.util.Optional;
import org.bukkit.Location;

/**
 * Stores and loads per-team spawn locations from config.
 */
public final class TeamSpawnStore {
    private final SpawnConfigHandler spawnConfigHandler;

    // == Lifecycle ==
    /**
     * Constructs a TeamSpawnStore instance.
     *
     * @param spawnConfigHandler Dependency responsible for spawn config handler.
     */
    public TeamSpawnStore(SpawnConfigHandler spawnConfigHandler) {
        // Capture spawn config handler.
        this.spawnConfigHandler = spawnConfigHandler;
    }

    /**
     * Executes reload.
     */
    public void reload() {
        if (spawnConfigHandler != null) {
            // Reload spawn config from disk.
            spawnConfigHandler.reloadSpawnConfig();
        }
    }

    // == Getters ==
    public Optional<Location> getSpawn(String teamName) {
        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return Optional.empty();
        }
        // Delegate reading to config handler.
        return spawnConfigHandler.getTeamSpawn(teamName);
    }

    // == Setters ==
    public void setSpawn(String teamName, Location loc) {
        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return;
        }
        // Delegate persistence to config handler.
        spawnConfigHandler.setTeamSpawn(teamName, loc);
    }
}

