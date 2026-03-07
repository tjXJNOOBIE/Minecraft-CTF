package dev.tjxjnoobie.ctf.game.combat.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.UiDependencyAccess;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearLockMetaData;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearInventoryUtils;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearTimerUtil;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.tasks.AbilityTaskOrchestrator;
import dev.tjxjnoobie.ctf.util.game.ArenaGuardUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Owns homing-spear activation, combat resolution, and death attribution.
 */
public final class HomingSpearAbilityHandler implements MessageAccess, BukkitMessageSender,
        FlagDependencyAccess, LifecycleDependencyAccess, PlayerDependencyAccess, UiDependencyAccess {

    private static final String LOG_PREFIX = "[HomingSpearAbilityHandler] ";
    private static final long COOLDOWN_MS = 30_000L;
    private static final double HIT_DAMAGE = 5.0;
    private static final double MELEE_DAMAGE = 1.0;
    private static final int DAMAGE_RETURN_DELAY_TICKS = 2;
    private static final int LOCK_ATTRIBUTION_WINDOW_MS = 5_000;
    private static final double INDIRECT_ATTRIBUTION_RANGE_SQUARED = 9.0;

    private final HomingSpearAbilityCooldown cooldown;
    private final HomingSpearInventoryUtils homingSpearInventoryUtils;
    private final HomingSpearItem homingSpearItem;
    private final Set<UUID> pendingHomingLaunchers = new HashSet<>();

    /**
     * Constructs a HomingSpearAbilityHandler instance.
     *
     * @param cooldown Runtime controller for spear cooldown, tracking, and placeholders.
     * @param homingSpearInventoryUtils Inventory helper for spear item matching and visuals.
     */
    public HomingSpearAbilityHandler(HomingSpearAbilityCooldown cooldown,
                                     HomingSpearInventoryUtils homingSpearInventoryUtils) {
        this.cooldown = cooldown;
        this.homingSpearInventoryUtils = homingSpearInventoryUtils;

        HomingSpearItem resolvedSpearItem = getHomingSpearItem();
        this.homingSpearItem = resolvedSpearItem != null ? resolvedSpearItem : HomingSpearItem.INSTANCE;
    }

    /**
     * Attempts to activate the player's homing-spear ability.
     *
     * @param shooter Player attempting to throw a homing spear.
     * @return {@code true} when the spear ability was activated successfully.
     */
    public boolean tryActivateSpearAbility(Player shooter) {
        if (!ArenaGuardUtil.isPlayerInArena(shooter) || !homingSpearInventoryUtils.isHoldingSpear(shooter)) {
            return false;
        }

        if (!ArenaGuardUtil.isMatchInProgressOrOvertime(getGameStateManager().getGameState())) {
            sendActionBar(shooter, Component.text("Spear is disabled in lobby.", NamedTextColor.RED));
            sendDebugMessage(LOG_PREFIX + shooter.getName() + " spear blocked (state=" + getGameStateManager().getGameState().name() + ")");
            return false;
        }

        UUID shooterId = shooter.getUniqueId();
        if (getFlagCarrierStateHandler().isFlagCarrier(shooterId)) {
            sendActionBar(shooter, getMessage(CTFKeys.uiSpearBlockedActionbarKey()));
            sendDebugMessage(LOG_PREFIX + shooter.getName() + " spear blocked (carrying_flag)");
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldownRemainingMs = cooldown.getPlayerCooldownRemainingMs(shooterId);
        if (cooldownRemainingMs > 0L) {
            // Keep action-bar updates alive so the player sees the remaining cooldown while blocked.
            cooldown.ensurePlayerActionBarTracking(shooterId);
            sendDebugMessage(LOG_PREFIX + shooter.getName() + " spear blocked (cooldown)");
            return false;
        }

        cooldown.clearPlayerActiveSpear(shooterId);
        launchTrackedSpearForPlayer(shooter);
        cooldown.setCooldownUntil(shooterId, now + COOLDOWN_MS);
        cooldown.refreshPlayerCombatUiNow(shooterId);
        sendDebugMessage(LOG_PREFIX + shooter.getName() + " F-threw Homing Spear");
        return true;
    }

    /**
     * Returns whether the player's swap-hand action should be intercepted for spear activation.
     *
     * @param shooter Player attempting to swap items.
     * @return {@code true} when the swap should be consumed by the spear ability.
     */
    public boolean shouldInterceptPlayerSwap(Player shooter) {
        return ArenaGuardUtil.isPlayerInArena(shooter) && homingSpearInventoryUtils.isHoldingSpear(shooter);
    }

    /**
     * Handles a trident launch event that may belong to the homing-spear flow.
     *
     * @param shooter Player who launched the projectile.
     * @param spear Trident projectile that was launched.
     */
    public void handleThrownSpearLaunch(Player shooter, Trident spear) {
        if (shooter == null || spear == null || !getMatchPlayerSessionHandler().isPlayerInArena(shooter)) {
            return;
        }

        UUID shooterId = shooter.getUniqueId();
        // Ignore the synthetic launch fired by the active-ability flow, and only catch normal throws here.
        if (pendingHomingLaunchers.contains(shooterId) || cooldown.isTrackedSpear(spear)) {
            return;
        }

        if (!homingSpearItem.matches(spear.getItemStack())) {
            return;
        }

        cooldown.startThrownSpearReturn(shooter, spear);
    }

    /**
     * Records that a tracked spear projectile hit a player directly.
     *
     * @param spear Projectile that hit the victim.
     * @param hitEntity Entity touched by the spear.
     */
    public void recordTrackedSpearHit(Trident spear, Entity hitEntity) {
        cooldown.rememberDirectHit(spear, hitEntity);
    }

    /**
     * Applies the spear melee damage override for close-range hits.
     *
     * @param attacker Player using the spear item as a melee weapon.
     * @param event Damage event to mutate.
     */
    public void applySpearMeleeDamage(Player attacker, EntityDamageByEntityEvent event) {
        if (attacker == null || event == null || !getMatchPlayerSessionHandler().isPlayerInArena(attacker)) {
            return;
        }

        if (homingSpearItem.matches(attacker.getInventory().getItemInMainHand())) {
            event.setDamage(MELEE_DAMAGE);
        }
    }

    /**
     * Applies tracked-spear projectile damage when the projectile hit is confirmed.
     *
     * @param spear Projectile that hit the victim.
     * @param victim Player damaged by the projectile.
     * @param event Damage event to mutate.
     */
    public void applyTrackedSpearProjectileDamage(Trident spear,
                                                  Player victim,
                                                  EntityDamageByEntityEvent event) {
        if (spear == null || victim == null || event == null || !cooldown.isTrackedSpear(spear)) {
            return;
        }

        UUID shooterId = cooldown.getShooterId(spear);
        if (shooterId == null) {
            return;
        }

        Player shooter = HomingSpearTimerUtil.resolveOnlinePlayer(shooterId);
        if (shooter == null) {
            return;
        }

        if (teamIsFriendlyFire(shooter, victim)) {
            event.setCancelled(true);
            return;
        }

        // The projectile-hit event marks the direct strike first; reject damage events that arrive without that mark.
        if (!cooldown.consumePendingDirectHit(spear.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(HIT_DAMAGE);
        cooldown.setPlayerHitFeedback(shooterId, victim.getName());
        AbilityTaskOrchestrator.startLater(null, () -> cooldown.clearPlayerActiveSpear(shooterId), DAMAGE_RETURN_DELAY_TICKS);
    }

    /**
     * Resolves death attribution for direct and indirect tracked-spear kills.
     *
     * @param victim Player whose death is being resolved.
     * @param cause Damage cause used for indirect-attribution rules.
     * @param directDamager Direct Bukkit damager for the event, when present.
     * @return Resolved death-attribution metadata, or {@code null} when none applies.
     */
    public SpearDeathAttributionMetaData resolveTrackedSpearAttribution(Player victim,
                                                                        EntityDamageEvent.DamageCause cause,
                                                                        Entity directDamager) {
        if (victim == null) {
            return null;
        }

        if (directDamager instanceof Trident trident && cooldown.isTrackedSpear(trident)) {
            UUID shooterId = cooldown.getShooterId(trident);
            Player shooter = HomingSpearTimerUtil.resolveOnlinePlayer(shooterId);
            if (shooter != null) {
                Component message = victim.displayName()
                        .append(Component.text(" was impaled by ", NamedTextColor.GRAY))
                        .append(shooter.displayName())
                        .append(Component.text("'s Homing Spear.", NamedTextColor.GRAY));
                return new SpearDeathAttributionMetaData(message, shooter.getUniqueId());
            }
        }

        // Fall back to the recent lock window when the victim dies near the tracked spear after fleeing it.
        SpearLockMetaData lockInfo = cooldown.getLastLock(victim.getUniqueId());
        if (lockInfo == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        boolean expired = now - lockInfo.lockedAtMs() > LOCK_ATTRIBUTION_WINDOW_MS;
        if (expired || !HomingSpearTimerUtil.isIndirectAttributionCause(cause)) {
            return null;
        }

        UUID spearEntityId = lockInfo.spearEntityId();
        if (spearEntityId == null) {
            return null;
        }

        Entity spearEntity = Bukkit.getEntity(spearEntityId);
        if (!(spearEntity instanceof Trident trackedSpear) || trackedSpear.isDead()) {
            return null;
        }

        if (!HomingSpearTimerUtil.sameWorld(victim.getLocation(), trackedSpear.getLocation())) {
            return null;
        }

        boolean tooFar = victim.getLocation().distanceSquared(trackedSpear.getLocation())
                > INDIRECT_ATTRIBUTION_RANGE_SQUARED;
        if (tooFar) {
            return null;
        }

        Player shooter = HomingSpearTimerUtil.resolveOnlinePlayer(lockInfo.shooterId());
        if (shooter == null) {
            return null;
        }

        sendDebugMessage(LOG_PREFIX + victim.getName() + " indirect death attributed to " + shooter.getName()
                + " (cause=" + cause.name() + ")");

        Component message = victim.displayName()
                .append(Component.text(" tried escaping ", NamedTextColor.GRAY))
                .append(shooter.displayName())
                .append(Component.text("'s Homing Spear and died anyway.", NamedTextColor.GRAY));
        return new SpearDeathAttributionMetaData(message, shooter.getUniqueId());
    }

    /**
     * Clears tracked homing-spear state when a player leaves the server.
     *
     * @param player Player leaving the match.
     */
    public void handlePlayerQuit(Player player) {
        if (player == null) {
            return;
        }

        UUID shooterId = player.getUniqueId();
        pendingHomingLaunchers.remove(shooterId);
        cooldown.clearPlayerCombatState(shooterId);
    }

    /**
     * Launches the synthetic tracked spear for the active ability.
     *
     * @param shooter Player using the active spear ability.
     */
    private void launchTrackedSpearForPlayer(Player shooter) {
        UUID shooterId = shooter.getUniqueId();
        // Mark the shooter so the ordinary projectile-launch hook does not treat this launch as a normal throw.
        pendingHomingLaunchers.add(shooterId);
        Trident trident;
        try {
            trident = shooter.launchProjectile(Trident.class);
        } finally {
            pendingHomingLaunchers.remove(shooterId);
        }

        cooldown.startActiveSpearTracking(shooter, trident);
    }
}
