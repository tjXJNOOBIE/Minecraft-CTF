package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.tasks.CTFTaskRegistry;
import dev.tjxjnoobie.ctf.util.tasks.FastTickBus;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public interface TaskDependencyAccess {
    default GameLoopTimer getGameLoopTimer() { return DependencyLoaderAccess.findInstance(GameLoopTimer.class); }
    default CTFTaskRegistry getCTFTaskRegistry() { return DependencyLoaderAccess.findInstance(CTFTaskRegistry.class); }
    default FastTickBus getFastTickBus() { return DependencyLoaderAccess.findInstance(FastTickBus.class); }

    default Duration getRemainingTime() {
        return getGameLoopTimer().getRemainingTime();
    }

    default boolean setRemainingTime(Duration duration) {
        return getGameLoopTimer().setRemainingTime(duration);
    }

    default void startMatchTimer(Duration duration, Consumer<Duration> tickCallback, Runnable timeoutCallback) {
        getGameLoopTimer().startMatchTimer(duration, tickCallback, timeoutCallback);
    }

    default void startMatchCountdown(int seconds,
                                     BooleanSupplier stillValid,
                                     Consumer<Integer> tickCallback,
                                     Runnable completeCallback,
                                     Runnable abortCallback) {
        getGameLoopTimer().startMatchCountdown(seconds, stillValid, tickCallback, completeCallback, abortCallback);
    }

    default void stopAllTimers() {
        getGameLoopTimer().stopAllTimers();
    }

    default boolean isStartCountdownActive() {
        return getGameLoopTimer().isStartCountdownActive();
    }

    default void registerTask(int taskId) {
        getCTFTaskRegistry().register(taskId);
    }

    default void unregisterTask(int taskId) {
        getCTFTaskRegistry().unregister(taskId);
    }

    default int getActiveTaskCount() {
        return getCTFTaskRegistry().getActiveTaskCount();
    }

    default void cancelAllTasks() {
        getCTFTaskRegistry().cancelAll();
    }

    default void registerFastTick(String key, int periodTicks, Runnable action) {
        getFastTickBus().register(key, periodTicks, action);
    }

    default void unregisterFastTick(String key) {
        getFastTickBus().unregister(key);
    }

    default boolean hasFastTickBus() {
        return getFastTickBus() != null;
    }

    default boolean registerFastTickIfAvailable(String key, int periodTicks, Runnable action) {
        FastTickBus bus = getFastTickBus();
        if (bus == null) {
            return false;
        }
        bus.register(key, periodTicks, action);
        return true;
    }

    default boolean unregisterFastTickIfAvailable(String key) {
        FastTickBus bus = getFastTickBus();
        if (bus == null) {
            return false;
        }
        bus.unregister(key);
        return true;
    }

    default boolean isFastTickBusRunning() {
        return getFastTickBus().isRunning();
    }
}
