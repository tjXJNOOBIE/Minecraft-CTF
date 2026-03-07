package dev.tjxjnoobie.ctf.game.combat.scout.handlers;

import dev.tjxjnoobie.ctf.game.combat.scout.metadata.ScoutTaggerPlayerMetaData;
import dev.tjxjnoobie.ctf.game.combat.scout.util.ScoutTaggerStateUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;

/**
 * Owns in-memory scout ammo and throw timestamp state.
 */
public final class ScoutTaggerRuntimeHandler {
    private static final String LOG_PREFIX = "[ScoutTaggerRuntimeHandler] ";

    // == Runtime state ==
    private final Map<UUID, ScoutTaggerPlayerMetaData> playerStateById = new HashMap<>();
    private final Map<UUID, Long> lastThrowAtMsByPlayerId = new HashMap<>();

    // == Getters ==
    /**
     * Ensures a player has tracked runtime state.
     *
     * @param playerUUID Unique id of the player.
     * @param maxAmmo Maximum ammo capacity for the tracked player.
     * @return Mutable runtime state for the player.
     */
    public ScoutTaggerPlayerMetaData ensurePlayerState(UUID playerUUID, int maxAmmo) {
        ScoutTaggerPlayerMetaData existingState = playerStateById.get(playerUUID);
        ScoutTaggerPlayerMetaData ensuredState = ScoutTaggerStateUtil.ensureAmmoEntry(playerStateById, playerUUID, maxAmmo);
        if (existingState == null && playerUUID != null) {
            Bukkit.getLogger().info(LOG_PREFIX + "created state for " + playerUUID + " ammo=" + ensuredState.getAmmo());
        }
        return ensuredState;
    }

    /**
     * Returns the remaining cooldown for the tracked player.
     *
     * @param playerUUID Unique id of the player.
     * @param nowMs Current timestamp in milliseconds.
     * @param cooldownMs Cooldown window length in milliseconds.
     * @return Remaining cooldown in milliseconds.
     */
    public long getCooldownRemainingMs(UUID playerUUID, long nowMs, long cooldownMs) {
        Long lastThrowAtMs = lastThrowAtMsByPlayerId.get(playerUUID);
        return ScoutTaggerStateUtil.getCooldownRemainingMs(lastThrowAtMs, cooldownMs, nowMs);
    }

    /**
     * Returns the tracked player ids snapshot.
     *
     * @return Snapshot of tracked player ids.
     */
    public Set<UUID> getTrackedPlayerIds() {
        return new HashSet<>(playerStateById.keySet());
    }

    // == Utilities ==
    /**
     * Records a successful scout throw timestamp.
     *
     * @param playerUUID Unique id of the player.
     * @param nowMs Current timestamp in milliseconds.
     */
    public void recordThrow(UUID playerUUID, long nowMs) {
        if (playerUUID == null) {
            return;
        }
        lastThrowAtMsByPlayerId.put(playerUUID, Math.max(0L, nowMs));
        Bukkit.getLogger().info(LOG_PREFIX + "recorded throw for " + playerUUID + " at=" + Math.max(0L, nowMs));
    }

    /**
     * Clears a single player's tracked runtime state.
     *
     * @param playerUUID Unique id of the player.
     */
    public void clearPlayer(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        boolean hadAmmoState = playerStateById.containsKey(playerUUID);
        boolean hadCooldownState = lastThrowAtMsByPlayerId.containsKey(playerUUID);
        playerStateById.remove(playerUUID);
        lastThrowAtMsByPlayerId.remove(playerUUID);
        if (hadAmmoState || hadCooldownState) {
            Bukkit.getLogger().info(LOG_PREFIX + "cleared full state for " + playerUUID
                    + " ammoState=" + hadAmmoState
                    + " cooldownState=" + hadCooldownState);
        }
    }

    /**
     * Clears only the tracked cooldown timestamp for a player.
     *
     * @param playerUUID Unique id of the player.
     */
    public void clearCooldown(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        Long removed = lastThrowAtMsByPlayerId.remove(playerUUID);
        if (removed != null) {
            Bukkit.getLogger().info(LOG_PREFIX + "cleared cooldown for " + playerUUID + " lastThrowAt=" + removed);
        }
    }

    /**
     * Clears all tracked scout runtime state.
     */
    public void clearAll() {
        if (!playerStateById.isEmpty() || !lastThrowAtMsByPlayerId.isEmpty()) {
            Bukkit.getLogger().info(LOG_PREFIX + "clearing all scout runtime state"
                    + " players=" + playerStateById.size()
                    + " cooldowns=" + lastThrowAtMsByPlayerId.size());
        }
        playerStateById.clear();
        lastThrowAtMsByPlayerId.clear();
    }

    /**
     * Clears all tracked cooldown timestamps while preserving ammo state.
     */
    public void clearAllCooldowns() {
        if (!lastThrowAtMsByPlayerId.isEmpty()) {
            Bukkit.getLogger().info(LOG_PREFIX + "clearing all cooldowns count=" + lastThrowAtMsByPlayerId.size());
        }
        lastThrowAtMsByPlayerId.clear();
    }
}
