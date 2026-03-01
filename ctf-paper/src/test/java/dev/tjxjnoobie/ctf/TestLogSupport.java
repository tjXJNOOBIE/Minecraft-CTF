package dev.tjxjnoobie.ctf;

import dev.tjxjnoobie.ctf.bootstrap.PluginBootstrap;
import dev.tjxjnoobie.ctf.config.message.MessageConfigHandler;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageHandler;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mockito;

/**
 * Sets up lightweight Bukkit stubs for unit tests.
 */
public abstract class TestLogSupport {
    private final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    protected static final MessageHandler MESSAGE_SERVICE = new MessageHandler() {
        @Override
        public void loadMessageConfig() {
        }

        @Override
        public void reloadMessageConfig() {
        }

        @Override
        public Component getMessage(String key) {
            return Component.text(key == null ? "" : key);
        }

        @Override
        public Component getMessage(String key, Map<String, String> placeholders) {
            return Component.text(key == null ? "" : key);
        }

        @Override
        public Component getMessageFormatted(String key, Object... args) {
            return Component.text(key == null ? "" : key);
        }

        @Override
        public boolean isMessageConfigActive() {
            return true;
        }
    };

    static {
        try {
            Logger logger = Logger.getLogger("CTF-Test");
            Server server = Mockito.mock(Server.class);
            ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
            BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
            ScoreboardManager scoreboardManager = Mockito.mock(ScoreboardManager.class);
            Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
            Team redTeam = Mockito.mock(Team.class);
            Team blueTeam = Mockito.mock(Team.class);
            Set<String> redEntries = new HashSet<>();
            Set<String> blueEntries = new HashSet<>();
            BukkitTask task = Mockito.mock(BukkitTask.class);
            UnsafeValues unsafeValues = Mockito.mock(UnsafeValues.class);

            Mockito.when(server.getLogger()).thenReturn(logger);
            Mockito.when(server.getServicesManager()).thenReturn(servicesManager);
            Mockito.when(server.getScheduler()).thenReturn(scheduler);
            Mockito.when(server.getScoreboardManager()).thenReturn(scoreboardManager);
            Mockito.when(server.getWorldContainer()).thenReturn(new java.io.File("."));
            Mockito.when(server.getUnsafe()).thenReturn(unsafeValues);
            Mockito.when(unsafeValues.getMainLevelName()).thenReturn("world");
            Mockito.when(servicesManager.load(MessageHandler.class)).thenReturn(MESSAGE_SERVICE);

            Mockito.when(scoreboardManager.getMainScoreboard()).thenReturn(scoreboard);
            Mockito.when(scoreboardManager.getNewScoreboard()).thenReturn(Mockito.mock(Scoreboard.class));
            Mockito.when(scoreboard.getTeam("ctf_red")).thenReturn(redTeam);
            Mockito.when(scoreboard.getTeam("ctf_blue")).thenReturn(blueTeam);
            Mockito.when(scoreboard.registerNewTeam("ctf_red")).thenReturn(redTeam);
            Mockito.when(scoreboard.registerNewTeam("ctf_blue")).thenReturn(blueTeam);

            Mockito.when(redTeam.getEntries()).thenReturn(redEntries);
            Mockito.when(blueTeam.getEntries()).thenReturn(blueEntries);
            Mockito.when(redTeam.hasEntry(Mockito.anyString())).thenAnswer(invocation -> redEntries.contains(invocation.getArgument(0)));
            Mockito.when(blueTeam.hasEntry(Mockito.anyString())).thenAnswer(invocation -> blueEntries.contains(invocation.getArgument(0)));

            Mockito.doAnswer(invocation -> redEntries.add(invocation.getArgument(0)))
                .when(redTeam).addEntry(Mockito.anyString());
            Mockito.doAnswer(invocation -> blueEntries.add(invocation.getArgument(0)))
                .when(blueTeam).addEntry(Mockito.anyString());

            Mockito.doAnswer(invocation -> redEntries.remove(invocation.getArgument(0)))
                .when(redTeam).removeEntry(Mockito.anyString());
            Mockito.doAnswer(invocation -> blueEntries.remove(invocation.getArgument(0)))
                .when(blueTeam).removeEntry(Mockito.anyString());

            Mockito.when(scheduler.runTask(Mockito.any(), Mockito.any(Runnable.class))).thenReturn(task);
            Mockito.when(scheduler.runTaskLater(Mockito.any(), Mockito.any(Runnable.class), Mockito.anyLong())).thenReturn(task);
            Mockito.when(scheduler.runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong())).thenReturn(task);

            setBukkitServer(server);
            DependencyLoader.getFallbackDependencyLoader().resetInstances();
        } catch (Exception ex) {
            Assertions.fail("Failed to register Bukkit test stubs: " + ex.getMessage());
        }
    }

    @BeforeEach
    void resetTestState(TestInfo testInfo) {
        try {
            String displayName = testInfo == null ? getClass().getSimpleName() : testInfo.getDisplayName();
            currentTestName.set(displayName);
            logStep("BEGIN");
            DependencyLoader.getFallbackDependencyLoader().resetInstances();
            clearActivePluginBootstrap();
            registerDependency(IInventoryUtils.class, new InventoryUtils());
        } catch (Exception ex) {
            Assertions.fail("Failed to reset test state: " + ex.getMessage());
        }
    }

    @AfterEach
    void logTestEnd() {
        logStep("END");
        currentTestName.remove();
    }

    protected void registerMessageAndSender() {
        registerMessageHandler();
        registerDependency(BukkitMessageUtil.class, Mockito.mock(BukkitMessageUtil.class));
    }

    protected void registerMessageHandler() {
        MessageConfigHandler messageConfigHandler = Mockito.mock(MessageConfigHandler.class);
        Mockito.when(messageConfigHandler.getMessage(Mockito.anyString()))
            .thenAnswer(invocation -> Component.text(invocation.getArgument(0, String.class)));
        Mockito.when(messageConfigHandler.getMessage(Mockito.anyString(), Mockito.anyMap()))
            .thenAnswer(invocation -> Component.text(invocation.getArgument(0, String.class)));
        Mockito.when(messageConfigHandler.getMessageFormatted(Mockito.anyString(), Mockito.any(Object[].class)))
            .thenAnswer(invocation -> Component.text(invocation.getArgument(0, String.class)));

        registerDependency(MessageConfigHandler.class, messageConfigHandler);
    }

    protected void registerDependencies(Object... typeAndInstance) {
        if (typeAndInstance == null) {
            return;
        }
        for (int i = 0; i + 1 < typeAndInstance.length; i += 2) {
            registerDependency((Class<?>) typeAndInstance[i], typeAndInstance[i + 1]);
        }
    }

    protected void registerDependency(Class<?> type, Object instance) {
        if (type == null || instance == null) {
            return;
        }
        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();
        registerTypeHierarchy(loader, type, instance, new HashSet<>());
    }

    protected void logStep(String message) {
        String testName = currentTestName.get();
        String resolvedTestName = testName == null ? getClass().getSimpleName() : testName;
        Bukkit.getLogger().info("[TEST] [" + getClass().getSimpleName() + "] [" + resolvedTestName + "] " + message);
    }

    protected void logValue(String label, Object value) {
        logStep(label + "=" + value);
    }

    private void registerTypeHierarchy(DependencyLoader loader, Class<?> type, Object instance, Set<Class<?>> visited) {
        if (type == null || type == Object.class || !visited.add(type)) {
            return;
        }
        if (type.isAssignableFrom(instance.getClass()) && !loader.isInstanceRegistered(type)) {
            loader.registerImportantInstance(type, instance);
        }
        for (Class<?> iface : type.getInterfaces()) {
            registerTypeHierarchy(loader, iface, instance, visited);
        }
        registerTypeHierarchy(loader, type.getSuperclass(), instance, visited);
    }

    private static void clearActivePluginBootstrap() throws Exception {
        Field field = PluginBootstrap.class.getDeclaredField("activePluginBootstrap");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static void setBukkitServer(Server server) throws Exception {
        try {
            Bukkit.setServer(server);
            return;
        } catch (Throwable ignored) {
            // fall through to reflection
        }

        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);
    }

}

