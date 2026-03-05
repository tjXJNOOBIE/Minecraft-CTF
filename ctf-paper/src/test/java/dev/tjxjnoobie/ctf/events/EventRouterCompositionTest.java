package dev.tjxjnoobie.ctf.events;

import static org.mockito.Mockito.verify;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.entity.CTFEntityDamageEvent;
import dev.tjxjnoobie.ctf.events.entity.CTFEntityDamageByEntityEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryClickEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryDragEvent;
import dev.tjxjnoobie.ctf.events.player.CTFBlockBreakEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDropItemEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerInteractEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerSwapHandItemsEvent;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.combat.handlers.CombatDamageRestrictionHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBreakHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierLockHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagInteractHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerBuildRestrictionHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EventRouterCompositionTest extends TestLogSupport {
    @Test
    void entityDamageRouterRunsAllInlineDelegates() {
        CombatDamageRestrictionHandler combatHandler = Mockito.mock(CombatDamageRestrictionHandler.class);
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        IScoutTaggerCombatEventHandler scoutHandler = Mockito.mock(IScoutTaggerCombatEventHandler.class);
        resetAndRegister(
            CombatDamageRestrictionHandler.class, combatHandler,
            IHomingSpearCombatEventHandler.class, homingHandler,
            IScoutTaggerCombatEventHandler.class, scoutHandler
        );

        CTFEntityDamageByEntityEvent listener = new CTFEntityDamageByEntityEvent();
        EntityDamageByEntityEvent event = Mockito.mock(EntityDamageByEntityEvent.class);
        listener.onDamage(event);

        verify(combatHandler).onEntityDamageByEntity(event);
        verify(homingHandler).onEntityDamageByEntity(event);
        verify(scoutHandler).onEntityDamageByEntity(event);
    }

    @Test
    void entityDamageRouterRunsRestrictionDelegate() {
        CombatDamageRestrictionHandler combatHandler = Mockito.mock(CombatDamageRestrictionHandler.class);
        resetAndRegister(CombatDamageRestrictionHandler.class, combatHandler);

        CTFEntityDamageEvent listener = new CTFEntityDamageEvent();
        EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);
        listener.onDamage(event);

        verify(combatHandler).onEntityDamage(event);
    }

    @Test
    void swapHandsRouterRunsAllInlineDelegates() {
        FlagCarrierLockHandler carrierLockHandler = Mockito.mock(FlagCarrierLockHandler.class);
        PlayerBuildRestrictionHandler buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        resetAndRegister(
            FlagCarrierLockHandler.class, carrierLockHandler,
            PlayerBuildRestrictionHandler.class, buildRestrictionHandler,
            IHomingSpearCombatEventHandler.class, homingHandler
        );

        CTFPlayerSwapHandItemsEvent listener = new CTFPlayerSwapHandItemsEvent();
        PlayerSwapHandItemsEvent event = Mockito.mock(PlayerSwapHandItemsEvent.class);
        listener.onSwapHandItems(event);

        verify(carrierLockHandler).onPlayerSwapHandItems(event);
        verify(buildRestrictionHandler).onPlayerSwapHandItems(event);
        verify(homingHandler).onPlayerSwapHandItems(event);
    }

    @Test
    void interactRouterRunsAllInlineDelegates() {
        FlagInteractHandler flagInteractHandler = Mockito.mock(FlagInteractHandler.class);
        IScoutTaggerCombatEventHandler scoutHandler = Mockito.mock(IScoutTaggerCombatEventHandler.class);
        resetAndRegister(
            FlagInteractHandler.class, flagInteractHandler,
            IScoutTaggerCombatEventHandler.class, scoutHandler
        );

        CTFPlayerInteractEvent listener = new CTFPlayerInteractEvent();
        PlayerInteractEvent event = Mockito.mock(PlayerInteractEvent.class);
        listener.onInteract(event);

        verify(flagInteractHandler).onPlayerInteract(event);
        verify(scoutHandler).onPlayerInteract(event);
    }

    @Test
    void inventoryClickRouterRunsAllInlineDelegates() {
        FlagCarrierLockHandler carrierLockHandler = Mockito.mock(FlagCarrierLockHandler.class);
        PlayerBuildRestrictionHandler buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        resetAndRegister(
            FlagCarrierLockHandler.class, carrierLockHandler,
            PlayerBuildRestrictionHandler.class, buildRestrictionHandler,
            IHomingSpearCombatEventHandler.class, homingHandler
        );

        CTFInventoryClickEvent listener = new CTFInventoryClickEvent();
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        listener.onClick(event);

        verify(carrierLockHandler).onInventoryClick(event);
        verify(buildRestrictionHandler).onInventoryClick(event);
        verify(homingHandler).onInventoryClick(event);
    }

    @Test
    void inventoryDragRouterRunsAllInlineDelegates() {
        FlagCarrierLockHandler carrierLockHandler = Mockito.mock(FlagCarrierLockHandler.class);
        PlayerBuildRestrictionHandler buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        resetAndRegister(
            FlagCarrierLockHandler.class, carrierLockHandler,
            PlayerBuildRestrictionHandler.class, buildRestrictionHandler,
            IHomingSpearCombatEventHandler.class, homingHandler
        );

        CTFInventoryDragEvent listener = new CTFInventoryDragEvent();
        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        listener.onDrag(event);

        verify(carrierLockHandler).onInventoryDrag(event);
        verify(buildRestrictionHandler).onInventoryDrag(event);
        verify(homingHandler).onInventoryDrag(event);
    }

    @Test
    void playerDropItemRouterRunsAllInlineDelegates() {
        FlagCarrierLockHandler carrierLockHandler = Mockito.mock(FlagCarrierLockHandler.class);
        PlayerBuildRestrictionHandler buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        resetAndRegister(
            FlagCarrierLockHandler.class, carrierLockHandler,
            PlayerBuildRestrictionHandler.class, buildRestrictionHandler,
            IHomingSpearCombatEventHandler.class, homingHandler
        );

        CTFPlayerDropItemEvent listener = new CTFPlayerDropItemEvent();
        PlayerDropItemEvent event = Mockito.mock(PlayerDropItemEvent.class);
        listener.onDropItem(event);

        verify(carrierLockHandler).onPlayerDropItem(event);
        verify(buildRestrictionHandler).onPlayerDropItem(event);
        verify(homingHandler).onPlayerDropItem(event);
    }

    @Test
    void blockBreakRouterRunsAllInlineDelegates() {
        FlagBreakHandler flagBreakHandler = Mockito.mock(FlagBreakHandler.class);
        PlayerBuildRestrictionHandler buildRestrictionHandler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        resetAndRegister(
            FlagBreakHandler.class, flagBreakHandler,
            PlayerBuildRestrictionHandler.class, buildRestrictionHandler
        );

        CTFBlockBreakEvent listener = new CTFBlockBreakEvent();
        BlockBreakEvent event = Mockito.mock(BlockBreakEvent.class);
        listener.onBreak(event);

        verify(flagBreakHandler).onBlockBreak(event);
        verify(buildRestrictionHandler).onBlockBreak(event);
    }

    private void resetAndRegister(Object... typeAndHandler) {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        for (int i = 0; i < typeAndHandler.length; i += 2) {
            registerDependency((Class<?>) typeAndHandler[i], typeAndHandler[i + 1]);
        }
    }
}
