package dev.tjxjnoobie.ctf.scoreboard.metadata;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Stores scoreboard state for a single player view.
 */
public final class ScoreboardState {

    // == Immutable scoreboard references ==
    public final Scoreboard scoreboard;
    public final Objective objective;

    // == Current rendered lines ==
    public final List<String> activeLines = new ArrayList<>();

    /**
     * Constructs a ScoreboardState instance.
     *
     * @param scoreboard Collection or state object updated by this operation.
     * @param objective Collection or state object updated by this operation.
     */
    public ScoreboardState(Scoreboard scoreboard, Objective objective) {
        // Capture scoreboard and objective handles.
        this.scoreboard = scoreboard;
        this.objective = objective;
    }
}

