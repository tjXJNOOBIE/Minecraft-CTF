package dev.tjxjnoobie.ctf.events.flag;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryClickEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryDragEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDropItemEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerItemHeldEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerSwapHandItemsEvent;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierLockHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerBuildRestrictionHandler;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryUtils;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFFlagCarrierLockEventTest extends TestLogSupport {
    // Dependencies
    private FlagCarrierStateHandler flagCarrierStateHandler;

    private CTFInventoryClickEvent clickListener;
    private CTFInventoryDragEvent dragListener;
    private CTFPlayerDropItemEvent dropListener;
    private CTFPlayerSwapHandItemsEvent swapListener;
    private CTFPlayerItemHeldEvent heldListener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerDependency(IInventoryUtils.class, new InventoryUtils());
        registerMessageAndSender();
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        registerDependency(FlagCarrierStateHandler.class, flagCarrierStateHandler);
        FlagCarrierLockHandler flagCarrierLockHandler = new FlagCarrierLockHandler();
        PlayerBuildRestrictionHandler buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        IHomingSpearCombatEventHandler homingSpearCombatHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        registerDependencies(
                FlagCarrierLockHandler.class, flagCarrierLockHandler,
                PlayerBuildRestrictionHandler.class, buildRestrictionHandler,
                IHomingSpearCombatEventHandler.class, homingSpearCombatHandler
        );

        clickListener = new CTFInventoryClickEvent();
        dragListener = new CTFInventoryDragEvent();
        dropListener = new CTFPlayerDropItemEvent();
        swapListener = new CTFPlayerSwapHandItemsEvent();
        heldListener = new CTFPlayerItemHeldEvent();
    }

    @Test
    void blocksClickForCarrier() {
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(true);

        clickListener.onClick(event);

        verify(event).setCancelled(true);
        verify(flagCarrierStateHandler).enforceCarrierFlagHotbarSlot(player);
    }

    @Test
    void blocksDragForCarrier() {
        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(true);

        dragListener.onDrag(event);

        verify(event).setCancelled(true);
        verify(flagCarrierStateHandler).enforceCarrierFlagHotbarSlot(player);
    }

    @Test
    void blocksDropForCarrier() {
        PlayerDropItemEvent event = Mockito.mock(PlayerDropItemEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(true);

        dropListener.onDropItem(event);

        verify(event).setCancelled(true);
        verify(flagCarrierStateHandler).enforceCarrierFlagHotbarSlot(player);
    }

    @Test
    void blocksSwapHandsForCarrier() {
        PlayerSwapHandItemsEvent event = Mockito.mock(PlayerSwapHandItemsEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(true);

        swapListener.onSwapHandItems(event);

        verify(event, never()).setCancelled(true);
        verify(flagCarrierStateHandler, never()).enforceCarrierFlagHotbarSlot(player);
    }

    @Test
    void allowsHeldSlotChangesForCarrier() {
        PlayerItemHeldEvent event = Mockito.mock(PlayerItemHeldEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getNewSlot()).thenReturn(0);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(true);

        heldListener.onItemHeld(event);

        verify(event, never()).setCancelled(true);
        verify(flagCarrierStateHandler, never()).enforceCarrierFlagHotbarSlot(player);
    }

    @Test
    void ignoresNonCarrier() {
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(false);

        clickListener.onClick(event);

        verify(event, never()).setCancelled(true);
        verify(flagCarrierStateHandler, never()).enforceCarrierFlagHotbarSlot(player);
    }
}

