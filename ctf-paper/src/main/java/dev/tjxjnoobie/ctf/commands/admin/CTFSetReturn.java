package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.commands.util.TeamTabCompleteUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles /ctf setreturn <red|blue>.
 */
public final class CTFSetReturn implements CommandExecutor, TabCompleter, MessageAccess, LifecycleDependencyAccess, PlayerDependencyAccess {

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender); // Require a player sender.
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName();

        boolean hasPermission = player.hasPermission(CTFKeys.permissionAdmin()); // Enforce admin permission.
        if (!hasPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        // Validate usage.
        if (args.length != 1) {
            Component message = getMessage("error.usage.ctf_setreturn");
            sender.sendMessage(message);
            return true;
        }

        String teamKey = getTeamManager().normalizeKey(args[0]); // Normalize team key.
        if (teamKey == null) {
            Component message = getMessage("error.usage.ctf_setreturn");
            sender.sendMessage(message);
            return true;
        }

        boolean added = getMatchPlayerSessionHandler().addTeamReturnPoint(player, teamKey); // Persist the return point for the team.
        if (!added) {
            Component message = getMessage("admin.setreturn.duplicate");
            sender.sendMessage(message);
            getDebugFeed().send("setreturn duplicate team=" + teamKey + " player=" + playerName);
            return true;
        }

        // Report success and log debug.
        getDebugFeed().send("setreturn team=" + teamKey + " player=" + playerName);
        String teamDisplayName = getTeamManager().getDisplayName(teamKey);
        Component message = getMessageFormatted("admin.setreturn.success", teamDisplayName);
        sender.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> teamSuggestions = TeamTabCompleteUtil.suggestTeamArgument(args, 0);
        // Guard: No team suggestion context is available for this argument count.
        if (!teamSuggestions.isEmpty()) {
            return teamSuggestions;
        }
        return List.of();
    }
}

