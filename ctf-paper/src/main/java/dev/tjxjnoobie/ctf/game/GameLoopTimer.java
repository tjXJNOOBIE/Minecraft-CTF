package dev.tjxjnoobie.ctf.game;

import dev.tjxjnoobie.ctf.game.tasks.CountDownTimer;
import dev.tjxjnoobie.ctf.util.tasks.GameTaskOrchestrator;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns pre-game countdowns and active match timers.
 */
public final class GameLoopTimer {

    // == Runtime state ==
    private BukkitTask startCountdownTask;
    private BukkitTask matchTimerTask;
    private long matchEndMillis;
    private CountDownTimer startCountdownTimer;

    // == Lifecycle ==
    /**
     * Constructs a GameLoopTimer instance.
     */
    public GameLoopTimer() {
    }

    // == Countdown timers ==
    /**
     * Executes the startMatchCountdown operation.
     *
     * @param seconds Number of seconds to count down before match start.
     * @param stillValid Predicate checked each tick to decide whether countdown should continue.
     * @param tickCallback Callback or dependency used during processing.
     * @param completeCallback Callback invoked when countdown reaches zero.
     * @param abortCallback Callback invoked when countdown is cancelled by {@code stillValid}.
     */
    public void startMatchCountdown(
            int seconds,
            BooleanSupplier stillValid,
            Consumer<Integer> tickCallback,
            Runnable completeCallback,
            Runnable abortCallback) {
        // Reset any existing countdown before starting a new one.
        cancelStartCountdown();
        startCountdownTimer = new CountDownTimer(seconds);

        startCountdownTask = GameTaskOrchestrator.startTimer(startCountdownTask, () -> {
            boolean conditionResult1 = stillValid != null && !stillValid.getAsBoolean();
            if (conditionResult1) {
                cancelStartCountdown();
                if (abortCallback != null) {
                    abortCallback.run();
                }
                return;
            }

            if (startCountdownTimer != null) {
                startCountdownTimer.tick(
                    remaining -> {
                        if (tickCallback != null) {
                            tickCallback.accept(remaining);
                        }
                    },
                    () -> {
                        cancelStartCountdown();
                        if (completeCallback != null) {
                            completeCallback.run();
                        }
                    }
                );
            }
        }, 0L, 20L);
    }

    // == Match timers ==
    /**
     * Executes the startMatchTimer operation.
     *
     * @param duration Duration value used by this operation.
     * @param tickCallback Callback or dependency used during processing.
     * @param timeoutCallback Callback invoked when match timer reaches zero.
     */
    public void startMatchTimer(Duration duration, Consumer<Duration> tickCallback, Runnable timeoutCallback) {
        // Ensure only one active match timer exists.
        stopMatchTimer();
        long durationMillis = Math.max(0L, duration == null ? 0L : duration.toMillis());
        matchEndMillis = System.currentTimeMillis() + durationMillis;

        matchTimerTask = GameTaskOrchestrator.startTimer(matchTimerTask, () -> {
            long remainingMillis = Math.max(0L, matchEndMillis - System.currentTimeMillis());
            Duration remaining = Duration.ofMillis(remainingMillis);

            if (tickCallback != null) {
                tickCallback.accept(remaining);
            }

            if (remainingMillis <= 0L) {
                stopMatchTimer();
                if (timeoutCallback != null) {
                    timeoutCallback.run();
                }
            }
        }, 0L, 20L);
    }

    /**
     * Executes the stopMatchTimer operation.
     */
    public void stopMatchTimer() {
        if (matchTimerTask != null) {
            matchTimerTask = GameTaskOrchestrator.cancel(matchTimerTask);
        }
        matchEndMillis = 0L;
    }

    /**
     * Executes the stopAllTimers operation.
     */
    public void stopAllTimers() {
        cancelStartCountdown();
        stopMatchTimer();
    }

    // == Getters ==
    public Duration getRemainingTime() {
        // Guard: short-circuit when matchTimerTask == null.
        if (matchTimerTask == null) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(Math.max(0L, matchEndMillis - System.currentTimeMillis()));
    }

    // == Setters ==

    // == Setters ==
    /**
     * Updates state for setRemainingTime.
     *
     * @param duration Duration value used by this operation.
     * @return {@code true} when remaining time was updated; otherwise {@code false}.
     */
    public boolean setRemainingTime(Duration duration) {
        // Guard: short-circuit when matchTimerTask == null.
        if (matchTimerTask == null) {
            return false;
        }
        long durationMillis = Math.max(0L, duration == null ? 0L : duration.toMillis());
        matchEndMillis = System.currentTimeMillis() + durationMillis;
        return true;
    }

    // == Utilities ==
    /**
     * Evaluates whether cancelStartCountdown is currently satisfied.
     */
    public void cancelStartCountdown() {
        if (startCountdownTask != null) {
            startCountdownTask = GameTaskOrchestrator.cancel(startCountdownTask);
        }
        if (startCountdownTimer != null) {
            startCountdownTimer.cancel();
            startCountdownTimer = null;
        }
    }

    // == Predicates ==

    /**
     * Evaluates whether isStartCountdownActive is currently satisfied.
     *
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    public boolean isStartCountdownActive() {
        return startCountdownTask != null;
    }
}