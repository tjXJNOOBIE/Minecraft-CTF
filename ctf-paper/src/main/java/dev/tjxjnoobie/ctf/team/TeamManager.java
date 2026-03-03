package dev.tjxjnoobie.ctf.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Owns team membership and team metadata for CTF players.
 */
public final class TeamManager {
    public static final String RED = "red";
    public static final String BLUE = "blue";

    // Plugin dependencies
    private final JavaPlugin plugin;
    private final SpawnConfigHandler spawnConfigHandler;
    private final TeamSpawnStore spawnStore;
    private final TeamReturnStore returnStore;
    private final LobbySpawnStore lobbySpawnStore;
    private final Scoreboard scoreboard;

    // Team membership state
    private Team redTeam;
    private Team blueTeam;

    public TeamManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spawnConfigHandler = new SpawnConfigHandler(plugin);
        this.spawnStore = new TeamSpawnStore(spawnConfigHandler);
        this.returnStore = new TeamReturnStore(spawnConfigHandler);
        this.lobbySpawnStore = new LobbySpawnStore(spawnConfigHandler);
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /**
     * Returns the spawn config handler used by team data.
     */
    public SpawnConfigHandler getSpawnConfigHandler() {
        return spawnConfigHandler;
    }

    // Team registration
    /**
     * Registers the fixed red/blue teams.
     */
    public void registerDefaultTeams() {
        redTeam = getOrRegisterTeam("ctf_red");
        blueTeam = getOrRegisterTeam("ctf_blue");

        if (redTeam != null) {
            redTeam.color(NamedTextColor.RED);
            redTeam.displayName(Component.text("Red", NamedTextColor.RED));
            redTeam.getEntries().forEach(redTeam::removeEntry);
        }

        if (blueTeam != null) {
            blueTeam.color(NamedTextColor.BLUE);
            blueTeam.displayName(Component.text("Blue", NamedTextColor.BLUE));
            blueTeam.getEntries().forEach(blueTeam::removeEntry);
        }
    }

    /**
     * Normalizes user input to a known team key, otherwise returns null.
     */
    public String normalizeKey(String input) {
        if (input == null) {
            return null;
        }

        String key = input.trim().toLowerCase(Locale.ROOT);
        if (RED.equals(key) || BLUE.equals(key)) {
            return key;
        }
        return null;
    }

    /**
     * Returns a balanced team key, falling back to preferred if counts tie.
     */
    public String getBalancedTeamKey(String preferred) {
        int redCount = getTeamSize(RED);
        int blueCount = getTeamSize(BLUE);
        if (redCount < blueCount) {
            return RED;
        }
        if (blueCount < redCount) {
            return BLUE;
        }
        return preferred == null ? RED : preferred;
    }

    /**
     * Returns the team key for a player currently joined to CTF.
     */
    public String getTeamKey(Player player) {
        if (player == null) {
            return null;
        }

        if (redTeam != null && redTeam.hasEntry(player.getName())) {
            return RED;
        }
        if (blueTeam != null && blueTeam.hasEntry(player.getName())) {
            return BLUE;
        }
        return null;
    }

    /**
     * Returns true when the player is currently joined to a CTF team.
     */
    public boolean isInGame(Player player) {
        return getTeamKey(player) != null;
    }

    /**
     * Returns the configured spawn for a team key when available.
     */
    public Optional<Location> getSpawn(String teamKey) {
        return spawnStore.getSpawn(teamKey);
    }

    /**
     * Persists a team spawn location.
     */
    public void setSpawn(String teamKey, Location location) {
        if (teamKey == null || location == null) {
            return;
        }
        spawnStore.setSpawn(teamKey, location);
    }

    /**
     * Persists a team return point.
     */
    public boolean addReturnPoint(String teamKey, Location location) {
        return returnStore.addReturnPoint(teamKey, location);
    }

    /**
     * Returns all return points for a team.
     */
    public List<Location> getReturnPoints(String teamKey) {
        return returnStore.getReturnPoints(teamKey);
    }

    /**
     * Persists the global CTF lobby spawn location.
     */
    public void setLobbySpawn(Location location) {
        lobbySpawnStore.setLobbySpawn(location);
    }

    /**
     * Returns the configured CTF lobby spawn when available.
     */
    public Optional<Location> getLobbySpawn() {
        return lobbySpawnStore.getLobbySpawn();
    }

    /**
     * Returns the configured spawn for the player's current team.
     */
    public Optional<Location> getSpawnFor(Player player) {
        String teamKey = getTeamKey(player);
        if (teamKey == null) {
            return Optional.empty();
        }
        return spawnStore.getSpawn(teamKey);
    }

    /**
     * Moves a player onto the specified team.
     */
    public void joinTeam(Player player, String teamKey) {
        if (player == null || teamKey == null) {
            return;
        }

        removeFromTeams(player);
        Team team = resolveTeam(teamKey);
        if (team != null) {
            team.addEntry(player.getName());
        }
    }

    /**
     * Adds an entity entry to the specified team for glow coloring.
     */
    public void addEntityToTeam(String teamKey, Entity entity) {
        if (entity == null || teamKey == null) {
            return;
        }

        String entry = entity.getUniqueId().toString();
        removeEntryFromTeams(entry);
        Team team = resolveTeam(teamKey);
        if (team != null) {
            team.addEntry(entry);
        }
    }

    /**
     * Removes an entity entry from both teams.
     */
    public void removeEntityFromTeams(Entity entity) {
        if (entity == null) {
            return;
        }
        removeEntryFromTeams(entity.getUniqueId().toString());
    }

    /**
     * Removes a player from CTF team tracking.
     */
    public void leaveTeam(Player player) {
        if (player == null) {
            return;
        }
        removeFromTeams(player);
    }

    /**
     * Clears all team membership mappings.
     */
    public void clearAllTeams() {
        if (redTeam != null) {
            redTeam.getEntries().forEach(redTeam::removeEntry);
        }
        if (blueTeam != null) {
            blueTeam.getEntries().forEach(blueTeam::removeEntry);
        }
    }

    /**
     * Returns the configured wool material for the team flag block/item.
     */
    public Material getFlagMaterial(String teamKey) {
        if (RED.equals(teamKey)) {
            return Material.RED_STAINED_GLASS;
        }
        if (BLUE.equals(teamKey)) {
            return Material.BLUE_STAINED_GLASS;
        }
        return null;
    }

    /**
     * Returns the capture-zone wool material for the team.
     */
    public Material getCaptureMaterial(String teamKey) {
        if (RED.equals(teamKey)) {
            return Material.RED_WOOL;
        }
        if (BLUE.equals(teamKey)) {
            return Material.BLUE_WOOL;
        }
        return null;
    }

    /**
     * Returns a plain-text display name for the team.
     */
    public String getDisplayName(String teamKey) {
        if (RED.equals(teamKey)) {
            return "Red";
        }
        if (BLUE.equals(teamKey)) {
            return "Blue";
        }
        return teamKey == null ? "Unknown" : teamKey;
    }

    /**
     * Returns the rich display component for the team.
     */
    public Component getDisplayComponent(String teamKey) {
        if (RED.equals(teamKey)) {
            return Component.text("Red", NamedTextColor.RED, TextDecoration.BOLD);
        }
        if (BLUE.equals(teamKey)) {
            return Component.text("Blue", NamedTextColor.BLUE, TextDecoration.BOLD);
        }
        return Component.text(teamKey == null ? "Unknown" : teamKey);
    }

    /**
     * Returns the number of players on a given team.
     */
    public int getTeamSize(String teamKey) {
        return getTeamPlayers(teamKey).size();
    }

    /**
     * Returns the total number of joined players.
     */
    public int getJoinedPlayerCount() {
        return getTeamSize(RED) + getTeamSize(BLUE);
    }

    /**
     * Returns a list of online players on a given team.
     */
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

    /**
     * Returns a list of all joined players.
     */
    public List<Player> getJoinedPlayers() {
        List<Player> players = new ArrayList<>();
        players.addAll(getTeamPlayers(RED));
        players.addAll(getTeamPlayers(BLUE));
        return players;
    }

    private Team resolveTeam(String teamKey) {
        if (RED.equals(teamKey)) {
            return redTeam;
        }
        if (BLUE.equals(teamKey)) {
            return blueTeam;
        }
        return null;
    }

    private void removeFromTeams(Player player) {
        if (player == null) {
            return;
        }

        if (redTeam != null) {
            redTeam.removeEntry(player.getName());
        }
        if (blueTeam != null) {
            blueTeam.removeEntry(player.getName());
        }
    }

    private void removeEntryFromTeams(String entry) {
        if (entry == null) {
            return;
        }

        if (redTeam != null) {
            redTeam.removeEntry(entry);
        }
        if (blueTeam != null) {
            blueTeam.removeEntry(entry);
        }
    }

    private Team getOrRegisterTeam(String name) {
        if (scoreboard == null || name == null) {
            return null;
        }

        Team team = scoreboard.getTeam(name);
        if (team != null) {
            return team;
        }
        return scoreboard.registerNewTeam(name);
    }
}

