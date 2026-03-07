package dev.tjxjnoobie.ctf.game.flag.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Covers dropped-flag location behavior around scoring zones.
 */
class FlagDropLocationUtilTest extends TestLogSupport {
    @Test
    void shiftsDropsOutsideCaptureZoneWhenCarrierDiesAtOwnBase() {
        logStep("arranging dropped-flag location near a capture zone");
        World world = Mockito.mock(World.class);
        Player player = Mockito.mock(Player.class);
        Location rawDrop = new Location(world, 2, 64, 2);
        Location baseLocation = new Location(world, 2, 64, 2);

        // The facing direction becomes the fallback when the carrier dies exactly on the zone center.
        when(player.getLocation()).thenReturn(new Location(world, 2.5, 64, 2.5, 0.0f, 0.0f) {
            @Override
            public Vector getDirection() {
                return new Vector(1.0, 0.0, 0.0);
            }
        });

        Location resolvedDrop = FlagDropLocationUtil.resolveDropLocation(
            player,
            rawDrop,
            baseLocation,
            List.of(baseLocation),
            3.0
        );

        assertNotNull(resolvedDrop);
        assertEquals(world, resolvedDrop.getWorld());
        assertEquals(64, resolvedDrop.getBlockY());
        assertTrue(resolvedDrop.distanceSquared(baseLocation) > 9.0);
        assertEquals(6, resolvedDrop.getBlockX());
        assertEquals(2, resolvedDrop.getBlockZ());
        logStep("validated dropped flag shifts outside the capture radius");
    }
}
