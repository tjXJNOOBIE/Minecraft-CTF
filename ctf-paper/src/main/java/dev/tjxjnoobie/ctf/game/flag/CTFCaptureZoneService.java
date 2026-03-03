package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.team.SpawnConfigHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Resolves whether a player location is inside a team's capture zone.
 */
public final class CTFCaptureZoneService {
    private static final double DEFAULT_CAPTURE_RADIUS = 3.0;
    private static final double MIN_CAPTURE_RADIUS = 1.0;

    // Dependencies
    private final SpawnConfigHandler spawnConfigHandler;
    private final TeamManager teamManager;

    public CTFCaptureZoneService(SpawnConfigHandler spawnConfigHandler, TeamManager teamManager) {
        this.spawnConfigHandler = spawnConfigHandler;
        this.teamManager = teamManager;
    }

    /**
     * Returns the configured capture radius from config, clamped to sane minimum.
     */
    public double getCaptureRadius() {
        if (spawnConfigHandler == null) {
            return DEFAULT_CAPTURE_RADIUS;
        }
        double raw = spawnConfigHandler.getCaptureRadius(DEFAULT_CAPTURE_RADIUS);
        return Math.max(MIN_CAPTURE_RADIUS, raw);
    }

    /**
     * Returns true when point is in the same world and within current capture radius.
     */
    public boolean isWithinCaptureRadius(Location center, Location point) {
        if (center == null || point == null || center.getWorld() == null || point.getWorld() == null) {
            return false;
        }
        if (!center.getWorld().equals(point.getWorld())) {
            return false;
        }

        double radius = getCaptureRadius();
        return center.distanceSquared(point) <= radius * radius;
    }

    /**
     * Returns true when the player is within the capture radius and on the team wool.
     */
    public boolean isInsideCaptureZone(String teamKey, Location playerLocation, Location baseLocation) {
        return isInsideCaptureZone(teamKey, playerLocation, List.of(), baseLocation);
    }

    /**
     * Returns true when the player is inside any configured return point radius,
     * otherwise falls back to the wool capture zone check around base.
     */
    public boolean isInsideCaptureZone(String teamKey, Location playerLocation, List<Location> returnPoints, Location baseLocation) {
        if (teamKey == null || playerLocation == null) {
            return false;
        }
        double radius = getCaptureRadius();
        double radiusSquared = radius * radius;

        if (returnPoints != null && !returnPoints.isEmpty()) {
            for (Location returnPoint : returnPoints) {
                if (returnPoint == null || returnPoint.getWorld() == null || playerLocation.getWorld() == null) {
                    continue;
                }
                if (!returnPoint.getWorld().equals(playerLocation.getWorld())) {
                    continue;
                }
                if (returnPoint.distanceSquared(playerLocation) <= radiusSquared) {
                    return true;
                }
            }
        }

        Material teamFloorMaterial = teamManager.getCaptureMaterial(teamKey);
        if (teamFloorMaterial == null) {
            return false;
        }

        Location floorLocation = playerLocation.clone().subtract(0.0, 1.0, 0.0);
        if (floorLocation.getBlock().getType() != teamFloorMaterial) {
            return false;
        }

        if (baseLocation == null || baseLocation.getWorld() == null || playerLocation.getWorld() == null) {
            return true;
        }

        return baseLocation.getWorld().equals(playerLocation.getWorld())
            && baseLocation.distanceSquared(playerLocation) <= radiusSquared;
    }
}

