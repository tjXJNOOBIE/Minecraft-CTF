package dev.tjxjnoobie.ctf.game.lifecycle.handlers;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.DependencyAccess;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.celebration.managers.WinnerCelebration;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.handlers.BaseMarkerHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.util.MatchCleanupUtil;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.game.tasks.LobbyWaitingTask;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.tasks.EffectTaskOrchestrator;
import dev.tjxjnoobie.ctf.util.tasks.GameTaskOrchestrator;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Match cleanup runtime: stop/shutdown, winner cleanup, and lobby/lone-player tasks.
 */
public final class MatchCleanupHandler implements MessageAccess, BukkitMessageSender, BukkitEffectsUtil, DependencyAccess {

    // == Constants ==
    private static final int MIN_PLAYERS = 2;
    private static final Duration WIN_CLEANUP_DURATION = Duration.ofSeconds(6);
    private static final Component WINNER_CELEBRATION_SUBTITLE =
        Component.text("Celebration time!", NamedTextColor.GOLD);
    // Core systems (plugin, game state, loop, debug)
    private final LonePlayerCountdownHandler lonePlayerCountdownHandler;

    // == Runtime state ==
    private BukkitTask lobbyWaitingTimerTask;
    private BukkitTask winnerCleanupTask;

    // == Lifecycle ==
    /**
     * Constructs a MatchCleanupHandler instance.
     */
    public MatchCleanupHandler() {
        this.lonePlayerCountdownHandler = new LonePlayerCountdownHandler(
            getTeamManager(),
            this::isRunning,
            () -> getMatchCleanupHandler().requestMatchStop(MatchStopReason.GENERIC, null),
            this::broadcast
        );
    }

    /**
     * Starts the lobby waiting ticker that watches for enough players to begin a match.
     */
    public void startLobbyWaitingTimer() {
        // Guard: short-circuit when lobbyWaitingTimerTask != null.
        if (lobbyWaitingTimerTask != null) {
            return;
        }

        FlagBaseSetupHandler flagBaseSetupHandler = getFlagBaseSetupHandler();
        // Guard: short-circuit when flagBaseSetupHandler == null.
        if (flagBaseSetupHandler == null) {
            return;
        }

        LobbyWaitingTask task = new LobbyWaitingTask(
            MIN_PLAYERS,
            getTeamManager(),
            getBossBarManager(),
            getGameStateManager(),
            getGameLoopTimer(),
            flagBaseSetupHandler,
            getMatchFlowHandler()::startAutoMatchStartCountdownTimer
        );
        lobbyWaitingTimerTask = GameTaskOrchestrator.startTimer(lobbyWaitingTimerTask, task, 0L, 20L);
    }

    private void startWinnerCleanup(String winningTeamKey) {
        // Validation & early exits
        if (winningTeamKey == null) {
            stopMatchAndResetToLobby(MatchStopReason.WIN, null);
            return;
        }

        // Freeze the current match state before winner celebration side effects run.
        getGameStateManager().setCleanupInProgress(true);
        getGameStateManager().setForcedCountdown(false);
        getGameStateManager().setLastEndCountdownAnnounced(-1L);
        stopMatchLifecycleTimers(false);
        cleanupRuntimeArtifacts(true);
        List<Player> joinedPlayers = MatchCleanupUtil.snapshotPlayers(getTeamManager().getJoinedPlayers());
        MatchCleanupUtil.clearInventories(joinedPlayers);
        MatchCleanupUtil.enableWinnerCelebrationFlight(winningTeamKey, joinedPlayers, this::teamGetTeamKey);

        String winningTeamDisplayName = getTeamManager().getDisplayName(winningTeamKey); // UX feedback
        Component winMessage = getMessageFormatted("broadcast.win", winningTeamDisplayName);
        broadcast(winMessage);

        Component title = Component.text(
            winningTeamDisplayName + " Wins!",
            TeamManager.RED.equals(winningTeamKey) ? NamedTextColor.RED : NamedTextColor.BLUE,
            TextDecoration.BOLD
        );
        broadcastToArenaTitle(title, WINNER_CELEBRATION_SUBTITLE);
        playSoundToPlayers(getCTFPlayerMetaData().getPlayers(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.1f);
        getWinnerCelebration().start(winningTeamKey, WIN_CLEANUP_DURATION.toMillis());

        // Debug/telemetry
        sendDebugMessage("cleanup celebration winner=" + winningTeamKey);

        // Delay the lobby reset until the celebration window completes.
        winnerCleanupTask = EffectTaskOrchestrator.startLater(
            winnerCleanupTask,
            () -> {
                winnerCleanupTask = null;
                stopMatchAndResetToLobby(MatchStopReason.WIN, winningTeamKey);
            },
            WIN_CLEANUP_DURATION.toSeconds() * 20L
        );
    }

    private void stopMatchAndResetToLobby(MatchStopReason reason, String winningTeamKey) {
        // Normalize the runtime back to the lobby baseline before reopening joins.
        getGameStateManager().setCleanupInProgress(true);
        getWinnerCelebration().cancel();
        stopMatchLifecycleTimers(false);
        getGameStateManager().resetCountdownState();

        List<Player> joinedPlayers = MatchCleanupUtil.snapshotPlayers(getTeamManager().getJoinedPlayers()); // Domain lookup
        MatchStopReason resolved = reason == null ? MatchStopReason.GENERIC : reason;
        if (resolved != MatchStopReason.WIN) {
            cleanupRuntimeArtifacts(false);
        }

        Component stopMessage = MatchCleanupUtil.resolveStopMessage(this, this::teamGetDisplayName, resolved,
                winningTeamKey); // UX feedback
        MatchCleanupUtil.sendMessageToPlayers(joinedPlayers, player -> sendMessage(player, stopMessage));
        Component ready = getMessage("broadcast.match_ready");
        MatchCleanupUtil.sendMessageToPlayers(MatchCleanupUtil.snapshotPlayers(new java.util.ArrayList<>(Bukkit.getOnlinePlayers())),
                player -> sendMessage(player, ready));

        // World/application side effects
        resetToLobbyState(joinedPlayers);
        FlagLifecycleHandler flagLifecycleHandler = getFlagLifecycleHandler();
        if (flagLifecycleHandler != null) {
            flagLifecycleHandler.resetFlagsToBase();
        }
        getGameStateManager().setCleanupInProgress(false);

        // Debug/telemetry
        sendDebugMessage("match stopped reason=" + resolved.name());
    }

    private void stopMatchLifecycleTimers(boolean shouldStopLobbyWaitingTimer) {
        getGameLoopTimer().stopAllTimers();
        lonePlayerCountdownHandler.cancelLonePlayerEndTimer(false);
        winnerCleanupTask = EffectTaskOrchestrator.cancel(winnerCleanupTask);
        if (shouldStopLobbyWaitingTimer) {
            stopLobbyWaitingTimer();
        }
    }

    private void stopLobbyWaitingTimer() {
        lobbyWaitingTimerTask = GameTaskOrchestrator.cancel(lobbyWaitingTimerTask);
    }

    /**
     * Stops the current countdown or match and optionally routes through winner cleanup first.
     *
     * @param reason Reason associated with this operation.
     * @param winningTeamKey Team key used for lookup or state updates.
     */
    public void requestMatchStop(MatchStopReason reason, String winningTeamKey) {
        MatchStopReason resolved = reason == null ? MatchStopReason.GENERIC : reason; // Domain lookup
        boolean hadMatch = isRunning();
        boolean hadCountdown = getGameLoopTimer().isStartCountdownActive();

        // Validation & early exits
        // Guard: short-circuit when !hadMatch && !hadCountdown.
        if (!hadMatch && !hadCountdown) {
            return;
        }

        boolean conditionResult1 = resolved == MatchStopReason.WIN && winningTeamKey != null && !getGameStateManager().isCleanupInProgress(); // State transition
        if (conditionResult1) {
            startWinnerCleanup(winningTeamKey);
            return;
        }

        stopMatchAndResetToLobby(resolved, winningTeamKey);
    }

    /**
     * Cancels active runtime tasks and clears the plugin back to an idle state.
     */
    public void shutdownMatchSystem() {
        // State transition
        getGameStateManager().setCleanupInProgress(true);
        stopMatchLifecycleTimers(true);
        cleanupRuntimeArtifacts(false);
        resetToLobbyState(List.of());
        getDebugFeed().clear();
        getGameStateManager().resetCountdownState();
        getGameStateManager().setCleanupInProgress(false);
    }

    /**
     * Executes the onPlayerJoinedDuringMatch operation.
     */
    public void onPlayerJoinedDuringMatch() {
        lonePlayerCountdownHandler.onPlayerJoinedDuringMatch();
    }

    /**
     * Executes the onPlayerLeftDuringMatch operation.
     */
    public void onPlayerLeftDuringMatch() {
        lonePlayerCountdownHandler.onPlayerLeftDuringMatch();
    }

    private void cleanupRuntimeArtifacts(boolean keepCelebration) {
        // State transition
        if (!keepCelebration) {
            getWinnerCelebration().cancel();
        }

        FlagCarrierStateHandler flagCarrierStateHandler = getFlagCarrierStateHandler(); // World/application side effects
        if (flagCarrierStateHandler != null) {
            flagCarrierStateHandler.clearCarrierItems();
        }

        FlagLifecycleHandler flagLifecycleHandler = getFlagLifecycleHandler();
        if (flagLifecycleHandler != null) {
            flagLifecycleHandler.onMatchStop();
        }

        HomingSpearAbilityCooldown homingSpearCooldown = getHomingSpearAbilityCooldown();
        if (homingSpearCooldown != null) {
            homingSpearCooldown.clearAllCombatState();
        }

        ScoutTaggerAbility scoutTaggerAbility = getScoutTaggerAbility();
        if (scoutTaggerAbility != null) {
            scoutTaggerAbility.stopAll();
        }

        BaseMarkerHandler baseMarkerHandler = getBaseMarkerHandler();
        if (baseMarkerHandler != null) {
            baseMarkerHandler.removeAllMarkers();
        }
        getBossBarManager().clearAll();
    }

    private void resetToLobbyState(List<Player> joinedPlayers) {
        // State transition
        getScoreBoardManager().resetScores();
        getScoreBoardManager().updateScoreboards(Duration.ZERO, GameState.LOBBY);
        getKitSelectionHandler().restoreAll();
        getKitSelectionHandler().clearAll();

        boolean conditionResult2 = joinedPlayers != null && !joinedPlayers.isEmpty(); // World/application side effects
        if (conditionResult2) {
            getPlayerManager().teleportPlayersToReturnLocations(joinedPlayers);
            MatchCleanupUtil.restorePostCleanupPlayerState(joinedPlayers);
        }
        getScoreBoardManager().clearAll();
        getScoreBoardManager().clearStats();
        getTeamManager().clearAllTeams();
        getPlayerManager().clearState();
        getGameStateManager().setGameState(GameState.LOBBY);
    }

    private void broadcast(Component message) { broadcastArenaPlayers(message); }

    // == Predicates ==
    private boolean isRunning() {
        return !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
    }
}

