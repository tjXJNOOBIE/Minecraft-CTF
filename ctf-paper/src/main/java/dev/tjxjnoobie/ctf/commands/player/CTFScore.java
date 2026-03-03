package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf score` output for current red/blue team scores.
 */
public final class CTFScore implements CommandExecutor, MessageAccess {
    private static final String ADMIN_PERMISSION = "ctf.admin";
    private static final String LOG_PREFIX = "[CTF] [CTFScore] ";
    
    // Dependencies
    private final TeamManager teamManager;
    private final CtfMatchOrchestrator gameManager;
    private final ScoreBoardManager scoreBoardManager;

    public CTFScore(TeamManager teamManager, CtfMatchOrchestrator gameManager, ScoreBoardManager scoreBoardManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.scoreBoardManager = scoreBoardManager;
    }

    @Override
    /**
     * Sends the current scoreboard-backed team scores to the sender.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf score - player=" + player.getName());

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_score"));
            return true;
        }

        if (!gameManager.isRunning()) {
            if (player.hasPermission(ADMIN_PERMISSION)) {
                player.sendMessage(msg("error.game_not_running_admin"));
            } else {
                player.sendMessage(msg("error.game_not_running"));
            }
            return true;
        }

        int redScore = scoreBoardManager.getScore(TeamManager.RED);
        int blueScore = scoreBoardManager.getScore(TeamManager.BLUE);
        player.sendMessage(msg("player.score", java.util.Map.of(
            "red", Integer.toString(redScore),
            "blue", Integer.toString(blueScore)
        )));
        return true;
    }
}


