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

    // == Constants ==
    private static final int RING_POINTS = 28;

    // == Getters ==
    private Particle resolveParticle(String teamKey) {
        boolean equalsResult = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult.
        if (equalsResult) {
            return Particle.FLAME;
        }
        return Particle.SOUL_FIRE_FLAME;
    }

    // == Utilities ==
    /**
     * Executes renderTeamZones.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @param viewers Collection or state object updated by this operation.
     * @param returnPoints Collection or state object updated by this operation.
     * @param baseFallback Location used for flag/base placement or fallback logic.
     * @param radius Particle or sound tuning value used for rendering/effects.
     */
    public void renderTeamZones(String teamKey, List<Player> viewers, List<Location> returnPoints,
                                Location baseFallback, double radius) {
        boolean conditionResult1 = viewers == null || viewers.isEmpty();
        // Guard: short-circuit when viewers == null || viewers.isEmpty().
        if (conditionResult1) {
            return;
        }
        // Guard: short-circuit when radius <= 0.0.
        if (radius <= 0.0) {
            return;
        }

        List<Location> centers = new ArrayList<>(); // Collect return-point centers (or base fallback).
        if (returnPoints != null) {
            for (Location returnPoint : returnPoints) {
                boolean conditionResult2 = returnPoint != null && returnPoint.getWorld() != null;
                if (conditionResult2) {
                    centers.add(returnPoint);
                }
            }
        }
        boolean conditionResult3 = centers.isEmpty() && baseFallback != null && baseFallback.getWorld() != null;
        if (conditionResult3) {
            centers.add(baseFallback);
        }
        boolean empty = centers.isEmpty();
        // Guard: short-circuit when empty.
        if (empty) {
            return;
        }

        Particle particle = resolveParticle(teamKey); // Render the ring for each center.
        for (Location center : centers) {
            renderRing(viewers, center, radius, particle);
        }
    }

    private void renderRing(List<Player> viewers, Location center, double radius, Particle particle) {
        World world = center.getWorld();
        // Guard: short-circuit when world == null.
        if (world == null) {
            return;
        }

        double y = center.getY() + 0.15; // Emit particles around a circle at the capture radius.
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = (Math.PI * 2.0 * i) / RING_POINTS;
            double x = center.getX() + Math.cos(angle) * radius + 0.5;
            double z = center.getZ() + Math.sin(angle) * radius + 0.5;
            Location particleLocation = new Location(world, x, y, z);
            for (Player viewer : viewers) {
                boolean conditionResult4 = viewer.getWorld().equals(world);
                if (conditionResult4) {
                    viewer.spawnParticle(particle, particleLocation, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }
}

