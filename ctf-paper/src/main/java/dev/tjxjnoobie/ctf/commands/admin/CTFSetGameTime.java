package dev.tjxjnoobie.ctf.commands.admin;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.MatchSessionDependencyAccess;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles /ctf setgametime <seconds>.
 */
public final class CTFSetGameTime
        implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender, LifecycleDependencyAccess, MatchSessionDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFSetGameTime] ";

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = CommandSenderUtil.requirePlayer(sender);

        // Guard: The command sender is not a player identity or cannot be cast to
        // one, so we cannot restrict or apply admin permission logic properly.
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName();

        boolean hasPermission = player.hasPermission(CTFKeys.permissionAdmin());
        // Guard: The executing player lacks the required admin permission,
        // preventing unauthorized tampering of game time.
        if (!hasPermission) {
            Component permissionMessage = getMessage(CTFKeys.messageErrorNoPermissionKey());
            // Inform the user they lack the required security clearance to adjust game
            // states.
            sender.sendMessage(permissionMessage);
            return true;
        }

        boolean hasSingleArgument = args.length == 1;
        // Guard: The command was executed with missing or too many arguments,
        // failing the syntax requirements.
        if (!hasSingleArgument) {
            Component usageMessage = getMessage("error.usage.ctf_setgametime");
            // Provide the correct command syntax to guide the admin towards proper usage.
            sender.sendMessage(usageMessage);
            return true;
        }

        boolean running = isRunning();
        // Guard: Active match state is false; we cannot modify the time of a match
        // that is not currently playing or already in cleanup.
        if (!running) {
            Component notRunningMessage = getMessage("error.game_not_running_admin");
            // Inform the admin that time manipulation requires an active match progression.
            sender.sendMessage(notRunningMessage);
            return true;
        }

        MatchPlayerSessionHandler playerSessionHandler = getMatchPlayerSessionHandler();
        boolean isPlayerInArena = playerSessionHandler.isPlayerInArena(player);
        // Guard: Sender is spectating or still in the lobby. We want to ensure
        // admins are in the same contextual state as the game when managing it.
        if (!isPlayerInArena) {
            Component notInGameStateMessage = getMessage("error.not_in_game");
            // Alert the admin to fully join the active arena before manipulating temporal
            // properties.
            sender.sendMessage(notInGameStateMessage);
            return true;
        }

        long requestedSeconds;
        try {
            String requestedText = args[0].trim();
            requestedSeconds = Long.parseLong(requestedText); // Translate the provided text argument into a calculable temporal duration.
        } catch (NumberFormatException ex) {
            Component numberFormatMessage = getMessage("error.usage.ctf_setgametime");
            // The provided argument was fully non-numeric. We guide the admin back to the
            // integer-based syntax format.
            sender.sendMessage(numberFormatMessage);
            return true;
        }

        MatchFlowHandler flowHandler = getMatchFlowHandler();
        int maxSeconds = flowHandler.getAllowedMatchTimeSeconds();

        long zeroFloor = Math.max(0L, requestedSeconds);
        long clampedSeconds = Math.min(zeroFloor, maxSeconds);

        boolean timeUpdated = flowHandler.setMatchTimeSeconds(clampedSeconds);
        // Guard: MatchFlowHandler refused the time update, possibly because the
        // game ticked a natural end exactly as this command parsed.
        if (!timeUpdated) {
            Component updateFailedMessage = getMessage("error.game_not_running_admin");
            // Warn that the match just closed window for temporal extensions.
            sender.sendMessage(updateFailedMessage);
            return true;
        }

        // Output trace logging for internal server administration and diagnostics.
        String internalLog = LOG_PREFIX + "set game time - player=" + playerName
                + " requested=" + requestedSeconds + "s applied=" + clampedSeconds + "s";
        sendDebugMessage(internalLog);

        String clampedSecondsText = Long.toString(clampedSeconds);
        Component successMessage = getMessageFormatted("admin.setgametime.success", clampedSecondsText);

        // Finalize state changes by updating the admin interface with visual
        // confirmation of the new time.
        sender.sendMessage(successMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isFirstArgument = args.length == 1;

        // Guard: This command only accepts a single numeric argument, therefore we
        // have nothing to recommend beyond the first index.
        if (!isFirstArgument) {
            List<String> emptyListFallback = List.of();
            return emptyListFallback;
        }

        String rawInput = args[0];
        String inputLower = rawInput.toLowerCase(Locale.ROOT);

        List<String> defaultSuggestions = List.of("60", "120", "300", "600");

        // We filter out any fixed textual suggestions that do not textually adhere to
        // the characters already typed by the player.
        List<String> validMatches = defaultSuggestions.stream()
                .filter(value -> value.startsWith(inputLower))
                .toList();

        return validMatches;
    }

    // == Predicates ==
    private boolean isRunning() {
        GameStateManager stateManager = getGameStateManager();
        boolean isCleanup = stateManager.isCleanupInProgress();
        boolean isRunning = stateManager.isRunning();

        // We evaluate logic independently onto line variables per strict business
        boolean isTrulyRunning = !isCleanup && isRunning; // rules.
        return isTrulyRunning;
    }
}
