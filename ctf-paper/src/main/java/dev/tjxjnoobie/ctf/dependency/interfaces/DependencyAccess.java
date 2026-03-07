package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

/**
 * Aggregate access layer for the main gameplay dependency domains.
 */
public interface DependencyAccess extends CombatDependencyAccess,
        ConfigDependencyAccess,
        FlagDependencyAccess,
        LifecycleDependencyAccess,
        PlayerDependencyAccess,
        TaskDependencyAccess,
        UiDependencyAccess {
}
