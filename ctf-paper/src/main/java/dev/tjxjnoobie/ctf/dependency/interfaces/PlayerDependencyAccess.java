package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

/**
 * Backward-compatible aggregate for player-adjacent dependency domains.
 */
public interface PlayerDependencyAccess extends KitDependencyAccess,
        TeamDependencyAccess,
        DebugDependencyAccess,
        BuildDependencyAccess,
        ArenaPlayerDependencyAccess,
        MatchSessionDependencyAccess,
        PlayerRuntimeDependencyAccess {
}
