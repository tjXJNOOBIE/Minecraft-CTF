package dev.tjxjnoobie.ctf.game.flag;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.team.SpawnConfigHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFCaptureZoneServiceTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFCaptureZoneServiceTest] ";

    @Test
    void returnsTrueWhenOnTeamFloorWithinRadius() {
        Bukkit.getLogger().info(LOG_PREFIX + "Capture zone: on team floor inside radius should score.");
        TeamManager teamManager = Mockito.mock(TeamManager.class);
        when(teamManager.getCaptureMaterial(TeamManager.RED)).thenReturn(Material.RED_WOOL);

        World world = Mockito.mock(World.class);
        Location baseLocation = new Location(world, 0, 65, 0);
        Location playerLocation = Mockito.spy(new Location(world, 2, 65, 0));
        Location floorLocation = Mockito.spy(new Location(world, 2, 64, 0));
        Block floorBlock = Mockito.mock(Block.class);

        // Mock the ground block under the player.
        when(playerLocation.clone()).thenReturn(floorLocation);
        when(floorLocation.subtract(0.0, 1.0, 0.0)).thenReturn(floorLocation);
        when(floorLocation.getBlock()).thenReturn(floorBlock);
        when(floorBlock.getType()).thenReturn(Material.RED_WOOL);

        CTFCaptureZoneService service = createService(teamManager);

        assertTrue(service.isInsideCaptureZone(TeamManager.RED, playerLocation, baseLocation));
        Bukkit.getLogger().info(LOG_PREFIX + "capture zone returns true for matching floor within radius");
    }

    @Test
    void returnsFalseWhenOutsideRadius() {
        Bukkit.getLogger().info(LOG_PREFIX + "Capture zone: correct floor but outside radius should not score.");
        TeamManager teamManager = Mockito.mock(TeamManager.class);
        when(teamManager.getCaptureMaterial(TeamManager.RED)).thenReturn(Material.RED_WOOL);

        World world = Mockito.mock(World.class);
        Location baseLocation = new Location(world, 0, 65, 0);
        Location playerLocation = Mockito.spy(new Location(world, 10, 65, 0));
        Location floorLocation = Mockito.spy(new Location(world, 10, 64, 0));
        Block floorBlock = Mockito.mock(Block.class);

        // Player stands on the correct block, but too far from base.
        when(playerLocation.clone()).thenReturn(floorLocation);
        when(floorLocation.subtract(0.0, 1.0, 0.0)).thenReturn(floorLocation);
        when(floorLocation.getBlock()).thenReturn(floorBlock);
        when(floorBlock.getType()).thenReturn(Material.RED_WOOL);

        CTFCaptureZoneService service = createService(teamManager);

        assertFalse(service.isInsideCaptureZone(TeamManager.RED, playerLocation, baseLocation));
        Bukkit.getLogger().info(LOG_PREFIX + "capture zone returns false when outside radius");
    }

    @Test
    void returnsFalseWhenFloorDoesNotMatch() {
        Bukkit.getLogger().info(LOG_PREFIX + "Capture zone: wrong floor block should not score.");
        TeamManager teamManager = Mockito.mock(TeamManager.class);
        when(teamManager.getCaptureMaterial(TeamManager.BLUE)).thenReturn(Material.BLUE_WOOL);

        World world = Mockito.mock(World.class);
        Location baseLocation = new Location(world, 0, 65, 0);
        Location playerLocation = Mockito.spy(new Location(world, 1, 65, 0));
        Location floorLocation = Mockito.spy(new Location(world, 1, 64, 0));
        Block floorBlock = Mockito.mock(Block.class);

        // Floor block does not match the expected team block.
        when(playerLocation.clone()).thenReturn(floorLocation);
        when(floorLocation.subtract(0.0, 1.0, 0.0)).thenReturn(floorLocation);
        when(floorLocation.getBlock()).thenReturn(floorBlock);
        when(floorBlock.getType()).thenReturn(Material.RED_WOOL);

        CTFCaptureZoneService service = createService(teamManager);

        assertFalse(service.isInsideCaptureZone(TeamManager.BLUE, playerLocation, baseLocation));
        Bukkit.getLogger().info(LOG_PREFIX + "capture zone returns false for wrong floor block");
    }

    @Test
    void returnsFalseWhenInputsNull() {
        Bukkit.getLogger().info(LOG_PREFIX + "Capture zone: null inputs return false.");
        TeamManager teamManager = Mockito.mock(TeamManager.class);
        CTFCaptureZoneService service = createService(teamManager);

        assertFalse(service.isInsideCaptureZone(null, null, null));
        Bukkit.getLogger().info(LOG_PREFIX + "capture zone returns false on null inputs");
    }

    private CTFCaptureZoneService createService(TeamManager teamManager) {
        SpawnConfigHandler spawnConfigHandler = Mockito.mock(SpawnConfigHandler.class);
        when(spawnConfigHandler.getCaptureRadius(Mockito.anyDouble()))
            .thenAnswer(invocation -> invocation.getArgument(0, Double.class));
        return new CTFCaptureZoneService(spawnConfigHandler, teamManager);
    }
}

