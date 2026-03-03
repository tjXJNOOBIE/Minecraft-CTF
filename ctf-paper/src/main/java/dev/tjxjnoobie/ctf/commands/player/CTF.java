package dev.tjxjnoobie.ctf.commands.player;

import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.commands.admin.CTFCanBuild;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetFlag;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetGameTime;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetLobby;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetReturn;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetScore;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetScoreLimit;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetSpawn;
import dev.tjxjnoobie.ctf.commands.admin.CTFStart;
import dev.tjxjnoobie.ctf.commands.admin.CTFStop;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Routes `/ctf` subcommands and delegates execution/tab completion to registered handlers.
 */
public final class CTF implements CommandExecutor, TabCompleter, MessageAccess {
    
    private static final String ADMIN_PERMISSION = "ctf.admin";
    private static final String DEBUG_PERMISSION = "ctf.debug";
    private static final String LOG_PREFIX = "[CTF] ";
    private static final Set<String> ADMIN_SUBCOMMANDS = Set.of(
        "setflag",
        "setlobby",
        "setspawn",
        "setreturn",
        "setgametime",
        "setscore",
        "setscorelimit",
        "canbuild",
        "start",
        "stop"
    );

    // Dependencies
    private final CTFJoin join;
    private final CTFLeave leave;
    private final CTFSetFlag setFlag;
    private final CTFSetLobby setLobby;
    private final CTFSetSpawn setSpawn;
    private final CTFSetReturn setReturn;
    private final CTFSetGameTime setGameTime;
    private final CTFSetScore setScore;
    private final CTFSetScoreLimit setScoreLimit;
    private final CTFCanBuild canBuild;
    private final CTFStart start;
    private final CTFStop stop;
    private final CTFScore score;
    private final CTFDebug debug;

    // Tab completion registry
    private final Map<String, TabCompleter> tabCompleters = new HashMap<>();
    private final List<String> subcommandNames;

    public CTF(CTFJoin join, CTFLeave leave, CTFSetFlag setFlag, CTFSetLobby setLobby,
               CTFSetSpawn setSpawn, CTFSetReturn setReturn, CTFSetGameTime setGameTime, CTFSetScore setScore,
               CTFSetScoreLimit setScoreLimit, CTFCanBuild canBuild, CTFStart start, CTFStop stop, CTFScore score,
               CTFDebug debug) {
        this.join = join;
        this.leave = leave;
        this.setFlag = setFlag;
        this.setLobby = setLobby;
        this.setSpawn = setSpawn;
        this.setReturn = setReturn;
        this.setGameTime = setGameTime;
        this.setScore = setScore;
        this.setScoreLimit = setScoreLimit;
        this.canBuild = canBuild;
        this.start = start;
        this.stop = stop;
        this.score = score;
        this.debug = debug;
        this.subcommandNames = List.of("join", "leave", "setflag", "setlobby", "setspawn", "setreturn", "setgametime",
            "setscore", "setscorelimit", "canbuild", "start", "stop", "score", "debug");
        registerTab("join", join);
        registerTab("setflag", setFlag);
        registerTab("setspawn", setSpawn);
        registerTab("setreturn", setReturn);
        registerTab("setgametime", setGameTime);
        registerTab("setscore", setScore);
        registerTab("setscorelimit", setScoreLimit);
    }

    // Command handling
    @Override
    /**
     * Dispatches a `/ctf` invocation to the matching subcommand handler.
     * Sender must be a player per project command policy.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Edge Case: command cannot be executed by console.
        if (CommandSenderUtil.requirePlayer(sender) == null) {
            return true;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "/ctf - sender=" + sender.getName());

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String key = args[0].toLowerCase(Locale.ROOT);
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return switch (key) {
            case "join" -> join.onCommand(sender, command, label, subArgs);
            case "leave" -> leave.onCommand(sender, command, label, subArgs);
            case "setflag" -> setFlag.onCommand(sender, command, label, subArgs);
            case "setlobby" -> setLobby.onCommand(sender, command, label, subArgs);
            case "setspawn" -> setSpawn.onCommand(sender, command, label, subArgs);
            case "setreturn" -> setReturn.onCommand(sender, command, label, subArgs);
            case "setgametime" -> setGameTime.onCommand(sender, command, label, subArgs);
            case "setscore" -> setScore.onCommand(sender, command, label, subArgs);
            case "setscorelimit" -> setScoreLimit.onCommand(sender, command, label, subArgs);
            case "canbuild" -> canBuild.onCommand(sender, command, label, subArgs);
            case "start" -> start.onCommand(sender, command, label, subArgs);
            case "stop" -> stop.onCommand(sender, command, label, subArgs);
            case "score" -> score.onCommand(sender, command, label, subArgs);
            case "debug" -> debug.onCommand(sender, command, label, subArgs);
            default -> {
                sender.sendMessage(msg("error.subcommand_unknown"));
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    /**
     * Provides first-token and delegated subcommand tab completion for `/ctf`.
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return subcommandNames.stream()
                .filter(name -> hasPermissionFor(name, sender))
                .filter(name -> name.startsWith(input))
                .sorted()
                .collect(Collectors.toList());
        }

        String key = args[0].toLowerCase(Locale.ROOT);
        if (!hasPermissionFor(key, sender)) {
            return List.of();
        }

        TabCompleter tabCompleter = tabCompleters.get(key);
        if (tabCompleter == null) {
            return List.of();
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return tabCompleter.onTabComplete(sender, command, alias, subArgs);
    }

    // Internal helpers
    private void registerTab(String name, TabCompleter tabCompleter) {
        tabCompleters.put(name, tabCompleter);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("help.welcome"));
        sender.sendMessage(msg("help.player.join"));
        sender.sendMessage(msg("help.player.leave"));
        sender.sendMessage(msg("help.player.score"));

        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(msg("help.admin.setflag"));
            sender.sendMessage(msg("help.admin.setlobby"));
            sender.sendMessage(msg("help.admin.setspawn"));
            sender.sendMessage(msg("help.admin.setreturn"));
            sender.sendMessage(msg("help.admin.setgametime"));
            sender.sendMessage(msg("help.admin.setscore"));
            sender.sendMessage(msg("help.admin.setscorelimit"));
            sender.sendMessage(msg("help.admin.canbuild"));
            sender.sendMessage(msg("help.admin.start"));
            sender.sendMessage(msg("help.admin.stop"));
        }

        if (sender.hasPermission(DEBUG_PERMISSION)) {
            sender.sendMessage(msg("help.player.debug"));
        }
    }

    private boolean hasPermissionFor(String subcommand, CommandSender sender) {
        if (subcommand == null || sender == null) {
            return false;
        }

        if (ADMIN_SUBCOMMANDS.contains(subcommand)) {
            return sender.hasPermission(ADMIN_PERMISSION);
        }

        if (subcommand.equals("debug")) {
            return sender.hasPermission(DEBUG_PERMISSION);
        }

        return true;
    }
}

