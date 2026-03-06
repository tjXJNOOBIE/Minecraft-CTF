package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
import dev.tjxjnoobie.ctf.game.celebration.managers.WinnerCelebration;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;

public interface LifecycleDependencyAccess {
    default GameStateManager getGameStateManager() { return DependencyLoaderAccess.findInstance(GameStateManager.class); }
    default WinnerCelebration getWinnerCelebration() { return DependencyLoaderAccess.findInstance(WinnerCelebration.class); }
    default MatchFlowHandler getMatchFlowHandler() { return DependencyLoaderAccess.findInstance(MatchFlowHandler.class); }
    default MatchCleanupHandler getMatchCleanupHandler() { return DependencyLoaderAccess.findInstance(MatchCleanupHandler.class); }

    default GameState gameState() {
        return getGameStateManager().getGameState();
    }

    default void setGameState(GameState gameState) {
        getGameStateManager().setGameState(gameState);
    }

    default boolean isGameRunning() {
        return getGameStateManager().isRunning();
    }

    default boolean isCleanupInProgress() {
        return getGameStateManager().isCleanupInProgress();
    }

    default void setCleanupInProgress(boolean cleanupInProgress) {
        getGameStateManager().setCleanupInProgress(cleanupInProgress);
    }

    default boolean isForcedCountdown() {
        return getGameStateManager().isForcedCountdown();
    }

    default void setForcedCountdown(boolean forcedCountdown) {
        getGameStateManager().setForcedCountdown(forcedCountdown);
    }

    default int getActiveStartCountdownTotal() {
        return getGameStateManager().getActiveStartCountdownTotal();
    }

    default void setActiveStartCountdownTotal(int total) {
        getGameStateManager().setActiveStartCountdownTotal(total);
    }

    default long getLastEndCountdownAnnounced() {
        return getGameStateManager().getLastEndCountdownAnnounced();
    }

    default void setLastEndCountdownAnnounced(long seconds) {
        getGameStateManager().setLastEndCountdownAnnounced(seconds);
    }

    default void resetCountdownState() {
        getGameStateManager().resetCountdownState();
    }

    default boolean requestMatchStart(boolean force) {
        return getMatchFlowHandler().requestMatchStart(force);
    }

    default int getAllowedMatchTimeSeconds() {
        return getMatchFlowHandler().getAllowedMatchTimeSeconds();
    }

    default boolean setMatchTimeSeconds(long seconds) {
        return getMatchFlowHandler().setMatchTimeSeconds(seconds);
    }

    default void startAutoMatchStartCountdownTimer() {
        getMatchFlowHandler().startAutoMatchStartCountdownTimer();
    }

    default void requestMatchStop(MatchStopReason reason, String winningTeamKey) {
        getMatchCleanupHandler().requestMatchStop(reason, winningTeamKey);
    }

    default void startLobbyWaitingTimer() {
        getMatchCleanupHandler().startLobbyWaitingTimer();
    }

    default void shutdownMatchSystem() {
        getMatchCleanupHandler().shutdownMatchSystem();
    }

    default void onPlayerJoinedDuringMatch() {
        getMatchCleanupHandler().onPlayerJoinedDuringMatch();
    }

    default void onPlayerLeftDuringMatch() {
        getMatchCleanupHandler().onPlayerLeftDuringMatch();
    }

    default void startWinnerCelebration(String winningTeamKey, long durationMs) {
        getWinnerCelebration().start(winningTeamKey, durationMs);
    }

    default void cancelWinnerCelebration() {
        getWinnerCelebration().cancel();
    }
}
