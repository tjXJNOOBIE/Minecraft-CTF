package dev.tjxjnoobie.ctf.events.player;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerQuitHandler;
import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerQuitEventTest extends TestLogSupport {
    // Dependencies
    private MatchPlayerSessionHandler sessionHandler;
    private BuildToggleUtil buildToggleUtil;
    private CTFPlayerQuitEvent listener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        buildToggleUtil = Mockito.mock(BuildToggleUtil.class);
        registerDependencies(
                MatchPlayerSessionHandler.class, sessionHandler,
                BuildToggleUtil.class, buildToggleUtil,
                PlayerQuitHandler.class, new PlayerQuitHandler(),
                IHomingSpearCombatEventHandler.class, Mockito.mock(IHomingSpearCombatEventHandler.class),
                IScoutTaggerCombatEventHandler.class, Mockito.mock(IScoutTaggerCombatEventHandler.class)
        );
        listener = new CTFPlayerQuitEvent();
    }

    @Test
    void delegatesToSessionCleanup() {
        PlayerQuitEvent event = Mockito.mock(PlayerQuitEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");
        when(sessionHandler.isPlayerInArena(player)).thenReturn(true);

        listener.onQuit(event);

        verify(sessionHandler).removePlayerFromArena(player, false);
        verify(buildToggleUtil).clearPlayer(player);
        verify(event).quitMessage(any());
    }
}
