package dev.tjxjnoobie.ctf.util;

import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
import dev.tjxjnoobie.ctf.game.tasks.CTFTaskRegistry;
import dev.tjxjnoobie.ctf.util.bukkit.runnable.BukkitRunnableUtil;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Thin helper for consistent Bukkit task lifecycle calls.
 */
public final class BukkitTaskOrchestrator {

    // == Lifecycle ==
    private BukkitTaskOrchestrator() {
    }

    private static void register(BukkitTask task) {
        if (task == null)
            return;
        CTFTaskRegistry registry = getRegistry();
        if (registry != null) {
            // Track the task id for later cancellation.
            registry.register(task.getTaskId());
        }
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
    public static BukkitTask startTimer(BukkitTask existing,
            Runnable runnable,
            long delayTicks,
            long periodTicks) {
        // Guard: short-circuit when existing != null || runnable == null.
        if (existing != null || runnable == null) {
            return existing;
        }
        long safePeriod = Math.max(1L, periodTicks); // Schedule a repeating sync task and register it.
        long safeDelay = Math.max(0L, delayTicks);
        BukkitTask task = BukkitRunnableUtil.task(runnable).runTaskTimer(safeDelay, safePeriod);
        register(task);
        return task;
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
        // Guard: short-circuit when existing != null || runnable == null.
        if (existing != null || runnable == null) {
            return existing;
        }
        long safeDelay = Math.max(0L, delayTicks); // Schedule a delayed sync task and register it.
        BukkitTask task = BukkitRunnableUtil.task(runnable).runTaskLater(safeDelay);
        register(task);
        return task;
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
    public static Integer startTimerTaskId(Integer existingTaskId,
            Runnable runnable,
            long delayTicks,
            long periodTicks) {
        // Guard: short-circuit when existingTaskId != null.
        if (existingTaskId != null) {
            return existingTaskId;
        }
        // Guard: short-circuit when runnable == null.
        if (runnable == null) {
            return null;
        }
        long safePeriod = Math.max(1L, periodTicks); // Schedule and return the repeating task id.
        long safeDelay = Math.max(0L, delayTicks);
        BukkitTask task = BukkitRunnableUtil.task(runnable).runTaskTimer(safeDelay, safePeriod);
        register(task);
        return task == null ? null : task.getTaskId();
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
        // Guard: short-circuit when existingTaskId != null.
        if (existingTaskId != null) {
            return existingTaskId;
        }
        // Guard: short-circuit when runnable == null.
        if (runnable == null) {
            return null;
        }
        long safeDelay = Math.max(0L, delayTicks); // Schedule and return the delayed task id.
        BukkitTask task = BukkitRunnableUtil.task(runnable).runTaskLater(safeDelay);
        register(task);
        return task == null ? null : task.getTaskId();
    }

    // == Getters ==
    private static CTFTaskRegistry getRegistry() {
        // Resolve the shared task registry.
        return new TaskDependencyAccess() {}.getCTFTaskRegistry();
    }

    // == Utilities ==
    private static void unregister(int taskId) {
        CTFTaskRegistry registry = getRegistry();
        if (registry != null) {
            // Remove from registry when cancelled.
            registry.unregister(taskId);
        }
    }

    /**
     * Evaluates whether cancel is currently satisfied.
     *
     * @param existing Existing scheduled task handle to cancel before replacement.
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    public static BukkitTask cancel(BukkitTask existing) {
        if (existing != null) {
            // Unregister and cancel the task.
            unregister(existing.getTaskId());
            existing.cancel();
        }
        return null;
    }

    /**
     * Evaluates whether cancelTaskId is currently satisfied.
     *
     * @param existingTaskId Existing scheduled task handle to cancel before replacement.
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    public static Integer cancelTaskId(Integer existingTaskId) {
        if (existingTaskId != null) {
            // Unregister and cancel a task by id.
            unregister(existingTaskId);
            Bukkit.getScheduler().cancelTask(existingTaskId);
        }
        return null;
    }
}
