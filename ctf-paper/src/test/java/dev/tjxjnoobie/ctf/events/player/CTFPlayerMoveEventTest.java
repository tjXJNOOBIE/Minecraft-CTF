package dev.tjxjnoobie.ctf.events.player;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerMoveEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFPlayerMoveEventTest] ";

    @Test
    void ignoresNullDestination() {
        Bukkit.getLogger().info(LOG_PREFIX + "Player move with null destination: skip flag checks.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFPlayerMoveEvent listener = new CTFPlayerMoveEvent(gameManager, flagManager);

        // Simulate a move event with no destination to ensure it short-circuits.
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(event.getTo()).thenReturn(null);
        when(player.getName()).thenReturn("Tester");

        listener.onMove(event);

        verify(gameManager, never()).handleMove(Mockito.any(), Mockito.any());
        verify(flagManager, never()).lockCarrierHotbarSlot(Mockito.any());
        Bukkit.getLogger().info(LOG_PREFIX + "move event ignores null destination");
    }

    @Test
    void ignoresSameBlockMove() {
        Bukkit.getLogger().info(LOG_PREFIX + "Player move within same block: no carrier checks.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFPlayerMoveEvent listener = new CTFPlayerMoveEvent(gameManager, flagManager);

        // Simulate a move that stays within the same block.
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        Player player = Mockito.mock(Player.class);

        Location from = new Location(null, 1, 64, 1);
        Location to = new Location(null, 1, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);
        when(player.getName()).thenReturn("Tester");

        listener.onMove(event);

        verify(gameManager, never()).handleMove(Mockito.any(), Mockito.any());
        verify(flagManager, never()).lockCarrierHotbarSlot(Mockito.any());
        Bukkit.getLogger().info(LOG_PREFIX + "move event ignores same-block movement");
    }

    @Test
    void handlesBlockChange() {
        Bukkit.getLogger().info(LOG_PREFIX + "Player crosses block boundary: evaluate capture and carrier slot lock.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        CTFPlayerMoveEvent listener = new CTFPlayerMoveEvent(gameManager, flagManager);

        // Simulate moving across a block boundary to trigger flag checks.
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        Player player = Mockito.mock(Player.class);

        Location from = new Location(null, 1, 64, 1);
        Location to = new Location(null, 2, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);
        when(player.getName()).thenReturn("Tester");

        listener.onMove(event);

        verify(gameManager).handleMove(player, to);
        verify(flagManager).lockCarrierHotbarSlot(player);
        Bukkit.getLogger().info(LOG_PREFIX + "move event processes block change");
    }
}


