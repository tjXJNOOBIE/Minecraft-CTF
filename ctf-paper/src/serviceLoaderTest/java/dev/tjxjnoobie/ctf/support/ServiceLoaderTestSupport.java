package dev.tjxjnoobie.ctf.support;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
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

public abstract class ServiceLoaderTestSupport {
    private final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    protected static void installBukkitServer() {
        try {
            Logger logger = Logger.getLogger("CTF-ServiceLoaderTest");
            Server server = Mockito.mock(Server.class);
            BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
            ScoreboardManager scoreboardManager = Mockito.mock(ScoreboardManager.class);
            Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
            Team redTeam = Mockito.mock(Team.class);
            Team blueTeam = Mockito.mock(Team.class);
            Set<String> redEntries = new HashSet<>();
            Set<String> blueEntries = new HashSet<>();
            BukkitTask task = Mockito.mock(BukkitTask.class);

            Mockito.when(server.getLogger()).thenReturn(logger);
            Mockito.when(server.getScheduler()).thenReturn(scheduler);
            Mockito.when(server.getScoreboardManager()).thenReturn(scoreboardManager);
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
            Mockito.doAnswer(invocation -> redEntries.add(invocation.getArgument(0))).when(redTeam).addEntry(Mockito.anyString());
            Mockito.doAnswer(invocation -> blueEntries.add(invocation.getArgument(0))).when(blueTeam).addEntry(Mockito.anyString());
            Mockito.doAnswer(invocation -> redEntries.remove(invocation.getArgument(0))).when(redTeam).removeEntry(Mockito.anyString());
            Mockito.doAnswer(invocation -> blueEntries.remove(invocation.getArgument(0))).when(blueTeam).removeEntry(Mockito.anyString());
            Mockito.when(scheduler.runTask(Mockito.any(), Mockito.any(Runnable.class))).thenReturn(task);
            Mockito.when(scheduler.runTaskLater(Mockito.any(), Mockito.any(Runnable.class), Mockito.anyLong())).thenReturn(task);
            Mockito.when(scheduler.runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong())).thenReturn(task);
            Mockito.when(scheduler.runTaskTimerAsynchronously(Mockito.any(), Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong())).thenReturn(task);

            setBukkitServer(server);
        } catch (Exception ex) {
            Assertions.fail("Failed to configure Bukkit service-loader test stubs: " + ex.getMessage());
        }
    }

    protected void resetFallbackLoader() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
    }

    @BeforeEach
    void logTestStart(TestInfo testInfo) {
        String displayName = testInfo == null ? getClass().getSimpleName() : testInfo.getDisplayName();
        currentTestName.set(displayName);
        logStep("BEGIN");
    }

    @AfterEach
    void logTestEnd() {
        logStep("END");
        currentTestName.remove();
    }

    protected void setStaticPlugin(Main plugin) throws Exception {
        // No-op: Main no longer exposes a static plugin singleton.
    }

    protected void clearStaticPlugin() throws Exception {
        // No-op: Main no longer exposes a static plugin singleton.
    }

    private static void setBukkitServer(Server server) throws Exception {
        try {
            Bukkit.setServer(server);
            return;
        } catch (Throwable ignored) {
            // Fall through to reflection.
        }

        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);
    }

    protected void logStep(String message) {
        String testName = currentTestName.get();
        String resolvedTestName = testName == null ? getClass().getSimpleName() : testName;
        Bukkit.getLogger().info("[TEST] [" + getClass().getSimpleName() + "] [" + resolvedTestName + "] " + message);
    }
}
