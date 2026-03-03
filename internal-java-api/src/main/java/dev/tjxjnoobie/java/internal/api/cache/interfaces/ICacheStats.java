package dev.tjxjnoobie.java.internal.api.cache.interfaces;

/**
 * Snapshot metrics describing cache occupancy.
 */
public interface ICacheStats {

    /**
     * @return total number of stored wrapper values
     */
    int getTotalEntries();

    /**
     * @return number of non-expired wrapper values
     */
    int getValidEntries();

    /**
     * @return number of expired wrapper values
     */
    int getExpiredEntries();
}
