package dev.tjxjnoobie.ctf.team;

import java.util.Optional;
import org.bukkit.Location;

/**
 * Stores and loads the CTF lobby spawn location from config.
 */
public final class LobbySpawnStore {
    private final SpawnConfigHandler spawnConfigHandler;

    // == Lifecycle ==
    /**
     * Constructs a LobbySpawnStore instance.
     *
     * @param spawnConfigHandler Dependency responsible for spawn config handler.
     */
    public LobbySpawnStore(SpawnConfigHandler spawnConfigHandler) {
        // Capture config handler for lobby spawn access.
        this.spawnConfigHandler = spawnConfigHandler;
    }

    // == Getters ==
    public Optional<Location> getLobbySpawn() {
        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return Optional.empty();
        }
        // Return configured lobby spawn if available.
        return spawnConfigHandler.getLobbySpawn();
    }

    // == Setters ==
    public void setLobbySpawn(Location location) {
        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return;
        }
        // Persist lobby spawn location in config.
        spawnConfigHandler.setLobbySpawn(location);
    }
}

