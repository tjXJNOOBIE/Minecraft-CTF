package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.dependency.interfaces.PluginConfigDependencyAccess;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

/**
 * Manages locator-bar waypoint markers for team bases.
 */
public final class BaseMarkerHandler implements PluginConfigDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[BaseMarkerHandler] ";
    private static final String CONFIG_ROOT = "locator-markers";
    private static final double WAYPOINT_TRANSMIT_RANGE = 1_000_000.0;
    private static final String RED_HEX = "FF0000";
    private static final String BLUE_HEX = "00A2FF";
    private static final String RED_STYLE = null;
    private static final String BLUE_STYLE = null;

    // == Runtime state ==
    private final Map<String, UUID> markerIdsByTeam = new HashMap<>();

    // == Lifecycle ==
    /**
     * Constructs a BaseMarkerHandler instance.
     */
    public BaseMarkerHandler() {
    }

    /**
     * Executes initializeFromConfig.
     */
    public void initializeFromConfig() {
        loadStoredMarkers();
    }

    private void loadStoredMarkers() {
        boolean conditionResult3 = getMainPlugin().getConfig() == null;
        // Guard: short-circuit when mainConfig() == null.
        if (conditionResult3) {
            return;
        }

        loadMarkerForTeam(TeamManager.RED);
        loadMarkerForTeam(TeamManager.BLUE);
    }

    private void loadMarkerForTeam(String teamKey) {
        String raw = getMainPlugin().getConfig().getString(CONFIG_ROOT + "." + teamKey + ".uuid", null);
        // Guard: short-circuit when raw == null.
        if (raw == null) {
            return;
        }

        try {
            markerIdsByTeam.put(teamKey, UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            Bukkit.getLogger().info(LOG_PREFIX + "Invalid marker UUID for team=" + teamKey);
        }
    }

    // == Getters ==
    private ArmorStand resolveMarker(String teamKey) {
        UUID markerId = markerIdsByTeam.get(teamKey);
        // Guard: short-circuit when markerId == null.
        if (markerId == null) {
            return null;
        }

        Entity entity = Bukkit.getEntity(markerId);
        // Guard: short-circuit when entity instanceof ArmorStand stand && !stand.isDead().
        if (entity instanceof ArmorStand stand && !stand.isDead()) {
            return stand;
        }

        markerIdsByTeam.remove(teamKey);
        persistMarker(teamKey, null);
        return null;
    }

    // == Utilities ==
    /**
     * Spawns or moves the base marker for a team to the given location.
     */
    private void spawnOrMoveBaseMarker(String teamKey, Location baseLocation) {
        boolean conditionResult1 = teamKey == null || baseLocation == null || baseLocation.getWorld() == null;
        // Guard: short-circuit when teamKey == null || baseLocation == null || baseLocation.getWorld() == null.
        if (conditionResult1) {
            return;
        }

        Location markerLocation = normalizeMarkerLocation(baseLocation);
        ArmorStand marker = resolveMarker(teamKey);
        if (marker == null) {
            marker = spawnMarker(markerLocation);
            // Guard: short-circuit when marker == null.
            if (marker == null) {
                return;
            }
            markerIdsByTeam.put(teamKey, marker.getUniqueId());
            persistMarker(teamKey, marker.getUniqueId());
        } else {
            marker.teleport(markerLocation);
        }

        applyWaypointSettings(marker, teamKey);
    }

    /**
     * Executes spawnOrMoveBaseMarker.
     *
     * @param baseData Resolved team base metadata used for capture and marker checks.
     */
    public void spawnOrMoveBaseMarker(TeamBaseMetaData baseData) {
        boolean conditionResult2 = baseData == null || baseData.getTeamId() == null;
        // Guard: short-circuit when baseData == null || baseData.getTeamId() == null.
        if (conditionResult2) {
            return;
        }

        Location flagBase = baseData.getFlagSpawnLocation();
        if (flagBase == null) {
            flagBase = baseData.getFlagBlockLocation();
        }
        // Guard: short-circuit when flagBase == null.
        if (flagBase == null) {
            return;
        }
        spawnOrMoveBaseMarker(baseData.getTeamId().key(), flagBase);
    }

    /**
     * Executes removeAllMarkers.
     */
    public void removeAllMarkers() {
        for (UUID markerId : markerIdsByTeam.values()) {
            Entity entity = Bukkit.getEntity(markerId);
            if (entity != null) {
                entity.remove();
            }
        }
        markerIdsByTeam.clear();
        clearPersistedMarkers();
    }

    private void persistMarker(String teamKey, UUID markerId) {
        boolean conditionResult4 = getMainPlugin().getConfig() == null;
        // Guard: short-circuit when mainConfig() == null.
        if (conditionResult4) {
            return;
        }

        getMainPlugin().getConfig().set(CONFIG_ROOT + "." + teamKey + ".uuid", markerId == null ? null : markerId.toString());
        getMainPlugin().saveConfig();
    }

    private void clearPersistedMarkers() {
        boolean conditionResult5 = getMainPlugin().getConfig() == null;
        // Guard: short-circuit when mainConfig() == null.
        if (conditionResult5) {
            return;
        }

        getMainPlugin().getConfig().set(CONFIG_ROOT, null);
        getMainPlugin().saveConfig();
    }

    private ArmorStand spawnMarker(Location location) {
        boolean conditionResult6 = location == null || location.getWorld() == null;
        // Guard: short-circuit when location == null || location.getWorld() == null.
        if (conditionResult6) {
            return null;
        }

        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(true);
        stand.setCollidable(false);
        stand.setSilent(true);
        stand.setCanPickupItems(false);
        return stand;
    }

    private void applyWaypointSettings(ArmorStand marker, String teamKey) {
        // Guard: short-circuit when marker == null || teamKey == null.
        if (marker == null || teamKey == null) {
            return;
        }

        AttributeInstance range = marker.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (range != null) {
            range.setBaseValue(WAYPOINT_TRANSMIT_RANGE);
        }

        String hex = TeamManager.RED.equals(teamKey) ? RED_HEX : BLUE_HEX;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            "waypoint modify " + marker.getUniqueId() + " color hex " + hex);

        String style = TeamManager.RED.equals(teamKey) ? RED_STYLE : BLUE_STYLE;
        boolean conditionResult7 = style != null && !style.isBlank();
        if (conditionResult7) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "waypoint modify " + marker.getUniqueId() + " style " + style);
        }
    }

    private Location normalizeMarkerLocation(Location baseLocation) {
        Location location = baseLocation.clone();
        location.setX(baseLocation.getBlockX() + 0.5);
        location.setY(baseLocation.getBlockY() - 0.5);
        location.setZ(baseLocation.getBlockZ() + 0.5);
        return location;
    }
}

