package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import dev.tjxjnoobie.ctf.game.flag.BaseMarkerService;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf setflag` base placement for red/blue flags.
 */
public final class CTFSetFlag implements CommandExecutor, TabCompleter, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFSetFlag] ";

    // Dependencies
    private final FlagManager flagManager;
    private final TeamManager teamManager;
    private final BaseMarkerService markerManager;

    public CTFSetFlag(FlagManager flagManager, TeamManager teamManager, BaseMarkerService markerManager) {
        this.flagManager = flagManager;
        this.teamManager = teamManager;
        this.markerManager = markerManager;
    }

    @Override
    /**
     * Sets the selected team flag base to the sender's current block location.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf setflag - player=" + player.getName());

        if (!player.hasPermission("ctf.admin")) {
            sender.sendMessage(msg("error.no_permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(msg("error.usage.ctf_setflag"));
            return true;
        }

        String teamKey = teamManager.normalizeKey(args[0]);
        if (teamKey == null) {
            sender.sendMessage(msg("error.usage.ctf_setflag"));
            return true;
        }

        flagManager.setFlagBase(player, teamKey);
        if (markerManager != null) {
            markerManager.spawnOrMoveBaseMarker(teamKey, player.getLocation());
        }
        sender.sendMessage(msg("admin.setflag.success", java.util.Map.of(
            "team", teamManager.getDisplayName(teamKey)
        )));
        return true;
    }

    @Override
    /**
     * Suggests valid team names for `/ctf setflag`.
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return List.of(TeamManager.RED, TeamManager.BLUE).stream()
                .filter(value -> value.startsWith(input))
                .toList();
        }
        return List.of();
    }
}


