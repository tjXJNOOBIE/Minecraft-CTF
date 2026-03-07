package dev.tjxjnoobie.ctf.team;

import dev.tjxjnoobie.ctf.team.handlers.TeamMembershipHandler;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Facade for team membership and team-related config access.
 */
public final class TeamManager {
    // == Constants ==
    public static final String RED = "red";
    public static final String BLUE = "blue";

    // == Dependencies ==
    private final SpawnConfigHandler spawnConfigHandler;
    private final TeamSpawnStore spawnStore;
    private final TeamReturnStore returnStore;
    private final LobbySpawnStore lobbySpawnStore;
    private final TeamMembershipHandler membershipService;

    // == Lifecycle ==
    /**
     * Constructs a TeamManager instance.
     *
     * @param spawnConfigHandler Dependency responsible for spawn config access.
     * @param spawnStore Dependency responsible for team spawn persistence.
     * @param returnStore Dependency responsible for return point persistence.
     * @param lobbySpawnStore Dependency responsible for lobby spawn persistence.
     * @param membershipService Dependency responsible for scoreboard-backed team membership.
     */
    public TeamManager(SpawnConfigHandler spawnConfigHandler,
                       TeamSpawnStore spawnStore,
                       TeamReturnStore returnStore,
                       LobbySpawnStore lobbySpawnStore,
                       TeamMembershipHandler membershipService) {
        this.spawnConfigHandler = Objects.requireNonNull(spawnConfigHandler, "spawnConfigHandler");
        this.spawnStore = Objects.requireNonNull(spawnStore, "spawnStore");
        this.returnStore = Objects.requireNonNull(returnStore, "returnStore");
        this.lobbySpawnStore = Objects.requireNonNull(lobbySpawnStore, "lobbySpawnStore");
        this.membershipService = Objects.requireNonNull(membershipService, "membershipService");
    }

    /**
     * Executes registerDefaultTeams.
     */
    public void registerDefaultTeams() {
        // Delegate scoreboard team bootstrap to the membership handler.
        membershipService.registerDefaultTeams();
    }

    // == Getters ==
    public SpawnConfigHandler getSpawnConfigHandler() {
        return spawnConfigHandler;
    }

    public String getBalancedTeamKey(String preferred) {
        return membershipService.getBalancedTeamKey(preferred);
    }

    public String getTeamKey(Player player) {
        return membershipService.getTeamKey(player);
    }

    public String getCachedTeamKey(UUID playerId) {
        return membershipService.getCachedTeamKey(playerId);
    }

    public TeamId getTeamId(Player player) {
        return membershipService.getTeamId(player);
    }

    public Optional<Location> getSpawn(String teamKey) {
        return spawnStore.getSpawn(teamKey);
    }

    public Optional<Location> getSpawn(TeamId teamId) {
        return getSpawn(teamId == null ? null : teamId.key());
    }

    public List<Location> getReturnPoints(String teamKey) {
        return returnStore.getReturnPoints(teamKey);
    }

    public List<Location> getReturnPoints(TeamId teamId) {
        return getReturnPoints(teamId == null ? null : teamId.key());
    }

    public Optional<Location> getLobbySpawn() {
        return lobbySpawnStore.getLobbySpawn();
    }

    public Optional<Location> getSpawnFor(Player player) {
        // Resolve the player's current team before delegating to spawn storage.
        String teamKey = getTeamKey(player);
        return teamKey == null ? Optional.empty() : spawnStore.getSpawn(teamKey);
    }

    public Material getFlagMaterial(String teamKey) {
        return TeamDomainUtil.flagMaterial(teamKey);
    }

    public Material getFlagMaterial(TeamId teamId) {
        return getFlagMaterial(teamId == null ? null : teamId.key());
    }

    public Material getCaptureMaterial(String teamKey) {
        return TeamDomainUtil.captureMaterial(teamKey);
    }

    public Material getCaptureMaterial(TeamId teamId) {
        return getCaptureMaterial(teamId == null ? null : teamId.key());
    }

    public String getDisplayName(String teamKey) {
        return TeamDomainUtil.displayName(teamKey);
    }

    public String getDisplayName(TeamId teamId) {
        return getDisplayName(teamId == null ? null : teamId.key());
    }

    public Component getDisplayComponent(String teamKey) {
        return TeamDomainUtil.displayComponent(teamKey);
    }

    public Component getDisplayComponent(TeamId teamId) {
        return getDisplayComponent(teamId == null ? null : teamId.key());
    }

    public int getTeamSize(String teamKey) {
        return membershipService.getTeamSize(teamKey);
    }

    public int getJoinedPlayerCount() {
        return membershipService.getJoinedPlayerCount();
    }

    public List<Player> getTeamPlayers(String teamKey) {
        return membershipService.getTeamPlayers(teamKey);
    }

    public List<Player> getTeamPlayers(TeamId teamId) {
        return getTeamPlayers(teamId == null ? null : teamId.key());
    }

    public List<Player> getJoinedPlayers() {
        return membershipService.getJoinedPlayers();
    }

    public List<String> getTeamEntries(String teamKey) {
        return membershipService.getTeamEntries(teamKey);
    }

    // == Setters ==
    /**
     * Executes setSpawn.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @param location World location used by this operation.
     */
    public void setSpawn(String teamKey, Location location) {
        // Guard: short-circuit when teamKey == null || location == null.
        if (teamKey == null || location == null) {
            return;
        }
        spawnStore.setSpawn(teamKey, location);
    }

    /**
     * Executes setLobbySpawn.
     *
     * @param location World location used by this operation.
     */
    public void setLobbySpawn(Location location) {
        lobbySpawnStore.setLobbySpawn(location);
    }

    // == Utilities ==
    /**
     * Returns the result of normalizeKey.
     *
     * @param input Input value to normalize or validate.
     * @return Resolved value for the requested lookup.
     */
    public String normalizeKey(String input) {
        return TeamDomainUtil.normalizeKey(input);
    }

    /**
     * Returns the result of normalizeTeamId.
     *
     * @param input Input value to normalize or validate.
     * @return Resolved value for the requested lookup.
     */
    public TeamId normalizeTeamId(String input) {
        return TeamId.fromKey(TeamDomainUtil.normalizeKey(input));
    }

    /**
     * Returns the result of addReturnPoint.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @param location World location used by this operation.
     * @return Result produced by this method.
     */
    public boolean addReturnPoint(String teamKey, Location location) {
        return returnStore.addReturnPoint(teamKey, location);
    }

    /**
     * Removes the nearest configured return point to the given location.
     *
     * @param reference Reference location used to choose the nearest return point.
     * @return Team key for the removed point, or {@code null} when none exist.
     */
    public String removeNearestReturnPoint(Location reference) {
        return returnStore.removeNearestReturnPoint(reference);
    }

    /**
     * Executes joinTeam.
     *
     * @param player Player involved in this operation.
     * @param teamKey Team key used for lookup or state updates.
     */
    public void joinTeam(Player player, String teamKey) {
        membershipService.joinTeam(player, teamKey);
    }

    /**
     * Executes addEntityToTeam.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @param entity Entity involved in this operation.
     */
    public void addEntityToTeam(String teamKey, Entity entity) {
        membershipService.addEntityToTeam(teamKey, entity);
    }

    /**
     * Executes removeEntityFromTeams.
     *
     * @param entity Entity involved in this operation.
     */
    public void removeEntityFromTeams(Entity entity) {
        membershipService.removeEntityFromTeams(entity);
    }

    /**
     * Executes leaveTeam.
     *
     * @param player Player involved in this operation.
     */
    public void leaveTeam(Player player) {
        membershipService.leaveTeam(player);
    }

    /**
     * Executes clearAllTeams.
     */
    public void clearAllTeams() {
        membershipService.clearAllTeams();
    }
}
