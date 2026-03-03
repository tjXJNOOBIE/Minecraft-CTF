package dev.tjxjnoobie.ctf.game.metadata;

/**
 * Tracks per-match player stats (non-persistent).
 */
public final class PlayerMatchStats {
    private int captures;
    private int kills;
    private int deaths;

    public int getCaptures() {
        return captures;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void incrementCaptures() {
        captures++;
    }

    public void incrementKills() {
        kills++;
    }

    public void incrementDeaths() {
        deaths++;
    }
}

