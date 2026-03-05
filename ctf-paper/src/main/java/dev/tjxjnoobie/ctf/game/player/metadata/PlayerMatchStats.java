package dev.tjxjnoobie.ctf.game.player.metadata;

/**
 * Tracks per-match player stats (non-persistent).
 */
public final class PlayerMatchStats {

    // == Runtime counters ==
    private int captures;
    private int kills;
    private int deaths;

    // == Getters ==
    public int getCaptures() {
        // Return capture count.
        return captures;
    }

    public int getKills() {
        // Return kill count.
        return kills;
    }

    public int getDeaths() {
        // Return death count.
        return deaths;
    }

    // == Utilities ==
    /**
     * Executes incrementCaptures.
     */
    public void incrementCaptures() {
        // Increment capture count.
        captures++;
    }

    /**
     * Executes incrementKills.
     */
    public void incrementKills() {
        // Increment kill count.
        kills++;
    }

    /**
     * Executes incrementDeaths.
     */
    public void incrementDeaths() {
        // Increment death count.
        deaths++;
    }
}

