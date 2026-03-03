package dev.tjxjnoobie.ctf.util.bukkit.runnable;

import java.util.Objects;
import java.util.function.Consumer;

import dev.tjxjnoobie.ctf.game.tasks.CTFTaskRegistry;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import dev.tjxjnoobie.ctf.dependency.interfaces.PluginConfigDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
public final class BukkitRunnableUtil extends BukkitRunnable implements PluginConfigDependencyAccess, TaskDependencyAccess {

    // == Runtime state ==
    private final Runnable plainRunnable;
    private final Consumer<BukkitRunnableUtil> selfAwareRunnable; // For cancelling tasks

    // == Lifecycle ==
    private BukkitRunnableUtil(Runnable runnable) {
        this.plainRunnable = Objects.requireNonNull(runnable, "runnable cannot be null");
        this.selfAwareRunnable = null; 
    }

    // New constructor for repeating tasks that need to cancel themselves
    private BukkitRunnableUtil(Consumer<BukkitRunnableUtil> selfAwareRunnable) {
        this.selfAwareRunnable = Objects.requireNonNull(selfAwareRunnable, "consumer cannot be null");
        this.plainRunnable = null;
    }

    private void register(BukkitTask task) {
        // Guard: short-circuit when task == null.
        if (task == null) {
            return;
        }
        CTFTaskRegistry registry = getCTFTaskRegistry();
        if (registry != null) {
            // Persist the task id for centralized cancellation.
            registry.register(task.getTaskId());
        }
    }

    // == Utilities ==
    /**
     * Returns the result of task.
     *
     * @param runnable Callback executed by this operation.
     * @return Result produced by this method.
     */
    public static BukkitRunnableUtil task(Runnable runnable) {
        // Wrap a plain runnable as a BukkitRunnable.
        return new BukkitRunnableUtil(runnable);
    }

    // == The Overload ==
    /**
     * Returns the result of task.
     *
     * @param taskAction Callback executed by this operation.
     * @return Result produced by this method.
     */
    public static BukkitRunnableUtil task(Consumer<BukkitRunnableUtil> taskAction) {
        // Wrap a self-aware runnable for cancellation support.
        return new BukkitRunnableUtil(taskAction);
    }

    /**
     * Executes the run operation.
     */
    @Override
    public void run() {
        if (plainRunnable != null) {
            // Execute one-shot runnable.
            plainRunnable.run();
        } else if (selfAwareRunnable != null) {
            // Execute self-aware task, allowing it to cancel itself.
            selfAwareRunnable.accept(this);
        }
    }

    /**
     * Executes the runTask operation.
     *
     * @return Task handle or id returned by the scheduler operation.
     */
    public BukkitTask runTask() {
        BukkitTask task = super.runTask(getMainPlugin()); // Schedule immediately on the main thread.
        // Track task id for later cleanup.
        register(task);
        return task;
    }

    /**
     * Executes the runTaskLater operation.
     *
     * @param delayTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public BukkitTask runTaskLater(long delayTicks) {
        BukkitTask task = super.runTaskLater(getMainPlugin(), delayTicks); // Schedule a delayed task on the main thread.
        // Track task id for later cleanup.
        register(task);
        return task;
    }

    /**
     * Executes the runTaskTimer operation.
     *
     * @param delayTicks Tick delay value used for scheduling.
     * @param periodTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public BukkitTask runTaskTimer(long delayTicks, long periodTicks) {
        BukkitTask task = super.runTaskTimer(getMainPlugin(), delayTicks, periodTicks); // Schedule a repeating task on the main thread.
        // Track task id for later cleanup.
        register(task);
        return task;
    }

    /**
     * Executes the runTaskAsync operation.
     *
     * @return Task handle or id returned by the scheduler operation.
     */
    public BukkitTask runTaskAsync() {
        BukkitTask task = super.runTaskAsynchronously(getMainPlugin()); // Schedule immediately on a background thread.
        // Track task id for later cleanup.
        register(task);
        return task;
    }

    /**
     * Executes the runTaskLaterAsync operation.
     *
     * @param delayTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public BukkitTask runTaskLaterAsync(long delayTicks) {
        BukkitTask task = super.runTaskLaterAsynchronously(getMainPlugin(), delayTicks); // Schedule a delayed background task.
        // Track task id for later cleanup.
        register(task);
        return task;
    }

    /**
     * Executes the runTaskTimerAsync operation.
     *
     * @param delayTicks Tick delay value used for scheduling.
     * @param periodTicks Tick delay value used for scheduling.
     * @return Task handle or id returned by the scheduler operation.
     */
    public BukkitTask runTaskTimerAsync(long delayTicks, long periodTicks) {
        BukkitTask task = super.runTaskTimerAsynchronously(getMainPlugin(), delayTicks, periodTicks); // Schedule a repeating background task.
        // Track task id for later cleanup.
        register(task);
        return task;
    }

}
