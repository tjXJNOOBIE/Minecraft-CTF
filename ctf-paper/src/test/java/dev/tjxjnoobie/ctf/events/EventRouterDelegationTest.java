package dev.tjxjnoobie.ctf.events;

import static org.mockito.Mockito.verify;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.events.entity.CTFEntityDamageEvent;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.entity.CTFEntityDamageByEntityEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryClickEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryDragEvent;
import dev.tjxjnoobie.ctf.events.player.CTFAsyncChatEvent;
import dev.tjxjnoobie.ctf.events.player.CTFBlockBreakEvent;
import dev.tjxjnoobie.ctf.events.player.CTFBlockPlaceEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDeathEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDropItemEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerInteractEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerItemHeldEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerJoinEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerMoveEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerQuitEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerRespawnEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerSwapHandItemsEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerTeleportEvent;
import dev.tjxjnoobie.ctf.events.projectile.CTFProjectileHitEvent;
import dev.tjxjnoobie.ctf.events.projectile.CTFProjectileLaunchEvent;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.combat.handlers.CombatDamageRestrictionHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBreakHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierLockHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagInteractHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerBuildRestrictionHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerChatHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerDeathHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerJoinHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerMoveHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerQuitHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerTeleportHandler;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EventRouterDelegationTest extends TestLogSupport {
    @Test
    void playerJoinRouterDelegatesOnce() {
        PlayerJoinHandler handler = Mockito.mock(PlayerJoinHandler.class);
        resetAndRegister(PlayerJoinHandler.class, handler);
        CTFPlayerJoinEvent listener = new CTFPlayerJoinEvent();
        PlayerJoinEvent event = Mockito.mock(PlayerJoinEvent.class);

        listener.onJoin(event);

        verify(handler).onPlayerJoin(event);
    }

    @Test
    void playerQuitRouterDelegatesOnce() {
        PlayerQuitHandler quitHandler = Mockito.mock(PlayerQuitHandler.class);
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        IScoutTaggerCombatEventHandler scoutHandler = Mockito.mock(IScoutTaggerCombatEventHandler.class);
        resetAndRegister(
            PlayerQuitHandler.class, quitHandler,
            IHomingSpearCombatEventHandler.class, homingHandler,
            IScoutTaggerCombatEventHandler.class, scoutHandler
        );
        CTFPlayerQuitEvent listener = new CTFPlayerQuitEvent();
        PlayerQuitEvent event = Mockito.mock(PlayerQuitEvent.class);

        listener.onQuit(event);

        verify(quitHandler).onPlayerQuit(event);
        verify(homingHandler).onPlayerQuit(event);
        verify(scoutHandler).onPlayerQuit(event);
    }

    @Test
    void playerMoveRouterDelegatesOnce() {
        PlayerMoveHandler handler = Mockito.mock(PlayerMoveHandler.class);
        resetAndRegister(PlayerMoveHandler.class, handler);
        CTFPlayerMoveEvent listener = new CTFPlayerMoveEvent();
        PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);

        listener.onMove(event);

        verify(handler).onPlayerMove(event);
    }

    @Test
    void playerTeleportRouterDelegatesOnce() {
        PlayerTeleportHandler handler = Mockito.mock(PlayerTeleportHandler.class);
        resetAndRegister(PlayerTeleportHandler.class, handler);
        CTFPlayerTeleportEvent listener = new CTFPlayerTeleportEvent();
        PlayerTeleportEvent event = Mockito.mock(PlayerTeleportEvent.class);

        listener.onTeleport(event);

        verify(handler).onPlayerTeleport(event);
    }

    @Test
    void playerRespawnRouterDelegatesOnce() {
        PlayerRespawnHandler handler = Mockito.mock(PlayerRespawnHandler.class);
        resetAndRegister(PlayerRespawnHandler.class, handler);
        CTFPlayerRespawnEvent listener = new CTFPlayerRespawnEvent();
        PlayerRespawnEvent event = Mockito.mock(PlayerRespawnEvent.class);

        listener.onRespawn(event);

        verify(handler).onPlayerRespawn(event);
    }

    @Test
    void playerDeathRouterDelegatesOnce() {
        PlayerDeathHandler handler = Mockito.mock(PlayerDeathHandler.class);
        resetAndRegister(PlayerDeathHandler.class, handler);
        CTFPlayerDeathEvent listener = new CTFPlayerDeathEvent();
        PlayerDeathEvent event = Mockito.mock(PlayerDeathEvent.class);

        listener.onDeath(event);

        verify(handler).onPlayerDeath(event);
    }

    @Test
    void asyncChatRouterDelegatesOnce() {
        PlayerChatHandler handler = Mockito.mock(PlayerChatHandler.class);
        resetAndRegister(PlayerChatHandler.class, handler);
        CTFAsyncChatEvent listener = new CTFAsyncChatEvent();
        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);

        listener.onChat(event);

        verify(handler).onAsyncChat(event);
    }

    @Test
    void playerInteractRouterDelegatesOnce() {
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
    void playerSwapHandRouterDelegatesOnce() {
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
    void playerItemHeldRouterDelegatesOnce() {
        FlagCarrierLockHandler handler = Mockito.mock(FlagCarrierLockHandler.class);
        resetAndRegister(FlagCarrierLockHandler.class, handler);
        CTFPlayerItemHeldEvent listener = new CTFPlayerItemHeldEvent();
        PlayerItemHeldEvent event = Mockito.mock(PlayerItemHeldEvent.class);

        listener.onItemHeld(event);

        verify(handler).onPlayerItemHeld(event);
    }

    @Test
    void playerDropItemRouterDelegatesOnce() {
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
    void inventoryClickRouterDelegatesOnce() {
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
    void inventoryDragRouterDelegatesOnce() {
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
    void blockPlaceRouterDelegatesOnce() {
        PlayerBuildRestrictionHandler handler = Mockito.mock(PlayerBuildRestrictionHandler.class);
        resetAndRegister(PlayerBuildRestrictionHandler.class, handler);
        CTFBlockPlaceEvent listener = new CTFBlockPlaceEvent();
        BlockPlaceEvent event = Mockito.mock(BlockPlaceEvent.class);

        listener.onPlace(event);

        verify(handler).onBlockPlace(event);
    }

    @Test
    void blockBreakRouterDelegatesOnce() {
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

    @Test
    void projectileHitRouterDelegatesOnce() {
        IHomingSpearCombatEventHandler homingHandler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        IScoutTaggerCombatEventHandler scoutHandler = Mockito.mock(IScoutTaggerCombatEventHandler.class);
        resetAndRegister(
            IHomingSpearCombatEventHandler.class, homingHandler,
            IScoutTaggerCombatEventHandler.class, scoutHandler
        );
        CTFProjectileHitEvent listener = new CTFProjectileHitEvent();
        ProjectileHitEvent event = Mockito.mock(ProjectileHitEvent.class);

        listener.onHit(event);

        verify(homingHandler).onProjectileHit(event);
        verify(scoutHandler).onProjectileHit(event);
    }

    @Test
    void projectileLaunchRouterDelegatesOnce() {
        IHomingSpearCombatEventHandler handler = Mockito.mock(IHomingSpearCombatEventHandler.class);
        resetAndRegister(IHomingSpearCombatEventHandler.class, handler);
        CTFProjectileLaunchEvent listener = new CTFProjectileLaunchEvent();
        ProjectileLaunchEvent event = Mockito.mock(ProjectileLaunchEvent.class);

        listener.onLaunch(event);

        verify(handler).onProjectileLaunch(event);
    }

    @Test
    void entityDamageByEntityRouterDelegatesOnce() {
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
    void entityDamageRouterDelegatesOnce() {
        CombatDamageRestrictionHandler combatHandler = Mockito.mock(CombatDamageRestrictionHandler.class);
        resetAndRegister(CombatDamageRestrictionHandler.class, combatHandler);
        CTFEntityDamageEvent listener = new CTFEntityDamageEvent();
        EntityDamageEvent event = Mockito.mock(EntityDamageEvent.class);

        listener.onDamage(event);

        verify(combatHandler).onEntityDamage(event);
    }

    // Helpers
    private void resetAndRegister(Object... typeAndHandler) {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        for (int i = 0; i < typeAndHandler.length; i += 2) {
            registerDependency((Class<?>) typeAndHandler[i], typeAndHandler[i + 1]);
        }
    }
}
