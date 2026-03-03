package dev.tjxjnoobie.ctf.events.flag;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFFlagBreakEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFFlagBreakEventTest] ";

    @Test
    void cancelsWhenFlagHandled() {
        Bukkit.getLogger().info(LOG_PREFIX + "Breaking flag block: intercept and cancel for pickup/return logic.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        CTFFlagBreakEvent listener = new CTFFlagBreakEvent(gameManager);

        BlockBreakEvent event = Mockito.mock(BlockBreakEvent.class);
        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);

        // Simulate breaking a block that maps to a flag.
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameManager.handleFlagTouch(player, location)).thenReturn(true);

        listener.onFlagBreak(event);

        verify(event).setCancelled(true);
        Bukkit.getLogger().info(LOG_PREFIX + "flag break cancels when handled");
    }

    @Test
    void ignoresNonFlagBreak() {
        Bukkit.getLogger().info(LOG_PREFIX + "Breaking non-flag block: allow normal break.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        CTFFlagBreakEvent listener = new CTFFlagBreakEvent(gameManager);

        BlockBreakEvent event = Mockito.mock(BlockBreakEvent.class);
        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);

        // Block does not belong to a flag.
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameManager.handleFlagTouch(player, location)).thenReturn(false);

        listener.onFlagBreak(event);

        verify(event, never()).setCancelled(true);
        Bukkit.getLogger().info(LOG_PREFIX + "flag break ignores non-flag block");
    }
}


