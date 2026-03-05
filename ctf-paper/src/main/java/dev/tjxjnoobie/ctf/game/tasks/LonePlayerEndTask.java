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

    private final CountDownTimer countdown;
    private boolean finished;

    // == Lifecycle ==
    /**
     * Constructs a LonePlayerEndTask instance.
     *
     * @param startSeconds Numeric value used by this operation.
     * @param minPlayers Numeric value used by this operation.
     * @param joinedPlayerCountSupplier Supplier that returns the current joined-player count.
     * @param runningSupplier Dependency responsible for running supplier.
     * @param onRecoveredPlayers Callback or dependency used during processing.
     * @param onStopGame Callback or dependency used during processing.
     * @param onCancelNoAnnouncement Callback or dependency used during processing.
     * @param onTenSecondTick Callback or dependency used during processing.
     */
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
        // Initialize countdown and callbacks.
        this.countdown = new CountDownTimer(startSeconds);
        this.minPlayers = minPlayers;
        this.joinedPlayerCountSupplier = joinedPlayerCountSupplier;
        this.runningSupplier = runningSupplier;
        this.onRecoveredPlayers = onRecoveredPlayers;
        this.onStopGame = onStopGame;
        this.onCancelNoAnnouncement = onCancelNoAnnouncement;
        this.onTenSecondTick = onTenSecondTick;
    }

    // == Getters ==
    public int getSecondsRemaining() {
        // Return the remaining countdown time.
        return countdown.getSecondsRemaining();
    }

    // == Utilities ==
    /**
     * Executes the run operation.
     */
    @Override
    public void run() {
        // Guard: short-circuit when finished.
        if (finished) {
            return;
        }

        boolean getAsBooleanResult = runningSupplier.getAsBoolean(); // Cancel if the match is no longer running.
        if (!getAsBooleanResult) {
            finish();
            onCancelNoAnnouncement.run();
            return;
        }

        int joinedCount = joinedPlayerCountSupplier.getAsInt();
        if (joinedCount >= minPlayers) {
            // Enough players returned; cancel countdown.
            finish();
            onRecoveredPlayers.run();
            return;
        }

        if (joinedCount <= 0) {
            // Stop match if no players remain or timer expired.
            finish();
            onCancelNoAnnouncement.run();
            onStopGame.run();
            return;
        }

        int remaining = countdown.getSecondsRemaining(); // Broadcast final 10-second countdown ticks.
        if (remaining <= 10) {
            onTenSecondTick.accept(remaining);
        }
        countdown.tick(null, () -> {
            finish();
            onCancelNoAnnouncement.run();
            onStopGame.run();
        });
    }

    private void finish() {
        finished = true; // Mark the countdown as completed to stop future runs.
        countdown.cancel();
    }
}

