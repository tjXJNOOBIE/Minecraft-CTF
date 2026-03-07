package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearInventoryUtils;

/**
 * DI access layer for homing-spear inventory visuals and placeholder rules.
 */
public interface HomingSpearInventoryUtilAccess {
    default HomingSpearInventoryUtils getHomingSpearInventoryUtils() {
        return DependencyLoaderAccess.findInstance(HomingSpearInventoryUtils.class);
    }
}
