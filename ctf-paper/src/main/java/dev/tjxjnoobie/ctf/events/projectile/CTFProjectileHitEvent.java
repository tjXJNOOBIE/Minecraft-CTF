package dev.tjxjnoobie.ctf.events.projectile;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class CTFProjectileHitEvent implements Listener {
    private final IHomingSpearCombatEventHandler homingSpearCombatHandler;
    private final IScoutTaggerCombatEventHandler scoutTaggerAbility;

    // == Lifecycle ==
    /**
     * Constructs a CTFProjectileHitEvent instance.
     */
    public CTFProjectileHitEvent() {
        Class<IHomingSpearCombatEventHandler> spearClass = IHomingSpearCombatEventHandler.class; // Resolve handler once after dependency registration.
        String spearMsg = "IHomingSpearCombatEventHandler not registered";
        this.homingSpearCombatHandler = DependencyLoaderAccess.requireInstance(spearClass, spearMsg);

        Class<IScoutTaggerCombatEventHandler> scoutClass = IScoutTaggerCombatEventHandler.class;
        String scoutMsg = "IScoutTaggerCombatEventHandler not registered";
        this.scoutTaggerAbility = DependencyLoaderAccess.requireInstance(scoutClass, scoutMsg);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.homingSpearCombatHandler.onProjectileHit(event);
        this.scoutTaggerAbility.onProjectileHit(event);
    }
}
