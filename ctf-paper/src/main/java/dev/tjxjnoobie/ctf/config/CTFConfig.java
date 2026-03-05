package dev.tjxjnoobie.ctf.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and reloads config.yml for the plugin.
 */
public final class CTFConfig {
    private final JavaPlugin plugin;

    // == Runtime state ==
    private boolean configActive;

    // == Lifecycle ==
    /**
     * Constructs a CTFConfig instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public CTFConfig(JavaPlugin plugin) {
        // Capture plugin reference.
        this.plugin = plugin;
    }

    /**
     * Returns data for loadConfig.
     */
    public void loadConfig() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }
        // Ensure default config is present.
        plugin.saveDefaultConfig();
        configActive = true;
    }

    /**
     * Executes reloadConfig.
     */
    public void reloadConfig() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }
        // Reload config from disk.
        plugin.reloadConfig();
        configActive = true;
    }

    // == Getters ==
    public FileConfiguration getConfig() {
        return plugin == null ? null : plugin.getConfig(); // Return the live config handle.
    }

    // == Predicates ==
    public boolean isConfigActive() {
        // Return whether config has been loaded.
        return configActive;
    }
}

