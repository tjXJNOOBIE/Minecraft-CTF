package dev.tjxjnoobie.ctf.team;

import java.util.ArrayList;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Shared scoreboard helpers for team membership handlers.
 */
public final class TeamScoreboardUtil {

    private TeamScoreboardUtil() {
    }

    public static Team getOrRegisterTeam(Scoreboard scoreboard, String name) {
        if (scoreboard == null || name == null) {
            return null;
        }

        Team existingTeam = scoreboard.getTeam(name);
        return existingTeam != null ? existingTeam : scoreboard.registerNewTeam(name);
    }

    public static boolean containsEntry(Team team, String entry) {
        return team != null && entry != null && team.hasEntry(entry);
    }

    public static void removeEntry(String entry, Team... teams) {
        if (entry == null || teams == null) {
            return;
        }

        for (Team team : teams) {
            if (team != null) {
                team.removeEntry(entry);
            }
        }
    }

    public static void clearEntries(Team team) {
        if (team == null) {
            return;
        }

        for (String entry : new ArrayList<>(team.getEntries())) {
            team.removeEntry(entry);
        }
    }
}
