package dev.tjxjnoobie.ctf.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and reloads config.yml for the plugin.
 */
public final class CTFConfig {
    private final JavaPlugin plugin;
    private boolean configActive;

    public CTFConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the default config.yml from disk.
     */
    public void loadConfig() {
        if (plugin == null) {
            return;
        }
        plugin.saveDefaultConfig();
        configActive = true;
    }

    /**
     * Reloads config.yml from disk.
     */
    public void reloadConfig() {
        if (plugin == null) {
            return;
        }
        plugin.reloadConfig();
        configActive = true;
    }

    /**
     * Returns the Bukkit config handle.
     */
    public FileConfiguration getConfig() {
        return plugin == null ? null : plugin.getConfig();
    }

    /**
     * Returns true when config has been loaded.
     */
    public boolean isConfigActive() {
        return configActive;
    }
}

