package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import java.time.Duration;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.ScoreboardDependencyAccess;
/**
 * Handles score updates and win/overtime rules on successful flag capture.
 */
public final class FlagScoreHandler implements FlagDependencyAccess, LifecycleDependencyAccess, PlayerDependencyAccess, TaskDependencyAccess, ScoreboardDependencyAccess {
    // Core systems (plugin, game state, loop, debug)
    /**
     * Executes the processFlagCapture operation.
     *
     * @param player Player involved in this operation.
     * @param scoringTeam Team key used for lookup or state updates.
     * @param capturedFlagTeam Team key used for lookup or state updates.
     * @return {@code true} when the operation succeeds; otherwise {@code false}.
     */

    // == Lifecycle ==

    public int processFlagCapture(Player player, String scoringTeam, String capturedFlagTeam) {
        TeamId scoringTeamId = TeamId.fromKey(scoringTeam);
        TeamId capturedTeamId = TeamId.fromKey(capturedFlagTeam);
        int newScore = getScoreBoardManager().incrementScore(scoringTeam);
        Duration remainingTime = getRemainingTime();
        GameState gameState = getGameStateManager().getGameState();
        getPlayerManager().recordCapture(player, remainingTime, gameState);

        getFlagEventEffects().showFlagCaptureTitle(player, scoringTeamId);
        String playerName = player.getName();
        getDebugFeed().send("capture scorer=" + playerName + " team=" + scoringTeamId + " captured="
            + capturedTeamId + " score=" + newScore);

        if (gameState == GameState.OVERTIME) {
            getMatchCleanupHandler().requestMatchStop(MatchStopReason.WIN, scoringTeam);
            return newScore;
        }

        int scoreLimit = getScoreBoardManager().getScoreLimit();
        int winScore = Math.max(1, scoreLimit);
        if (newScore >= winScore) {
            getMatchCleanupHandler().requestMatchStop(MatchStopReason.WIN, scoringTeam);
        }
        return newScore;
    }
}

