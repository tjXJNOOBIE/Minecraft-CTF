package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerQuitEventHandler;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Owns quit cleanup and messaging.
 */
public final class PlayerQuitHandler implements PlayerQuitEventHandler, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, PlayerDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerQuitEvent] ";
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        String playerName = player.getName();
        boolean inArena = getMatchPlayerSessionHandler().isPlayerInArena(player);
        String messageKey = inArena ? "broadcast.match_quit" : "broadcast.server_quit";
        Map<String, String> quitPlaceholders = Map.of("player", playerName);
        Component quitMessage = getMessage(messageKey, quitPlaceholders);
        event.quitMessage(quitMessage);

        getMatchPlayerSessionHandler().removePlayerFromArena(player, false);
        getBuildToggleUtil().clearPlayer(player);
        sendDebugMessage(LOG_PREFIX + "Quit handled - player=" + playerName);
    }
}
