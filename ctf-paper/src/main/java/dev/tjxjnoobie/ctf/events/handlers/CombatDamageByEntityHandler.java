package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface CombatDamageByEntityHandler {
    void onEntityDamageByEntity(EntityDamageByEntityEvent event);
}
