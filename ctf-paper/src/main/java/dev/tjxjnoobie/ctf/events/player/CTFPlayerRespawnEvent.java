package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerRespawnEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class CTFPlayerRespawnEvent implements Listener {
    private final PlayerRespawnEventHandler playerRespawnHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerRespawnEvent instance.
     */
    public CTFPlayerRespawnEvent() {
        Class<PlayerRespawnEventHandler> respawnClass = PlayerRespawnEventHandler.class; // Resolve handler once after dependency registration.
        String respawnMsg = "PlayerRespawnEventHandler not registered";
        this.playerRespawnHandler = DependencyLoaderAccess.requireInstance(respawnClass, respawnMsg);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerRespawnHandler.onPlayerRespawn(event);
    }
}
