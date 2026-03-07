package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import java.time.Duration;

/**
 * Dependency-access surface for scoreboard state and scoreboard refreshes.
 */
public interface ScoreboardDependencyAccess {
    default ScoreBoardManager getScoreBoardManager() { return DependencyLoaderAccess.findInstance(ScoreBoardManager.class); }

    default int scoreboardGetScore(String teamKey) {
        return getScoreBoardManager().getScore(teamKey);
    }

    default int scoreboardGetScoreLimit() {
        return getScoreBoardManager().getScoreLimit();
    }

    default int scoreboardSetScore(String teamKey, int score) {
        return getScoreBoardManager().setScore(teamKey, score);
    }

    default void scoreboardSetScoreLimit(int scoreLimit) {
        getScoreBoardManager().setScoreLimit(scoreLimit);
    }

    default void scoreboardUpdate(Duration remaining, GameState gameState) {
        getScoreBoardManager().updateScoreboards(remaining, gameState);
    }

    default int scoreboardIncrementScore(String teamKey) {
        return getScoreBoardManager().incrementScore(teamKey);
    }

    default void scoreboardResetScores() {
        getScoreBoardManager().resetScores();
    }

    default void scoreboardClearAll() {
        getScoreBoardManager().clearAll();
    }

    default void scoreboardClearStats() {
        getScoreBoardManager().clearStats();
    }
}
