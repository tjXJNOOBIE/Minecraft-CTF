package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.player.managers.BuildBypassManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Prevents building while players are inside an active CTF match.
 */
public final class CTFPlayerBuildEvent implements Listener, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFPlayerBuildEvent] ";
    private final CtfMatchOrchestrator gameManager;
    private final BuildBypassManager buildBypassManager;

    public CTFPlayerBuildEvent(CtfMatchOrchestrator gameManager, BuildBypassManager buildBypassManager) {
        this.gameManager = gameManager;
        this.buildBypassManager = buildBypassManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        if (buildBypassManager != null && buildBypassManager.canBypass(player)) {
            return;
        }

        // Edge Case: block breaking is disabled once joined to CTF.
        event.setCancelled(true);
        player.sendMessage(msg("error.build_blocked"));
        Bukkit.getLogger().info(LOG_PREFIX + "Block break blocked - player=" + player.getName());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        if (buildBypassManager != null && buildBypassManager.canBypass(player)) {
            return;
        }

        // Edge Case: block placing is disabled once joined to CTF.
        event.setCancelled(true);
        player.sendMessage(msg("error.build_blocked"));
        Bukkit.getLogger().info(LOG_PREFIX + "Block place blocked - player=" + player.getName());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        if (buildBypassManager != null && buildBypassManager.canBypass(player)) {
            return;
        }
        if (gameManager.getGameState() != GameState.LOBBY) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        if (buildBypassManager != null && buildBypassManager.canBypass(player)) {
            return;
        }
        if (gameManager.getGameState() != GameState.LOBBY) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        if (buildBypassManager != null && buildBypassManager.canBypass(player)) {
            return;
        }
        if (gameManager.getGameState() != GameState.LOBBY) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        if (buildBypassManager != null && buildBypassManager.canBypass(player)) {
            return;
        }
        if (gameManager.getGameState() != GameState.LOBBY) {
            return;
        }

        event.setCancelled(true);
    }
}




