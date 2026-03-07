package dev.tjxjnoobie.ctf.game.combat.util;

import dev.tjxjnoobie.ctf.game.combat.metadata.SpearShooterMetaData;
import dev.tjxjnoobie.ctf.util.CooldownTracker;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Shared lookup and predicate helpers for homing-spear runtime logic.
 */
public final class HomingSpearTimerUtil {

    private HomingSpearTimerUtil() {
    }

    /**
     * Resolves an online player by uuid.
     *
     * @param playerId Player id to look up.
     * @return Online player instance, or {@code null} when unavailable.
     */
    public static Player resolveOnlinePlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return null;
        }
        return player;
    }

    /**
     * Resolves the currently tracked trident from shooter state.
     *
     * @param shooterState Shooter state containing the active projectile id.
     * @return Live tracked trident, or {@code null} when unavailable.
     */
    public static Trident resolveActiveTrident(SpearShooterMetaData shooterState) {
        UUID spearEntityId = shooterState == null ? null : shooterState.getActiveSpearEntityId();
        if (spearEntityId == null) {
            return null;
        }

        Entity entity = Bukkit.getEntity(spearEntityId);
        if (!(entity instanceof Trident trident) || trident.isDead()) {
            return null;
        }
        return trident;
    }

    /**
     * Returns whether two locations belong to the same loaded world.
     *
     * @param first First location to compare.
     * @param second Second location to compare.
     * @return {@code true} when both locations share the same world.
     */
    public static boolean sameWorld(Location first, Location second) {
        return first != null
                && second != null
                && first.getWorld() != null
                && first.getWorld().equals(second.getWorld());
    }

    /**
     * Returns whether the damage cause supports indirect homing-spear attribution.
     *
     * @param cause Damage cause to inspect.
     * @return {@code true} when the cause is part of the indirect attribution window.
     */
    public static boolean isIndirectAttributionCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.DROWNING;
    }

    /**
     * Returns remaining cooldown from the shooter expiry timestamp.
     *
     * @param shooterState Shooter state containing the cooldown expiry.
     * @param nowMs Current wall-clock time in milliseconds.
     * @return Remaining cooldown in milliseconds.
     */
    public static long getCooldownRemainingMs(SpearShooterMetaData shooterState, long nowMs) {
        if (shooterState == null) {
            return 0L;
        }
        return CooldownTracker.remainingFromExpiry(shooterState.getCooldownUntilMs(), nowMs);
    }

    /**
     * Returns remaining time from a generic expiry timestamp.
     *
     * @param expiryMs Absolute expiry time in milliseconds.
     * @param nowMs Current wall-clock time in milliseconds.
     * @return Remaining time in milliseconds.
     */
    public static long getRemainingMsFromExpiry(long expiryMs, long nowMs) {
        return CooldownTracker.remainingFromExpiry(expiryMs, nowMs);
    }

    /**
     * Finds the first valid enemy in range of the spear location.
     *
     * @param spearLocation Current spear location.
     * @param candidates Enemy candidates to scan.
     * @param scanRangeBlocks Maximum scan distance in blocks.
     * @return First valid enemy player in range, or {@code null} when none qualify.
     */
    public static Player findFirstEnemyInRange(Location spearLocation, Iterable<Player> candidates, int scanRangeBlocks) {
        if (spearLocation == null || candidates == null) {
            return null;
        }

        double rangeSquared = scanRangeBlocks * (double) scanRangeBlocks;
        for (Player enemy : candidates) {
            // Skip offline players and cross-world targets before doing the distance check.
            if (enemy == null || !enemy.isOnline()) {
                continue;
            }
            if (!sameWorld(spearLocation, enemy.getLocation())) {
                continue;
            }
            if (enemy.getLocation().distanceSquared(spearLocation) <= rangeSquared) {
                return enemy;
            }
        }
        return null;
    }

    /**
     * Formats a millisecond duration as a one-decimal second string.
     *
     * @param remainingMs Duration to format.
     * @return One-decimal seconds string such as {@code 2.4}.
     */
    public static String formatTenths(long remainingMs) {
        double seconds = Math.max(0.0d, remainingMs / 1000.0d);
        double floored = Math.floor(seconds * 10.0d) / 10.0d;
        return String.format(Locale.US, "%.1f", floored);
    }
}
