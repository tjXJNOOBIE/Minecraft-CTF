package dev.tjxjnoobie.ctf.game.state;

public enum GameState {
    // Waiting in lobby.
    LOBBY,
    // Active match is running.
    IN_PROGRESS,
    // Match is in overtime.
    OVERTIME,
    // Cleanup and shutdown state.
    CLEAN_UP
}

