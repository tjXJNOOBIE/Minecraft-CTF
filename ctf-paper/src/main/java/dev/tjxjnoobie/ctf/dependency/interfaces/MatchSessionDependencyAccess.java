package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.player.metadata.PlayerMatchStats;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Dependency-access surface for arena join/leave state and per-match player session data.
 */
public interface MatchSessionDependencyAccess {
    default MatchPlayerSessionHandler getMatchPlayerSessionHandler() { return DependencyLoaderAccess.findInstance(MatchPlayerSessionHandler.class); }

    default String sessionResolveJoinTeamKey(String requestedTeamKey) {
        return getMatchPlayerSessionHandler().resolveJoinTeamKey(requestedTeamKey);
    }

    default void sessionAddPlayerToArena(Player player, String teamKey) {
        getMatchPlayerSessionHandler().addPlayerToArena(player, teamKey);
    }

    default void sessionRemovePlayerFromArena(Player player, boolean restoreLocation) {
        getMatchPlayerSessionHandler().removePlayerFromArena(player, restoreLocation);
    }

    default void sessionRecordKill(Player killer) {
        getMatchPlayerSessionHandler().recordKill(killer);
    }

    default void sessionRecordDeath(Player victim) {
        getMatchPlayerSessionHandler().recordDeath(victim);
    }

    default boolean sessionIsPlayerInArena(Player player) {
        return getMatchPlayerSessionHandler().isPlayerInArena(player);
    }

    default Location sessionGetRespawnLocation(Player player) {
        return getMatchPlayerSessionHandler().getRespawnLocation(player);
    }

    default boolean sessionSetTeamSpawn(Player player, String teamKey) {
        return getMatchPlayerSessionHandler().setTeamSpawn(player, teamKey);
    }

    default boolean sessionSetLobbySpawn(Player player) {
        return getMatchPlayerSessionHandler().setLobbySpawn(player);
    }

    default boolean sessionAddTeamReturnPoint(Player player, String teamKey) {
        return getMatchPlayerSessionHandler().addTeamReturnPoint(player, teamKey);
    }

    default PlayerMatchStats sessionGetPlayerStats(UUID playerId) {
        return getMatchPlayerSessionHandler().getPlayerStats(playerId);
    }
}
