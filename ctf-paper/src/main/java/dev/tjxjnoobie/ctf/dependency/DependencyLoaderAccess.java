package dev.tjxjnoobie.ctf.dependency;

import dev.tjxjnoobie.ctf.bootstrap.PluginBootstrap;
import java.util.Objects;

/**
 * Provides static access to the active dependency loader.
 */
public final class DependencyLoaderAccess {

    // == Lifecycle ==
    /**
     * Creates a non-instantiable utility holder.
     */
    private DependencyLoaderAccess() {
    }

    /**
     * Resolves the active dependency loader for runtime lookups.
     *
     * @return Active loader from bootstrap, or the fallback loader when bootstrap is unavailable.
     */
    private static DependencyLoader loader() {
        PluginBootstrap activePluginBootstrap = PluginBootstrap.getActivePluginBootstrap();
        // Guard: Plugin bootstrap is unavailable during startup/shutdown, so use fallback resolution.
        if (activePluginBootstrap != null) {
            return activePluginBootstrap.getDependencyLoader();
        }
        return DependencyLoader.getFallbackDependencyLoader();
    }

    // == Getters ==
    public static <T> T findInstance(Class<T> type) {
        DependencyLoader activeLoader = loader();
        boolean isRegistered = activeLoader.isInstanceRegistered(type);
        // Guard: The dependency has not been registered, so return null for optional resolution.
        if (!isRegistered) {
            return null;
        }
        return activeLoader.requireInstance(type);
    }

    public static <T> T requireInstance(Class<T> type, String message) {
        T instance = findInstance(type);
        return Objects.requireNonNull(instance, message);
    }
}
