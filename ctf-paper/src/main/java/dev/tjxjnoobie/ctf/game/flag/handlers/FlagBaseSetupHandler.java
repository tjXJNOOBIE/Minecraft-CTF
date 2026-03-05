package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigData;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles flag base setup/config bootstrapping and base readiness queries.
 */
public final class FlagBaseSetupHandler implements BukkitMessageSender, FlagDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[FlagBaseSetupHandler] ";

    // == Lifecycle ==
    /**
     * Constructs a FlagBaseSetupHandler instance.
     */
    public FlagBaseSetupHandler() {
    }

    /**
     * Executes initializeFlagsFromConfig.
     */
    public void initializeFlagsFromConfig() {
        getFlagStateRegistry().registerDefaultFlags();
        loadFlagFromConfig(TeamId.RED);
        loadFlagFromConfig(TeamId.BLUE);
    }

    private void loadFlagFromConfig(TeamId teamId) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return;
        }

        String teamKey = teamId.key();
        FlagConfigData flagConfig = getFlagData(teamKey);
        boolean conditionResult2 = flagConfig == null || flagConfig.getBaseLocation() == null;
        // Guard: short-circuit when flagConfig == null || flagConfig.getBaseLocation() == null.
        if (conditionResult2) {
            return;
        }

        FlagMetaData flag = getFlagStateRegistry().flagFor(teamId);
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return;
        }

        Location base = LocationFormatUtils.toBlockLocation(flagConfig.getBaseLocation());
        flag.baseLocation = base;
        getFlagStateRegistry().setAtBase(teamId, base);
        getFlagBlockPlacer().placeBaseFlagBlockWithoutIndicator(base, teamId);
    }

    // == Getters ==
    public Location getBaseLocation(TeamId teamId) {
        TeamBaseMetaData baseData = getTeamBaseMetaData(teamId);
        // Guard: short-circuit when baseData == null.
        if (baseData == null) {
            return null;
        }
        Location flagSpawn = baseData.getFlagSpawnLocation();
        return LocationFormatUtils.cloneLocation(flagSpawn);
    }

    public TeamBaseMetaData getTeamBaseMetaData(TeamId teamId) {
        return getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(teamId);
    }

    // == Setters ==
    public boolean setFlagBase(Player player, TeamId teamId) {
        // Guard: short-circuit when player == null || teamId == null.
        if (player == null || teamId == null) {
            return false;
        }

        FlagMetaData flag = getFlagStateRegistry().flagFor(teamId);
        // Guard: short-circuit when flag == null.
        if (flag == null) {
            return false;
        }

        String teamKey = teamId.key();
        Location playerLocation = player.getLocation();
        Location base = LocationFormatUtils.toBlockLocation(playerLocation);
        boolean conditionResult1 = flag.activeLocation != null && !LocationFormatUtils.sameBlock(flag.activeLocation, base);
        if (conditionResult1) {
            getFlagBlockPlacer().clearFlagBlock(flag.activeLocation, teamId);
        }

        Location indicator = LocationFormatUtils.toIndicatorLocation(base);
        Material flagMaterial = getTeamBaseMetaDataResolver().resolveFlagMaterial(teamId);
        FlagConfigData flagData = new FlagConfigData(base, indicator, flagMaterial);
        getFlagConfigHandler().setFlagData(teamKey, flagData);

        flag.baseLocation = base;
        getFlagStateRegistry().setAtBase(teamId, base);

        Location indicatorLocation = getTeamBaseMetaDataResolver().resolveBaseIndicatorLocation(teamId, base);
        getFlagBlockPlacer().placeBaseFlagBlock(base, teamId, indicatorLocation);
        getFlagUiTicker().tickFlagUi();

        String formattedLocation = LocationFormatUtils.formatBlockLocation(base);
        sendDebugMessage(LOG_PREFIX + "Base set - team=" + teamKey + " location=" + formattedLocation);
        return true;
    }

    // == Utilities ==
    /**
     * Returns the result of areBasesReady.
     *
     * @return {@code true} when the condition is satisfied; otherwise {@code false}.
     */
    public boolean areBasesReady() {
        return hasBase(TeamId.RED) && hasBase(TeamId.BLUE);
    }

    // == Predicates ==
    private boolean hasBase(TeamId teamId) {
        TeamBaseMetaData baseData = getTeamBaseMetaData(teamId);
        return baseData != null && baseData.getFlagSpawnLocation() != null;
    }
}

