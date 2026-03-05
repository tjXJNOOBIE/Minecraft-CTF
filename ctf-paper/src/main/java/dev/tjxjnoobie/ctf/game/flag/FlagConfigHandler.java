package dev.tjxjnoobie.ctf.game.flag;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
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

    // == Constants ==
    private static final String LOG_PREFIX = "[CTF] [FlagConfig] ";
    private static final String FILE_NAME = "flag-locations.yml";
    private final JavaPlugin plugin;

    // == Runtime state ==
    private FileConfiguration config;
    private File configFile;
    private boolean active;

    // == Lifecycle ==
    /**
     * Constructs a FlagConfigHandler instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public FlagConfigHandler(JavaPlugin plugin) {
        // Store plugin; loading is invoked explicitly during bootstrap.
        this.plugin = plugin;
    }

    /**
     * Returns data for loadFlagConfig.
     */
    public void loadFlagConfig() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }

        File dataFolder = plugin.getDataFolder(); // Ensure data folder exists before config access.
        // Guard: short-circuit when dataFolder == null.
        if (dataFolder == null) {
            return;
        }

        boolean existsResult = dataFolder.exists();
        if (!existsResult) {
            dataFolder.mkdirs();
        }

        configFile = new File(dataFolder, FILE_NAME);
        boolean created = !configFile.exists();
        if (created) {
            plugin.saveResource(FILE_NAME, false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        mergeDefaultsFromResource();
        if (created) {
            migrateFromLegacyConfig();
        }
        active = true;
    }

    /**
     * Executes reloadFlagConfig.
     */
    public void reloadFlagConfig() {
        if (configFile == null) {
            loadFlagConfig();
            return;
        }
        config = YamlConfiguration.loadConfiguration(configFile); // Reload the config from disk.
        mergeDefaultsFromResource();
        active = true;
    }

    // == Getters ==
    public FlagConfigData getFlagData(String teamKey) {
        // Guard: short-circuit when teamKey == null || config == null.
        if (teamKey == null || config == null) {
            return null;
        }

        ConfigurationSection section = config.getConfigurationSection(CTFKeys.flagConfigFlagsPathPrefix() + teamKey); // Read the team config section into a DTO.
        // Guard: short-circuit when section == null.
        if (section == null) {
            return null;
        }

        Location base = readLocation(section.getConfigurationSection("base"));
        Location indicator = readLocation(section.getConfigurationSection("indicator"));
        Material material = readMaterial(section.getString("material", null));
        return new FlagConfigData(base, indicator, material);
    }

    public Optional<Location> getBase(String teamKey) {
        FlagConfigData flagConfigData = getFlagData(teamKey);
        return flagConfigData == null || flagConfigData.getBaseLocation() == null
            ? Optional.empty()
            : Optional.of(flagConfigData.getBaseLocation());
    }

    public Optional<Location> getIndicator(String teamKey) {
        FlagConfigData flagConfigData = getFlagData(teamKey);
        return flagConfigData == null || flagConfigData.getIndicatorLocation() == null
            ? Optional.empty()
            : Optional.of(flagConfigData.getIndicatorLocation());
    }

    public Material getMaterial(String teamKey, Material fallback) {
        FlagConfigData flagConfigData = getFlagData(teamKey);
        boolean conditionResult1 = flagConfigData == null || flagConfigData.getMaterial() == null;
        // Guard: short-circuit when flagConfigData == null || flagConfigData.getMaterial() == null.
        if (conditionResult1) {
            return fallback;
        }
        return flagConfigData.getMaterial();
    }

    // == Setters ==
    public void setFlagData(String teamKey, FlagConfigData flagConfigData) {
        // Guard: short-circuit when teamKey == null || flagConfigData == null || config == null.
        if (teamKey == null || flagConfigData == null || config == null) {
            return;
        }

        String path = CTFKeys.flagConfigFlagsPathPrefix() + teamKey; // Persist the provided flag data for the team.
        Location base = flagConfigData.getBaseLocation();
        Location indicator = flagConfigData.getIndicatorLocation();
        Material material = flagConfigData.getMaterial();

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

    // == Utilities ==
    private void migrateFromLegacyConfig() {
        boolean conditionResult2 = plugin == null || plugin.getConfig() == null;
        // Guard: short-circuit when plugin == null || plugin.getConfig() == null.
        if (conditionResult2) {
            return;
        }

        ConfigurationSection legacyFlags = plugin.getConfig().getConfigurationSection("flags"); // Read legacy config and store it into the new file format.
        boolean conditionResult3 = legacyFlags == null || legacyFlags.getKeys(false).isEmpty();
        // Guard: short-circuit when legacyFlags == null || legacyFlags.getKeys(false).isEmpty().
        if (conditionResult3) {
            return;
        }

        FileConfiguration legacy = plugin.getConfig();
        for (String teamKey : legacyFlags.getKeys(false)) {
            ConfigurationSection team = legacyFlags.getConfigurationSection(teamKey);
            // Guard: short-circuit when team == null.
            if (team == null) {
                continue;
            }

            String worldName = team.getString("world", null);
            // Guard: short-circuit when worldName == null.
            if (worldName == null) {
                continue;
            }

            World world = Bukkit.getWorld(worldName);
            // Guard: short-circuit when world == null.
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

    private void mergeDefaultsFromResource() {
        if (plugin == null || config == null) {
            return;
        }

        try (InputStream stream = plugin.getResource(FILE_NAME)) {
            if (stream == null) {
                return;
            }

            FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            save();
        } catch (IOException ignored) {
            // Keep config loading resilient when bundled defaults are unavailable.
        }
    }

    private Location readLocation(ConfigurationSection section) {
        // Guard: short-circuit when section == null.
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world", null);
        // Guard: short-circuit when worldName == null.
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName); // Resolve the world for the stored location.
        // Guard: short-circuit when world == null.
        if (world == null) {
            return null;
        }

        return new Location(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"));
    }

    private void writeLocation(String path, Location location) {
        boolean conditionResult4 = location == null || location.getWorld() == null;
        // Guard: short-circuit when location == null || location.getWorld() == null.
        if (conditionResult4) {
            return;
        }

        // Persist a world + xyz location.
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
    }

    private Material readMaterial(String raw) {
        // Guard: short-circuit when raw == null.
        if (raw == null) {
            return null;
        }
        Material material = Material.matchMaterial(raw); // Resolve a material name to Bukkit Material.
        return material == null ? null : material;
    }

    private void save() {
        // Guard: short-circuit when configFile == null || config == null.
        if (configFile == null || config == null) {
            return;
        }
        try {
            // Persist config changes to disk.
            config.save(configFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning(LOG_PREFIX + "Failed saving " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    // == Predicates ==
    public boolean isActive() {
        return active;
    }
}

