package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerTeleportEventHandler;

import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
/**
 * Owns teleport bridge into arena gameplay movement processing.
 */
public final class PlayerTeleportHandler implements PlayerTeleportEventHandler, BukkitMessageSender, FlagDependencyAccess, LifecycleDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerTeleportEvent] ";
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Guard: short-circuit when event == null.
        if (event == null) {
            return;
        }

        Location destination = event.getTo();
        // Guard: short-circuit when destination == null.
        if (destination == null) {
            return;
        }

        Player player = event.getPlayer();
        GameStateManager gameStateManager = getGameStateManager();
        FlagCarrierHandler flagCarrierHandler = getFlagCarrierHandler();
        FlagCarrierStateHandler flagCarrierStateHandler = getFlagCarrierStateHandler();
        boolean processed = FlagCarrierMovementHelper.processMovement(
            gameStateManager,
            flagCarrierHandler,
            flagCarrierStateHandler,
            player,
            destination
        );
        if (processed) {
            String playerName = player.getName();
            sendDebugMessage(LOG_PREFIX + "Teleport processed - player=" + playerName);
        }
    }
}
