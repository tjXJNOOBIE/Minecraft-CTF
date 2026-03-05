package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerTeleportEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class CTFPlayerTeleportEvent implements Listener {
    private final PlayerTeleportEventHandler playerTeleportHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerTeleportEvent instance.
     */
    public CTFPlayerTeleportEvent() {
        Class<PlayerTeleportEventHandler> teleportClass = PlayerTeleportEventHandler.class; // Resolve handler once after dependency registration.
        String teleportMsg = "PlayerTeleportEventHandler not registered";
        this.playerTeleportHandler = DependencyLoaderAccess.requireInstance(teleportClass, teleportMsg);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerTeleportHandler.onPlayerTeleport(event);
    }
}
