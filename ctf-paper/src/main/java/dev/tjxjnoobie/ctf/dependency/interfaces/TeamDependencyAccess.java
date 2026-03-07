package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Dependency-access surface for team membership, spawns, and team metadata.
 */
public interface TeamDependencyAccess {
    default TeamManager getTeamManager() { return DependencyLoaderAccess.findInstance(TeamManager.class); }

    default String teamGetBalancedTeamKey(String preferred) {
        return getTeamManager().getBalancedTeamKey(preferred);
    }

    default String teamGetTeamKey(Player player) {
        return getTeamManager().getTeamKey(player);
    }

    default String teamGetCachedTeamKey(UUID playerId) {
        return getTeamManager().getCachedTeamKey(playerId);
    }

    default TeamId teamGetTeamId(Player player) {
        return getTeamManager().getTeamId(player);
    }

    default Optional<Location> teamGetSpawn(String teamKey) {
        return getTeamManager().getSpawn(teamKey);
    }

    default Optional<Location> teamGetSpawn(TeamId teamId) {
        return getTeamManager().getSpawn(teamId);
    }

    default List<Location> teamGetReturnPoints(String teamKey) {
        return getTeamManager().getReturnPoints(teamKey);
    }

    default List<Location> teamGetReturnPoints(TeamId teamId) {
        return getTeamManager().getReturnPoints(teamId);
    }

    default Optional<Location> teamGetSpawnFor(Player player) {
        return getTeamManager().getSpawnFor(player);
    }

    default Material teamGetFlagMaterial(String teamKey) {
        return getTeamManager().getFlagMaterial(teamKey);
    }

    default Material teamGetFlagMaterial(TeamId teamId) {
        return getTeamManager().getFlagMaterial(teamId);
    }

    default Material teamGetCaptureMaterial(String teamKey) {
        return getTeamManager().getCaptureMaterial(teamKey);
    }

    default Material teamGetCaptureMaterial(TeamId teamId) {
        return getTeamManager().getCaptureMaterial(teamId);
    }

    default String teamGetDisplayName(String teamKey) {
        return getTeamManager().getDisplayName(teamKey);
    }

    default String teamGetDisplayName(TeamId teamId) {
        return getTeamManager().getDisplayName(teamId);
    }

    default Component teamGetDisplayComponent(String teamKey) {
        return getTeamManager().getDisplayComponent(teamKey);
    }

    default Component teamGetDisplayComponent(TeamId teamId) {
        return getTeamManager().getDisplayComponent(teamId);
    }

    default int teamGetJoinedPlayerCount() {
        return getTeamManager().getJoinedPlayerCount();
    }

    default List<Player> teamGetTeamPlayers(String teamKey) {
        return getTeamManager().getTeamPlayers(teamKey);
    }

    default List<Player> teamGetTeamPlayers(TeamId teamId) {
        return getTeamManager().getTeamPlayers(teamId);
    }

    default boolean teamIsFriendlyFire(Player shooter, Player victim) {
        String shooterTeam = teamGetTeamKey(shooter);
        String victimTeam = teamGetTeamKey(victim);
        return shooterTeam == null || victimTeam == null || shooterTeam.equals(victimTeam);
    }

    default List<Player> teamGetJoinedPlayers() {
        return getTeamManager().getJoinedPlayers();
    }

    default void teamSetSpawn(String teamKey, Location location) {
        getTeamManager().setSpawn(teamKey, location);
    }

    default void teamSetLobbySpawn(Location location) {
        getTeamManager().setLobbySpawn(location);
    }

    default String teamNormalizeKey(String input) {
        return getTeamManager().normalizeKey(input);
    }

    default TeamId teamNormalizeTeamId(String input) {
        return getTeamManager().normalizeTeamId(input);
    }

    default boolean teamAddReturnPoint(String teamKey, Location location) {
        return getTeamManager().addReturnPoint(teamKey, location);
    }

    default void teamClearAllTeams() {
        getTeamManager().clearAllTeams();
    }
}
