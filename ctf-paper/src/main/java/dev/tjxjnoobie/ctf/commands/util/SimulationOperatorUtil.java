package dev.tjxjnoobie.ctf.commands.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Grants operator access to bundled simulation bots from per-mode manifest files.
 */
public final class SimulationOperatorUtil {
    private static final String MODES_DIRECTORY = "tools/sim/src/modes";

    private SimulationOperatorUtil() {
    }

    /**
     * Grants operator access to every bot identity listed for the selected mode.
     *
     * @param startDirectory Directory to begin bundled simulation-root discovery from.
     * @param modeName Public simulation mode name.
     * @throws IOException When the bundled simulation root or operator manifest cannot be read.
     */
    public static void ensureOperatorAccess(Path startDirectory, String modeName) throws IOException {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String botName : listOperatorNames(startDirectory, modeName)) {
            Bukkit.dispatchCommand(console, "op " + botName);
        }
    }

    /**
     * Lists the bot usernames that require operator access for the selected mode.
     *
     * @param startDirectory Directory to begin bundled simulation-root discovery from.
     * @param modeName Public simulation mode name.
     * @return Ordered usernames loaded from the bundled operator manifest.
     * @throws IOException When the bundled simulation root cannot be resolved or the manifest cannot be read.
     */
    static List<String> listOperatorNames(Path startDirectory, String modeName) throws IOException {
        Path repositoryRoot = SimulationScriptUtil.findRepositoryRoot(startDirectory);
        Path manifestPath = repositoryRoot
            .resolve(MODES_DIRECTORY)
            .resolve(normalizeModeName(modeName) + ".operators.txt");

        if (!Files.exists(manifestPath)) {
            return List.of();
        }

        try {
            return Files.readAllLines(manifestPath, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .toList();
        } catch (IOException exception) {
            throw new IOException("Failed to read simulation operator manifest: " + manifestPath, exception);
        }
    }

    /**
     * Normalizes a public simulation mode name into the corresponding manifest file prefix.
     *
     * @param modeName Public simulation mode name.
     * @return Lowercase manifest prefix.
     */
    private static String normalizeModeName(String modeName) {
        return modeName == null ? "" : modeName.trim().toLowerCase(Locale.ROOT);
    }
}
