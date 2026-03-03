package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Sends CTF welcome/active-game info on player join.
 */
public final class CTFPlayerJoinEvent implements Listener, MessageAccess {
    private final CtfMatchOrchestrator gameManager;

    public CTFPlayerJoinEvent(CtfMatchOrchestrator gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().displayName(Component.text(event.getPlayer().getName(), NamedTextColor.GRAY));

        event.joinMessage(msg("broadcast.server_join", Map.of(
            "player", event.getPlayer().getName()
        )));

        event.getPlayer().sendMessage(msg("player.welcome"));
        if (gameManager.getGameState() == GameState.IN_PROGRESS || gameManager.getGameState() == GameState.OVERTIME) {
            event.getPlayer().sendMessage(msg("player.active_game_notice"));
        }
        if (gameManager.getFlagManager() != null) {
            gameManager.getFlagManager().syncIndicatorVisibility();
        }
    }
}




