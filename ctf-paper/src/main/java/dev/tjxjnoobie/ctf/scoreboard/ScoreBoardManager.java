package dev.tjxjnoobie.ctf.scoreboard;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.game.player.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.state.GameState;
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

    // == Lifecycle ==
    /**
     * Constructs a ScoreBoardManager instance.
     *
     * @param teamManager Dependency responsible for team manager.
     */
    public ScoreBoardManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    // == Getters ==
    public int getScore(String teamKey) {
        boolean equalsResult = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult.
        if (equalsResult) {
            return redScore;
        }
        boolean equalsResult2 = TeamManager.BLUE.equals(teamKey);
        // Guard: short-circuit when equalsResult2.
        if (equalsResult2) {
            return blueScore;
        }
        return 0;
    }

    public int getScoreLimit() {
        return scoreLimit;
    }

    private Team getOrCreateLocalTeam(Scoreboard scoreboard, String name, NamedTextColor color, String displayName) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.color(color);
        team.displayName(Component.text(displayName, color));
        return team;
    }

    // == Setters ==
    public int setScore(String teamKey, int score) {
        int clamped = Math.max(0, score);
        boolean equalsResult5 = TeamManager.RED.equals(teamKey);
        if (equalsResult5) {
            redScore = clamped;
            return redScore;
        }
        boolean equalsResult6 = TeamManager.BLUE.equals(teamKey);
        if (equalsResult6) {
            blueScore = clamped;
            return blueScore;
        }
        return 0;
    }

    public void setScoreLimit(int scoreLimit) {
        this.scoreLimit = Math.max(1, scoreLimit);
    }

    public void setStatsProvider(Function<UUID, PlayerMatchStats> statsProvider) {
        if (statsProvider != null) {
            this.statsProvider = statsProvider;
        }
    }

    public void updateScoreboards(Duration remaining, GameState gameState) {
        for (Player player : teamManager.getJoinedPlayers()) {
            updateScoreboardFor(player, remaining, gameState);
        }
    }

    private void updateScoreboardFor(Player player, Duration remaining, GameState gameState) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        ScoreboardState scoreboardState = scoreboards.computeIfAbsent(playerUUID, id -> createState());
        Objective objective = scoreboardState.objective;
        syncTeamEntries(scoreboardState.scoreboard);
        objective.displayName(buildHeader(remaining, gameState));

        List<String> lines = new ArrayList<>();
        if (gameState != GameState.LOBBY) {
            String displayName = safeDisplayName(player);
            Component youLine = Component.text("You: ", NamedTextColor.GRAY)
                .append(Component.text(displayName, NamedTextColor.WHITE));
            addLine(lines, youLine);
            addBlank(lines, "0");
        }

        addFormattedLine(lines, "scoreboard.line.first_to_win", scoreLimit);
        addBlank(lines, "1");
        addFormattedLine(lines, "scoreboard.line.red", redScore);
        addFormattedLine(lines, "scoreboard.line.blue", blueScore);
        addBlank(lines, "2");
        addFormattedLine(lines, "scoreboard.line.red_players", teamManager.getTeamSize(TeamManager.RED));
        addFormattedLine(lines, "scoreboard.line.blue_players", teamManager.getTeamSize(TeamManager.BLUE));
        addBlank(lines, "3");

        Component teamLine = Component.text("Team: ", NamedTextColor.WHITE)
            .append(teamComponent(teamManager.getTeamKey(player)));
        addLine(lines, teamLine);

        if (gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME) {
            PlayerMatchStats stats = statsProvider.apply(player.getUniqueId());
            int captures = stats == null ? 0 : stats.getCaptures();
            int kills = stats == null ? 0 : stats.getKills();
            int deaths = stats == null ? 0 : stats.getDeaths();

            addBlank(lines, "4");
            Component capturesLine = Component.text("Captures: ", NamedTextColor.GOLD)
                .append(Component.text(captures, NamedTextColor.WHITE));
            addLine(lines, capturesLine);
            Component killsLine = Component.text("Kills: ", NamedTextColor.GREEN)
                .append(Component.text(kills, NamedTextColor.WHITE));
            addLine(lines, killsLine);
            Component deathsLine = Component.text("Deaths: ", NamedTextColor.RED)
                .append(Component.text(deaths, NamedTextColor.WHITE));
            addLine(lines, deathsLine);
        }

        setLines(scoreboardState, lines);
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

    // == Utilities ==
    /**
     * Returns the result of incrementScore.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @return Updated value after applying this operation.
     */
    public int incrementScore(String teamKey) {
        boolean equalsResult3 = TeamManager.RED.equals(teamKey);
        if (equalsResult3) {
            redScore++;
            return redScore;
        }
        boolean equalsResult4 = TeamManager.BLUE.equals(teamKey);
        if (equalsResult4) {
            blueScore++;
            return blueScore;
        }
        return 0;
    }

    /**
     * Executes resetScores.
     */
    public void resetScores() {
        redScore = 0;
        blueScore = 0;
    }

    /**
     * Executes clearStats.
     */
    public void clearStats() {
        // Stats are owned by game manager; scoreboard only reads provider.
    }

    /**
     * Executes showScoreboard.
     *
     * @param player Player involved in this operation.
     */
    public void showScoreboard(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        ScoreboardState state = scoreboards.computeIfAbsent(playerUUID, id -> createState());
        player.setScoreboard(state.scoreboard);
    }

    /**
     * Executes hideScoreboard.
     *
     * @param player Player involved in this operation.
     */
    public void hideScoreboard(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        player.setScoreboard(mainScoreboard);
        scoreboards.remove(playerUUID);
    }

    /**
     * Executes clearAll.
     */
    public void clearAll() {
        for (Player player : teamManager.getJoinedPlayers()) {
            hideScoreboard(player);
        }
        scoreboards.clear();
    }

    private ScoreboardState createState() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.getObjective(CTFKeys.scoreboardObjectiveId());
        if (objective == null) {
            Component title = getMessage("scoreboard.header_lobby");
            objective = scoreboard.registerNewObjective(CTFKeys.scoreboardObjectiveId(), Criteria.DUMMY, title);
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return new ScoreboardState(scoreboard, objective);
    }

    private Component buildHeader(Duration remaining, GameState gameState) {
        // Guard: short-circuit when gameState == GameState.LOBBY.
        if (gameState == GameState.LOBBY) {
            return getMessage("scoreboard.header_lobby");
        }
        String timeText = formatTime(remaining);
        // Guard: short-circuit when gameState == GameState.OVERTIME.
        if (gameState == GameState.OVERTIME) {
            return getMessageFormatted("scoreboard.header_overtime", timeText);
        }
        return getMessageFormatted("scoreboard.header", timeText);
    }

    private Component teamComponent(String teamKey) {
        return switch (teamKey == null ? "" : teamKey) {
            case TeamManager.RED -> Component.text("Red", NamedTextColor.RED);
            case TeamManager.BLUE -> Component.text("Blue", NamedTextColor.BLUE);
            default -> Component.text("None", NamedTextColor.GRAY);
        };
    }

    private void addLine(List<String> lines, Component component) {
        lines.add(legacy(component));
    }

    private void addFormattedLine(List<String> lines, String key, Object... args) {
        lines.add(legacy(getMessageFormatted(key, args)));
    }

    private void addBlank(List<String> lines, String suffix) {
        lines.add(blankLine(suffix));
    }

    private void clearTeamEntries(Team team) {
        // Guard: short-circuit when team == null.
        if (team == null) {
            return;
        }
        for (String entry : new ArrayList<>(team.getEntries())) {
            team.removeEntry(entry);
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
        // Guard: short-circuit when scoreboard == null.
        if (scoreboard == null) {
            return;
        }

        Team redTeam = getOrCreateLocalTeam(scoreboard, LOCAL_RED_TEAM, NamedTextColor.RED, "Red");
        Team blueTeam = getOrCreateLocalTeam(scoreboard, LOCAL_BLUE_TEAM, NamedTextColor.BLUE, "Blue");

        clearTeamEntries(redTeam);
        clearTeamEntries(blueTeam);

        for (Player red : teamManager.getTeamPlayers(TeamManager.RED)) {
            redTeam.addEntry(red.getName());
        }
        for (String redEntry : teamManager.getTeamEntries(TeamManager.RED)) {
            redTeam.addEntry(redEntry);
        }
        for (Player blue : teamManager.getTeamPlayers(TeamManager.BLUE)) {
            blueTeam.addEntry(blue.getName());
        }
        for (String blueEntry : teamManager.getTeamEntries(TeamManager.BLUE)) {
            blueTeam.addEntry(blueEntry);
        }
    }

    private String safeDisplayName(Player player) {
        String plainName = "Unknown";
        if (player != null) {
            Component displayName = player.displayName();
            plainName = PLAIN.serialize(displayName);
        }
        boolean conditionResult1 = plainName.length() > 16;
        // Guard: short-circuit when plainName.length() > 16.
        if (conditionResult1) {
            return plainName.substring(0, 16);
        }
        return plainName;
    }
}

