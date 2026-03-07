package dev.tjxjnoobie.ctf.game.combat.handlers;

import dev.tjxjnoobie.ctf.game.combat.metadata.SpearLockMetaData;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearShooterMetaData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns mutable homing-spear runtime state for shooters and ticker membership.
 */
public final class HomingSpearRuntimeRegistry {
    private final Map<UUID, SpearShooterMetaData> shooterStateById = new HashMap<>();
    private final Map<UUID, SpearLockMetaData> lastSpearLockByVictim = new HashMap<>();
    private final Set<UUID> activeSpearTickers = new HashSet<>();
    private final Set<UUID> activeActionBars = new HashSet<>();
    private final Set<UUID> pendingDirectHits = new HashSet<>();

    /**
     * Returns the shooter state for the given player, creating it when absent.
     *
     * @param shooterId Player id used to resolve runtime state.
     * @return Existing or newly created shooter state, or {@code null} for invalid input.
     */
    public SpearShooterMetaData getOrCreateShooterState(UUID shooterId) {
        if (shooterId == null) {
            return null;
        }
        return shooterStateById.computeIfAbsent(shooterId, ignored -> new SpearShooterMetaData());
    }

    /**
     * Returns the shooter state for the given player.
     *
     * @param shooterId Player id used to resolve runtime state.
     * @return Stored shooter state, or {@code null} when none exists.
     */
    public SpearShooterMetaData getShooterState(UUID shooterId) {
        return shooterId == null ? null : shooterStateById.get(shooterId);
    }

    /**
     * Returns a snapshot of all shooter ids currently stored in the registry.
     *
     * @return Copy of the registered shooter ids.
     */
    public Set<UUID> getShooterIds() {
        return new HashSet<>(shooterStateById.keySet());
    }

    /**
     * Removes the stored shooter state for the given player.
     *
     * @param shooterId Player id whose runtime state should be discarded.
     */
    public void removeShooterState(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        shooterStateById.remove(shooterId);
    }

    /**
     * Returns the last recorded spear lock for the given victim.
     *
     * @param victimId Player id used to resolve last-lock state.
     * @return Stored lock metadata, or {@code null} when none exists.
     */
    public SpearLockMetaData getLastLock(UUID victimId) {
        return victimId == null ? null : lastSpearLockByVictim.get(victimId);
    }

    /**
     * Stores the latest lock metadata for the given victim.
     *
     * @param victimId Victim player id.
     * @param lockMetaData Lock metadata to remember for attribution.
     */
    public void rememberLock(UUID victimId, SpearLockMetaData lockMetaData) {
        if (victimId == null || lockMetaData == null) {
            return;
        }
        lastSpearLockByVictim.put(victimId, lockMetaData);
    }

    /**
     * Removes all remembered locks created by the given shooter.
     *
     * @param shooterId Player id whose lock attribution should be cleared.
     */
    public void clearLocksForShooter(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        lastSpearLockByVictim.entrySet().removeIf(entry -> shooterId.equals(entry.getValue().shooterId()));
    }

    /**
     * Adds the shooter to the active spear ticker set.
     *
     * @param shooterId Player id to register.
     * @return {@code true} when the shooter was newly added.
     */
    public boolean addActiveSpearTicker(UUID shooterId) {
        return shooterId != null && activeSpearTickers.add(shooterId);
    }

    /**
     * Removes the shooter from the active spear ticker set.
     *
     * @param shooterId Player id to unregister.
     */
    public void removeActiveSpearTicker(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        activeSpearTickers.remove(shooterId);
    }

    /**
     * Returns a snapshot of shooter ids with active spear ticks.
     *
     * @return Copy of active spear ticker ids.
     */
    public Set<UUID> getActiveSpearTickerIds() {
        return new HashSet<>(activeSpearTickers);
    }

    /**
     * Returns whether any spear tickers are currently active.
     *
     * @return {@code true} when at least one shooter is registered for spear ticks.
     */
    public boolean hasActiveSpearTickers() {
        return !activeSpearTickers.isEmpty();
    }

    /**
     * Adds the shooter to the active action-bar refresh set.
     *
     * @param shooterId Player id to register.
     * @return {@code true} when the shooter was newly added.
     */
    public boolean addActiveActionBar(UUID shooterId) {
        return shooterId != null && activeActionBars.add(shooterId);
    }

    /**
     * Removes the shooter from the active action-bar refresh set.
     *
     * @param shooterId Player id to unregister.
     */
    public void removeActiveActionBar(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        activeActionBars.remove(shooterId);
    }

    /**
     * Returns a snapshot of shooter ids with active action-bar refreshes.
     *
     * @return Copy of active action-bar ids.
     */
    public Set<UUID> getActiveActionBarIds() {
        return new HashSet<>(activeActionBars);
    }

    /**
     * Returns whether any action-bar refresh registrations are active.
     *
     * @return {@code true} when at least one shooter is registered.
     */
    public boolean hasActiveActionBars() {
        return !activeActionBars.isEmpty();
    }

    /**
     * Marks the spear as having landed a direct hit that still needs damage confirmation.
     *
     * @param spearId Projectile id to track.
     */
    public void rememberDirectHit(UUID spearId) {
        if (spearId == null) {
            return;
        }
        pendingDirectHits.add(spearId);
    }

    /**
     * Consumes a pending direct-hit marker for the given spear.
     *
     * @param spearId Projectile id to resolve.
     * @return {@code true} when a pending hit existed and was removed.
     */
    public boolean consumePendingDirectHit(UUID spearId) {
        return spearId != null && pendingDirectHits.remove(spearId);
    }

    /**
     * Clears every tracked homing-spear runtime collection.
     */
    public void clearAll() {
        shooterStateById.clear();
        lastSpearLockByVictim.clear();
        activeSpearTickers.clear();
        activeActionBars.clear();
        pendingDirectHits.clear();
    }
}
