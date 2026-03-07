package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.player.effects.PlayerDeathEffects;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnScheduler;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.player.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.team.handlers.TeamChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Dependency-access surface for player-session runtime behavior and stats.
 */
public interface PlayerRuntimeDependencyAccess {
    default PlayerManager getPlayerManager() { return DependencyLoaderAccess.findInstance(PlayerManager.class); }
    default PlayerRespawnScheduler getPlayerRespawnScheduler() { return DependencyLoaderAccess.findInstance(PlayerRespawnScheduler.class); }
    default PlayerDeathEffects getPlayerDeathEffects() { return DependencyLoaderAccess.findInstance(PlayerDeathEffects.class); }
    default TeamChatRenderer getTeamChatRenderer() { return DependencyLoaderAccess.findInstance(TeamChatRenderer.class); }

    default PlayerMatchStats playerGetStats(UUID playerId) {
        return getPlayerManager().getPlayerStats(playerId);
    }

    default void playerJoinCTFArena(Player player, String teamKey, GameState gameState, Duration remainingTime) {
        getPlayerManager().joinCTFArena(player, teamKey, gameState, remainingTime);
    }

    default void playerLeaveCTFArena(Player player, boolean restoreLocation, Duration remainingTime, GameState gameState) {
        getPlayerManager().leaveCTFArena(player, restoreLocation, remainingTime, gameState);
    }

    default void playerTeleportToTeamSpawns() {
        getPlayerManager().teleportPlayersToTeamSpawns();
    }

    default void playerTeleportToLobby(List<Player> players) {
        getPlayerManager().teleportPlayersToLobby(players);
    }

    default void playerRecordKill(Player killer, Duration remainingTime, GameState gameState) {
        getPlayerManager().recordKill(killer, remainingTime, gameState);
    }

    default void playerRecordDeath(Player victim, Duration remainingTime, GameState gameState) {
        getPlayerManager().recordDeath(victim, remainingTime, gameState);
    }

    default void playerRecordCapture(Player scorer, Duration remainingTime, GameState gameState) {
        getPlayerManager().recordCapture(scorer, remainingTime, gameState);
    }

    default void playerApplyMatchVitals(List<Player> players) {
        getPlayerManager().applyMatchVitals(players);
    }

    default void playerApplyMatchVitals(Player player) {
        getPlayerManager().applyMatchVitals(player);
    }

    default void playerClearState() {
        getPlayerManager().clearState();
    }

    default void scheduleInstantRespawn(Player player) {
        getPlayerRespawnScheduler().scheduleInstantRespawn(player);
    }

    default void playAllDeathEffects(Player victim,
                                     Player killer,
                                     Player creditedKiller,
                                     Component spearDeathMessage,
                                     boolean wasCarrier) {
        getPlayerDeathEffects().playAllDeathEffects(victim, killer, creditedKiller, spearDeathMessage, wasCarrier);
    }

    default void applyTeamChatRenderer(AsyncChatEvent event) {
        getTeamChatRenderer().applyTeamChatRenderer(event);
    }
}
