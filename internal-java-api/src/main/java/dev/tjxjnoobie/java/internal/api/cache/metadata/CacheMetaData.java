package dev.tjxjnoobie.java.internal.api.cache.metadata;

import dev.tjxjnoobie.java.internal.api.cache.interfaces.ICacheStats;

/**
 * Immutable metrics snapshot for cache occupancy state.
 */
public final class CacheMetaData implements ICacheStats {

    private final int totalEntries;
    private final int validEntries;
    private final int expiredEntries;

    /**
     * Creates a metadata snapshot.
     *
     * @param totalEntries total wrapper count
     * @param validEntries non-expired wrapper count
     * @param expiredEntries expired wrapper count
     */
    public CacheMetaData(int totalEntries, int validEntries, int expiredEntries) {
        this.totalEntries = totalEntries;
        this.validEntries = validEntries;
        this.expiredEntries = expiredEntries;
    }

    @Override
    public int getTotalEntries() {
        return totalEntries;
    }

    @Override
    public int getValidEntries() {
        return validEntries;
    }

    @Override
    public int getExpiredEntries() {
        return expiredEntries;
    }

    /**
     * Returns a copy with updated expired entry count.
     *
     * @param newExpiredEntries new expired count
     * @return updated metadata snapshot
     */
    public CacheMetaData withExpiredEntries(int newExpiredEntries) {
        return new CacheMetaData(totalEntries, validEntries, newExpiredEntries);
    }

    /**
     * Returns a copy with updated total entry count.
     *
     * @param newTotalEntries new total count
     * @return updated metadata snapshot
     */
    public CacheMetaData withTotalEntries(int newTotalEntries) {
        return new CacheMetaData(newTotalEntries, validEntries, expiredEntries);
    }

    /**
     * Returns a copy with updated valid entry count.
     *
     * @param newValidEntries new valid count
     * @return updated metadata snapshot
     */
    public CacheMetaData withValidEntries(int newValidEntries) {
        return new CacheMetaData(totalEntries, newValidEntries, expiredEntries);
    }

    @Override
    public String toString() {
        return String.format(
                "CacheMetaData{totalEntries=%d, validEntries=%d, expiredEntries=%d}",
                totalEntries,
                validEntries,
                expiredEntries
        );
    }
}
