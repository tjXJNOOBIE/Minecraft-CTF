package dev.tjxjnoobie.ctf.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagScoreHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchFlowHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class PluginBootstrapTest extends TestLogSupport {
    @Test
    void bootstrapOnEnableRegistersServicesAndStartsLobbyWaitingTimer() throws Exception {
        Main plugin = Mockito.mock(Main.class);

        File dataDir = Files.createTempDirectory("ctf-bootstrap-test").toFile();
        PluginManager pluginManager = Mockito.mock(PluginManager.class);
        World world = Mockito.mock(World.class);
        Block block = Mockito.mock(Block.class);
        Location spawnLocation = new Location(world, 0.5, 64.0, 0.5);
        Files.writeString(dataDir.toPath().resolve("ctf-spawns.yml"), "capture-zone:\n  radius: 3.0\n");
        Files.writeString(dataDir.toPath().resolve("flag-locations.yml"), "flags: {}\n");

        when(plugin.getDataFolder()).thenReturn(dataDir);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getCommand("ctf")).thenReturn(null);
        when(plugin.getName()).thenReturn("ctf-test");
        when(plugin.getServer()).thenReturn(Bukkit.getServer());
        when(Bukkit.getServer().getPluginManager()).thenReturn(pluginManager);
        when(world.getName()).thenReturn("CTFMap");
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(world));
        when(Bukkit.getServer().getWorld(Mockito.anyString())).thenReturn(world);
        when(Bukkit.getServer().createWorld(Mockito.any(org.bukkit.WorldCreator.class))).thenReturn(world);
        when(world.getSpawnLocation()).thenReturn(spawnLocation);
        when(world.getHighestBlockYAt(Mockito.anyInt(), Mockito.anyInt())).thenReturn(64);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(block);
        when(plugin.getResource(Mockito.anyString())).thenReturn((InputStream) null);

        PluginBootstrap bootstrap = new PluginBootstrap(plugin);
        bootstrap.onEnable();

        assertSame(bootstrap, PluginBootstrap.getActivePluginBootstrap());
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(TeamManager.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(MatchFlowHandler.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(MatchCleanupHandler.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(FlagScoreHandler.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(FlagCarrierHandler.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(FlagBaseSetupHandler.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(FlagLifecycleHandler.class));
        assertTrue(bootstrap.getDependencyLoader().isInstanceRegistered(FlagCarrierStateHandler.class));

        MatchCleanupHandler cleanupService = bootstrap.getDependencyLoader().requireInstance(MatchCleanupHandler.class);
        Field lobbyTaskField = MatchCleanupHandler.class.getDeclaredField("lobbyWaitingTimerTask");
        lobbyTaskField.setAccessible(true);
        assertNotNull(lobbyTaskField.get(cleanupService));

        bootstrap.onDisable();

        assertNull(PluginBootstrap.getActivePluginBootstrap());
        assertFalse(bootstrap.getDependencyLoader().isInstanceRegistered(MatchCleanupHandler.class));
        assertFalse(bootstrap.getDependencyLoader().isInstanceRegistered(TeamManager.class));
    }

    @Test
    void mainOnDisableDelegatesToLifecycleAndClearsLifecycleField() throws Exception {
        Main main = Mockito.mock(Main.class, Mockito.CALLS_REAL_METHODS);
        PluginLifecycle lifecycle = Mockito.mock(PluginLifecycle.class);

        Field lifecycleField = Main.class.getDeclaredField("lifecycle");
        lifecycleField.setAccessible(true);
        lifecycleField.set(main, lifecycle);

        main.onDisable();

        verify(lifecycle).onDisable();
        assertNull(lifecycleField.get(main));
    }

    @Test
    void mainOnDisableWithoutLifecycleIsSafe() throws Exception {
        Main main = Mockito.mock(Main.class, Mockito.CALLS_REAL_METHODS);
        Field lifecycleField = Main.class.getDeclaredField("lifecycle");
        lifecycleField.setAccessible(true);

        assertDoesNotThrow(main::onDisable);
        assertNull(lifecycleField.get(main));
    }

    @Test
    void bootstrapCopiesBundledSpawnAndFlagDefaultsWhenFilesAreMissing() throws Exception {
        Main plugin = Mockito.mock(Main.class);

        File dataDir = Files.createTempDirectory("ctf-bootstrap-defaults").toFile();
        PluginManager pluginManager = Mockito.mock(PluginManager.class);
        World world = Mockito.mock(World.class);
        Block block = Mockito.mock(Block.class);
        Location spawnLocation = new Location(world, 0.5, 64.0, 0.5);
        Files.writeString(dataDir.toPath().resolve("messages.yml"), "prefix: \"[test] \"\n", StandardCharsets.UTF_8);

        when(plugin.getDataFolder()).thenReturn(dataDir);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getCommand("ctf")).thenReturn(null);
        when(plugin.getName()).thenReturn("ctf-test");
        when(plugin.getServer()).thenReturn(Bukkit.getServer());
        when(plugin.getResource(Mockito.anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0, String.class);
            return getClass().getClassLoader().getResourceAsStream(name);
        });
        when(Bukkit.getServer().getPluginManager()).thenReturn(pluginManager);
        when(world.getName()).thenReturn("CTFMap");
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(world));
        when(Bukkit.getServer().getWorld(Mockito.anyString())).thenReturn(world);
        when(Bukkit.getServer().createWorld(Mockito.any(org.bukkit.WorldCreator.class))).thenReturn(world);
        when(world.getSpawnLocation()).thenReturn(spawnLocation);
        when(world.getHighestBlockYAt(Mockito.anyInt(), Mockito.anyInt())).thenReturn(64);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(block);
        when(world.getBlockAt(Mockito.any(Location.class))).thenReturn(block);
        Mockito.doAnswer(invocation -> {
            ItemDisplay display = Mockito.mock(ItemDisplay.class);
            when(display.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
            return display;
        }).when(world).spawn(Mockito.any(Location.class), Mockito.eq(ItemDisplay.class));

        try (MockedConstruction<dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem> ignored =
                 Mockito.mockConstruction(dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem.class,
                     (mocked, context) -> when(mocked.create()).thenReturn(Mockito.mock(org.bukkit.inventory.ItemStack.class)))) {
            PluginBootstrap bootstrap = new PluginBootstrap(plugin);
            bootstrap.onEnable();

            YamlConfiguration writtenSpawns = YamlConfiguration.loadConfiguration(
                dataDir.toPath().resolve("ctf-spawns.yml").toFile());
            YamlConfiguration writtenFlags = YamlConfiguration.loadConfiguration(
                dataDir.toPath().resolve("flag-locations.yml").toFile());
            YamlConfiguration expectedSpawns;
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream("ctf-spawns.yml")) {
                assertNotNull(stream);
                expectedSpawns = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
            }
            YamlConfiguration expectedFlags;
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream("flag-locations.yml")) {
                assertNotNull(stream);
                expectedFlags = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
            }

            assertEquals(expectedSpawns.getDouble("team-spawns.red.x"), writtenSpawns.getDouble("team-spawns.red.x"));
            assertEquals(expectedSpawns.getDouble("team-spawns.blue.x"), writtenSpawns.getDouble("team-spawns.blue.x"));
            assertEquals(expectedSpawns.getDouble("capture-zone.radius"), writtenSpawns.getDouble("capture-zone.radius"));
            assertEquals(expectedFlags.getDouble("flags.red.base.y"), writtenFlags.getDouble("flags.red.base.y"));
            assertEquals(expectedFlags.getDouble("flags.blue.base.y"), writtenFlags.getDouble("flags.blue.base.y"));

            bootstrap.onDisable();
        }
    }
}
