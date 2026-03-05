package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagPickupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagReturnHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FlagCaptureHandlerTest extends TestLogSupport {
    // Dependencies
    private TeamManager teamManager;
    private CTFCaptureZoneHandler captureZoneHandler;
    private FlagScoreHandler flagScoreHandler;
    private FlagBlockPlacer flagBlockPlacer;
    private FlagPickupHandler flagPickupHandler;
    private FlagReturnHandler flagReturnHandler;
    private FlagStateRegistry flagStateRegistry;
    private FlagUiTicker flagUiTicker;
    private FlagEventEffects flagEventEffects;
    private FlagCarrierStateHandler flagCarrierStateHandler;
    private TeamBaseMetaDataResolver teamBaseMetaDataResolver;

    private FlagCarrierHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();

        teamManager = Mockito.mock(TeamManager.class);
        captureZoneHandler = Mockito.mock(CTFCaptureZoneHandler.class);
        flagScoreHandler = Mockito.mock(FlagScoreHandler.class);
        flagBlockPlacer = Mockito.mock(FlagBlockPlacer.class);
        flagPickupHandler = Mockito.mock(FlagPickupHandler.class);
        flagReturnHandler = Mockito.mock(FlagReturnHandler.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);
        flagUiTicker = Mockito.mock(FlagUiTicker.class);
        flagEventEffects = Mockito.mock(FlagEventEffects.class);
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        teamBaseMetaDataResolver = Mockito.mock(TeamBaseMetaDataResolver.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CTFCaptureZoneHandler.class, captureZoneHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagScoreHandler.class, flagScoreHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagBlockPlacer.class, flagBlockPlacer);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagPickupHandler.class, flagPickupHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagReturnHandler.class, flagReturnHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagStateRegistry.class, flagStateRegistry);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagUiTicker.class, flagUiTicker);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagEventEffects.class, flagEventEffects);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagCarrierStateHandler.class, flagCarrierStateHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamBaseMetaDataResolver.class, teamBaseMetaDataResolver);

        handler = new FlagCarrierHandler();
    }

    @Test
    void touchingOwnDroppedFlagUsesReturnPath() {
        Player player = Mockito.mock(Player.class);
        Location touched = new Location(Mockito.mock(World.class), 1, 64, 1);

        FlagMetaData ownFlag = new FlagMetaData();
        ownFlag.state = FlagState.DROPPED;

        when(teamManager.getTeamId(player)).thenReturn(TeamId.RED);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(touched, flagStateRegistry)).thenReturn(TeamId.RED);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(ownFlag);
        when(flagReturnHandler.returnDroppedOwnFlagToBase(player, TeamId.RED)).thenReturn(true);

        boolean handled = handler.processFlagTouch(player, touched, true);

        assertTrue(handled);
        verify(flagReturnHandler).returnDroppedOwnFlagToBase(player, TeamId.RED);
        verify(flagPickupHandler, never()).processEnemyFlagPickup(any(), any());
    }

    @Test
    void touchingEnemyFlagUsesPickupPath() {
        Player player = Mockito.mock(Player.class);
        Location touched = new Location(Mockito.mock(World.class), 3, 65, 3);

        when(teamManager.getTeamId(player)).thenReturn(TeamId.RED);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(touched, flagStateRegistry)).thenReturn(TeamId.BLUE);
        when(flagPickupHandler.processEnemyFlagPickup(player, TeamId.BLUE)).thenReturn(true);

        boolean handled = handler.processFlagTouch(player, touched, true);

        assertTrue(handled);
        verify(flagPickupHandler).processEnemyFlagPickup(player, TeamId.BLUE);
    }

    @Test
    void touchingOwnFlagAtBaseDoesNotReturnHandled() {
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();
        Location touched = new Location(Mockito.mock(World.class), 1, 64, 1);

        FlagMetaData ownFlag = new FlagMetaData();
        ownFlag.state = FlagState.AT_BASE;

        when(player.getUniqueId()).thenReturn(playerId);
        when(teamManager.getTeamId(player)).thenReturn(TeamId.RED);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(touched, flagStateRegistry)).thenReturn(TeamId.RED);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(ownFlag);

        boolean handled = handler.processFlagTouch(player, touched, true);

        assertFalse(handled);
        verify(flagReturnHandler, never()).returnDroppedOwnFlagToBase(any(), any());
        verify(flagPickupHandler, never()).processEnemyFlagPickup(any(), any());
    }

    @Test
    void touchingOwnFlagAtBaseOnlySendsCaptureBlockedMessageOnceDuringCooldown() {
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();
        Location touched = new Location(Mockito.mock(World.class), 1, 64, 1);

        FlagMetaData ownFlag = new FlagMetaData();
        ownFlag.state = FlagState.AT_BASE;

        when(player.getUniqueId()).thenReturn(playerId);
        when(teamManager.getTeamId(player)).thenReturn(TeamId.RED);
        when(flagBlockPlacer.resolveFlagTeamAtBlockLocation(touched, flagStateRegistry)).thenReturn(TeamId.RED);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(ownFlag);

        handler.processFlagTouch(player, touched, true);
        handler.processFlagTouch(player, touched, true);

        verify(player).sendMessage(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void captureMovementGatesAndScoresOnlyInsideZoneWithOwnFlagHome() {
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Carrier");

        World world = Mockito.mock(World.class);
        Location to = new Location(world, 8, 64, 8);

        FlagMetaData ownFlag = new FlagMetaData();
        ownFlag.state = FlagState.AT_BASE;
        FlagMetaData carriedEnemyFlag = new FlagMetaData();
        carriedEnemyFlag.state = FlagState.CARRIED;
        carriedEnemyFlag.baseLocation = new Location(world, 20, 64, 20);

        TeamBaseMetaData redBaseMeta = new TeamBaseMetaData();
        redBaseMeta.setTeamId(TeamId.RED);
        redBaseMeta.setFlagSpawnLocation(new Location(world, 4, 64, 4));

        when(teamManager.getTeamId(player)).thenReturn(TeamId.RED);
        when(flagStateRegistry.findCarriedFlagTeam(playerId)).thenReturn(TeamId.BLUE);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(ownFlag);
        when(flagStateRegistry.flagFor(TeamId.BLUE)).thenReturn(carriedEnemyFlag);
        when(teamBaseMetaDataResolver.resolveTeamBaseMetaData(TeamId.RED)).thenReturn(redBaseMeta);
        when(captureZoneHandler.isInsideCaptureZone(redBaseMeta, to)).thenReturn(true);

        handler.processFlagCarrierMovement(player, to, true);

        verify(flagStateRegistry).setAtBase(TeamId.BLUE, carriedEnemyFlag.baseLocation);
        verify(flagBlockPlacer).placeBaseFlagBlock(carriedEnemyFlag.baseLocation, TeamId.BLUE, null);
        verify(flagCarrierStateHandler).clearCarrierFlagItemAndEffects(player);
        verify(flagScoreHandler).processFlagCapture(player, "red", "blue");
        verify(flagEventEffects).showFlagCaptureBroadcast(player, TeamId.RED, TeamId.BLUE);
        verify(flagUiTicker).tickFlagUi();
    }

    @Test
    void captureMovementStopsWhenOwnFlagNotAtBase() {
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        FlagMetaData ownFlag = new FlagMetaData();
        ownFlag.state = FlagState.DROPPED;

        when(teamManager.getTeamId(player)).thenReturn(TeamId.RED);
        when(flagStateRegistry.findCarriedFlagTeam(playerId)).thenReturn(TeamId.BLUE);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(ownFlag);

        handler.processFlagCarrierMovement(player, new Location(Mockito.mock(World.class), 1, 64, 1), true);

        verify(flagScoreHandler, never()).processFlagCapture(any(), any(), any());
    }
}
