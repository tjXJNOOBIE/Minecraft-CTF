package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.player.managers.BuildBypassManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Toggles admin bypass for lobby build/inventory locks.
 */
public final class CTFCanBuild implements CommandExecutor, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFCanBuild] ";

    private final BuildBypassManager buildBypassManager;

    public CTFCanBuild(BuildBypassManager buildBypassManager) {
        this.buildBypassManager = buildBypassManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!player.hasPermission("ctf.admin")) {
            sender.sendMessage(msg("error.no_permission"));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_canbuild"));
            return true;
        }

        boolean enabled = buildBypassManager != null && buildBypassManager.toggle(player);
        sender.sendMessage(enabled ? msg("admin.canbuild.enabled") : msg("admin.canbuild.disabled"));
        Bukkit.getLogger().info(LOG_PREFIX + "canbuild=" + enabled + " player=" + player.getName());
        return true;
    }
}
