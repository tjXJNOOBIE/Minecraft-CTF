package dev.tjxjnoobie.ctf.game.flag.interfaces;

import org.bukkit.entity.Player;

/**
 * Receives capture callbacks for win/stop handling.
 */
public interface FlagCaptureHandler {
    /**
     * Called after a capture and returns the updated score.
     */
    int onCapture(Player player, String scoringTeam, String capturedTeam);
}

