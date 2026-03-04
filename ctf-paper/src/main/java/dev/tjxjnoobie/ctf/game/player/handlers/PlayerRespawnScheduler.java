package dev.tjxjnoobie.ctf.game.player.handlers;

import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.util.tasks.GameTaskOrchestrator;

/**
 * Owns respawn scheduling side effects for players.
 */
public final class PlayerRespawnScheduler {

    /**
     * Executes scheduleInstantRespawn.
     *
     * @param player Player involved in this operation.
     */

    // == Lifecycle ==

    public void scheduleInstantRespawn(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        // Schedule a sync respawn to avoid async Bukkit access.
        GameTaskOrchestrator.startLater(null, player.spigot()::respawn, 0L);
    }
}
