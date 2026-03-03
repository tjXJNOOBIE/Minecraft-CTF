package dev.tjxjnoobie.ctf.team;

import java.util.Optional;
import org.bukkit.Location;

/**
 * Stores and loads per-team spawn locations from config.
 */
public final class TeamSpawnStore {

    private final SpawnConfigHandler spawnConfigHandler;

    public TeamSpawnStore(SpawnConfigHandler spawnConfigHandler) {
        this.spawnConfigHandler = spawnConfigHandler;
    }

    /**
     * Persists a team's spawn location to config.
     */
    public void setSpawn(String teamName, Location loc) {
        if (spawnConfigHandler == null) {
            return;
        }
        spawnConfigHandler.setTeamSpawn(teamName, loc);
    }

    /**
     * Loads a team's spawn location from config when present.
     */
    public Optional<Location> getSpawn(String teamName) {
        if (spawnConfigHandler == null) {
            return Optional.empty();
        }
        return spawnConfigHandler.getTeamSpawn(teamName);
    }

    /**
     * Reloads configuration values from disk.
     */
    public void reload() {
        if (spawnConfigHandler != null) {
            spawnConfigHandler.reloadSpawnConfig();
        }
    }
}

