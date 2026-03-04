package dev.tjxjnoobie.ctf.team;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lifecycle wrapper for ctf-spawns.yml.
 */
public final class SpawnConfig {

    // == Constants ==
    private static final String LOG_PREFIX = "[SpawnConfig] ";
    private static final String FILE_NAME = "ctf-spawns.yml";

    // == Runtime state ==
    private FileConfiguration config;
    private File configFile;
    private boolean active;
    private boolean createdOnLoad;
    private final JavaPlugin plugin;

    // == Lifecycle ==
    /**
     * Constructs a SpawnConfig instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public SpawnConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns data for load.
     */
    public void load() {
        File dataFolder = plugin.getDataFolder();
        // Guard: short-circuit when dataFolder == null.
        if (dataFolder == null) {
            return;
        }

        boolean existsResult = dataFolder.exists();
        if (!existsResult) {
            dataFolder.mkdirs();
        }

        configFile = new File(dataFolder, FILE_NAME);
        createdOnLoad = !configFile.exists();
        if (createdOnLoad) {
            plugin.saveResource(FILE_NAME, false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        active = true;
    }

    /**
     * Executes reload.
     */
    public void reload() {
        if (configFile == null) {
            load();
            return;
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        createdOnLoad = false;
        active = true;
    }

    // == Getters ==
    public FileConfiguration getConfig() {
        return config;
    }

    public File getConfigFile() {
        return configFile;
    }

    // == Utilities ==
    /**
     * Updates state for save.
     */
    public void save() {
        // Guard: short-circuit when configFile == null || config == null.
        if (configFile == null || config == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning(LOG_PREFIX + "Failed saving " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    // == Predicates ==
    public boolean isActive() {
        return active;
    }

    public boolean wasCreatedOnLoad() {
        return createdOnLoad;
    }
}
