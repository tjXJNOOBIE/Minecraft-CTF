package dev.tjxjnoobie.ctf.game.tasks;

import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fires repeating winner fireworks during cleanup celebration phase.
 */
public final class WinnerCelebrationTask implements Runnable {
    private final JavaPlugin plugin;
    private final String winningTeamKey;
    private final TeamManager teamManager;
    private final BooleanSupplier cleanupInProgressSupplier;
    private final Runnable onTaskComplete;
    private final BiConsumer<Location, String> fireworkSpawner;
    private final long startMillis;
    private final long durationMillis;

    public WinnerCelebrationTask(
        JavaPlugin plugin,
        String winningTeamKey,
        TeamManager teamManager,
        BooleanSupplier cleanupInProgressSupplier,
        Runnable onTaskComplete,
        BiConsumer<Location, String> fireworkSpawner,
        long durationMillis
    ) {
        this.plugin = plugin;
        this.winningTeamKey = winningTeamKey;
        this.teamManager = teamManager;
        this.cleanupInProgressSupplier = cleanupInProgressSupplier;
        this.onTaskComplete = onTaskComplete;
        this.fireworkSpawner = fireworkSpawner;
        this.startMillis = System.currentTimeMillis();
        this.durationMillis = Math.max(0L, durationMillis);
    }

    @Override
    public void run() {
        if (!cleanupInProgressSupplier.getAsBoolean() || System.currentTimeMillis() - startMillis >= durationMillis) {
            onTaskComplete.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : teamManager.getTeamPlayers(winningTeamKey)) {
                fireworkSpawner.accept(player.getLocation(), winningTeamKey);
            }
        });
    }
}

