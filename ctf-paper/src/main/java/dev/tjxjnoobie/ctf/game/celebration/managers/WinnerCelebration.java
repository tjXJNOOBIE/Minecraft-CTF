package dev.tjxjnoobie.ctf.game.celebration.managers;

import dev.tjxjnoobie.ctf.game.tasks.WinnerCelebrationTask;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.tasks.EffectTaskOrchestrator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns winner-celebration task lifecycle and firework effects.
 */
public final class WinnerCelebration {

    // == Constants ==
    private static final int FIREWORK_MIN_HEIGHT = 5;
    private static final int FIREWORK_MAX_HEIGHT = 8;
    private static final int FIREWORK_DETONATE_TICKS = 46;
    private static final List<org.bukkit.FireworkEffect.Type> FIREWORK_TYPES = List.of(
            org.bukkit.FireworkEffect.Type.BALL,
            org.bukkit.FireworkEffect.Type.BALL_LARGE,
            org.bukkit.FireworkEffect.Type.BURST,
            org.bukkit.FireworkEffect.Type.CREEPER,
            org.bukkit.FireworkEffect.Type.STAR);
    private final TeamManager teamManager;
    private final BooleanSupplier cleanupInProgressSupplier;

    // == Runtime state ==
    private BukkitTask winnerCelebrationTask;
    private org.bukkit.FireworkEffect.Type lastType;

    // == Lifecycle ==
    /**
     * Constructs a WinnerCelebration instance.
     *
     * @param teamManager Dependency responsible for team manager.
     * @param cleanupInProgressSupplier Callback or dependency used during processing.
     */
    public WinnerCelebration(TeamManager teamManager, BooleanSupplier cleanupInProgressSupplier) {
        this.teamManager = teamManager;
        this.cleanupInProgressSupplier = cleanupInProgressSupplier;
    }

    /**
     * Executes the start operation.
     *
     * @param winningTeamKey Team key used for lookup or state updates.
     * @param durationMs Duration or timestamp value in milliseconds.
     */
    public void start(String winningTeamKey, long durationMs) {
        // Replace any existing celebration before starting a new one.
        cancel();

        WinnerCelebrationTask task = new WinnerCelebrationTask(
                winningTeamKey,
                teamManager,
                cleanupInProgressSupplier,
                this::cancel,
                this::spawnCelebrationFirework,
                durationMs);
        winnerCelebrationTask = EffectTaskOrchestrator.startTimer(winnerCelebrationTask, task, 0L, 10L);
    }

    // == Utilities ==
    /**
     * Evaluates whether cancel is currently satisfied.
     */
    public void cancel() {
        if (winnerCelebrationTask != null) {
            winnerCelebrationTask = EffectTaskOrchestrator.cancel(winnerCelebrationTask);
        }
    }

    private void spawnCelebrationFirework(Location location, String teamKey) {
        boolean conditionResult1 = location == null || location.getWorld() == null;
        // Guard: short-circuit when location == null || location.getWorld() == null.
        if (conditionResult1) {
            return;
        }

        Location spawnLocation = location.clone().add(0.0, nextHeightOffset(), 0.0); // Spawn a single celebratory firework with random effects.
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

