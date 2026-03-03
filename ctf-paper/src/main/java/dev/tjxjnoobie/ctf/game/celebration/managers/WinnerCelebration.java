package dev.tjxjnoobie.ctf.game.celebration.managers;

import dev.tjxjnoobie.ctf.game.tasks.WinnerCelebrationTask;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns winner-celebration task lifecycle and firework effects.
 */
public final class WinnerCelebration {
    private static final int FIREWORK_MIN_HEIGHT = 5;
    private static final int FIREWORK_MAX_HEIGHT = 8;
    private static final int FIREWORK_DETONATE_TICKS = 46;
    private static final List<org.bukkit.FireworkEffect.Type> FIREWORK_TYPES = List.of(
        org.bukkit.FireworkEffect.Type.BALL,
        org.bukkit.FireworkEffect.Type.BALL_LARGE,
        org.bukkit.FireworkEffect.Type.BURST,
        org.bukkit.FireworkEffect.Type.CREEPER,
        org.bukkit.FireworkEffect.Type.STAR
    );

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final BooleanSupplier cleanupInProgressSupplier;
    private BukkitTask winnerCelebrationTask;
    private org.bukkit.FireworkEffect.Type lastType;

    public WinnerCelebration(JavaPlugin plugin, TeamManager teamManager, BooleanSupplier cleanupInProgressSupplier) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.cleanupInProgressSupplier = cleanupInProgressSupplier;
    }

    /**
     * Starts celebration fireworks for the winning team.
     */
    public void start(String winningTeamKey, long durationMs) {
        cancel();

        WinnerCelebrationTask task = new WinnerCelebrationTask(
            plugin,
            winningTeamKey,
            teamManager,
            cleanupInProgressSupplier,
            this::cancel,
            this::spawnCelebrationFirework,
            durationMs
        );
        winnerCelebrationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, 0L, 10L);
    }

    /**
     * Cancels active winner celebration task.
     */
    public void cancel() {
        if (winnerCelebrationTask != null) {
            winnerCelebrationTask.cancel();
            winnerCelebrationTask = null;
        }
    }

    private void spawnCelebrationFirework(Location location, String teamKey) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        Location spawnLocation = location.clone().add(0.0, nextHeightOffset(), 0.0);
        Firework firework = spawnLocation.getWorld().spawn(spawnLocation, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        Color color = TeamManager.RED.equals(teamKey) ? Color.RED : Color.BLUE;
        boolean flicker = ThreadLocalRandom.current().nextBoolean();
        boolean trail = ThreadLocalRandom.current().nextBoolean();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
            .withColor(color)
            .with(nextType())
            .flicker(flicker)
            .trail(trail)
            .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.setSilent(true);
        firework.setTicksToDetonate(FIREWORK_DETONATE_TICKS);
    }

    private org.bukkit.FireworkEffect.Type nextType() {
        org.bukkit.FireworkEffect.Type next = lastType;
        for (int attempts = 0; attempts < 6 && next == lastType; attempts++) {
            next = FIREWORK_TYPES.get(ThreadLocalRandom.current().nextInt(FIREWORK_TYPES.size()));
        }
        lastType = next;
        return next;
    }

    private double nextHeightOffset() {
        return ThreadLocalRandom.current().nextDouble(FIREWORK_MIN_HEIGHT, FIREWORK_MAX_HEIGHT + 1.0);
    }
}


