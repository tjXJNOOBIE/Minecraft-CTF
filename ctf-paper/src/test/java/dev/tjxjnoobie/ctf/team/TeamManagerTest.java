package dev.tjxjnoobie.ctf.team;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tjxjnoobie.ctf.TestLogSupport;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import dev.tjxjnoobie.ctf.team.handlers.TeamMembershipHandler;

class TeamManagerTest extends TestLogSupport {
    // Constants
    private static final String LOG_PREFIX = "[Test] [TeamManagerTest] ";

    @Test
    void normalizesTeamKeys() throws Exception {
        Bukkit.getLogger().info(LOG_PREFIX + "Team keys normalize to RED/BLUE and reject unknowns.");
        TeamManager manager = newManager(Files.createTempDirectory("team-manager-normalize").toFile());
        manager.registerDefaultTeams();

        assertEquals(TeamManager.RED, manager.normalizeKey("RED"));
        assertEquals(TeamManager.BLUE, manager.normalizeKey("blue"));
        assertNull(manager.normalizeKey("green"));
        assertNull(manager.normalizeKey(null));
        Bukkit.getLogger().info(LOG_PREFIX + "team manager normalizes team keys");
    }

    @Test
    void joinAndLeaveUpdateMembership() throws Exception {
        Bukkit.getLogger().info(LOG_PREFIX + "Team join/leave updates membership cleanly.");
        TeamManager manager = newManager(Files.createTempDirectory("team-manager-join").toFile());
        manager.registerDefaultTeams();
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(player.getName()).thenReturn("Tester");
        Mockito.when(Bukkit.getServer().getPlayer("Tester")).thenReturn(player);

        manager.joinTeam(player, TeamManager.RED);
        assertEquals(TeamManager.RED, manager.getTeamKey(player));

        manager.leaveTeam(player);
        assertNull(manager.getTeamKey(player));
        Bukkit.getLogger().info(LOG_PREFIX + "team manager join/leave updates membership");
    }

    @Test
    void balancesTowardSmallerTeam() throws Exception {
        Bukkit.getLogger().info(LOG_PREFIX + "Team balance favors smaller team.");
        TeamManager manager = newManager(Files.createTempDirectory("team-manager-balance").toFile());
        manager.registerDefaultTeams();
        Player redPlayer = Mockito.mock(Player.class);
        UUID redPlayerId = UUID.randomUUID();
        Mockito.when(redPlayer.getUniqueId()).thenReturn(redPlayerId);
        Mockito.when(redPlayer.getName()).thenReturn("TesterRed");
        Mockito.when(Bukkit.getServer().getPlayer("TesterRed")).thenReturn(redPlayer);

        manager.joinTeam(redPlayer, TeamManager.RED);
        assertEquals(TeamManager.BLUE, manager.getBalancedTeamKey(TeamManager.RED));
        Bukkit.getLogger().info(LOG_PREFIX + "team manager balances toward smaller team");
    }

    @Test
    void spawnRoundTripUsesConcreteSpawnConfigDelegation() throws Exception {
        Bukkit.getLogger().info(LOG_PREFIX + "Spawn round-trip persists through TeamManager -> stores -> SpawnConfigHandler.");
        File dataFolder = Files.createTempDirectory("team-manager-spawn").toFile();
        TeamManager manager = newManager(dataFolder);

        World world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn("ctf-world");
        Mockito.when(Bukkit.getServer().getWorld("ctf-world")).thenReturn(world);

        Location location = new Location(world, 100.5, 65.0, -22.25, 90.0f, 0.0f);
        manager.setSpawn(TeamManager.RED, location);

        Location restored = manager.getSpawn(TeamManager.RED).orElseThrow();
        assertEquals("ctf-world", restored.getWorld().getName());
        assertEquals(location.getX(), restored.getX());
        assertEquals(location.getY(), restored.getY());
        assertEquals(location.getZ(), restored.getZ());
        assertTrue(manager.getSpawn("missing").isEmpty());
        Bukkit.getLogger().info(LOG_PREFIX + "spawn round-trip delegated successfully through concrete config classes");
    }

    @Test
    void removeNearestReturnPointRemovesClosestConfiguredPoint() throws Exception {
        Bukkit.getLogger().info(LOG_PREFIX + "Nearest return-point removal delegates through concrete config storage.");
        File dataFolder = Files.createTempDirectory("team-manager-removereturn").toFile();
        TeamManager manager = newManager(dataFolder);

        World world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn("ctf-world");
        Mockito.when(Bukkit.getServer().getWorld("ctf-world")).thenReturn(world);

        Location redPoint = new Location(world, 10, 65, 10);
        Location bluePoint = new Location(world, 40, 65, 40);
        assertTrue(manager.addReturnPoint(TeamManager.RED, redPoint));
        assertTrue(manager.addReturnPoint(TeamManager.BLUE, bluePoint));

        String removedTeam = manager.removeNearestReturnPoint(new Location(world, 12, 65, 12));

        assertEquals(TeamManager.RED, removedTeam);
        assertTrue(manager.getReturnPoints(TeamManager.RED).isEmpty());
        assertEquals(1, manager.getReturnPoints(TeamManager.BLUE).size());
        Bukkit.getLogger().info(LOG_PREFIX + "nearest return-point removal delegated successfully");
    }

    private TeamManager newManager(File dataFolder) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Team redTeam = mockTeam();
        Team blueTeam = mockTeam();
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder);
        Mockito.when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        Mockito.when(scoreboard.getTeam("ctf_red")).thenReturn(null, redTeam);
        Mockito.when(scoreboard.getTeam("ctf_blue")).thenReturn(null, blueTeam);
        Mockito.when(scoreboard.registerNewTeam("ctf_red")).thenReturn(redTeam);
        Mockito.when(scoreboard.registerNewTeam("ctf_blue")).thenReturn(blueTeam);
        SpawnConfigHandler spawnConfigHandler = new SpawnConfigHandler(plugin);
        spawnConfigHandler.loadSpawnConfig();
        return new TeamManager(
                spawnConfigHandler,
                new TeamSpawnStore(spawnConfigHandler),
                new TeamReturnStore(spawnConfigHandler),
                new LobbySpawnStore(spawnConfigHandler),
                new TeamMembershipHandler(scoreboard));
    }

    private Team mockTeam() {
        Team team = Mockito.mock(Team.class);
        Set<String> entries = new LinkedHashSet<>();
        Mockito.when(team.getEntries()).thenAnswer(ignored -> new LinkedHashSet<>(entries));
        Mockito.when(team.hasEntry(Mockito.anyString())).thenAnswer(invocation -> entries.contains(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            entries.add(invocation.getArgument(0));
            return true;
        }).when(team).addEntry(Mockito.anyString());
        Mockito.doAnswer(invocation -> {
            entries.remove(invocation.getArgument(0));
            return true;
        }).when(team).removeEntry(Mockito.anyString());
        return team;
    }
}

