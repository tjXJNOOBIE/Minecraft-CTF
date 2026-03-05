package dev.tjxjnoobie.ctf.events.player;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.player.effects.PlayerDeathEffects;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerDeathHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnScheduler;
import java.util.ArrayList;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFPlayerDeathEventTest extends TestLogSupport {
    // Dependencies
    private MatchPlayerSessionHandler sessionHandler;
    private FlagCarrierStateHandler flagCarrierStateHandler;
    private FlagDropHandler flagDropHandler;
    private HomingSpearAbilityHandler HomingSpearAbilityHandler;
    private PlayerRespawnScheduler respawnScheduler;
    private PlayerDeathEffects deathEffects;
    private CTFPlayerDeathEvent listener;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();
        sessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        flagDropHandler = Mockito.mock(FlagDropHandler.class);
        HomingSpearAbilityHandler = Mockito.mock(HomingSpearAbilityHandler.class);
        respawnScheduler = Mockito.mock(PlayerRespawnScheduler.class);
        deathEffects = Mockito.mock(PlayerDeathEffects.class);

        registerDependencies(
                MatchPlayerSessionHandler.class, sessionHandler,
                FlagCarrierStateHandler.class, flagCarrierStateHandler,
                FlagDropHandler.class, flagDropHandler,
                HomingSpearAbilityHandler.class, HomingSpearAbilityHandler,
                PlayerRespawnScheduler.class, respawnScheduler,
                PlayerDeathEffects.class, deathEffects,
                PlayerDeathHandler.class, new PlayerDeathHandler()
        );
        listener = new CTFPlayerDeathEvent();
    }

    @Test
    void clearsDropsAndKeepsInventory() {
        PlayerDeathEvent event = Mockito.mock(PlayerDeathEvent.class);
        Player victim = Mockito.mock(Player.class);
        Player killer = Mockito.mock(Player.class);
        Player.Spigot spigot = Mockito.mock(Player.Spigot.class);
        org.bukkit.inventory.PlayerInventory inventory = Mockito.mock(org.bukkit.inventory.PlayerInventory.class);

        when(event.getEntity()).thenReturn(victim);
        when(victim.getName()).thenReturn("Victim");
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.spigot()).thenReturn(spigot);
        when(victim.getInventory()).thenReturn(inventory);
        when(victim.displayName()).thenReturn(Component.text("Victim"));
        when(victim.getKiller()).thenReturn(killer);
        when(killer.displayName()).thenReturn(Component.text("Killer"));
        when(sessionHandler.isPlayerInArena(victim)).thenReturn(true);
        when(flagCarrierStateHandler.isFlagCarrier(Mockito.any())).thenReturn(false);
        when(event.getDrops()).thenReturn(new ArrayList<>());

        listener.onDeath(event);

        verify(event).setKeepInventory(true);
        verify(event).setDroppedExp(0);
        verify(event).setDeathMessage(null);
        verify(flagDropHandler).dropCarriedFlagIfPresent(victim);
        verify(sessionHandler).recordDeath(victim);
    }
}
