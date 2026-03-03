package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /ctf debug toggle for users with ctf.debug.
 */
public final class CTFDebug implements CommandExecutor, MessageAccess {
    private final DebugFeed debugFeedManager;

    public CTFDebug(DebugFeed debugFeedManager) {
        this.debugFeedManager = debugFeedManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!player.hasPermission("ctf.debug")) {
            sender.sendMessage(msg("error.no_permission"));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_debug"));
            return true;
        }

        boolean enabled = debugFeedManager.toggle(player);
        if (enabled) {
            debugFeedManager.sendEnabledMessage(player);
        } else {
            sender.sendMessage(msg("player.debug_disabled"));
        }
        return true;
    }
}


