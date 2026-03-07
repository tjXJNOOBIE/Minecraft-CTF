package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;

/**
 * Handles /ctf removereturn by removing the nearest configured return point.
 */
public final class CTFRemoveReturn implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender,
        PlayerDependencyAccess {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!player.hasPermission(CTFKeys.permissionAdmin())) {
            sender.sendMessage(getMessage(CTFKeys.messageErrorNoPermissionKey()));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(getMessage("error.usage.ctf_removereturn"));
            return true;
        }

        String removedTeamKey = getMatchPlayerSessionHandler().removeNearestReturnPoint(player);
        if (removedTeamKey == null) {
            sender.sendMessage(getMessage("admin.removereturn.none"));
            return true;
        }

        String teamDisplayName = getTeamManager().getDisplayName(removedTeamKey);
        Component message = getMessageFormatted("admin.removereturn.success", teamDisplayName);
        sender.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
