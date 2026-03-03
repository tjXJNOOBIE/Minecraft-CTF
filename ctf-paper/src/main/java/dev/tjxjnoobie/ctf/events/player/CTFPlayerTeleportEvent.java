package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Bridges teleport movement into the capture checks for carriers.
 */
public final class CTFPlayerTeleportEvent implements Listener {
    private static final String LOG_PREFIX = "[CTF] [CTFPlayerTeleportEvent] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final FlagManager flagManager;

    public CTFPlayerTeleportEvent(CtfMatchOrchestrator gameManager, FlagManager flagManager) {
        this.gameManager = gameManager;
        this.flagManager = flagManager;
    }

    @EventHandler
    /**
     * Re-runs move handling for teleports to ensure captures can trigger.
     */
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            // Edge Case: teleport without a destination should be ignored.
            return;
        }

        gameManager.handleMove(event.getPlayer(), event.getTo());
        flagManager.lockCarrierHotbarSlot(event.getPlayer());
        Bukkit.getLogger().fine(LOG_PREFIX + "Teleport processed - player=" + event.getPlayer().getName());
    }
}



