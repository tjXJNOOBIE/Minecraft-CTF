package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.FlagCarrierLockEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

public final class CTFPlayerItemHeldEvent implements Listener {
    private final FlagCarrierLockEventHandler playerItemHeldHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerItemHeldEvent instance.
     */
    public CTFPlayerItemHeldEvent() {
        Class<FlagCarrierLockEventHandler> flagClass = FlagCarrierLockEventHandler.class; // Resolve handler once after dependency registration.
        String flagMsg = "FlagCarrierLockEventHandler not registered";
        this.playerItemHeldHandler = DependencyLoaderAccess.requireInstance(flagClass, flagMsg);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerItemHeldHandler.onPlayerItemHeld(event);
    }
}
