package dev.tjxjnoobie.ctf.game.flag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.team.SpawnConfigHandler;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFCaptureZoneHandlerTest extends TestLogSupport {
    // Dependencies
    // Gameplay systems (teams, players, flags, kits, scoreboards, bossbars)
    private TeamManager teamManager;
    private CTFCaptureZoneHandler captureZoneHandler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        teamManager = Mockito.mock(TeamManager.class);
        SpawnConfigHandler spawnConfigHandler = Mockito.mock(SpawnConfigHandler.class);
        when(teamManager.getSpawnConfigHandler()).thenReturn(spawnConfigHandler);
        when(spawnConfigHandler.getCaptureRadius(Mockito.anyDouble()))
            .thenAnswer(invocation -> invocation.getArgument(0, Double.class));
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        captureZoneHandler = new CTFCaptureZoneHandler();
    }

    @Test
    void returnsTrueWhenInsideReturnPointRadius() {
        World world = Mockito.mock(World.class);
        Location returnPoint = new Location(world, 0, 64, 0);
        Location playerLocation = new Location(world, 2, 64, 0);
        TeamBaseMetaData baseData = new TeamBaseMetaData();
        baseData.setTeamId(TeamId.RED);
        baseData.setReturnSpawnLocations(List.of(returnPoint));

        assertTrue(captureZoneHandler.isInsideCaptureZone(baseData, playerLocation));
    }

    @Test
    void returnsFalseWhenBaseDataMissingTeamId() {
        TeamBaseMetaData baseData = new TeamBaseMetaData();
        assertFalse(captureZoneHandler.isInsideCaptureZone(baseData, new Location(null, 0, 64, 0)));
    }

    @Test
    void returnsFalseWhenNoReturnPointAndNoCaptureMaterial() {
        World world = Mockito.mock(World.class);
        TeamBaseMetaData baseData = new TeamBaseMetaData();
        baseData.setTeamId(TeamId.RED);
        baseData.setFlagSpawnLocation(new Location(world, 0, 64, 0));
        baseData.setReturnSpawnLocations(List.of());
        when(teamManager.getCaptureMaterial(TeamId.RED.key())).thenReturn(null);

        assertFalse(captureZoneHandler.isInsideCaptureZone(baseData, new Location(world, 1, 64, 1)));
    }
}
