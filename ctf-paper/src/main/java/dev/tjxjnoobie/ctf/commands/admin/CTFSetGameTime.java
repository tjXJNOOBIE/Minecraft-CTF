package dev.tjxjnoobie.ctf.commands.admin;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
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
 * Handles /ctf setgametime <seconds>.
 */
public final class CTFSetGameTime implements CommandExecutor, TabCompleter, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFSetGameTime] ";

    private final CtfMatchOrchestrator gameManager;

    public CTFSetGameTime(CtfMatchOrchestrator gameManager) {
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

        if (args.length != 1) {
            sender.sendMessage(msg("error.usage.ctf_setgametime"));
            return true;
        }

        if (gameManager == null || !gameManager.isRunning()) {
            sender.sendMessage(msg("error.game_not_running_admin"));
            return true;
        }

        if (!gameManager.isPlayerInGame(player)) {
            sender.sendMessage(msg("error.not_in_game"));
            return true;
        }

        long requestedSeconds;
        try {
            requestedSeconds = Long.parseLong(args[0].trim());
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("error.usage.ctf_setgametime"));
            return true;
        }

        int maxSeconds = gameManager.getAllowedMatchTimeSeconds();
        long clampedSeconds = Math.max(0L, Math.min(requestedSeconds, maxSeconds));
        if (!gameManager.setMatchTimeSeconds(clampedSeconds)) {
            sender.sendMessage(msg("error.game_not_running_admin"));
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "set game time - player=" + player.getName()
            + " requested=" + requestedSeconds + "s"
            + " applied=" + clampedSeconds + "s");
        sender.sendMessage(msg("admin.setgametime.success", Map.of(
            "seconds", Long.toString(clampedSeconds)
        )));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return List.of("60", "120", "300", "600").stream()
                .filter(value -> value.startsWith(input))
                .toList();
        }
        return List.of();
    }
}

