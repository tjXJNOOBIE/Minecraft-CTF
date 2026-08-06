package dev.tjxjnoobie.ctf.util.bukkit.message;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/**
 * Consumer-facing access contract for reusable Bukkit messages and UI output.
 */
public interface IBukkitMessageUtil {

    default BukkitMessageUtil getBukkitMessageUtil() {
        return DependencyLoaderAccess.findInstance(BukkitMessageUtil.class);
    }

    default void sendActionBar(Player player, Component component) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.sendActionBar(player, component);
    }

    default void sendMessage(Player player, Component component) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.sendMessage(player, component);
    }

    default void sendTitle(Player player, Title title) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.sendTitle(player, title);
    }

    default void sendTitle(Player player, Component title, Component subtitle) {
        sendTitle(player, BukkitMessageUtils.title(title, subtitle));
    }

    default void sendTitle(Player player, Component title, Component subtitle, Title.Times times) {
        sendTitle(player, BukkitMessageUtils.title(title, subtitle, times));
    }

    default void broadcastToArena(Component component) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.broadcastToArena(component);
    }

    default void broadcastToArenaTitle(Title title) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.broadcastToArenaTitle(title);
    }

    default void broadcastToArenaTitle(Component title, Component subtitle) {
        broadcastToArenaTitle(BukkitMessageUtils.title(title, subtitle));
    }

    default void broadcastToArenaTitle(Component title, Component subtitle, Title.Times times) {
        broadcastToArenaTitle(BukkitMessageUtils.title(title, subtitle, times));
    }

    default void sendDebugMessage(String message) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.debug(message);
    }

    default void showBossBar(Player player,
                             BukkitBossBarType bossBarType,
                             Component text,
                             float progress) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.showBossBar(player, bossBarType, text, progress);
    }

    default void hideBossBar(Player player, BukkitBossBarType bossBarType) {
        BukkitMessageUtil messageUtil = getBukkitMessageUtil();
        if (messageUtil == null) {
            return;
        }
        messageUtil.hideBossBar(player, bossBarType);
    }
}
