package dev.tjxjnoobie.ctf.commands.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SimulationOperatorUtilTest extends TestLogSupport {
    private Server server;
    private ConsoleCommandSender console;

    @BeforeEach
    void setUp() {
        server = Bukkit.getServer();
        console = Mockito.mock(ConsoleCommandSender.class);

        when(server.getConsoleSender()).thenReturn(console);
        when(server.dispatchCommand(Mockito.eq(console), Mockito.anyString())).thenReturn(true);
    }

    @Test
    void loadsHeadlessOperatorRosterFromManifest() throws Exception {
        Path repositoryRoot = createRepositoryRootWithOperatorManifests();

        assertEquals(
            List.of(
                "SimDirector",
                "SimRed01",
                "SimRed02",
                "SimBlue01",
                "SimBlue02"),
            SimulationOperatorUtil.listOperatorNames(repositoryRoot, "headless")
        );
    }

    @Test
    void loadsLegacyOperatorManifest() throws Exception {
        Path repositoryRoot = createRepositoryRootWithOperatorManifests();

        assertIterableEquals(
            List.of("RedLeader", "RedOne", "RedTwo", "BlueLeader", "BlueOne", "BlueTwo"),
            SimulationOperatorUtil.listOperatorNames(repositoryRoot, "basic")
        );

        List<String> fullModeOperators = SimulationOperatorUtil.listOperatorNames(repositoryRoot, "full");
        assertEquals(4, fullModeOperators.size());
        assertTrue(fullModeOperators.contains("RedLeader"));
        assertTrue(fullModeOperators.contains("BlueLeader"));
    }

    @Test
    void dispatchesOperatorCommandsThroughServerConsole() throws Exception {
        Path repositoryRoot = createRepositoryRootWithOperatorManifests();

        SimulationOperatorUtil.ensureOperatorAccess(repositoryRoot, "basic");

        verify(server).dispatchCommand(console, "op RedLeader");
        verify(server).dispatchCommand(console, "op RedOne");
        verify(server).dispatchCommand(console, "op RedTwo");
        verify(server).dispatchCommand(console, "op BlueLeader");
        verify(server).dispatchCommand(console, "op BlueOne");
        verify(server).dispatchCommand(console, "op BlueTwo");
        verify(server, times(6)).dispatchCommand(Mockito.eq(console), Mockito.anyString());
    }

    private Path createRepositoryRootWithOperatorManifests() throws Exception {
        Path repositoryRoot = Files.createTempDirectory("ctf-sim-operator-test");
        Path scriptsDirectory = Files.createDirectories(repositoryRoot.resolve("tools/sim/scripts"));
        Path modesDirectory = Files.createDirectories(repositoryRoot.resolve("tools/sim/src/modes"));

        Files.writeString(scriptsDirectory.resolve("run-local.ps1"), "Write-Output 'placeholder'", StandardCharsets.UTF_8);
        Files.writeString(scriptsDirectory.resolve("run-local.sh"), "#!/usr/bin/env bash\nexit 0\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("headless.operators.txt"), "SimDirector\nSimRed01\nSimRed02\nSimBlue01\nSimBlue02\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("basic.operators.txt"), "RedLeader\nRedOne\nRedTwo\nBlueLeader\nBlueOne\nBlueTwo\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("full.operators.txt"), "# comment\nRedLeader\nRedTwo\nBlueLeader\nBlueTwo\n", StandardCharsets.UTF_8);
        return repositoryRoot;
    }
}
