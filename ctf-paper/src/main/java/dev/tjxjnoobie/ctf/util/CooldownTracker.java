package dev.tjxjnoobie.ctf.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reusable UUID cooldown storage.
 */
public final class CooldownTracker {

    // == Runtime state ==
    private final Map<UUID, Long> cooldownUntilMsById = new HashMap<>();

    // == Lifecycle ==
    /**
     * Executes the start operation.
     *
     * @param id Identifier for the e nt it y.
     * @param durationMs Duration or timestamp value in milliseconds.
     * @param nowMs Timestamp or duration in milliseconds.
     */
    public void start(UUID id, long durationMs, long nowMs) {
        // Guard: short-circuit when id == null.
        if (id == null) {
            return;
        }
        long safeDuration = Math.max(0L, durationMs);
        setCooldownUntilMs(id, nowMs + safeDuration);
    }

    // == Getters ==
    public long getCooldownUntilMs(UUID id) {
        // Guard: short-circuit when id == null.
        if (id == null) {
            return 0L;
        }
        return cooldownUntilMsById.getOrDefault(id, 0L);
    }

    public long getCooldownRemainingMs(UUID id, long nowMs) {
        long untilMs = getCooldownUntilMs(id);
        return Math.max(0L, untilMs - nowMs);
    }

    // == Setters ==
    public void setCooldownUntilMs(UUID id, long untilMs) {
        // Guard: short-circuit when id == null.
        if (id == null) {
            return;
        }
        cooldownUntilMsById.put(id, Math.max(0L, untilMs));
    }

    // == Utilities ==
    /**
     * Executes clearCooldown.
     *
     * @param id Identifier for the e nt it y.
     */
    public void clearCooldown(UUID id) {
        // Guard: short-circuit when id == null.
        if (id == null) {
            return;
        }
        cooldownUntilMsById.remove(id);
    }

    /**
     * Executes clearAllCooldowns.
     */
    public void clearAllCooldowns() {
        cooldownUntilMsById.clear();
    }

    /**
     * Returns the result of remainingFromExpiry.
     *
     * @param expiryMs Duration or timestamp value in milliseconds.
     * @param nowMs Timestamp or duration in milliseconds.
     * @return Result produced by this method.
     */
    public static long remainingFromExpiry(long expiryMs, long nowMs) {
        return Math.max(0L, expiryMs - nowMs);
    }

    // == Predicates ==
    public boolean isOnCooldown(UUID id, long nowMs) {
        return getCooldownRemainingMs(id, nowMs) > 0L;
    }
}
