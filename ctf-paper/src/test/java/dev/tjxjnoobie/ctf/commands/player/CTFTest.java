package dev.tjxjnoobie.ctf.commands.player;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
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
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFTest] ";

    @Test
    void routesJoinSubcommand() {
        Bukkit.getLogger().info(LOG_PREFIX + "/ctf join routes to join handler with team argument.");
        CTFJoin join = Mockito.mock(CTFJoin.class);
        CTFLeave leave = Mockito.mock(CTFLeave.class);
        CTFSetFlag setFlag = Mockito.mock(CTFSetFlag.class);
        CTFSetLobby setLobby = Mockito.mock(CTFSetLobby.class);
        CTFSetSpawn setSpawn = Mockito.mock(CTFSetSpawn.class);
        CTFSetReturn setReturn = Mockito.mock(CTFSetReturn.class);
        CTFSetGameTime setGameTime = Mockito.mock(CTFSetGameTime.class);
        CTFSetScore setScore = Mockito.mock(CTFSetScore.class);
        CTFSetScoreLimit setScoreLimit = Mockito.mock(CTFSetScoreLimit.class);
        CTFCanBuild canBuild = Mockito.mock(CTFCanBuild.class);
        CTFStart start = Mockito.mock(CTFStart.class);
        CTFStop stop = Mockito.mock(CTFStop.class);
        CTFScore score = Mockito.mock(CTFScore.class);
        CTFDebug debug = Mockito.mock(CTFDebug.class);

        CTF ctf = new CTF(join, leave, setFlag, setLobby, setSpawn, setReturn, setGameTime,
            setScore, setScoreLimit, canBuild, start, stop, score, debug);

        Player sender = Mockito.mock(Player.class);
        Command command = Mockito.mock(Command.class);
        when(join.onCommand(eq(sender), eq(command), eq("ctf"), any(String[].class))).thenReturn(true);

        // Executes /ctf join red and expects delegation to join handler.
        boolean result = ctf.onCommand(sender, command, "ctf", new String[] {"join", "red"});

        assertTrue(result);
        verify(join).onCommand(eq(sender), eq(command), eq("ctf"), argThat((String[] args) -> Arrays.equals(args, new String[] {"red"})));
        Bukkit.getLogger().info(LOG_PREFIX + "/ctf routes join subcommand");
    }

    @Test
    void listsTopLevelCompletions() {
        Bukkit.getLogger().info(LOG_PREFIX + "/ctf tab completion: admin sees top-level subcommands.");
        CTFJoin join = Mockito.mock(CTFJoin.class);
        CTFLeave leave = Mockito.mock(CTFLeave.class);
        CTFSetFlag setFlag = Mockito.mock(CTFSetFlag.class);
        CTFSetLobby setLobby = Mockito.mock(CTFSetLobby.class);
        CTFSetSpawn setSpawn = Mockito.mock(CTFSetSpawn.class);
        CTFSetReturn setReturn = Mockito.mock(CTFSetReturn.class);
        CTFSetGameTime setGameTime = Mockito.mock(CTFSetGameTime.class);
        CTFSetScore setScore = Mockito.mock(CTFSetScore.class);
        CTFSetScoreLimit setScoreLimit = Mockito.mock(CTFSetScoreLimit.class);
        CTFCanBuild canBuild = Mockito.mock(CTFCanBuild.class);
        CTFStart start = Mockito.mock(CTFStart.class);
        CTFStop stop = Mockito.mock(CTFStop.class);
        CTFScore score = Mockito.mock(CTFScore.class);
        CTFDebug debug = Mockito.mock(CTFDebug.class);

        CTF ctf = new CTF(join, leave, setFlag, setLobby, setSpawn, setReturn, setGameTime,
            setScore, setScoreLimit, canBuild, start, stop, score, debug);

        CommandSender sender = Mockito.mock(CommandSender.class);
        Command command = Mockito.mock(Command.class);
        when(sender.hasPermission("ctf.admin")).thenReturn(true);

        // Filters available commands by prefix.
        List<String> completions = ctf.onTabComplete(sender, command, "ctf", new String[] {"s"});

        assertEquals(List.of("score", "setflag", "setgametime", "setlobby", "setreturn",
            "setscore", "setscorelimit", "setspawn", "start", "stop"), completions);
        Bukkit.getLogger().info(LOG_PREFIX + "/ctf tab completion lists top-level commands");
    }
}

