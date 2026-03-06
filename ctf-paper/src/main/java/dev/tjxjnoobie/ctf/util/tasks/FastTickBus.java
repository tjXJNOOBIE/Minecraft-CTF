package dev.tjxjnoobie.ctf.util.tasks;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.scheduler.BukkitTask;

/**
 * Shared 1-tick bus for high-frequency gameplay updates.
 */
public final class FastTickBus {
    private static final long BASE_PERIOD_TICKS = 1L;

    private final Map<String, TickSubscription> subscriptions = new HashMap<>();
    private BukkitTask tickTask;
    private long tickCounter;

    // == Lifecycle ==
    /**
     * Updates state for register.
     *
     * @param key Lookup key used to identify the scheduled action.
     * @param periodTicks Tick delay value used for scheduling.
     * @param action Callback executed by this operation.
     */
    public void register(String key, int periodTicks, Runnable action) {
        // Guard: short-circuit when key == null || action == null.
        if (key == null || action == null) {
            return;
        }
        int safePeriod = Math.max(1, periodTicks);
        subscriptions.put(key, new TickSubscription(safePeriod, action));
        ensureStarted();
    }

    private void stopIfIdle() {
        boolean empty = subscriptions.isEmpty();
        // Guard: short-circuit when !empty.
        if (!empty) {
            return;
        }
        tickTask = GameTaskOrchestrator.cancel(tickTask);
        tickCounter = 0L;
    }

    // == Utilities ==
    /**
     * Executes unregister.
     *
     * @param key Lookup key used to identify the scheduled action.
     */
    public void unregister(String key) {
        // Guard: short-circuit when key == null.
        if (key == null) {
            return;
        }
        subscriptions.remove(key);
        stopIfIdle();
    }

    private void ensureStarted() {
        // Guard: short-circuit when tickTask != null.
        if (tickTask != null) {
            return;
        }
        tickCounter = 0L;
        tickTask = GameTaskOrchestrator.startTimer(tickTask, this::tick, 0L, BASE_PERIOD_TICKS);
    }

    private void tick() {
        boolean empty2 = subscriptions.isEmpty();
        if (empty2) {
            stopIfIdle();
            return;
        }
        tickCounter++;
        for (TickSubscription subscription : new HashMap<>(subscriptions).values()) {
            // Guard: short-circuit when subscription == null.
            if (subscription == null) {
                continue;
            }
            // Guard: short-circuit when tickCounter % subscription.periodTicks != 0L.
            if (tickCounter % subscription.periodTicks != 0L) {
                continue;
            }
            try {
                subscription.action.run();
            } catch (Exception ignored) {
                // Ignore ticker failures to avoid stopping the bus.
            }
        }
    }

    // == Predicates ==
    public boolean isRunning() {
        return tickTask != null;
    }

    private static final class TickSubscription {
        private final int periodTicks;
        private final Runnable action;

        private TickSubscription(int periodTicks, Runnable action) {
            this.periodTicks = periodTicks;
            this.action = action;
        }
    }
}
