package dev.tjxjnoobie.ctf.events.player;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.player.managers.BuildBypassManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerQuitEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFPlayerQuitEventTest] ";

    @Test
    void delegatesToGameManager() {
        Bukkit.getLogger().info(LOG_PREFIX + "Player quit: remove from arena and cleanup state.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        BuildBypassManager bypassManager = Mockito.mock(BuildBypassManager.class);
        CTFPlayerQuitEvent listener = new CTFPlayerQuitEvent(gameManager, bypassManager);

        // Simulate a player quitting while in a match.
        PlayerQuitEvent event = Mockito.mock(PlayerQuitEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");

        listener.onQuit(event);

        verify(gameManager).handlePlayerLeave(player, false);
        Bukkit.getLogger().info(LOG_PREFIX + "quit event delegates to game manager");
    }
}


