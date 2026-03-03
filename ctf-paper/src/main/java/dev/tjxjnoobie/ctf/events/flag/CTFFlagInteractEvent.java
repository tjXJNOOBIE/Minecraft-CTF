package dev.tjxjnoobie.ctf.events.flag;

import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CTFFlagInteractEvent implements Listener {
    private static final int FLAG_SLOT = 0;
    private static final String LOG_PREFIX = "[CTF] [CTFFlagInteractEvent] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final FlagManager flagManager;

    public CTFFlagInteractEvent(CtfMatchOrchestrator gameManager, FlagManager flagManager) {
        this.gameManager = gameManager;
        this.flagManager = flagManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (flagManager.isFlagCarrier(player.getUniqueId())
            && player.getInventory().getHeldItemSlot() == FLAG_SLOT) {
            // Edge Case: flag carriers cannot interact while holding the flag item.
            event.setCancelled(true);
            flagManager.lockCarrierHotbarSlot(player);
            Bukkit.getLogger().info(LOG_PREFIX + "Carrier interaction blocked - player=" + player.getName());
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            // Edge Case: ignore non-block interactions.
            return;
        }

        if (event.getClickedBlock() == null) {
            // Edge Case: ignore interactions without a block target.
            return;
        }

        Location location = event.getClickedBlock().getLocation();
        if (gameManager.handleFlagTouch(player, location)) {
            event.setCancelled(true);
            String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
            Bukkit.getLogger().info(LOG_PREFIX + "Interact handled - player=" + player.getName()
                + " action=" + action.name()
                + " block=" + worldName + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
    }
}



