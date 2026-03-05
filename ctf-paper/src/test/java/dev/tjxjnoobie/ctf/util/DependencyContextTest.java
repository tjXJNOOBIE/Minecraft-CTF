package dev.tjxjnoobie.ctf.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DependencyContextTest extends TestLogSupport {
    private final DependencyLoader dependencyLoader = DependencyLoader.getFallbackDependencyLoader();

    @BeforeEach
    void setUp() {
        dependencyLoader.resetInstances();
    }

    @AfterEach
    void tearDown() {
        dependencyLoader.resetInstances();
    }

    @Test
    void requiredThrowsWhenDependencyIsMissing() {
        assertThrows(IllegalStateException.class, () -> dependencyLoader.requireInstance(FakeDependency.class));
    }

    @Test
    void optionalReturnsNullWhenDependencyIsMissing() {
        assertNull(dependencyLoader.isInstanceRegistered(FakeDependency.class) ? dependencyLoader.requireInstance(FakeDependency.class) : null);
    }

    @Test
    void lazySuppliersResolveAfterLateRegistration() {
        Supplier<FakeDependency> lazyRequired = () -> dependencyLoader.requireInstance(FakeDependency.class);
        Supplier<FakeDependency> lazyOptional =
            () -> dependencyLoader.isInstanceRegistered(FakeDependency.class) ? dependencyLoader.requireInstance(FakeDependency.class) : null;

        assertNull(lazyOptional.get());

        FakeDependency dependency = new FakeDependency("value");
        dependencyLoader.registerImportantInstance(FakeDependency.class, dependency);

        assertEquals("value", lazyRequired.get().value());
        assertEquals("value", lazyOptional.get().value());
    }

    private record FakeDependency(String value) {}
}
