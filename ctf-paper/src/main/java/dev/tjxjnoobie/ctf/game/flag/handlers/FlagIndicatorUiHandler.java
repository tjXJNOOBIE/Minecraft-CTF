package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.FlagIndicator;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.Map;
import org.bukkit.Location;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles flag indicator spawn/removal and visibility refresh behavior.
 */
public final class FlagIndicatorUiHandler implements FlagDependencyAccess, PlayerDependencyAccess {

    // == Runtime state ==
    private int indicatorVisibilityTickCounter;

    // == Getters ==
    private Location resolveIndicatorSpawnLocation(TeamId teamId, FlagMetaData flag) {
        return getTeamBaseMetaDataResolver().resolveIndicatorSpawnLocation(teamId, flag);
    }

    // == Utilities ==
    /**
     * Executes ensureFlagIndicators.
     */
    public void ensureFlagIndicators() {
        for (Map.Entry<TeamId, FlagMetaData> entry : getFlagMetaDataByTeamId().entrySet()) {
            TeamId teamId = entry.getKey();
            FlagMetaData flag = entry.getValue();
            // Guard: short-circuit when teamId == null || flag == null.
            if (teamId == null || flag == null) {
                continue;
            }

            String teamKey = teamId.key();
            if (flag.state == FlagState.CARRIED || flag.activeLocation == null) {
                getFlagIndicator().removeFlagIndicatorForTeam(teamKey);
                continue;
            }

            Location indicatorLocation = resolveIndicatorSpawnLocation(teamId, flag);
            if (indicatorLocation == null) {
                getFlagIndicator().removeFlagIndicatorForTeam(teamKey);
                continue;
            }

            boolean conditionResult1 = !getFlagIndicator().hasFlagIndicatorForTeam(teamKey);
            if (conditionResult1) {
                getFlagIndicator().spawnFlagIndicatorForTeam(teamKey, indicatorLocation);
            }
        }
    }

    /**
     * Executes syncIndicatorVisibilityNow.
     */
    public void syncIndicatorVisibilityNow() {
        getFlagIndicator().syncVisibility(getTeamManager().getJoinedPlayers());
    }

    /**
     * Executes tickIndicatorVisibility.
     */
    public void tickIndicatorVisibility() {
        indicatorVisibilityTickCounter += 2;
        // Guard: short-circuit when indicatorVisibilityTickCounter < 20.
        if (indicatorVisibilityTickCounter < 20) {
            return;
        }
        indicatorVisibilityTickCounter = 0;
        syncIndicatorVisibilityNow();
    }
}

