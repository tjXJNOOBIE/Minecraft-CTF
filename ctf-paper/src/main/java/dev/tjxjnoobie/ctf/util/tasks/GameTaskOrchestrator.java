package dev.tjxjnoobie.ctf.util.tasks;

import dev.tjxjnoobie.ctf.util.BukkitTaskOrchestrator;
import org.bukkit.scheduler.BukkitTask;

/**
 * Game-domain scheduler wrapper for Bukkit tasks.
 */
public final class GameTaskOrchestrator {

    // == Lifecycle ==
    private GameTaskOrchestrator() {
    }

    /**
     * Executes the startTimer operation.
     *
     * @param existing Existing scheduled task handle to cancel before replacement.
     * @param runnable Callback executed by this operation.
     * @param delayTicks Tick delay value used for scheduling.
     * @param periodTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public static BukkitTask startTimer(BukkitTask existing, Runnable runnable, long delayTicks, long periodTicks) {
        return BukkitTaskOrchestrator.startTimer(existing, runnable, delayTicks, periodTicks);
    }

    /**
     * Executes the startLater operation.
     *
     * @param existing Existing scheduled task handle to cancel before replacement.
     * @param runnable Callback executed by this operation.
     * @param delayTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public static BukkitTask startLater(BukkitTask existing, Runnable runnable, long delayTicks) {
        return BukkitTaskOrchestrator.startLater(existing, runnable, delayTicks);
    }

    /**
     * Executes the startTimerTaskId operation.
     *
     * @param existingTaskId Existing scheduled task handle to cancel before replacement.
     * @param runnable Callback executed by this operation.
     * @param delayTicks Tick delay value used for scheduling.
     * @param periodTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public static Integer startTimerTaskId(Integer existingTaskId, Runnable runnable, long delayTicks, long periodTicks) {
        return BukkitTaskOrchestrator.startTimerTaskId(existingTaskId, runnable, delayTicks, periodTicks);
    }

    /**
     * Executes the startLaterTaskId operation.
     *
     * @param existingTaskId Existing scheduled task handle to cancel before replacement.
     * @param runnable Callback executed by this operation.
     * @param delayTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public static Integer startLaterTaskId(Integer existingTaskId, Runnable runnable, long delayTicks) {
        return BukkitTaskOrchestrator.startLaterTaskId(existingTaskId, runnable, delayTicks);
    }

    // == Utilities ==
    /**
     * Evaluates whether cancel is currently satisfied.
     *
     * @param existing Existing scheduled task handle to cancel before replacement.
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    public static BukkitTask cancel(BukkitTask existing) {
        return BukkitTaskOrchestrator.cancel(existing);
    }

    /**
     * Evaluates whether cancelTaskId is currently satisfied.
     *
     * @param existingTaskId Existing scheduled task handle to cancel before replacement.
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    public static Integer cancelTaskId(Integer existingTaskId) {
        return BukkitTaskOrchestrator.cancelTaskId(existingTaskId);
    }
}
