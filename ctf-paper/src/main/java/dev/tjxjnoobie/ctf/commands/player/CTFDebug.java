package dev.tjxjnoobie.ctf.commands.player;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /ctf debug toggle for users with ctf.debug.
 */
public final class CTFDebug implements CommandExecutor, MessageAccess, BukkitMessageSender, PlayerDependencyAccess {

    // == Permission gate ==
    private static final String PERMISSION = CTFKeys.permissionDebug();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender); // Require a player sender.
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        boolean hasPermission = player.hasPermission(PERMISSION); // Enforce debug permission.
        if (!hasPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sendMessage(player, message);
            return true;
        }

        // Validate usage (no args).
        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_debug");
            sender.sendMessage(message);
            return true;
        }

        DebugFeed debugFeed = getDebugFeed(); // Toggle debug subscription.
        boolean enabled = debugFeed.toggle(player);
        if (enabled) {
            debugFeed.sendEnabledMessage(player);
        } else {
            Component disabled = getMessage("player.debug_disabled");
            sendMessage(player, disabled);
        }
        return true;
    }
}