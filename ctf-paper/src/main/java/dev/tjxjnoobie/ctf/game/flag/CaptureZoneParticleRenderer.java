package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Renders capture-zone border particles around return points/base.
 */
public final class CaptureZoneParticleRenderer {
    private static final int RING_POINTS = 28;

    /**
     * Renders ring borders around return zones for team viewers.
     * Falls back to base center when no return points are configured.
     */
    public void renderTeamZones(String teamKey, List<Player> viewers, List<Location> returnPoints,
                                Location baseFallback, double radius) {
        if (viewers == null || viewers.isEmpty()) {
            return;
        }
        if (radius <= 0.0) {
            return;
        }

        List<Location> centers = new ArrayList<>();
        if (returnPoints != null) {
            for (Location returnPoint : returnPoints) {
                if (returnPoint != null && returnPoint.getWorld() != null) {
                    centers.add(returnPoint);
                }
            }
        }
        if (centers.isEmpty() && baseFallback != null && baseFallback.getWorld() != null) {
            centers.add(baseFallback);
        }
        if (centers.isEmpty()) {
            return;
        }

        Particle particle = resolveParticle(teamKey);
        for (Location center : centers) {
            renderRing(viewers, center, radius, particle);
        }
    }

    private void renderRing(List<Player> viewers, Location center, double radius, Particle particle) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        double y = center.getY() + 0.15;
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = (Math.PI * 2.0 * i) / RING_POINTS;
            double x = center.getX() + Math.cos(angle) * radius + 0.5;
            double z = center.getZ() + Math.sin(angle) * radius + 0.5;
            Location particleLocation = new Location(world, x, y, z);
            for (Player viewer : viewers) {
                if (viewer.getWorld().equals(world)) {
                    viewer.spawnParticle(particle, particleLocation, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private Particle resolveParticle(String teamKey) {
        if (TeamManager.RED.equals(teamKey)) {
            return Particle.FLAME;
        }
        return Particle.SOUL_FIRE_FLAME;
    }
}

