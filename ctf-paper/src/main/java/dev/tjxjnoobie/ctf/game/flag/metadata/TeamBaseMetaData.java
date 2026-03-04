package dev.tjxjnoobie.ctf.game.flag.metadata;

import dev.tjxjnoobie.ctf.team.TeamId;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Mutable DTO containing per-team base data used by flag/base systems.
 */
public final class TeamBaseMetaData {

    // == Runtime state ==
    private TeamId teamId;
    private Location flagSpawnLocation;
    private Location flagBlockLocation;
    private Material flagBlockMaterial;
    private Location indicatorSpawnLocation;
    private Location baseSpawnLocation;
    private List<Location> returnSpawnLocations = new ArrayList<>();

    // == Getters ==
    public TeamId getTeamId() {
        return teamId;
    }

    public Location getFlagSpawnLocation() {
        return flagSpawnLocation;
    }

    public Location getFlagBlockLocation() {
        return flagBlockLocation;
    }

    public Material getFlagBlockMaterial() {
        return flagBlockMaterial;
    }

    public Location getIndicatorSpawnLocation() {
        return indicatorSpawnLocation;
    }

    public Location getBaseSpawnLocation() {
        return baseSpawnLocation;
    }

    public List<Location> getReturnSpawnLocations() {
        return returnSpawnLocations;
    }

    // == Setters ==
    public void setTeamId(TeamId teamId) {
        this.teamId = teamId;
    }

    public void setFlagSpawnLocation(Location flagSpawnLocation) {
        this.flagSpawnLocation = flagSpawnLocation;
    }

    public void setFlagBlockLocation(Location flagBlockLocation) {
        this.flagBlockLocation = flagBlockLocation;
    }

    public void setFlagBlockMaterial(Material flagBlockMaterial) {
        this.flagBlockMaterial = flagBlockMaterial;
    }

    public void setIndicatorSpawnLocation(Location indicatorSpawnLocation) {
        this.indicatorSpawnLocation = indicatorSpawnLocation;
    }

    public void setBaseSpawnLocation(Location baseSpawnLocation) {
        this.baseSpawnLocation = baseSpawnLocation;
    }

    public void setReturnSpawnLocations(List<Location> returnSpawnLocations) {
        this.returnSpawnLocations = returnSpawnLocations == null ? new ArrayList<>() : returnSpawnLocations;
    }
}
