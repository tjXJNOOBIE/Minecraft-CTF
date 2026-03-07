package dev.tjxjnoobie.ctf.game.combat.util;

import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearRuntimeRegistry;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearLockMetaData;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearShooterMetaData;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.tasks.AbilityTaskOrchestrator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.util.Vector;

/**
 * Shared runtime-state helpers for homing-spear timer orchestration.
 */
public final class HomingSpearStateUtil {
    /**
     * Snapshot of the work produced by a single tracked-spear tick.
     *
     * @param shooter Shooter whose spear was advanced.
     * @param trident Trident that was advanced this tick.
     * @param newlyLockedTarget Target that was newly acquired this tick, when present.
     * @param clearActiveSpear Whether the caller should clear the tracked spear entirely.
     * @param startReturn Whether the caller should begin the spear return flow.
     */
    public record SpearTickResult(Player shooter,
                                  Trident trident,
                                  Player newlyLockedTarget,
                                  boolean clearActiveSpear,
                                  boolean startReturn) {
    }

    private HomingSpearStateUtil() {
    }

    /**
     * Clears any scheduled return state for the given shooter.
     *
     * @param runtimeRegistry Runtime registry that owns shooter state.
     * @param shooterId Shooter whose return state should be cleared.
     * @param removeProjectile Whether tracked return-state tridents should also be removed.
     * @param returnShooterKey Persistent-data key used for return-state ownership.
     */
    public static void clearReturnState(HomingSpearRuntimeRegistry runtimeRegistry,
                                        UUID shooterId,
                                        boolean removeProjectile,
                                        NamespacedKey returnShooterKey) {
        if (runtimeRegistry == null || shooterId == null) {
            return;
        }

        SpearShooterMetaData shooterState = runtimeRegistry.getShooterState(shooterId);
        if (shooterState != null) {
            shooterState.setSpearReturnTimerTaskId(
                    AbilityTaskOrchestrator.cancelTaskId(shooterState.getSpearReturnTimerTaskId()));
            shooterState.setReturnUntilMs(0L);
        }

        if (removeProjectile) {
            HomingSpearProjectileUtil.removeReturningTridents(shooterId, returnShooterKey);
        }
    }

    /**
     * Finds the first enemy player in range for the given shooter.
     *
     * @param shooter Shooter trying to acquire a target.
     * @param spearLocation Current spear location.
     * @param teamKeyResolver Resolver used to find the shooter team.
     * @param teamPlayersResolver Resolver used to list team members by team key.
     * @param scanRangeBlocks Maximum scan range in blocks.
     * @return First enemy player in range, or {@code null} when none qualify.
     */
    public static Player findFirstEnemyForShooter(Player shooter,
                                                  Location spearLocation,
                                                  Function<Player, String> teamKeyResolver,
                                                  Function<String, List<Player>> teamPlayersResolver,
                                                  int scanRangeBlocks) {
        if (shooter == null || spearLocation == null || teamKeyResolver == null || teamPlayersResolver == null) {
            return null;
        }

        String shooterTeam = teamKeyResolver.apply(shooter);
        if (shooterTeam == null) {
            return null;
        }

        String enemyTeam = TeamManager.RED.equals(shooterTeam) ? TeamManager.BLUE : TeamManager.RED;
        List<Player> candidates = teamPlayersResolver.apply(enemyTeam);
        return HomingSpearTimerUtil.findFirstEnemyInRange(spearLocation, candidates, scanRangeBlocks);
    }

    /**
     * Clears action-bar and cooldown visual state for the given shooter.
     *
     * @param homingSpearInventoryUtils Inventory helper for restoring spear display names.
     * @param runtimeRegistry Runtime registry that owns shooter state.
     * @param shooterId Shooter whose action-bar state should be reset.
     */
    public static void clearActionBarState(HomingSpearInventoryUtils homingSpearInventoryUtils,
                                           HomingSpearRuntimeRegistry runtimeRegistry,
                                           UUID shooterId) {
        if (homingSpearInventoryUtils == null || runtimeRegistry == null || shooterId == null) {
            return;
        }

        homingSpearInventoryUtils.restoreSpearDisplayName(shooterId);
        SpearShooterMetaData shooterState = runtimeRegistry.getShooterState(shooterId);
        if (shooterState == null) {
            return;
        }

        shooterState.setHitUntilMs(0L);
        shooterState.setHitTargetName(null);
        shooterState.setActiveUntilMs(0L);
        shooterState.setReturnUntilMs(0L);
        shooterState.setCooldownUntilMs(0L);
    }

    /**
     * Advances one tracked spear tick and returns the resulting orchestration actions.
     *
     * @param shooterId Shooter whose tracked spear should be advanced.
     * @param runtimeRegistry Runtime registry that owns shooter and lock state.
     * @param teamKeyResolver Resolver used to find the shooter team.
     * @param teamPlayersResolver Resolver used to list enemy candidates.
     * @param totalLifetimeTicks Maximum tracked lifetime before return starts.
     * @param scanIntervalTicks Tick interval used for target reacquisition scans.
     * @param scanRangeBlocks Maximum targeting range in blocks.
     * @param lockSteerStrength Steering factor applied toward locked targets.
     * @param dropSlowMultiplier Velocity multiplier used when the spear loses a target or expires.
     * @return Tick result describing the state changes the caller should apply.
     */
    public static SpearTickResult tickTrackedSpear(UUID shooterId,
                                                   HomingSpearRuntimeRegistry runtimeRegistry,
                                                   Function<Player, String> teamKeyResolver,
                                                   Function<String, List<Player>> teamPlayersResolver,
                                                   int totalLifetimeTicks,
                                                   int scanIntervalTicks,
                                                   int scanRangeBlocks,
                                                   double lockSteerStrength,
                                                   double dropSlowMultiplier) {
        Player shooter = HomingSpearTimerUtil.resolveOnlinePlayer(shooterId);
        SpearShooterMetaData shooterState = runtimeRegistry == null ? null : runtimeRegistry.getShooterState(shooterId);
        Trident trident = shooterState == null ? null : HomingSpearTimerUtil.resolveActiveTrident(shooterState);
        if (shooter == null || shooterState == null || trident == null) {
            return new SpearTickResult(null, null, null, true, false);
        }

        int tick = shooterState.getSpearTick() + 1;
        shooterState.setSpearTick(tick);

        UUID lockedTargetId = shooterState.getLockedTargetId();
        Player lockedTarget = HomingSpearTimerUtil.resolveOnlinePlayer(lockedTargetId);
        Location tridentLocation = trident.getLocation();
        Location lockedTargetLocation = lockedTarget == null ? null : lockedTarget.getLocation();
        boolean sameWorld = lockedTarget != null && HomingSpearTimerUtil.sameWorld(tridentLocation, lockedTargetLocation);
        if (sameWorld) {
            // Blend a steering impulse toward the target with the existing projectile momentum.
            Vector toTarget = lockedTarget.getLocation().add(0.0, 1.1, 0.0).toVector()
                    .subtract(trident.getLocation().toVector());
            if (toTarget.lengthSquared() > 0.0001) {
                trident.setVelocity(toTarget.normalize().multiply(lockSteerStrength)
                        .add(trident.getVelocity().multiply(0.62)));
            }
        } else if (lockedTargetId != null) {
            shooterState.setLockedTargetId(null);
            trident.setGravity(true);
            trident.setVelocity(trident.getVelocity().multiply(dropSlowMultiplier));
        }

        Player newlyLockedTarget = null;
        boolean noLockedTarget = shooterState.getLockedTargetId() == null;
        boolean scanTick = tick % scanIntervalTicks == 0;
        if (noLockedTarget && scanTick) {
            Player target = findFirstEnemyForShooter(
                    shooter,
                    trident.getLocation(),
                    teamKeyResolver,
                    teamPlayersResolver,
                    scanRangeBlocks);
            if (target != null) {
                shooterState.setLockedTargetId(target.getUniqueId());
                runtimeRegistry.rememberLock(
                        target.getUniqueId(),
                        new SpearLockMetaData(shooterId, System.currentTimeMillis(), trident.getUniqueId()));
                newlyLockedTarget = target;
            }
        }

        if (tick >= totalLifetimeTicks) {
            trident.setGravity(true);
            trident.setVelocity(trident.getVelocity().multiply(dropSlowMultiplier));
            return new SpearTickResult(shooter, trident, newlyLockedTarget, false, true);
        }

        return new SpearTickResult(shooter, trident, newlyLockedTarget, false, false);
    }

    /**
     * Refreshes the shooter's spear action bar and item visuals.
     *
     * @param shooterId Shooter whose UI should be refreshed.
     * @param runtimeRegistry Runtime registry that owns shooter state.
     * @param homingSpearInventoryUtils Inventory helper for spear slot visuals.
     * @param formattedMessageResolver Resolver used to render configured action-bar messages.
     * @param actionBarSender Consumer used to send the final action-bar component.
     * @return {@code true} when the caller should keep refreshing the action bar.
     */
    public static boolean refreshPlayerActionBar(UUID shooterId,
                                                 HomingSpearRuntimeRegistry runtimeRegistry,
                                                 HomingSpearInventoryUtils homingSpearInventoryUtils,
                                                 BiFunction<String, String, Component> formattedMessageResolver,
                                                 BiConsumer<Player, Component> actionBarSender) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null || !shooter.isOnline()) {
            return false;
        }

        SpearShooterMetaData shooterState = runtimeRegistry == null ? null : runtimeRegistry.getShooterState(shooterId);
        if (shooterState == null || formattedMessageResolver == null || actionBarSender == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long hitUntil = shooterState.getHitUntilMs();
        if (hitUntil > 0L && hitUntil <= now) {
            shooterState.setHitUntilMs(0L);
            shooterState.setHitTargetName(null);
            hitUntil = 0L;
        }

        long returnUntil = Math.max(shooterState.getActiveUntilMs(), shooterState.getReturnUntilMs());
        long returnRemainingMs = HomingSpearTimerUtil.getRemainingMsFromExpiry(returnUntil, now);
        long cooldownRemainingMs = HomingSpearTimerUtil.getCooldownRemainingMs(shooterState, now);
        // Keep the spear slot name in sync with whichever state currently owns the player-facing cooldown text.
        homingSpearInventoryUtils.refreshSpearItemVisual(
                shooter,
                cooldownRemainingMs,
                returnRemainingMs,
                HomingSpearTimerUtil::formatTenths);

        if (hitUntil > 0L) {
            String target = shooterState.getHitTargetName() == null ? "Target" : shooterState.getHitTargetName();
            actionBarSender.accept(shooter, formattedMessageResolver.apply(CTFKeys.uiSpearHitActionbarKey(), target));
            return true;
        }

        Component statusMessage = null;
        UUID lockedTargetId = shooterState.getLockedTargetId();
        if (lockedTargetId != null) {
            Player lockedTarget = Bukkit.getPlayer(lockedTargetId);
            if (lockedTarget != null && lockedTarget.isOnline()) {
                statusMessage = formattedMessageResolver.apply(CTFKeys.uiSpearLockedActionbarKey(), lockedTarget.getName());
            } else {
                shooterState.setLockedTargetId(null);
            }
        }

        if (statusMessage == null && returnRemainingMs > 0L) {
            statusMessage = formattedMessageResolver.apply(
                    CTFKeys.uiSpearReturningActionbarKey(),
                    HomingSpearTimerUtil.formatTenths(returnRemainingMs));
        }

        if (statusMessage != null) {
            actionBarSender.accept(shooter, statusMessage);
            return true;
        }

        if (cooldownRemainingMs > 0L) {
            actionBarSender.accept(shooter, Component.empty());
            return true;
        }

        return returnRemainingMs > 0L;
    }
}
