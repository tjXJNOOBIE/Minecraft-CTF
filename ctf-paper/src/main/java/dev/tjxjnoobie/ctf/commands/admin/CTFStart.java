package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
import dev.tjxjnoobie.ctf.util.game.ArenaSetupGuardUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles `/ctf start` for transitioning the match into IN_PROGRESS.
 */
public final class CTFStart implements CommandExecutor, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess {

    // == State ==

    private static final String LOG_PREFIX = "[CTFStart] ";
    // == Lifecycle ==

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "/ctf start - player=" + playerName);

        boolean hasPermission = player.hasPermission(CTFKeys.permissionAdmin());
        if (!hasPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sender.sendMessage(message);
            return true;
        }

        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_start");
            sender.sendMessage(message);
            return true;
        }

        String missingSetup = ArenaSetupGuardUtil.describeMissingArenaSetup();
        if (!missingSetup.isBlank()) {
            Component message = getMessage("error.start_setup_incomplete", Map.of("missing", missingSetup));
            sender.sendMessage(message);
            return true;
        }

        boolean conditionResult1 = !getMatchFlowHandler().requestMatchStart(true);
        if (conditionResult1) {
            Component message = getMessage("error.start_failed");
            sender.sendMessage(message);
            return true;
        }

        Component message = getMessage("admin.start.success");
        sender.sendMessage(message);
        return true;
    }
}

