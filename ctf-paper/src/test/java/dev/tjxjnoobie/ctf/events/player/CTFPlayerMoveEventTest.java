package dev.tjxjnoobie.ctf.events.player;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerMoveHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerMoveEventTest extends TestLogSupport {
    // Dependencies
    private FlagCarrierHandler flagCarrierHandler;
    private FlagCarrierStateHandler flagCarrierStateHandler;
    private GameStateManager gameStateManager;
    private CTFPlayerMoveEvent listener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();
        flagCarrierHandler = Mockito.mock(FlagCarrierHandler.class);
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        registerDependencies(
                FlagCarrierHandler.class, flagCarrierHandler,
                FlagCarrierStateHandler.class, flagCarrierStateHandler,
                GameStateManager.class, gameStateManager,
                PlayerMoveHandler.class, new PlayerMoveHandler()
        );
        listener = new CTFPlayerMoveEvent();
    }

    @Test
    void ignoresNullDestination() {
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");
        when(event.getTo()).thenReturn(null);

        listener.onMove(event);

        verify(flagCarrierHandler, never()).processFlagCarrierMovement(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void ignoresSameBlockMove() {
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        Player player = Mockito.mock(Player.class);
        Location from = new Location(null, 1, 64, 1);
        Location to = new Location(null, 1, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);

        listener.onMove(event);

        verify(flagCarrierHandler, never()).processFlagCarrierMovement(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void handlesBlockChange() {
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        Player player = Mockito.mock(Player.class);
        Location from = new Location(null, 1, 64, 1);
        Location to = new Location(null, 2, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);

        listener.onMove(event);

        verify(flagCarrierHandler).processFlagCarrierMovement(player, to, true);
        verify(flagCarrierStateHandler).enforceCarrierFlagHotbarSlot(player);
    }
}
