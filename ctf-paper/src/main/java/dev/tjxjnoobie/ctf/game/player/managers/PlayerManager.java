package dev.tjxjnoobie.ctf.game.player.managers;

import dev.tjxjnoobie.ctf.game.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.kit.KitManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles arena player lifecycle and per-match player stats.
 */
public final class PlayerManager {
    private static final int MATCH_FOOD_LEVEL = 20;
    private static final float MATCH_SATURATION = 6.0f;

    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final ScoreBoardManager scoreBoardManager;

    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, PlayerMatchStats> matchStatsByPlayer = new HashMap<>();

    public PlayerManager(TeamManager teamManager, KitManager kitManager, ScoreBoardManager scoreBoardManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        this.scoreBoardManager = scoreBoardManager;
    }

    public void joinArenaPlayer(Player player, String teamKey, GameState gameState, Duration remainingTime) {
        if (player == null || teamKey == null) {
            return;
        }

        ensureSurvival(player);
        kitManager.clearSelection(player);
        returnLocations.put(player.getUniqueId(), player.getLocation());
        kitManager.recordOriginalInventory(player);
        player.getInventory().clear();
        teamManager.joinTeam(player, teamKey);
        ensurePlayerStats(player.getUniqueId());

        scoreBoardManager.showScoreboard(player);
        scoreBoardManager.updateScoreboards(remainingTime, gameState);

        if (gameState == GameState.LOBBY) {
            teleportToLobby(player);
            kitManager.openKitSelector(player);
            return;
        }

        if (gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME) {
            teamManager.getSpawn(teamKey).ifPresent(player::teleport);
            if (kitManager.hasSelection(player)) {
                kitManager.applyKit(player);
            } else {
                kitManager.openKitSelector(player, true);
            }
            applyMatchVitals(player);
            return;
        }

        teleportToLobby(player);
    }

    public void leaveArenaPlayer(Player player, boolean restoreLocation, Duration remainingTime, GameState gameState) {
        if (player == null) {
            return;
        }

        teamManager.leaveTeam(player);
        scoreBoardManager.hideScoreboard(player);
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
        kitManager.restoreInventory(player);
        kitManager.clearSelection(player);

        if (restoreLocation) {
            Location returnLocation = returnLocations.remove(player.getUniqueId());
            if (returnLocation != null) {
                player.teleport(returnLocation);
            }
            return;
        }
        returnLocations.remove(player.getUniqueId());
    }

    public void teleportPlayersToTeamSpawns() {
        for (Player player : teamManager.getJoinedPlayers()) {
            String teamKey = teamManager.getTeamKey(player);
            if (teamKey == null) {
                continue;
            }
            teamManager.getSpawn(teamKey).ifPresent(player::teleport);
        }
    }

    public void teleportPlayersToLobby(List<Player> players) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            teleportToLobby(player);
        }
    }

    public PlayerMatchStats getPlayerStats(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return matchStatsByPlayer.get(playerId);
    }

    public void recordKill(Player killer, Duration remainingTime, GameState gameState) {
        if (killer == null) {
            return;
        }
        ensurePlayerStats(killer.getUniqueId()).incrementKills();
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
    }

    public void recordDeath(Player victim, Duration remainingTime, GameState gameState) {
        if (victim == null) {
            return;
        }
        ensurePlayerStats(victim.getUniqueId()).incrementDeaths();
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
    }

    public void recordCapture(Player scorer, Duration remainingTime, GameState gameState) {
        if (scorer == null) {
            return;
        }
        ensurePlayerStats(scorer.getUniqueId()).incrementCaptures();
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
    }

    public void applyMatchVitals(List<Player> players) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            applyMatchVitals(player);
        }
    }

    public void applyMatchVitals(Player player) {
        if (player == null) {
            return;
        }
        ensureSurvival(player);
        player.setFoodLevel(MATCH_FOOD_LEVEL);
        player.setSaturation(MATCH_SATURATION);
    }

    public void clearState() {
        returnLocations.clear();
        matchStatsByPlayer.clear();
    }

    private void teleportToLobby(Player player) {
        if (player == null) {
            return;
        }
        teamManager.getLobbySpawn().ifPresent(player::teleport);
    }

    private void ensureSurvival(Player player) {
        if (player == null) {
            return;
        }
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    }

    private PlayerMatchStats ensurePlayerStats(UUID playerId) {
        return matchStatsByPlayer.computeIfAbsent(playerId, key -> new PlayerMatchStats());
    }
}

