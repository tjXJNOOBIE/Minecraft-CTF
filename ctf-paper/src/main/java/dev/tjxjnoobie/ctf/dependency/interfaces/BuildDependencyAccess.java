package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import org.bukkit.entity.Player;

/**
 * Dependency-access surface for build-bypass toggles.
 */
public interface BuildDependencyAccess {
    default BuildToggleUtil getBuildToggleUtil() { return DependencyLoaderAccess.findInstance(BuildToggleUtil.class); }

    default boolean toggleBuildBypass(Player player) {
        return getBuildToggleUtil().toggle(player);
    }

    default void clearBuildBypass(Player player) {
        getBuildToggleUtil().clearPlayer(player);
    }

    default void clearAllBuildBypass() {
        getBuildToggleUtil().clear();
    }

    default boolean canBuildBypass(Player player) {
        return getBuildToggleUtil().canBypass(player);
    }
}
