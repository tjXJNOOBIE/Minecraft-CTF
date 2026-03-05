package dev.tjxjnoobie.ctf.events.projectile;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class CTFProjectileLaunchEvent implements Listener {
    private final IHomingSpearCombatEventHandler homingSpearCombatHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFProjectileLaunchEvent instance.
     */
    public CTFProjectileLaunchEvent() {
        Class<IHomingSpearCombatEventHandler> spearClass = IHomingSpearCombatEventHandler.class; // Resolve handler once after dependency registration.
        String spearMsg = "IHomingSpearCombatEventHandler not registered";
        this.homingSpearCombatHandler = DependencyLoaderAccess.requireInstance(spearClass, spearMsg);
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.homingSpearCombatHandler.onProjectileLaunch(event);
    }
}
