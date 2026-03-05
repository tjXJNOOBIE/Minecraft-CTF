package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.CarrierInventoryTracker;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagIndicator;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.FlagUiDependencyAccess;
/**
 * Handles flag lifecycle transitions and flag UI lifecycle.
 */
public final class FlagLifecycleHandler implements BukkitMessageSender, FlagDependencyAccess, FlagUiDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[FlagLifecycleHandler] ";

    // == Lifecycle ==
    /**
     * Constructs a FlagLifecycleHandler instance.
     */
    public FlagLifecycleHandler() {
    }

    // == Utilities ==
    /**
     * Executes the onMatchStart operation.
     */
    public void onMatchStart() {
        resetFlagsToBase();
        getFlagUiTicker().resetCaptureZoneParticleTickCounter();
        getFlagUiTicker().startFlagUiUpdateTimer();
        sendDebugMessage(LOG_PREFIX + "Handler started - match active.");
    }

    /**
     * Executes the onMatchStop operation.
     */
    public void onMatchStop() {
        getFlagUiTicker().stopFlagUiUpdateTimer();
        getBossBarManager().clearAll();
        getFlagIndicator().removeAllFlagIndicators();
        getFlagUiTicker().clearTeamGlowVisuals();
        getCarrierEffects().clearCarrierEffectsForCarrierIds(getTrackedCarrierIdsSnapshot());
        getCarrierEffects().clearCarrierGlowTeamEntries();
        sendDebugMessage(LOG_PREFIX + "Handler stopped - indicators and boss bars cleared.");
    }

    /**
     * Executes resetFlagsToBase.
     */
    public void resetFlagsToBase() {
        getFlagStateRegistry().resetFlagsToBase(
            (teamId, droppedLocation) -> getFlagBlockPlacer().clearFlagBlock(droppedLocation, teamId),
            (teamId, baseLocation) -> getFlagBlockPlacer().placeBaseFlagBlock(
                baseLocation,
                teamId,
                getTeamBaseMetaDataResolver().resolveBaseIndicatorLocation(teamId, baseLocation)
            )
        );
        getFlagUiTicker().tickFlagUi();
    }

    /**
     * Executes resetFlagIndicators.
     */
    public void resetFlagIndicators() {
        List<Location> indicatorLocations = new ArrayList<>();
        for (Map.Entry<TeamId, FlagMetaData> entry : getFlagMetaDataByTeamId().entrySet()) {
            TeamBaseMetaData baseData = getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(entry.getKey());
            Location indicator = baseData == null ? null : baseData.getIndicatorSpawnLocation();
            if (indicator != null) {
                indicatorLocations.add(indicator);
            }
        }

        getFlagIndicator().resetFlagIndicators(indicatorLocations);
        getFlagUiTicker().ensureFlagIndicators();
        getFlagUiTicker().syncIndicatorVisibilityNow();
    }

    /**
     * Executes syncIndicatorVisibility.
     */
    public void syncIndicatorVisibility() {
        getFlagUiTicker().syncIndicatorVisibilityNow();
    }
}

