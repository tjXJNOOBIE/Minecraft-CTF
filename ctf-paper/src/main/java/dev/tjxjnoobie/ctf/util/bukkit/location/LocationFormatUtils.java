package dev.tjxjnoobie.ctf.util.bukkit.location;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Location formatting and cloning helpers.
 */
public final class LocationFormatUtils {

    // == Lifecycle ==
    private LocationFormatUtils() {}

    // == Utilities ==
    /**
     * Returns the result of formatBlockLocation.
     *
     * @param location World location used by this operation.
     * @return Result produced by this method.
     */
    public static String formatBlockLocation(Location location) {
        boolean conditionResult1 = location == null || location.getWorld() == null;
        if (conditionResult1) {
            // Fallback when world or location is unavailable.
            return "unknown";
        }
        // Format as world:x,y,z.
        return location.getWorld().getName()
            + ":" + location.getBlockX()
            + "," + location.getBlockY()
            + "," + location.getBlockZ();
    }

    /**
     * Returns the result of cloneLocation.
     *
     * @param location World location used by this operation.
     * @return Result produced by this method.
     */
    public static Location cloneLocation(Location location) {
        return location == null ? null : location.clone(); // Clone only when present.
    }

    /**
     * Returns the result of toBlockLocation.
     *
     * @param location World location used by this operation.
     * @return Result produced by this method.
     */
    public static Location toBlockLocation(Location location) {
        // Guard: short-circuit when location == null.
        if (location == null) {
            return null;
        }
        World world = location.getWorld();
        // Snap to integer block coordinates.
        return new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Returns the result of toIndicatorLocation.
     *
     * @param baseBlockLocation Location used for flag/base placement or fallback logic.
     * @return Result produced by this method.
     */
    public static Location toIndicatorLocation(Location baseBlockLocation) {
        // Guard: short-circuit when baseBlockLocation == null.
        if (baseBlockLocation == null) {
            return null;
        }
        // Offset to the indicator position above the block.
        return baseBlockLocation.toBlockLocation().add(0.5, 2.25, 0.5);
    }

    /**
     * Returns the result of sameBlock.
     *
     * @param first Operand value used by this operation.
     * @param second Operand value used by this operation.
     * @return Result produced by this method.
     */
    public static boolean sameBlock(Location first, Location second) {
        boolean conditionResult2 = first == null || second == null || first.getWorld() == null || second.getWorld() == null;
        // Guard: short-circuit when first == null || second == null || first.getWorld() == null || second.getWorld() == null.
        if (conditionResult2) {
            return false;
        }
        // Compare world and integer block coordinates.
        return first.getWorld().equals(second.getWorld())
            && first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY()
            && first.getBlockZ() == second.getBlockZ();
    }
}
