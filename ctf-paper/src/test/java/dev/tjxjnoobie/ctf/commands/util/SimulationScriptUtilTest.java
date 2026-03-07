package dev.tjxjnoobie.ctf.commands.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tjxjnoobie.ctf.TestLogSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationScriptUtilTest extends TestLogSupport {

    @Test
    void detectsCurrentHostOperatingSystem() {
        boolean expected = System.getProperty("os.name", "").toLowerCase().contains("win");

        assertEquals(expected, SimulationScriptUtil.isWindowsHost());
    }

    @Test
    void buildsWindowsRunnerCommand() throws Exception {
        Path repositoryRoot = createRepositoryRootWithScripts();
        Path scriptPath = repositoryRoot.resolve("tools/sim/scripts/run-local.ps1");

        List<String> command = SimulationScriptUtil.buildCommand(
            repositoryRoot,
            true,
            "127.0.0.1",
            25565,
            12,
            75,
            "demo-seed",
            "hyperreal");

        assertIterableEquals(
            List.of(
                "powershell",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                scriptPath.toString(),
                "-Mode",
                "hyperreal",
                "-ServerHost",
                "127.0.0.1",
                "-ServerPort",
                "25565",
                "-BotCount",
                "12",
                "-ArenaSize",
                "75",
                "-Seed",
                "demo-seed"),
            command);
    }

    @Test
    void buildsLinuxRunnerCommand() throws Exception {
        Path repositoryRoot = createRepositoryRootWithScripts();
        Path scriptPath = repositoryRoot.resolve("tools/sim/scripts/run-local.sh");

        List<String> command = SimulationScriptUtil.buildCommand(
            repositoryRoot,
            false,
            "127.0.0.1",
            25565,
            12,
            75,
            "demo-seed",
            "hyperreal");

        assertIterableEquals(
            List.of(
                "bash",
                scriptPath.toString(),
                "--mode",
                "hyperreal",
                "--host",
                "127.0.0.1",
                "--port",
                "25565",
                "--bots",
                "12",
                "--arenaSize",
                "75",
                "--seed",
                "demo-seed"),
            command);
    }

    @Test
    void wrapsWindowsRunnerCommandInNewConsoleLauncher() {
        List<String> command = SimulationScriptUtil.buildWindowsConsoleCommand(
            List.of(
                "powershell",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                "tools/sim/scripts/run-local.ps1"));

        assertIterableEquals(
            List.of(
                "cmd.exe",
                "/c",
                "start",
                "\"\"",
                "powershell",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                "tools/sim/scripts/run-local.ps1"),
            command);
    }

    @Test
    void listsOnlyPublicModesFromModesDirectory() throws Exception {
        Path repositoryRoot = createRepositoryRootWithScripts();

        List<String> modes = SimulationScriptUtil.listAvailableModes(repositoryRoot);

        assertEquals(List.of("basic", "full", "headless", "hyperreal"), modes);
    }

    @Test
    void executesLocalRunnerScriptFromDiscoveredRepositoryRoot() throws Exception {
        Path repositoryRoot = createRepositoryRootWithScripts();
        Path nestedDirectory = Files.createDirectories(repositoryRoot.resolve("ctf-paper/build/tmp"));
        Path outputFile = repositoryRoot.resolve("sim.out");

        if (SimulationScriptUtil.isWindowsHost()) {
            Files.writeString(
                repositoryRoot.resolve("tools/sim/scripts/run-local.ps1"),
                """
                param(
                  [string]$ServerHost,
                  [int]$ServerPort,
                  [int]$BotCount,
                  [int]$ArenaSize,
                  [string]$Seed = ""
                )
                Set-Content -Path "sim.out" -Value "$ServerHost|$ServerPort|$BotCount|$ArenaSize|$Seed"
                """,
                StandardCharsets.UTF_8);
        } else {
            Files.writeString(
                repositoryRoot.resolve("tools/sim/scripts/run-local.sh"),
                """
                #!/usr/bin/env bash
                set -euo pipefail
                printf "%s|%s|%s|%s|%s" "$2" "$4" "$6" "$8" "${10:-}" > sim.out
                """,
                StandardCharsets.UTF_8);
        }

        Process process = SimulationScriptUtil.executeScript(nestedDirectory, "127.0.0.1", 25565, 12, 75, "demo-seed", "hyperreal");
        int exitCode = process.waitFor();
        String output = Files.readString(outputFile, StandardCharsets.UTF_8).trim();

        assertEquals(0, exitCode);
        assertEquals("127.0.0.1|25565|12|75|demo-seed", output);
        assertTrue(Files.exists(outputFile));
    }

    private Path createRepositoryRootWithScripts() throws Exception {
        Path repositoryRoot = Files.createTempDirectory("ctf-sim-script-test");
        Path scriptsDirectory = Files.createDirectories(repositoryRoot.resolve("tools/sim/scripts"));
        Path modesDirectory = Files.createDirectories(repositoryRoot.resolve("tools/sim/src/modes"));
        Files.writeString(scriptsDirectory.resolve("run-local.ps1"), "Write-Output 'placeholder'", StandardCharsets.UTF_8);
        Files.writeString(scriptsDirectory.resolve("run-local.sh"), "#!/usr/bin/env bash\nexit 0\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("hyperreal.ts"), "console.log('hyperreal');\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("headless.ts"), "console.log('headless');\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("basic.ts"), "console.log('basic');\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("full.ts"), "console.log('full');\n", StandardCharsets.UTF_8);
        Files.writeString(modesDirectory.resolve("hyperRealDefault.ts"), "console.log('internal');\n", StandardCharsets.UTF_8);
        return repositoryRoot;
    }
}
