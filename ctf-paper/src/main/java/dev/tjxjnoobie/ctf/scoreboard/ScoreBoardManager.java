package dev.tjxjnoobie.ctf.scoreboard;

import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.game.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.scoreboard.metadata.ScoreboardState;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Owns per-player scoreboard views and score rendering.
 */
public final class ScoreBoardManager implements MessageAccess {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final String LOCAL_RED_TEAM = "ctf_red_local";
    private static final String LOCAL_BLUE_TEAM = "ctf_blue_local";

    private final TeamManager teamManager;
    private final Map<UUID, ScoreboardState> scoreboards = new HashMap<>();
    private Function<UUID, PlayerMatchStats> statsProvider = ignored -> null;

    private int redScore;
    private int blueScore;
    private int scoreLimit = 3;

    public ScoreBoardManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * Returns the score for the given team.
     */
    public int getScore(String teamKey) {
        if (TeamManager.RED.equals(teamKey)) {
            return redScore;
        }
        if (TeamManager.BLUE.equals(teamKey)) {
            return blueScore;
        }
        return 0;
    }

    /**
     * Increments the score for the given team and returns the new total.
     */
    public int incrementScore(String teamKey) {
        if (TeamManager.RED.equals(teamKey)) {
            redScore++;
            return redScore;
        }
        if (TeamManager.BLUE.equals(teamKey)) {
            blueScore++;
            return blueScore;
        }
        return 0;
    }

    /**
     * Resets both team scores to zero.
     */
    public void resetScores() {
        redScore = 0;
        blueScore = 0;
    }

    /**
     * Sets the score for a specific team, clamped at zero.
     */
    public int setScore(String teamKey, int score) {
        int clamped = Math.max(0, score);
        if (TeamManager.RED.equals(teamKey)) {
            redScore = clamped;
            return redScore;
        }
        if (TeamManager.BLUE.equals(teamKey)) {
            blueScore = clamped;
            return blueScore;
        }
        return 0;
    }

    /**
     * Updates the score limit shown on scoreboards.
     */
    public void setScoreLimit(int scoreLimit) {
        this.scoreLimit = Math.max(1, scoreLimit);
    }

    public int getScoreLimit() {
        return scoreLimit;
    }

    /**
     * Registers a match-stats provider for per-player scoreboard lines.
     */
    public void setStatsProvider(Function<UUID, PlayerMatchStats> statsProvider) {
        if (statsProvider != null) {
            this.statsProvider = statsProvider;
        }
    }

    /**
     * Clears in-memory stats hooks.
     */
    public void clearStats() {
        // Stats are owned by game manager; scoreboard only reads provider.
    }

    /**
     * Attaches the CTF scoreboard to a player.
     */
    public void showScoreboard(Player player) {
        if (player == null) {
            return;
        }

        ScoreboardState state = scoreboards.computeIfAbsent(player.getUniqueId(), id -> createState());
        player.setScoreboard(state.scoreboard);
    }

    /**
     * Removes the CTF scoreboard from a player.
     */
    public void hideScoreboard(Player player) {
        if (player == null) {
            return;
        }

        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        scoreboards.remove(player.getUniqueId());
    }

    /**
     * Removes all scoreboards from joined players.
     */
    public void clearAll() {
        for (Player player : teamManager.getJoinedPlayers()) {
            hideScoreboard(player);
        }
        scoreboards.clear();
    }

    /**
     * Updates all scoreboards with fresh scores and timing.
     */
    public void updateScoreboards(Duration remaining) {
        updateScoreboards(remaining, GameState.IN_PROGRESS);
    }

    /**
     * Updates all scoreboards with fresh scores and state-aware header.
     */
    public void updateScoreboards(Duration remaining, GameState gameState) {
        for (Player player : teamManager.getJoinedPlayers()) {
            updateScoreboardFor(player, remaining, gameState);
        }
    }

    private void updateScoreboardFor(Player player, Duration remaining, GameState gameState) {
        if (player == null) {
            return;
        }

        ScoreboardState scoreboardState = scoreboards.computeIfAbsent(player.getUniqueId(), id -> createState());
        Objective objective = scoreboardState.objective;
        syncTeamEntries(scoreboardState.scoreboard);

        if (gameState == GameState.LOBBY) {
            objective.displayName(msg("scoreboard.header_lobby"));
        } else if (gameState == GameState.OVERTIME) {
            String timeText = formatTime(remaining);
            objective.displayName(msg("scoreboard.header_overtime", Map.of("time", timeText)));
        } else {
            String timeText = formatTime(remaining);
            objective.displayName(msg("scoreboard.header", Map.of("time", timeText)));
        }

        List<String> lines = new ArrayList<>();
        if (gameState != GameState.LOBBY) {
            lines.add(legacy(Component.text("You: ", NamedTextColor.GRAY)
                .append(Component.text(safeDisplayName(player), NamedTextColor.WHITE))));
            lines.add(blankLine("0"));
        }

        lines.add(legacy(msg("scoreboard.line.first_to_win", Map.of(
            "limit", Integer.toString(scoreLimit)
        ))));
        lines.add(blankLine("1"));
        lines.add(legacy(msg("scoreboard.line.red", Map.of("score", Integer.toString(redScore)))));
        lines.add(legacy(msg("scoreboard.line.blue", Map.of("score", Integer.toString(blueScore)))));
        lines.add(blankLine("2"));
        lines.add(legacy(msg("scoreboard.line.red_players", Map.of(
            "count", Integer.toString(teamManager.getTeamSize(TeamManager.RED))
        ))));
        lines.add(legacy(msg("scoreboard.line.blue_players", Map.of(
            "count", Integer.toString(teamManager.getTeamSize(TeamManager.BLUE))
        ))));
        lines.add(blankLine("3"));

        String teamKey = teamManager.getTeamKey(player);
        Component teamComponent = switch (teamKey == null ? "" : teamKey) {
            case TeamManager.RED -> Component.text("Red", NamedTextColor.RED);
            case TeamManager.BLUE -> Component.text("Blue", NamedTextColor.BLUE);
            default -> Component.text("None", NamedTextColor.GRAY);
        };
        lines.add(legacy(Component.text("Team: ", NamedTextColor.WHITE).append(teamComponent)));

        if (gameState == GameState.IN_PROGRESS) {
            PlayerMatchStats stats = statsProvider.apply(player.getUniqueId());
            int captures = stats == null ? 0 : stats.getCaptures();
            int kills = stats == null ? 0 : stats.getKills();
            int deaths = stats == null ? 0 : stats.getDeaths();

            lines.add(blankLine("4"));
            lines.add(legacy(Component.text("Captures: ", NamedTextColor.GOLD)
                .append(Component.text(captures, NamedTextColor.WHITE))));
            lines.add(legacy(Component.text("Kills: ", NamedTextColor.GREEN)
                .append(Component.text(kills, NamedTextColor.WHITE))));
            lines.add(legacy(Component.text("Deaths: ", NamedTextColor.RED)
                .append(Component.text(deaths, NamedTextColor.WHITE))));
        }

        setLines(scoreboardState, lines);
    }

    private ScoreboardState createState() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.getObjective("ctf_score");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("ctf_score", Criteria.DUMMY, Component.text("CTF Score"));
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return new ScoreboardState(scoreboard, objective);
    }

    private void setLines(ScoreboardState state, List<String> lines) {
        for (String line : state.activeLines) {
            state.scoreboard.resetScores(line);
        }
        state.activeLines.clear();

        int score = lines.size();
        for (String line : lines) {
            state.objective.getScore(line).setScore(score--);
            state.activeLines.add(line);
        }
    }

    private String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    private String blankLine(String suffix) {
        return "\u00A7" + suffix;
    }

    private String formatTime(Duration remaining) {
        long seconds = remaining == null ? 0 : Math.max(0, remaining.toSeconds());
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        return String.format("%02d:%02d", minutes, remainder);
    }

    private void syncTeamEntries(Scoreboard scoreboard) {
        if (scoreboard == null) {
            return;
        }

        Team redTeam = scoreboard.getTeam(LOCAL_RED_TEAM);
        if (redTeam == null) {
            redTeam = scoreboard.registerNewTeam(LOCAL_RED_TEAM);
        }
        redTeam.color(NamedTextColor.RED);
        redTeam.displayName(Component.text("Red", NamedTextColor.RED));

        Team blueTeam = scoreboard.getTeam(LOCAL_BLUE_TEAM);
        if (blueTeam == null) {
            blueTeam = scoreboard.registerNewTeam(LOCAL_BLUE_TEAM);
        }
        blueTeam.color(NamedTextColor.BLUE);
        blueTeam.displayName(Component.text("Blue", NamedTextColor.BLUE));

        for (String entry : new ArrayList<>(redTeam.getEntries())) {
            redTeam.removeEntry(entry);
        }
        for (String entry : new ArrayList<>(blueTeam.getEntries())) {
            blueTeam.removeEntry(entry);
        }

        for (Player red : teamManager.getTeamPlayers(TeamManager.RED)) {
            redTeam.addEntry(red.getName());
        }
        for (Player blue : teamManager.getTeamPlayers(TeamManager.BLUE)) {
            blueTeam.addEntry(blue.getName());
        }
    }

    private String safeDisplayName(Player player) {
        String plainName = player == null ? "Unknown" : PLAIN.serialize(player.displayName());
        if (plainName.length() > 16) {
            return plainName.substring(0, 16);
        }
        return plainName;
    }
}

