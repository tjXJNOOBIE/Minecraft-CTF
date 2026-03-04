package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerMoveEventHandler;

import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
/**
 * Owns player movement bridge into arena gameplay movement processing.
 */
public final class PlayerMoveHandler implements PlayerMoveEventHandler, BukkitMessageSender, FlagDependencyAccess, LifecycleDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerMoveEvent] ";
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        Location destination = event.getTo();
        if (destination == null) {
            String playerName = player.getName();
            sendDebugMessage(LOG_PREFIX + "Move ignored - no destination player=" + playerName);
            return;
        }

        Location origin = event.getFrom();
        int fromBlockX = origin.getBlockX();
        int fromBlockY = origin.getBlockY();
        int fromBlockZ = origin.getBlockZ();
        int toBlockX = destination.getBlockX();
        int toBlockY = destination.getBlockY();
        int toBlockZ = destination.getBlockZ();
        boolean sameBlock = fromBlockX == toBlockX
            && fromBlockY == toBlockY
            && fromBlockZ == toBlockZ;
        // Guard: short-circuit when sameBlock.
        if (sameBlock) {
            return;
        }

        GameStateManager gameStateManager = getGameStateManager();
        FlagCarrierHandler flagCarrierHandler = getFlagCarrierHandler();
        FlagCarrierStateHandler flagCarrierStateHandler = getFlagCarrierStateHandler();
        FlagCarrierMovementHelper.processMovement(
            gameStateManager,
            flagCarrierHandler,
            flagCarrierStateHandler,
            player,
            destination
        );
    }
}
