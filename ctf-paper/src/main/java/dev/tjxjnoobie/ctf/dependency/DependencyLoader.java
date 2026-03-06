package dev.tjxjnoobie.ctf.dependency;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;

/**
 * Stores and resolves runtime dependency instances for the plugin lifecycle.
 */
public final class DependencyLoader {
    private static final DependencyLoader FALLBACK_DEPENDENCY_LOADER = new DependencyLoader();

    private final ConcurrentMap<Class<?>, Object> dependencies = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Class<?>> loadOrder = new ConcurrentLinkedDeque<>();

    // == Lifecycle ==
    /**
     * Registers a concrete dependency instance for a type.
     *
     * @param type Dependency key type.
     * @param instance Dependency instance to store.
     * @param <T> Dependency generic type.
     */
    public <T> void registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");

        // Enforce type safety and deduplicate.
        ensureAssignable(type, instance, "registerInstance");

        Object existing = dependencies.putIfAbsent(type, instance);
        if (existing == null) {
            rememberLoadOrder(type);
            return;
        }
        if (existing instanceof Supplier<?>) {
            // Promote queued dependency to a concrete instance once available.
            dependencies.put(type, instance);
            return;
        }
        fail("registerInstance: instance already registered: " + type.getName());
    }

    /**
     * Registers an important dependency through dynamic type input.
     *
     * @param type Dependency key type.
     * @param instance Dependency instance to store.
     */
    @SuppressWarnings("unchecked")
    public void registerImportantInstance(Class<?> type, Object instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        ensureAssignable(type, instance, "registerImportantInstance");
        registerInstance((Class<Object>) type, instance);
    }

    /**
     * Returns data for loadQueuedDependenciesInOrder.
     */
    public void loadQueuedDependenciesInOrder() {
        for (Class<?> type : loadOrder) {
            Object value = dependencies.get(type);
            // Guard: short-circuit when !(value instanceof Supplier<?> supplier).
            if (!(value instanceof Supplier<?> supplier)) {
                continue;
            }

            Object instance = Objects.requireNonNull(supplier.get(), "supplier returned null");
            ensureAssignable(type, instance, "loadQueuedDependenciesInOrder");
            boolean replaceResult2 = dependencies.replace(type, value, instance);
            if (!replaceResult2) {
                fail("loadQueuedDependenciesInOrder: dependency changed concurrently: " + type.getName());
            }
        }
    }

    // == Getters ==
    public static DependencyLoader getFallbackDependencyLoader() {
        return FALLBACK_DEPENDENCY_LOADER;
    }

    public <T> T requireInstance(Class<T> type) {
        Objects.requireNonNull(type, "type");

        Object value = dependencies.get(type);
        // Guard: Required dependency was never registered.
        if (value == null) {
            fail("getInstance: missing instance: " + type.getName());
        }
        // Guard: Dependency is still queued and has not been fully instantiated.
        if (value instanceof Supplier<?>) {
            fail("getInstance: dependency queued but not loaded: " + type.getName());
        }

        // Safe cast after validation.
        return type.cast(value);
    }

    public java.util.Collection<Object> getAllInstances() {
        return new java.util.ArrayList<>(dependencies.values());
    }

    // == Utilities ==
    /**
     * Replaces an already-registered concrete dependency instance.
     *
     * @param type Dependency key type.
     * @param supplier Supplier that builds the replacement instance.
     * @param <T> Dependency generic type.
     * @return The replacement instance that was stored.
     */
    public <T> T replaceInstance(Class<T> type, Supplier<? extends T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");

        Object existing = dependencies.get(type);
        // Guard: Replacement can only occur when a concrete dependency is already present.
        if (existing == null || existing instanceof Supplier<?>) {
            fail("replaceInstance: instance not registered: " + type.getName());
        }

        T instance = Objects.requireNonNull(supplier.get(), "supplier returned null"); // Resolve replacement and swap atomically.
        ensureAssignable(type, instance, "replaceInstance");

        boolean replaceResult = dependencies.replace(type, existing, instance);
        if (!replaceResult) {
            fail("replaceInstance: instance changed concurrently: " + type.getName());
        }
        return instance;
    }

    /**
     * Executes resetInstances.
     */
    public void resetInstances() {
        dependencies.clear();
        loadOrder.clear();
    }

    /**
     * Queues dependency construction to be resolved later in load order.
     *
     * @param type Dependency key type.
     * @param supplier Supplier that creates the dependency instance.
     */
    public void queueDependency(Class<?> type, Supplier<?> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");

        Object existing = dependencies.putIfAbsent(type, supplier); // Queue supplier for ordered construction.
        if (existing == null) {
            rememberLoadOrder(type);
            return;
        }
        fail("queueDependency: dependency already queued: " + type.getName());
    }

    private static void ensureAssignable(Class<?> type, Object value, String action) {
        boolean isAssignable = type.isAssignableFrom(value.getClass());
        // Guard: Incoming instance does not implement/extend the requested type contract.
        if (!isAssignable) {
            fail(action + ": type mismatch. expected=" + type.getName() + " got=" + value.getClass().getName());
        }
    }

    private void rememberLoadOrder(Class<?> type) {
        loadOrder.addLast(type);
    }

    private static void fail(String message) {
        Bukkit.getLogger().severe("[CTF] ServiceLoader: " + message);
        throw new IllegalStateException(message);
    }

    // == Predicates ==
    public boolean isInstanceRegistered(Class<?> type) {
        // Guard: Caller passed a null dependency key, so registration cannot be checked.
        if (type == null) {
            return false;
        }

        Object value = dependencies.get(type);
        return value != null && !(value instanceof Supplier<?>);
    }
}
