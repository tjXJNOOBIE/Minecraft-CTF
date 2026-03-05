package dev.tjxjnoobie.ctf.game;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerAttemptPickupItemEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerInteractEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerMoveEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerQuitEvent;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerItemPickupEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagInteractHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerMoveHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerItemPickupHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerQuitHandler;
import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryUtils;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Simulates a lightweight event-router flow for a single arena player.
 *
 * This is not a full 3v3 match. It exercises one player touching a flag block,
 * moving while in-arena, and quitting, with no real combat or scoring loop.
 */
class CTFGameSimulationTest extends TestLogSupport {
    // Dependencies
    private FlagCarrierHandler flagCarrierHandler;
    private FlagCarrierStateHandler flagCarrierStateHandler;
    private GameStateManager gameStateManager;
    private MatchPlayerSessionHandler sessionHandler;
    private BuildToggleUtil buildToggleUtil;

    @BeforeEach
    void setUp() {
        // Register only the collaborators needed for the touch -> move -> quit event chain.
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerDependency(IInventoryUtils.class, new InventoryUtils());
        registerMessageAndSender();

        flagCarrierHandler = Mockito.mock(FlagCarrierHandler.class);
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        buildToggleUtil = Mockito.mock(BuildToggleUtil.class);

        registerDependencies(
                FlagCarrierHandler.class, flagCarrierHandler,
                FlagCarrierStateHandler.class, flagCarrierStateHandler,
                GameStateManager.class, gameStateManager,
                MatchPlayerSessionHandler.class, sessionHandler,
                BuildToggleUtil.class, buildToggleUtil,
                FlagInteractHandler.class, new FlagInteractHandler(),
                PlayerItemPickupHandler.class, new PlayerItemPickupHandler(),
                IPlayerItemPickupEventHandler.class, new PlayerItemPickupHandler(),
                IScoutTaggerCombatEventHandler.class, Mockito.mock(IScoutTaggerCombatEventHandler.class),
                PlayerQuitHandler.class, new PlayerQuitHandler(),
                IHomingSpearCombatEventHandler.class, Mockito.mock(IHomingSpearCombatEventHandler.class),
                PlayerMoveHandler.class, new PlayerMoveHandler()
        );
    }

    @Test
    void simulatesTouchMoveQuitFlow() {
        logStep("arranging single-player touch/move/quit simulation");
        Player player = Mockito.mock(Player.class);
        org.bukkit.inventory.PlayerInventory inventory = Mockito.mock(org.bukkit.inventory.PlayerInventory.class);
        UUID playerId = UUID.randomUUID();
        Location flagLocation = new Location(null, 10, 64, 10);
        Location from = new Location(null, 10, 64, 10);
        Location to = new Location(null, 11, 64, 10);

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("SimPlayer");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(1);
        when(flagCarrierStateHandler.isFlagCarrier(playerId)).thenReturn(false);
        when(flagCarrierHandler.processFlagTouch(player, flagLocation, true)).thenReturn(true);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);
        when(sessionHandler.isPlayerInArena(player)).thenReturn(true);
        logValue("playerId", playerId);

        // Touching the flag should route through the flag-carrier handler and cancel the raw interact event.
        CTFPlayerInteractEvent interactListener = new CTFPlayerInteractEvent();
        PlayerInteractEvent interactEvent = Mockito.mock(PlayerInteractEvent.class);
        Block clickedBlock = Mockito.mock(Block.class);
        when(interactEvent.getPlayer()).thenReturn(player);
        when(interactEvent.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(interactEvent.getClickedBlock()).thenReturn(clickedBlock);
        when(clickedBlock.getLocation()).thenReturn(flagLocation);
        interactListener.onInteract(interactEvent);
        logStep("interact event routed");

        // Movement while in-arena should continue flag-carrier movement checks.
        CTFPlayerMoveEvent moveListener = new CTFPlayerMoveEvent();
        PlayerMoveEvent moveEvent = Mockito.mock(PlayerMoveEvent.class);
        when(moveEvent.getPlayer()).thenReturn(player);
        when(moveEvent.getFrom()).thenReturn(from);
        when(moveEvent.getTo()).thenReturn(to);
        moveListener.onMove(moveEvent);
        logStep("move event routed");

        // Quitting should delegate to session cleanup with restoreLocation=false.
        CTFPlayerQuitEvent quitListener = new CTFPlayerQuitEvent();
        PlayerQuitEvent quitEvent = Mockito.mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(player);
        quitListener.onQuit(quitEvent);
        logStep("quit event routed");

        verify(interactEvent).setCancelled(true);
        verify(quitEvent).quitMessage(Mockito.any());

        InOrder order = inOrder(flagCarrierHandler, sessionHandler);
        order.verify(flagCarrierHandler).processFlagTouch(player, flagLocation, true);
        order.verify(flagCarrierHandler).processFlagCarrierMovement(player, to, true);
        order.verify(sessionHandler).removePlayerFromArena(player, false);
        logStep("single-player simulation assertions completed");
    }

    @Test
    void simulatesArenaPickupRestrictionFlow() {
        Player player = Mockito.mock(Player.class);
        PlayerAttemptPickupItemEvent pickupEvent = Mockito.mock(PlayerAttemptPickupItemEvent.class);

        when(pickupEvent.getPlayer()).thenReturn(player);
        when(sessionHandler.isPlayerInArena(player)).thenReturn(true);

        CTFPlayerAttemptPickupItemEvent pickupListener = new CTFPlayerAttemptPickupItemEvent();
        pickupListener.onPlayerAttemptPickupItem(pickupEvent);

        verify(pickupEvent).setCancelled(true);
        logStep("pickup restriction simulation assertions completed");
    }
}
