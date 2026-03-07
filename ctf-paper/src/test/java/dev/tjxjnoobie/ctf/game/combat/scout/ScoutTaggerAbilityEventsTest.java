package dev.tjxjnoobie.ctf.game.combat.scout;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ScoutTaggerAbilityEventsTest extends TestLogSupport {

    private ScoutTaggerAbility ability;
    private ScoutTaggerAbilityEvents events;

    @BeforeEach
    void setUp() {
        ability = Mockito.mock(ScoutTaggerAbility.class);
        events = new ScoutTaggerAbilityEvents(ability);
        logStep("constructed scout event adapter");
    }

    @Test
    void onPlayerInteractIgnoresLeftClickActions() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);

        events.onPlayerInteract(event);

        verify(ability, never()).tryThrowScoutSnowball(Mockito.any());
    }

    @Test
    void onPlayerInteractAllowsOffhandFallbackForAirUse() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(ability.tryThrowScoutSnowball(event.getPlayer())).thenReturn(true);

        events.onPlayerInteract(event);

        verify(ability, times(1)).tryThrowScoutSnowball(Mockito.any());
        verify(event).setCancelled(true);
    }

    @Test
    void onPlayerInteractCancelsMainHandRightClickWhenAbilityConsumesThrow() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(ability.tryThrowScoutSnowball(event.getPlayer())).thenReturn(true);

        events.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(event).setUseItemInHand(Result.DENY);
    }

    @Test
    void onPlayerInteractAlsoConsumesMainHandRightClickBlock() {
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(ability.tryThrowScoutSnowball(event.getPlayer())).thenReturn(true);

        events.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(event).setUseItemInHand(Result.DENY);
    }
}
