package dev.tjxjnoobie.ctf.bootstrap.registries;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.config.CTFConfig;
import dev.tjxjnoobie.ctf.config.message.MessageConfig;
import dev.tjxjnoobie.ctf.config.message.MessageConfigHandler;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerBuildGuardEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerChatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerDeathEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerItemPickupEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerQuitEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IPlayerRespawnEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerBuildGuardEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerChatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerDeathEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerItemPickupEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerQuitEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerRespawnEventHandler;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.celebration.managers.WinnerCelebration;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
import dev.tjxjnoobie.ctf.game.player.effects.PlayerDeathEffects;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerBuildRestrictionHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerChatHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerDeathHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerItemPickupHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerQuitHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnHandler;
import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnScheduler;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.tasks.CTFTaskRegistry;
import dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.items.kit.RangerIconItem;
import dev.tjxjnoobie.ctf.items.kit.ScoutIconItem;
import dev.tjxjnoobie.ctf.items.kit.ScoutSwordItem;
import dev.tjxjnoobie.ctf.items.kit.SpearLockedPlaceholderItem;
import dev.tjxjnoobie.ctf.items.kit.SpearReturningPlaceholderItem;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.scoreboard.handlers.CooldownLineProviderHandler;
import dev.tjxjnoobie.ctf.team.SpawnConfigHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.team.TeamReturnStore;
import dev.tjxjnoobie.ctf.team.TeamSpawnStore;
import dev.tjxjnoobie.ctf.team.LobbySpawnStore;
import dev.tjxjnoobie.ctf.team.handlers.TeamChatRenderer;
import dev.tjxjnoobie.ctf.team.handlers.TeamMembershipHandler;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import dev.tjxjnoobie.ctf.util.tasks.FastTickBus;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Registers the shared game-domain dependencies that the rest of the plugin builds on.
 */
public final class GameBootstrapRegistry implements BootstrapRegistry {
        // == Lifecycle ==

    /**
     * Queues the base game dependencies in the order they must be constructed.
     *
     * @param loader Dependency container used by plugin bootstrap.
     * @param plugin Active plugin instance for config and Bukkit access.
     */
    @Override
    public void register(DependencyLoader loader, Main plugin) {
        // Register core config, task, item, and gameplay collaborators first.
        loader.registerImportantInstance(Main.class, plugin);
        loader.queueDependency(IInventoryUtils.class, InventoryUtils::new);
        loader.queueDependency(CTFConfig.class, () -> new CTFConfig(plugin));
        loader.queueDependency(MessageConfig.class, () -> new MessageConfig(plugin));
        loader.queueDependency(MessageConfigHandler.class, MessageConfigHandler::new);
        loader.queueDependency(CTFTaskRegistry.class, CTFTaskRegistry::new);
        loader.queueDependency(FastTickBus.class, FastTickBus::new);
        loader.queueDependency(ScoutIconItem.class, ScoutIconItem::new);
        loader.queueDependency(RangerIconItem.class, RangerIconItem::new);
        loader.queueDependency(FlagIndicatorItem.class, FlagIndicatorItem::new);
        loader.queueDependency(SpearLockedPlaceholderItem.class, SpearLockedPlaceholderItem::new);
        loader.queueDependency(SpearReturningPlaceholderItem.class, SpearReturningPlaceholderItem::new);
        loader.queueDependency(ScoutSwordItem.class,
                () -> new ScoutSwordItem(loader.requireInstance(IInventoryUtils.class)));
        loader.queueDependency(HomingSpearItem.class,
                () -> new HomingSpearItem(loader.requireInstance(IInventoryUtils.class)));

        loader.queueDependency(KitSelectionHandler.class, KitSelectionHandler::new);
        loader.queueDependency(KitSelectorGUI.class, () -> new KitSelectorGUI(
                loader.requireInstance(KitSelectionHandler.class)));

        loader.queueDependency(SpawnConfigHandler.class, () -> new SpawnConfigHandler(plugin));
        loader.queueDependency(TeamSpawnStore.class,
                () -> new TeamSpawnStore(loader.requireInstance(SpawnConfigHandler.class)));
        loader.queueDependency(TeamReturnStore.class,
                () -> new TeamReturnStore(loader.requireInstance(SpawnConfigHandler.class)));
        loader.queueDependency(LobbySpawnStore.class,
                () -> new LobbySpawnStore(loader.requireInstance(SpawnConfigHandler.class)));
        // Team membership is scoreboard-backed, so register the shared scoreboard first.
        loader.queueDependency(Scoreboard.class,
                () -> Bukkit.getScoreboardManager().getMainScoreboard());
        loader.queueDependency(TeamMembershipHandler.class,
                () -> new TeamMembershipHandler(loader.requireInstance(Scoreboard.class)));
        loader.queueDependency(TeamManager.class, () -> new TeamManager(
                loader.requireInstance(SpawnConfigHandler.class),
                loader.requireInstance(TeamSpawnStore.class),
                loader.requireInstance(TeamReturnStore.class),
                loader.requireInstance(LobbySpawnStore.class),
                loader.requireInstance(TeamMembershipHandler.class)));

        loader.queueDependency(CooldownLineProviderHandler.class, CooldownLineProviderHandler::new);
        loader.queueDependency(ScoreBoardManager.class,
                () -> new ScoreBoardManager(loader.requireInstance(TeamManager.class)));
        loader.queueDependency(BossBarManager.class, () -> new BossBarManager(plugin));
        loader.queueDependency(DebugFeed.class, DebugFeed::new);
        loader.queueDependency(BuildToggleUtil.class, BuildToggleUtil::new);
        loader.queueDependency(CTFPlayerMetaData.class,
                () -> new CTFPlayerMetaData(loader.requireInstance(TeamManager.class)));
        loader.queueDependency(BukkitMessageUtil.class, BukkitMessageUtil::new);
        loader.queueDependency(GameStateManager.class, GameStateManager::new);
        loader.queueDependency(GameLoopTimer.class, GameLoopTimer::new);
        loader.queueDependency(PlayerManager.class, () -> new PlayerManager(
                loader.requireInstance(TeamManager.class),
                loader.requireInstance(KitSelectionHandler.class),
                loader.requireInstance(KitSelectorGUI.class),
                loader.requireInstance(ScoreBoardManager.class)));
        loader.queueDependency(WinnerCelebration.class, () -> new WinnerCelebration(
                loader.requireInstance(TeamManager.class),
                () -> loader.requireInstance(GameStateManager.class).isCleanupInProgress()));
        loader.queueDependency(MatchFlowHandler.class, MatchFlowHandler::new);
        loader.queueDependency(MatchCleanupHandler.class, MatchCleanupHandler::new);
        loader.queueDependency(MatchPlayerSessionHandler.class, MatchPlayerSessionHandler::new);

        loader.queueDependency(PlayerRespawnScheduler.class, PlayerRespawnScheduler::new);
        loader.queueDependency(PlayerDeathEffects.class, PlayerDeathEffects::new);
        loader.queueDependency(PlayerDeathHandler.class, PlayerDeathHandler::new);
        loader.queueDependency(PlayerDeathEventHandler.class,
                () -> loader.requireInstance(PlayerDeathHandler.class));
        loader.queueDependency(IPlayerDeathEventHandler.class,
                () -> loader.requireInstance(PlayerDeathHandler.class));
        loader.queueDependency(PlayerRespawnHandler.class, PlayerRespawnHandler::new);
        loader.queueDependency(PlayerRespawnEventHandler.class,
                () -> loader.requireInstance(PlayerRespawnHandler.class));
        loader.queueDependency(IPlayerRespawnEventHandler.class,
                () -> loader.requireInstance(PlayerRespawnHandler.class));
        loader.queueDependency(PlayerQuitHandler.class, PlayerQuitHandler::new);
        loader.queueDependency(PlayerQuitEventHandler.class,
                () -> loader.requireInstance(PlayerQuitHandler.class));
        loader.queueDependency(IPlayerQuitEventHandler.class,
                () -> loader.requireInstance(PlayerQuitHandler.class));
        loader.queueDependency(PlayerBuildRestrictionHandler.class, PlayerBuildRestrictionHandler::new);
        loader.queueDependency(PlayerBuildGuardEventHandler.class,
                () -> loader.requireInstance(PlayerBuildRestrictionHandler.class));
        loader.queueDependency(IPlayerBuildGuardEventHandler.class,
                () -> loader.requireInstance(PlayerBuildRestrictionHandler.class));
        loader.queueDependency(PlayerItemPickupHandler.class, PlayerItemPickupHandler::new);
        loader.queueDependency(PlayerItemPickupEventHandler.class,
                () -> loader.requireInstance(PlayerItemPickupHandler.class));
        loader.queueDependency(IPlayerItemPickupEventHandler.class,
                () -> loader.requireInstance(PlayerItemPickupHandler.class));
        loader.queueDependency(TeamChatRenderer.class, TeamChatRenderer::new);
        loader.queueDependency(PlayerChatHandler.class, PlayerChatHandler::new);
        loader.queueDependency(PlayerChatEventHandler.class,
                () -> loader.requireInstance(PlayerChatHandler.class));
        loader.queueDependency(IPlayerChatEventHandler.class,
                () -> loader.requireInstance(PlayerChatHandler.class));
    }
}
