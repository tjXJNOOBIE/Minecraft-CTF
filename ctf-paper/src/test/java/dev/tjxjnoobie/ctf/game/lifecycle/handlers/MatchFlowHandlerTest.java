package dev.tjxjnoobie.ctf.game.lifecycle.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class MatchFlowHandlerTest extends TestLogSupport {

    private GameStateManager gameStateManager;
    private ScoreBoardManager scoreBoardManager;
    private PlayerManager playerManager;
    private FlagBaseSetupHandler flagBaseSetupHandler;
    private FlagLifecycleHandler flagLifecycleHandler;
    private TeamManager teamManager;
    private KitSelectionHandler kitSelectionHandler;
    private KitSelectorGUI kitSelectorGui;
    private GameLoopTimer gameLoopTimer;
    private BukkitMessageUtil bukkitMessageUtil;
    private CTFPlayerMetaData arenaPlayers;

    @BeforeEach
    void setUp() {
        registerMessageHandler();

        gameStateManager = new GameStateManager();
        scoreBoardManager = Mockito.mock(ScoreBoardManager.class);
        playerManager = Mockito.mock(PlayerManager.class);
        flagBaseSetupHandler = Mockito.mock(FlagBaseSetupHandler.class);
        flagLifecycleHandler = Mockito.mock(FlagLifecycleHandler.class);
        teamManager = Mockito.mock(TeamManager.class);
        kitSelectionHandler = Mockito.mock(KitSelectionHandler.class);
        kitSelectorGui = Mockito.mock(KitSelectorGUI.class);
        gameLoopTimer = Mockito.mock(GameLoopTimer.class);
        bukkitMessageUtil = Mockito.mock(BukkitMessageUtil.class);
        arenaPlayers = Mockito.mock(CTFPlayerMetaData.class);

        registerDependencies(
                GameStateManager.class, gameStateManager,
                ScoreBoardManager.class, scoreBoardManager,
                PlayerManager.class, playerManager,
                FlagBaseSetupHandler.class, flagBaseSetupHandler,
                FlagLifecycleHandler.class, flagLifecycleHandler,
                TeamManager.class, teamManager,
                KitSelectionHandler.class, kitSelectionHandler,
                KitSelectorGUI.class, kitSelectorGui,
                GameLoopTimer.class, gameLoopTimer,
                BukkitMessageUtil.class, bukkitMessageUtil,
                CTFPlayerMetaData.class, arenaPlayers);

        gameStateManager.setGameState(GameState.LOBBY);
        gameStateManager.setForcedCountdown(true);
        when(flagBaseSetupHandler.areBasesReady()).thenReturn(true);
    }

    @Test
    void startMatchOpensKitSelectorForUnselectedPlayers() throws Exception {
        Player player = Mockito.mock(Player.class);
        when(teamManager.getJoinedPlayers()).thenReturn(List.of(player));
        when(kitSelectionHandler.hasSelection(player)).thenReturn(false);

        try (MockedConstruction<MatchEffectsHandler> ignored = Mockito.mockConstruction(MatchEffectsHandler.class)) {
            MatchFlowHandler matchFlowHandler = new MatchFlowHandler();
            invokeStartMatch(matchFlowHandler);
        }

        verify(kitSelectorGui).openKitSelector(player, true);
        verify(kitSelectionHandler, never()).applyKitLoadout(Mockito.any(Player.class), Mockito.any(KitType.class));
        verify(scoreBoardManager).updateScoreboards(Duration.ofMinutes(10), GameState.IN_PROGRESS);
    }

    private void invokeStartMatch(MatchFlowHandler matchFlowHandler) throws Exception {
        Method startMatchMethod = MatchFlowHandler.class.getDeclaredMethod("startMatch");
        startMatchMethod.setAccessible(true);
        startMatchMethod.invoke(matchFlowHandler);
        logStep("invoked private startMatch() for default-kit verification");
    }
}
