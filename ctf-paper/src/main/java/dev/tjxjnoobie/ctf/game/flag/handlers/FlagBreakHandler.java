package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.events.handlers.FlagBreakEventHandler;

import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
/**
 * Owns block-break behavior for flag block touches.
 */
public final class FlagBreakHandler implements FlagBreakEventHandler, BukkitMessageSender, FlagDependencyAccess, LifecycleDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFFlagBreakEvent] ";
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        boolean isMatchRunning = !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
        boolean touchedFlagBlock = isMatchRunning
            && getFlagBlockPlacer().resolveFlagTeamAtBlockLocation(location, getFlagStateRegistry()) != null;
        boolean conditionResult1 = getFlagCarrierHandler() != null
            && getFlagCarrierHandler().processFlagTouch(player, location, isMatchRunning);
        if (conditionResult1 || touchedFlagBlock) {
            event.setCancelled(true);
            String playerName = player.getName();
            String formattedLocation = LocationFormatUtils.formatBlockLocation(location);
            sendDebugMessage(LOG_PREFIX + "Break handled - player=" + playerName
                + " block=" + formattedLocation);
        }
    }
}
