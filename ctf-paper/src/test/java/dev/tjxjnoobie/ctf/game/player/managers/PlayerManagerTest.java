package dev.tjxjnoobie.ctf.game.player.managers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlayerManagerTest extends TestLogSupport {

    private TeamManager teamManager;
    private KitSelectionHandler kitSelectionHandler;
    private KitSelectorGUI kitSelectorGui;
    private ScoreBoardManager scoreBoardManager;
    private PlayerManager playerManager;

    @BeforeEach
    void setUp() {
        teamManager = Mockito.mock(TeamManager.class);
        kitSelectionHandler = Mockito.mock(KitSelectionHandler.class);
        kitSelectorGui = Mockito.mock(KitSelectorGUI.class);
        scoreBoardManager = Mockito.mock(ScoreBoardManager.class);
        playerManager = new PlayerManager(teamManager, kitSelectionHandler, kitSelectorGui, scoreBoardManager);
    }

    @Test
    void joinCTFArenaLobbyStagesPlayerWithoutOpeningKitSelector() {
        Player player = mockArenaPlayer("LobbyScout");
        Location lobby = new Location(null, 0, 80, 0);

        when(teamManager.getLobbySpawn()).thenReturn(Optional.of(lobby));

        playerManager.joinCTFArena(player, TeamManager.RED, GameState.LOBBY, Duration.ZERO);

        verify(player).teleport(lobby);
        verify(player).setHealth(20.0);
        verify(player).setFoodLevel(20);
        verify(player).setSaturation(6.0f);
        verify(kitSelectorGui, never()).openKitSelector(Mockito.any(), Mockito.anyBoolean());
        verify(scoreBoardManager).showScoreboard(player);
        logStep("validated lobby join does not force kit gui open");
    }

    @Test
    void joinCTFArenaInProgressOpensKitSelectorWhenPlayerHasNoSelection() {
        Player player = mockArenaPlayer("LiveScout");
        Location spawn = new Location(null, 5, 90, 5);

        when(teamManager.getSpawn(TeamManager.BLUE)).thenReturn(Optional.of(spawn));
        when(kitSelectionHandler.hasSelection(player)).thenReturn(false);

        playerManager.joinCTFArena(player, TeamManager.BLUE, GameState.IN_PROGRESS, Duration.ofMinutes(4));

        verify(player).teleport(spawn);
        verify(kitSelectorGui).openKitSelector(player, true);
        verify(kitSelectionHandler, never()).applyKitLoadout(Mockito.any(Player.class), Mockito.any(KitType.class));
        verify(player).setFoodLevel(20);
        logStep("validated live join opens kit gui");
    }

    private Player mockArenaPlayer(String playerName) {
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);

        when(player.getName()).thenReturn(playerName);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        when(player.getLocation()).thenReturn(new Location(null, 10, 64, 10));
        when(player.getMaxHealth()).thenReturn(20.0);
        return player;
    }
}
