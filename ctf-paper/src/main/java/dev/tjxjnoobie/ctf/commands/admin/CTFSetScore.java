package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
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
 * Handles /ctf setscore <red|blue> <score> [confirm].
 */
public final class CTFSetScore implements CommandExecutor, TabCompleter, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFSetScore] ";

    private final CtfMatchOrchestrator gameManager;
    private final TeamManager teamManager;
    public CTFSetScore(CtfMatchOrchestrator gameManager, TeamManager teamManager) {
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

        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(msg("error.usage.ctf_setscore"));
            return true;
        }

        String teamKey = teamManager.normalizeKey(args[0]);
        if (teamKey == null) {
            sender.sendMessage(msg("error.usage.ctf_setscore"));
            return true;
        }

        int requestedScore;
        try {
            requestedScore = Integer.parseInt(args[1].trim());
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("error.usage.ctf_setscore"));
            return true;
        }

        int clampedScore = Math.max(0, requestedScore);
        int scoreLimit = gameManager.getScoreLimit();
        boolean wouldWin = gameManager.isRunning() && clampedScore >= scoreLimit;
        boolean confirmed = args.length == 3 && "confirm".equalsIgnoreCase(args[2]);

        if (wouldWin && !confirmed) {
            sender.sendMessage(msg("admin.setscore.confirm", Map.of(
                "team", teamManager.getDisplayName(teamKey),
                "score", Integer.toString(clampedScore),
                "limit", Integer.toString(scoreLimit)
            )));
            return true;
        }

        int applied = gameManager.setTeamScore(teamKey, clampedScore);
        gameManager.publishDebug("admin setscore team=" + teamKey + " score=" + applied);
        Bukkit.getLogger().info(LOG_PREFIX + "set score - player=" + player.getName()
            + " team=" + teamKey + " score=" + applied);
        sender.sendMessage(msg("admin.setscore.success", Map.of(
            "team", teamManager.getDisplayName(teamKey),
            "score", Integer.toString(applied)
        )));

        if (wouldWin && confirmed) {
            gameManager.stop(MatchStopReason.WIN, teamKey);
        }
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
        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return List.of("0", "1", "2", "3", "5", "10").stream()
                .filter(value -> value.startsWith(input))
                .toList();
        }
        if (args.length == 3) {
            return List.of("confirm");
        }
        return List.of();
    }
}

