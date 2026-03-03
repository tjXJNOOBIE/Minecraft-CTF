package dev.tjxjnoobie.ctf.game.flag;

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
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages locator-bar waypoint markers for team bases.
 */
public final class BaseMarkerService {
    private static final String LOG_PREFIX = "[CTF] [BaseMarkerService] ";
    private static final String CONFIG_ROOT = "locator-markers";
    private static final double WAYPOINT_TRANSMIT_RANGE = 1_000_000.0;
    private static final String RED_HEX = "FF0000";
    private static final String BLUE_HEX = "00A2FF";
    private static final String RED_STYLE = null;
    private static final String BLUE_STYLE = null;

    private final JavaPlugin plugin;
    private final Map<String, UUID> markerIdsByTeam = new HashMap<>();

    public BaseMarkerService(JavaPlugin plugin) {
        this.plugin = plugin;
        loadStoredMarkers();
    }

    /**
     * Spawns or moves the base marker for a team to the given location.
     */
    public void spawnOrMoveBaseMarker(String teamKey, Location baseLocation) {
        if (teamKey == null || baseLocation == null || baseLocation.getWorld() == null) {
            return;
        }

        Location markerLocation = normalizeMarkerLocation(baseLocation);
        ArmorStand marker = resolveMarker(teamKey);
        if (marker == null) {
            marker = spawnMarker(markerLocation);
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
     * Removes all locator markers and clears persisted entries.
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

    private void loadStoredMarkers() {
        if (plugin == null || plugin.getConfig() == null) {
            return;
        }

        loadMarkerForTeam(TeamManager.RED);
        loadMarkerForTeam(TeamManager.BLUE);
    }

    private void loadMarkerForTeam(String teamKey) {
        String raw = plugin.getConfig().getString(CONFIG_ROOT + "." + teamKey + ".uuid", null);
        if (raw == null) {
            return;
        }

        try {
            markerIdsByTeam.put(teamKey, UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            Bukkit.getLogger().info(LOG_PREFIX + "Invalid marker UUID for team=" + teamKey);
        }
    }

    private void persistMarker(String teamKey, UUID markerId) {
        if (plugin == null || plugin.getConfig() == null) {
            return;
        }

        plugin.getConfig().set(CONFIG_ROOT + "." + teamKey + ".uuid", markerId == null ? null : markerId.toString());
        plugin.saveConfig();
    }

    private void clearPersistedMarkers() {
        if (plugin == null || plugin.getConfig() == null) {
            return;
        }

        plugin.getConfig().set(CONFIG_ROOT, null);
        plugin.saveConfig();
    }

    private ArmorStand resolveMarker(String teamKey) {
        UUID markerId = markerIdsByTeam.get(teamKey);
        if (markerId == null) {
            return null;
        }

        Entity entity = Bukkit.getEntity(markerId);
        if (entity instanceof ArmorStand stand && !stand.isDead()) {
            return stand;
        }

        markerIdsByTeam.remove(teamKey);
        persistMarker(teamKey, null);
        return null;
    }

    private ArmorStand spawnMarker(Location location) {
        if (location == null || location.getWorld() == null) {
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
        if (style != null && !style.isBlank()) {
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

