package dev.tjxjnoobie.ctf.game.state.managers;

import dev.tjxjnoobie.ctf.game.tags.GameState;

/**
 * Owns mutable game state and lifecycle flags for the match.
 */
public final class GameStateManager {
    private GameState gameState = GameState.LOBBY;
    private boolean cleanupInProgress;
    private boolean forcedCountdown;
    private int activeStartCountdownTotal;
    private int lonePlayerSecondsRemaining;
    private long lastEndCountdownAnnounced = -1L;

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        if (gameState != null) {
            this.gameState = gameState;
        }
    }

    public boolean isRunning() {
        return gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME;
    }

    public boolean isCleanupInProgress() {
        return cleanupInProgress;
    }

    public void setCleanupInProgress(boolean cleanupInProgress) {
        this.cleanupInProgress = cleanupInProgress;
    }

    public boolean isForcedCountdown() {
        return forcedCountdown;
    }

    public void setForcedCountdown(boolean forcedCountdown) {
        this.forcedCountdown = forcedCountdown;
    }

    public int getActiveStartCountdownTotal() {
        return activeStartCountdownTotal;
    }

    public void setActiveStartCountdownTotal(int activeStartCountdownTotal) {
        this.activeStartCountdownTotal = Math.max(0, activeStartCountdownTotal);
    }

    public int getLonePlayerSecondsRemaining() {
        return lonePlayerSecondsRemaining;
    }

    public void setLonePlayerSecondsRemaining(int lonePlayerSecondsRemaining) {
        this.lonePlayerSecondsRemaining = Math.max(0, lonePlayerSecondsRemaining);
    }

    public long getLastEndCountdownAnnounced() {
        return lastEndCountdownAnnounced;
    }

    public void setLastEndCountdownAnnounced(long lastEndCountdownAnnounced) {
        this.lastEndCountdownAnnounced = lastEndCountdownAnnounced;
    }

    public void resetCountdownState() {
        activeStartCountdownTotal = 0;
        lonePlayerSecondsRemaining = 0;
        lastEndCountdownAnnounced = -1L;
        forcedCountdown = false;
    }
}

