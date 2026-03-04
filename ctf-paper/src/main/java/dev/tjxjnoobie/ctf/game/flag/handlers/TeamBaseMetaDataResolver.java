package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.FlagConfigHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Material;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Resolves TeamBaseMetaData and indicator locations from flag/team state.
 */
public final class TeamBaseMetaDataResolver implements FlagDependencyAccess, PlayerDependencyAccess {

    // == Getters ==
    public TeamBaseMetaData resolveTeamBaseMetaData(TeamId teamId) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return null;
        }

        FlagMetaData flag = getFlagStateRegistry().flagFor(teamId);
        TeamBaseMetaData teamBaseMetaData = new TeamBaseMetaData();
        teamBaseMetaData.setTeamId(teamId);
        teamBaseMetaData.setFlagSpawnLocation(LocationFormatUtils.cloneLocation(flag == null ? null : flag.baseLocation));
        teamBaseMetaData.setFlagBlockLocation(LocationFormatUtils.cloneLocation(flag == null ? null : flag.activeLocation));
        teamBaseMetaData.setFlagBlockMaterial(resolveFlagMaterial(teamId));
        teamBaseMetaData.setIndicatorSpawnLocation(resolveBaseIndicatorLocation(teamId, flag));
        teamBaseMetaData.setBaseSpawnLocation(getTeamManager().getSpawn(teamId).map(Location::clone).orElse(null));
        teamBaseMetaData.setReturnSpawnLocations(new ArrayList<>(getTeamManager().getReturnPoints(teamId)));
        return teamBaseMetaData;
    }

    public Location resolveIndicatorSpawnLocation(TeamId teamId, FlagMetaData flag) {
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return null;
        }

        if (flag.state == FlagState.AT_BASE) {
            TeamBaseMetaData baseData = getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(teamId);
            Location baseIndicator = baseData == null ? null : baseData.getIndicatorSpawnLocation();
            // Guard: short-circuit when baseIndicator != null.
            if (baseIndicator != null) {
                return baseIndicator;
            }
        }

        // Guard: short-circuit when flag.activeLocation != null.
        if (flag.activeLocation != null) {
            return LocationFormatUtils.toIndicatorLocation(flag.activeLocation);
        }

        return null;
    }

    public Location resolveBaseIndicatorLocation(TeamId teamId, FlagMetaData flag) {
        Location baseLocation = flag == null ? null : flag.baseLocation;
        return resolveBaseIndicatorLocation(teamId, baseLocation);
    }

    public Location resolveBaseIndicatorLocation(TeamId teamId, Location baseLocation) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return LocationFormatUtils.toIndicatorLocation(baseLocation);
        }
        Location indicator = getFlagIndicatorLocation(teamId.key()).orElse(null);
        // Guard: short-circuit when indicator != null.
        if (indicator != null) {
            return indicator;
        }
        return LocationFormatUtils.toIndicatorLocation(baseLocation);
    }

    public Material resolveFlagMaterial(TeamId teamId) {
        return teamId == null ? null : getFlagMaterial(teamId.key(), getTeamManager().getFlagMaterial(teamId));
    }
}

