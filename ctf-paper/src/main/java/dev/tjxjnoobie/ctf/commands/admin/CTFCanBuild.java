package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Toggles admin bypass for lobby build/inventory locks.
 */
public final class CTFCanBuild implements CommandExecutor, MessageAccess, PlayerDependencyAccess {

    // == State ==

    private static final String LOG_PREFIX = "[CTF] [CTFCanBuild] ";

    // == Lifecycle ==

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        boolean hasAdminPermission = player.hasPermission(CTFKeys.permissionAdmin());
        // Guard: Player lacks the required admin permission for build bypass controls.
        if (!hasAdminPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        // Guard: Invalid argument count for /ctf canbuild.
        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_canbuild");
            sender.sendMessage(message);
            return true;
        }

        boolean enabled = getBuildToggleUtil().toggle(player); // Toggle admin build bypass so staff can edit arena state safely between matches.
        Component message = enabled ? getMessage("admin.canbuild.enabled") : getMessage("admin.canbuild.disabled");
        sender.sendMessage(message);
        Bukkit.getLogger().info(LOG_PREFIX + "canbuild=" + enabled + " player=" + player.getName());
        return true;
    }
}
