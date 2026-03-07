package dev.tjxjnoobie.ctf.events.entity;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.game.combat.handlers.CombatDamageRestrictionHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class CTFEntityDamageEvent implements Listener {
    private final CombatDamageRestrictionHandler combatDamageRestrictionHandler;

    public CTFEntityDamageEvent() {
        Class<CombatDamageRestrictionHandler> combatClass = CombatDamageRestrictionHandler.class;
        String combatMsg = "CombatDamageRestrictionHandler not registered";
        this.combatDamageRestrictionHandler = DependencyLoaderAccess.requireInstance(combatClass, combatMsg);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        this.combatDamageRestrictionHandler.onEntityDamage(event);
    }
}
