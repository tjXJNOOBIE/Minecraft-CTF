package dev.tjxjnoobie.ctf.game.flag.metadata;

import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import java.util.UUID;
import org.bukkit.Location;

/**
 * Holds mutable state for a single team's flag.
 */
public final class FlagMetaData {
    // Base flag position (immutable baseline).
    public Location baseLocation;
    // Current flag location when dropped or carried.
    public Location activeLocation;
    public FlagState state = FlagState.AT_BASE; // Current lifecycle state.
    // Current carrier UUID when carried.
    public UUID carrier;
}

