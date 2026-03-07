package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerItemPickupEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

/**
 * Routes player item-pickup attempts into the arena pickup guard.
 */
public final class CTFPlayerAttemptPickupItemEvent implements Listener {
    private final IPlayerItemPickupEventHandler itemPickupHandler;

    public CTFPlayerAttemptPickupItemEvent() {
        Class<IPlayerItemPickupEventHandler> pickupClass = IPlayerItemPickupEventHandler.class;
        String pickupMessage = "IPlayerItemPickupEventHandler not registered";
        this.itemPickupHandler = DependencyLoaderAccess.requireInstance(pickupClass, pickupMessage);
    }

    @EventHandler
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        itemPickupHandler.onPlayerAttemptPickupItem(event);
    }
}
