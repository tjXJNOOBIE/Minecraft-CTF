package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles `/ctf leave` and removes player state from active CTF tracking.
 */
public final class CTFLeave implements CommandExecutor, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, PlayerDependencyAccess {

    // == State ==

    private static final String LOG_PREFIX = "[CTFLeave] ";
    // == Lifecycle ==

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "/ctf leave - player=" + playerName);

        if (args.length != 0) {
            Component message = getMessage("error.usage.ctf_leave");
            sender.sendMessage(message);
            return true;
        }

        String previousTeam = getTeamManager().getTeamKey(player);
        getMatchPlayerSessionHandler().removePlayerFromArena(player, true);
        Component leftMessage = getMessage("player.left");
        sendMessage(player, leftMessage);

        if (previousTeam == null) {
            Component broadcast = getMessageFormatted("broadcast.team_leave_unknown", playerName);
            broadcastToArena(broadcast);
        } else {
            String previousTeamDisplayName = getTeamManager().getDisplayName(previousTeam);
            Component broadcast = getMessageFormatted("broadcast.team_leave", playerName, previousTeamDisplayName);
            broadcastToArena(broadcast);
        }
        return true;
    }
}

