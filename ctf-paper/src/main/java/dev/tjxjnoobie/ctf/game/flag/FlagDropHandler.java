package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.util.FlagDropLocationUtil;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles carried flag drop transitions.
 */
public final class FlagDropHandler implements BukkitMessageSender, FlagDependencyAccess, PlayerDependencyAccess {

    // == Getters ==
    private Location resolveDroppedFlagLocation(Player player) {
        Location rawDrop = LocationFormatUtils.toBlockLocation(player.getLocation()); // Validation & early exits
        TeamId scoringTeam = getTeamManager().getTeamId(player);
        // Guard: short-circuit when scoringTeam == null.
        if (scoringTeam == null) {
            return rawDrop;
        }

        TeamBaseMetaData baseData = getTeamBaseMetaData(scoringTeam); // Domain lookup
        Location ownBaseLocation = baseData == null ? null : baseData.getFlagSpawnLocation();
        List<Location> returnPoints = baseData == null ? List.of() : baseData.getReturnSpawnLocations();

        return FlagDropLocationUtil.resolveDropLocation(
            player,
            rawDrop,
            ownBaseLocation,
            returnPoints,
            getCaptureRadius()
        );
    }

    private TeamBaseMetaData getTeamBaseMetaData(TeamId teamId) {
        return getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(teamId);
    }

    // == Utilities ==
    /**
     * Executes dropCarriedFlagIfPresent.
     *
     * @param player Player involved in this operation.
     */
    public void dropCarriedFlagIfPresent(Player player) {
        // Validation & early exits
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        TeamId carriedTeam = getFlagStateRegistry().findCarriedFlagTeam(player.getUniqueId());
        // Guard: short-circuit when carriedTeam == null.
        if (carriedTeam == null) {
            return;
        }

        FlagMetaData flag = getFlagStateRegistry().flagFor(carriedTeam);
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return;
        }

        Location dropLocation = resolveDroppedFlagLocation(player); // Domain lookup

        // State transition
        getFlagStateRegistry().setDropped(carriedTeam, dropLocation);
        getFlagBlockPlacer().placeDroppedFlagBlock(dropLocation, carriedTeam);

        // World/application side effects
        getCarrierInventoryTracker().restoreCarrierFlagItem(player);
        hideBossBar(player, BukkitBossBarType.CARRIER);
        getCarrierEffects().clearCarrierEffects(player);

        // UX feedback
        getFlagEventEffects().showFlagDropBroadcast(player, carriedTeam);

        String playerName = player.getName(); // Debug/telemetry
        sendDebugMessage("flag drop team=" + carriedTeam + " player=" + playerName);

        // UX feedback
        getFlagEventEffects().playFlagDropEffects(player, carriedTeam, dropLocation);
        getFlagUiTicker().tickFlagUi();
    }
}

