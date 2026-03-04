package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerDeathEventHandler;

import dev.tjxjnoobie.ctf.game.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.player.effects.PlayerDeathEffects;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnScheduler;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.CombatDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;

/**
 * Handles arena-only death cleanup, stat attribution, and immediate respawn flow.
 */
public final class PlayerDeathHandler implements PlayerDeathEventHandler, BukkitMessageSender,
        CombatDependencyAccess, FlagDependencyAccess, LifecycleDependencyAccess, PlayerDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerDeathEvent] ";

    /**
     * Processes arena player deaths and applies the plugin's custom death flow.
     *
     * @param event Bukkit death event for the victim.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player victim = event.getEntity();
        java.util.UUID victimId = victim.getUniqueId();
        boolean conditionResult1 = !getMatchPlayerSessionHandler().isPlayerInArena(victim);
        // Guard: short-circuit when !sessionIsPlayerInArena(victim).
        if (conditionResult1) {
            return;
        }

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        // Resolve carrier and combat attribution before clearing the victim state.
        FlagCarrierStateHandler flagCarrierStateHandler = getFlagCarrierStateHandler();
        FlagDropHandler flagDropHandler = getFlagDropHandler();
        boolean wasCarrier = flagCarrierStateHandler != null && flagCarrierStateHandler.isFlagCarrier(victimId);
        Player killer = victim.getKiller();
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        Entity directDamager = lastDamage instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
        EntityDamageEvent.DamageCause cause = lastDamage == null ? EntityDamageEvent.DamageCause.CUSTOM : lastDamage.getCause();

        if (flagDropHandler != null) {
            flagDropHandler.dropCarriedFlagIfPresent(victim);
        }
        victim.getInventory().clear();

        HomingSpearAbilityHandler abilityHandler = getHomingSpearAbilityService();
        SpearDeathAttributionMetaData spearAttribution = abilityHandler == null ? null
            : abilityHandler.resolveTrackedSpearAttribution(victim, cause, directDamager);
        Component spearDeathMessage = spearAttribution == null ? null : spearAttribution.message();
        Player creditedKiller = killer;
        boolean conditionResult2 = creditedKiller == null && spearAttribution != null && spearAttribution.creditedKillerId() != null;
        if (conditionResult2) {
            java.util.UUID creditedKillerId = spearAttribution.creditedKillerId();
            creditedKiller = Bukkit.getPlayer(creditedKillerId);
        }

        if (creditedKiller != null) {
            getMatchPlayerSessionHandler().recordKill(creditedKiller);
        }
        // Death effects need both the original killer and any spear-attributed fallback killer.
        getPlayerDeathEffects().playAllDeathEffects(victim, killer, creditedKiller, spearDeathMessage, wasCarrier);

        try {
            getMatchPlayerSessionHandler().recordDeath(victim);
        } catch (Throwable ex) {
            String errorMessage = ex.getMessage();
            sendDebugMessage(LOG_PREFIX + "Death sound skipped - reason=" + errorMessage);
        }
        getPlayerRespawnScheduler().scheduleInstantRespawn(victim);

        // Keep the debug feed aligned with the final credited killer after attribution.
        String victimName = victim.getName();
        String killerName = creditedKiller == null ? "none" : creditedKiller.getName();
        sendDebugMessage(LOG_PREFIX + "Death handled - victim=" + victimName + " killer=" + killerName);
    }
}

