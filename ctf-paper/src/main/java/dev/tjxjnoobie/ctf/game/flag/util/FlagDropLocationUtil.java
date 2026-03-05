package dev.tjxjnoobie.ctf.game.flag.util;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Pure calculations for safer dropped-flag positions in scoring zones.
 */
public final class FlagDropLocationUtil {
    private static final double MIN_DROP_OFFSET_BLOCKS = 2.0;

    // == Lifecycle ==
    private FlagDropLocationUtil() {
    }

    // == Getters ==
    public static Location resolveDropLocation(Player player, Location rawDropLocation, Location baseLocation,
                                               List<Location> returnPoints, double captureRadius) {
        boolean conditionResult1 = rawDropLocation == null || rawDropLocation.getWorld() == null;
        // Guard: short-circuit when rawDropLocation == null || rawDropLocation.getWorld() == null.
        if (conditionResult1) {
            return rawDropLocation;
        }

        Location blockDrop = toBlockLocation(rawDropLocation); // Snap the drop to block coordinates for consistent positioning.
        Location nearestZoneCenter = findNearestScoringZone(blockDrop, baseLocation, returnPoints, captureRadius); // Find the nearest scoring zone the drop is inside.
        // Guard: short-circuit when nearestZoneCenter == null.
        if (nearestZoneCenter == null) {
            return blockDrop;
        }

        double requiredOffset = Math.max(MIN_DROP_OFFSET_BLOCKS, captureRadius + 1.0); // Push the drop outside the capture radius with a minimum offset.
        Vector away = blockDrop.toVector().subtract(nearestZoneCenter.toVector());
        away.setY(0.0);

        boolean conditionResult2 = away.lengthSquared() < 0.0001 && player != null;
        if (conditionResult2) {
            away = player.getLocation().getDirection().setY(0.0); // Fall back to player facing direction when offset is too small.
        }
        boolean conditionResult3 = away.lengthSquared() < 0.0001;
        if (conditionResult3) {
            away = new Vector(1.0, 0.0, 0.0); // Final fallback to a unit vector if direction is still degenerate.
        }

        // Normalize and apply offset.
        away.normalize().multiply(requiredOffset);
        Location shifted = nearestZoneCenter.clone().add(away.getX(), 0.0, away.getZ());
        shifted.setY(blockDrop.getY());
        // Return a block-aligned result.
        return toBlockLocation(shifted);
    }

    private static Location findNearestScoringZone(Location point, Location baseLocation, List<Location> returnPoints, double captureRadius) {
        Location nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        double radiusSquared = captureRadius * captureRadius;

        boolean withinRadius = isWithinRadius(baseLocation, point, radiusSquared);
        if (withinRadius) {
            nearest = baseLocation; // Base location is within the scoring radius.
            nearestDistanceSquared = baseLocation.distanceSquared(point);
        }

        boolean conditionResult4 = returnPoints == null || returnPoints.isEmpty();
        // Guard: short-circuit when returnPoints == null || returnPoints.isEmpty().
        if (conditionResult4) {
            return nearest;
        }

        for (Location returnPoint : returnPoints) {
            boolean withinRadius2 = isWithinRadius(returnPoint, point, radiusSquared);
            // Guard: short-circuit when !withinRadius2.
            if (!withinRadius2) {
                continue;
            }

            double distanceSquared = returnPoint.distanceSquared(point);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared; // Track the closest scoring zone.
                nearest = returnPoint;
            }
        }
        return nearest;
    }

    // == Utilities ==
    private static Location toBlockLocation(Location location) {
        // Guard: short-circuit when location == null.
        if (location == null) {
            return null;
        }
        World world = location.getWorld();
        // Create a new location with integer block coords.
        return new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // == Predicates ==
    private static boolean isWithinRadius(Location center, Location point, double radiusSquared) {
        boolean conditionResult5 = center == null || point == null || center.getWorld() == null || point.getWorld() == null;
        // Guard: short-circuit when center == null || point == null || center.getWorld() == null || point.getWorld() == null.
        if (conditionResult5) {
            return false;
        }
        boolean conditionResult6 = !center.getWorld().equals(point.getWorld());
        // Guard: short-circuit when !center.getWorld().equals(point.getWorld()).
        if (conditionResult6) {
            return false;
        }
        // Compare distance without square roots.
        return center.distanceSquared(point) <= radiusSquared;
    }
}

