package dev.tjxjnoobie.ctf.commands.util;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Executes the local simulation runner script for the current host OS.
 */
public final class SimulationScriptUtil {
    private static final String DEFAULT_MODE = "headless";
    private static final String MODES_DIRECTORY = "tools/sim/src/modes";
    private static final Pattern PUBLIC_MODE_NAME = Pattern.compile("^[a-z0-9-]+$");
    private static final List<String> REPOSITORY_ROOT_ENV_KEYS = List.of("CTF_SIM_REPO_ROOT", "CTF_REPO_ROOT");
    private static final List<String> KNOWN_REPOSITORY_ROOTS = List.of(
        "/srv/local-minecraft-ctf",
        "/srv/local-pc-root/F:/workspace/Minecraft-CTF",
        "F:/workspace/Minecraft-CTF"
    );

    /**
     * Creates a non-instantiable utility holder.
     */
    private SimulationScriptUtil() {
    }

    /**
     * Determines whether the current JVM is running on Windows.
     *
     * @return {@code true} when the host OS is Windows.
     */
    public static boolean isWindowsHost() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * Executes the bundled local simulation script from the repository root.
     *
     * @param startDirectory Directory to begin repository-root discovery from.
     * @param host Target Minecraft host.
     * @param port Target Minecraft port.
     * @param bots Number of bots to spawn.
     * @param arenaSize Arena size argument for the simulation.
     * @param seed Optional simulation seed.
     * @return Started process for the simulation runner.
     * @throws IOException When the repository root or script cannot be found or started.
     */
    public static Process executeScript(Path startDirectory,
                                        String host,
                                        int port,
                                        int bots,
                                        int arenaSize,
                                        String seed) throws IOException {
        return executeScript(startDirectory, host, port, bots, arenaSize, seed, DEFAULT_MODE);
    }

    /**
     * Executes the bundled local simulation script from the repository root.
     *
     * @param startDirectory Directory to begin repository-root discovery from.
     * @param host Target Minecraft host.
     * @param port Target Minecraft port.
     * @param bots Number of bots to spawn.
     * @param arenaSize Arena size argument for the simulation.
     * @param seed Optional simulation seed.
     * @param modeName Public simulation mode name.
     * @return Started process for the simulation runner.
     * @throws IOException When the repository root or script cannot be found or started.
     */
    public static Process executeScript(Path startDirectory,
                                        String host,
                                        int port,
                                        int bots,
                                        int arenaSize,
                                        String seed,
                                        String modeName) throws IOException {
        return executeScript(startDirectory, host, port, bots, arenaSize, seed, modeName, false);
    }

    /**
     * Executes the bundled local simulation script from the repository root.
     *
     * @param startDirectory Directory to begin repository-root discovery from.
     * @param host Target Minecraft host.
     * @param port Target Minecraft port.
     * @param bots Number of bots to spawn.
     * @param arenaSize Arena size argument for the simulation.
     * @param seed Optional simulation seed.
     * @param modeName Public simulation mode name.
     * @param openWindowsConsole When true, Windows launches the runner in a separate console window.
     * @return Started process for the simulation runner.
     * @throws IOException When the repository root or script cannot be found or started.
     */
    public static Process executeScript(Path startDirectory,
                                        String host,
                                        int port,
                                        int bots,
                                        int arenaSize,
                                        String seed,
                                        String modeName,
                                        boolean openWindowsConsole) throws IOException {
        boolean windowsHost = isWindowsHost();
        Path repositoryRoot = findRepositoryRoot(startDirectory);
        List<String> command = buildCommand(repositoryRoot, windowsHost, host, port, bots, arenaSize, seed, modeName);
        if (windowsHost && openWindowsConsole) {
            command = buildWindowsConsoleCommand(command);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(repositoryRoot.toFile());
        if (!windowsHost || !openWindowsConsole) {
            processBuilder.redirectErrorStream(true);
            processBuilder.inheritIO();
        }
        return processBuilder.start();
    }

    /**
     * Returns the public simulation mode used when the command is run without an explicit mode.
     *
     * @return Default public simulation mode name.
     */
    public static String defaultModeName() {
        return DEFAULT_MODE;
    }

    /**
     * Lists allowlisted public simulation modes from the bundled modes directory.
     *
     * @param startDirectory Directory to begin repository-root discovery from.
     * @return Sorted public mode names available to the command.
     * @throws IOException When the repository root cannot be resolved.
     */
    public static List<String> listAvailableModes(Path startDirectory) throws IOException {
        Path repositoryRoot = findRepositoryRoot(startDirectory);
        Path modesDirectory = repositoryRoot.resolve(MODES_DIRECTORY);
        if (!Files.isDirectory(modesDirectory)) {
            return List.of();
        }

        try (Stream<Path> modeEntries = Files.list(modesDirectory)) {
            return modeEntries
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.endsWith(".ts"))
                .map(name -> name.substring(0, name.length() - 3))
                .filter(SimulationScriptUtil::isPublicModeName)
                .sorted()
                .toList();
        }
    }

    /**
     * Locates the repository root by walking upward from known candidate directories.
     *
     * @param startDirectory Directory to begin repository-root discovery from.
     * @return Resolved repository root containing the simulation scripts.
     * @throws IOException When no repository root containing the scripts can be found.
     */
    static Path findRepositoryRoot(Path startDirectory) throws IOException {
        Set<Path> candidateRoots = new LinkedHashSet<>();
        Path currentDirectory = startDirectory == null ? Path.of("").toAbsolutePath() : startDirectory.toAbsolutePath();
        candidateRoots.add(currentDirectory);

        for (String envKey : REPOSITORY_ROOT_ENV_KEYS) {
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.isBlank()) {
                addCandidatePath(candidateRoots, envValue);
            }
        }

        for (String repositoryRoot : KNOWN_REPOSITORY_ROOTS) {
            addCandidatePath(candidateRoots, repositoryRoot);
        }

        for (Path candidateRoot : candidateRoots) {
            Path repositoryRoot = findRepositoryRootFrom(candidateRoot);
            if (repositoryRoot != null) {
                return repositoryRoot;
            }
        }

        throw new IOException("Could not locate tools/sim/scripts from " + currentDirectory);
    }

    /**
     * Builds the platform-specific process command used to launch a simulation mode.
     *
     * @param repositoryRoot Resolved repository root containing the scripts.
     * @param windowsHost Whether the current host platform is Windows.
     * @param host Target Minecraft host.
     * @param port Target Minecraft port.
     * @param bots Number of bots to spawn.
     * @param arenaSize Arena size argument for the simulation.
     * @param seed Optional simulation seed.
     * @param modeName Public simulation mode name.
     * @return Command line ready to pass to {@link ProcessBuilder}.
     * @throws IOException When the selected mode or platform script cannot be found.
     */
    static List<String> buildCommand(Path repositoryRoot,
                                     boolean windowsHost,
                                     String host,
                                     int port,
                                     int bots,
                                     int arenaSize,
                                     String seed,
                                     String modeName) throws IOException {
        String resolvedModeName = resolveModeName(repositoryRoot, modeName);
        Path scriptPath = windowsHost
            ? repositoryRoot.resolve("tools/sim/scripts/run-local.ps1")
            : repositoryRoot.resolve("tools/sim/scripts/run-local.sh");

        if (!Files.exists(scriptPath)) {
            throw new IOException("Missing simulation script: " + scriptPath);
        }

        List<String> command = new ArrayList<>();
        if (windowsHost) {
            command.add("powershell");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(scriptPath.toString());
            command.add("-Mode");
            command.add(resolvedModeName);
            command.add("-ServerHost");
            command.add(host);
            command.add("-ServerPort");
            command.add(String.valueOf(port));
            command.add("-BotCount");
            command.add(String.valueOf(bots));
            command.add("-ArenaSize");
            command.add(String.valueOf(arenaSize));
            if (seed != null && !seed.isBlank()) {
                command.add("-Seed");
                command.add(seed);
            }
            return command;
        }

        command.add("bash");
        command.add(scriptPath.toString());
        command.add("--mode");
        command.add(resolvedModeName);
        command.add("--host");
        command.add(host);
        command.add("--port");
        command.add(String.valueOf(port));
        command.add("--bots");
        command.add(String.valueOf(bots));
        command.add("--arenaSize");
        command.add(String.valueOf(arenaSize));
        if (seed != null && !seed.isBlank()) {
            command.add("--seed");
            command.add(seed);
        }
        return command;
    }

    /**
     * Wraps a Windows runner command in a fresh console window so bot logs remain visible.
     *
     * @param baseCommand Base runner command to execute in the new console.
     * @return Windows {@code cmd.exe start} wrapper command.
     */
    static List<String> buildWindowsConsoleCommand(List<String> baseCommand) {
        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add("start");
        command.add("\"\"");
        command.addAll(baseCommand);
        return command;
    }

    private static Path findRepositoryRootFrom(Path candidateRoot) {
        Path repositoryRoot = candidateRoot;

        while (repositoryRoot != null
                && !Files.exists(repositoryRoot.resolve("tools/sim/scripts/run-local.ps1"))
                && !Files.exists(repositoryRoot.resolve("tools/sim/scripts/run-local.sh"))) {
            repositoryRoot = repositoryRoot.getParent();
        }

        if (repositoryRoot == null) {
            return null;
        }
        return repositoryRoot;
    }

    private static void addCandidatePath(Set<Path> candidateRoots, String rawPath) {
        try {
            candidateRoots.add(Path.of(rawPath).toAbsolutePath());
        } catch (InvalidPathException ignored) {
            // Ignore roots that are invalid for the current host OS.
        }
    }

    private static String resolveModeName(Path repositoryRoot, String modeName) throws IOException {
        String normalizedModeName = modeName == null || modeName.isBlank()
            ? DEFAULT_MODE
            : modeName.trim().toLowerCase(Locale.ROOT);

        if (!isPublicModeName(normalizedModeName)) {
            throw new IOException("Invalid simulation mode: " + modeName);
        }

        Path modeEntry = repositoryRoot.resolve(MODES_DIRECTORY).resolve(normalizedModeName + ".ts");
        if (!Files.exists(modeEntry)) {
            throw new IOException("Unknown simulation mode: " + normalizedModeName);
        }
        return normalizedModeName;
    }

    private static boolean isPublicModeName(String modeName) {
        return PUBLIC_MODE_NAME.matcher(modeName).matches();
    }
}
