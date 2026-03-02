package dev.tjxjnoobie.ctf.game.debug.managers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Owns debug-feed subscriptions and message delivery.
 */
public final class DebugFeed implements MessageAccess {

    // == Constants ==
    private static final String DEBUG_PERMISSION = "ctf.debug";
    private static final TextColor DEBUG_MESSAGE_COLOR = TextColor.color(255, 85, 85);

    // == Runtime state ==
    private final Set<UUID> subscribers = new HashSet<>();

    // == Utilities ==
    /**
     * Toggles whether player receives debug feed messages.
     *
     * @return true when feed is now enabled for player.
     */
    public boolean toggle(Player player) {
        // Validation & early exits
        // Guard: short-circuit when player == null.
        if (player == null) {
            return false;
        }

        UUID playerId = player.getUniqueId(); // Domain lookup

        boolean containsResult = subscribers.contains(playerId); // State transition
        if (containsResult) {
            subscribers.remove(playerId);
            return false;
        }

        subscribers.add(playerId);
        return true;
    }

    /**
     * Sends one debug line to all subscribed players with permission.
     */
    public void send(String message) {
        // Validation & early exits
        // Guard: short-circuit when message == null || subscribers.isEmpty().
        if (message == null || subscribers.isEmpty()) {
            return;
        }

        Component line = buildDebugLine(message); // Domain lookup

        // State transition
        for (UUID playerId : new HashSet<>(subscribers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || !player.hasPermission(DEBUG_PERMISSION)) {
                subscribers.remove(playerId);
                continue;
            }

            // UX feedback
            player.sendMessage(line);
        }
    }

    /**
     * Sends styled enabled confirmation to one player when permitted.
     */
    public void sendEnabledMessage(Player player) {
        // Validation & early exits
        // Guard: short-circuit when player == null || !player.hasPermission(DEBUG_PERMISSION).
        if (player == null || !player.hasPermission(DEBUG_PERMISSION)) {
            return;
        }

        // UX feedback
        player.sendMessage(getMessage("player.debug_enabled"));
    }

    /**
     * Clears all debug subscribers.
     */
    public void clear() {
        subscribers.clear();
    }

    private Component buildDebugLine(String message) {
        return getMessage("debug.prefix")
            .append(Component.text(message, DEBUG_MESSAGE_COLOR));
    }
}

