package dev.tjxjnoobie.ctf.game;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns pre-game countdowns and active match timers.
 */
public final class GameLoopManager {
    private final JavaPlugin plugin;

    private BukkitTask startCountdownTask;
    private BukkitTask matchTimerTask;
    private volatile long matchEndMillis;
    private int countdownSecondsRemaining;

    public GameLoopManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts a pre-match countdown.
     */
    public void startMatchCountdown(
        int seconds,
        BooleanSupplier stillValid,
        Consumer<Integer> tickCallback,
        Runnable completeCallback,
        Runnable abortCallback
    ) {
        cancelStartCountdown();
        countdownSecondsRemaining = Math.max(0, seconds);

        startCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (stillValid != null && !stillValid.getAsBoolean()) {
                cancelStartCountdown();
                if (abortCallback != null) {
                    abortCallback.run();
                }
                return;
            }

            if (countdownSecondsRemaining <= 0) {
                cancelStartCountdown();
                if (completeCallback != null) {
                    completeCallback.run();
                }
                return;
            }

            if (tickCallback != null) {
                tickCallback.accept(countdownSecondsRemaining);
            }
            countdownSecondsRemaining--;
        }, 0L, 20L);
    }

    /**
     * Returns true when a start countdown is in progress.
     */
    public boolean isStartCountdownActive() {
        return startCountdownTask != null;
    }

    /**
     * Cancels the pre-match countdown.
     */
    public void cancelStartCountdown() {
        if (startCountdownTask != null) {
            startCountdownTask.cancel();
            startCountdownTask = null;
        }
        countdownSecondsRemaining = 0;
    }

    /**
     * Starts a repeating match timer.
     */
    public void startMatchTimer(Duration duration, Consumer<Duration> tickCallback, Runnable timeoutCallback) {
        stopMatchTimer();
        long durationMillis = Math.max(0L, duration == null ? 0L : duration.toMillis());
        matchEndMillis = System.currentTimeMillis() + durationMillis;

        matchTimerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long remainingMillis = Math.max(0L, matchEndMillis - System.currentTimeMillis());
            Duration remaining = Duration.ofMillis(remainingMillis);

            if (tickCallback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> tickCallback.accept(remaining));
            }

            if (remainingMillis <= 0L) {
                stopMatchTimer();
                if (timeoutCallback != null) {
                    Bukkit.getScheduler().runTask(plugin, timeoutCallback);
                }
            }
        }, 0L, 20L);
    }

    /**
     * Returns the remaining duration of the active match timer.
     */
    public Duration getRemainingTime() {
        if (matchTimerTask == null) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(Math.max(0L, matchEndMillis - System.currentTimeMillis()));
    }

    /**
     * Updates the remaining time for the active match timer.
     */
    public boolean setRemainingTime(Duration duration) {
        if (matchTimerTask == null) {
            return false;
        }
        long durationMillis = Math.max(0L, duration == null ? 0L : duration.toMillis());
        matchEndMillis = System.currentTimeMillis() + durationMillis;
        return true;
    }

    /**
     * Stops the active match timer.
     */
    public void stopMatchTimer() {
        if (matchTimerTask != null) {
            matchTimerTask.cancel();
            matchTimerTask = null;
        }
        matchEndMillis = 0L;
    }

    /**
     * Stops all active timers.
     */
    public void stopAll() {
        cancelStartCountdown();
        stopMatchTimer();
    }
}

