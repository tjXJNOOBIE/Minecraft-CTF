package dev.tjxjnoobie.ctf.game.player.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Tracks players who may bypass lobby build/inventory locks.
 */
public final class BuildToggleUtil {
    private final Set<UUID> bypassPlayers = new HashSet<>();

    // == Utilities ==
    /**
     * Returns the result of toggle.
     *
     * @param player Player involved in this operation.
     * @return Updated toggle state after applying this operation.
     */
    public boolean toggle(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return false;
        }

        UUID playerUUID = player.getUniqueId();
        boolean containsResult = bypassPlayers.contains(playerUUID);
        if (containsResult) {
            // Disable bypass when already enabled.
            bypassPlayers.remove(playerUUID);
            return false;
        }

        // Enable bypass when not currently set.
        bypassPlayers.add(playerUUID);
        return true;
    }

    /**
     * Executes clearPlayer.
     *
     * @param player Player involved in this operation.
     */
    public void clearPlayer(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }
        // Remove a single player from bypass tracking.
        bypassPlayers.remove(player.getUniqueId());
    }

    /**
     * Executes clear.
     */
    public void clear() {
        // Clear all bypass tracking.
        bypassPlayers.clear();
    }

    // == Predicates ==
    public boolean canBypass(Player player) {
        // Check whether the player is in the bypass set.
        return player != null && bypassPlayers.contains(player.getUniqueId());
    }
}
