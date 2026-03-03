package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles flag carrier interaction rules and delegates to FlagManager.
 */
public final class FlagCarrierHandler {
    private final TeamManager teamManager;
    private final FlagManager flagManager;

    public FlagCarrierHandler(TeamManager teamManager, FlagManager flagManager) {
        this.teamManager = teamManager;
        this.flagManager = flagManager;
    }

    public boolean handleFlagTouch(Player player, Location blockLocation, boolean running) {
        if (!running || player == null || blockLocation == null) {
            return false;
        }
        if (teamManager.getTeamKey(player) == null) {
            return false;
        }
        return flagManager.handleFlagTouch(player, blockLocation);
    }

    public void handleMove(Player player, Location to, boolean running) {
        if (!running || player == null || to == null) {
            return;
        }
        if (teamManager.getTeamKey(player) == null) {
            return;
        }
        flagManager.handleMove(player, to);
    }
}

