package dev.tjxjnoobie.java.internal.api.cache.maps;

import dev.tjxjnoobie.java.internal.api.cache.enums.CacheDomain;
import dev.tjxjnoobie.java.internal.api.cache.enums.CacheType;
import dev.tjxjnoobie.java.internal.api.cache.interfaces.ICacheKey;
import dev.tjxjnoobie.java.internal.api.cache.interfaces.ICacheValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe key-to-bucket cache index.
 *
 * <p>This structure stores each key as a bucket of {@link ICacheValue} wrappers, which allows
 * multiple values to accumulate under the same domain key.</p>
 */
public final class CacheMap {

    private static final CacheMap INSTANCE = new CacheMap();

    private final ConcurrentMap<ICacheKey<?>, CopyOnWriteArrayList<ICacheValue<?>>> buckets;

    private CacheMap() {
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * Returns the shared singleton cache map.
     *
     * @return singleton instance
     */
    public static CacheMap getCacheMap() {
        return INSTANCE;
    }

    // --- Write operations ---

    /**
     * Appends a wrapped value to the key bucket. A new bucket is created if the key is absent.
     *
     * @param cacheKey bucket key
     * @param newValue wrapped value
     */
    public void add(ICacheKey<?> cacheKey, ICacheValue<?> newValue) {
        Objects.requireNonNull(cacheKey, "cacheKey cannot be null");
        Objects.requireNonNull(newValue, "newValue cannot be null");

        buckets.computeIfAbsent(cacheKey, ignored -> new CopyOnWriteArrayList<>()).add(newValue);
    }

    /**
     * Removes a specific wrapper instance from all buckets.
     *
     * @param valueToRemove wrapper to remove
     */
    public void removeValue(ICacheValue<?> valueToRemove) {
        if (valueToRemove == null) {
            return;
        }
        buckets.forEach((key, bucket) -> bucket.remove(valueToRemove));
    }

    /**
     * Removes a key and its bucket.
     *
     * @param key cache key to remove
     */
    public void removeCacheKey(ICacheKey<?> key) {
        if (key != null) {
            buckets.remove(key);
        }
    }

    /**
     * Clears all buckets and values.
     */
    public void clear() {
        buckets.clear();
    }

    // --- Query operations ---

    /**
     * Returns a detached copy of the bucket for a key.
     *
     * @param key bucket key
     * @return detached bucket values, empty when key is absent
     */
    public List<ICacheValue<?>> getBucket(ICacheKey<?> key) {
        List<ICacheValue<?>> bucket = buckets.get(key);
        return bucket == null ? List.of() : new ArrayList<>(bucket);
    }

    /**
     * @param key cache key to test
     * @return {@code true} if the key exists
     */
    public boolean containsDomainKey(ICacheKey<?> key) {
        return key != null && buckets.containsKey(key);
    }

    /**
     * Checks if a raw payload value already exists in the key bucket.
     *
     * @param key cache key
     * @param realPayload payload to compare by {@link Object#equals(Object)}
     * @return {@code true} if payload exists in bucket
     */
    public boolean containsPayload(ICacheKey<?> key, Object realPayload) {
        if (key == null || realPayload == null) {
            return false;
        }
        List<ICacheValue<?>> bucket = buckets.get(key);
        if (bucket == null) {
            return false;
        }
        for (ICacheValue<?> wrapper : bucket) {
            if (realPayload.equals(wrapper.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Type-safe extraction helper for wrapper payloads.
     *
     * @param wrapper wrapped value
     * @param type expected runtime type
     * @param <T> expected type parameter
     * @return payload cast to {@code T}, or {@code null} when type does not match
     */
    public <T> T unwrap(ICacheValue<?> wrapper, Class<T> type) {
        if (wrapper == null || wrapper.getValue() == null) {
            return null;
        }
        Object rawValue = wrapper.getValue();
        return type.isInstance(rawValue) ? type.cast(rawValue) : null;
    }

    /**
     * Finds all buckets for a cache domain.
     *
     * @param domain domain to match
     * @return detached bucket copies matching the domain
     */
    public List<List<ICacheValue<?>>> findByDomain(CacheDomain domain) {
        if (domain == null) {
            return List.of();
        }
        return findBy(entry -> entry.getKey().getCacheDomain() == domain);
    }

    /**
     * Finds all buckets for a cache type.
     *
     * @param type type to match
     * @return detached bucket copies matching the type
     */
    public List<List<ICacheValue<?>>> findByType(CacheType type) {
        if (type == null) {
            return List.of();
        }
        return findBy(entry -> entry.getKey().getCacheType() == type);
    }

    /**
     * @return number of top-level keys in the cache map
     */
    public int size() {
        return buckets.size();
    }

    /**
     * Returns a detached copy of the bucket for direct assertions in tests.
     *
     * @param key cache key
     * @return detached bucket list, empty when absent
     */
    public List<ICacheValue<?>> get(ICacheKey<?> key) {
        return getBucket(key);
    }

    /**
     * Applies a predicate over entries and returns detached bucket copies.
     *
     * @param predicate entry match condition
     * @return matched detached buckets
     */
    private List<List<ICacheValue<?>>> findBy(
            java.util.function.Predicate<Map.Entry<ICacheKey<?>, CopyOnWriteArrayList<ICacheValue<?>>>> predicate
    ) {
        List<List<ICacheValue<?>>> matches = new ArrayList<>();
        for (Map.Entry<ICacheKey<?>, CopyOnWriteArrayList<ICacheValue<?>>> entry : buckets.entrySet()) {
            if (predicate.test(entry)) {
                matches.add(new ArrayList<>(entry.getValue()));
            }
        }
        return matches;
    }
}
