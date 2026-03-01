package dev.tjxjnoobie.ctf;

import dev.tjxjnoobie.ctf.bootstrap.PluginBootstrap;
import dev.tjxjnoobie.ctf.bootstrap.PluginLifecycle;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements PluginLifecycle {
    private PluginLifecycle lifecycle;



    @Override
    public void onEnable() {
        lifecycle = new PluginBootstrap(this); // Bootstrap the plugin lifecycle and dependencies.
        lifecycle.onEnable();
    }


    @Override
    public void onDisable() {
        // Shutdown lifecycle hooks and clear singleton state.
        if (lifecycle != null) {
            lifecycle.onDisable();
            lifecycle = null;
        }
    }
}