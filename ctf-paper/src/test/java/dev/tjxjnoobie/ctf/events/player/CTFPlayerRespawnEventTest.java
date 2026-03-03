package dev.tjxjnoobie.ctf.events.player;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.combat.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.kit.KitManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerRespawnEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFPlayerRespawnEventTest] ";

    @Test
    void appliesSpawnAndKitWhenRunning() {
        Bukkit.getLogger().info(LOG_PREFIX + "Respawn during match: set spawn, reapply kit, grant brief protection.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        KitManager kitManager = Mockito.mock(KitManager.class);
        ScoutTaggerAbility scoutAbility = Mockito.mock(ScoutTaggerAbility.class);
        CTFPlayerRespawnEvent listener = new CTFPlayerRespawnEvent(gameManager, kitManager, scoutAbility);

        // Emulate a respawn during an active match.
        PlayerRespawnEvent event = Mockito.mock(PlayerRespawnEvent.class);
        Player player = Mockito.mock(Player.class);
        Location respawn = new Location(null, 5, 64, 5);

        when(event.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Tester");
        when(gameManager.getRespawnLocation(player)).thenReturn(respawn);
        when(gameManager.isRunning()).thenReturn(true);
        when(gameManager.isPlayerInGame(player)).thenReturn(true);

        listener.onRespawn(event);

        verify(event).setRespawnLocation(respawn);
        verify(kitManager).applyKit(player);
        verify(player).setNoDamageTicks(40);
        verify(scoutAbility).handleRespawn(player);
        Bukkit.getLogger().info(LOG_PREFIX + "respawn event applies spawn and kit when running");
    }
}


