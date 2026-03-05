package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerMoveEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class CTFPlayerMoveEvent implements Listener {
    private final PlayerMoveEventHandler playerMoveHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerMoveEvent instance.
     */
    public CTFPlayerMoveEvent() {
        Class<PlayerMoveEventHandler> moveClass = PlayerMoveEventHandler.class; // Resolve handler once after dependency registration.
        String moveMsg = "PlayerMoveEventHandler not registered";
        this.playerMoveHandler = DependencyLoaderAccess.requireInstance(moveClass, moveMsg);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerMoveHandler.onPlayerMove(event);
    }
}
