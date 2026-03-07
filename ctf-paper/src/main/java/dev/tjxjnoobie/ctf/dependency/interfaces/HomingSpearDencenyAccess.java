package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearTimerUtil;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Shared dependency-access facade for the homing-spear combat domain.
 */
public interface HomingSpearDencenyAccess {
    /**
     * Returns the main homing-spear ability handler.
     *
     * @return Registered homing-spear ability handler instance.
     */
    default HomingSpearAbilityHandler getHomingSpearAbilityService() {
        return DependencyLoaderAccess.findInstance(HomingSpearAbilityHandler.class);
    }

    /**
     * Returns the homing-spear cooldown runtime controller.
     *
     * @return Registered homing-spear cooldown instance.
     */
    default HomingSpearAbilityCooldown getHomingSpearAbilityCooldown() {
        return DependencyLoaderAccess.findInstance(HomingSpearAbilityCooldown.class);
    }

    default boolean homingSpearTryActivate(Player shooter) {
        return getHomingSpearAbilityService().tryActivateSpearAbility(shooter);
    }

    default boolean homingSpearShouldInterceptSwap(Player shooter) {
        return getHomingSpearAbilityService().shouldInterceptPlayerSwap(shooter);
    }

    default void homingSpearHandleThrownSpearLaunch(Player shooter, Trident spear) {
        getHomingSpearAbilityService().handleThrownSpearLaunch(shooter, spear);
    }

    default void homingSpearRecordTrackedSpearHit(Trident spear, Entity hitEntity) {
        getHomingSpearAbilityService().recordTrackedSpearHit(spear, hitEntity);
    }

    default void homingSpearApplyMeleeDamage(Player attacker, EntityDamageByEntityEvent event) {
        getHomingSpearAbilityService().applySpearMeleeDamage(attacker, event);
    }

    default void homingSpearApplyTrackedProjectileDamage(Trident spear,
                                                         Player victim,
                                                         EntityDamageByEntityEvent event) {
        getHomingSpearAbilityService().applyTrackedSpearProjectileDamage(spear, victim, event);
    }

    default SpearDeathAttributionMetaData homingSpearResolveTrackedAttribution(Player victim,
                                                                               EntityDamageEvent.DamageCause cause,
                                                                               Entity directDamager) {
        return getHomingSpearAbilityService().resolveTrackedSpearAttribution(victim, cause, directDamager);
    }

    default void homingSpearHandlePlayerQuit(Player player) {
        getHomingSpearAbilityService().handlePlayerQuit(player);
    }

    default void homingSpearClearPlayerActiveSpear(UUID shooterId) {
        getHomingSpearAbilityCooldown().clearPlayerActiveSpear(shooterId);
    }

    default void homingSpearClearPlayerCombatState(UUID shooterId) {
        getHomingSpearAbilityCooldown().clearPlayerCombatState(shooterId);
    }

    default void homingSpearClearAllCombatState() {
        getHomingSpearAbilityCooldown().clearAllCombatState();
    }

    default String homingSpearFormatPlayerCooldownTenths(long remainingMs) {
        return HomingSpearTimerUtil.formatTenths(remainingMs);
    }

    default long homingSpearPlayerCooldownRemainingMs(UUID shooterId) {
        return getHomingSpearAbilityCooldown().getPlayerCooldownRemainingMs(shooterId);
    }
}
