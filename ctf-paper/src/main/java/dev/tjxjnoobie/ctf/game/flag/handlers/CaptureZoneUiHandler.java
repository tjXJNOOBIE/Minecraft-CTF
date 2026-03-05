package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.CaptureZoneParticleRenderer;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles periodic capture-zone particle border rendering.
 */
public final class CaptureZoneUiHandler implements FlagDependencyAccess, PlayerDependencyAccess {

    // == Constants ==
    private static final int CAPTURE_ZONE_PARTICLE_PERIOD_TICKS = 10;

    // == Runtime state ==
    private int captureZoneParticleTickCounter;

    // == Utilities ==
    /**
     * Executes resetCaptureZoneParticleTickCounter.
     */
    public void resetCaptureZoneParticleTickCounter() {
        captureZoneParticleTickCounter = 0;
    }

    /**
     * Executes renderCaptureZoneBorders.
     */
    public void renderCaptureZoneBorders() {
        captureZoneParticleTickCounter += 2;
        // Guard: short-circuit when captureZoneParticleTickCounter < CAPTURE_ZONE_PARTICLE_PERIOD_TICKS.
        if (captureZoneParticleTickCounter < CAPTURE_ZONE_PARTICLE_PERIOD_TICKS) {
            return;
        }
        captureZoneParticleTickCounter = 0;

        double captureRadius = getCaptureRadius();
        for (TeamId teamId : TeamId.values()) {
            renderCaptureZoneBorderForTeam(teamId, captureRadius);
        }
    }

    private void renderCaptureZoneBorderForTeam(TeamId teamId, double captureRadius) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return;
        }

        String teamKey = teamId.key();
        List<Player> viewers = getTeamManager().getTeamPlayers(teamKey);
        boolean empty = viewers.isEmpty();
        // Guard: short-circuit when empty.
        if (empty) {
            return;
        }

        TeamBaseMetaData baseData = getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(teamId);
        List<Location> returnPoints = baseData == null ? List.of() : baseData.getReturnSpawnLocations();
        Location baseLocation = baseData == null ? null : baseData.getFlagSpawnLocation();
        getCaptureZoneParticleRenderer().renderTeamZones(teamKey, viewers, returnPoints, baseLocation, captureRadius);
    }
}

