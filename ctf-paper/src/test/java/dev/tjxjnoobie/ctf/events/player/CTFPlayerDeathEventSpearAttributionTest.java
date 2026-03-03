package dev.tjxjnoobie.ctf.events.player;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.combat.HomingSpearAbility;
import dev.tjxjnoobie.ctf.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerDeathEventSpearAttributionTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [CTFPlayerDeathEventSpearAttributionTest] ";

    @Test
    void creditsIndirectSpearKillWhenNoDirectKiller() {
        Bukkit.getLogger().info(LOG_PREFIX + "Spear indirect death: credit shooter and broadcast spear kill feed.");
        CtfMatchOrchestrator gameManager = Mockito.mock(CtfMatchOrchestrator.class);
        FlagManager flagManager = Mockito.mock(FlagManager.class);
        BossBarManager bossBarManager = Mockito.mock(BossBarManager.class);
        HomingSpearAbility homingSpearAbility = Mockito.mock(HomingSpearAbility.class);
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);

        CTFPlayerDeathEvent listener = new CTFPlayerDeathEvent(gameManager, flagManager, bossBarManager, homingSpearAbility, plugin);

        PlayerDeathEvent event = Mockito.mock(PlayerDeathEvent.class);
        Player victim = Mockito.mock(Player.class);
        Player credited = Mockito.mock(Player.class);
        Player.Spigot spigot = Mockito.mock(Player.Spigot.class);
        org.bukkit.inventory.PlayerInventory inventory = Mockito.mock(org.bukkit.inventory.PlayerInventory.class);
        EntityDamageEvent lastDamage = Mockito.mock(EntityDamageEvent.class);

        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        when(event.getEntity()).thenReturn(victim);
        when(victim.getName()).thenReturn("Victim");
        when(victim.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(victim.spigot()).thenReturn(spigot);
        when(victim.getInventory()).thenReturn(inventory);
        when(victim.getKiller()).thenReturn(null);
        when(victim.getLastDamageCause()).thenReturn(lastDamage);
        when(lastDamage.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        when(credited.getName()).thenReturn("Shooter");
        java.util.UUID creditedId = java.util.UUID.randomUUID();
        when(credited.getUniqueId()).thenReturn(creditedId);
        when(Bukkit.getServer().getPlayer(creditedId)).thenReturn(credited);
        when(gameManager.isPlayerInGame(victim)).thenReturn(true);
        when(event.getDrops()).thenReturn(drops);

        SpearDeathAttributionMetaData attribution = new SpearDeathAttributionMetaData(Component.text("spear"), creditedId);
        when(homingSpearAbility.resolveSpearAttribution(victim, EntityDamageEvent.DamageCause.FALL, null))
            .thenReturn(attribution);

        listener.onDeath(event);

        verify(gameManager).recordKill(credited);
        verify(bossBarManager).showKillBar(Mockito.eq(credited), Mockito.any());
        verify(gameManager).broadcastToArena(Mockito.any(Component.class));
    }
}

