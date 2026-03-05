package dev.tjxjnoobie.ctf.game.state;

/**
 * Owns mutable game state and lifecycle flags for the match.
 */
public final class GameStateManager {

    // == Runtime state ==
    private GameState gameState = GameState.LOBBY;
    private boolean cleanupInProgress;
    private boolean forcedCountdown;
    private int activeStartCountdownTotal;
    private int lonePlayerSecondsRemaining;
    private long lastEndCountdownAnnounced = -1L;

    // == Getters ==
    public GameState getGameState() {
        // Return current game state.
        return gameState;
    }

    public int getActiveStartCountdownTotal() {
        // Return active countdown total in seconds.
        return activeStartCountdownTotal;
    }

    public int getLonePlayerSecondsRemaining() {
        // Return the remaining lone-player countdown.
        return lonePlayerSecondsRemaining;
    }

    public long getLastEndCountdownAnnounced() {
        // Return last announced end countdown value.
        return lastEndCountdownAnnounced;
    }

    // == Setters ==
    public void setGameState(GameState gameState) {
        if (gameState != null) {
            // Update the current game state.
            this.gameState = gameState;
        }
    }

    public void setCleanupInProgress(boolean cleanupInProgress) {
        // Toggle cleanup flag.
        this.cleanupInProgress = cleanupInProgress;
    }

    public void setForcedCountdown(boolean forcedCountdown) {
        // Update forced countdown flag.
        this.forcedCountdown = forcedCountdown;
    }

    public void setActiveStartCountdownTotal(int activeStartCountdownTotal) {
        // Clamp countdown total to non-negative.
        this.activeStartCountdownTotal = Math.max(0, activeStartCountdownTotal);
    }

    public void setLonePlayerSecondsRemaining(int lonePlayerSecondsRemaining) {
        // Clamp lone-player countdown to non-negative.
        this.lonePlayerSecondsRemaining = Math.max(0, lonePlayerSecondsRemaining);
    }

    public void setLastEndCountdownAnnounced(long lastEndCountdownAnnounced) {
        // Update last announced end countdown.
        this.lastEndCountdownAnnounced = lastEndCountdownAnnounced;
    }

    // == Utilities ==
    /**
     * Executes resetCountdownState.
     */
    public void resetCountdownState() {
        activeStartCountdownTotal = 0; // Reset countdown-related fields.
        lonePlayerSecondsRemaining = 0;
        lastEndCountdownAnnounced = -1L;
        forcedCountdown = false;
    }

    // == Predicates ==
    public boolean isRunning() {
        return gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME; // Match is running during active or overtime states.
    }

    public boolean isCleanupInProgress() {
        // True while cleanup is running.
        return cleanupInProgress;
    }

    public boolean isForcedCountdown() {
        // True when countdown is forced by admin.
        return forcedCountdown;
    }
}

