package dev.tjxjnoobie.ctf.events.flag;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFFlagCarrierLockEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFFlagCarrierLockEventTest] ";

    @Test
    void blocksClickForCarrier() {
        Bukkit.getLogger().info(LOG_PREFIX + "Carrier inventory click: block moving the flag item.");
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagCarrierLockEvent listener = new CTFFlagCarrierLockEvent(flagManager);

        // Carrier should not be able to click-move the flag item.
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");
        when(flagManager.isFlagCarrier(playerId)).thenReturn(true);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(flagManager).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "carrier click is blocked");
    }

    @Test
    void blocksDragForCarrier() {
        Bukkit.getLogger().info(LOG_PREFIX + "Carrier inventory drag: block dragging the flag item.");
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagCarrierLockEvent listener = new CTFFlagCarrierLockEvent(flagManager);

        // Dragging across slots should be blocked for carriers.
        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");
        when(flagManager.isFlagCarrier(playerId)).thenReturn(true);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
        verify(flagManager).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "carrier drag is blocked");
    }

    @Test
    void blocksDrop() {
        Bukkit.getLogger().info(LOG_PREFIX + "Carrier item drop: prevent dropping the flag.");
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagCarrierLockEvent listener = new CTFFlagCarrierLockEvent(flagManager);

        // Dropping the flag item should be blocked.
        PlayerDropItemEvent event = Mockito.mock(PlayerDropItemEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");
        when(flagManager.isFlagCarrier(playerId)).thenReturn(true);

        listener.onDrop(event);

        verify(event).setCancelled(true);
        verify(flagManager).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "carrier drop is blocked");
    }

    @Test
    void blocksSwap() {
        Bukkit.getLogger().info(LOG_PREFIX + "Carrier offhand swap: block to keep flag locked.");
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagCarrierLockEvent listener = new CTFFlagCarrierLockEvent(flagManager);

        // Offhand swaps should be blocked.
        PlayerSwapHandItemsEvent event = Mockito.mock(PlayerSwapHandItemsEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");
        when(flagManager.isFlagCarrier(playerId)).thenReturn(true);

        listener.onSwapHands(event);

        verify(event).setCancelled(true);
        verify(flagManager).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "carrier swap is blocked");
    }

    @Test
    void blocksHotbarSwap() {
        Bukkit.getLogger().info(LOG_PREFIX + "Carrier hotbar swap: block slot switching.");
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagCarrierLockEvent listener = new CTFFlagCarrierLockEvent(flagManager);

        // Selecting the flag slot should be blocked to prevent interactions.
        PlayerItemHeldEvent event = Mockito.mock(PlayerItemHeldEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");
        when(flagManager.isFlagCarrier(playerId)).thenReturn(true);
        when(event.getNewSlot()).thenReturn(0);

        listener.onHeldSlotChange(event);

        verify(event).setCancelled(true);
        verify(flagManager).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "carrier hotbar swap is blocked");
    }

    @Test
    void ignoresNonCarrier() {
        Bukkit.getLogger().info(LOG_PREFIX + "Non-carrier inventory actions: allow normal interaction.");
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagCarrierLockEvent listener = new CTFFlagCarrierLockEvent(flagManager);

        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Tester");
        when(flagManager.isFlagCarrier(playerId)).thenReturn(false);

        listener.onInventoryClick(event);

        verify(event, never()).setCancelled(true);
        verify(flagManager, never()).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "non-carrier inventory click is ignored");
    }
}

