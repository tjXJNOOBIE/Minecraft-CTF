package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigData;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FlagBaseSetupHandlerTest extends TestLogSupport {
    private FlagBlockPlacer flagBlockPlacer;
    private FlagStateRegistry flagStateRegistry;
    private FlagUiTicker flagUiTicker;
    private TeamBaseMetaDataResolver teamBaseMetaDataResolver;
    private FlagConfigHandler flagConfigHandler;

    private FlagBaseSetupHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();

        flagBlockPlacer = Mockito.mock(FlagBlockPlacer.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);
        flagUiTicker = Mockito.mock(FlagUiTicker.class);
        teamBaseMetaDataResolver = Mockito.mock(TeamBaseMetaDataResolver.class);
        flagConfigHandler = Mockito.mock(FlagConfigHandler.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagBlockPlacer.class, flagBlockPlacer);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagStateRegistry.class, flagStateRegistry);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagUiTicker.class, flagUiTicker);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamBaseMetaDataResolver.class, teamBaseMetaDataResolver);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagConfigHandler.class, flagConfigHandler);

        handler = new FlagBaseSetupHandler();
    }

    @Test
    void initializeFlagsFromConfigLoadsConfiguredTeamBases() {
        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");

        FlagMetaData redFlag = new FlagMetaData();
        Location redBase = new Location(world, 10, 64, 10);
        when(flagConfigHandler.getFlagData("red"))
            .thenReturn(new FlagConfigData(redBase, null, Material.RED_WOOL));
        when(flagConfigHandler.getFlagData("blue")).thenReturn(null);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(redFlag);
        when(flagStateRegistry.flagFor(TeamId.BLUE)).thenReturn(new FlagMetaData());

        handler.initializeFlagsFromConfig();

        verify(flagStateRegistry).registerDefaultFlags();
        verify(flagStateRegistry).setAtBase(eq(TeamId.RED), eq(new Location(world, 10, 64, 10)));
        verify(flagBlockPlacer).placeBaseFlagBlockWithoutIndicator(eq(new Location(world, 10, 64, 10)), eq(TeamId.RED));
    }

    @Test
    void setFlagBasePersistsStateAndTicksUi() {
        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");

        Player player = Mockito.mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(world, 3.7, 70.2, 9.4));

        FlagMetaData redFlag = new FlagMetaData();
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(redFlag);
        when(teamBaseMetaDataResolver.resolveFlagMaterial(TeamId.RED)).thenReturn(Material.RED_WOOL);
        Location indicator = new Location(world, 3.5, 72.25, 9.5);
        when(teamBaseMetaDataResolver.resolveBaseIndicatorLocation(eq(TeamId.RED), eq(new Location(world, 3, 70, 9))))
            .thenReturn(indicator);

        boolean updated = handler.setFlagBase(player, TeamId.RED);

        assertTrue(updated);
        verify(flagStateRegistry).setAtBase(eq(TeamId.RED), eq(new Location(world, 3, 70, 9)));

        ArgumentCaptor<FlagConfigData> configDataCaptor = ArgumentCaptor.forClass(FlagConfigData.class);
        verify(flagConfigHandler).setFlagData(eq("red"), configDataCaptor.capture());
        FlagConfigData data = configDataCaptor.getValue();
        assertTrue(data.getBaseLocation().equals(new Location(world, 3, 70, 9)));

        verify(flagBlockPlacer).placeBaseFlagBlock(eq(new Location(world, 3, 70, 9)), eq(TeamId.RED), eq(indicator));
        verify(flagUiTicker).tickFlagUi();
    }

    @Test
    void areBasesReadyReturnsTrueWhenBothBaseLocationsExist() {
        TeamBaseMetaData red = new TeamBaseMetaData();
        TeamBaseMetaData blue = new TeamBaseMetaData();
        red.setFlagSpawnLocation(new Location(Mockito.mock(World.class), 1, 64, 1));
        blue.setFlagSpawnLocation(new Location(Mockito.mock(World.class), 2, 64, 2));

        when(teamBaseMetaDataResolver.resolveTeamBaseMetaData(TeamId.RED)).thenReturn(red);
        when(teamBaseMetaDataResolver.resolveTeamBaseMetaData(TeamId.BLUE)).thenReturn(blue);

        assertTrue(handler.areBasesReady());
    }
}
