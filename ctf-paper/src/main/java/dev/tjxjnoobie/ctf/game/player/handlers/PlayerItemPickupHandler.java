package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.dependency.interfaces.MatchSessionDependencyAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerItemPickupEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

/**
 * Blocks arena players from collecting loose items while they are in CTF.
 */
public final class PlayerItemPickupHandler implements PlayerItemPickupEventHandler, MatchSessionDependencyAccess {

    @Override
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        if (event == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!sessionIsPlayerInArena(player)) {
            return;
        }

        event.setCancelled(true);
    }
}
