package dev.tjxjnoobie.ctf.dependency.interfaces;

/**
 * Backward-compatible aggregate for config-adjacent dependency domains.
 */
public interface ConfigDependencyAccess extends MessageConfigDependencyAccess,
        PluginConfigDependencyAccess,
        SpawnConfigDependencyAccess {
}
