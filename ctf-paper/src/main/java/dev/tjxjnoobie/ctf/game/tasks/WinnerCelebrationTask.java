package dev.tjxjnoobie.ctf.game.tasks;

import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Fires repeating winner fireworks during cleanup celebration phase.
 */
public final class WinnerCelebrationTask implements Runnable {
    private final String winningTeamKey;
    private final TeamManager teamManager;
    private final BooleanSupplier cleanupInProgressSupplier;
    private final Runnable onTaskComplete;
    private final BiConsumer<Location, String> fireworkSpawner;

    // == Runtime state ==
    private final long startMillis;
    private final long durationMillis;

    // == Lifecycle ==
    /**
     * Constructs a WinnerCelebrationTask instance.
     *
     * @param winningTeamKey Team key used for lookup or state updates.
     * @param teamManager Dependency responsible for team manager.
     * @param cleanupInProgressSupplier Callback or dependency used during processing.
     * @param onTaskComplete Callback or dependency used during processing.
     * @param fireworkSpawner Callback or dependency used during processing.
     * @param durationMillis Duration or timestamp value in milliseconds.
     */
    public WinnerCelebrationTask(
        String winningTeamKey,
        TeamManager teamManager,
        BooleanSupplier cleanupInProgressSupplier,
        Runnable onTaskComplete,
        BiConsumer<Location, String> fireworkSpawner,
        long durationMillis
    ) {
        this.winningTeamKey = winningTeamKey;
        this.teamManager = teamManager;
        this.cleanupInProgressSupplier = cleanupInProgressSupplier;
        this.onTaskComplete = onTaskComplete;
        this.fireworkSpawner = fireworkSpawner;
        this.startMillis = System.currentTimeMillis();
        this.durationMillis = Math.max(0L, durationMillis);
    }

    // == Utilities ==
    /**
     * Executes the run operation.
     */
    @Override
    public void run() {
        boolean conditionResult1 = !cleanupInProgressSupplier.getAsBoolean() || System.currentTimeMillis() - startMillis >= durationMillis;
        if (conditionResult1) {
            onTaskComplete.run();
            return;
        }

        // Spawn a firework for each winning-team player.
        for (Player player : teamManager.getTeamPlayers(winningTeamKey)) {
            fireworkSpawner.accept(player.getLocation(), winningTeamKey);
        }
    }
}

