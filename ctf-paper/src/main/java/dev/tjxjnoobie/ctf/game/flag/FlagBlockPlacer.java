package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PluginConfigDependencyAccess;
/**
 * Owns flag block placement/clearing and metadata lookup.
 */
public final class FlagBlockPlacer implements PluginConfigDependencyAccess, FlagDependencyAccess, PlayerDependencyAccess {

    // == Constants ==
    private static final String FLAG_METADATA_KEY = CTFKeys.flagTeamMetadataTag();

    // == Getters ==
    public TeamId resolveFlagTeamAtBlockLocation(Location blockLocation, FlagStateRegistry flagStateRegistry) {
        boolean conditionResult1 = blockLocation == null || blockLocation.getBlock() == null || flagStateRegistry == null;
        // Guard: short-circuit when blockLocation == null || blockLocation.getBlock() == null || flagStateRegistry == null.
        if (conditionResult1) {
            return null;
        }

        String teamKey = null;
        for (MetadataValue value : blockLocation.getBlock().getMetadata(FLAG_METADATA_KEY)) {
            boolean conditionResult2 = value.getOwningPlugin() == getMainPlugin();
            if (conditionResult2) {
                teamKey = value.asString();
                break;
            }
        }

        TeamId teamId = TeamId.fromKey(teamKey);
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return null;
        }

        FlagMetaData flag = flagStateRegistry.flagFor(teamId);
        // Guard: short-circuit when flag == null || flag.activeLocation == null.
        if (flag == null || flag.activeLocation == null) {
            return null;
        }

        return LocationFormatUtils.sameBlock(flag.activeLocation, blockLocation) ? teamId : null;
    }

    private Material resolveBaseFlagMaterial(TeamId teamId) {
        return teamId == null ? null : getFlagMaterial(teamId.key(), getTeamManager().getFlagMaterial(teamId));
    }

    private Material resolveDroppedFlagMaterial(TeamId teamId) {
        return teamId == null ? null : getTeamManager().getCaptureMaterial(teamId);
    }

    // == Utilities ==
    /**
     * Executes clearFlagBlock.
     *
     * @param location World location used by this operation.
     * @param teamId Team identifier used for lookup or state updates.
     */
    public void clearFlagBlock(Location location, TeamId teamId) {
        // Guard: short-circuit when location == null.
        if (location == null) {
            return;
        }

        location.getBlock().removeMetadata(FLAG_METADATA_KEY, getMainPlugin());
        location.getBlock().setType(Material.AIR);
        if (teamId != null) {
            getFlagIndicator().removeFlagIndicatorForTeam(teamId.key());
        }
    }

    /**
     * Executes placeBaseFlagBlock.
     *
     * @param location World location used by this operation.
     * @param teamId Team identifier used for lookup or state updates.
     * @param indicatorLocation Location used for flag/base placement or fallback logic.
     */
    public void placeBaseFlagBlock(Location location, TeamId teamId, Location indicatorLocation) {
        // Guard: short-circuit when location == null || teamId == null.
        if (location == null || teamId == null) {
            return;
        }

        Material material = resolveBaseFlagMaterial(teamId);
        // Guard: short-circuit when material == null.
        if (material == null) {
            return;
        }

        location.getBlock().setType(material);
        location.getBlock().setMetadata(FLAG_METADATA_KEY, new FixedMetadataValue(getMainPlugin(), teamId.key()));
        Location resolvedIndicatorLocation = indicatorLocation == null ? LocationFormatUtils.toIndicatorLocation(location) : indicatorLocation;
        if (resolvedIndicatorLocation != null) {
            getFlagIndicator().spawnFlagIndicatorForTeam(teamId.key(), resolvedIndicatorLocation);
        }
    }

    /**
     * Executes placeBaseFlagBlockWithoutIndicator.
     *
     * @param location World location used by this operation.
     * @param teamId Team identifier used for lookup or state updates.
     */
    public void placeBaseFlagBlockWithoutIndicator(Location location, TeamId teamId) {
        // Guard: short-circuit when location == null || teamId == null.
        if (location == null || teamId == null) {
            return;
        }

        Material material = resolveBaseFlagMaterial(teamId);
        // Guard: short-circuit when material == null.
        if (material == null) {
            return;
        }

        location.getBlock().setType(material);
        location.getBlock().setMetadata(FLAG_METADATA_KEY, new FixedMetadataValue(getMainPlugin(), teamId.key()));
    }

    /**
     * Executes placeDroppedFlagBlock.
     *
     * @param location World location used by this operation.
     * @param teamId Team identifier used for lookup or state updates.
     */
    public void placeDroppedFlagBlock(Location location, TeamId teamId) {
        // Guard: short-circuit when location == null || teamId == null.
        if (location == null || teamId == null) {
            return;
        }

        Material material = resolveDroppedFlagMaterial(teamId);
        // Guard: short-circuit when material == null.
        if (material == null) {
            return;
        }

        location.getBlock().setType(material);
        location.getBlock().setMetadata(FLAG_METADATA_KEY, new FixedMetadataValue(getMainPlugin(), teamId.key()));
        Location indicatorLocation = LocationFormatUtils.toIndicatorLocation(location);
        if (indicatorLocation != null) {
            getFlagIndicator().spawnFlagIndicatorForTeam(teamId.key(), indicatorLocation);
        }
    }
}

