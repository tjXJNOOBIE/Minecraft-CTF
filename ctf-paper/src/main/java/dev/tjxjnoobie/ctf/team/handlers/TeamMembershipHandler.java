package dev.tjxjnoobie.ctf.team.handlers;

import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.team.TeamScoreboardUtil;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Owns scoreboard-backed team registration and membership state.
 */
public final class TeamMembershipHandler {
    private final Scoreboard scoreboard;
    private final Map<UUID, String> teamKeyByPlayerId = new ConcurrentHashMap<>();

    private Team redTeam;
    private Team blueTeam;

    public TeamMembershipHandler(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public void registerDefaultTeams() {
        redTeam = TeamScoreboardUtil.getOrRegisterTeam(scoreboard, CTFKeys.teamScoreboardRedId());
        blueTeam = TeamScoreboardUtil.getOrRegisterTeam(scoreboard, CTFKeys.teamScoreboardBlueId());
        configureTeam(redTeam, NamedTextColor.RED, "Red");
        configureTeam(blueTeam, NamedTextColor.BLUE, "Blue");
    }

    public String getBalancedTeamKey(String preferred) {
        int redCount = getTeamSize(TeamManager.RED);
        int blueCount = getTeamSize(TeamManager.BLUE);
        if (redCount < blueCount) {
            return TeamManager.RED;
        }
        if (blueCount < redCount) {
            return TeamManager.BLUE;
        }
        return preferred == null ? TeamManager.RED : preferred;
    }

    public String getTeamKey(Player player) {
        if (player == null) {
            return null;
        }

        UUID playerId = player.getUniqueId();
        String cachedTeamKey = teamKeyByPlayerId.get(playerId);
        if (cachedTeamKey != null) {
            return cachedTeamKey;
        }

        String playerName = player.getName();
        if (TeamScoreboardUtil.containsEntry(redTeam, playerName)) {
            teamKeyByPlayerId.put(playerId, TeamManager.RED);
            return TeamManager.RED;
        }
        if (TeamScoreboardUtil.containsEntry(blueTeam, playerName)) {
            teamKeyByPlayerId.put(playerId, TeamManager.BLUE);
            return TeamManager.BLUE;
        }
        return null;
    }

    public String getCachedTeamKey(UUID playerId) {
        return playerId == null ? null : teamKeyByPlayerId.get(playerId);
    }

    public TeamId getTeamId(Player player) {
        return TeamId.fromKey(getTeamKey(player));
    }

    public int getTeamSize(String teamKey) {
        return getTeamPlayers(teamKey).size();
    }

    public int getJoinedPlayerCount() {
        return getTeamSize(TeamManager.RED) + getTeamSize(TeamManager.BLUE);
    }

    public List<Player> getTeamPlayers(String teamKey) {
        List<Player> players = new ArrayList<>();
        Team team = resolveTeam(teamKey);
        if (team == null) {
            return players;
        }

        for (String entry : team.getEntries()) {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public List<Player> getJoinedPlayers() {
        List<Player> players = new ArrayList<>(getTeamPlayers(TeamManager.RED));
        players.addAll(getTeamPlayers(TeamManager.BLUE));
        return players;
    }

    public List<String> getTeamEntries(String teamKey) {
        Team team = resolveTeam(teamKey);
        if (team == null) {
            return List.of();
        }
        return new ArrayList<>(team.getEntries());
    }

    public void joinTeam(Player player, String teamKey) {
        if (player == null || teamKey == null) {
            return;
        }

        removeFromTeams(player);

        Team team = resolveTeam(teamKey);
        if (team != null) {
            team.addEntry(player.getName());
            teamKeyByPlayerId.put(player.getUniqueId(), teamKey);
        }
    }

    public void addEntityToTeam(String teamKey, Entity entity) {
        if (teamKey == null || entity == null) {
            return;
        }

        String entry = entity.getUniqueId().toString();
        TeamScoreboardUtil.removeEntry(entry, redTeam, blueTeam);

        Team team = resolveTeam(teamKey);
        if (team != null) {
            team.addEntry(entry);
        }
    }

    public void removeEntityFromTeams(Entity entity) {
        if (entity == null) {
            return;
        }
        TeamScoreboardUtil.removeEntry(entity.getUniqueId().toString(), redTeam, blueTeam);
    }

    public void leaveTeam(Player player) {
        removeFromTeams(player);
    }

    public void clearAllTeams() {
        TeamScoreboardUtil.clearEntries(redTeam);
        TeamScoreboardUtil.clearEntries(blueTeam);
        teamKeyByPlayerId.clear();
    }

    private void configureTeam(Team team, NamedTextColor color, String displayName) {
        if (team == null) {
            return;
        }
        team.color(color);
        team.displayName(Component.text(displayName, color));
        TeamScoreboardUtil.clearEntries(team);
    }

    private Team resolveTeam(String teamKey) {
        if (TeamManager.RED.equals(teamKey)) {
            return redTeam;
        }
        if (TeamManager.BLUE.equals(teamKey)) {
            return blueTeam;
        }
        return null;
    }

    private void removeFromTeams(Player player) {
        if (player == null) {
            return;
        }

        teamKeyByPlayerId.remove(player.getUniqueId());
        TeamScoreboardUtil.removeEntry(player.getName(), redTeam, blueTeam);
    }
}
