package dev.tjxjnoobie.ctf.bootstrap;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.bootstrap.registries.BootstrapRegistry;
import dev.tjxjnoobie.ctf.bootstrap.registries.CombatBootstrapRegistry;
import dev.tjxjnoobie.ctf.bootstrap.registries.GameBootstrapRegistry;
import dev.tjxjnoobie.ctf.bootstrap.registries.FlagBootstrapRegistry;
import dev.tjxjnoobie.ctf.commands.player.CTF;
import dev.tjxjnoobie.ctf.config.CTFConfig;
import dev.tjxjnoobie.ctf.config.message.MessageConfig;
import dev.tjxjnoobie.ctf.dependency.interfaces.DependencyAccess;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.entity.CTFEntityDamageEvent;
import dev.tjxjnoobie.ctf.events.entity.CTFEntityDamageByEntityEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryClickEvent;
import dev.tjxjnoobie.ctf.events.inventory.CTFInventoryDragEvent;
import dev.tjxjnoobie.ctf.events.player.CTFAsyncChatEvent;
import dev.tjxjnoobie.ctf.events.player.CTFBlockBreakEvent;
import dev.tjxjnoobie.ctf.events.player.CTFBlockPlaceEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerAttemptPickupItemEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDeathEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDropItemEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerInteractEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerItemHeldEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerJoinEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerMoveEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerRespawnEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerTeleportEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerQuitEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerSwapHandItemsEvent;
import dev.tjxjnoobie.ctf.events.projectile.CTFProjectileHitEvent;
import dev.tjxjnoobie.ctf.events.projectile.CTFProjectileLaunchEvent;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.tasks.CTFTaskRegistry;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.BaseMarkerHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.SpawnConfigHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

/**
 * Boots the Paper plugin by wiring dependencies and registering the command and event entry points.
 */
public final class PluginBootstrap implements DependencyAccess, PluginLifecycle {

    // == Constants ==
    private static final String LOG_PREFIX = "[PluginBootstrap] ";
    private static final String WORLD_NAME = "CTFMap";

    // == Dependency registry order ==
    private static final BootstrapRegistry[] DEPENDENCY_REGISTRIES = new BootstrapRegistry[] {
            new GameBootstrapRegistry(),
            new FlagBootstrapRegistry(),
            new CombatBootstrapRegistry()
    };

    // == Runtime state ==
    private static PluginBootstrap activePluginBootstrap;
    private final Main plugin;
    private final DependencyLoader dependencyLoader = new DependencyLoader();

    // == Lifecycle ==
    /**
     * Constructs a PluginBootstrap instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public PluginBootstrap(Main plugin) {
        // Bind plugin reference and mark this bootstrap as active.
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        activePluginBootstrap = this;
    }

    /**
     * Initializes runtime dependencies and registers Bukkit entry points.
     */
    @Override
    public void onEnable() {
        activePluginBootstrap = this; // Reset and register dependencies before any listeners run.
        getDependencyLoader().resetInstances();

        // Register dependencies in strict order.
        queueDependenciesInOrder();
        getDependencyLoader().loadQueuedDependenciesInOrder();
        runPostRegistrationHooks();

        // Register commands and listeners last.
        registerCommands();
        registerEventListeners();

        Bukkit.getLogger().info(LOG_PREFIX + "Plugin enabled.");
    }

    /**
     * Shuts down match runtime state and clears the dependency container.
     */
    @Override
    public void onDisable() {
        // Shutdown runtime services before dependency reset.
        runShutdown();
        getDependencyLoader().requireInstance(CTFTaskRegistry.class).cancelAll();
        getDependencyLoader().resetInstances();
        if (activePluginBootstrap == this) {
            activePluginBootstrap = null;
        }
        Bukkit.getLogger().info(LOG_PREFIX + "Plugin disabled.");
    }

    private void registerCommands() {
        PluginCommand ctfCommand = plugin.getCommand("ctf");
        // Some test/bootstrap flows intentionally run without the command being declared.
        if (ctfCommand == null) {
            return;
        }
        CTF ctf = new CTF(); // Bind command executor and tab completer.
        ctfCommand.setExecutor(ctf);
        ctfCommand.setTabCompleter(ctf);
    }

    private void registerEventListeners() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        // Register gameplay listeners by domain.
        registerGameEventListeners(pluginManager);
        registerCombatEventListeners(pluginManager);
        registerKitEventListeners(pluginManager);
    }

    private void registerGameEventListeners(PluginManager pluginManager) {
        // Core gameplay and player lifecycle events.
        registerEvent(pluginManager, new CTFPlayerDeathEvent());
        registerEvent(pluginManager, new CTFPlayerJoinEvent());
        registerEvent(pluginManager, new CTFPlayerQuitEvent());
        registerEvent(pluginManager, new CTFPlayerRespawnEvent());
        registerEvent(pluginManager, new CTFPlayerMoveEvent());
        registerEvent(pluginManager, new CTFPlayerTeleportEvent());
        registerEvent(pluginManager, new CTFBlockPlaceEvent());
        registerEvent(pluginManager, new CTFBlockBreakEvent());
        registerEvent(pluginManager, new CTFAsyncChatEvent());
        registerEvent(pluginManager, new CTFPlayerInteractEvent());
        registerEvent(pluginManager, new CTFPlayerAttemptPickupItemEvent());
        registerEvent(pluginManager, new CTFPlayerDropItemEvent());
        registerEvent(pluginManager, new CTFPlayerItemHeldEvent());
        registerEvent(pluginManager, new CTFInventoryClickEvent());
        registerEvent(pluginManager, new CTFInventoryDragEvent());
    }

    private void registerCombatEventListeners(PluginManager pluginManager) {
        // Combat-specific events.
        registerEvent(pluginManager, new CTFEntityDamageEvent());
        registerEvent(pluginManager, new CTFEntityDamageByEntityEvent());
        registerEvent(pluginManager, new CTFProjectileHitEvent());
        registerEvent(pluginManager, new CTFProjectileLaunchEvent());
        registerEvent(pluginManager, new CTFPlayerSwapHandItemsEvent());
    }

    private void registerKitEventListeners(PluginManager pluginManager) {
        KitSelectorGUI kitSelectorGui = getKitSelectorGui();
        // GUI handles its own inventory events.
        registerEvent(pluginManager, kitSelectorGui);
    }

    private void registerEvent(PluginManager pluginManager, Listener listener) {
        // Guard: Exit early when required input or runtime state is
        // missing/invalid.
        // Guard: short-circuit when pluginManager == null || listener == null.
        if (pluginManager == null || listener == null) {
            return;
        }
        // Bind listener to this plugin instance.
        pluginManager.registerEvents(listener, plugin);
    }

    // == Getters ==
    public static PluginBootstrap getActivePluginBootstrap() {
        // Provide the active bootstrap for dependency access.
        return activePluginBootstrap;
    }

    public DependencyLoader getDependencyLoader() {
        // Expose loader for testing and registries.
        return dependencyLoader;
    }

    // == Utilities ==
    private void queueDependenciesInOrder() {
        DependencyLoader loader = getDependencyLoader();
        for (BootstrapRegistry registry : DEPENDENCY_REGISTRIES) {
            // Register core dependency bundles in fixed order.
            registry.register(loader, plugin);
        }
    }

    private void runPostRegistrationHooks() {
        // Load configs and initialize runtime stores.
        getDependencyLoader().requireInstance(CTFConfig.class).loadConfig();
        getDependencyLoader().requireInstance(MessageConfig.class).loadMessageConfig();
        Bukkit.createWorld(new WorldCreator(WORLD_NAME));
        getDependencyLoader().requireInstance(SpawnConfigHandler.class).loadSpawnConfig();
        getDependencyLoader().requireInstance(TeamManager.class).registerDefaultTeams();
        getDependencyLoader().requireInstance(FlagConfigHandler.class).loadFlagConfig();
        getDependencyLoader().requireInstance(BaseMarkerHandler.class).initializeFromConfig();
        getDependencyLoader().requireInstance(FlagBaseSetupHandler.class).initializeFlagsFromConfig();
        getDependencyLoader().requireInstance(FlagLifecycleHandler.class).resetFlagIndicators();
        MatchPlayerSessionHandler sessionHandler = getMatchPlayerSessionHandler();
        getDependencyLoader().requireInstance(ScoreBoardManager.class)
                .setStatsProvider(sessionHandler::getPlayerStats);
        getDependencyLoader().requireInstance(MatchCleanupHandler.class).startLobbyWaitingTimer();
    }

    private void runShutdown() {
        MatchCleanupHandler cleanupService = getMatchCleanupHandler(); // Stop match systems and clear temporary state.
        cleanupService.shutdownMatchSystem();

        HomingSpearAbilityCooldown homingSpearCooldown = getHomingSpearAbilityCooldown();
        homingSpearCooldown.clearAllCombatState();

        ScoutTaggerAbility currentScoutTaggerAbility = getScoutTaggerAbility();
        currentScoutTaggerAbility.stopAll();

        BuildToggleUtil buildToggleUtil = getBuildToggleUtil();
        buildToggleUtil.clear();
    }
}
