package dev.tjxjnoobie.ctf.game.combat.spear;

import dev.tjxjnoobie.ctf.events.handlers.HomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearEventUtil;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearInventoryUtils;
import dev.tjxjnoobie.ctf.kit.KitSlots;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Adapts Bukkit combat events into homing-spear ability operations.
 */
public final class HomingSpearAbilityEvents implements HomingSpearCombatEventHandler {
    private final HomingSpearAbilityHandler abilityHandler;
    private final HomingSpearInventoryUtils homingSpearInventoryUtils;

    /**
     * Constructs a HomingSpearAbilityEvents instance.
     *
     * @param abilityHandler Domain handler for spear combat operations.
     * @param homingSpearInventoryUtils Inventory helper for placeholder protection rules.
     */
    public HomingSpearAbilityEvents(HomingSpearAbilityHandler abilityHandler,
                                    HomingSpearInventoryUtils homingSpearInventoryUtils) {
        this.abilityHandler = Objects.requireNonNull(abilityHandler, "abilityHandler");
        this.homingSpearInventoryUtils = Objects.requireNonNull(homingSpearInventoryUtils, "homingSpearInventoryUtils");
    }

    /**
     * Routes spear melee and projectile damage events into the combat handler.
     *
     * @param event Bukkit damage event to inspect.
     */
    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event == null) {
            return;
        }

        if (event.getDamager() instanceof Player attacker) {
            abilityHandler.applySpearMeleeDamage(attacker, event);
        }

        if (event.getDamager() instanceof Trident spear && event.getEntity() instanceof Player victim) {
            abilityHandler.applyTrackedSpearProjectileDamage(spear, victim, event);
        }
    }

    /**
     * Records direct spear projectile hits for later damage confirmation.
     *
     * @param event Bukkit projectile-hit event to inspect.
     */
    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event == null) {
            return;
        }

        if (event.getEntity() instanceof Trident spear) {
            abilityHandler.recordTrackedSpearHit(spear, event.getHitEntity());
        }
    }

    /**
     * Routes trident launches into normal-throw spear handling.
     *
     * @param event Bukkit projectile-launch event to inspect.
     */
    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event == null) {
            return;
        }

        if (event.getEntity() instanceof Trident spear && spear.getShooter() instanceof Player shooter) {
            abilityHandler.handleThrownSpearLaunch(shooter, spear);
        }
    }

    /**
     * Prevents placeholder spear items from being moved by inventory clicks.
     *
     * @param event Bukkit inventory-click event to inspect.
     */
    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        if (event == null) {
            return;
        }

        Player player = HomingSpearEventUtil.resolveArenaPlayer(event.getWhoClicked(),
                abilityHandler::sessionIsPlayerInArena);
        if (player == null) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ItemStack hotbarItem = null;
        int hotbarButton = event.getHotbarButton();
        if (hotbarButton >= 0) {
            hotbarItem = player.getInventory().getItem(hotbarButton);
        }

        // Protect the reserved spear slot and any placeholder items from being swapped or picked up.
        if (HomingSpearEventUtil.containsPlaceholderItem(homingSpearInventoryUtils, current, cursor, hotbarItem)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents drag operations from moving placeholder spear items.
     *
     * @param event Bukkit inventory-drag event to inspect.
     */
    @Override
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event == null) {
            return;
        }

        Player player = HomingSpearEventUtil.resolveArenaPlayer(event.getWhoClicked(),
                abilityHandler::sessionIsPlayerInArena);
        if (player == null) {
            return;
        }

        if (event.getNewItems() != null
                && event.getNewItems().values().stream().anyMatch(homingSpearInventoryUtils::isPlaceholderItem)) {
            event.setCancelled(true);
            return;
        }

        // Also block raw-slot drags that would pass through the dedicated spear kit slot.
        if (event.getRawSlots() != null && event.getRawSlots().contains(KitSlots.SPEAR_SLOT)) {
            ItemStack slotItem = player.getInventory().getItem(KitSlots.SPEAR_SLOT);
            if (homingSpearInventoryUtils.isPlaceholderItem(slotItem)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents placeholder spear items from being dropped.
     *
     * @param event Bukkit item-drop event to inspect.
     */
    @Override
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event == null
                || HomingSpearEventUtil.resolveArenaPlayer(event.getPlayer(),
                abilityHandler::sessionIsPlayerInArena) == null) {
            return;
        }

        ItemStack dropped = event.getItemDrop().getItemStack();
        if (homingSpearInventoryUtils.isPlaceholderItem(dropped)) {
            event.setCancelled(true);
        }
    }

    /**
     * Clears spear runtime state when a player quits.
     *
     * @param event Bukkit quit event to inspect.
     */
    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event == null) {
            return;
        }
        abilityHandler.handlePlayerQuit(event.getPlayer());
    }

    /**
     * Consumes the swap-hand hotkey and uses it as spear activation.
     *
     * @param event Bukkit swap-hand event to inspect.
     */
    @Override
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (event == null) {
            return;
        }

        Player shooter = event.getPlayer();
        if (!abilityHandler.shouldInterceptPlayerSwap(shooter)) {
            return;
        }

        event.setCancelled(true);
        abilityHandler.tryActivateSpearAbility(shooter);
    }
}
