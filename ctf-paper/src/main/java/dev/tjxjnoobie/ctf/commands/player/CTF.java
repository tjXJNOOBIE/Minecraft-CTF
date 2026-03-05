package dev.tjxjnoobie.ctf.commands.player;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.commands.admin.CTFCanBuild;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetFlag;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetGameTime;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetLobby;
import dev.tjxjnoobie.ctf.commands.admin.CTFRemoveReturn;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetReturn;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetScore;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetScoreLimit;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetSpawn;
import dev.tjxjnoobie.ctf.commands.admin.CTFStart;
import dev.tjxjnoobie.ctf.commands.admin.CTFStop;
import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Canonical `/ctf` router over the active command set.
 */
public final class CTF implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender {

    // == Permissions ==
    private static final String ADMIN_PERMISSION = CTFKeys.permissionAdmin();
    private static final String DEBUG_PERMISSION = CTFKeys.permissionDebug();
    private static final String SIMULATE_PERMISSION = CTFKeys.permissionSimulate();

    // == Debug logging ==
    private static final String LOG_PREFIX = "[CTF] ";

    // == Command groups ==
    private static final Set<String> ADMIN_SUBCOMMANDS = Set.of(
        "setflag",
        "setlobby",
        "setspawn",
        "setreturn",
        "removereturn",
        "setgametime",
        "setscore",
        "setscorelimit",
        "canbuild",
        "start",
        "stop"
    );

    // == Subcommand executors ==
    private final CTFJoin joinCommand = new CTFJoin();
    private final CTFLeave leaveCommand = new CTFLeave();
    private final CTFSetFlag setFlagCommand = new CTFSetFlag();
    private final CTFSetLobby setLobbyCommand = new CTFSetLobby();
    private final CTFSetSpawn setSpawnCommand = new CTFSetSpawn();
    private final CTFSetReturn setReturnCommand = new CTFSetReturn();
    private final CTFRemoveReturn removeReturnCommand = new CTFRemoveReturn();
    private final CTFSetGameTime setGameTimeCommand = new CTFSetGameTime();
    private final CTFSetScore setScoreCommand = new CTFSetScore();
    private final CTFSetScoreLimit setScoreLimitCommand = new CTFSetScoreLimit();
    private final CTFCanBuild canBuildCommand = new CTFCanBuild();
    private final CTFStart startCommand = new CTFStart();
    private final CTFStop stopCommand = new CTFStop();
    private final CTFScore scoreCommand = new CTFScore();
    private final CTFDebug debugCommand = new CTFDebug();
    private final CTFSimulate simulateCommand = new CTFSimulate();

    // == Tab completion keys ==
    private final List<String> subcommandKeys = List.of(
        "join", "leave", "setflag", "setlobby", "setspawn", "setreturn", "removereturn", "setgametime",
        "setscore", "setscorelimit", "canbuild", "start", "stop", "score", "debug", "simulate"
    );

    // == Lifecycle ==

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean conditionResult1 = CommandSenderUtil.requirePlayer(sender) == null; // Require a player sender.
        // Guard: stop command handling for non-player senders.
        if (conditionResult1) {
            return true;
        }

        String senderName = sender.getName(); // Debug telemetry for command entry.
        sendDebugMessage(LOG_PREFIX + "/ctf - sender=" + senderName);

        // Default to help when no subcommand provided.
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommandKey = args[0].toLowerCase(Locale.ROOT); // Dispatch subcommand routing.
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return switch (subcommandKey) {
            case "join" -> joinCommand.onCommand(sender, command, label, subArgs);
            case "leave" -> leaveCommand.onCommand(sender, command, label, subArgs);
            case "setflag" -> setFlagCommand.onCommand(sender, command, label, subArgs);
            case "setlobby" -> setLobbyCommand.onCommand(sender, command, label, subArgs);
            case "setspawn" -> setSpawnCommand.onCommand(sender, command, label, subArgs);
            case "setreturn" -> setReturnCommand.onCommand(sender, command, label, subArgs);
            case "removereturn", "remvoereturn" -> removeReturnCommand.onCommand(sender, command, label, subArgs);
            case "setgametime" -> setGameTimeCommand.onCommand(sender, command, label, subArgs);
            case "setscore" -> setScoreCommand.onCommand(sender, command, label, subArgs);
            case "setscorelimit" -> setScoreLimitCommand.onCommand(sender, command, label, subArgs);
            case "canbuild" -> canBuildCommand.onCommand(sender, command, label, subArgs);
            case "start" -> startCommand.onCommand(sender, command, label, subArgs);
            case "stop" -> stopCommand.onCommand(sender, command, label, subArgs);
            case "score" -> scoreCommand.onCommand(sender, command, label, subArgs);
            case "debug" -> debugCommand.onCommand(sender, command, label, subArgs);
            case "simulate" -> simulateCommand.onCommand(sender, command, label, subArgs);
            default -> {
                Component message = getMessage("error.subcommand_unknown");
                sender.sendMessage(message);
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return subcommandKeys.stream()
                .filter(name -> hasPermissionFor(name, sender))
                .filter(name -> name.startsWith(input))
                .sorted()
                .collect(Collectors.toList());
        }

        String subcommandKey = args[0].toLowerCase(Locale.ROOT);
        boolean hasPermissionFor = hasPermissionFor(subcommandKey, sender);
        // Guard: short-circuit when !hasPermissionFor.
        if (!hasPermissionFor) {
            return List.of();
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return switch (subcommandKey) {
            case "join" -> joinCommand.onTabComplete(sender, command, alias, subArgs);
            case "setflag" -> setFlagCommand.onTabComplete(sender, command, alias, subArgs);
            case "setspawn" -> setSpawnCommand.onTabComplete(sender, command, alias, subArgs);
            case "setreturn" -> setReturnCommand.onTabComplete(sender, command, alias, subArgs);
            case "removereturn", "remvoereturn" -> removeReturnCommand.onTabComplete(sender, command, alias, subArgs);
            case "setgametime" -> setGameTimeCommand.onTabComplete(sender, command, alias, subArgs);
            case "setscore" -> setScoreCommand.onTabComplete(sender, command, alias, subArgs);
            case "setscorelimit" -> setScoreLimitCommand.onTabComplete(sender, command, alias, subArgs);
            case "simulate" -> simulateCommand.onTabComplete(sender, command, alias, subArgs);
            default -> List.of();
        };
    }

    // == Utilities ==
    private void sendHelp(CommandSender sender) {
        // Base help for all players.
        sendHelpLine(sender, "help.welcome");
        sendHelpLine(sender, "help.player.join");
        sendHelpLine(sender, "help.player.leave");
        sendHelpLine(sender, "help.player.score");

        boolean hasPermission = sender.hasPermission(ADMIN_PERMISSION); // Admin-specific help.
        if (hasPermission) {
            sendHelpLine(sender, "help.admin.setflag");
            sendHelpLine(sender, "help.admin.setlobby");
            sendHelpLine(sender, "help.admin.setspawn");
            sendHelpLine(sender, "help.admin.setreturn");
            sendHelpLine(sender, "help.admin.removereturn");
            sendHelpLine(sender, "help.admin.setgametime");
            sendHelpLine(sender, "help.admin.setscore");
            sendHelpLine(sender, "help.admin.setscorelimit");
            sendHelpLine(sender, "help.admin.canbuild");
            sendHelpLine(sender, "help.admin.start");
            sendHelpLine(sender, "help.admin.stop");
        }

        boolean hasPermission2 = sender.hasPermission(DEBUG_PERMISSION); // Debug help.
        if (hasPermission2) {
            sendHelpLine(sender, "help.player.debug");
        }

        boolean hasPermission3 = sender.hasPermission(SIMULATE_PERMISSION); // Simulation help.
        if (hasPermission3) {
            sendHelpLine(sender, "help.admin.simulate");
        }
    }

    private void sendHelpLine(CommandSender sender, String key) {
        Component message = getMessage(key); // Render a message key as a help line.
        sender.sendMessage(message);
    }

    // == Predicates ==
    private boolean hasPermissionFor(String subcommand, CommandSender sender) {
        // Guard: short-circuit when subcommand == null || sender == null.
        if (subcommand == null || sender == null) {
            return false;
        }
        boolean containsResult = ADMIN_SUBCOMMANDS.contains(subcommand); // Gate admin-only commands.
        // Guard: short-circuit when containsResult.
        if (containsResult) {
            return sender.hasPermission(ADMIN_PERMISSION);
        }
        boolean conditionResult2 = "debug".equals(subcommand); // Gate debug/simulate commands.
        // Guard: enforce debug permission for the debug subcommand.
        if (conditionResult2) {
            return sender.hasPermission(DEBUG_PERMISSION);
        }
        boolean conditionResult3 = "simulate".equals(subcommand);
        // Guard: short-circuit when "simulate".equals(subcommand).
        if (conditionResult3) {
            return sender.hasPermission(SIMULATE_PERMISSION);
        }
        return true;
    }
}
