package dev.tjxjnoobie.ctf.events.entity;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.CombatDamageByEntityHandler;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CTFEntityDamageByEntityEvent implements Listener {
    private final CombatDamageByEntityHandler combatDamageRestrictionHandler;
    private final IHomingSpearCombatEventHandler homingSpearCombatHandler;
    private final IScoutTaggerCombatEventHandler scoutTaggerAbility;

    // == Lifecycle ==
    /**
     * Constructs a CTFEntityDamageByEntityEvent instance.
     */
    public CTFEntityDamageByEntityEvent() {
        Class<CombatDamageByEntityHandler> combatClass = CombatDamageByEntityHandler.class; // Resolve handler once after dependency registration.
        String combatMsg = "CombatDamageByEntityHandler not registered";
        this.combatDamageRestrictionHandler = DependencyLoaderAccess.requireInstance(combatClass, combatMsg);

        Class<IHomingSpearCombatEventHandler> spearClass = IHomingSpearCombatEventHandler.class;
        String spearMsg = "IHomingSpearCombatEventHandler not registered";
        this.homingSpearCombatHandler = DependencyLoaderAccess.requireInstance(spearClass, spearMsg);

        Class<IScoutTaggerCombatEventHandler> scoutClass = IScoutTaggerCombatEventHandler.class;
        String scoutMsg = "IScoutTaggerCombatEventHandler not registered";
        this.scoutTaggerAbility = DependencyLoaderAccess.requireInstance(scoutClass, scoutMsg);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.combatDamageRestrictionHandler.onEntityDamageByEntity(event);
        boolean cancelledAfterRestriction = event.isCancelled();
        // Guard: short-circuit when cancelledAfterRestriction.
        if (cancelledAfterRestriction) {
            return;
        }

        this.homingSpearCombatHandler.onEntityDamageByEntity(event);
        boolean cancelledAfterSpear = event.isCancelled();
        // Guard: short-circuit when cancelledAfterSpear.
        if (cancelledAfterSpear) {
            return;
        }

        this.scoutTaggerAbility.onEntityDamageByEntity(event);
    }
}
