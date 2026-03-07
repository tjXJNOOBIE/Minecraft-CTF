package dev.tjxjnoobie.ctf.game.player.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlayerItemPickupHandlerTest extends TestLogSupport {

    private MatchPlayerSessionHandler sessionHandler;
    private PlayerItemPickupHandler pickupHandler;

    @BeforeEach
    void setUp() {
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        registerDependency(MatchPlayerSessionHandler.class, sessionHandler);
        pickupHandler = new PlayerItemPickupHandler();
    }

    @Test
    void blocksItemPickupWhilePlayerIsInArena() {
        Player player = Mockito.mock(Player.class);
        PlayerAttemptPickupItemEvent event = Mockito.mock(PlayerAttemptPickupItemEvent.class);

        when(event.getPlayer()).thenReturn(player);
        when(sessionHandler.isPlayerInArena(player)).thenReturn(true);

        pickupHandler.onPlayerAttemptPickupItem(event);

        verify(event).setCancelled(true);
    }

    @Test
    void allowsItemPickupOutsideArena() {
        Player player = Mockito.mock(Player.class);
        PlayerAttemptPickupItemEvent event = Mockito.mock(PlayerAttemptPickupItemEvent.class);

        when(event.getPlayer()).thenReturn(player);
        when(sessionHandler.isPlayerInArena(player)).thenReturn(false);

        pickupHandler.onPlayerAttemptPickupItem(event);

        verify(event, never()).setCancelled(true);
    }
}
