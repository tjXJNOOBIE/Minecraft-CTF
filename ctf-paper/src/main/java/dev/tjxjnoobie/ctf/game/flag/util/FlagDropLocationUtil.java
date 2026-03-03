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

    private FlagDropLocationUtil() {
    }

    /**
     * Returns a block location for dropped flags. If death happened in a scoring zone,
     * the drop is shifted outside the zone radius so it does not sit directly on capture spots.
     */
    public static Location resolveDropLocation(Player player, Location rawDropLocation, Location baseLocation,
                                               List<Location> returnPoints, double captureRadius) {
        if (rawDropLocation == null || rawDropLocation.getWorld() == null) {
            return rawDropLocation;
        }

        Location blockDrop = toBlockLocation(rawDropLocation);
        Location nearestZoneCenter = findNearestScoringZone(blockDrop, baseLocation, returnPoints, captureRadius);
        if (nearestZoneCenter == null) {
            return blockDrop;
        }

        double requiredOffset = Math.max(MIN_DROP_OFFSET_BLOCKS, captureRadius + 1.0);
        Vector away = blockDrop.toVector().subtract(nearestZoneCenter.toVector());
        away.setY(0.0);

        if (away.lengthSquared() < 0.0001 && player != null) {
            away = player.getLocation().getDirection().setY(0.0);
        }
        if (away.lengthSquared() < 0.0001) {
            away = new Vector(1.0, 0.0, 0.0);
        }

        away.normalize().multiply(requiredOffset);
        Location shifted = nearestZoneCenter.clone().add(away.getX(), 0.0, away.getZ());
        shifted.setY(blockDrop.getY());
        return toBlockLocation(shifted);
    }

    private static Location findNearestScoringZone(Location point, Location baseLocation, List<Location> returnPoints, double captureRadius) {
        Location nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        double radiusSquared = captureRadius * captureRadius;

        if (isWithinRadius(baseLocation, point, radiusSquared)) {
            nearest = baseLocation;
            nearestDistanceSquared = baseLocation.distanceSquared(point);
        }

        if (returnPoints == null || returnPoints.isEmpty()) {
            return nearest;
        }

        for (Location returnPoint : returnPoints) {
            if (!isWithinRadius(returnPoint, point, radiusSquared)) {
                continue;
            }

            double distanceSquared = returnPoint.distanceSquared(point);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = returnPoint;
            }
        }
        return nearest;
    }

    private static boolean isWithinRadius(Location center, Location point, double radiusSquared) {
        if (center == null || point == null || center.getWorld() == null || point.getWorld() == null) {
            return false;
        }
        if (!center.getWorld().equals(point.getWorld())) {
            return false;
        }
        return center.distanceSquared(point) <= radiusSquared;
    }

    private static Location toBlockLocation(Location location) {
        if (location == null) {
            return null;
        }
        World world = location.getWorld();
        return new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}

