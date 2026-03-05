package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.commands.util.TeamTabCompleteUtil;
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
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.ScoreboardDependencyAccess;
/**
 * Handles /ctf setscore <red|blue> <score> [confirm].
 */
public final class CTFSetScore implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, PlayerDependencyAccess, TaskDependencyAccess, ScoreboardDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFSetScore] ";

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
        if (args.length < 2 || args.length > 3) {
            Component message = getMessage("error.usage.ctf_setscore");
            sender.sendMessage(message);
            return true;
        }

        String teamKey = getTeamManager().normalizeKey(args[0]); // Normalize team key.
        if (teamKey == null) {
            Component message = getMessage("error.usage.ctf_setscore");
            sender.sendMessage(message);
            return true;
        }

        // Parse requested score.
        int requestedScore;
        try {
            String requestedScoreText = args[1].trim();
            requestedScore = Integer.parseInt(requestedScoreText);
        } catch (NumberFormatException ex) {
            Component message = getMessage("error.usage.ctf_setscore");
            sender.sendMessage(message);
            return true;
        }

        int clampedScore = Math.max(0, requestedScore); // Clamp score and check win condition.
        int scoreLimit = getScoreBoardManager().getScoreLimit();
        String teamDisplayName = getTeamManager().getDisplayName(teamKey);
        boolean wouldWin = isRunning() && clampedScore >= scoreLimit;
        boolean confirmed = args.length == 3 && "confirm".equalsIgnoreCase(args[2]);

        if (wouldWin && !confirmed) {
            String clampedScoreText = Integer.toString(clampedScore);
            String scoreLimitText = Integer.toString(scoreLimit);
            Component confirmMessage = getMessageFormatted("admin.setscore.confirm", teamDisplayName, clampedScoreText, scoreLimitText);
            sender.sendMessage(confirmMessage);
            return true;
        }

        int applied = getScoreBoardManager().setScore(teamKey, clampedScore); // Apply score and refresh scoreboards.
        Duration remainingTime = getRemainingTime();
        GameState gameState = getGameStateManager().getGameState();
        getScoreBoardManager().updateScoreboards(remainingTime, gameState);
        sendDebugMessage("admin setscore team=" + teamKey + " score=" + applied);
        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "set score - player=" + playerName + " team=" + teamKey + " score=" + applied);
        String appliedScoreText = Integer.toString(applied);
        Component message = getMessageFormatted("admin.setscore.success", teamDisplayName, appliedScoreText);
        sender.sendMessage(message);

        if (wouldWin && confirmed) {
            getMatchCleanupHandler().requestMatchStop(MatchStopReason.WIN, teamKey);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> teamSuggestions = TeamTabCompleteUtil.suggestTeamArgument(args, 0);
        // Guard: No team suggestion context is available for this argument count.
        if (!teamSuggestions.isEmpty()) {
            return teamSuggestions;
        }
        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return List.of("0", "1", "2", "3", "5", "10").stream()
                .filter(value -> value.startsWith(input))
                .toList();
        }
        // Guard: short-circuit when args.length == 3.
        if (args.length == 3) {
            return List.of("confirm");
        }
        return List.of();
    }

    // == Predicates ==
    private boolean isRunning() {
        return !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
    }
}
