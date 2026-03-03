package dev.tjxjnoobie.ctf.game.lifecycle.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.tasks.LonePlayerEndTask;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.tasks.GameTaskOrchestrator;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles lone-player match-end countdown lifecycle.
 */
public final class LonePlayerCountdownHandler implements MessageAccess, BukkitEffectsUtil, BukkitMessageSender {

    // == Constants ==
    private static final int MIN_PLAYERS = 2;
    private static final int LONE_PLAYER_END_SECONDS = 20;
    private final TeamManager teamManager;

    // == Runtime state ==
    private final Runnable requestMatchStopAction;
    private BukkitTask lonePlayerEndTimerTask;
    private final BooleanSupplier runningSupplier;
    private final Consumer<net.kyori.adventure.text.Component> broadcastConsumer;

    // == Lifecycle ==
    /**
     * Constructs a LonePlayerCountdownHandler instance.
     *
     * @param teamManager Dependency responsible for team manager.
     * @param runningSupplier Dependency responsible for running supplier.
     * @param requestMatchStopAction Callback executed by this operation.
     * @param broadcastConsumer Callback executed by this operation.
     */
    public LonePlayerCountdownHandler(
        TeamManager teamManager,
        BooleanSupplier runningSupplier,
        Runnable requestMatchStopAction,
        Consumer<net.kyori.adventure.text.Component> broadcastConsumer
    ) {
        this.teamManager = teamManager;
        this.runningSupplier = runningSupplier;
        this.requestMatchStopAction = requestMatchStopAction;
        this.broadcastConsumer = broadcastConsumer;
    }

    private void startLonePlayerEndTimer() {
        boolean conditionResult2 = lonePlayerEndTimerTask != null || !runningSupplier.getAsBoolean();
        // Guard: short-circuit when lonePlayerEndTimerTask != null || !runningSupplier.getAsBoolean().
        if (conditionResult2) {
            return;
        }

        String startSecondsText = Integer.toString(LONE_PLAYER_END_SECONDS);
        Component startMessage = getMessageFormatted("broadcast.lone_player.start", startSecondsText);
        broadcast(startMessage);
        sendDebugMessage("lone-player countdown started");

        LonePlayerEndTask task = new LonePlayerEndTask(
            LONE_PLAYER_END_SECONDS,
            MIN_PLAYERS,
            teamManager::getJoinedPlayerCount,
            runningSupplier,
            () -> cancelLonePlayerEndTimer(true),
            requestMatchStopAction,
            () -> cancelLonePlayerEndTimer(false),
            secondsRemaining -> {
                String tickSecondsText = Integer.toString(secondsRemaining);
                Component tickMessage = getMessageFormatted("broadcast.lone_player.tick", tickSecondsText);
                broadcast(tickMessage);
                float pitch = Math.max(0.5f, 1.5f - ((10 - secondsRemaining) * 0.1f));
                playSoundToJoined(Sound.BLOCK_NOTE_BLOCK_BASS, pitch);
            }
        );

        lonePlayerEndTimerTask = GameTaskOrchestrator.startTimer(lonePlayerEndTimerTask, task, 0L, 20L);
    }

    // == Utilities ==
    /**
     * Executes the onPlayerJoinedDuringMatch operation.
     */
    public void onPlayerJoinedDuringMatch() {
        boolean conditionResult1 = runningSupplier.getAsBoolean() && teamManager.getJoinedPlayerCount() >= MIN_PLAYERS;
        if (conditionResult1) {
            cancelLonePlayerEndTimer(true);
        }
    }

    /**
     * Executes the onPlayerLeftDuringMatch operation.
     */
    public void onPlayerLeftDuringMatch() {
        boolean getAsBooleanResult = runningSupplier.getAsBoolean();
        // Guard: short-circuit when !getAsBooleanResult.
        if (!getAsBooleanResult) {
            return;
        }

        int joinedCount = teamManager.getJoinedPlayerCount();
        if (joinedCount <= 0) {
            requestMatchStopAction.run();
        } else if (joinedCount == 1) {
            startLonePlayerEndTimer();
        }
    }

    /**
     * Evaluates whether cancelLonePlayerEndTimer is currently satisfied.
     *
     * @param announce Control flag that changes how this operation is executed.
     */
    public void cancelLonePlayerEndTimer(boolean announce) {
        // Guard: short-circuit when lonePlayerEndTimerTask == null.
        if (lonePlayerEndTimerTask == null) {
            return;
        }

        lonePlayerEndTimerTask = GameTaskOrchestrator.cancel(lonePlayerEndTimerTask);
        if (announce) {
            Component message = getMessage("broadcast.lone_player.cancelled");
            broadcast(message);
            sendDebugMessage("lone-player countdown cancelled");
        }
    }

    private void playSoundToJoined(Sound sound, float pitch) {
        playSoundToPlayers(teamManager.getJoinedPlayers(), sound, 1.0f, pitch);
    }

    private void broadcast(net.kyori.adventure.text.Component message) {
        broadcastConsumer.accept(message);
    }
}

