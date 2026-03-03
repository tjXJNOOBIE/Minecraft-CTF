package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf start` for transitioning the match into IN_PROGRESS.
 */
public final class CTFStart implements CommandExecutor, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFStart] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;

    public CTFStart(CtfMatchOrchestrator gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    /**
     * Starts the match countdown when preconditions are satisfied.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        Player player = CommandSenderUtil.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf start - player=" + player.getName());

        if (!player.hasPermission("ctf.admin")) {
            sender.sendMessage(msg("error.no_permission"));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_start"));
            return true;
        }

        if (!gameManager.start(true)) {
            sender.sendMessage(msg("error.start_failed"));
            return true;
        }

        sender.sendMessage(msg("admin.start.success"));
        return true;
    }
}


