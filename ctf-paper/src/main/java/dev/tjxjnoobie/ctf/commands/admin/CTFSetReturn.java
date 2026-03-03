package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles /ctf setreturn <red|blue>.
 */
public final class CTFSetReturn implements CommandExecutor, TabCompleter, MessageAccess {
    private final CtfMatchOrchestrator gameManager;
    private final TeamManager teamManager;

    public CTFSetReturn(CtfMatchOrchestrator gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
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

        if (args.length != 1) {
            sender.sendMessage(msg("error.usage.ctf_setreturn"));
            return true;
        }

        String teamKey = teamManager.normalizeKey(args[0]);
        if (teamKey == null) {
            sender.sendMessage(msg("error.usage.ctf_setreturn"));
            return true;
        }

        boolean added = gameManager.addTeamReturnPoint(player, teamKey);
        if (!added) {
            sender.sendMessage(msg("admin.setreturn.duplicate"));
            gameManager.publishDebug("setreturn duplicate team=" + teamKey + " player=" + player.getName());
            return true;
        }

        gameManager.publishDebug("setreturn team=" + teamKey + " player=" + player.getName());
        sender.sendMessage(msg("admin.setreturn.success", java.util.Map.of(
            "team", teamManager.getDisplayName(teamKey)
        )));
        return true;
    }

    @Override
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


