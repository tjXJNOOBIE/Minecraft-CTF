package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.events.handlers.FlagCarrierLockEventHandler;

import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.InventoryUtilAccess;
/**
 * Owns carrier inventory/interaction locking while holding a flag.
 */
public final class FlagCarrierLockHandler implements FlagCarrierLockEventHandler, BukkitMessageSender, FlagDependencyAccess, InventoryUtilAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFFlagCarrierLockEvent] ";

    // == Lifecycle ==
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // Flag items now live in the main inventory, so hotbar swaps are no longer locked.
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            java.util.UUID playerUUID = player.getUniqueId();
            boolean conditionResult1 = getFlagCarrierStateHandler().isFlagCarrier(playerUUID);
            if (conditionResult1) {
                blockCarrierAction(player, event, "click");
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            java.util.UUID playerUUID = player.getUniqueId();
            boolean conditionResult2 = getFlagCarrierStateHandler().isFlagCarrier(playerUUID);
            if (conditionResult2) {
                blockCarrierAction(player, event, "drag");
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        java.util.UUID playerUUID = player.getUniqueId();
        boolean conditionResult3 = getFlagCarrierStateHandler().isFlagCarrier(playerUUID);
        if (conditionResult3) {
            blockCarrierAction(player, event, "drop");
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Flag items now live in the main inventory, so off-hand swaps are no longer locked.
    }

    // == Utilities ==
    private void blockCarrierAction(Player player, Cancellable cancellableEvent, String action) {
        // Guard: short-circuit when player == null || cancellableEvent == null.
        if (player == null || cancellableEvent == null) {
            return;
        }

        cancellableEvent.setCancelled(true);
        getFlagCarrierStateHandler().enforceCarrierFlagHotbarSlot(player);
        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "Carrier " + action + " blocked - player=" + playerName);
    }
}

