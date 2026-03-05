package dev.tjxjnoobie.ctf.game.tasks;

import org.bukkit.Bukkit;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry for tracking and managing active Bukkit tasks.
 */
public final class CTFTaskRegistry {

    // == Runtime state ==
    private final Set<Integer> activeTasks = new HashSet<>();

    // == Lifecycle ==
    /**
     * Updates state for register.
     *
     * @param taskId Identifier for the t as k.
     */
    public void register(int taskId) {
        synchronized (activeTasks) {
            // Track the task id for centralized cancellation.
            activeTasks.add(taskId);
        }
    }

    // == Getters ==
    public int getActiveTaskCount() {
        synchronized (activeTasks) {
            return activeTasks.size();
        }
    }

    // == Utilities ==
    /**
     * Executes unregister.
     *
     * @param taskId Identifier for the t as k.
     */
    public void unregister(int taskId) {
        synchronized (activeTasks) {
            // Remove from tracking once cancelled.
            activeTasks.remove(taskId);
        }
    }

    /**
     * Evaluates whether cancelAll is currently satisfied.
     */
    public void cancelAll() {
        synchronized (activeTasks) {
            // Cancel and clear all tracked tasks.
            for (int taskId : activeTasks) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            activeTasks.clear();
        }
    }
}
