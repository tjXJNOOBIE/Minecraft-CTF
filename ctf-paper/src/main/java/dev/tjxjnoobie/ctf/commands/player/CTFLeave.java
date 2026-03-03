package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf leave` and removes player state from active CTF tracking.
 */
public final class CTFLeave implements CommandExecutor, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFLeave] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final TeamManager teamManager;

    public CTFLeave(CtfMatchOrchestrator gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }

    @Override
    /**
     * Removes the sender from team/game context and clears carrier side effects.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf leave - player=" + player.getName());

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_leave"));
            return true;
        }

        String previousTeam = teamManager.getTeamKey(player);
        gameManager.handlePlayerLeave(player, true);
        player.sendMessage(msg("player.left"));

        if (previousTeam == null) {
            gameManager.broadcastToArena(msg("broadcast.team_leave_unknown", java.util.Map.of(
                "player", player.getName()
            )));
        } else {
            gameManager.broadcastToArena(msg("broadcast.team_leave", java.util.Map.of(
                "player", player.getName(),
                "team", teamManager.getDisplayName(previousTeam)
            )));
        }
        return true;
    }
}


