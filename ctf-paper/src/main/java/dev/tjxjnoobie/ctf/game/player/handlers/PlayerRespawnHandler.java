package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerRespawnEventHandler;

import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.CombatDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.KitUiDependencyAccess;
/**
 * Owns arena respawn state transitions: respawn location + kit re-application.
 */
public final class PlayerRespawnHandler implements PlayerRespawnEventHandler, BukkitMessageSender, CombatDependencyAccess, LifecycleDependencyAccess, PlayerDependencyAccess, KitUiDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerRespawnEvent] ";
    // Core systems (plugin, game state, loop, debug)
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();
        Location respawn = getMatchPlayerSessionHandler().getRespawnLocation(player);
        if (respawn != null) {
            event.setRespawnLocation(respawn);
        }

        boolean running = !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
        boolean conditionResult1 = running && getMatchPlayerSessionHandler().isPlayerInArena(player);
        if (conditionResult1) {
            boolean conditionResult2 = getKitSelectionHandler().hasSelection(player);
            if (conditionResult2) {
                KitType selectedKit = getKitSelectionHandler().getSelectedKit(player);
                getKitSelectionHandler().applyKitLoadout(player, selectedKit);
            } else {
                getKitSelectorGui().openKitSelector(player, true);
            }
            player.setNoDamageTicks(60);
            ScoutTaggerAbility scoutTaggerAbility = getScoutTaggerAbility();
            if (scoutTaggerAbility != null) {
                scoutTaggerAbility.processRespawnState(player);
            }
        }

        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "Respawn handled - player=" + playerName);
    }
}
