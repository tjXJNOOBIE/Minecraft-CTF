package dev.tjxjnoobie.ctf.team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages spawn-related configuration stored in ctf-spawns.yml.
 */
public final class SpawnConfigHandler {
    private static final String LOG_PREFIX = "[CTF] [SpawnConfig] ";
    private static final String FILE_NAME = "ctf-spawns.yml";
    private static final double DEFAULT_CAPTURE_RADIUS = 3.0;

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    private boolean active;

    public SpawnConfigHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadSpawnConfig();
    }

    /**
     * Loads spawn configuration from disk, creating file if missing.
     */
    public void loadSpawnConfig() {
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
     * Reloads spawn configuration from disk.
     */
    public void reloadSpawnConfig() {
        if (configFile == null) {
            loadSpawnConfig();
            return;
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        active = true;
    }

    /**
     * Returns true when spawn config is loaded.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the stored lobby spawn when available.
     */
    public Optional<Location> getLobbySpawn() {
        if (config == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(readLocation(config.getConfigurationSection("lobby-spawn")));
    }

    /**
     * Persists the lobby spawn location.
     */
    public void setLobbySpawn(Location location) {
        if (location == null || location.getWorld() == null || config == null) {
            return;
        }
        writeLocation("lobby-spawn", location);
        save();
    }

    /**
     * Returns a team's configured spawn when available.
     */
    public Optional<Location> getTeamSpawn(String teamKey) {
        if (teamKey == null || config == null) {
            return Optional.empty();
        }

        ConfigurationSection section = config.getConfigurationSection("team-spawns." + teamKey);
        return Optional.ofNullable(readLocation(section));
    }

    /**
     * Persists a team's spawn location.
     */
    public void setTeamSpawn(String teamKey, Location location) {
        if (teamKey == null || location == null || location.getWorld() == null || config == null) {
            return;
        }

        writeLocation("team-spawns." + teamKey, location);
        save();
    }

    /**
     * Appends a new return point for a team.
     */
    public boolean addReturnPoint(String teamKey, Location location) {
        if (teamKey == null || location == null || location.getWorld() == null || config == null) {
            return false;
        }

        List<Location> existingPoints = getReturnPoints(teamKey);
        for (Location existing : existingPoints) {
            if (isSameBlock(existing, location)) {
                return false;
            }
        }

        String basePath = "return-points." + teamKey;
        ConfigurationSection existing = config.getConfigurationSection(basePath);
        int nextIndex = existing == null ? 0 : existing.getKeys(false).size();
        String path = basePath + "." + nextIndex;
        writeLocation(path, location);
        save();
        return true;
    }

    /**
     * Returns all configured return points for a team.
     */
    public List<Location> getReturnPoints(String teamKey) {
        List<Location> points = new ArrayList<>();
        if (teamKey == null || config == null) {
            return points;
        }

        ConfigurationSection section = config.getConfigurationSection("return-points." + teamKey);
        if (section == null) {
            return points;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection point = section.getConfigurationSection(key);
            Location location = readLocation(point);
            if (location != null && !containsSameBlock(points, location)) {
                points.add(location);
            }
        }

        return points;
    }

    /**
     * Returns the configured capture radius.
     */
    public double getCaptureRadius(double fallback) {
        if (config == null) {
            return fallback;
        }
        return config.getDouble("capture-zone.radius", fallback);
    }

    /**
     * Persists the capture radius.
     */
    public void setCaptureRadius(double radius) {
        if (config == null) {
            return;
        }
        config.set("capture-zone.radius", radius);
        save();
    }

    private void migrateFromLegacyConfig() {
        if (plugin == null || plugin.getConfig() == null || config == null) {
            return;
        }

        FileConfiguration legacy = plugin.getConfig();
        boolean migrated = false;

        ConfigurationSection lobby = legacy.getConfigurationSection("lobby-spawn");
        Location lobbyLocation = readLocation(lobby);
        if (lobbyLocation != null) {
            writeLocation("lobby-spawn", lobbyLocation);
            migrated = true;
        }

        ConfigurationSection teamSpawns = legacy.getConfigurationSection("team-spawns");
        if (teamSpawns != null) {
            for (String teamKey : teamSpawns.getKeys(false)) {
                ConfigurationSection team = teamSpawns.getConfigurationSection(teamKey);
                Location spawn = readLocation(team);
                if (spawn != null) {
                    writeLocation("team-spawns." + teamKey, spawn);
                    migrated = true;
                }
            }
        }

        ConfigurationSection returnPoints = legacy.getConfigurationSection("return-points");
        if (returnPoints != null) {
            for (String teamKey : returnPoints.getKeys(false)) {
                ConfigurationSection teamSection = returnPoints.getConfigurationSection(teamKey);
                if (teamSection == null) {
                    continue;
                }
                for (String key : teamSection.getKeys(false)) {
                    ConfigurationSection point = teamSection.getConfigurationSection(key);
                    Location location = readLocation(point);
                    if (location != null) {
                        writeLocation("return-points." + teamKey + "." + key, location);
                        migrated = true;
                    }
                }
            }
        }

        double radius = legacy.getDouble("capture-zone.radius", DEFAULT_CAPTURE_RADIUS);
        config.set("capture-zone.radius", radius);
        migrated = true;

        if (migrated) {
            save();
            Bukkit.getLogger().info(LOG_PREFIX + "Migrated legacy spawn data to " + FILE_NAME);
        }
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

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private boolean containsSameBlock(List<Location> locations, Location candidate) {
        if (locations == null || candidate == null) {
            return false;
        }

        for (Location location : locations) {
            if (isSameBlock(location, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameBlock(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().equals(second.getWorld())
            && first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY()
            && first.getBlockZ() == second.getBlockZ();
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
