package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.FlagInteractEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CTFPlayerInteractEvent implements Listener {
    private final FlagInteractEventHandler flagInteractHandler;
    private final IScoutTaggerCombatEventHandler scoutTaggerAbility;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerInteractEvent instance.
     */
    public CTFPlayerInteractEvent() {
        Class<FlagInteractEventHandler> flagClass = FlagInteractEventHandler.class; // Resolve handler once after
                                                                                    // dependency registration.
        String flagMsg = "FlagInteractEventHandler not registered";
        this.flagInteractHandler = DependencyLoaderAccess.requireInstance(flagClass, flagMsg);

        Class<IScoutTaggerCombatEventHandler> scoutClass = IScoutTaggerCombatEventHandler.class;
        String scoutMsg = "IScoutTaggerCombatEventHandler not registered";
        this.scoutTaggerAbility = DependencyLoaderAccess.requireInstance(scoutClass, scoutMsg);
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.flagInteractHandler.onPlayerInteract(event);
        boolean cancelledAfterFlagInteract = event.isCancelled();
        // Guard: short-circuit when cancelledAfterFlagInteract.
        if (cancelledAfterFlagInteract) {
            return;
        }

        this.scoutTaggerAbility.onPlayerInteract(event);
    }
}
