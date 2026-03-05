package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.CaptureZoneParticleRenderer;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CaptureZoneUiHandlerTest extends TestLogSupport {
    // Dependencies
    private TeamManager teamManager;
    private CTFCaptureZoneHandler captureZoneHandler;
    private CaptureZoneParticleRenderer captureZoneParticleRenderer;
    private TeamBaseMetaDataResolver teamBaseMetaDataResolver;
    private CaptureZoneUiHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        teamManager = Mockito.mock(TeamManager.class);
        captureZoneHandler = Mockito.mock(CTFCaptureZoneHandler.class);
        captureZoneParticleRenderer = Mockito.mock(CaptureZoneParticleRenderer.class);
        teamBaseMetaDataResolver = Mockito.mock(TeamBaseMetaDataResolver.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CTFCaptureZoneHandler.class, captureZoneHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CaptureZoneParticleRenderer.class, captureZoneParticleRenderer);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamBaseMetaDataResolver.class, teamBaseMetaDataResolver);

        handler = new CaptureZoneUiHandler();
    }

    @Test
    void renderCaptureZoneBordersRunsOnConfiguredPeriod() {
        World world = Mockito.mock(World.class);
        Player redViewer = Mockito.mock(Player.class);
        TeamBaseMetaData redBaseData = new TeamBaseMetaData();
        redBaseData.setFlagSpawnLocation(new Location(world, 10, 64, 10));
        redBaseData.setReturnSpawnLocations(List.of(new Location(world, 12, 64, 10)));

        when(captureZoneHandler.getCaptureRadius()).thenReturn(3.0);
        when(teamManager.getTeamPlayers(TeamId.RED.key())).thenReturn(List.of(redViewer));
        when(teamManager.getTeamPlayers(TeamId.BLUE.key())).thenReturn(List.of());
        when(teamBaseMetaDataResolver.resolveTeamBaseMetaData(TeamId.RED)).thenReturn(redBaseData);

        for (int index = 0; index < 4; index++) {
            handler.renderCaptureZoneBorders();
        }

        verify(captureZoneParticleRenderer, never()).renderTeamZones(
            Mockito.anyString(), Mockito.anyList(), Mockito.anyList(), Mockito.any(), Mockito.anyDouble());

        handler.renderCaptureZoneBorders();

        verify(captureZoneParticleRenderer, times(1)).renderTeamZones(
            TeamId.RED.key(),
            List.of(redViewer),
            redBaseData.getReturnSpawnLocations(),
            redBaseData.getFlagSpawnLocation(),
            3.0
        );
    }

    @Test
    void resetCaptureZoneParticleTickCounterDelaysRenderAgain() {
        when(captureZoneHandler.getCaptureRadius()).thenReturn(3.0);
        when(teamManager.getTeamPlayers(TeamId.RED.key())).thenReturn(List.of());
        when(teamManager.getTeamPlayers(TeamId.BLUE.key())).thenReturn(List.of());

        for (int index = 0; index < 4; index++) {
            handler.renderCaptureZoneBorders();
        }

        handler.resetCaptureZoneParticleTickCounter();
        handler.renderCaptureZoneBorders();

        verify(captureZoneParticleRenderer, never()).renderTeamZones(
            Mockito.anyString(), Mockito.anyList(), Mockito.anyList(), Mockito.any(), Mockito.anyDouble());
    }
}

