package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerDeathEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class CTFPlayerDeathEvent implements Listener {
    private final PlayerDeathEventHandler playerDeathHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerDeathEvent instance.
     */
    public CTFPlayerDeathEvent() {
        Class<PlayerDeathEventHandler> deathClass = PlayerDeathEventHandler.class; // Resolve handler once after dependency registration.
        String deathMsg = "PlayerDeathEventHandler not registered";
        this.playerDeathHandler = DependencyLoaderAccess.requireInstance(deathClass, deathMsg);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerDeathHandler.onPlayerDeath(event);
    }
}
