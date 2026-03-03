package dev.tjxjnoobie.ctf.events.flag;

import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class CTFFlagCarrierLockEvent implements Listener {
    private static final String LOG_PREFIX = "[CTF] [CTFFlagCarrierLockEvent] ";

    private final FlagManager flagManager;

    public CTFFlagCarrierLockEvent(FlagManager flagManager) {
        this.flagManager = flagManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
            && flagManager.isFlagCarrier(player.getUniqueId())) {
            // Edge Case: carriers cannot move the flag item via clicks.
            event.setCancelled(true);
            flagManager.lockCarrierHotbarSlot(player);
            Bukkit.getLogger().info(LOG_PREFIX + "Carrier click blocked - player=" + player.getName());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
            && flagManager.isFlagCarrier(player.getUniqueId())) {
            // Edge Case: carriers cannot drag the flag item.
            event.setCancelled(true);
            flagManager.lockCarrierHotbarSlot(player);
            Bukkit.getLogger().info(LOG_PREFIX + "Carrier drag blocked - player=" + player.getName());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (flagManager.isFlagCarrier(event.getPlayer().getUniqueId())) {
            // Edge Case: carriers cannot drop the flag item.
            event.setCancelled(true);
            flagManager.lockCarrierHotbarSlot(event.getPlayer());
            Bukkit.getLogger().info(LOG_PREFIX + "Carrier drop blocked - player=" + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (flagManager.isFlagCarrier(event.getPlayer().getUniqueId())) {
            // Edge Case: carriers cannot swap the flag item.
            event.setCancelled(true);
            flagManager.lockCarrierHotbarSlot(event.getPlayer());
            Bukkit.getLogger().info(LOG_PREFIX + "Carrier swap blocked - player=" + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onHeldSlotChange(PlayerItemHeldEvent event) {

        int FLAG_SLOT = 0;
        if (flagManager.isFlagCarrier(event.getPlayer().getUniqueId()) && event.getNewSlot() == FLAG_SLOT) {
            // Edge Case: carriers cannot select the flag slot for interactions.
            event.setCancelled(true);
            flagManager.lockCarrierHotbarSlot(event.getPlayer());
            Bukkit.getLogger().info(LOG_PREFIX + "Carrier hotbar change blocked - player=" + event.getPlayer().getName());
        }
    }
}

