package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles own flag return transitions.
 */
public final class FlagReturnHandler implements BukkitMessageSender, FlagDependencyAccess {

    // == Getters ==
    private Location resolveBaseIndicatorLocation(TeamId teamId, Location baseLocation) {
        // Guard: short-circuit when teamId == null || baseLocation == null.
        if (teamId == null || baseLocation == null) {
            return LocationFormatUtils.toIndicatorLocation(baseLocation);
        }
        String teamKey = teamId.key();
        Location fallback = LocationFormatUtils.toIndicatorLocation(baseLocation);
        return getFlagIndicatorLocation(teamKey)
            .orElse(fallback);
    }

    // == Utilities ==
    /**
     * Returns the result of returnDroppedOwnFlagToBase.
     *
     * @param player Player involved in this operation.
     * @param teamId Team identifier used for lookup or state updates.
     * @return Result produced by this method.
     */
    public boolean returnDroppedOwnFlagToBase(Player player, TeamId teamId) {
        FlagMetaData flag = getFlagStateRegistry().flagFor(teamId); // Validation & early exits
        Location baseLocation = LocationFormatUtils.cloneLocation(flag == null ? null : flag.baseLocation);
        // Guard: short-circuit when flag == null || flag.state != FlagState.DROPPED || baseLocation == null.
        if (flag == null || flag.state != FlagState.DROPPED || baseLocation == null) {
            return false;
        }

        // World/application side effects
        if (flag.activeLocation != null) {
            getFlagBlockPlacer().clearFlagBlock(flag.activeLocation, teamId);
        }

        // State transition
        getFlagStateRegistry().setAtBase(teamId, baseLocation);
        getFlagBlockPlacer().placeBaseFlagBlock(baseLocation, teamId, resolveBaseIndicatorLocation(teamId, baseLocation));

        // UX feedback
        getFlagEventEffects().showFlagReturnMessages(player, teamId);
        getFlagEventEffects().playFlagReturnEffects(teamId, baseLocation);

        String playerName = player.getName(); // Debug/telemetry
        sendDebugMessage("flag return team=" + teamId + " player=" + playerName);

        // UX feedback
        getFlagUiTicker().tickFlagUi();
        return true;
    }
}

