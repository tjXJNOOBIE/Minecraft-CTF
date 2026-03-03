package dev.tjxjnoobie.ctf.game.flag;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages flag configuration stored in flag-locations.yml.
 */
public final class FlagConfigHandler {
    private static final String LOG_PREFIX = "[CTF] [FlagConfig] ";
    private static final String FILE_NAME = "flag-locations.yml";

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    private boolean active;

    public FlagConfigHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadFlagConfig();
    }

    /**
     * Loads flag configuration from disk, creating file if missing.
     */
    public void loadFlagConfig() {
        if (plugin == null) {
            return;
        }

        File dataFolder = plugin.getDataFolder();
        if (dataFolder == null) {
            return;
        }

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        configFile = new File(dataFolder, FILE_NAME);
        boolean created = !configFile.exists();
        if (created) {
            plugin.saveResource(FILE_NAME, false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        if (created) {
            migrateFromLegacyConfig();
        }
        active = true;
    }

    /**
     * Reloads flag configuration from disk.
     */
    public void reloadFlagConfig() {
        if (configFile == null) {
            loadFlagConfig();
            return;
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        active = true;
    }

    /**
     * Returns true when flag config is loaded.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the stored flag data for a team.
     */
    public FlagConfigData getFlagData(String teamKey) {
        if (teamKey == null || config == null) {
            return null;
        }

        ConfigurationSection section = config.getConfigurationSection("flags." + teamKey);
        if (section == null) {
            return null;
        }

        Location base = readLocation(section.getConfigurationSection("base"));
        Location indicator = readLocation(section.getConfigurationSection("indicator"));
        Material material = readMaterial(section.getString("material", null));
        return new FlagConfigData(base, indicator, material);
    }

    /**
     * Sets flag data for a team and persists changes.
     */
    public void setFlagData(String teamKey, FlagConfigData data) {
        if (teamKey == null || data == null || config == null) {
            return;
        }

        String path = "flags." + teamKey;
        Location base = data.getBaseLocation();
        Location indicator = data.getIndicatorLocation();
        Material material = data.getMaterial();

        if (base != null) {
            writeLocation(path + ".base", base);
        }
        if (indicator != null) {
            writeLocation(path + ".indicator", indicator);
        }
        if (material != null) {
            config.set(path + ".material", material.name());
        }

        save();
    }

    public Optional<Location> getBase(String teamKey) {
        FlagConfigData data = getFlagData(teamKey);
        return data == null || data.getBaseLocation() == null ? Optional.empty() : Optional.of(data.getBaseLocation());
    }

    public Optional<Location> getIndicator(String teamKey) {
        FlagConfigData data = getFlagData(teamKey);
        return data == null || data.getIndicatorLocation() == null ? Optional.empty() : Optional.of(data.getIndicatorLocation());
    }

    public Material getMaterial(String teamKey, Material fallback) {
        FlagConfigData data = getFlagData(teamKey);
        if (data == null || data.getMaterial() == null) {
            return fallback;
        }
        return data.getMaterial();
    }

    private void migrateFromLegacyConfig() {
        if (plugin == null || plugin.getConfig() == null) {
            return;
        }

        ConfigurationSection legacyFlags = plugin.getConfig().getConfigurationSection("flags");
        if (legacyFlags == null || legacyFlags.getKeys(false).isEmpty()) {
            return;
        }

        FileConfiguration legacy = plugin.getConfig();
        for (String teamKey : legacyFlags.getKeys(false)) {
            ConfigurationSection team = legacyFlags.getConfigurationSection(teamKey);
            if (team == null) {
                continue;
            }

            String worldName = team.getString("world", null);
            if (worldName == null) {
                continue;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            Location base = new Location(world,
                team.getDouble("x"),
                team.getDouble("y"),
                team.getDouble("z"));
            String rawMaterial = team.getString("material", null);
            Material material = readMaterial(rawMaterial);

            Location indicator = base.toBlockLocation().add(0.5, 2.25, 0.5);
            setFlagData(teamKey, new FlagConfigData(base, indicator, material));
        }

        Bukkit.getLogger().info(LOG_PREFIX + "Migrated legacy flag data to " + FILE_NAME);
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world", null);
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"));
    }

    private void writeLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
    }

    private Material readMaterial(String raw) {
        if (raw == null) {
            return null;
        }
        Material material = Material.matchMaterial(raw);
        return material == null ? null : material;
    }

    private void save() {
        if (configFile == null || config == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning(LOG_PREFIX + "Failed saving " + FILE_NAME + ": " + ex.getMessage());
        }
    }
}
