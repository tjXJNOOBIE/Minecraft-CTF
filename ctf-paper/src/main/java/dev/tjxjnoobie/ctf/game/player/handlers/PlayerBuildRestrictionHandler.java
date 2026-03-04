package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerBuildGuardEventHandler;

import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.game.ArenaGuardUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Owns build/inventory restrictions for players inside the arena.
 */
public final class PlayerBuildRestrictionHandler implements PlayerBuildGuardEventHandler, MessageAccess, BukkitMessageSender, PlayerDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerBuildEvent] ";

    // == Lifecycle ==
    // Core systems (plugin, game state, loop, debug)
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        var buildToggleUtil = getBuildToggleUtil();
        boolean shouldRestrictBuild = ArenaGuardUtil.shouldRestrictBuild(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictBuild.
        if (!shouldRestrictBuild) {
            return;
        }

        event.setCancelled(true);
        Component message = getMessage(CTFKeys.messageBuildBlockedErrorKey());
        sendMessage(player, message);
        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "Block break blocked - player=" + playerName);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        var buildToggleUtil = getBuildToggleUtil();
        boolean shouldRestrictBuild = ArenaGuardUtil.shouldRestrictBuild(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictBuild.
        if (!shouldRestrictBuild) {
            return;
        }

        event.setCancelled(true);
        Component message = getMessage(CTFKeys.messageBuildBlockedErrorKey());
        sendMessage(player, message);
        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "Block place blocked - player=" + playerName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Object whoClicked = event.getWhoClicked();
        // Guard: short-circuit when !(whoClicked instanceof Player player).
        if (!(whoClicked instanceof Player player)) {
            return;
        }

        var buildToggleUtil = getBuildToggleUtil();
        boolean shouldRestrictInventory = ArenaGuardUtil.shouldRestrictLobbyInventory(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictInventory.
        if (!shouldRestrictInventory) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Object whoClicked = event.getWhoClicked();
        // Guard: short-circuit when !(whoClicked instanceof Player player).
        if (!(whoClicked instanceof Player player)) {
            return;
        }

        var buildToggleUtil = getBuildToggleUtil();
        boolean shouldRestrictInventory = ArenaGuardUtil.shouldRestrictLobbyInventory(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictInventory.
        if (!shouldRestrictInventory) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Player player = event.getPlayer();
        var buildToggleUtil = getBuildToggleUtil();
        boolean shouldRestrictBuild = ArenaGuardUtil.shouldRestrictBuild(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictBuild.
        if (!shouldRestrictBuild) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {

        Player player = event.getPlayer();
        var buildToggleUtil = getBuildToggleUtil();
        boolean shouldRestrictInventory = ArenaGuardUtil.shouldRestrictLobbyInventory(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictInventory.
        if (!shouldRestrictInventory) {
            return;
        }

        event.setCancelled(true);
    }
}
