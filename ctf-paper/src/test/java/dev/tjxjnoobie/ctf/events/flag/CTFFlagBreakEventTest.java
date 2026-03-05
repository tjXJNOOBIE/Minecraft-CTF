package dev.tjxjnoobie.ctf.events.flag;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.player.CTFBlockBreakEvent;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBreakHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerBuildRestrictionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFFlagBreakEventTest extends TestLogSupport {
    // Dependencies
    private FlagCarrierHandler flagCarrierHandler;
    private GameStateManager gameStateManager;
    private FlagBlockPlacer flagBlockPlacer;
    private FlagStateRegistry flagStateRegistry;
    private PlayerBuildRestrictionHandler buildRestrictionHandler;
    private CTFBlockBreakEvent listener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();
        flagCarrierHandler = Mockito.mock(FlagCarrierHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        flagBlockPlacer = Mockito.mock(FlagBlockPlacer.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);
        buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        registerDependencies(
                FlagCarrierHandler.class, flagCarrierHandler,
                GameStateManager.class, gameStateManager,
                FlagBlockPlacer.class, flagBlockPlacer,
                FlagStateRegistry.class, flagStateRegistry,
                FlagBreakHandler.class, new FlagBreakHandler(),
                PlayerBuildRestrictionHandler.class, buildRestrictionHandler
        );
        listener = new CTFBlockBreakEvent();
    }

    @Test
    void cancelsWhenFlagHandled() {
        BlockBreakEvent event = Mockito.mock(BlockBreakEvent.class);
        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);
        wireCancellationState(event);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(location, flagStateRegistry)).thenReturn(TeamId.RED);
        when(flagCarrierHandler.processFlagTouch(player, location, true)).thenReturn(true);

        listener.onBreak(event);

        verify(event).setCancelled(true);
    }

    @Test
    void ignoresNonFlagBreak() {
        BlockBreakEvent event = Mockito.mock(BlockBreakEvent.class);
        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);
        wireCancellationState(event);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(location, flagStateRegistry)).thenReturn(null);
        when(flagCarrierHandler.processFlagTouch(player, location, true)).thenReturn(false);

        listener.onBreak(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void cancelsFlagBlocksEvenWhenTouchFlowReturnsFalse() {
        BlockBreakEvent event = Mockito.mock(BlockBreakEvent.class);
        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);
        wireCancellationState(event);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(location, flagStateRegistry)).thenReturn(TeamId.RED);
        when(flagCarrierHandler.processFlagTouch(player, location, true)).thenReturn(false);

        listener.onBreak(event);

        verify(event).setCancelled(true);
        verify(buildRestrictionHandler, never()).onBlockBreak(event);
    }

    private void wireCancellationState(BlockBreakEvent event) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            cancelled.set(invocation.getArgument(0, Boolean.class));
            return null;
        }).when(event).setCancelled(Mockito.anyBoolean());
        when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());
    }
}
