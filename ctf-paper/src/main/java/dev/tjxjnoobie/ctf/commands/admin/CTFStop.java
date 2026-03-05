package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf stop` for forcing a match stop.
 */
public final class CTFStop implements CommandExecutor, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess {

    // == State ==

    private static final String LOG_PREFIX = "[CTFStop] ";
    // == Lifecycle ==

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "/ctf stop - player=" + playerName);

        boolean hasPermission = player.hasPermission(CTFKeys.permissionAdmin());
        if (!hasPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_stop");
            sender.sendMessage(message);
            return true;
        }

        getMatchCleanupHandler().requestMatchStop(MatchStopReason.ADMIN, null);
        Component message = getMessage("admin.stop.success");
        sender.sendMessage(message);
        return true;
    }
}

