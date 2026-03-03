package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /ctf setlobby.
 */
public final class CTFSetLobby implements CommandExecutor, MessageAccess {
    private final CtfMatchOrchestrator gameManager;

    public CTFSetLobby(CtfMatchOrchestrator gameManager) {
        this.gameManager = gameManager;
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

        if (args.length != 0) {
            sender.sendMessage(msg("error.usage.ctf_setlobby"));
            return true;
        }

        gameManager.setLobbySpawn(player);
        sender.sendMessage(msg("admin.setlobby.success"));
        return true;
    }
}


