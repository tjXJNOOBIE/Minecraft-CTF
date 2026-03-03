package dev.tjxjnoobie.ctf.game;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.state.managers.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.kit.KitManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.Optional;
import java.lang.reflect.Field;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CtfMatchOrchestratorTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CtfMatchOrchestratorTest] ";

    @Test
    void tracksRunningState() {
        Bukkit.getLogger().info(LOG_PREFIX + "Game state toggles: LOBBY -> IN_PROGRESS -> LOBBY.");
        CtfMatchOrchestrator manager = new CtfMatchOrchestrator(
            Mockito.mock(JavaPlugin.class),
            Mockito.mock(TeamManager.class),
            Mockito.mock(ScoreBoardManager.class),
            Mockito.mock(KitManager.class),
            Mockito.mock(BossBarManager.class),
            Mockito.mock(DebugFeed.class)
        );

        // Initial state is idle.
        assertFalse(manager.isRunning());
        setState(manager, GameState.IN_PROGRESS);
        assertTrue(manager.isRunning());
        setState(manager, GameState.LOBBY);
        assertFalse(manager.isRunning());
        Bukkit.getLogger().info(LOG_PREFIX + "game manager running state toggles");
    }

    @Test
    void usesTeamSpawnWhenConfigured() {
        Bukkit.getLogger().info(LOG_PREFIX + "Respawn uses team spawn when configured.");
        TeamManager teamManager = Mockito.mock(TeamManager.class);
        CtfMatchOrchestrator manager = new CtfMatchOrchestrator(
            Mockito.mock(JavaPlugin.class),
            teamManager,
            Mockito.mock(ScoreBoardManager.class),
            Mockito.mock(KitManager.class),
            Mockito.mock(BossBarManager.class),
            Mockito.mock(DebugFeed.class)
        );

        Player player = Mockito.mock(Player.class);
        Location spawn = new Location((World) null, 5, 64, 5);
        when(teamManager.getSpawnFor(player)).thenReturn(Optional.of(spawn));

        Location result = manager.getRespawnLocation(player);

        assertEquals(5, result.getBlockX());
        Bukkit.getLogger().info(LOG_PREFIX + "game manager returns team spawn when configured");
    }

    @Test
    void returnsNullWhenSpawnMissing() {
        Bukkit.getLogger().info(LOG_PREFIX + "Respawn location is null when spawn not set.");
        TeamManager teamManager = Mockito.mock(TeamManager.class);
        CtfMatchOrchestrator manager = new CtfMatchOrchestrator(
            Mockito.mock(JavaPlugin.class),
            teamManager,
            Mockito.mock(ScoreBoardManager.class),
            Mockito.mock(KitManager.class),
            Mockito.mock(BossBarManager.class),
            Mockito.mock(DebugFeed.class)
        );

        Player player = Mockito.mock(Player.class);
        when(teamManager.getSpawnFor(player)).thenReturn(Optional.empty());

        assertNull(manager.getRespawnLocation(player));
        Bukkit.getLogger().info(LOG_PREFIX + "game manager returns null when spawn missing");
    }

    private void setState(CtfMatchOrchestrator manager, GameState state) {
        try {
            Field field = CtfMatchOrchestrator.class.getDeclaredField("gameStateManager");
            field.setAccessible(true);
            GameStateManager gameStateManager = (GameStateManager) field.get(manager);
            gameStateManager.setGameState(state);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Failed to set game state", ex);
        }
    }
}


