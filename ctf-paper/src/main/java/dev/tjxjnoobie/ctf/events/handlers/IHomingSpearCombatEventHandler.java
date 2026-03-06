package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Event contract for Bukkit hooks used by the homing-spear combat domain.
 */
public interface IHomingSpearCombatEventHandler {
    /**
     * Handles entity-vs-entity damage for homing-spear melee and projectile logic.
     *
     * @param event Bukkit damage event to inspect.
     */
    void onEntityDamageByEntity(EntityDamageByEntityEvent event);

    /**
     * Handles tracked spear projectile-hit callbacks.
     *
     * @param event Bukkit projectile-hit event to inspect.
     */
    void onProjectileHit(ProjectileHitEvent event);

    /**
     * Handles spear projectile launches.
     *
     * @param event Bukkit projectile-launch event to inspect.
     */
    void onProjectileLaunch(ProjectileLaunchEvent event);

    /**
     * Handles inventory-click protection for spear placeholders.
     *
     * @param event Bukkit inventory-click event to inspect.
     */
    void onInventoryClick(InventoryClickEvent event);

    /**
     * Handles inventory-drag protection for spear placeholders.
     *
     * @param event Bukkit inventory-drag event to inspect.
     */
    void onInventoryDrag(InventoryDragEvent event);

    /**
     * Handles placeholder drop protection.
     *
     * @param event Bukkit item-drop event to inspect.
     */
    void onPlayerDropItem(PlayerDropItemEvent event);

    /**
     * Handles player quit cleanup.
     *
     * @param event Bukkit quit event to inspect.
     */
    void onPlayerQuit(PlayerQuitEvent event);

    /**
     * Handles swap-hand activation for the spear ability.
     *
     * @param event Bukkit swap-hand event to inspect.
     */
    void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event);
}
