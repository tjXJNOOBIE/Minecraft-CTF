package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.dependency.interfaces.DependencyAccess;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.player.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

/**
 * Player session runtime: join/leave side effects, spawn wiring, and stat updates.
 */
public final class MatchPlayerSessionHandler implements DependencyAccess, BukkitMessageSender {

    // == Constants ==
    private static final int MIN_PLAYERS = 2;

    // == Getters ==
    /**
     * Resolves the spawn location used when a tracked arena player respawns.
     *
     * @param player Player involved in this operation.
     * @return Resolved value for the requested lookup.
     */
    public Location getRespawnLocation(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return null;
        }
        return getTeamManager().getSpawnFor(player).orElse(null);
    }

    public String resolveJoinTeamKey(String requestedTeamKey) {
        boolean conditionResult1 = requestedTeamKey == null || requestedTeamKey.isBlank();
        // Blank joins fall back to the team's balancing rules.
        if (conditionResult1) {
            return getTeamManager().getBalancedTeamKey(TeamManager.RED);
        }

        String normalized = getTeamManager().normalizeKey(requestedTeamKey);
        // Guard: short-circuit when normalized == null.
        if (normalized == null) {
            return null;
        }

        return isRunning() ? getTeamManager().getBalancedTeamKey(normalized) : normalized;
    }

    public PlayerMatchStats getPlayerStats(UUID playerUUID) {
        return getPlayerManager().getPlayerStats(playerUUID);
    }

    // == Setters ==
    public boolean setTeamSpawn(Player player, String teamKey) {
        // Guard: short-circuit when player == null || teamKey == null.
        if (player == null || teamKey == null) {
            return false;
        }
        Location playerLocation = player.getLocation();
        getTeamManager().setSpawn(teamKey, playerLocation);
        return true;
    }

    public boolean setLobbySpawn(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return false;
        }
        Location playerLocation = player.getLocation();
        getTeamManager().setLobbySpawn(playerLocation);
        return true;
    }

    // == Utilities ==
    /**
     * Returns the result of addTeamReturnPoint.
     *
     * @param player Player involved in this operation.
     * @param teamKey Team key used for lookup or state updates.
     * @return Result produced by this method.
     */
    public boolean addTeamReturnPoint(Player player, String teamKey) {
        // Guard: short-circuit when player == null || teamKey == null.
        if (player == null || teamKey == null) {
            return false;
        }
        Location playerLocation = player.getLocation();
        return getTeamManager().addReturnPoint(teamKey, playerLocation);
    }

    /**
     * Removes the nearest configured return point to the player.
     *
     * @param player Player whose current location is used as the reference point.
     * @return Team key for the removed point, or {@code null} when no return point was removed.
     */
    public String removeNearestReturnPoint(Player player) {
        if (player == null) {
            return null;
        }
        return getTeamManager().removeNearestReturnPoint(player.getLocation());
    }

    /**
     * Adds a player to the arena and applies the state transitions for the current game phase.
     *
     * @param player Player involved in this operation.
     * @param teamKey Team key used for lookup or state updates.
     */
    public void addPlayerToArena(Player player, String teamKey) {
        boolean conditionResult2 = player == null || teamKey == null || getGameStateManager().isCleanupInProgress(); // Validation & early exits
        // Guard: ignore arena-join requests when input is invalid or cleanup is active.
        if (conditionResult2) {
            return;
        }

        GameState gameState = getGameStateManager().getGameState(); // World/application side effects
        Duration remainingTime = getRemainingTime();
        getPlayerManager().joinCTFArena(player, teamKey, gameState, remainingTime);
        FlagLifecycleHandler flagLifecycleHandler = getFlagLifecycleHandler();
        if (flagLifecycleHandler != null) {
            flagLifecycleHandler.syncIndicatorVisibility();
        }

        boolean conditionResult3 = isRunning() && getTeamManager().getJoinedPlayerCount() >= MIN_PLAYERS; // State transition
        if (conditionResult3) {
            getMatchCleanupHandler().onPlayerJoinedDuringMatch();
        }

        String playerName = player.getName(); // Debug/telemetry
        String stateName = gameState.name();
        sendDebugMessage("join player=" + playerName + " team=" + teamKey + " state=" + stateName);
    }

    /**
     * Removes a player from the arena and tears down combat, flag, and scoreboard state.
     *
     * @param player Player involved in this operation.
     * @param restoreLocation World location used by this operation.
     */
    public void removePlayerFromArena(Player player, boolean restoreLocation) {
        // Validation & early exits
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }
        UUID playerUUID = player.getUniqueId();

        // Force the player to drop or clear every arena-owned state before leaving team membership.
        FlagDropHandler flagDropHandler = getFlagDropHandler(); // World/application side effects
        if (flagDropHandler != null) {
            flagDropHandler.dropCarriedFlagIfPresent(player);
        }

        FlagCarrierStateHandler flagCarrierStateHandler = getFlagCarrierStateHandler();
        if (flagCarrierStateHandler != null) {
            flagCarrierStateHandler.clearCarrierFlagItemAndEffects(player);
        }

        HomingSpearAbilityCooldown homingSpearCooldown = getHomingSpearAbilityCooldown();
        if (homingSpearCooldown != null) {
            homingSpearCooldown.clearPlayerCombatState(playerUUID);
        }

        ScoutTaggerAbility scoutTaggerAbility = getScoutTaggerAbility();
        if (scoutTaggerAbility != null) {
            scoutTaggerAbility.removePlayerFromArena(player);
        }

        GameState gameState = getGameStateManager().getGameState(); // World/application side effects
        Duration remainingTime = getRemainingTime();
        getPlayerManager().leaveCTFArena(player, restoreLocation, remainingTime, gameState);
        FlagLifecycleHandler flagLifecycleHandler = getFlagLifecycleHandler();
        if (flagLifecycleHandler != null) {
            flagLifecycleHandler.syncIndicatorVisibility();
        }

        boolean running = isRunning(); // State transition
        if (running) {
            getMatchCleanupHandler().onPlayerLeftDuringMatch();
        }

        String playerName = player.getName(); // Debug/telemetry
        String stateName = gameState.name();
        sendDebugMessage("leave player=" + playerName + " state=" + stateName);
    }

    /**
     * Executes recordKill.
     *
     * @param killer Player involved in this operation.
     */
    public void recordKill(Player killer) {
        Duration remainingTime = getRemainingTime();
        GameState gameState = getGameStateManager().getGameState();
        getPlayerManager().recordKill(killer, remainingTime, gameState);
    }

    /**
     * Executes recordDeath.
     *
     * @param victim Player involved in this operation.
     */
    public void recordDeath(Player victim) {
        Duration remainingTime = getRemainingTime();
        GameState gameState = getGameStateManager().getGameState();
        getPlayerManager().recordDeath(victim, remainingTime, gameState);
    }

    // == Predicates ==
    public boolean isPlayerInArena(Player player) {
        return player != null && getTeamManager().getTeamKey(player) != null;
    }

    private boolean isRunning() {
        return !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
    }
}

