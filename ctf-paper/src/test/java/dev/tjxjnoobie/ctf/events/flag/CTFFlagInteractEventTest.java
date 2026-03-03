package dev.tjxjnoobie.ctf.events.flag;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFFlagInteractEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFFlagInteractEventTest] ";

    @Test
    void blocksCarrierInteraction() {
        Bukkit.getLogger().info(LOG_PREFIX + "Carrier interaction: block using the flag slot.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagInteractEvent listener = new CTFFlagInteractEvent(gameManager, flagManager);

        // Carrier holding the flag slot should be blocked from interacting.
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        UUID playerId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(0);
        when(flagManager.isFlagCarrier(playerId)).thenReturn(true);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(flagManager).lockCarrierHotbarSlot(player);
        verify(gameManager, never()).handleFlagTouch(Mockito.any(), Mockito.any());
        Bukkit.getLogger().info(LOG_PREFIX + "flag interact blocks carrier interaction");
    }

    @Test
    void ignoresNonBlockAction() {
        Bukkit.getLogger().info(LOG_PREFIX + "Physical/non-block interactions: ignore flag checks.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagInteractEvent listener = new CTFFlagInteractEvent(gameManager, flagManager);

        // Physical interactions should not trigger flag checks.
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Tester");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(1);
        when(event.getAction()).thenReturn(Action.PHYSICAL);

        listener.onInteract(event);

        verify(gameManager, never()).handleFlagTouch(Mockito.any(), Mockito.any());
        verify(event, never()).setCancelled(true);
        Bukkit.getLogger().info(LOG_PREFIX + "flag interact ignores non-block actions");
    }

    @Test
    void ignoresMissingBlock() {
        Bukkit.getLogger().info(LOG_PREFIX + "Block action without target: ignore flag checks.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagInteractEvent listener = new CTFFlagInteractEvent(gameManager, flagManager);

        // Block actions without a block target should be ignored.
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Tester");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(1);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(null);

        listener.onInteract(event);

        verify(gameManager, never()).handleFlagTouch(Mockito.any(), Mockito.any());
        verify(event, never()).setCancelled(true);
        Bukkit.getLogger().info(LOG_PREFIX + "flag interact ignores missing block");
    }

    @Test
    void cancelsWhenHandled() {
        Bukkit.getLogger().info(LOG_PREFIX + "Click flag block: handle pickup/return and cancel interaction.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFFlagInteractEvent listener = new CTFFlagInteractEvent(gameManager, flagManager);

        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Tester");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(1);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameManager.handleFlagTouch(player, location)).thenReturn(true);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(gameManager).handleFlagTouch(player, location);
        Bukkit.getLogger().info(LOG_PREFIX + "flag interact cancels when handled");
    }
}


