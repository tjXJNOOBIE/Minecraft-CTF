package dev.tjxjnoobie.java.internal.api.cache;

import dev.tjxjnoobie.java.internal.api.cache.enums.CacheDomain;
import dev.tjxjnoobie.java.internal.api.cache.enums.CacheSource;
import dev.tjxjnoobie.java.internal.api.cache.enums.CacheType;
import dev.tjxjnoobie.java.internal.api.cache.enums.CacheVersion;
import dev.tjxjnoobie.java.internal.api.cache.interfaces.ICacheKey;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable cache key wrapper containing both the raw key and optional metadata.
 *
 * @param <K> raw key type
 */
public final class CacheKey<K> implements ICacheKey<K> {

    private final K rawKey;
    private final CacheType cacheType;
    private final CacheDomain cacheDomain;
    private final CacheSource source;
    private final CacheVersion version;
    private final long createdAt;
    private final AtomicInteger accessCount;

    /**
     * Creates a fully specified cache key.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param cacheDomain cache domain
     * @param source source subsystem
     * @param version version tag
     */
    public CacheKey(
            K rawKey,
            CacheType cacheType,
            CacheDomain cacheDomain,
            CacheSource source,
            CacheVersion version
    ) {
        this.rawKey = rawKey;
        this.cacheType = cacheType;
        this.cacheDomain = cacheDomain;
        this.source = source;
        this.version = version;
        this.createdAt = System.currentTimeMillis();
        this.accessCount = new AtomicInteger(0);
    }

    /**
     * Creates a key with no metadata.
     *
     * @param rawKey raw domain key
     */
    public CacheKey(K rawKey) {
        this(rawKey, null, null, null, null);
    }

    /**
     * Creates a key with cache type metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     */
    public CacheKey(K rawKey, CacheType cacheType) {
        this(rawKey, cacheType, null, null, null);
    }

    /**
     * Creates a key with type and domain metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param cacheDomain cache domain
     */
    public CacheKey(K rawKey, CacheType cacheType, CacheDomain cacheDomain) {
        this(rawKey, cacheType, cacheDomain, null, null);
    }

    /**
     * Creates a key with type and source metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param source source subsystem
     */
    public CacheKey(K rawKey, CacheType cacheType, CacheSource source) {
        this(rawKey, cacheType, null, source, null);
    }

    /**
     * Creates a key with type and version metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param version version tag
     */
    public CacheKey(K rawKey, CacheType cacheType, CacheVersion version) {
        this(rawKey, cacheType, null, null, version);
    }

    /**
     * Creates a key with type, domain, and source metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param cacheDomain cache domain
     * @param source source subsystem
     */
    public CacheKey(K rawKey, CacheType cacheType, CacheDomain cacheDomain, CacheSource source) {
        this(rawKey, cacheType, cacheDomain, source, null);
    }

    /**
     * Creates a key with type, source, and version metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param source source subsystem
     * @param version version tag
     */
    public CacheKey(K rawKey, CacheType cacheType, CacheSource source, CacheVersion version) {
        this(rawKey, cacheType, null, source, version);
    }

    /**
     * Creates a key with type, domain, and version metadata.
     *
     * @param rawKey raw domain key
     * @param cacheType cache storage type
     * @param cacheDomain cache domain
     * @param version version tag
     */
    public CacheKey(K rawKey, CacheType cacheType, CacheDomain cacheDomain, CacheVersion version) {
        this(rawKey, cacheType, cacheDomain, null, version);
    }

    // --- Core getters ---

    @Override
    public K getRawCacheKey() {
        return rawKey;
    }

    @Override
    public CacheType getCacheType() {
        return cacheType;
    }

    @Override
    public CacheDomain getCacheDomain() {
        return cacheDomain;
    }

    @Override
    public CacheSource getSource() {
        return source;
    }

    @Override
    public CacheVersion getVersion() {
        return version;
    }

    /**
     * @return epoch milliseconds when the key was created
     */
    public long getCreatedAt() {
        return createdAt;
    }

    // --- Access tracking ---

    /**
     * Atomically increments the access counter.
     *
     * @return incremented access count
     */
    public int incrementAccessCount() {
        return accessCount.incrementAndGet();
    }

    /**
     * @return current access count
     */
    public int getAccessCount() {
        return accessCount.get();
    }

    // --- Object contract ---

    /**
     * Compares keys by raw key and metadata fields.
     *
     * @param other comparison target
     * @return {@code true} when both keys represent the same identity
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CacheKey<?> that)) {
            return false;
        }
        return Objects.equals(rawKey, that.rawKey)
                && cacheType == that.cacheType
                && cacheDomain == that.cacheDomain
                && source == that.source
                && version == that.version;
    }

    /**
     * Computes hash from raw key and metadata fields.
     *
     * @return stable hash code for key identity
     */
    @Override
    public int hashCode() {
        return Objects.hash(rawKey, cacheType, cacheDomain, source, version);
    }

    /**
     * Returns a debug-friendly key representation.
     *
     * @return key string
     */
    @Override
    public String toString() {
        return "CacheKey{" +
                "rawKey=" + rawKey +
                ", cacheType=" + cacheType +
                ", cacheDomain=" + cacheDomain +
                ", source=" + source +
                ", version=" + version +
                ", accessCount=" + accessCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
