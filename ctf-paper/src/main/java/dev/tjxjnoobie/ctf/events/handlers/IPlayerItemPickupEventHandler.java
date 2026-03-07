package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

/**
 * Event contract for arena item-pickup restrictions.
 */
public interface IPlayerItemPickupEventHandler {
    void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event);
}
