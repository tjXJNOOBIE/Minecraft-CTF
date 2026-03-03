package dev.tjxjnoobie.ctf.game.flag.metadata;

import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import java.util.UUID;
import org.bukkit.Location;

/**
 * Holds mutable state for a single team's flag.
 */
public final class FlagMetaData {
    public Location baseLocation;
    public Location activeLocation;
    public FlagState state = FlagState.AT_BASE;
    public UUID carrier;
}

