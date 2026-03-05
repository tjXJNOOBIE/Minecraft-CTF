package dev.tjxjnoobie.ctf.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.bootstrap.registries.CombatBootstrapRegistry;
import dev.tjxjnoobie.ctf.bootstrap.registries.FlagBootstrapRegistry;
import dev.tjxjnoobie.ctf.bootstrap.registries.GameBootstrapRegistry;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.support.ServiceLoaderTestSupport;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BootstrapRegistryIsolationTest extends ServiceLoaderTestSupport {
    @BeforeAll
    static void installBukkit() {
        installBukkitServer();
    }

    @BeforeEach
    void setUp() throws Exception {
        resetFallbackLoader();
        clearStaticPlugin();
    }

    @Test
    void registriesLoadCrossDomainDependenciesInDeclaredOrder() throws Exception {
        Main plugin = Mockito.mock(Main.class);
        File dataDir = Files.createTempDirectory("ctf-registry-test").toFile();

        when(plugin.getDataFolder()).thenReturn(dataDir);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getServer()).thenReturn(Bukkit.getServer());
        when(plugin.getName()).thenReturn("ctf-test");
        when(plugin.getResource(Mockito.anyString())).thenReturn((InputStream) null);

        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();
        new GameBootstrapRegistry().register(loader, plugin);
        new FlagBootstrapRegistry().register(loader, plugin);
        new CombatBootstrapRegistry().register(loader, plugin);

        loader.loadQueuedDependenciesInOrder();

        assertTrue(loader.isInstanceRegistered(TeamManager.class));
        assertTrue(loader.isInstanceRegistered(MatchCleanupHandler.class));
        assertTrue(loader.isInstanceRegistered(FlagCarrierHandler.class));
        assertTrue(loader.isInstanceRegistered(HomingSpearAbilityCooldown.class));
        assertTrue(loader.isInstanceRegistered(HomingSpearAbilityHandler.class));
        assertTrue(loader.isInstanceRegistered(ScoutTaggerAbility.class));
    }
}
