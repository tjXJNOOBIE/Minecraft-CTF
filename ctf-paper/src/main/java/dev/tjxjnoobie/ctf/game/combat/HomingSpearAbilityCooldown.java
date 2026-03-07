package dev.tjxjnoobie.ctf.game.combat;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PluginConfigDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearLockMetaData;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearShooterMetaData;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearRuntimeRegistry;
import dev.tjxjnoobie.ctf.game.combat.util.CombatNamespacedKeyFactory;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearInventoryUtils;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearProjectileUtil;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearStateUtil;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearTimerUtil;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.tasks.AbilityTaskOrchestrator;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

/**
 * Owns homing-spear cooldown state, projectile tracking, and action-bar refresh orchestration.
 */
public final class HomingSpearAbilityCooldown implements MessageAccess, BukkitMessageSender,
        PlayerDependencyAccess, PluginConfigDependencyAccess, TaskDependencyAccess {

    private static final String LOG_PREFIX = "[HomingSpearCombatHandler] ";
    private static final String FALLBACK_NAMESPACE = CTFKeys.coreFallbackNamespace();
    private static final int TOTAL_LIFETIME_TICKS = 100;
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int UPDATE_INTERVAL_TICKS = 1;
    private static final int SCAN_RANGE_BLOCKS = 20;
    private static final double LOCK_STEER_STRENGTH = 0.34;
    private static final double CRUISE_SPEED = 1.15;
    private static final double DROP_SLOW_MULTIPLIER = 0.25;
    private static final long RETURN_DELAY_MS = 5_000L;
    private static final int RETURN_DELAY_TICKS = 100;
    private static final long HIT_DISPLAY_MS = 3_300L;
    private static final String SPEAR_TICK_KEY = CTFKeys.uiSpearTickBusKey();
    private static final String ACTION_BAR_TICK_KEY = CTFKeys.uiSpearActionbarTickBusKey();
    private static final int ACTION_BAR_REFRESH_TICKS = 1;

    private final NamespacedKey spearKey;
    private final NamespacedKey shooterKey;
    private final NamespacedKey returnShooterKey;
    private final HomingSpearInventoryUtils homingSpearInventoryUtils;
    private final HomingSpearRuntimeRegistry runtimeRegistry;

    /**
     * Constructs a HomingSpearAbilityCooldown instance.
     *
     * @param homingSpearInventoryUtils Inventory helper for spear slot visuals and placeholders.
     * @param runtimeRegistry Runtime state store for tracked spears and shooter metadata.
     */
    public HomingSpearAbilityCooldown(HomingSpearInventoryUtils homingSpearInventoryUtils,
                                      HomingSpearRuntimeRegistry runtimeRegistry) {
        // Build persistent keys once so projectile metadata stays consistent across the runtime.
        this.spearKey = CombatNamespacedKeyFactory.create(
                CTFKeys.combatHomingSpearTag(),
                FALLBACK_NAMESPACE,
                this::getMainPlugin);
        this.shooterKey = CombatNamespacedKeyFactory.create(
                CTFKeys.combatHomingSpearShooterTag(),
                FALLBACK_NAMESPACE,
                this::getMainPlugin);
        this.returnShooterKey = CombatNamespacedKeyFactory.create(
                CTFKeys.combatReturningSpearShooterTag(),
                FALLBACK_NAMESPACE,
                this::getMainPlugin);
        this.homingSpearInventoryUtils = Objects.requireNonNull(homingSpearInventoryUtils, "homingSpearInventoryUtils");
        this.runtimeRegistry = Objects.requireNonNull(runtimeRegistry, "runtimeRegistry");
    }

    // == Query API ==
    /**
     * Returns the remaining homing-spear cooldown for the given shooter.
     *
     * @param shooterId Player id used to resolve tracked cooldown state.
     * @return Remaining cooldown in milliseconds, or {@code 0} when none is active.
     */
    public long getPlayerCooldownRemainingMs(UUID shooterId) {
        return HomingSpearTimerUtil.getCooldownRemainingMs(
                runtimeRegistry.getShooterState(shooterId),
                System.currentTimeMillis());
    }

    /**
     * Returns the most recent spear-lock metadata for the given victim.
     *
     * @param victimId Player id used to resolve lock attribution state.
     * @return Last recorded lock data, or {@code null} when none exists.
     */
    public SpearLockMetaData getLastLock(UUID victimId) {
        return runtimeRegistry.getLastLock(victimId);
    }

    /**
     * Returns whether the given trident belongs to the tracked homing-spear flow.
     *
     * @param trident Projectile to inspect.
     * @return {@code true} when the trident is tagged as a tracked spear.
     */
    public boolean isTrackedSpear(Trident trident) {
        return HomingSpearProjectileUtil.isTrackedSpear(trident, spearKey);
    }

    /**
     * Returns the shooter id stored on the tracked spear projectile.
     *
     * @param trident Projectile carrying shooter metadata.
     * @return Shooter uuid, or {@code null} when the metadata is missing.
     */
    public UUID getShooterId(Trident trident) {
        return HomingSpearProjectileUtil.getShooterId(trident, shooterKey);
    }

    // == Combat state API ==
    /**
     * Records a pending direct-hit spear strike for later damage resolution.
     *
     * @param spear Tracked spear projectile that hit something.
     * @param hitEntity Entity touched by the spear.
     */
    public void rememberDirectHit(Trident spear, Entity hitEntity) {
        if (!isTrackedSpear(spear) || !(hitEntity instanceof Player)) {
            return;
        }
        // Delay the damage confirmation until the combat event consumes the projectile hit.
        runtimeRegistry.rememberDirectHit(spear.getUniqueId());
    }

    /**
     * Consumes a pending direct-hit marker for the given spear entity.
     *
     * @param spearId Projectile id used to resolve pending damage state.
     * @return {@code true} when a pending hit marker existed and was removed.
     */
    public boolean consumePendingDirectHit(UUID spearId) {
        return runtimeRegistry.consumePendingDirectHit(spearId);
    }

    /**
     * Shows temporary hit feedback on the shooter action bar.
     *
     * @param shooterId Player id that should receive the hit feedback.
     * @param victimName Display name of the player that was hit.
     */
    public void setPlayerHitFeedback(UUID shooterId, String victimName) {
        SpearShooterMetaData shooterState = runtimeRegistry.getOrCreateShooterState(shooterId);
        if (shooterState == null) {
            return;
        }

        // Hold the hit banner long enough for the next few action-bar refreshes to surface it.
        shooterState.setHitUntilMs(System.currentTimeMillis() + HIT_DISPLAY_MS);
        shooterState.setHitTargetName(victimName);
        ensurePlayerActionBarTracking(shooterId);
    }

    /**
     * Clears all tracked combat state for the given shooter.
     *
     * @param shooterId Player id whose spear, cooldown visuals, and lock state should be reset.
     */
    public void clearPlayerCombatState(UUID shooterId) {
        if (shooterId == null) {
            return;
        }

        // Remove projectile state first so later visual cleanup does not race against active spear ticks.
        clearPlayerActiveSpear(shooterId);
        HomingSpearStateUtil.clearActionBarState(homingSpearInventoryUtils, runtimeRegistry, shooterId);
        runtimeRegistry.removeShooterState(shooterId);
        runtimeRegistry.clearLocksForShooter(shooterId);
    }

    /**
     * Clears all tracked homing-spear state for every shooter.
     */
    public void clearAllCombatState() {
        for (UUID shooterId : runtimeRegistry.getShooterIds()) {
            clearPlayerActiveSpear(shooterId);
            HomingSpearStateUtil.clearActionBarState(homingSpearInventoryUtils, runtimeRegistry, shooterId);
        }
        runtimeRegistry.clearAll();
        unregisterFastTickIfAvailable(SPEAR_TICK_KEY);
        unregisterFastTickIfAvailable(ACTION_BAR_TICK_KEY);
    }

    /**
     * Sets the cooldown expiry timestamp for the given shooter.
     *
     * @param shooterId Player id used to resolve shooter state.
     * @param cooldownUntilMs Absolute cooldown-expiry time in milliseconds.
     */
    public void setCooldownUntil(UUID shooterId, long cooldownUntilMs) {
        SpearShooterMetaData shooterState = runtimeRegistry.getOrCreateShooterState(shooterId);
        if (shooterState == null) {
            return;
        }
        shooterState.setCooldownUntilMs(cooldownUntilMs);
    }

    /**
     * Ensures action-bar refresh scheduling is active for the given shooter.
     *
     * @param shooterId Player id that should keep receiving action-bar updates.
     */
    public void ensurePlayerActionBarTracking(UUID shooterId) {
        registerActionBarRefresh(shooterId);
    }

    /**
     * Refreshes the tracked spear visuals immediately for the given shooter.
     *
     * @param shooterId Player id whose spear UI should be refreshed now.
     */
    public void refreshPlayerCombatUiNow(UUID shooterId) {
        if (shooterId == null) {
            return;
        }

        boolean keepRefreshing = HomingSpearStateUtil.refreshPlayerActionBar(
                shooterId,
                runtimeRegistry,
                homingSpearInventoryUtils,
                this::getMessageFormatted,
                this::sendActionBar);
        if (keepRefreshing) {
            registerActionBarRefresh(shooterId);
            return;
        }
        unregisterActionBarRefresh(shooterId);
    }

    // == Tracking entry points ==
    /**
     * Starts runtime tracking for a newly launched homing spear.
     *
     * @param shooter Player who launched the tracked spear.
     * @param trident Projectile entity to tag and follow.
     */
    public void startActiveSpearTracking(Player shooter, Trident trident) {
        if (shooter == null || trident == null) {
            return;
        }

        UUID shooterId = shooter.getUniqueId();
        // Normalize the projectile into the homing runtime before the ticker starts reading it.
        trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        trident.setGravity(false);
        trident.setVelocity(shooter.getLocation().getDirection().normalize().multiply(CRUISE_SPEED));
        HomingSpearProjectileUtil.markTrackedSpear(trident, spearKey, shooterKey, shooterId);

        SpearShooterMetaData shooterState = runtimeRegistry.getOrCreateShooterState(shooterId);
        shooterState.setActiveSpearEntityId(trident.getUniqueId());
        shooterState.setSpearTick(0);
        shooterState.setLockedTargetId(null);
        shooterState.setActiveUntilMs(System.currentTimeMillis() + (TOTAL_LIFETIME_TICKS * 50L));

        homingSpearInventoryUtils.setSpearPlaceholder(shooter);
        refreshPlayerCombatUiNow(shooterId);
        startSpearTick(shooterId);
    }

    /**
     * Starts the delayed return flow for a normal thrown spear that should snap back to the player.
     *
     * @param shooter Player who threw the spear.
     * @param spear Projectile that should be converted into the return state.
     */
    public void startThrownSpearReturn(Player shooter, Trident spear) {
        if (shooter == null || spear == null) {
            return;
        }

        spear.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        beginSpearReturn(shooter, spear);
    }

    /**
     * Clears the currently tracked active spear for the given shooter.
     *
     * @param shooterId Player id whose spear entity and return timers should be removed.
     */
    public void clearPlayerActiveSpear(UUID shooterId) {
        if (shooterId == null) {
            return;
        }

        // Stop fast-tick registrations before mutating runtime state so no stale tick reads survive.
        unregisterActionBarRefresh(shooterId);
        stopSpearTick(shooterId);
        HomingSpearStateUtil.clearReturnState(runtimeRegistry, shooterId, true, returnShooterKey);

        SpearShooterMetaData shooterState = runtimeRegistry.getShooterState(shooterId);
        if (shooterState != null) {
            UUID spearEntityId = shooterState.getActiveSpearEntityId();
            if (spearEntityId != null) {
                Entity spearEntity = Bukkit.getEntity(spearEntityId);
                if (spearEntity != null) {
                    spearEntity.remove();
                }
                runtimeRegistry.consumePendingDirectHit(spearEntityId);
                shooterState.setActiveSpearEntityId(null);
            }

            shooterState.setLockedTargetId(null);
            shooterState.setActiveUntilMs(0L);
            shooterState.setSpearTick(0);
        }

        homingSpearInventoryUtils.restoreSpearItem(shooterId);
    }

    // == Internal orchestration ==
    /**
     * Starts the delayed return placeholder flow for a tracked spear.
     *
     * @param shooter Player that should receive the spear back.
     * @param trident Projectile entering the return state.
     */
    private void beginSpearReturn(Player shooter, Trident trident) {
        UUID shooterId = shooter.getUniqueId();
        HomingSpearStateUtil.clearReturnState(runtimeRegistry, shooterId, true, returnShooterKey);

        // Retag the projectile so future cleanup can find return-state tridents by owner.
        HomingSpearProjectileUtil.markReturningSpear(trident, returnShooterKey, shooterId);

        SpearShooterMetaData shooterState = runtimeRegistry.getOrCreateShooterState(shooterId);
        shooterState.setReturnUntilMs(System.currentTimeMillis() + RETURN_DELAY_MS);

        homingSpearInventoryUtils.setReturnPlaceholder(shooter);
        refreshPlayerCombatUiNow(shooterId);
        sendDebugMessage(LOG_PREFIX + shooter.getName() + " threw spear, returning in 5s");

        Runnable returnTask = () -> {
            if (!trident.isDead()) {
                trident.remove();
            }
            homingSpearInventoryUtils.restoreSpearItem(shooterId);

            SpearShooterMetaData refreshedState = runtimeRegistry.getShooterState(shooterId);
            if (refreshedState != null) {
                refreshedState.setReturnUntilMs(0L);
            }

            HomingSpearStateUtil.clearReturnState(runtimeRegistry, shooterId, false, returnShooterKey);
            refreshPlayerCombatUiNow(shooterId);
            sendDebugMessage(LOG_PREFIX + shooter.getName() + " spear returned");
        };

        shooterState.setSpearReturnTimerTaskId(AbilityTaskOrchestrator.startLaterTaskId(
                shooterState.getSpearReturnTimerTaskId(),
                returnTask,
                RETURN_DELAY_TICKS));
    }

    /**
     * Registers the shooter on the fast spear-tick bus when needed.
     *
     * @param shooterId Player id whose active spear should be advanced each tick.
     */
    private void startSpearTick(UUID shooterId) {
        if (shooterId == null || !hasFastTickBus()) {
            return;
        }
        if (runtimeRegistry.addActiveSpearTicker(shooterId)) {
            registerFastTickIfAvailable(SPEAR_TICK_KEY, UPDATE_INTERVAL_TICKS, this::tickActiveSpears);
        }
    }

    /**
     * Unregisters the shooter from the fast spear-tick bus when no longer needed.
     *
     * @param shooterId Player id whose active spear tracking should stop.
     */
    private void stopSpearTick(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        if (!hasFastTickBus()) {
            runtimeRegistry.removeActiveSpearTicker(shooterId);
            return;
        }
        runtimeRegistry.removeActiveSpearTicker(shooterId);
        if (!runtimeRegistry.hasActiveSpearTickers()) {
            unregisterFastTickIfAvailable(SPEAR_TICK_KEY);
        }
    }

    /**
     * Registers action-bar refresh scheduling for the given shooter.
     *
     * @param shooterId Player id that should receive refresh ticks.
     */
    private void registerActionBarRefresh(UUID shooterId) {
        if (shooterId == null || !hasFastTickBus()) {
            return;
        }
        runtimeRegistry.getOrCreateShooterState(shooterId);
        if (runtimeRegistry.addActiveActionBar(shooterId)) {
            registerFastTickIfAvailable(ACTION_BAR_TICK_KEY, ACTION_BAR_REFRESH_TICKS, this::tickActionBars);
        }
    }

    /**
     * Unregisters action-bar refresh scheduling for the given shooter.
     *
     * @param shooterId Player id that should stop receiving refresh ticks.
     */
    private void unregisterActionBarRefresh(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        if (!hasFastTickBus()) {
            runtimeRegistry.removeActiveActionBar(shooterId);
            return;
        }
        runtimeRegistry.removeActiveActionBar(shooterId);
        if (!runtimeRegistry.hasActiveActionBars()) {
            unregisterFastTickIfAvailable(ACTION_BAR_TICK_KEY);
        }
    }

    /**
     * Advances every active tracked spear on the fast ticker.
     */
    private void tickActiveSpears() {
        if (!hasFastTickBus()) {
            return;
        }
        for (UUID shooterId : runtimeRegistry.getActiveSpearTickerIds()) {
            // Delegate projectile steering and lock acquisition to the state util.
            HomingSpearStateUtil.SpearTickResult tickResult = HomingSpearStateUtil.tickTrackedSpear(
                    shooterId,
                    runtimeRegistry,
                    this::teamGetTeamKey,
                    this::teamGetTeamPlayers,
                    TOTAL_LIFETIME_TICKS,
                    SCAN_INTERVAL_TICKS,
                    SCAN_RANGE_BLOCKS,
                    LOCK_STEER_STRENGTH,
                    DROP_SLOW_MULTIPLIER);
            if (tickResult.clearActiveSpear()) {
                clearPlayerActiveSpear(shooterId);
                continue;
            }
            if (tickResult.newlyLockedTarget() != null) {
                sendDebugMessage(LOG_PREFIX + tickResult.shooter().getName()
                        + " locked onto " + tickResult.newlyLockedTarget().getName());
            }
            if (tickResult.startReturn()) {
                beginSpearReturn(tickResult.shooter(), tickResult.trident());
                sendDebugMessage(LOG_PREFIX + tickResult.shooter().getName() + " spear returned");
            }
        }
        if (!runtimeRegistry.hasActiveSpearTickers()) {
            unregisterFastTickIfAvailable(SPEAR_TICK_KEY);
        }
    }

    /**
     * Refreshes every shooter action bar that is currently registered on the fast ticker.
     */
    private void tickActionBars() {
        if (!hasFastTickBus()) {
            return;
        }
        for (UUID shooterId : runtimeRegistry.getActiveActionBarIds()) {
            // The util owns message selection and visual refresh rules; the cooldown class owns scheduling.
            boolean keepRefreshing = HomingSpearStateUtil.refreshPlayerActionBar(
                    shooterId,
                    runtimeRegistry,
                    homingSpearInventoryUtils,
                    this::getMessageFormatted,
                    this::sendActionBar);
            if (!keepRefreshing) {
                unregisterActionBarRefresh(shooterId);
            }
        }
        if (!runtimeRegistry.hasActiveActionBars()) {
            unregisterFastTickIfAvailable(ACTION_BAR_TICK_KEY);
        }
    }
}
