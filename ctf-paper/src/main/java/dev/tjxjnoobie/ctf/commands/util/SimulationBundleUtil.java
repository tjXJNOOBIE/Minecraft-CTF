package dev.tjxjnoobie.ctf.commands.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Extracts the bundled simulation runtime from the plugin jar to the plugin data folder.
 */
public final class SimulationBundleUtil {
    private static final String BUNDLE_PREFIX = "sim-bundle/";
    private static final String BUNDLE_ROOT_DIRECTORY = "sim-bundle";

    private SimulationBundleUtil() {
    }

    public static Path ensureExtracted(JavaPlugin plugin) throws IOException {
        Path dataFolder = plugin.getDataFolder().toPath();
        Path bundleRoot = dataFolder.resolve(BUNDLE_ROOT_DIRECTORY);
        Files.createDirectories(bundleRoot);

        Path pluginJar = resolvePluginJar(plugin);
        try (JarFile jarFile = new JarFile(pluginJar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith(BUNDLE_PREFIX)) {
                    continue;
                }

                String relativePath = entryName.substring(BUNDLE_PREFIX.length());
                if (relativePath.isBlank()) {
                    continue;
                }

                Path outputPath = bundleRoot.resolve(relativePath).normalize();
                if (!outputPath.startsWith(bundleRoot)) {
                    throw new IOException("Refusing to extract simulation entry outside bundle root: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                    continue;
                }

                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return bundleRoot;
    }

    private static Path resolvePluginJar(JavaPlugin plugin) throws IOException {
        try {
            return Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException exception) {
            throw new IOException("Could not resolve plugin jar location", exception);
        }
    }
}
