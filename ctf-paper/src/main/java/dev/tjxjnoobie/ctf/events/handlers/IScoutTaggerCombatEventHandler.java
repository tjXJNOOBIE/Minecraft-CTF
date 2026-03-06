package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public interface IScoutTaggerCombatEventHandler {
    void onEntityDamageByEntity(EntityDamageByEntityEvent event);
    void onProjectileHit(ProjectileHitEvent event);
    void onPlayerInteract(PlayerInteractEvent event);
    void onPlayerQuit(PlayerQuitEvent event);
}
