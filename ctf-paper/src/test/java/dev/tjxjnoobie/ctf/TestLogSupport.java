package dev.tjxjnoobie.ctf;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageService;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

/**
 * Sets up lightweight Bukkit stubs for unit tests.
 */
public abstract class TestLogSupport {
    protected static final MessageService MESSAGE_SERVICE = new MessageService() {
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

            Mockito.when(server.getLogger()).thenReturn(logger);
            Mockito.when(server.getServicesManager()).thenReturn(servicesManager);
            Mockito.when(server.getScheduler()).thenReturn(scheduler);
            Mockito.when(server.getScoreboardManager()).thenReturn(scoreboardManager);
            Mockito.when(servicesManager.load(MessageService.class)).thenReturn(MESSAGE_SERVICE);

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

            Mockito.when(scheduler.runTask(Mockito.any(), Mockito.any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return Mockito.mock(BukkitTask.class);
            });

            setBukkitServer(server);
        } catch (Exception ex) {
            Assertions.fail("Failed to register Bukkit test stubs: " + ex.getMessage());
        }
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

