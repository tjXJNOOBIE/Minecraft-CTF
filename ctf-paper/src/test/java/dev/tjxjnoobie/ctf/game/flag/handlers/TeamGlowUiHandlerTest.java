package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TeamGlowUiHandlerTest extends TestLogSupport {
    private TeamManager teamManager;
    private FlagStateRegistry flagStateRegistry;
    private TeamGlowUiHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        teamManager = Mockito.mock(TeamManager.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagStateRegistry.class, flagStateRegistry);

        handler = new TeamGlowUiHandler();
    }

    @Test
    void clearTeamGlowVisualsTurnsOffRealGlowingForAllJoinedPlayers() {
        Player first = Mockito.mock(Player.class);
        Player second = Mockito.mock(Player.class);
        when(first.getUniqueId()).thenReturn(UUID.randomUUID());
        when(second.getUniqueId()).thenReturn(UUID.randomUUID());
        when(teamManager.getJoinedPlayers()).thenReturn(List.of(first, second));

        handler.clearTeamGlowVisuals();

        verify(first, times(1)).setGlowing(false);
        verify(second, times(1)).setGlowing(false);
    }
}

