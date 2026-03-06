package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.MatchSessionDependencyAccess;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /ctf setlobby.
 */
public final class CTFSetLobby implements CommandExecutor, MessageAccess, LifecycleDependencyAccess, MatchSessionDependencyAccess {
    // == Lifecycle ==

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        boolean hasAdminPermission = player.hasPermission(CTFKeys.permissionAdmin());
        // Guard: Player lacks the required admin permission for lobby configuration.
        if (!hasAdminPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        // Guard: Invalid argument count for /ctf setlobby.
        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_setlobby");
            sender.sendMessage(message);
            return true;
        }

        // Persist lobby spawn so future joins and cleanup teleports return to this location.
        getMatchPlayerSessionHandler().setLobbySpawn(player);
        Component message = getMessage("admin.setlobby.success");
        sender.sendMessage(message);
        return true;
    }
}

