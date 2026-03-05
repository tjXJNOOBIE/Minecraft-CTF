package dev.tjxjnoobie.ctf.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tjxjnoobie.ctf.support.ServiceLoaderTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceLoaderIsolationTest extends ServiceLoaderTestSupport {
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
    void getInstanceFailsWhenMissing() {
        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();

        assertThrows(IllegalStateException.class, () -> loader.requireInstance(String.class));
    }

    @Test
    void getInstanceFailsWhenDependencyIsQueuedButNotLoaded() {
        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();
        loader.queueDependency(String.class, () -> "queued");

        assertThrows(IllegalStateException.class, () -> loader.requireInstance(String.class));
    }

    @Test
    void loadQueuedDependenciesInOrderInstantiatesSuppliersSequentially() {
        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();
        List<String> order = new ArrayList<>();

        loader.queueDependency(First.class, () -> {
            order.add("first");
            return new First();
        });
        loader.queueDependency(Second.class, () -> {
            order.add(loader.requireInstance(First.class) != null ? "second-after-first" : "second-before-first");
            return new Second();
        });

        loader.loadQueuedDependenciesInOrder();

        assertEquals(List.of("first", "second-after-first"), order);
        assertTrue(loader.isInstanceRegistered(First.class));
        assertTrue(loader.isInstanceRegistered(Second.class));
    }

    @Test
    void replaceInstanceSwapsRegisteredConcreteInstance() {
        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();
        loader.registerImportantInstance(StringBuilder.class, new StringBuilder("before"));

        StringBuilder replaced = loader.replaceInstance(StringBuilder.class, () -> new StringBuilder("after"));

        assertEquals("after", replaced.toString());
        assertSame(replaced, loader.requireInstance(StringBuilder.class));
    }

    @Test
    void resetInstancesClearsRegistry() {
        DependencyLoader loader = DependencyLoader.getFallbackDependencyLoader();
        loader.registerImportantInstance(StringBuilder.class, new StringBuilder("value"));

        loader.resetInstances();

        assertFalse(loader.isInstanceRegistered(StringBuilder.class));
    }

    private static final class First {
    }

    private static final class Second {
    }
}
