package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerJoinEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CTFPlayerJoinEvent implements Listener {
    private final PlayerJoinEventHandler playerJoinHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerJoinEvent instance.
     */
    public CTFPlayerJoinEvent() {
        Class<PlayerJoinEventHandler> joinClass = PlayerJoinEventHandler.class; // Resolve handler once after dependency registration.
        String joinMsg = "PlayerJoinEventHandler not registered";
        this.playerJoinHandler = DependencyLoaderAccess.requireInstance(joinClass, joinMsg);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerJoinHandler.onPlayerJoin(event);
    }
}
