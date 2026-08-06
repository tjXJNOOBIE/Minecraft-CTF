package dev.tjxjnoobie.ctf.util.bukkit.message;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.dependency.interfaces.DependencyAccess;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/**
 * Reusable Bukkit API adapter for player messages and UI output.
 */
public final class BukkitMessageUtil implements IBukkitMessageUtil, DependencyAccess {

    private CTFPlayerMetaData requireArenaPlayers() {
        CTFPlayerMetaData arenaPlayers = getCTFPlayerMetaData();
        if (arenaPlayers == null) {
            throw new NullPointerException("CTFPlayerMetaData dependency not available");
        }
        return arenaPlayers;
    }

    private DebugFeed requireDebugFeed() {
        DebugFeed debugFeed = getDebugFeed();
        if (debugFeed == null) {
            throw new NullPointerException("DebugFeed dependency not available");
        }
        return debugFeed;
    }

    private BossBarManager requireBossBarManager() {
        BossBarManager bossBarManager = getBossBarManager();
        if (bossBarManager == null) {
            throw new NullPointerException("BossBarManager dependency not available");
        }
        return bossBarManager;
    }

    @Override
    public BukkitMessageUtil getBukkitMessageUtil() {
        return this;
    }

    @Override
    public void sendActionBar(Player player, Component component) {
        if (player == null || component == null) {
            return;
        }
        player.sendActionBar(component);
    }

    @Override
    public void sendMessage(Player player, Component component) {
        if (player == null || component == null) {
            return;
        }
        player.sendMessage(component);
    }

    @Override
    public void sendTitle(Player player, Title title) {
        if (player == null || title == null) {
            return;
        }
        player.showTitle(title);
    }

    @Override
    public void broadcastToArena(Component component) {
        if (component == null) {
            return;
        }
        requireArenaPlayers().broadcast(component);
    }

    @Override
    public void broadcastToArenaTitle(Title title) {
        if (title == null) {
            return;
        }
        requireArenaPlayers().broadcastTitle(title);
    }

    public void debug(String message) {
        if (message == null) {
            return;
        }
        requireDebugFeed().send(message);
    }

    @Override
    public void showBossBar(Player player,
                            BukkitBossBarType bossBarType,
                            Component text,
                            float progress) {
        if (player == null || bossBarType == null || text == null) {
            return;
        }

        BossBarManager bossBarManager = requireBossBarManager();
        switch (bossBarType) {
            case CARRIER -> bossBarManager.showCarrierBar(player, text, progress);
            case RETURN -> bossBarManager.showReturnBar(player, text, progress);
            case WAITING -> bossBarManager.showWaitingBar(player, text, progress);
            case KILL -> bossBarManager.showKillBar(player, text);
        }
    }

    @Override
    public void hideBossBar(Player player, BukkitBossBarType bossBarType) {
        if (player == null || bossBarType == null) {
            return;
        }

        BossBarManager bossBarManager = requireBossBarManager();
        switch (bossBarType) {
            case CARRIER -> bossBarManager.hideCarrierBar(player);
            case RETURN -> bossBarManager.hideReturnBar(player);
            case WAITING -> bossBarManager.hideWaitingBar(player);
            case KILL -> bossBarManager.hideKillBar(player);
        }
    }
}
