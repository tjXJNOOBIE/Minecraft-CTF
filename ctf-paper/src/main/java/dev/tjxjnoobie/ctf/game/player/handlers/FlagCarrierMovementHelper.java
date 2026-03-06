package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class FlagCarrierMovementHelper {

    // == Lifecycle ==
    private FlagCarrierMovementHelper() {
        // Utility class.
    }

    // == Utilities ==
    static boolean processMovement(GameStateManager gameStateManager,
                                   FlagCarrierHandler flagCarrierHandler,
                                   FlagCarrierStateHandler flagCarrierStateHandler,
                                   Player player,
                                   Location destination) {
        // Guard: short-circuit when flagCarrierHandler == null || flagCarrierStateHandler == null.
        if (flagCarrierHandler == null || flagCarrierStateHandler == null) {
            return false;
        }

        boolean isMatchRunning = !gameStateManager.isCleanupInProgress() && gameStateManager.isRunning();
        flagCarrierHandler.processFlagCarrierMovement(player, destination, isMatchRunning);
        flagCarrierStateHandler.enforceCarrierFlagHotbarSlot(player);
        return true;
    }
}
