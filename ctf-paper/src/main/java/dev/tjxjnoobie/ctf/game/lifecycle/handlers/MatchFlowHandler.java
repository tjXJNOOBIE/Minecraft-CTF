package dev.tjxjnoobie.ctf.game.lifecycle.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.DependencyAccess;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.BaseMarkerHandler;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.game.ArenaSetupGuardUtil;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;

import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

/**
 * Match flow runtime: countdown, active timer, overtime, and match-time updates.
 */
public final class MatchFlowHandler implements MessageAccess, DependencyAccess, BukkitMessageSender {

    // == Constants ==
    private static final int MIN_PLAYERS = 2;
    private static final int AUTO_START_COUNTDOWN_SECONDS = 15;
    private static final int FORCE_START_COUNTDOWN_SECONDS = 10;
    private static final Duration MATCH_DURATION = Duration.ofMinutes(10);
    private static final Duration OVERTIME_DURATION = Duration.ofMinutes(2);
    private final MatchEffectsHandler effectsHandler = new MatchEffectsHandler();

    // == Lifecycle ==
    /**
     * Starts the normal lobby countdown used when enough players are present.
     */
    public void startAutoMatchStartCountdownTimer() {
        startMatchStartCountdownTimer(AUTO_START_COUNTDOWN_SECONDS, false);
    }

    private void startMatchStartCountdownTimer(int seconds, boolean force) {
        // State transition
        getGameStateManager().setActiveStartCountdownTotal(Math.max(1, seconds));

        // World/application side effects
        getGameLoopTimer().startMatchCountdown(
            seconds,
            () -> {
                FlagBaseSetupHandler flagBaseSetupHandler = getFlagBaseSetupHandler();
                return getGameStateManager().getGameState() == GameState.LOBBY
                    && !getGameStateManager().isCleanupInProgress()
                    && flagBaseSetupHandler != null
                    && flagBaseSetupHandler.areBasesReady()
                    && ArenaSetupGuardUtil.isArenaConfigured()
                    && (force || hasMinimumArenaPlayers());
            },
            this::processMatchStartCountdownTick,
            this::startMatch,
            this::processMatchStartCountdownAbort
        );

        // UX feedback
        if (force) {
            broadcastMessage("broadcast.countdown.force");
            return;
        }
        broadcastMessage("broadcast.countdown.auto");
    }

    private void startMatch() {
        FlagBaseSetupHandler flagBaseSetupHandler = getFlagBaseSetupHandler();
        FlagLifecycleHandler flagLifecycleHandler = getFlagLifecycleHandler();
        boolean conditionResult8 = getGameStateManager().isCleanupInProgress() || getGameStateManager().getGameState() != GameState.LOBBY; // Validation & early exits
        // Guard: only start matches from lobby when cleanup is not running.
        if (conditionResult8) {
            return;
        }
        boolean conditionResult9 = !getGameStateManager().isForcedCountdown() && !hasMinimumArenaPlayers();
        // Guard: short-circuit when !isForcedCountdown() && !hasMinimumArenaPlayers().
        if (conditionResult9) {
            return;
        }
        boolean conditionResult10 = flagBaseSetupHandler == null || !flagBaseSetupHandler.areBasesReady() || flagLifecycleHandler == null;
        // Guard: short-circuit when flagBaseSetupHandler == null || !flagBaseSetupHandler.areBasesReady() || flagLifecycleHandler == null.
        if (conditionResult10) {
            return;
        }
        if (!ArenaSetupGuardUtil.isArenaConfigured()) {
            return;
        }

        // Flip to in-progress before teleporting players so dependent systems see the live match state.
        getGameStateManager().setForcedCountdown(false);
        getGameStateManager().setActiveStartCountdownTotal(0);
        getGameStateManager().setLastEndCountdownAnnounced(-1L);

        getGameStateManager().setGameState(GameState.IN_PROGRESS);
        getScoreBoardManager().resetScores();
        GameState gameState = getGameStateManager().getGameState();
        getScoreBoardManager().updateScoreboards(MATCH_DURATION, gameState);

        // Teleport, arm kits, and prime combat helpers for the newly started match.
        getPlayerManager().teleportPlayersToTeamSpawns();
        flagLifecycleHandler.onMatchStart();
        List<org.bukkit.entity.Player> joinedPlayers = getTeamManager().getJoinedPlayers();
        for (org.bukkit.entity.Player player : joinedPlayers) {
            if (player != null) {
                boolean hasSelection = getKitSelectionHandler().hasSelection(player);
                if (hasSelection) {
                    getKitSelectionHandler().applyKitLoadout(player, getKitSelectionHandler().getSelectedKit(player));
                } else {
                    getKitSelectorGui().openKitSelector(player, true);
                }
            }
        }
        getPlayerManager().applyMatchVitals(joinedPlayers);

        ScoutTaggerAbility scoutTaggerAbility = getScoutTaggerAbility();
        if (scoutTaggerAbility != null) {
            scoutTaggerAbility.startForMatch();
        }

        BaseMarkerHandler baseMarkerHandler = getBaseMarkerHandler();
        if (baseMarkerHandler != null) {
            TeamBaseMetaData redBaseData = flagBaseSetupHandler.getTeamBaseMetaData(TeamId.RED);
            TeamBaseMetaData blueBaseData = flagBaseSetupHandler.getTeamBaseMetaData(TeamId.BLUE);
            baseMarkerHandler.spawnOrMoveBaseMarker(redBaseData);
            baseMarkerHandler.spawnOrMoveBaseMarker(blueBaseData);
        }

        // UX feedback
        effectsHandler.playMatchStartEffects(getTeamManager());
        broadcastMessage("broadcast.start");

        // Debug/telemetry
        sendDebugMessage("match started");

        // World/application side effects
        getGameLoopTimer().startMatchTimer(
            MATCH_DURATION,
            this::processMatchDurationTimerTick,
            this::processMatchDurationTimerEnd
        );
    }

    private void startOvertimeMatchDurationTimer() {
        // State transition
        getGameStateManager().setGameState(GameState.OVERTIME);
        getGameStateManager().setLastEndCountdownAnnounced(-1L);

        // UX feedback
        broadcastMessage("broadcast.overtime.start");
        effectsHandler.playOvertimeReveal(getGameStateManager(), getCTFPlayerMetaData());

        // Debug/telemetry
        sendDebugMessage("overtime started");

        // World/application side effects
        getGameLoopTimer().startMatchTimer(
            OVERTIME_DURATION,
            this::processMatchDurationTimerTick,
            this::processMatchDurationTimerEnd
        );
    }

    // == Getters ==
    public int getAllowedMatchTimeSeconds() {
        boolean conditionResult1 = getGameStateManager().getGameState() == GameState.OVERTIME;
        // Guard: short-circuit when gameState() == GameState.OVERTIME.
        if (conditionResult1) {
            return (int) OVERTIME_DURATION.toSeconds();
        }
        return (int) MATCH_DURATION.toSeconds();
    }

    // == Setters ==
    public boolean setMatchTimeSeconds(long seconds) {
        boolean conditionResult2 = !getGameStateManager().isRunning();
        // Guard: short-circuit when !isGameRunning().
        if (conditionResult2) {
            return false;
        }
        long clampedSeconds = Math.max(0L, seconds);
        Duration remaining = Duration.ofSeconds(clampedSeconds);
        boolean conditionResult3 = !getGameLoopTimer().setRemainingTime(remaining);
        // Guard: short-circuit when !setRemainingTime(remaining).
        if (conditionResult3) {
            return false;
        }
        GameState gameState = getGameStateManager().getGameState();
        getScoreBoardManager().updateScoreboards(remaining, gameState);
        getGameStateManager().setLastEndCountdownAnnounced(-1L);
        return true;
    }

    // == Utilities ==
    /**
     * Returns the result of requestMatchStart.
     *
     * @param force Control flag that changes how this operation is executed.
     * @return {@code true} when the operation succeeds; otherwise {@code false}.
     */
    public boolean requestMatchStart(boolean force) {
        FlagBaseSetupHandler flagBaseSetupHandler = getFlagBaseSetupHandler();
        // Validation & early exits
        if (getGameStateManager().isCleanupInProgress()
            || getGameStateManager().getGameState() != GameState.LOBBY
            || flagBaseSetupHandler == null
            || !flagBaseSetupHandler.areBasesReady()) {
            return false;
        }
        if (!ArenaSetupGuardUtil.isArenaConfigured()) {
            return false;
        }
        boolean conditionResult4 = getTeamManager().getJoinedPlayerCount() <= 0;
        // Guard: short-circuit when teamGetJoinedPlayerCount() <= 0.
        if (conditionResult4) {
            return false;
        }
        boolean conditionResult5 = getGameLoopTimer().isStartCountdownActive();
        // Guard: short-circuit when isStartCountdownActive().
        if (conditionResult5) {
            return true;
        }
        boolean conditionResult6 = !force && !hasMinimumArenaPlayers();
        // Guard: short-circuit when !force && !hasMinimumArenaPlayers().
        if (conditionResult6) {
            return false;
        }

        // State transition
        getGameStateManager().setForcedCountdown(force);
        int countdownSeconds = force ? FORCE_START_COUNTDOWN_SECONDS : AUTO_START_COUNTDOWN_SECONDS;
        startMatchStartCountdownTimer(countdownSeconds, force);
        return true;
    }

    private void processMatchStartCountdownTick(int seconds) {
        if (seconds <= 10) {
            String secondsText = Integer.toString(seconds);
            Component message = getMessageFormatted("broadcast.countdown.tick", secondsText);
            broadcast(message);
        }

        int elapsed = getActiveStartCountdownTotal() - seconds;
        float pitch = Math.min(2.0f, 0.6f + (elapsed * 0.08f));
        effectsHandler.playSoundToJoined(getTeamManager(), Sound.BLOCK_NOTE_BLOCK_HAT, pitch);
        sendDebugMessage("start-countdown t=" + seconds);
    }

    private void processMatchStartCountdownAbort() {
        getGameStateManager().setActiveStartCountdownTotal(0);
        boolean conditionResult7 = getGameStateManager().isForcedCountdown();
        // Guard: short-circuit when isForcedCountdown().
        if (conditionResult7) {
            return;
        }
        broadcastMessage("broadcast.countdown.cancelled");
        sendDebugMessage("start-countdown aborted");
    }

    private void processMatchDurationTimerTick(Duration remaining) {
        GameState gameState = getGameStateManager().getGameState();
        getScoreBoardManager().updateScoreboards(remaining, gameState);
        playMatchEndCountdownSounds(remaining);
    }

    private void processMatchDurationTimerEnd() {
        int red = getScoreBoardManager().getScore(TeamManager.RED); // Domain lookup
        int blue = getScoreBoardManager().getScore(TeamManager.BLUE);

        // State transition
        if (red == blue) {
            startOvertimeMatchDurationTimer();
            return;
        }

        if (red > blue) {
            getMatchCleanupHandler().requestMatchStop(MatchStopReason.WIN, TeamManager.RED);
            return;
        }

        if (blue > red) {
            getMatchCleanupHandler().requestMatchStop(MatchStopReason.WIN, TeamManager.BLUE);
            return;
        }

        getMatchCleanupHandler().requestMatchStop(MatchStopReason.TIMEOUT, null);
    }

    private void playMatchEndCountdownSounds(Duration remaining) {
        long seconds = remaining.toSeconds();
        boolean conditionResult12 = seconds == 60 && getLastEndCountdownAnnounced() != 60L;
        if (conditionResult12) {
            getGameStateManager().setLastEndCountdownAnnounced(60L);
            broadcastMessage("broadcast.end_soon");
        }

        boolean conditionResult13 = seconds <= 10 && seconds > 0 && seconds != getLastEndCountdownAnnounced();
        if (conditionResult13) {
            getGameStateManager().setLastEndCountdownAnnounced(seconds);
            String secondsText = Long.toString(seconds);
            broadcastMessageFormatted("broadcast.end_countdown", secondsText);
            float pitch = Math.max(0.5f, 1.7f - ((10 - seconds) * 0.12f));
            effectsHandler.playSoundToJoined(getTeamManager(), Sound.BLOCK_NOTE_BLOCK_BASS, pitch);
        }
    }

    private void broadcast(Component message) {
        getCTFPlayerMetaData().broadcast(message);
    }

    private void broadcastMessage(String key) {
        broadcast(getMessage(key));
    }

    private void broadcastMessageFormatted(String key, Object... args) {
        broadcast(getMessageFormatted(key, args));
    }

    // == Predicates ==
    private boolean hasMinimumArenaPlayers() {
        return getTeamManager().getJoinedPlayerCount() >= MIN_PLAYERS;
    }
}

