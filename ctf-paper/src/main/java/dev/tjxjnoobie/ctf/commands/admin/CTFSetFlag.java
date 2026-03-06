package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.commands.util.TeamTabCompleteUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.team.TeamId;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf setflag` base placement for red/blue flags.
 */
public final class CTFSetFlag implements CommandExecutor, TabCompleter, MessageAccess, PlayerDependencyAccess, FlagDependencyAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFSetFlag] ";

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf setflag - player=" + player.getName());

        boolean hasAdminPermission = player.hasPermission(CTFKeys.permissionAdmin());
        // Guard: Player lacks the required admin permission for flag-base configuration.
        if (!hasAdminPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        // Guard: Invalid argument count for /ctf setflag.
        if (args.length != 1) {
            Component message = getMessage("error.usage.ctf_setflag");
            sender.sendMessage(message);
            return true;
        }

        String teamKey = getTeamManager().normalizeKey(args[0]); // Normalize human-friendly aliases so command input maps to canonical team keys.
        // Guard: Team key is invalid or unsupported.
        if (teamKey == null) {
            Component message = getMessage("error.usage.ctf_setflag");
            sender.sendMessage(message);
            return true;
        }

        TeamId teamId = TeamId.fromKey(teamKey);
        // Guard: Team identifier cannot be parsed into the typed team model.
        if (teamId == null) {
            Component message = getMessage("error.usage.ctf_setflag");
            sender.sendMessage(message);
            return true;
        }

        // Persist and immediately render marker updates to keep setup feedback instant for admins.
        getFlagBaseSetupHandler().setFlagBase(player, teamId);
        getBaseMarkerHandler().spawnOrMoveBaseMarker(getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(teamId));

        String teamDisplayName = getTeamManager().getDisplayName(teamKey);
        Component message = getMessageFormatted("admin.setflag.success", teamDisplayName);
        sender.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> teamSuggestions = TeamTabCompleteUtil.suggestTeamArgument(args, 0);
        // Guard: The current argument index is not the team slot, so return default empty suggestions.
        if (!teamSuggestions.isEmpty()) {
            return teamSuggestions;
        }
        return List.of();
    }
}

