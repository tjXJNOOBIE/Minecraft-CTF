package dev.tjxjnoobie.ctf.game;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.combat.HomingSpearAbility;
import dev.tjxjnoobie.ctf.combat.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.celebration.managers.WinnerCelebration;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import dev.tjxjnoobie.ctf.game.flag.BaseMarkerService;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCaptureHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.state.managers.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.game.tasks.LobbyWaitingTask;
import dev.tjxjnoobie.ctf.game.tasks.LonePlayerEndTask;
import dev.tjxjnoobie.ctf.kit.KitManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Orchestrator; delegates to focused services; commands/listeners should call one-line methods.
 */
public final class CtfMatchOrchestrator implements MessageAccess {
    private static final int DEFAULT_SCORE_LIMIT = 3;
    private static final int MIN_PLAYERS = 2;
    private static final int AUTO_START_COUNTDOWN_SECONDS = 15;
    private static final int FORCE_START_COUNTDOWN_SECONDS = 10;
    private static final int LONE_PLAYER_END_SECONDS = 20;
    private static final Duration MATCH_DURATION = Duration.ofMinutes(10);
    private static final Duration OVERTIME_DURATION = Duration.ofMinutes(2);
    private static final Duration WIN_CLEANUP_DURATION = Duration.ofSeconds(6);

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CTFPlayerContainer arenaPlayers;
    private final ScoreBoardManager scoreBoardManager;
    private final KitManager kitManager;
    private final BossBarManager bossBarManager;
    private final FlagManager flagManager;
    private final GameLoopManager gameLoopManager;

    private final GameStateManager gameStateManager;
    private final PlayerManager playerManager;
    private final FlagCarrierHandler flagCarrierHandler;
    private final FlagCaptureHandler flagCaptureHandler;
    private final DebugFeed debugFeed;
    private final WinnerCelebration winnerCelebration;

    private HomingSpearAbility homingSpearAbility;
    private ScoutTaggerAbility scoutTaggerAbility;
    private BaseMarkerService baseMarkerService;
    private BukkitTask waitingTask;
    private BukkitTask lonePlayerStopTask;
    private int scoreLimit = DEFAULT_SCORE_LIMIT;

    public CtfMatchOrchestrator(
        JavaPlugin plugin,
        TeamManager teamManager,
        ScoreBoardManager scoreBoardManager,
        KitManager kitManager,
        BossBarManager bossBarManager,
        DebugFeed debugFeed
    ) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.arenaPlayers = new CTFPlayerContainer(teamManager);
        this.scoreBoardManager = scoreBoardManager;
        this.kitManager = kitManager;
        this.bossBarManager = bossBarManager;
        this.gameStateManager = new GameStateManager();
        this.gameLoopManager = new GameLoopManager(plugin);
        this.playerManager = new PlayerManager(teamManager, kitManager, scoreBoardManager);
        this.flagManager = new FlagManager(plugin, teamManager, arenaPlayers, bossBarManager, this::handleCapture, debugFeed::send);
        this.flagCarrierHandler = new FlagCarrierHandler(teamManager, flagManager);
        this.debugFeed = debugFeed;
        this.winnerCelebration = new WinnerCelebration(plugin, teamManager, gameStateManager::isCleanupInProgress);
        this.flagCaptureHandler = new FlagCaptureHandler(
            this::getScoreLimit,
            scoreBoardManager,
            teamManager,
            arenaPlayers,
            gameStateManager,
            playerManager,
            gameLoopManager::getRemainingTime,
            this::stop,
            this::sendDebug
        );
        startLobbyTask();
    }

    /**
     * Returns true while gameplay is active.
     */
    public boolean isRunning() {
        return !gameStateManager.isCleanupInProgress() && gameStateManager.isRunning();
    }

    public GameState getGameState() {
        return gameStateManager.getGameState();
    }

    public int getScoreLimit() {
        return scoreLimit;
    }

    public int setScoreLimit(int newLimit) {
        scoreLimit = Math.max(1, newLimit);
        scoreBoardManager.setScoreLimit(scoreLimit);
        scoreBoardManager.updateScoreboards(gameLoopManager.getRemainingTime(), gameStateManager.getGameState());
        return scoreLimit;
    }

    public int setTeamScore(String teamKey, int score) {
        int applied = scoreBoardManager.setScore(teamKey, score);
        scoreBoardManager.updateScoreboards(gameLoopManager.getRemainingTime(), gameStateManager.getGameState());
        return applied;
    }


    public boolean hasEnoughPlayers() {
        return teamManager.getJoinedPlayerCount() >= MIN_PLAYERS;
    }

    public boolean isJoinLocked() {
        return gameStateManager.isCleanupInProgress();
    }

    public String resolveJoinTeamKey(String requestedTeamKey) {
        if (requestedTeamKey == null || requestedTeamKey.isBlank()) {
            return teamManager.getBalancedTeamKey(TeamManager.RED);
        }

        String normalized = teamManager.normalizeKey(requestedTeamKey);
        if (normalized == null) {
            return null;
        }

        if (isRunning()) {
            return teamManager.getBalancedTeamKey(normalized);
        }
        return normalized;
    }

    /**
     * Requests match start through countdown flow.
     */
    public boolean start() {
        return start(false);
    }

    /**
     * Requests match start through countdown flow.
     * force=true uses shorter countdown and ignores player minimum.
     */
    public boolean start(boolean force) {
        if (gameStateManager.isCleanupInProgress()
            || gameStateManager.getGameState() != GameState.LOBBY
            || !flagManager.areBasesReady()) {
            return false;
        }
        if (teamManager.getJoinedPlayerCount() <= 0) {
            return false;
        }
        if (gameLoopManager.isStartCountdownActive()) {
            return true;
        }
        if (!force && !hasEnoughPlayers()) {
            return false;
        }

        gameStateManager.setForcedCountdown(force);
        int countdown = force ? FORCE_START_COUNTDOWN_SECONDS : AUTO_START_COUNTDOWN_SECONDS;
        beginCountdown(countdown, force);
        return true;
    }

    public void stop(MatchStopReason reason) {
        stop(reason, null);
    }

    /**
     * Stops active match or pending countdown and returns to lobby.
     */
    public void stop(MatchStopReason reason, String winningTeamKey) {
        MatchStopReason resolved = reason == null ? MatchStopReason.GENERIC : reason;
        boolean hadMatch = isRunning() || gameStateManager.isCleanupInProgress();
        boolean hadCountdown = gameLoopManager.isStartCountdownActive();
        if (!hadMatch && !hadCountdown) {
            return;
        }

        if (resolved == MatchStopReason.WIN && winningTeamKey != null && !gameStateManager.isCleanupInProgress()) {
            beginWinnerCleanup(winningTeamKey);
            return;
        }

        finishStop(resolved, winningTeamKey);
    }

    public void shutdown() {
        gameStateManager.setCleanupInProgress(true);
        stopLoopTasksAndCountdowns(true);
        cleanupRuntimeArtifacts(false);
        resetToLobbyState(List.of());
        debugFeed.clear();
        gameStateManager.resetCountdownState();
        gameStateManager.setCleanupInProgress(false);
    }

    /**
     * Assigns player to team and teleports based on current game state.
     */
    public void joinPlayer(Player player, String teamKey) {
        if (player == null || teamKey == null || gameStateManager.isCleanupInProgress()) {
            return;
        }

        playerManager.joinArenaPlayer(player, teamKey, gameStateManager.getGameState(), gameLoopManager.getRemainingTime());
        flagManager.syncIndicatorVisibility();

        if (isRunning() && teamManager.getJoinedPlayerCount() >= MIN_PLAYERS) {
            cancelLonePlayerStopCountdown(true);
        }

        sendDebug("join player=" + player.getName() + " team=" + teamKey + " state=" + gameStateManager.getGameState().name());
    }

    /**
     * Handles leave-side effects and optional return teleport.
     */
    public void handlePlayerLeave(Player player, boolean restoreLocation) {
        if (player == null) {
            return;
        }

        flagManager.dropFlagIfCarrier(player);
        flagManager.clearCarrierItem(player);
        if (homingSpearAbility != null) {
            homingSpearAbility.handlePlayerLeave(player.getUniqueId());
        }
        if (scoutTaggerAbility != null) {
            scoutTaggerAbility.handlePlayerLeave(player);
        }
        playerManager.leaveArenaPlayer(player, restoreLocation, gameLoopManager.getRemainingTime(), gameStateManager.getGameState());
        flagManager.syncIndicatorVisibility();

        if (isRunning()) {
            int joinedCount = teamManager.getJoinedPlayerCount();
            if (joinedCount <= 0) {
                stop(MatchStopReason.GENERIC);
            } else if (joinedCount == 1) {
                startLonePlayerStopCountdown();
            }
        }

        sendDebug("leave player=" + player.getName() + " state=" + gameStateManager.getGameState().name());
    }

    public boolean setTeamSpawn(Player player, String teamKey) {
        if (player == null || teamKey == null) {
            return false;
        }
        teamManager.setSpawn(teamKey, player.getLocation());
        return true;
    }

    public boolean addTeamReturnPoint(Player player, String teamKey) {
        if (player == null || teamKey == null) {
            return false;
        }
        return teamManager.addReturnPoint(teamKey, player.getLocation());
    }

    public boolean setLobbySpawn(Player player) {
        if (player == null) {
            return false;
        }
        teamManager.setLobbySpawn(player.getLocation());
        return true;
    }

    public boolean handleFlagTouch(Player player, Location blockLocation) {
        return flagCarrierHandler.handleFlagTouch(player, blockLocation, isRunning());
    }

    public void handleMove(Player player, Location to) {
        flagCarrierHandler.handleMove(player, to, isRunning());
    }

    public FlagManager getFlagManager() {
        return flagManager;
    }

    public Location getRespawnLocation(Player player) {
        if (player == null) {
            return null;
        }
        return teamManager.getSpawnFor(player).orElse(null);
    }

    public boolean isPlayerInGame(Player player) {
        return player != null && teamManager.getTeamKey(player) != null;
    }

    /**
     * Returns the maximum allowed match time (seconds) for the current state.
     */
    public int getAllowedMatchTimeSeconds() {
        GameState state = gameStateManager.getGameState();
        if (state == GameState.OVERTIME) {
            return (int) OVERTIME_DURATION.toSeconds();
        }
        return (int) MATCH_DURATION.toSeconds();
    }

    /**
     * Updates the remaining match time when a match is active.
     */
    public boolean setMatchTimeSeconds(long seconds) {
        if (!gameStateManager.isRunning()) {
            return false;
        }
        long clampedSeconds = Math.max(0L, seconds);
        Duration remaining = Duration.ofSeconds(clampedSeconds);
        if (!gameLoopManager.setRemainingTime(remaining)) {
            return false;
        }
        scoreBoardManager.updateScoreboards(remaining, gameStateManager.getGameState());
        gameStateManager.setLastEndCountdownAnnounced(-1L);
        return true;
    }

    public void broadcastToArena(Component message) {
        arenaPlayers.broadcast(message);
    }

    public void setHomingSpearAbility(HomingSpearAbility ability) {
        this.homingSpearAbility = ability;
    }

    public void setScoutTaggerAbility(ScoutTaggerAbility ability) {
        this.scoutTaggerAbility = ability;
    }

    public void setBaseMarkerService(BaseMarkerService markerManager) {
        this.baseMarkerService = markerManager;
    }

    /**
     * Increments killer stat and refreshes scoreboards.
     */
    public void recordKill(Player killer) {
        playerManager.recordKill(killer, gameLoopManager.getRemainingTime(), gameStateManager.getGameState());
    }

    /**
     * Increments death stat and refreshes scoreboards.
     */
    public void recordDeath(Player victim) {
        playerManager.recordDeath(victim, gameLoopManager.getRemainingTime(), gameStateManager.getGameState());
    }

    /**
     * Returns per-match stats for a player.
     */
    public PlayerMatchStats getPlayerStats(UUID playerId) {
        return playerManager.getPlayerStats(playerId);
    }

    private void sendDebug(String message) {
        debugFeed.send(message);
    }

    public void publishDebug(String message) {
        debugFeed.send(message);
    }

    private int handleCapture(Player player, String scoringTeam, String capturedFlagTeam) {
        return flagCaptureHandler.handleCapture(player, scoringTeam, capturedFlagTeam);
    }

    private void beginCountdown(int seconds, boolean force) {
        gameStateManager.setActiveStartCountdownTotal(Math.max(1, seconds));
        gameLoopManager.startMatchCountdown(
            seconds,
            () -> gameStateManager.getGameState() == GameState.LOBBY
                && !gameStateManager.isCleanupInProgress()
                && flagManager.areBasesReady()
                && (force || hasEnoughPlayers()),
            this::handleCountdownTick,
            this::beginMatch,
            this::handleCountdownAbort
        );

        if (force) {
            broadcast(msg("broadcast.countdown.force", Map.of(
                "seconds", Integer.toString(seconds)
            )));
            return;
        }
        broadcast(msg("broadcast.countdown.auto", Map.of(
            "seconds", Integer.toString(seconds)
        )));
    }

    private void handleCountdownTick(int seconds) {
        if (seconds <= 10) {
            broadcast(msg("broadcast.countdown.tick", Map.of(
                "seconds", Integer.toString(seconds)
            )));
        }

        int elapsed = gameStateManager.getActiveStartCountdownTotal() - seconds;
        float pitch = Math.min(2.0f, 0.6f + (elapsed * 0.08f));
        playSoundToJoined(Sound.BLOCK_NOTE_BLOCK_HAT, pitch);
        sendDebug("start-countdown t=" + seconds);
    }

    private void handleCountdownAbort() {
        gameStateManager.setActiveStartCountdownTotal(0);
        if (gameStateManager.isForcedCountdown()) {
            return;
        }
        broadcast(msg("broadcast.countdown.cancelled"));
        sendDebug("start-countdown aborted");
    }

    private void beginMatch() {
        if (gameStateManager.isCleanupInProgress() || gameStateManager.getGameState() != GameState.LOBBY) {
            return;
        }
        if (!gameStateManager.isForcedCountdown() && !hasEnoughPlayers()) {
            return;
        }
        if (!flagManager.areBasesReady()) {
            return;
        }

        gameStateManager.setForcedCountdown(false);
        gameStateManager.setActiveStartCountdownTotal(0);
        gameStateManager.setLastEndCountdownAnnounced(-1L);
        cancelLonePlayerStopCountdown(false);

        gameStateManager.setGameState(GameState.IN_PROGRESS);
        scoreBoardManager.resetScores();
        scoreBoardManager.setScoreLimit(scoreLimit);
        scoreBoardManager.updateScoreboards(MATCH_DURATION, gameStateManager.getGameState());
        playerManager.teleportPlayersToTeamSpawns();
        flagManager.onMatchStart();
        kitManager.applyKit(teamManager.getJoinedPlayers());
        playerManager.applyMatchVitals(teamManager.getJoinedPlayers());
        if (scoutTaggerAbility != null) {
            scoutTaggerAbility.startForMatch();
        }
        if (baseMarkerService != null) {
            Location redBase = flagManager.getBaseLocation(TeamManager.RED);
            if (redBase != null) {
                baseMarkerService.spawnOrMoveBaseMarker(TeamManager.RED, redBase);
            }
            Location blueBase = flagManager.getBaseLocation(TeamManager.BLUE);
            if (blueBase != null) {
                baseMarkerService.spawnOrMoveBaseMarker(TeamManager.BLUE, blueBase);
            }
        }
        playMatchStartEffects();
        broadcast(msg("broadcast.start"));
        sendDebug("match started");

        gameLoopManager.startMatchTimer(
            MATCH_DURATION,
            this::onTimerTick,
            this::handleTimerEnd
        );
    }

    private void onTimerTick(Duration remaining) {
        scoreBoardManager.updateScoreboards(remaining, gameStateManager.getGameState());
        playEndCountdownSounds(remaining);
    }

    private void handleTimerEnd() {
        int red = scoreBoardManager.getScore(TeamManager.RED);
        int blue = scoreBoardManager.getScore(TeamManager.BLUE);

        if (gameStateManager.getGameState() == GameState.IN_PROGRESS && red == blue) {
            enterOvertime();
            return;
        }

        if (red > blue) {
            stop(MatchStopReason.WIN, TeamManager.RED);
            return;
        }

        if (blue > red) {
            stop(MatchStopReason.WIN, TeamManager.BLUE);
            return;
        }

        stop(MatchStopReason.TIMEOUT);
    }

    private void enterOvertime() {
        gameStateManager.setGameState(GameState.OVERTIME);
        gameStateManager.setLastEndCountdownAnnounced(-1L);
        broadcast(msg("broadcast.overtime.start"));
        playOvertimeReveal();
        sendDebug("overtime started");
        gameLoopManager.startMatchTimer(
            OVERTIME_DURATION,
            this::onTimerTick,
            this::handleTimerEnd
        );
    }

    private void playOvertimeReveal() {
        arenaPlayers.playSound(Sound.AMBIENT_CAVE, 1.0f, 0.6f);

        String full = "OVERTIME";
        int steps = full.length();
        int totalTicks = 46;
        int stepTicks = Math.max(1, totalTicks / steps);
        Title.Times stepTimes = Title.Times.times(Duration.ZERO, Duration.ofMillis(stepTicks * 50L), Duration.ZERO);
        Title.Times finalTimes = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);

        for (int i = 1; i <= steps; i++) {
            int revealed = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (gameStateManager.getGameState() != GameState.OVERTIME) {
                    return;
                }

                String left = full.substring(0, revealed);
                String right = full.substring(revealed);
                Component title = Component.text(left, NamedTextColor.RED, TextDecoration.BOLD);
                if (!right.isEmpty()) {
                    title = title.append(Component.text(right, NamedTextColor.RED, TextDecoration.BOLD, TextDecoration.OBFUSCATED));
                }

                Component subtitle = Component.empty();
                if (revealed == steps) {
                    subtitle = Component.text("Sudden Death: First to score wins", NamedTextColor.GOLD);
                    arenaPlayers.playSound(Sound.BLOCK_BELL_RESONATE, 1.0f, 1.0f);
                }

                Title reveal = Title.title(title, subtitle, revealed == steps ? finalTimes : stepTimes);
                arenaPlayers.broadcastTitle(reveal);
            }, (long) i * stepTicks);
        }
    }

    private void startLobbyTask() {
        if (waitingTask != null) {
            return;
        }

        LobbyWaitingTask task = new LobbyWaitingTask(
            MIN_PLAYERS,
            teamManager,
            bossBarManager,
            gameStateManager,
            gameLoopManager,
            flagManager,
            () -> beginCountdown(AUTO_START_COUNTDOWN_SECONDS, false)
        );
        waitingTask = Bukkit.getScheduler().runTaskTimer(plugin, task, 0L, 20L);
    }

    private void stopLobbyTask() {
        if (waitingTask != null) {
            waitingTask.cancel();
            waitingTask = null;
        }
    }

    private void playEndCountdownSounds(Duration remaining) {
        long seconds = remaining.toSeconds();
        if (seconds == 60) {
            broadcast(msg("broadcast.end_soon"));
        }

        if (seconds <= 10 && seconds > 0 && seconds != gameStateManager.getLastEndCountdownAnnounced()) {
            gameStateManager.setLastEndCountdownAnnounced(seconds);
            broadcast(msg("broadcast.end_countdown", Map.of(
                "seconds", Long.toString(seconds)
            )));
            float pitch = Math.max(0.5f, 1.7f - ((10 - seconds) * 0.12f));
            playSoundToJoined(Sound.BLOCK_NOTE_BLOCK_BASS, pitch);
        }
    }

    private void playMatchStartEffects() {
        for (Player player : teamManager.getJoinedPlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
            player.spawnParticle(Particle.FIREWORK, player.getLocation().add(0.0, 1.0, 0.0), 25, 0.4, 0.6, 0.4, 0.02);
        }
    }

    private void playSoundToJoined(Sound sound, float pitch) {
        for (Player player : teamManager.getJoinedPlayers()) {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }

    private void broadcast(Component message) {
        arenaPlayers.broadcast(message);
    }

    private void beginWinnerCleanup(String winningTeamKey) {
        if (winningTeamKey == null) {
            finishStop(MatchStopReason.WIN, null);
            return;
        }

        gameStateManager.setCleanupInProgress(true);
        gameStateManager.setForcedCountdown(false);
        gameStateManager.setLastEndCountdownAnnounced(-1L);
        stopLoopTasksAndCountdowns();
        cleanupRuntimeArtifacts(true);

        Component winMessage = msg("broadcast.win", Map.of(
            "team", teamManager.getDisplayName(winningTeamKey)
        ));
        broadcast(winMessage);

        Component title = Component.text(teamManager.getDisplayName(winningTeamKey) + " Wins!", TeamManager.RED.equals(winningTeamKey)
            ? NamedTextColor.RED : NamedTextColor.BLUE, TextDecoration.BOLD);
        Component subtitle = Component.text("Celebration time!", NamedTextColor.GOLD);
        arenaPlayers.broadcastTitle(Title.title(title, subtitle));
        arenaPlayers.playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.1f);
        winnerCelebration.start(winningTeamKey, WIN_CLEANUP_DURATION.toMillis());
        sendDebug("cleanup celebration winner=" + winningTeamKey);

        Bukkit.getScheduler().runTaskLater(plugin, () -> finishStop(MatchStopReason.WIN, winningTeamKey),
            WIN_CLEANUP_DURATION.toSeconds() * 20L);
    }

    private void finishStop(MatchStopReason reason, String winningTeamKey) {
        gameStateManager.setCleanupInProgress(true);
        winnerCelebration.cancel();
        stopLoopTasksAndCountdowns();
        gameStateManager.resetCountdownState();

        List<Player> joinedPlayers = new ArrayList<>(teamManager.getJoinedPlayers());
        MatchStopReason resolved = reason == null ? MatchStopReason.GENERIC : reason;
        if (resolved != MatchStopReason.WIN) {
            cleanupRuntimeArtifacts(false);
        }

        Component stopMessage = switch (resolved) {
            case ADMIN -> msg("broadcast.stop.admin");
            case TIMEOUT -> msg("broadcast.stop.timeout");
            case WIN -> msg("broadcast.win", Map.of(
                "team", teamManager.getDisplayName(winningTeamKey)
            ));
            case GENERIC -> msg("broadcast.stop.generic");
        };
        for (Player player : joinedPlayers) {
            player.sendMessage(stopMessage);
        }
        broadcastMatchReady();

        resetToLobbyState(joinedPlayers);
        flagManager.resetFlagsToBase();
        gameStateManager.setCleanupInProgress(false);
        sendDebug("match stopped reason=" + resolved.name());
    }

    private void broadcastMatchReady() {
        Component ready = msg("broadcast.match_ready");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ready);
        }
    }

    private void startLonePlayerStopCountdown() {
        if (lonePlayerStopTask != null || !isRunning()) {
            return;
        }

        broadcast(msg("broadcast.lone_player.start", Map.of(
            "seconds", Integer.toString(LONE_PLAYER_END_SECONDS)
        )));
        sendDebug("lone-player countdown started");

        LonePlayerEndTask task = new LonePlayerEndTask(
            LONE_PLAYER_END_SECONDS,
            MIN_PLAYERS,
            teamManager::getJoinedPlayerCount,
            this::isRunning,
            () -> cancelLonePlayerStopCountdown(true),
            () -> stop(MatchStopReason.GENERIC),
            () -> cancelLonePlayerStopCountdown(false),
            secondsRemaining -> {
                broadcast(msg("broadcast.lone_player.tick", Map.of(
                    "seconds", Integer.toString(secondsRemaining)
                )));
                float pitch = Math.max(0.5f, 1.5f - ((10 - secondsRemaining) * 0.1f));
                playSoundToJoined(Sound.BLOCK_NOTE_BLOCK_BASS, pitch);
            }
        );

        lonePlayerStopTask = Bukkit.getScheduler().runTaskTimer(plugin, task, 0L, 20L);
    }

    private void cancelLonePlayerStopCountdown(boolean announce) {
        if (lonePlayerStopTask != null) {
            lonePlayerStopTask.cancel();
            lonePlayerStopTask = null;
            if (announce) {
                broadcast(msg("broadcast.lone_player.cancelled"));
                sendDebug("lone-player countdown cancelled");
            }
        }
    }

    private void stopLoopTasksAndCountdowns() {
        stopLoopTasksAndCountdowns(false);
    }

    private void stopLoopTasksAndCountdowns(boolean stopWaitingTask) {
        gameLoopManager.stopAll();
        cancelLonePlayerStopCountdown(false);
        if (stopWaitingTask) {
            stopLobbyTask();
        }
    }

    private void cleanupRuntimeArtifacts(boolean keepCelebration) {
        if (!keepCelebration) {
            winnerCelebration.cancel();
        }
        flagManager.clearCarrierItems();
        flagManager.onMatchStop();
        if (homingSpearAbility != null) {
            homingSpearAbility.clearAll();
        }
        if (scoutTaggerAbility != null) {
            scoutTaggerAbility.stopAll();
        }
        if (baseMarkerService != null) {
            baseMarkerService.removeAllMarkers();
        }
        bossBarManager.clearAll();
    }

    private void resetToLobbyState(List<Player> joinedPlayers) {
        scoreBoardManager.resetScores();
        scoreBoardManager.updateScoreboards(Duration.ZERO, GameState.LOBBY);
        kitManager.restoreAll();
        kitManager.clearAll();
        if (joinedPlayers != null && !joinedPlayers.isEmpty()) {
            playerManager.teleportPlayersToLobby(joinedPlayers);
        }
        scoreBoardManager.clearAll();
        scoreBoardManager.clearStats();
        teamManager.clearAllTeams();
        playerManager.clearState();
        gameStateManager.setGameState(GameState.LOBBY);
    }

}




