package dev.tjxjnoobie.ctf.commands.util;

import dev.tjxjnoobie.ctf.config.message.MessageConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Pure command sender guards shared by /ctf handlers.
 */
public final class CommandSenderUtil {
    private CommandSenderUtil() {
    }

    /**
     * Ensures sender is a player before continuing command execution.
     */
    public static Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        if (MessageConfig.get() != null) {
            sender.sendMessage(MessageConfig.get().getMessage("error.console_not_allowed"));
        } else {
            sender.sendMessage(Component.text("Console cannot perform this command."));
        }
        return null;
    }
}


