package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.FlagIndicator;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FlagIndicatorUiHandlerTest extends TestLogSupport {
    private TeamManager teamManager;
    private FlagIndicator flagIndicator;
    private FlagStateRegistry flagStateRegistry;
    private TeamBaseMetaDataResolver teamBaseMetaDataResolver;
    private FlagIndicatorUiHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        teamManager = Mockito.mock(TeamManager.class);
        flagIndicator = Mockito.mock(FlagIndicator.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);
        teamBaseMetaDataResolver = Mockito.mock(TeamBaseMetaDataResolver.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagIndicator.class, flagIndicator);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagStateRegistry.class, flagStateRegistry);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamBaseMetaDataResolver.class, teamBaseMetaDataResolver);

        handler = new FlagIndicatorUiHandler();
    }

    @Test
    void ensureFlagIndicatorsSpawnsWhenMissing() {
        World world = Mockito.mock(World.class);
        FlagMetaData redFlag = new FlagMetaData();
        redFlag.state = FlagState.AT_BASE;
        redFlag.activeLocation = new Location(world, 5, 64, 5);

        Map<TeamId, FlagMetaData> flags = new EnumMap<>(TeamId.class);
        flags.put(TeamId.RED, redFlag);
        when(flagStateRegistry.getFlagMetaDataByTeamId()).thenReturn(flags);
        when(flagIndicator.hasFlagIndicatorForTeam(TeamId.RED.key())).thenReturn(false);
        when(teamBaseMetaDataResolver.resolveIndicatorSpawnLocation(TeamId.RED, redFlag))
            .thenReturn(new Location(world, 5.5, 66, 5.5));

        handler.ensureFlagIndicators();

        verify(flagIndicator).spawnFlagIndicatorForTeam(TeamId.RED.key(), new Location(world, 5.5, 66, 5.5));
    }

    @Test
    void ensureFlagIndicatorsRemovesWhenFlagIsCarried() {
        FlagMetaData redFlag = new FlagMetaData();
        redFlag.state = FlagState.CARRIED;
        redFlag.activeLocation = null;

        Map<TeamId, FlagMetaData> flags = new EnumMap<>(TeamId.class);
        flags.put(TeamId.RED, redFlag);
        when(flagStateRegistry.getFlagMetaDataByTeamId()).thenReturn(flags);

        handler.ensureFlagIndicators();

        verify(flagIndicator).removeFlagIndicatorForTeam(TeamId.RED.key());
        verify(flagIndicator, never()).spawnFlagIndicatorForTeam(Mockito.anyString(), Mockito.any(Location.class));
    }

    @Test
    void tickIndicatorVisibilityThrottlesSyncUntilOneSecond() {
        when(teamManager.getJoinedPlayers()).thenReturn(List.of());

        for (int index = 0; index < 9; index++) {
            handler.tickIndicatorVisibility();
        }

        verify(flagIndicator, never()).syncVisibility(Mockito.anyList());

        handler.tickIndicatorVisibility();

        verify(flagIndicator, times(1)).syncVisibility(Mockito.anyList());
    }
}

