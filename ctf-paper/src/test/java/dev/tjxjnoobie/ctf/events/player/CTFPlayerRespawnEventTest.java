package dev.tjxjnoobie.ctf.events.player;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerRespawnEventTest extends TestLogSupport {
    // Dependencies
    private MatchPlayerSessionHandler sessionHandler;
    private GameStateManager gameStateManager;
    private KitSelectionHandler kitSelectionHandler;
    private KitSelectorGUI kitSelectorGui;
    private ScoutTaggerAbility scoutAbility;
    private CTFPlayerRespawnEvent listener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        gameStateManager = Mockito.mock(GameStateManager.class);
        kitSelectionHandler = Mockito.mock(KitSelectionHandler.class);
        kitSelectorGui = Mockito.mock(KitSelectorGUI.class);
        scoutAbility = Mockito.mock(ScoutTaggerAbility.class);
        registerDependencies(
                MatchPlayerSessionHandler.class, sessionHandler,
                GameStateManager.class, gameStateManager,
                KitSelectionHandler.class, kitSelectionHandler,
                KitSelectorGUI.class, kitSelectorGui,
                ScoutTaggerAbility.class, scoutAbility,
                PlayerRespawnHandler.class, new PlayerRespawnHandler()
        );
        listener = new CTFPlayerRespawnEvent();
    }

    @Test
    void appliesSpawnAndKitWhenRunning() {
        PlayerRespawnEvent event = Mockito.mock(PlayerRespawnEvent.class);
        Player player = Mockito.mock(Player.class);
        Location respawn = new Location(null, 5, 64, 5);

        when(event.getPlayer()).thenReturn(player);
        when(sessionHandler.getRespawnLocation(player)).thenReturn(respawn);
        when(gameStateManager.isCleanupInProgress()).thenReturn(false);
        when(gameStateManager.isRunning()).thenReturn(true);
        when(sessionHandler.isPlayerInArena(player)).thenReturn(true);
        when(kitSelectionHandler.hasSelection(player)).thenReturn(true);
        when(kitSelectionHandler.getSelectedKit(player)).thenReturn(KitType.SCOUT);

        listener.onRespawn(event);

        verify(event).setRespawnLocation(respawn);
        verify(kitSelectionHandler).applyKitLoadout(player, KitType.SCOUT);
        verify(player).setNoDamageTicks(60);
        verify(scoutAbility).processRespawnState(player);
    }
}
