package dev.tjxjnoobie.ctf.events.player;

import org.bukkit.Bukkit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.combat.HomingSpearAbility;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerDeathEventTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFPlayerDeathEventTest] ";

    @Test
    void clearsDropsAndKeepsInventory() {
        Bukkit.getLogger().info(LOG_PREFIX + "Simulate CTF death: expect keep inventory, no drops, kill feed handled.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        BossBarManager bossBarManager = Mockito.mock(BossBarManager.class);
        HomingSpearAbility homingSpearAbility = Mockito.mock(HomingSpearAbility.class);
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);

        CTFPlayerDeathEvent listener = new CTFPlayerDeathEvent(gameManager, flagManager, bossBarManager, homingSpearAbility, plugin);

        PlayerDeathEvent event = Mockito.mock(PlayerDeathEvent.class);
        Player victim = Mockito.mock(Player.class);
        Player killer = Mockito.mock(Player.class);
        Player.Spigot spigot = Mockito.mock(Player.Spigot.class);
        org.bukkit.inventory.PlayerInventory inventory = Mockito.mock(org.bukkit.inventory.PlayerInventory.class);

        // Configure a death event in a running match.
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        when(event.getEntity()).thenReturn(victim);
        when(victim.getName()).thenReturn("Victim");
        when(victim.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(victim.spigot()).thenReturn(spigot);
        when(victim.getInventory()).thenReturn(inventory);
        when(victim.getKiller()).thenReturn(killer);
        when(killer.getName()).thenReturn("Killer");
        when(gameManager.isPlayerInGame(victim)).thenReturn(true);
        when(event.getDrops()).thenReturn(drops);

        listener.onDeath(event);

        verify(event).setKeepInventory(true);
        verify(event).setDroppedExp(0);
        verify(event).setDeathMessage(null);
        Bukkit.getLogger().info(LOG_PREFIX + "death event clears drops and keeps inventory");
    }
}


