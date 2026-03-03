package dev.tjxjnoobie.java.internal.api.cache.interfaces;

/**
 * Read-only wrapper interface for cached values.
 *
 * @param <V> payload type
 */
public interface ICacheValue<V> {

    /**
     * Returns the wrapped payload.
     *
     * @return payload
     */
    V getValue();
}
