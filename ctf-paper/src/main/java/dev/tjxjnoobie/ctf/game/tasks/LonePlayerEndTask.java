package dev.tjxjnoobie.ctf.game.tasks;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Ends active matches when player count stays below minimum for too long.
 */
public final class LonePlayerEndTask implements Runnable {
    private final int minPlayers;
    private final IntSupplier joinedPlayerCountSupplier;
    private final BooleanSupplier runningSupplier;
    private final Runnable onRecoveredPlayers;
    private final Runnable onStopGame;
    private final Runnable onCancelNoAnnouncement;
    private final IntConsumer onTenSecondTick;

    private int secondsRemaining;
    private boolean finished;

    public LonePlayerEndTask(
        int startSeconds,
        int minPlayers,
        IntSupplier joinedPlayerCountSupplier,
        BooleanSupplier runningSupplier,
        Runnable onRecoveredPlayers,
        Runnable onStopGame,
        Runnable onCancelNoAnnouncement,
        IntConsumer onTenSecondTick
    ) {
        this.secondsRemaining = Math.max(0, startSeconds);
        this.minPlayers = minPlayers;
        this.joinedPlayerCountSupplier = joinedPlayerCountSupplier;
        this.runningSupplier = runningSupplier;
        this.onRecoveredPlayers = onRecoveredPlayers;
        this.onStopGame = onStopGame;
        this.onCancelNoAnnouncement = onCancelNoAnnouncement;
        this.onTenSecondTick = onTenSecondTick;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    @Override
    public void run() {
        if (finished) {
            return;
        }

        if (!runningSupplier.getAsBoolean()) {
            finish();
            onCancelNoAnnouncement.run();
            return;
        }

        int joinedCount = joinedPlayerCountSupplier.getAsInt();
        if (joinedCount >= minPlayers) {
            finish();
            onRecoveredPlayers.run();
            return;
        }

        if (joinedCount <= 0 || secondsRemaining <= 0) {
            finish();
            onCancelNoAnnouncement.run();
            onStopGame.run();
            return;
        }

        if (secondsRemaining <= 10) {
            onTenSecondTick.accept(secondsRemaining);
        }
        secondsRemaining--;
    }

    private void finish() {
        finished = true;
    }
}

