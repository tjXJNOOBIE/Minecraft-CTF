package dev.tjxjnoobie.ctf.game.player.managers;

import dev.tjxjnoobie.ctf.game.player.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
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

    // == Constants ==
    private static final int MATCH_FOOD_LEVEL = 20;
    private static final float MATCH_SATURATION = 6.0f;
    private final TeamManager teamManager;
    private final KitSelectionHandler kitSelectionHandler;
    private final KitSelectorGUI kitSelectorGui;
    private final ScoreBoardManager scoreBoardManager;

    // == Runtime state ==
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, PlayerMatchStats> matchStatsByPlayer = new HashMap<>();

    // == Lifecycle ==
    /**
     * Constructs a PlayerManager instance.
     *
     * @param teamManager Dependency responsible for team manager.
     * @param kitSelectionHandler Dependency responsible for kit selection handler.
     * @param kitSelectorGui Dependency responsible for kit selector gui.
     * @param scoreBoardManager Dependency responsible for score board manager.
     */
    public PlayerManager(TeamManager teamManager,
                         KitSelectionHandler kitSelectionHandler,
                         KitSelectorGUI kitSelectorGui,
                         ScoreBoardManager scoreBoardManager) {
        // Wire dependencies for player lifecycle management.
        this.teamManager = teamManager;
        this.kitSelectionHandler = kitSelectionHandler;
        this.kitSelectorGui = kitSelectorGui;
        this.scoreBoardManager = scoreBoardManager;
    }

    // == Getters ==
    public PlayerMatchStats getPlayerStats(UUID playerUUID) {
        // Guard: short-circuit when playerUUID == null.
        if (playerUUID == null) {
            return null;
        }
        // Return cached match stats for the player.
        return matchStatsByPlayer.get(playerUUID);
    }

    // == Utilities ==
    /**
     * Adds a player to arena state, captures their return location, and applies the correct UI flow.
     *
     * @param player Player involved in this operation.
     * @param teamKey Team key used for lookup or state updates.
     * @param gameState Domain enum value used to control behavior.
     * @param remainingTime Duration or timestamp value in milliseconds.
     */
    public void joinCTFArena(Player player, String teamKey, GameState gameState, Duration remainingTime) {
        // Guard: short-circuit when player == null || teamKey == null.
        if (player == null || teamKey == null) {
            return;
        }

        // Reset inventory and store return location.
        ensureSurvival(player);
        kitSelectionHandler.clearSelection(player);
        UUID playerUUID = player.getUniqueId();
        Location playerLocation = player.getLocation();
        returnLocations.put(playerUUID, playerLocation);
        kitSelectionHandler.recordOriginalInventory(player);
        player.getInventory().clear();
        teamManager.joinTeam(player, teamKey);
        ensurePlayerStats(playerUUID);

        // Initialize scoreboard for the player.
        scoreBoardManager.showScoreboard(player);
        scoreBoardManager.updateScoreboards(remainingTime, gameState);

        if (gameState == GameState.LOBBY) {
            // Lobby flow only stages the player in the arena; kit choice opens when the match goes live.
            restoreJoinLobbyVitals(player);
            teleportToLobby(player);
            return;
        }

        if (gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME) {
            // Match flow: teleport to spawn and apply kit.
            teamManager.getSpawn(teamKey).ifPresent(player::teleport);
            boolean hasSelection = kitSelectionHandler.hasSelection(player);
            if (hasSelection) {
                KitType selectedKit = kitSelectionHandler.getSelectedKit(player);
                kitSelectionHandler.applyKitLoadout(player, selectedKit);
            } else {
                kitSelectorGui.openKitSelector(player, true);
            }
            applyMatchVitals(player);
            return;
        }

        teleportToLobby(player);
    }

    /**
     * Removes a player from arena state and restores their inventory and optional return location.
     *
     * @param player Player involved in this operation.
     * @param restoreLocation World location used by this operation.
     * @param remainingTime Duration or timestamp value in milliseconds.
     * @param gameState Domain enum value used to control behavior.
     */
    public void leaveCTFArena(Player player, boolean restoreLocation, Duration remainingTime, GameState gameState) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId(); // Restore inventory and scoreboard when leaving.
        teamManager.leaveTeam(player);
        scoreBoardManager.hideScoreboard(player);
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
        kitSelectionHandler.restoreInventory(player);
        kitSelectionHandler.clearSelection(player);

        if (restoreLocation) {
            Location returnLocation = returnLocations.remove(playerUUID);
            if (returnLocation != null) {
                player.teleport(returnLocation);
            }
            return;
        }
        returnLocations.remove(playerUUID);
    }

    /**
     * Teleports every joined player to their configured team spawn.
     */
    public void teleportPlayersToTeamSpawns() {
        // Teleport each joined player to their team spawn.
        for (Player player : teamManager.getJoinedPlayers()) {
            String teamKey = teamManager.getTeamKey(player);
            // Guard: short-circuit when teamKey == null.
            if (teamKey == null) {
                continue;
            }
            teamManager.getSpawn(teamKey).ifPresent(player::teleport);
        }
    }

    /**
     * Teleports the provided players to the shared lobby spawn when it is configured.
     *
     * @param players Players involved in this operation.
     */
    public void teleportPlayersToLobby(List<Player> players) {
        // Send each player to the lobby spawn.
        forEachPlayer(players, this::teleportToLobby);
    }

    /**
     * Teleports the provided players back to their saved pre-join locations when available.
     *
     * @param players Players involved in this operation.
     */
    public void teleportPlayersToReturnLocations(List<Player> players) {
        forEachPlayer(players, this::teleportToReturnLocation);
    }

    /**
     * Records a kill for the player and refreshes live scoreboard lines.
     *
     * @param killer Player involved in this operation.
     * @param remainingTime Duration or timestamp value in milliseconds.
     * @param gameState Domain enum value used to control behavior.
     */
    public void recordKill(Player killer, Duration remainingTime, GameState gameState) {
        // Guard: short-circuit when killer == null.
        if (killer == null) {
            return;
        }
        UUID killerId = killer.getUniqueId(); // Increment kill stats and refresh scoreboards.
        ensurePlayerStats(killerId).incrementKills();
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
    }

    /**
     * Records a death for the player and refreshes live scoreboard lines.
     *
     * @param victim Player involved in this operation.
     * @param remainingTime Duration or timestamp value in milliseconds.
     * @param gameState Domain enum value used to control behavior.
     */
    public void recordDeath(Player victim, Duration remainingTime, GameState gameState) {
        // Guard: short-circuit when victim == null.
        if (victim == null) {
            return;
        }
        UUID victimId = victim.getUniqueId(); // Increment death stats and refresh scoreboards.
        ensurePlayerStats(victimId).incrementDeaths();
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
    }

    /**
     * Records a flag capture for the player and refreshes live scoreboard lines.
     *
     * @param scorer Player involved in this operation.
     * @param remainingTime Duration or timestamp value in milliseconds.
     * @param gameState Domain enum value used to control behavior.
     */
    public void recordCapture(Player scorer, Duration remainingTime, GameState gameState) {
        // Guard: short-circuit when scorer == null.
        if (scorer == null) {
            return;
        }
        UUID scorerId = scorer.getUniqueId(); // Increment capture stats and refresh scoreboards.
        ensurePlayerStats(scorerId).incrementCaptures();
        scoreBoardManager.updateScoreboards(remainingTime, gameState);
    }

    /**
     * Executes applyMatchVitals.
     *
     * @param players Players involved in this operation.
     */
    public void applyMatchVitals(List<Player> players) {
        // Apply match vitals for all provided players.
        forEachPlayer(players, this::applyMatchVitals);
    }

    /**
     * Executes applyMatchVitals.
     *
     * @param player Player involved in this operation.
     */
    public void applyMatchVitals(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }
        // Force survival and match hunger settings.
        ensureSurvival(player);
        player.setFoodLevel(MATCH_FOOD_LEVEL);
        player.setSaturation(MATCH_SATURATION);
    }

    private void restoreJoinLobbyVitals(Player player) {
        if (player == null) {
            return;
        }

        ensureSurvival(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(MATCH_FOOD_LEVEL);
        player.setSaturation(MATCH_SATURATION);
    }

    /**
     * Executes clearState.
     */
    public void clearState() {
        // Clear return locations and per-match stats.
        returnLocations.clear();
        matchStatsByPlayer.clear();
    }

    private void teleportToLobby(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }
        // Use the configured lobby spawn when available.
        teamManager.getLobbySpawn().ifPresent(player::teleport);
    }

    private void ensureSurvival(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }
        // Force survival mode for match flow.
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    }

    private void teleportToReturnLocation(Player player) {
        if (player == null) {
            return;
        }

        Location returnLocation = returnLocations.get(player.getUniqueId());
        if (returnLocation != null) {
            player.teleport(returnLocation);
            return;
        }

        teleportToLobby(player);
    }

    private PlayerMatchStats ensurePlayerStats(UUID playerUUID) {
        return matchStatsByPlayer.computeIfAbsent(playerUUID, key -> new PlayerMatchStats());
    }

    private void forEachPlayer(List<Player> players, java.util.function.Consumer<Player> action) {
        // Guard: short-circuit when players == null || action == null.
        if (players == null || action == null) {
            return;
        }
        for (Player player : players) {
            if (player != null) {
                action.accept(player);
            }
        }
    }
}

