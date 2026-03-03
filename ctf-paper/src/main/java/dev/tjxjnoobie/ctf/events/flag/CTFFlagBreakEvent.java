package dev.tjxjnoobie.ctf.events.flag;

import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class CTFFlagBreakEvent implements Listener {
    private static final String LOG_PREFIX = "[CTF] [CTFFlagBreakEvent] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;

    public CTFFlagBreakEvent(CtfMatchOrchestrator gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onFlagBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (gameManager.handleFlagTouch(player, location)) {
            event.setCancelled(true);
            String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
            Bukkit.getLogger().info(LOG_PREFIX + "Break handled - player=" + player.getName()
                + " block=" + worldName + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
    }
}



