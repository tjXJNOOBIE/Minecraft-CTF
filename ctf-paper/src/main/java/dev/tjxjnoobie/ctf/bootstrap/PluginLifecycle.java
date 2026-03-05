package dev.tjxjnoobie.ctf.bootstrap;

public interface PluginLifecycle {
    // Called by Bukkit on plugin enable.
    void onEnable();

    // Called by Bukkit on plugin disable.
    void onDisable();
}
