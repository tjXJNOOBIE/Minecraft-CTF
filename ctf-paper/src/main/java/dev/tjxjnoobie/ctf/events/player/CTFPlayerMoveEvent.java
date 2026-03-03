package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class CTFPlayerMoveEvent implements Listener {
    private static final String LOG_PREFIX = "[CTF] [CTFPlayerMoveEvent] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final FlagManager flagManager;

    public CTFPlayerMoveEvent(CtfMatchOrchestrator gameManager, FlagManager flagManager) {
        this.gameManager = gameManager;
        this.flagManager = flagManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            // Edge Case: some move events do not have a destination location.
            Bukkit.getLogger().fine(LOG_PREFIX + "Move ignored - no destination player=" + event.getPlayer().getName());
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            // Edge Case: ignore move events that do not change block position.
            return;
        }

        gameManager.handleMove(event.getPlayer(), event.getTo());
        flagManager.lockCarrierHotbarSlot(event.getPlayer());
    }
}



