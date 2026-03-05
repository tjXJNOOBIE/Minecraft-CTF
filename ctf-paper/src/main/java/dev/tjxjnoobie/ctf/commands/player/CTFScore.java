package dev.tjxjnoobie.ctf.commands.player;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.ScoreboardDependencyAccess;
/**
 * Handles `/ctf score` output for current red/blue team scores.
 */
public final class CTFScore implements CommandExecutor, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, ScoreboardDependencyAccess {
    private static final String ADMIN_PERMISSION = CTFKeys.permissionAdmin();
    private static final String LOG_PREFIX = "[CTFScore] ";

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "/ctf score - player=" + playerName);

        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_score");
            sender.sendMessage(message);
            return true;
        }

        boolean running = isRunning();
        if (!running) {
            boolean hasPermission = player.hasPermission(ADMIN_PERMISSION);
            if (hasPermission) {
                Component message = getMessage("error.game_not_running_admin");
                sendMessage(player, message);
            } else {
                Component message = getMessage("error.game_not_running");
                sendMessage(player, message);
            }
            return true;
        }

        int redScore = getScoreBoardManager().getScore(TeamManager.RED);
        int blueScore = getScoreBoardManager().getScore(TeamManager.BLUE);
        String redScoreText = Integer.toString(redScore);
        String blueScoreText = Integer.toString(blueScore);
        Component scoreMessage = getMessageFormatted("player.score", redScoreText, blueScoreText);
        sendMessage(player, scoreMessage);
        return true;
    }

    // == Predicates ==
    private boolean isRunning() {
        return !getGameStateManager().isCleanupInProgress() && getGameStateManager().isRunning();
    }
}

