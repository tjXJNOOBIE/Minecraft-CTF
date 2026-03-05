package dev.tjxjnoobie.ctf.team;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
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

    // == Constants ==
    private static final String LOG_PREFIX = "[CTF] [SpawnConfig] ";
    private static final String FILE_NAME = "ctf-spawns.yml";
    private static final double DEFAULT_CAPTURE_RADIUS = 3.0;
    private final JavaPlugin plugin;

    // == Runtime state ==
    private FileConfiguration config;
    private File configFile;
    private boolean active;

    // == Lifecycle ==
    /**
     * Constructs a SpawnConfigHandler instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public SpawnConfigHandler(JavaPlugin plugin) {
        // Store plugin; loading is invoked explicitly during bootstrap.
        this.plugin = plugin;
    }

    /**
     * Returns data for loadSpawnConfig.
     */
    public void loadSpawnConfig() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }

        File dataFolder = plugin.getDataFolder(); // Ensure data folder exists before reading config.
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
     * Executes reloadSpawnConfig.
     */
    public void reloadSpawnConfig() {
        if (configFile == null) {
            loadSpawnConfig();
            return;
        }
        config = YamlConfiguration.loadConfiguration(configFile); // Reload config from disk.
        mergeDefaultsFromResource();
        active = true;
    }

    // == Getters ==
    public Optional<Location> getLobbySpawn() {
        // Guard: short-circuit when config == null.
        if (config == null) {
            return Optional.empty();
        }
        // Read lobby spawn from config.
        return Optional.ofNullable(readLocation(config.getConfigurationSection(CTFKeys.spawnLobbyPath())));
    }

    public Optional<Location> getTeamSpawn(String teamKey) {
        // Guard: short-circuit when teamKey == null || config == null.
        if (teamKey == null || config == null) {
            return Optional.empty();
        }

        ConfigurationSection section = config.getConfigurationSection(CTFKeys.spawnTeamSpawnsPathPrefix() + teamKey); // Read team spawn location.
        return Optional.ofNullable(readLocation(section));
    }

    public List<Location> getReturnPoints(String teamKey) {
        List<Location> points = new ArrayList<>();
        // Guard: short-circuit when teamKey == null || config == null.
        if (teamKey == null || config == null) {
            return points;
        }

        ConfigurationSection section = config.getConfigurationSection(CTFKeys.spawnReturnPointsPathPrefix() + teamKey); // Read all return points for the team.
        // Guard: short-circuit when section == null.
        if (section == null) {
            return points;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection point = section.getConfigurationSection(key);
            Location location = readLocation(point);
            boolean conditionResult4 = location != null && !containsSameBlock(points, location);
            if (conditionResult4) {
                points.add(location);
            }
        }

        return points;
    }

    public double getCaptureRadius(double fallback) {
        // Guard: short-circuit when config == null.
        if (config == null) {
            return fallback;
        }
        // Read capture radius with fallback.
        return config.getDouble(CTFKeys.spawnCaptureZoneRadiusPath(), fallback);
    }

    // == Setters ==
    public void setLobbySpawn(Location location) {
        boolean conditionResult1 = location == null || location.getWorld() == null || config == null;
        // Guard: short-circuit when location == null || location.getWorld() == null || config == null.
        if (conditionResult1) {
            return;
        }
        // Persist lobby spawn location.
        writeLocation(CTFKeys.spawnLobbyPath(), location);
        save();
    }

    public void setTeamSpawn(String teamKey, Location location) {
        boolean conditionResult2 = teamKey == null || location == null || location.getWorld() == null || config == null;
        // Guard: short-circuit when teamKey == null || location == null || location.getWorld() == null || config == null.
        if (conditionResult2) {
            return;
        }

        // Persist team spawn location.
        writeLocation(CTFKeys.spawnTeamSpawnsPathPrefix() + teamKey, location);
        save();
    }

    public void setCaptureRadius(double radius) {
        // Guard: short-circuit when config == null.
        if (config == null) {
            return;
        }
        // Persist capture radius.
        config.set(CTFKeys.spawnCaptureZoneRadiusPath(), radius);
        save();
    }

    // == Utilities ==
    /**
     * Returns the result of addReturnPoint.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @param location World location used by this operation.
     * @return Result produced by this method.
     */
    public boolean addReturnPoint(String teamKey, Location location) {
        boolean conditionResult3 = teamKey == null || location == null || location.getWorld() == null || config == null;
        // Guard: short-circuit when teamKey == null || location == null || location.getWorld() == null || config == null.
        if (conditionResult3) {
            return false;
        }

        List<Location> existingPoints = getReturnPoints(teamKey); // Reject duplicate return points for the same block.
        for (Location existing : existingPoints) {
            boolean sameBlock = isSameBlock(existing, location);
            // Guard: short-circuit when sameBlock.
            if (sameBlock) {
                return false;
            }
        }

        String basePath = CTFKeys.spawnReturnPointsPathPrefix() + teamKey;
        ConfigurationSection existing = config.getConfigurationSection(basePath);
        int nextIndex = existing == null ? 0 : existing.getKeys(false).size();
        String path = basePath + "." + nextIndex;
        writeLocation(path, location);
        save();
        return true;
    }

    /**
     * Removes the nearest stored return point to the provided location.
     *
     * @param reference Reference location used to choose the nearest return point.
     * @return Team key for the removed return point, or {@code null} when none match.
     */
    public String removeNearestReturnPoint(Location reference) {
        boolean invalidReference = reference == null || reference.getWorld() == null || config == null;
        if (invalidReference) {
            return null;
        }

        String nearestTeamKey = null;
        String nearestPath = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (TeamId teamId : TeamId.values()) {
            String teamKey = teamId.key();
            String basePath = CTFKeys.spawnReturnPointsPathPrefix() + teamKey;
            ConfigurationSection section = config.getConfigurationSection(basePath);
            if (section == null) {
                continue;
            }

            for (String key : section.getKeys(false)) {
                String path = basePath + "." + key;
                Location candidate = readLocation(config.getConfigurationSection(path));
                if (!isComparableLocation(reference, candidate)) {
                    continue;
                }

                double distanceSquared = reference.distanceSquared(candidate);
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearestTeamKey = teamKey;
                    nearestPath = path;
                }
            }
        }

        if (nearestPath == null || nearestTeamKey == null) {
            return null;
        }

        config.set(nearestPath, null);
        save();
        return nearestTeamKey;
    }

    private void migrateFromLegacyConfig() {
        boolean conditionResult5 = plugin == null || plugin.getConfig() == null || config == null;
        // Guard: short-circuit when plugin == null || plugin.getConfig() == null || config == null.
        if (conditionResult5) {
            return;
        }

        FileConfiguration legacy = plugin.getConfig(); // Convert legacy config.yml spawn entries into the new file.
        boolean migrated = false;

        ConfigurationSection lobby = legacy.getConfigurationSection(CTFKeys.spawnLobbyPath());
        Location lobbyLocation = readLocation(lobby);
        if (lobbyLocation != null) {
            writeLocation(CTFKeys.spawnLobbyPath(), lobbyLocation);
            migrated = true;
        }

        ConfigurationSection teamSpawns = legacy.getConfigurationSection("team-spawns");
        if (teamSpawns != null) {
            for (String teamKey : teamSpawns.getKeys(false)) {
                ConfigurationSection team = teamSpawns.getConfigurationSection(teamKey);
                Location spawn = readLocation(team);
                if (spawn != null) {
                    writeLocation(CTFKeys.spawnTeamSpawnsPathPrefix() + teamKey, spawn);
                    migrated = true;
                }
            }
        }

        ConfigurationSection returnPoints = legacy.getConfigurationSection("return-points");
        if (returnPoints != null) {
            for (String teamKey : returnPoints.getKeys(false)) {
                ConfigurationSection teamSection = returnPoints.getConfigurationSection(teamKey);
                // Guard: short-circuit when teamSection == null.
                if (teamSection == null) {
                    continue;
                }
                for (String key : teamSection.getKeys(false)) {
                    ConfigurationSection point = teamSection.getConfigurationSection(key);
                    Location location = readLocation(point);
                    if (location != null) {
                        writeLocation(CTFKeys.spawnReturnPointsPathPrefix() + teamKey + "." + key, location);
                        migrated = true;
                    }
                }
            }
        }

        double radius = legacy.getDouble(CTFKeys.spawnCaptureZoneRadiusPath(), DEFAULT_CAPTURE_RADIUS);
        config.set(CTFKeys.spawnCaptureZoneRadiusPath(), radius);
        migrated = true;

        if (migrated) {
            save();
            Bukkit.getLogger().info(LOG_PREFIX + "Migrated legacy spawn data to " + FILE_NAME);
        }
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

        World world = Bukkit.getWorld(worldName); // Resolve world for the stored location.
        // Guard: short-circuit when world == null.
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
        boolean conditionResult6 = location == null || location.getWorld() == null;
        // Guard: short-circuit when location == null || location.getWorld() == null.
        if (conditionResult6) {
            return;
        }

        // Persist world and coordinates.
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private boolean containsSameBlock(List<Location> locations, Location candidate) {
        // Guard: short-circuit when locations == null || candidate == null.
        if (locations == null || candidate == null) {
            return false;
        }

        // Ensure unique block positions in a list.
        for (Location location : locations) {
            boolean sameBlock2 = isSameBlock(location, candidate);
            // Guard: short-circuit when sameBlock2.
            if (sameBlock2) {
                return true;
            }
        }
        return false;
    }

    private void save() {
        // Guard: short-circuit when configFile == null || config == null.
        if (configFile == null || config == null) {
            return;
        }
        try {
            // Write the config to disk.
            config.save(configFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning(LOG_PREFIX + "Failed saving " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    // == Predicates ==
    public boolean isActive() {
        return active;
    }

    private boolean isSameBlock(Location first, Location second) {
        boolean conditionResult7 = first == null || second == null || first.getWorld() == null || second.getWorld() == null;
        // Guard: short-circuit when first == null || second == null || first.getWorld() == null || second.getWorld() == null.
        if (conditionResult7) {
            return false;
        }
        // Compare block coordinates in the same world.
        return first.getWorld().equals(second.getWorld())
            && first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY()
            && first.getBlockZ() == second.getBlockZ();
    }

    private boolean isComparableLocation(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().equals(second.getWorld());
    }
}

