package dev.tjxjnoobie.ctf.events.player;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.player.effects.PlayerDeathEffects;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerDeathHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnScheduler;
import java.util.ArrayList;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CTFPlayerDeathEventSpearAttributionTest extends TestLogSupport {
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
    void creditsIndirectSpearKillWhenNoDirectKiller() {
        PlayerDeathEvent event = Mockito.mock(PlayerDeathEvent.class);
        Player victim = Mockito.mock(Player.class);
        Player credited = Mockito.mock(Player.class);
        Player.Spigot spigot = Mockito.mock(Player.Spigot.class);
        org.bukkit.inventory.PlayerInventory inventory = Mockito.mock(org.bukkit.inventory.PlayerInventory.class);
        EntityDamageEvent lastDamage = Mockito.mock(EntityDamageEvent.class);
        UUID creditedId = UUID.randomUUID();

        when(event.getEntity()).thenReturn(victim);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.getName()).thenReturn("Victim");
        when(victim.spigot()).thenReturn(spigot);
        when(victim.getInventory()).thenReturn(inventory);
        when(victim.getKiller()).thenReturn(null);
        when(victim.getLastDamageCause()).thenReturn(lastDamage);
        when(lastDamage.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        when(sessionHandler.isPlayerInArena(victim)).thenReturn(true);
        when(flagCarrierStateHandler.isFlagCarrier(Mockito.any())).thenReturn(false);
        when(event.getDrops()).thenReturn(new ArrayList<>());

        SpearDeathAttributionMetaData attribution = new SpearDeathAttributionMetaData(Component.text("spear"), creditedId);
        when(HomingSpearAbilityHandler.resolveTrackedSpearAttribution(victim, EntityDamageEvent.DamageCause.FALL, null))
            .thenReturn(attribution);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(creditedId)).thenReturn(credited);

            listener.onDeath(event);
        }

        verify(sessionHandler).recordKill(credited);
        verify(deathEffects).playAllDeathEffects(victim, null, credited, attribution.message(), false);
    }
}
