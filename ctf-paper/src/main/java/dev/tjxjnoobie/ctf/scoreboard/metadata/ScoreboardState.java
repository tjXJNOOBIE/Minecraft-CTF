package dev.tjxjnoobie.ctf.scoreboard.metadata;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Stores scoreboard state for a single player view.
 */
public final class ScoreboardState {
    public final Scoreboard scoreboard;
    public final Objective objective;
    public final List<String> activeLines = new ArrayList<>();

    public ScoreboardState(Scoreboard scoreboard, Objective objective) {
        this.scoreboard = scoreboard;
        this.objective = objective;
    }
}

