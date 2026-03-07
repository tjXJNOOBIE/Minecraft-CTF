package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.team.SpawnConfigHandler;

/**
 * Dependency-access surface for team spawn and return-point config storage.
 */
public interface SpawnConfigDependencyAccess {
    default SpawnConfigHandler getSpawnConfigHandler() { return DependencyLoaderAccess.findInstance(SpawnConfigHandler.class); }

    default void loadSpawnConfig() {
        getSpawnConfigHandler().loadSpawnConfig();
    }

    default void reloadSpawnConfig() {
        getSpawnConfigHandler().reloadSpawnConfig();
    }
}
