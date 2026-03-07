package dev.tjxjnoobie.ctf.game.combat.util;

import java.util.function.Supplier;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Builds plugin-scoped keys with a fallback namespace for isolated tests.
 */
public final class CombatNamespacedKeyFactory {

    private CombatNamespacedKeyFactory() {
    }

    public static NamespacedKey create(String key,
                                       String fallbackNamespace,
                                       Supplier<JavaPlugin> pluginSupplier) {
        try {
            JavaPlugin plugin = pluginSupplier == null ? null : pluginSupplier.get();
            if (plugin == null) {
                throw new IllegalStateException("plugin unavailable");
            }

            String pluginName = plugin.getName();
            if (pluginName != null && !pluginName.isBlank()) {
                try {
                    return new NamespacedKey(plugin, key);
                } catch (RuntimeException ignored) {
                    // Fall through to the test-safe fallback namespace.
                }
            }
        } catch (IllegalStateException ignored) {
            // Fallback path used in isolated unit tests before plugin bootstrap.
        }

        return new NamespacedKey(fallbackNamespace, key);
    }
}
