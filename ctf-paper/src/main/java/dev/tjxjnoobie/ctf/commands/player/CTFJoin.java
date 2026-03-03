package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
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
 * Handles `/ctf join` team assignment for players.
 */
public final class CTFJoin implements CommandExecutor, TabCompleter, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFJoin] ";

    // Dependencies
    private final TeamManager teamManager;
    private final CtfMatchOrchestrator gameManager;

    public CTFJoin(TeamManager teamManager, CtfMatchOrchestrator gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    @Override
    /**
     * Joins the sender to red/blue and updates team-owned scoreboard membership.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (gameManager.isJoinLocked()) {
            player.sendMessage(msg("error.join_locked"));
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf join - player=" + player.getName());

        String requestedTeam = null;
        if (args.length == 0) {
            requestedTeam = null;
        } else if (args.length == 1) {
            requestedTeam = args[0];
        } else {
            sender.sendMessage(msg("error.usage.ctf_join"));
            return true;
        }

        String teamKey = gameManager.resolveJoinTeamKey(requestedTeam);
        if (teamKey == null) {
            sender.sendMessage(msg("error.usage.ctf_join"));
            return true;
        }

        gameManager.joinPlayer(player, teamKey);
        player.sendMessage(msg("player.joined", java.util.Map.of(
            "team", teamManager.getDisplayName(teamKey)
        )));
        gameManager.broadcastToArena(msg("broadcast.team_join", java.util.Map.of(
            "player", player.getName(),
            "team", teamManager.getDisplayName(teamKey)
        )));
        return true;
    }

   
    /**
     * Suggests valid team names for `/ctf join`.
     */
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


