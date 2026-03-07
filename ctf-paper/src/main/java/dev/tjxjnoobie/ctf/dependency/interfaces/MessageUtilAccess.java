package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;

/**
 * DI access layer for the shared Bukkit message utility.
 */
public interface MessageUtilAccess {
    default BukkitMessageUtil getBukkitMessageUtil() { return DependencyLoaderAccess.findInstance(BukkitMessageUtil.class); }
}
