package dev.tjxjnoobie.ctf.team;

import java.util.Locale;

/**
 * Strongly typed team identifier for CTF teams.
 */
public enum TeamId {
    RED(TeamManager.RED),
    BLUE(TeamManager.BLUE);

    // == State ==
    private final String key;

    // == Lifecycle ==
    TeamId(String key) {
        this.key = key;
    }

    // == Getters ==
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }

    // == Utilities ==
    public TeamId opposite() {
        return this == RED ? BLUE : RED;
    }

    public static TeamId fromKey(String key) {
        // Guard: short-circuit when key == null.
        if (key == null) {
            return null;
        }
        return switch (key.trim().toLowerCase(Locale.ROOT)) {
            case TeamManager.RED -> RED;
            case TeamManager.BLUE -> BLUE;
            default -> null;
        };
    }
}

