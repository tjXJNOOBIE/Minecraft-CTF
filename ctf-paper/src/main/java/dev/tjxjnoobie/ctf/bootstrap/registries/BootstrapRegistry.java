package dev.tjxjnoobie.ctf.bootstrap.registries;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;

public interface BootstrapRegistry {
    // Register dependency bindings into the loader.
    void register(DependencyLoader loader, Main plugin);
}
