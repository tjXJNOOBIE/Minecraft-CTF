package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public interface IPlayerBuildGuardEventHandler {
    void onBlockBreak(BlockBreakEvent event);
    void onBlockPlace(BlockPlaceEvent event);
    void onInventoryClick(InventoryClickEvent event);
    void onInventoryDrag(InventoryDragEvent event);
    void onPlayerDropItem(PlayerDropItemEvent event);
    void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event);
}
