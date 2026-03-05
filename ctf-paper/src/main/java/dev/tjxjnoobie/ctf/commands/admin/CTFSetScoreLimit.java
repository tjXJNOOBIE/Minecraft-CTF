package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.ScoreboardDependencyAccess;
/**
 * Handles /ctf setscorelimit <limit> [confirm].
 */
public final class CTFSetScoreLimit implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, TaskDependencyAccess, ScoreboardDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFSetScoreLimit] ";

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender); // Require a player sender.
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        boolean hasPermission = player.hasPermission(CTFKeys.permissionAdmin()); // Enforce admin permission.
        if (!hasPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        // Validate usage.
        if (args.length < 1 || args.length > 2) {
            Component message = getMessage("error.usage.ctf_setscorelimit");
            sender.sendMessage(message);
            return true;
        }

        // Parse requested limit.
        int requestedLimit;
        try {
            String requestedLimitText = args[0].trim();
            requestedLimit = Integer.parseInt(requestedLimitText);
        } catch (NumberFormatException ex) {
            Component message = getMessage("error.usage.ctf_setscorelimit");
            sender.sendMessage(message);
            return true;
        }

        int clampedLimit = Math.max(1, requestedLimit); // Clamp score limit to minimum of 1.
        boolean confirmed = args.length == 2 && "confirm".equalsIgnoreCase(args[1]);

        int redScore = getScoreBoardManager().getScore(TeamManager.RED); // Check for a potential immediate win.
        int blueScore = getScoreBoardManager().getScore(TeamManager.BLUE);
        String winningTeam = null;
        boolean conditionResult1 = isRunning() && (redScore >= clampedLimit || blueScore >= clampedLimit);
        if (conditionResult1) {
            if (redScore != blueScore) {
                winningTeam = redScore > blueScore ? TeamManager.RED : TeamManager.BLUE;
            }
        }

        if (winningTeam != null && !confirmed) {
            String winningTeamLabel = TeamManager.RED.equals(winningTeam) ? "Red" : "Blue";
            String clampedLimitText = Integer.toString(clampedLimit);
            Component confirmMessage = getMessageFormatted("admin.setscorelimit.confirm", winningTeamLabel, clampedLimitText);
            sender.sendMessage(confirmMessage);
            return true;
        }

        // Apply new score limit and update scoreboards.
        getScoreBoardManager().setScoreLimit(clampedLimit);
        int applied = getScoreBoardManager().getScoreLimit();
        Duration remainingTime = getRemainingTime();
        GameState gameState = getGameStateManager().getGameState();
        getScoreBoardManager().updateScoreboards(remainingTime, gameState);
        sendDebugMessage("admin setscorelimit limit=" + applied);
        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "set score limit - player=" + playerName + " limit=" + applied);
        String appliedText = Integer.toString(applied);
        Component message = getMessageFormatted("admin.setscorelimit.success", appliedText);
        sender.sendMessage(message);

        if (winningTeam != null && confirmed) {
            getMatchCleanupHandler().requestMatchStop(MatchStopReason.WIN, winningTeam);
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
        // Guard: short-circuit when args.length == 2.
        if (args.length == 2) {
            return List.of("confirm");
        }
        return List.of();
    }

    // == Predicates ==
    private boolean isRunning() {
        return !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
    }
}
