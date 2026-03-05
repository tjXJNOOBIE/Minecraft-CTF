package dev.tjxjnoobie.ctf.bootstrap.registries;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.handlers.FlagBreakEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.FlagCarrierLockEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.FlagInteractEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IFlagBreakEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IFlagCarrierLockEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IFlagInteractEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerJoinEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerMoveEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerTeleportEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerJoinEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerMoveEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerTeleportEventHandler;
import dev.tjxjnoobie.ctf.game.flag.CaptureZoneParticleRenderer;
import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.CarrierInventoryTracker;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagIndicator;
import dev.tjxjnoobie.ctf.game.flag.FlagPickupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagReturnHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.handlers.CaptureZoneUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBossBarUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBreakHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierLockHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagIndicatorUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagInteractHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagScoreHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamGlowUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.BaseMarkerHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerJoinHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerMoveHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerTeleportHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;

public final class FlagBootstrapRegistry implements BootstrapRegistry {
        /**
         * Updates state for register.
         *
         * @param loader Service loader used to register this bootstrap component.
         * @param plugin Plugin instance used to access Bukkit runtime services.
     */
        // == Lifecycle ==

        @Override
        public void register(DependencyLoader loader, Main plugin) {
                // Register flag lifecycle services and handlers.
                loader.queueDependency(FlagConfigHandler.class, () -> new FlagConfigHandler(plugin));
                loader.queueDependency(FlagEventEffects.class, FlagEventEffects::new);
                loader.queueDependency(FlagIndicator.class,
                                () -> new FlagIndicator(
                                                plugin,
                                                loader.requireInstance(TeamManager.class),
                                                loader.requireInstance(
                                                                dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem.class)));
                loader.queueDependency(CTFCaptureZoneHandler.class, CTFCaptureZoneHandler::new);
                loader.queueDependency(CaptureZoneParticleRenderer.class, CaptureZoneParticleRenderer::new);
                loader.queueDependency(FlagStateRegistry.class, FlagStateRegistry::new);
                loader.queueDependency(TeamBaseMetaDataResolver.class, TeamBaseMetaDataResolver::new);
                loader.queueDependency(FlagIndicatorUiHandler.class, FlagIndicatorUiHandler::new);
                loader.queueDependency(CaptureZoneUiHandler.class, CaptureZoneUiHandler::new);
                loader.queueDependency(FlagBossBarUiHandler.class, FlagBossBarUiHandler::new);
                loader.queueDependency(TeamGlowUiHandler.class, TeamGlowUiHandler::new);
                loader.queueDependency(FlagUiTicker.class, FlagUiTicker::new);
                loader.queueDependency(FlagBlockPlacer.class, FlagBlockPlacer::new);
                loader.queueDependency(CarrierInventoryTracker.class,
                                () -> new CarrierInventoryTracker(loader.requireInstance(
                                                dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils.class)));
                loader.queueDependency(CarrierEffects.class, CarrierEffects::new);
                loader.queueDependency(FlagPickupHandler.class, FlagPickupHandler::new);
                loader.queueDependency(FlagDropHandler.class, FlagDropHandler::new);
                loader.queueDependency(FlagReturnHandler.class, FlagReturnHandler::new);
                loader.queueDependency(BaseMarkerHandler.class, BaseMarkerHandler::new);
                loader.queueDependency(FlagScoreHandler.class, FlagScoreHandler::new);
                loader.queueDependency(FlagCarrierStateHandler.class, FlagCarrierStateHandler::new);
                loader.queueDependency(FlagBaseSetupHandler.class, FlagBaseSetupHandler::new);
                loader.queueDependency(FlagLifecycleHandler.class, FlagLifecycleHandler::new);
                loader.queueDependency(FlagCarrierHandler.class, FlagCarrierHandler::new);
                loader.queueDependency(FlagInteractHandler.class, FlagInteractHandler::new);
                loader.queueDependency(FlagInteractEventHandler.class,
                                () -> loader.requireInstance(FlagInteractHandler.class));
                loader.queueDependency(IFlagInteractEventHandler.class,
                                () -> loader.requireInstance(FlagInteractHandler.class));
                loader.queueDependency(FlagCarrierLockHandler.class, FlagCarrierLockHandler::new);
                loader.queueDependency(FlagCarrierLockEventHandler.class,
                                () -> loader.requireInstance(FlagCarrierLockHandler.class));
                loader.queueDependency(IFlagCarrierLockEventHandler.class,
                                () -> loader.requireInstance(FlagCarrierLockHandler.class));
                loader.queueDependency(FlagBreakHandler.class, FlagBreakHandler::new);
                loader.queueDependency(FlagBreakEventHandler.class,
                                () -> loader.requireInstance(FlagBreakHandler.class));
                loader.queueDependency(IFlagBreakEventHandler.class,
                                () -> loader.requireInstance(FlagBreakHandler.class));
                loader.queueDependency(PlayerJoinHandler.class, PlayerJoinHandler::new);
                loader.queueDependency(PlayerJoinEventHandler.class,
                                () -> loader.requireInstance(PlayerJoinHandler.class));
                loader.queueDependency(IPlayerJoinEventHandler.class,
                                () -> loader.requireInstance(PlayerJoinHandler.class));
                loader.queueDependency(PlayerMoveHandler.class, PlayerMoveHandler::new);
                loader.queueDependency(PlayerMoveEventHandler.class,
                                () -> loader.requireInstance(PlayerMoveHandler.class));
                loader.queueDependency(IPlayerMoveEventHandler.class,
                                () -> loader.requireInstance(PlayerMoveHandler.class));
                loader.queueDependency(PlayerTeleportHandler.class, PlayerTeleportHandler::new);
                loader.queueDependency(PlayerTeleportEventHandler.class,
                                () -> loader.requireInstance(PlayerTeleportHandler.class));
                loader.queueDependency(IPlayerTeleportEventHandler.class,
                                () -> loader.requireInstance(PlayerTeleportHandler.class));
        }
}
