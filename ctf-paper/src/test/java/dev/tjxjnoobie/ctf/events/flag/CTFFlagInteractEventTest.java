package dev.tjxjnoobie.ctf.events.flag;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerInteractEvent;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagInteractHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryUtils;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFFlagInteractEventTest extends TestLogSupport {
    // Dependencies
    private FlagCarrierHandler flagCarrierHandler;
    private FlagCarrierStateHandler flagCarrierStateHandler;
    private GameStateManager gameStateManager;
    private CTFPlayerInteractEvent listener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerDependency(IInventoryUtils.class, new InventoryUtils());
        registerMessageAndSender();
        flagCarrierHandler = Mockito.mock(FlagCarrierHandler.class);
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        registerDependencies(
                FlagCarrierHandler.class, flagCarrierHandler,
                FlagCarrierStateHandler.class, flagCarrierStateHandler,
                GameStateManager.class, gameStateManager,
                FlagInteractHandler.class, new FlagInteractHandler(),
                IScoutTaggerCombatEventHandler.class, Mockito.mock(IScoutTaggerCombatEventHandler.class)
        );
        listener = new CTFPlayerInteractEvent();
    }

    @Test
    void carrierInteractionsStillReachFlagTouchHandling() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Tester");
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(true);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);

        listener.onInteract(event);

        verify(flagCarrierHandler).processFlagTouch(player, location, true);
        verify(event, never()).setCancelled(true);
    }

    @Test
    void ignoresNonBlockAction() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(event.getAction()).thenReturn(Action.PHYSICAL);

        listener.onInteract(event);

        verify(flagCarrierHandler, never()).processFlagTouch(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        verify(event, never()).setCancelled(true);
    }

    @Test
    void ignoresMissingBlock() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(null);

        listener.onInteract(event);

        verify(flagCarrierHandler, never()).processFlagTouch(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        verify(event, never()).setCancelled(true);
    }

    @Test
    void cancelsWhenHandled() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);
        Location location = new Location(null, 1, 64, 1);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Tester");
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);
        when(flagCarrierHandler.processFlagTouch(player, location, true)).thenReturn(true);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(flagCarrierHandler).processFlagTouch(player, location, true);
    }
}
