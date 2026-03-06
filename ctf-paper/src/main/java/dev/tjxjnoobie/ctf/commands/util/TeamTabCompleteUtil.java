package dev.tjxjnoobie.ctf.commands.util;

import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.Locale;

/**
 * Provides reusable tab-complete suggestions for team-based command arguments.
 */
public final class TeamTabCompleteUtil {

    // == Lifecycle ==
    /**
     * Creates a non-instantiable utility holder.
     */
    private TeamTabCompleteUtil() {
    }

    // == Utilities ==
    /**
     * Returns team suggestions for a specific argument index.
     *
     * @param args Raw command argument array.
     * @param index Argument index that should receive team suggestions.
     * @return Filtered team key suggestions for the requested index.
     */
    public static List<String> suggestTeamArgument(String[] args, int index) {
        // Guard: Arguments are unavailable or index is invalid, so no suggestions can be generated safely.
        if (args == null || index < 0 || args.length != index + 1) {
            return List.of();
        }

        String rawInput = args[index];
        String normalizedInput = rawInput == null ? "" : rawInput.toLowerCase(Locale.ROOT);
        return List.of(TeamManager.RED, TeamManager.BLUE).stream()
            .filter(value -> value.startsWith(normalizedInput))
            .toList();
    }
}
