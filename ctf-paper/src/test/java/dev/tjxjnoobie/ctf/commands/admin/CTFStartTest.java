package dev.tjxjnoobie.ctf.commands.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
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

class CTFStartTest extends TestLogSupport {
    private TeamManager teamManager;
    private FlagBaseSetupHandler flagBaseSetupHandler;
    private MatchFlowHandler matchFlowHandler;

    @BeforeEach
    void setUp() {
        registerMessageHandler();

        teamManager = Mockito.mock(TeamManager.class);
        flagBaseSetupHandler = Mockito.mock(FlagBaseSetupHandler.class);
        matchFlowHandler = Mockito.mock(MatchFlowHandler.class);

        registerDependencies(
            TeamManager.class, teamManager,
            FlagBaseSetupHandler.class, flagBaseSetupHandler,
            MatchFlowHandler.class, matchFlowHandler,
            BukkitMessageUtil.class, Mockito.mock(BukkitMessageUtil.class),
            DebugFeed.class, Mockito.mock(DebugFeed.class)
        );
    }

    @Test
    void blocksStartWhenArenaSetupIsIncomplete() {
        CTFStart startCommand = new CTFStart();
        Player player = Mockito.mock(Player.class);
        Command command = Mockito.mock(Command.class);

        when(player.hasPermission("ctf.admin")).thenReturn(true);
        when(player.getName()).thenReturn("Admin");
        when(teamManager.getLobbySpawn()).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getSpawn(TeamId.RED)).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getSpawn(TeamId.BLUE)).thenReturn(Optional.of(Mockito.mock(Location.class)));
        when(teamManager.getReturnPoints(TeamId.RED)).thenReturn(List.of());
        when(teamManager.getReturnPoints(TeamId.BLUE)).thenReturn(List.of(Mockito.mock(Location.class)));
        when(flagBaseSetupHandler.getBaseLocation(TeamId.RED)).thenReturn(Mockito.mock(Location.class));
        when(flagBaseSetupHandler.getBaseLocation(TeamId.BLUE)).thenReturn(Mockito.mock(Location.class));

        boolean result = startCommand.onCommand(player, command, "ctf", new String[0]);

        assertTrue(result);
        verify(matchFlowHandler, never()).requestMatchStart(true);
        verify(player).sendMessage(Component.text("error.start_setup_incomplete"));
    }
}
