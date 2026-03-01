package dev.tjxjnoobie.ctf.game.tags;

/**
 * Enumerates reasons a CTF match may stop.
 */
public enum MatchStopReason {
    // Stopped manually by admin.
    ADMIN,
    // Ended due to time running out.
    TIMEOUT,
    // Ended due to a win condition.
    WIN,
    // Generic fallback reason.
    GENERIC
}

