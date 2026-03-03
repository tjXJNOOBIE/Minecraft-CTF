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

    public BossBarManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Shows or updates the carrier boss bar for a player.
     */
    public void showCarrierBar(Player player, Component text) {
        showCarrierBar(player, text, 1.0f);
    }

    /**
     * Shows or updates the carrier boss bar for a player with progress.
     */
    public void showCarrierBar(Player player, Component text, float progress) {
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
     * Hides the carrier boss bar from a player.
     */
    public void hideCarrierBar(Player player) {
        if (player == null) {
            return;
        }

        BossBar bar = carrierBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Shows or updates the return boss bar for a player.
     */
    public void showReturnBar(Player player, Component text) {
        showReturnBar(player, text, 1.0f);
    }

    /**
     * Shows or updates the return boss bar for a player with progress.
     */
    public void showReturnBar(Player player, Component text, float progress) {
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
     * Hides the return boss bar from a player.
     */
    public void hideReturnBar(Player player) {
        if (player == null) {
            return;
        }

        BossBar bar = returnBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Shows or updates lobby waiting status for a player.
     */
    public void showWaitingBar(Player player, Component text, float progress) {
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
     * Hides lobby waiting status for a player.
     */
    public void hideWaitingBar(Player player) {
        if (player == null) {
            return;
        }

        BossBar bar = waitingBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Shows the short-lived kill feedback boss bar.
     */
    public void showKillBar(Player player, Component text) {
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
     * Clears the kill feedback boss bar for a player.
     */
    public void hideKillBar(Player player) {
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
     * Clears all boss bars from tracked players.
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

