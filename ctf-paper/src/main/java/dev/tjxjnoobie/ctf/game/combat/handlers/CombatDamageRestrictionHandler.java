package dev.tjxjnoobie.ctf.game.combat.handlers;

import dev.tjxjnoobie.ctf.events.handlers.CombatDamageByEntityHandler;

import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Owns match combat boundary decisions for player-vs-player damage.
 */
public final class CombatDamageRestrictionHandler implements CombatDamageByEntityHandler, BukkitMessageSender, LifecycleDependencyAccess, PlayerDependencyAccess {
    // Core systems (plugin, game state, loop, debug)

    // == Lifecycle ==
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        boolean playerInArena = getMatchPlayerSessionHandler().isPlayerInArena(player);
        if (!playerInArena || isRunning()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // Guard: short-circuit when !(event.getEntity() instanceof Player target).
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        boolean conditionResult1 = attacker == null || attacker.getUniqueId().equals(target.getUniqueId());
        // Guard: short-circuit when attacker == null || attacker.getUniqueId().equals(target.getUniqueId()).
        if (conditionResult1) {
            return;
        }

        boolean attackerInArena = getMatchPlayerSessionHandler().isPlayerInArena(attacker);
        boolean targetInArena = getMatchPlayerSessionHandler().isPlayerInArena(target);
        // Guard: short-circuit when !attackerInArena && !targetInArena.
        if (!attackerInArena && !targetInArena) {
            return;
        }

        boolean combatAllowed = isCombatAllowed(attacker, target, attackerInArena, targetInArena);
        if (!combatAllowed) {
            event.setCancelled(true);
            net.kyori.adventure.text.Component blockedReason = resolveBlockedReason(attacker, target);
            sendActionBar(attacker, blockedReason);
        }
    }

    // == Getters ==
    private net.kyori.adventure.text.Component resolveBlockedReason(Player attacker, Player target) {
        boolean attackerInArena = getMatchPlayerSessionHandler().isPlayerInArena(attacker);
        boolean targetInArena = getMatchPlayerSessionHandler().isPlayerInArena(target);
        // Guard: short-circuit when attackerInArena != targetInArena.
        if (attackerInArena != targetInArena) {
            return net.kyori.adventure.text.Component.text("You cannot fight players across CTF and non-CTF.");
        }
        boolean running = isRunning();
        // Guard: short-circuit when !running.
        if (!running) {
            return net.kyori.adventure.text.Component.text("PvP is disabled while CTF is in lobby/cleanup.");
        }
        boolean conditionResult2 = !getKitSelectionHandler().hasSelection(target);
        // Guard: short-circuit when !kitHasSelection(target).
        if (conditionResult2) {
            return net.kyori.adventure.text.Component.text("Target is invulnerable until they choose a kit.");
        }
        boolean conditionResult3 = !getKitSelectionHandler().hasSelection(attacker);
        // Guard: short-circuit when !kitHasSelection(attacker).
        if (conditionResult3) {
            return net.kyori.adventure.text.Component.text("Choose a kit before fighting.");
        }
        return net.kyori.adventure.text.Component.text("Friendly fire is disabled.");
    }

    private Player resolveAttacker(Entity damager) {
        // Guard: short-circuit when damager instanceof Player player.
        if (damager instanceof Player player) {
            return player;
        }
        // Guard: short-circuit when damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter.
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    // == Predicates ==
    private boolean isCombatAllowed(Player attacker, Player target, boolean attackerInArena, boolean targetInArena) {
        // Guard: short-circuit when attackerInArena != targetInArena.
        if (attackerInArena != targetInArena) {
            return false;
        }
        // Guard: short-circuit when !attackerInArena.
        if (!attackerInArena) {
            return true;
        }
        boolean running2 = isRunning();
        // Guard: short-circuit when !running2.
        if (!running2) {
            return false;
        }

        String attackerTeam = getTeamManager().getTeamKey(attacker);
        String targetTeam = getTeamManager().getTeamKey(target);
        boolean conditionResult4 = attackerTeam == null || targetTeam == null || attackerTeam.equals(targetTeam);
        // Guard: short-circuit when attackerTeam == null || targetTeam == null || attackerTeam.equals(targetTeam).
        if (conditionResult4) {
            return false;
        }
        return getKitSelectionHandler().hasSelection(attacker) && getKitSelectionHandler().hasSelection(target);
    }

    private boolean isRunning() {
        return !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
    }
}
