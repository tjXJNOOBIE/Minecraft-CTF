package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.commands.util.TeamTabCompleteUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.util.game.ArenaSetupGuardUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles `/ctf join` team assignment for players.
 */
public final class CTFJoin implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, PlayerDependencyAccess {
    private static final String LOG_PREFIX = "[CTFJoin] ";

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender);
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        boolean cleanupInProgress = getGameStateManager().isCleanupInProgress();
        // Guard: Match cleanup is in progress, so team joins are temporarily locked.
        if (cleanupInProgress) {
            Component message = getMessage("error.join_locked");
            sendMessage(player, message);
            return true;
        }

        String missingSetup = ArenaSetupGuardUtil.describeMissingArenaSetup();
        if (!missingSetup.isBlank()) {
            Component message = getMessage("error.join_setup_incomplete", Map.of("missing", missingSetup));
            sendMessage(player, message);
            return true;
        }

        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "/ctf join - player=" + playerName);

        String requestedTeam = null;
        if (args.length == 1) {
            requestedTeam = args[0];
        } else if (args.length > 1) {
            // Guard: Too many arguments were provided for /ctf join.
            Component message = getMessage("error.usage.ctf_join");
            sender.sendMessage(message);
            return true;
        }

        String teamKey = getMatchPlayerSessionHandler().resolveJoinTeamKey(requestedTeam); // Resolve explicit team choice or fall back to balancing rules from session handler.
        // Guard: Requested team key is invalid or unsupported.
        if (teamKey == null) {
            Component message = getMessage("error.usage.ctf_join");
            sender.sendMessage(message);
            return true;
        }

        // Apply team assignment and broadcast it to currently joined arena players.
        getMatchPlayerSessionHandler().addPlayerToArena(player, teamKey);
        String teamDisplayName = getTeamManager().getDisplayName(teamKey);
        Component joined = getMessageFormatted("player.joined", teamDisplayName);
        sendMessage(player, joined);
        Component broadcast = getMessageFormatted("broadcast.team_join", playerName, teamDisplayName);
        broadcastToArena(broadcast);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> teamSuggestions = TeamTabCompleteUtil.suggestTeamArgument(args, 0);
        // Guard: The current argument index is not the team slot, so return default empty suggestions.
        if (!teamSuggestions.isEmpty()) {
            return teamSuggestions;
        }
        return List.of();
    }
}

