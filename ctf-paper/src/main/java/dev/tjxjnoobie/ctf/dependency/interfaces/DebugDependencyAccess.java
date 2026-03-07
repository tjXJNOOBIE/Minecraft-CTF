package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;

/**
 * Dependency-access surface for debug feed output.
 */
public interface DebugDependencyAccess {
    default DebugFeed getDebugFeed() { return DependencyLoaderAccess.findInstance(DebugFeed.class); }

    default void debugFeedSend(String message) {
        getDebugFeed().send(message);
    }

    default void debugFeedClear() {
        getDebugFeed().clear();
    }
}
