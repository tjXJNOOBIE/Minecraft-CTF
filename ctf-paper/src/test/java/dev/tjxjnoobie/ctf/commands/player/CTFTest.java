package dev.tjxjnoobie.ctf.commands.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.handlers.BaseMarkerHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.Optional;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFTest extends TestLogSupport {
    // Dependencies
    private TeamManager teamManager;
    private MatchPlayerSessionHandler sessionHandler;
    private GameStateManager gameStateManager;
    private BuildToggleUtil buildToggleUtil;
    private DebugFeed debugFeed;
    private FlagBaseSetupHandler flagBaseSetupHandler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();

        teamManager = Mockito.mock(TeamManager.class);
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        buildToggleUtil = Mockito.mock(BuildToggleUtil.class);
        debugFeed = Mockito.mock(DebugFeed.class);
        flagBaseSetupHandler = Mockito.mock(FlagBaseSetupHandler.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(MatchPlayerSessionHandler.class, sessionHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(GameStateManager.class, gameStateManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagBaseSetupHandler.class, flagBaseSetupHandler);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(BaseMarkerHandler.class, Mockito.mock(BaseMarkerHandler.class));
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(MatchFlowHandler.class, Mockito.mock(MatchFlowHandler.class));
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(MatchCleanupHandler.class, Mockito.mock(MatchCleanupHandler.class));
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(GameLoopTimer.class, Mockito.mock(GameLoopTimer.class));
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(ScoreBoardManager.class, Mockito.mock(ScoreBoardManager.class));
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(BuildToggleUtil.class, buildToggleUtil);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(DebugFeed.class, debugFeed);
    }

    @Test
    void routesJoinSubcommand() {
        CTF ctf = buildCtf();
        Player sender = Mockito.mock(Player.class);
        Command command = Mockito.mock(Command.class);

        when(sender.hasPermission("ctf.admin")).thenReturn(true);
        when(sender.getName()).thenReturn("Tester");
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(teamManager.getLobbySpawn()).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getSpawn(TeamId.RED)).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getSpawn(TeamId.BLUE)).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getReturnPoints(TeamId.RED)).thenReturn(List.of(Mockito.mock(Location.class)));
        when(teamManager.getReturnPoints(TeamId.BLUE)).thenReturn(List.of(Mockito.mock(Location.class)));
        when(flagBaseSetupHandler.getBaseLocation(TeamId.RED)).thenReturn(Mockito.mock(Location.class));
        when(flagBaseSetupHandler.getBaseLocation(TeamId.BLUE)).thenReturn(Mockito.mock(Location.class));
        when(sessionHandler.resolveJoinTeamKey("red")).thenReturn("red");
        when(teamManager.getDisplayName("red")).thenReturn("Red");

        boolean result = ctf.onCommand(sender, command, "ctf", new String[] {"join", "red"});

        assertTrue(result);
        verify(sessionHandler).addPlayerToArena(sender, "red");
    }

    @Test
    void listsTopLevelCompletions() {
        CTF ctf = buildCtf();
        CommandSender sender = Mockito.mock(CommandSender.class);
        Command command = Mockito.mock(Command.class);

        when(sender.hasPermission("ctf.admin")).thenReturn(true);
        when(sender.hasPermission("ctf.debug")).thenReturn(false);

        List<String> completions = ctf.onTabComplete(sender, command, "ctf", new String[] {"s"});

        assertEquals(
            List.of("score", "setflag", "setgametime", "setlobby", "setreturn",
                "setscore", "setscorelimit", "setspawn", "start", "stop"),
            completions
        );
    }

    @Test
    void usesAdminDebugHelpKey() {
        CTF ctf = buildCtf();
        Player sender = Mockito.mock(Player.class);
        Command command = Mockito.mock(Command.class);

        when(sender.getName()).thenReturn("Tester");
        when(sender.hasPermission("ctf.admin")).thenReturn(false);
        when(sender.hasPermission("ctf.debug")).thenReturn(true);
        when(sender.hasPermission("ctf.simulate")).thenReturn(false);

        boolean result = ctf.onCommand(sender, command, "ctf", new String[0]);

        assertTrue(result);
        verify(sender).sendMessage(net.kyori.adventure.text.Component.text("help.admin.debug"));
    }

    private CTF buildCtf() {
        return new CTF();
    }
}
