package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.player.managers.BuildBypassManager;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CTFPlayerQuitEvent implements Listener, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFPlayerQuitEvent] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final BuildBypassManager buildBypassManager;

    public CTFPlayerQuitEvent(CtfMatchOrchestrator gameManager, BuildBypassManager buildBypassManager) {
        this.gameManager = gameManager;
        this.buildBypassManager = buildBypassManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boolean inGame = gameManager.isPlayerInGame(event.getPlayer());
        event.quitMessage(msg(inGame ? "broadcast.match_quit" : "broadcast.server_quit", Map.of(
            "player", event.getPlayer().getName()
        )));
        gameManager.handlePlayerLeave(event.getPlayer(), false);
        if (buildBypassManager != null) {
            buildBypassManager.clearPlayer(event.getPlayer());
        }
        Bukkit.getLogger().info(LOG_PREFIX + "Quit handled - player=" + event.getPlayer().getName());
    }
}



