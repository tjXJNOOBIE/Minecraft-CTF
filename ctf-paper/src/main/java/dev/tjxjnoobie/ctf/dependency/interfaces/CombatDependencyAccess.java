package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.combat.handlers.CombatDamageRestrictionHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.ScoutTaggerAbilityEvents;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerCooldownHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerGlowHandler;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface CombatDependencyAccess extends HomingSpearDencenyAccess {
    default ScoutTaggerAbility getScoutTaggerAbility() { return DependencyLoaderAccess.findInstance(ScoutTaggerAbility.class); }
    default ScoutTaggerAbilityEvents getScoutTaggerAbilityEvents() { return DependencyLoaderAccess.findInstance(ScoutTaggerAbilityEvents.class); }
    default ScoutTaggerCooldownHandler getScoutTaggerCooldownHandler() { return DependencyLoaderAccess.findInstance(ScoutTaggerCooldownHandler.class); }
    default ScoutTaggerGlowHandler getScoutTaggerGlowHandler() { return DependencyLoaderAccess.findInstance(ScoutTaggerGlowHandler.class); }
    default CombatDamageRestrictionHandler getCombatDamageRestrictionHandler() { return DependencyLoaderAccess.findInstance(CombatDamageRestrictionHandler.class); }

    default void scoutStartForMatch() {
        getScoutTaggerAbility().startForMatch();
    }

    default void scoutStopAll() {
        getScoutTaggerAbility().stopAll();
    }

    default boolean scoutTryThrowSnowball(Player player) {
        return getScoutTaggerAbility().tryThrowScoutSnowball(player);
    }

    default void scoutProcessRespawnState(Player player) {
        getScoutTaggerAbility().processRespawnState(player);
    }

    default void scoutProcessPlayerQuitCleanup(Player player) {
        getScoutTaggerAbility().processPlayerQuitCleanup(player);
    }

    default void scoutProcessPlayerLeaveArena(Player player) {
        getScoutTaggerAbility().processPlayerLeaveArena(player);
    }

    default long scoutGetCooldownRemainingMs(UUID playerId) {
        return getScoutTaggerAbility().getCooldownRemainingMs(playerId);
    }

    default String scoutFormatCooldownTenths(long remainingMs) {
        return getScoutTaggerAbility().formatCooldownTenths(remainingMs);
    }

    default void scoutHandleProjectileHit(ProjectileHitEvent event) {
        getScoutTaggerAbilityEvents().onProjectileHit(event);
    }

    default void scoutHandlePlayerInteract(PlayerInteractEvent event) {
        getScoutTaggerAbilityEvents().onPlayerInteract(event);
    }

    default void scoutHandleEntityDamageByEntity(EntityDamageByEntityEvent event) {
        getScoutTaggerAbilityEvents().onEntityDamageByEntity(event);
    }

    default void scoutCooldownStart(UUID playerId, long cooldownMs, long nowMs) {
        getScoutTaggerCooldownHandler().startCooldown(playerId, cooldownMs, nowMs);
    }

    default long scoutCooldownRemainingMs(UUID playerId, long nowMs) {
        return getScoutTaggerCooldownHandler().getCooldownRemainingMs(playerId, nowMs);
    }

    default void scoutCooldownClear(UUID playerId, boolean restoreName) {
        getScoutTaggerCooldownHandler().clearCooldown(playerId, restoreName);
    }

    default void scoutCooldownClearAll() {
        getScoutTaggerCooldownHandler().clearAllCooldowns();
    }

    default void scoutCooldownRefreshVisuals() {
        getScoutTaggerCooldownHandler().refreshCooldownVisuals();
    }

    default void scoutGlowApply(Player target) {
        getScoutTaggerGlowHandler().applyGlow(target);
    }

    default void scoutGlowClear(UUID targetId) {
        getScoutTaggerGlowHandler().clearGlow(targetId);
    }

    default void scoutGlowStopAll() {
        getScoutTaggerGlowHandler().stopAll();
    }

    default void restrictCombatDamage(EntityDamageByEntityEvent event) {
        getCombatDamageRestrictionHandler().onEntityDamageByEntity(event);
    }
}
