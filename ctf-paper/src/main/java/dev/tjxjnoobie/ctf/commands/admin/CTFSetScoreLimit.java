package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles /ctf setscorelimit <limit> [confirm].
 */
public final class CTFSetScoreLimit implements CommandExecutor, TabCompleter, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFSetScoreLimit] ";

    private final CtfMatchOrchestrator gameManager;
    private final ScoreBoardManager scoreBoardManager;

    public CTFSetScoreLimit(CtfMatchOrchestrator gameManager, ScoreBoardManager scoreBoardManager) {
        this.gameManager = gameManager;
        this.scoreBoardManager = scoreBoardManager;
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

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(msg("error.usage.ctf_setscorelimit"));
            return true;
        }

        int requestedLimit;
        try {
            requestedLimit = Integer.parseInt(args[0].trim());
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("error.usage.ctf_setscorelimit"));
            return true;
        }

        int clampedLimit = Math.max(1, requestedLimit);
        boolean confirmed = args.length == 2 && "confirm".equalsIgnoreCase(args[1]);

        int redScore = scoreBoardManager.getScore(TeamManager.RED);
        int blueScore = scoreBoardManager.getScore(TeamManager.BLUE);
        String winningTeam = null;
        if (gameManager.isRunning() && (redScore >= clampedLimit || blueScore >= clampedLimit)) {
            if (redScore != blueScore) {
                winningTeam = redScore > blueScore ? TeamManager.RED : TeamManager.BLUE;
            }
        }

        if (winningTeam != null && !confirmed) {
            sender.sendMessage(msg("admin.setscorelimit.confirm", Map.of(
                "team", TeamManager.RED.equals(winningTeam) ? "Red" : "Blue",
                "limit", Integer.toString(clampedLimit)
            )));
            return true;
        }

        int applied = gameManager.setScoreLimit(clampedLimit);
        gameManager.publishDebug("admin setscorelimit limit=" + applied);
        Bukkit.getLogger().info(LOG_PREFIX + "set score limit - player=" + player.getName() + " limit=" + applied);
        sender.sendMessage(msg("admin.setscorelimit.success", Map.of(
            "limit", Integer.toString(applied)
        )));

        if (winningTeam != null && confirmed) {
            gameManager.stop(MatchStopReason.WIN, winningTeam);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return List.of("1", "3", "5", "10").stream()
                .filter(value -> value.startsWith(input))
                .toList();
        }
        if (args.length == 2) {
            return List.of("confirm");
        }
        return List.of();
    }
}

