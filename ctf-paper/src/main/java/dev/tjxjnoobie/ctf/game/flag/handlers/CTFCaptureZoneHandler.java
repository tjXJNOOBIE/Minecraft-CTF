package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;

import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Resolves whether a player location is inside a team's capture zone.
 */
public final class CTFCaptureZoneHandler implements PlayerDependencyAccess {

    // == Constants ==
    private static final double DEFAULT_CAPTURE_RADIUS = 3.0;
    private static final double MIN_CAPTURE_RADIUS = 1.0;

    // == Getters ==
    public double getCaptureRadius() {
        var spawnConfigHandler = getTeamManager() == null ? null : getTeamManager().getSpawnConfigHandler();
        // Guard: short-circuit when spawnConfigHandler == null.
        if (spawnConfigHandler == null) {
            return DEFAULT_CAPTURE_RADIUS;
        }
        double raw = spawnConfigHandler.getCaptureRadius(DEFAULT_CAPTURE_RADIUS);
        return Math.max(MIN_CAPTURE_RADIUS, raw);
    }

    // == Utilities ==
    private double horizontalDistanceSquared(Location first, Location second) {
        // Guard: short-circuit when first == null || second == null.
        if (first == null || second == null) {
            return Double.MAX_VALUE;
        }
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return (dx * dx) + (dz * dz);
    }

    // == Predicates ==
    public boolean isWithinCaptureRadius(Location center, Location point) {
        boolean conditionResult1 = center == null || point == null || center.getWorld() == null || point.getWorld() == null;
        // Guard: short-circuit when center == null || point == null || center.getWorld() == null || point.getWorld() == null.
        if (conditionResult1) {
            return false;
        }
        boolean conditionResult2 = !center.getWorld().equals(point.getWorld());
        // Guard: short-circuit when !center.getWorld().equals(point.getWorld()).
        if (conditionResult2) {
            return false;
        }

        double radius = getCaptureRadius();
        return horizontalDistanceSquared(center, point) <= radius * radius;
    }

    private boolean isInsideCaptureZone(String teamKey, Location playerLocation, List<Location> returnPoints, Location baseLocation) {
        // Guard: short-circuit when teamKey == null || playerLocation == null.
        if (teamKey == null || playerLocation == null) {
            return false;
        }
        double radius = getCaptureRadius();
        double radiusSquared = radius * radius;

        boolean conditionResult3 = returnPoints != null && !returnPoints.isEmpty();
        if (conditionResult3) {
            for (Location returnPoint : returnPoints) {
                boolean conditionResult4 = returnPoint == null || returnPoint.getWorld() == null || playerLocation.getWorld() == null;
                // Guard: short-circuit when returnPoint == null || returnPoint.getWorld() == null || playerLocation.getWorld() == null.
                if (conditionResult4) {
                    continue;
                }
                boolean conditionResult5 = !returnPoint.getWorld().equals(playerLocation.getWorld());
                // Guard: short-circuit when !returnPoint.getWorld().equals(playerLocation.getWorld()).
                if (conditionResult5) {
                    continue;
                }
                boolean conditionResult6 = horizontalDistanceSquared(returnPoint, playerLocation) <= radiusSquared;
                // Guard: short-circuit when horizontalDistanceSquared(returnPoint, playerLocation) <= radiusSquared.
                if (conditionResult6) {
                    return true;
                }
            }
        }

        Material teamFloorMaterial = getTeamManager().getCaptureMaterial(teamKey);
        // Guard: short-circuit when teamFloorMaterial == null.
        if (teamFloorMaterial == null) {
            return false;
        }

        Location floorLocation = playerLocation.clone().subtract(0.0, 1.0, 0.0);
        boolean conditionResult7 = floorLocation.getBlock().getType() != teamFloorMaterial;
        // Guard: short-circuit when floorLocation.getBlock().getType() != teamFloorMaterial.
        if (conditionResult7) {
            return false;
        }

        boolean conditionResult8 = baseLocation == null || baseLocation.getWorld() == null || playerLocation.getWorld() == null;
        // Guard: short-circuit when baseLocation == null || baseLocation.getWorld() == null || playerLocation.getWorld() == null.
        if (conditionResult8) {
            return true;
        }

        return baseLocation.getWorld().equals(playerLocation.getWorld())
            && horizontalDistanceSquared(baseLocation, playerLocation) <= radiusSquared;
    }

    public boolean isInsideCaptureZone(TeamBaseMetaData baseData, Location playerLocation) {
        boolean conditionResult9 = baseData == null || baseData.getTeamId() == null;
        // Guard: short-circuit when baseData == null || baseData.getTeamId() == null.
        if (conditionResult9) {
            return false;
        }
        return isInsideCaptureZone(
            baseData.getTeamId().key(),
            playerLocation,
            baseData.getReturnSpawnLocations(),
            baseData.getFlagSpawnLocation()
        );
    }
}

