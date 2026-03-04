package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.bukkit.Location;

/**
 * Owns mutable flag state and carrier lookup for all teams.
 */
public final class FlagStateRegistry {

    // == Runtime state ==
    private final Map<TeamId, FlagMetaData> flagMetaDataByTeamId = new EnumMap<>(TeamId.class);

    // == Lifecycle ==
    /**
     * Ensures the registry contains default state holders for both CTF teams.
     */
    public void registerDefaultFlags() {
        flagMetaDataByTeamId.putIfAbsent(TeamId.RED, new FlagMetaData());
        flagMetaDataByTeamId.putIfAbsent(TeamId.BLUE, new FlagMetaData());
    }

    // == Getters ==
    public Map<TeamId, FlagMetaData> getFlagMetaDataByTeamId() {
        return flagMetaDataByTeamId;
    }

    public Collection<FlagMetaData> getAllFlagMetaData() {
        return flagMetaDataByTeamId.values();
    }

    public TeamId findCarriedFlagTeam(UUID playerUUID) {
        // Guard: short-circuit when playerUUID == null.
        if (playerUUID == null) {
            return null;
        }

        // Scan the live registry to find which team flag, if any, is currently carried by this player.
        for (Map.Entry<TeamId, FlagMetaData> entry : flagMetaDataByTeamId.entrySet()) {
            FlagMetaData flag = entry.getValue();
            boolean conditionResult1 = flag != null && flag.state == FlagState.CARRIED && playerUUID.equals(flag.carrier);
            // Guard: short-circuit when flag != null && flag.state == FlagState.CARRIED && playerUUID.equals(flag.carrier).
            if (conditionResult1) {
                return entry.getKey();
            }
        }
        return null;
    }

    // == Setters ==
    public void setAtBase(TeamId teamId, Location baseLocation) {
        FlagMetaData flag = flagFor(teamId);
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return;
        }

        flag.state = FlagState.AT_BASE;
        flag.activeLocation = baseLocation;
        flag.carrier = null;
    }

    public void setDropped(TeamId teamId, Location droppedLocation) {
        FlagMetaData flag = flagFor(teamId);
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return;
        }

        flag.state = FlagState.DROPPED;
        flag.activeLocation = droppedLocation;
        flag.carrier = null;
    }

    public void setCarried(TeamId teamId, UUID carrierId) {
        FlagMetaData flag = flagFor(teamId);
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return;
        }

        flag.state = FlagState.CARRIED;
        flag.activeLocation = null;
        flag.carrier = carrierId;
    }

    // == Utilities ==
    /**
     * Returns the tracked flag state for the given team id.
     *
     * @param teamId Team identifier used for lookup or state updates.
     * @return Resolved value for the requested lookup.
     */
    public FlagMetaData flagFor(TeamId teamId) {
        return teamId == null ? null : flagMetaDataByTeamId.get(teamId);
    }

    /**
     * Restores every tracked flag to its base location and optionally mirrors the world blocks.
     *
     * @param clearDroppedFlagBlock Control flag that changes how this operation is executed.
     * @param placeBaseFlagBlock Control flag that changes how this operation is executed.
     */
    public void resetFlagsToBase(BiConsumer<TeamId, Location> clearDroppedFlagBlock,
                                 BiConsumer<TeamId, Location> placeBaseFlagBlock) {
        for (Map.Entry<TeamId, FlagMetaData> entry : flagMetaDataByTeamId.entrySet()) {
            TeamId teamId = entry.getKey();
            FlagMetaData flag = entry.getValue();
            Location baseLocation = LocationFormatUtils.cloneLocation(flag == null ? null : flag.baseLocation);
            // Guard: short-circuit when flag == null || baseLocation == null.
            if (flag == null || baseLocation == null) {
                continue;
            }

            if (flag.state == FlagState.DROPPED && flag.activeLocation != null && clearDroppedFlagBlock != null) {
                clearDroppedFlagBlock.accept(teamId, flag.activeLocation);
            }

            flag.state = FlagState.AT_BASE;
            flag.activeLocation = baseLocation;
            flag.carrier = null;

            if (placeBaseFlagBlock != null) {
                placeBaseFlagBlock.accept(teamId, baseLocation);
            }
        }
    }

    // == Predicates ==
    public boolean isFlagCarrier(UUID playerUUID) {
        return findCarriedFlagTeam(playerUUID) != null;
    }
}
