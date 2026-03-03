package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf stop` for forcing a match stop.
 */
public final class CTFStop implements CommandExecutor, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFStop] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;

    public CTFStop(CtfMatchOrchestrator gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    /**
     * Stops the match and clears state when running.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf stop - player=" + player.getName());

        if (!player.hasPermission("ctf.admin")) {
            sender.sendMessage(msg("error.no_permission"));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_stop"));
            return true;
        }

        gameManager.stop(MatchStopReason.ADMIN);
        sender.sendMessage(msg("admin.stop.success"));
        return true;
    }
}


