package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.events.handlers.FlagInteractEventHandler;

import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.InventoryUtilAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
/**
 * Owns interaction handling for flags and carrier restrictions.
 */
public final class FlagInteractHandler implements FlagInteractEventHandler, BukkitMessageSender, FlagDependencyAccess, LifecycleDependencyAccess, InventoryUtilAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFFlagInteractEvent] ";
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        Action action = event.getAction();
        // Guard: short-circuit when action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK.
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        // Guard: short-circuit when clickedBlock == null.
        if (clickedBlock == null) {
            return;
        }

        Location location = clickedBlock.getLocation();
        boolean isMatchRunning = !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
        boolean conditionResult1 = getFlagCarrierHandler() != null && getFlagCarrierHandler().processFlagTouch(player, location, isMatchRunning);
        if (conditionResult1) {
            event.setCancelled(true);
            String actionName = action.name();
            String formattedLocation = LocationFormatUtils.formatBlockLocation(location);
            sendDebugMessage(LOG_PREFIX + "Interact handled - player=" + playerName
                    + " action=" + actionName + " block=" + formattedLocation);
        }
    }
}
