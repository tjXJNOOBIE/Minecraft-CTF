package dev.tjxjnoobie.ctf.game.player.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Tracks players who may bypass lobby build/inventory locks.
 */
public final class BuildBypassManager {
    private final Set<UUID> bypassPlayers = new HashSet<>();

    /**
     * Toggles bypass state for the player.
     *
     * @return true when bypass is now enabled.
     */
    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (bypassPlayers.contains(playerId)) {
            bypassPlayers.remove(playerId);
            return false;
        }

        bypassPlayers.add(playerId);
        return true;
    }

    public boolean canBypass(Player player) {
        return player != null && bypassPlayers.contains(player.getUniqueId());
    }

    public void clearPlayer(Player player) {
        if (player == null) {
            return;
        }
        bypassPlayers.remove(player.getUniqueId());
    }

    public void clear() {
        bypassPlayers.clear();
    }
}
