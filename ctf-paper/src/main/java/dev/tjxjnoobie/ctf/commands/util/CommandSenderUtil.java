package dev.tjxjnoobie.ctf.commands.util;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Pure command sender guards shared by /ctf handlers.
 */
public final class CommandSenderUtil {

    // == Lifecycle ==
    private CommandSenderUtil() {
    }

    // == Getters ==
    public static Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            // Allow command flow to continue with a player sender.
            return player;
        }

        Component message = MESSAGE_ACCESS.getMessage("error.console_not_allowed"); // Use configured message through message access helper.
        if (message != null) {
            sender.sendMessage(message);
        } else {
            // Fall back to a hard-coded message.
            sender.sendMessage(Component.text("Console cannot perform this command."));
        }
        return null;
    }

    // == Utilities ==
    private static final MessageAccess MESSAGE_ACCESS = new MessageAccess() {};
}

