package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Dependency-access surface for the plugin's primary config file and plugin instance.
 */
public interface PluginConfigDependencyAccess {
    default Main getMainPlugin() { return DependencyLoaderAccess.findInstance(Main.class); }
    default Main mainPlugin() { return getMainPlugin(); }

    default FileConfiguration mainConfig() {
        return getMainPlugin().getConfig();
    }

    default void saveMainConfig() {
        getMainPlugin().saveConfig();
    }
}
