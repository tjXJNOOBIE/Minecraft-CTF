package dev.tjxjnoobie.ctf.util.bukkit.message;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.dependency.interfaces.DependencyAccess;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

public final class BukkitMessageUtil implements BukkitMessageSender, DependencyAccess {

    // == Getters ==
    private CTFPlayerMetaData requireArenaPlayers() {
        CTFPlayerMetaData arenaPlayers = getCTFPlayerMetaData();
        // Guard: Fail fast when required dependencies or invariants are missing.
        if (arenaPlayers == null) {
            throw new NullPointerException("CTFPlayerMetaData dependency not available");
        }
        return arenaPlayers;
    }

    private DebugFeed requireDebugFeed() {
        DebugFeed debugFeed = getDebugFeed();
        // Guard: Fail fast when required dependencies or invariants are missing.
        if (debugFeed == null) {
            throw new NullPointerException("DebugFeed dependency not available");
        }
        return debugFeed;
    }

    private BossBarManager requireBossBarManager() {
        BossBarManager bossBarManager = getBossBarManager();
        // Guard: Fail fast when required dependencies or invariants are missing.
        if (bossBarManager == null) {
            throw new NullPointerException("BossBarManager dependency not available");
        }
        return bossBarManager;
    }

    // == Utilities ==
    /**
     * Executes sendActionBar.
     *
     * @param player Player involved in this operation.
     * @param component Component content applied to the output item or message.
     */
    public void sendActionBar(Player player, Component component) {
        // Guard: short-circuit when player == null || component == null.
        if (player == null || component == null) {
            return;
        }
        // Send action bar to player only
        player.sendActionBar(component);
    }

    /**
     * Executes sendMessage.
     *
     * @param player Player involved in this operation.
     * @param component Component content applied to the output item or message.
     */
    public void sendMessage(Player player, Component component) {
        // Guard: short-circuit when player == null || component == null.
        if (player == null || component == null) {
            return;
        }
        // Send message to player only
        player.sendMessage(component);
    }

    /**
     * Executes sendTitle.
     *
     * @param player Player involved in this operation.
     * @param title User-facing display text.
     */
    public void sendTitle(Player player, Title title) {
        // Guard: short-circuit when player == null || title == null.
        if (player == null || title == null) {
            return;
        }
        // Send title to player only
        player.showTitle(title);
    }

    /**
     * Executes broadcastToArena.
     *
     * @param component Component content applied to the output item or message.
     */
    public void broadcastToArena(Component component) {
        // Guard: short-circuit when component == null.
        if (component == null) {
            return;
        }
        // Send message to arena players only
        requireArenaPlayers().broadcast(component);
    }

    /**
     * Executes broadcastToArenaTitle.
     *
     * @param title User-facing display text.
     */
    public void broadcastToArenaTitle(Title title) {
        // Guard: short-circuit when title == null.
        if (title == null) {
            return;
        }
        // Send title to arena players only
        requireArenaPlayers().broadcastTitle(title);
    }

    /**
     * Executes debug.
     *
     * @param message User-facing text value.
     */
    public void debug(String message) {
        // Guard: short-circuit when message == null.
        if (message == null) {
            return;
        }
        // Send debug message to debug feed      
        requireDebugFeed().send(message);
    }

    /**
     * Executes showBossBar.
     *
     * @param player Player involved in this operation.
     * @param bossBarType Domain enum value used to control behavior.
     * @param text User-facing text value.
     * @param progress Boss bar progress value in the range 0.0 to 1.0.
     */
    public void showBossBar(Player player, BukkitBossBarType bossBarType, Component text, float progress) {
        // Guard: short-circuit when player == null || bossBarType == null || text == null.
        if (player == null || bossBarType == null || text == null) {
            return;
        }
        BossBarManager bossBarManager = requireBossBarManager();
        // Show boss bar type for player
        switch (bossBarType) {
            case CARRIER -> bossBarManager.showCarrierBar(player, text, progress);
            case RETURN -> bossBarManager.showReturnBar(player, text, progress);
            case WAITING -> bossBarManager.showWaitingBar(player, text, progress);
            case KILL -> bossBarManager.showKillBar(player, text);
        }
    }

    /**
     * Executes hideBossBar.
     *
     * @param player Player involved in this operation.
     * @param bossBarType Domain enum value used to control behavior.
     */
    public void hideBossBar(Player player, BukkitBossBarType bossBarType) {
        // Guard: short-circuit when player == null || bossBarType == null.
        if (player == null || bossBarType == null) {
            return;
        }
        BossBarManager bossBarManager = requireBossBarManager();
        // Hide boss bar type for player
        switch (bossBarType) {
            case CARRIER -> bossBarManager.hideCarrierBar(player);
            case RETURN -> bossBarManager.hideReturnBar(player);
            case WAITING -> bossBarManager.hideWaitingBar(player);
            case KILL -> bossBarManager.hideKillBar(player);
        }
    }
}
