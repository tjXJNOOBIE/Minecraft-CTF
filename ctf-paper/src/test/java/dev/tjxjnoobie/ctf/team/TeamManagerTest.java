package dev.tjxjnoobie.ctf.team;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.tjxjnoobie.ctf.TestLogSupport;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TeamManagerTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [TeamManagerTest] ";

    @Test
    void normalizesTeamKeys() {
        Bukkit.getLogger().info(LOG_PREFIX + "Team keys normalize to RED/BLUE and reject unknowns.");
        TeamManager manager = new TeamManager(Mockito.mock(JavaPlugin.class));
        manager.registerDefaultTeams();

        assertEquals(TeamManager.RED, manager.normalizeKey("RED"));
        assertEquals(TeamManager.BLUE, manager.normalizeKey("blue"));
        assertNull(manager.normalizeKey("green"));
        assertNull(manager.normalizeKey(null));
        Bukkit.getLogger().info(LOG_PREFIX + "team manager normalizes team keys");
    }

    @Test
    void joinAndLeaveUpdateMembership() {
        Bukkit.getLogger().info(LOG_PREFIX + "Team join/leave updates membership cleanly.");
        TeamManager manager = new TeamManager(Mockito.mock(JavaPlugin.class));
        manager.registerDefaultTeams();
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        Mockito.when(player.getName()).thenReturn("Tester");

        manager.joinTeam(player, TeamManager.RED);
        assertEquals(TeamManager.RED, manager.getTeamKey(player));

        manager.leaveTeam(player);
        assertNull(manager.getTeamKey(player));
        Bukkit.getLogger().info(LOG_PREFIX + "team manager join/leave updates membership");
    }

    @Test
    void balancesTowardSmallerTeam() {
        Bukkit.getLogger().info(LOG_PREFIX + "Team balance favors smaller team.");
        TeamManager manager = new TeamManager(Mockito.mock(JavaPlugin.class));
        manager.registerDefaultTeams();
        Player player = Mockito.mock(Player.class);
        java.util.UUID playerId = java.util.UUID.randomUUID();
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.getName()).thenReturn("Tester");

        manager.joinTeam(player, TeamManager.RED);
        assertEquals(TeamManager.BLUE, manager.getBalancedTeamKey(TeamManager.RED));
        Bukkit.getLogger().info(LOG_PREFIX + "team manager balances toward smaller team");
    }
}

