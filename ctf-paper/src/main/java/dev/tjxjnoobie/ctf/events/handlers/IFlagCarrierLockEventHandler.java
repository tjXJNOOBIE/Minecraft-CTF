package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public interface IFlagCarrierLockEventHandler {
    void onInventoryClick(InventoryClickEvent event);
    void onInventoryDrag(InventoryDragEvent event);
    void onPlayerDropItem(PlayerDropItemEvent event);
    void onPlayerItemHeld(PlayerItemHeldEvent event);
    void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event);
}
