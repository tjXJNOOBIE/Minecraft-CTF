package dev.tjxjnoobie.ctf.bossbar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Centralizes boss bar creation and lifecycle for CTF gameplay feedback.
 */
public final class BossBarManager {
    private static final long KILL_HIDE_DELAY_TICKS = 70L;

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> carrierBars = new HashMap<>();
    private final Map<UUID, BossBar> returnBars = new HashMap<>();
    private final Map<UUID, BossBar> killBars = new HashMap<>();
    private final Map<UUID, BossBar> waitingBars = new HashMap<>();
    private final Map<UUID, BukkitTask> killHideTasks = new HashMap<>();

    // == Lifecycle ==
    /**
     * Constructs a BossBarManager instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public BossBarManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // == Utilities ==
    /**
     * Executes showCarrierBar.
     *
     * @param player Player involved in this operation.
     * @param text User-facing text value.
     */
    public void showCarrierBar(Player player, Component text) {
        showCarrierBar(player, text, 1.0f);
    }

    /**
     * Executes showCarrierBar.
     *
     * @param player Player involved in this operation.
     * @param text User-facing text value.
     * @param progress Boss bar progress value in the range 0.0 to 1.0.
     */
    public void showCarrierBar(Player player, Component text, float progress) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = carrierBars.computeIfAbsent(player.getUniqueId(), id ->
            BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
        );
        bar.name(text == null ? Component.empty() : text);
        bar.progress(clampProgress(progress));
        player.showBossBar(bar);
    }

    /**
     * Executes hideCarrierBar.
     *
     * @param player Player involved in this operation.
     */
    public void hideCarrierBar(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = carrierBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Executes showReturnBar.
     *
     * @param player Player involved in this operation.
     * @param text User-facing text value.
     */
    public void showReturnBar(Player player, Component text) {
        showReturnBar(player, text, 1.0f);
    }

    /**
     * Executes showReturnBar.
     *
     * @param player Player involved in this operation.
     * @param text User-facing text value.
     * @param progress Boss bar progress value in the range 0.0 to 1.0.
     */
    public void showReturnBar(Player player, Component text, float progress) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = returnBars.computeIfAbsent(player.getUniqueId(), id ->
            BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS)
        );
        bar.name(text == null ? Component.empty() : text);
        bar.progress(clampProgress(progress));
        player.showBossBar(bar);
    }

    /**
     * Executes hideReturnBar.
     *
     * @param player Player involved in this operation.
     */
    public void hideReturnBar(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = returnBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Executes showWaitingBar.
     *
     * @param player Player involved in this operation.
     * @param text User-facing text value.
     * @param progress Boss bar progress value in the range 0.0 to 1.0.
     */
    public void showWaitingBar(Player player, Component text, float progress) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = waitingBars.computeIfAbsent(player.getUniqueId(), id ->
            BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
        );
        bar.name(text == null ? Component.empty() : text);
        bar.progress(clampProgress(progress));
        player.showBossBar(bar);
    }

    /**
     * Executes hideWaitingBar.
     *
     * @param player Player involved in this operation.
     */
    public void hideWaitingBar(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = waitingBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Executes showKillBar.
     *
     * @param player Player involved in this operation.
     * @param text User-facing text value.
     */
    public void showKillBar(Player player, Component text) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = killBars.computeIfAbsent(player.getUniqueId(), id ->
            BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS)
        );
        bar.name(text == null ? Component.empty() : text);
        player.showBossBar(bar);
        rescheduleKillHide(player, bar);
    }

    /**
     * Executes hideKillBar.
     *
     * @param player Player involved in this operation.
     */
    public void hideKillBar(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        BossBar bar = killBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }

        BukkitTask task = killHideTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Executes clearAll.
     */
    public void clearAll() {
        for (UUID playerId : carrierBars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(carrierBars.get(playerId));
            }
        }
        carrierBars.clear();

        for (UUID playerId : returnBars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(returnBars.get(playerId));
            }
        }
        returnBars.clear();

        for (UUID playerId : killBars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(killBars.get(playerId));
            }
        }
        killBars.clear();

        for (UUID playerId : waitingBars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(waitingBars.get(playerId));
            }
        }
        waitingBars.clear();

        for (BukkitTask task : killHideTasks.values()) {
            task.cancel();
        }
        killHideTasks.clear();
    }

    private float clampProgress(float progress) {
        // Guard: short-circuit when progress < 0.0f.
        if (progress < 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, progress);
    }

    private void rescheduleKillHide(Player player, BossBar bar) {
        BukkitTask existing = killHideTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.hideBossBar(bar);
            killBars.remove(player.getUniqueId());
            killHideTasks.remove(player.getUniqueId());
        }, KILL_HIDE_DELAY_TICKS);

        killHideTasks.put(player.getUniqueId(), task);
    }
}

