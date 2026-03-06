package dev.tjxjnoobie.ctf.game.tasks;

import java.util.function.IntConsumer;

/**
 * Simple second-based countdown helper.
 */
public final class CountDownTimer {
    private int secondsRemaining;
    private boolean finished;

    // == Lifecycle ==
    /**
     * Constructs a CountDownTimer instance.
     *
     * @param startSeconds Numeric value used by this operation.
     */
    public CountDownTimer(int startSeconds) {
        reset(startSeconds);
    }

    // == Getters ==
    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    // == Utilities ==
    /**
     * Executes reset.
     *
     * @param startSeconds Numeric value used by this operation.
     */
    public void reset(int startSeconds) {
        this.secondsRemaining = Math.max(0, startSeconds);
        this.finished = false;
    }

    /**
     * Evaluates whether cancel is currently satisfied.
     */
    public void cancel() {
        finished = true;
    }

    /**
     * Returns the result of tick.
     *
     * @param onTick Callback or dependency used during processing.
     * @param onComplete Callback or dependency used during processing.
     * @return Result produced by this method.
     */
    public boolean tick(IntConsumer onTick, Runnable onComplete) {
        // Guard: short-circuit when finished.
        if (finished) {
            return true;
        }
        if (secondsRemaining <= 0) {
            finished = true;
            if (onComplete != null) {
                onComplete.run();
            }
            return true;
        }
        if (onTick != null) {
            onTick.accept(secondsRemaining);
        }
        secondsRemaining--;
        return false;
    }

    // == Predicates ==
    public boolean isFinished() {
        return finished;
    }
}
