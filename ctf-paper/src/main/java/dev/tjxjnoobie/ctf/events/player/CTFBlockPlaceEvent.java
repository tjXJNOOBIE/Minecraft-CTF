package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerBuildGuardEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class CTFBlockPlaceEvent implements Listener {
    private final PlayerBuildGuardEventHandler blockPlaceHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFBlockPlaceEvent instance.
     */
    public CTFBlockPlaceEvent() {
        Class<PlayerBuildGuardEventHandler> buildClass = PlayerBuildGuardEventHandler.class; // Resolve handler once after dependency registration.
        String buildMsg = "PlayerBuildGuardEventHandler not registered";
        this.blockPlaceHandler = DependencyLoaderAccess.requireInstance(buildClass, buildMsg);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.blockPlaceHandler.onBlockPlace(event);
    }
}
