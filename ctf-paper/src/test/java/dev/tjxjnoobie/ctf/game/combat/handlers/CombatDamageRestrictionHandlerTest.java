package dev.tjxjnoobie.ctf.game.combat.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CombatDamageRestrictionHandlerTest extends TestLogSupport {
    private MatchPlayerSessionHandler matchPlayerSessionHandler;
    private GameStateManager gameStateManager;
    private CombatDamageRestrictionHandler handler;

    @BeforeEach
    void setUp() {
        registerMessageAndSender();
        matchPlayerSessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        registerDependencies(
                MatchPlayerSessionHandler.class, matchPlayerSessionHandler,
                GameStateManager.class, gameStateManager);
        handler = new CombatDamageRestrictionHandler();
    }

    @Test
    void lobbyDamageCancelsForArenaPlayers() {
        Player player = Mockito.mock(Player.class);
        EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);

        when(event.getEntity()).thenReturn(player);
        when(matchPlayerSessionHandler.isPlayerInArena(player)).thenReturn(true);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(false);

        handler.onEntityDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    void liveMatchDamageDoesNotCancelGenericDamage() {
        Player player = Mockito.mock(Player.class);
        EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);

        when(event.getEntity()).thenReturn(player);
        when(matchPlayerSessionHandler.isPlayerInArena(player)).thenReturn(true);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);

        handler.onEntityDamage(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void nonArenaPlayersAreIgnored() {
        Player player = Mockito.mock(Player.class);
        EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);

        when(event.getEntity()).thenReturn(player);
        when(matchPlayerSessionHandler.isPlayerInArena(player)).thenReturn(false);

        handler.onEntityDamage(event);

        verify(event, never()).setCancelled(true);
    }
}
