package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

/**
 * Aggregate access layer for DI-backed utility collaborators.
 */
public interface DependencyUtilAccess extends HomingSpearInventoryUtilAccess, InventoryUtilAccess, MessageUtilAccess {
}
