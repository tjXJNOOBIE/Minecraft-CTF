package dev.tjxjnoobie.ctf;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.combat.HomingSpearAbility;
import dev.tjxjnoobie.ctf.combat.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.commands.admin.CTFCanBuild;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetFlag;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetGameTime;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetLobby;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetReturn;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetScore;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetScoreLimit;
import dev.tjxjnoobie.ctf.commands.admin.CTFSetSpawn;
import dev.tjxjnoobie.ctf.commands.admin.CTFStart;
import dev.tjxjnoobie.ctf.commands.admin.CTFStop;
import dev.tjxjnoobie.ctf.commands.player.CTF;
import dev.tjxjnoobie.ctf.commands.player.CTFDebug;
import dev.tjxjnoobie.ctf.commands.player.CTFJoin;
import dev.tjxjnoobie.ctf.commands.player.CTFLeave;
import dev.tjxjnoobie.ctf.commands.player.CTFScore;
import dev.tjxjnoobie.ctf.config.CTFConfig;
import dev.tjxjnoobie.ctf.config.message.MessageConfig;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageService;
import dev.tjxjnoobie.ctf.events.flag.CTFFlagBreakEvent;
import dev.tjxjnoobie.ctf.events.flag.CTFFlagCarrierLockEvent;
import dev.tjxjnoobie.ctf.events.flag.CTFFlagInteractEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerBuildEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerChatEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerDeathEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerJoinEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerMoveEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerQuitEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerRespawnEvent;
import dev.tjxjnoobie.ctf.events.player.CTFPlayerTeleportEvent;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.game.flag.BaseMarkerService;
import dev.tjxjnoobie.ctf.game.player.managers.BuildBypassManager;
import dev.tjxjnoobie.ctf.kit.KitManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private static final String LOG_PREFIX = "[CTF] [Main] ";

    private TeamManager teamManager;
    private ScoreBoardManager scoreBoardManager;
    private KitManager kitManager;
    private BossBarManager bossBarManager;
    private HomingSpearAbility homingSpearAbility;
    private ScoutTaggerAbility scoutTaggerAbility;
    private CtfMatchOrchestrator gameManager;
    private DebugFeed debugFeedManager;
    private BuildBypassManager buildBypassManager;
    private BaseMarkerService markerManager;
    private CTFConfig config;
    private MessageService messageService;

    @Override
    public void onEnable() {
        registerConfig();
        registerMessages();
        registerCore();
        gameManager.getFlagManager().resetFlagIndicators();
        registerCommands();
        registerEvents();
        Bukkit.getLogger().info(LOG_PREFIX + "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (homingSpearAbility != null) {
            homingSpearAbility.clearAll();
        }
        if (buildBypassManager != null) {
            buildBypassManager.clear();
        }
        Bukkit.getLogger().info(LOG_PREFIX + "Plugin disabled.");
    }

    private void registerConfig() {
        config = new CTFConfig(this);
        config.loadConfig();
    }

    private void registerMessages() {
        MessageConfig messageConfig = new MessageConfig(this);
        messageConfig.loadMessageConfig();
        this.messageService = messageConfig;
        getServer().getServicesManager().register(MessageService.class, messageConfig, this, ServicePriority.Normal);
    }

    private void registerCore() {
        teamManager = new TeamManager(this);
        teamManager.registerDefaultTeams();
        scoreBoardManager = new ScoreBoardManager(teamManager);
        kitManager = new KitManager();
        bossBarManager = new BossBarManager(this);
        debugFeedManager = new DebugFeed();
        buildBypassManager = new BuildBypassManager();
        markerManager = new BaseMarkerService(this);
        gameManager = new CtfMatchOrchestrator(this, teamManager, scoreBoardManager, kitManager, bossBarManager, debugFeedManager);
        kitManager.setMatchRunningSupplier(gameManager::isRunning);
        scoreBoardManager.setStatsProvider(gameManager::getPlayerStats);
        homingSpearAbility = new HomingSpearAbility(this, teamManager, gameManager, gameManager.getFlagManager(), kitManager);
        gameManager.setHomingSpearAbility(homingSpearAbility);
        scoutTaggerAbility = new ScoutTaggerAbility(this, teamManager, gameManager, gameManager.getFlagManager(), kitManager);
        gameManager.setScoutTaggerAbility(scoutTaggerAbility);
        gameManager.setBaseMarkerService(markerManager);
    }

    private void registerCommands() {
        PluginCommand ctfCommand = getCommand("ctf");
        if (ctfCommand == null) {
            return;
        }

        CTFJoin join = new CTFJoin(teamManager, gameManager);
        CTFLeave leave = new CTFLeave(gameManager, teamManager);
        CTFSetFlag setFlag = new CTFSetFlag(gameManager.getFlagManager(), teamManager, markerManager);
        CTFSetLobby setLobby = new CTFSetLobby(gameManager);
        CTFSetSpawn setSpawn = new CTFSetSpawn(gameManager, teamManager);
        CTFSetReturn setReturn = new CTFSetReturn(gameManager, teamManager);
        CTFSetGameTime setGameTime = new CTFSetGameTime(gameManager);
        CTFSetScore setScore = new CTFSetScore(gameManager, teamManager);
        CTFSetScoreLimit setScoreLimit = new CTFSetScoreLimit(gameManager, scoreBoardManager);
        CTFCanBuild canBuild = new CTFCanBuild(buildBypassManager);
        CTFStart start = new CTFStart(gameManager);
        CTFStop stop = new CTFStop(gameManager);
        CTFScore score = new CTFScore(teamManager, gameManager, scoreBoardManager);
        CTFDebug debug = new CTFDebug(debugFeedManager);

        CTF ctf = new CTF(join, leave, setFlag, setLobby, setSpawn, setReturn, setGameTime,
            setScore, setScoreLimit, canBuild, start, stop, score, debug);
        ctfCommand.setExecutor(ctf);
        ctfCommand.setTabCompleter(ctf);
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new CTFFlagInteractEvent(gameManager, gameManager.getFlagManager()), this);
        getServer().getPluginManager().registerEvents(new CTFFlagBreakEvent(gameManager), this);
        getServer().getPluginManager().registerEvents(new CTFFlagCarrierLockEvent(gameManager.getFlagManager()), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerDeathEvent(gameManager, gameManager.getFlagManager(), bossBarManager, homingSpearAbility, this), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerJoinEvent(gameManager), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerRespawnEvent(gameManager, kitManager, scoutTaggerAbility), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerQuitEvent(gameManager, buildBypassManager), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerMoveEvent(gameManager, gameManager.getFlagManager()), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerTeleportEvent(gameManager, gameManager.getFlagManager()), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerBuildEvent(gameManager, buildBypassManager), this);
        getServer().getPluginManager().registerEvents(new CTFPlayerChatEvent(teamManager), this);
        getServer().getPluginManager().registerEvents(kitManager.getSelectorListener(), this);
        getServer().getPluginManager().registerEvents(homingSpearAbility, this);
        getServer().getPluginManager().registerEvents(scoutTaggerAbility, this);
    }
}




