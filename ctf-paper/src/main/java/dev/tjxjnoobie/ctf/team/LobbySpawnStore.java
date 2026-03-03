package dev.tjxjnoobie.ctf.team;

import java.util.Optional;
import org.bukkit.Location;

/**
 * Stores and loads the CTF lobby spawn location from config.
 */
public final class LobbySpawnStore {
    private final SpawnConfigHandler spawnConfigHandler;

    public LobbySpawnStore(SpawnConfigHandler spawnConfigHandler) {
        this.spawnConfigHandler = spawnConfigHandler;
    }

    /**
     * Persists lobby spawn location.
     */
    public void setLobbySpawn(Location location) {
        if (spawnConfigHandler == null) {
            return;
        }
        spawnConfigHandler.setLobbySpawn(location);
    }

    /**
     * Loads lobby spawn location when present and valid.
     */
    public Optional<Location> getLobbySpawn() {
        if (spawnConfigHandler == null) {
            return Optional.empty();
        }
        return spawnConfigHandler.getLobbySpawn();
    }
}

