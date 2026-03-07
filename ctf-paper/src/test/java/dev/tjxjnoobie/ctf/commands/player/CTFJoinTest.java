package dev.tjxjnoobie.ctf.commands.player;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFJoinTest extends TestLogSupport {
    private TeamManager teamManager;
    private MatchPlayerSessionHandler sessionHandler;
    private GameStateManager gameStateManager;
    private FlagBaseSetupHandler flagBaseSetupHandler;
    private BukkitMessageUtil messageUtil;

    @BeforeEach
    void setUp() {
        registerMessageHandler();

        teamManager = Mockito.mock(TeamManager.class);
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        flagBaseSetupHandler = Mockito.mock(FlagBaseSetupHandler.class);
        messageUtil = Mockito.mock(BukkitMessageUtil.class);

        registerDependencies(
            TeamManager.class, teamManager,
            MatchPlayerSessionHandler.class, sessionHandler,
            GameStateManager.class, gameStateManager,
            FlagBaseSetupHandler.class, flagBaseSetupHandler,
            BukkitMessageUtil.class, messageUtil,
            DebugFeed.class, Mockito.mock(DebugFeed.class)
        );
    }

    @Test
    void blocksJoinWhenArenaSetupIsIncomplete() {
        CTFJoin joinCommand = new CTFJoin();
        Player player = Mockito.mock(Player.class);
        Command command = Mockito.mock(Command.class);

        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(teamManager.getLobbySpawn()).thenReturn(Optional.empty());
        when(teamManager.getSpawn(TeamId.RED)).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getSpawn(TeamId.BLUE)).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getReturnPoints(TeamId.RED)).thenReturn(List.of(Mockito.mock(Location.class)));
        when(teamManager.getReturnPoints(TeamId.BLUE)).thenReturn(List.of(Mockito.mock(Location.class)));
        when(flagBaseSetupHandler.getBaseLocation(TeamId.RED)).thenReturn(Mockito.mock(Location.class));
        when(flagBaseSetupHandler.getBaseLocation(TeamId.BLUE)).thenReturn(Mockito.mock(Location.class));

        boolean result = joinCommand.onCommand(player, command, "ctf", new String[] {"red"});

        assertTrue(result);
        verify(sessionHandler, never()).addPlayerToArena(Mockito.any(Player.class), Mockito.anyString());
        verify(messageUtil).sendMessage(player, Component.text("error.join_setup_incomplete"));
    }
}
