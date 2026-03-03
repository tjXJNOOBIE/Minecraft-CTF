package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

/**
 * Renders an item display indicator above active flags.
 */
public final class FlagIndicator {
    private static final String FLAG_INDICATOR_TAG = "ctf_flag_indicator";
    private static final double LEGACY_MATCH_RADIUS_SQUARED = 9.0;
    private static final String LOG_PREFIX = "[CTF] [FlagIndicator] ";

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final Map<String, UUID> indicatorEntityIdsByTeam = new HashMap<>();

    public FlagIndicator(JavaPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    /**
     * Spawns an indicator at the provided indicator location.
     */
    public void spawnFlagIndicatorForTeam(String teamName, Location indicatorLocation) {
        removeFlagIndicatorForTeam(teamName);
        if (indicatorLocation == null || indicatorLocation.getWorld() == null) {
            return;
        }

        ItemDisplay indicatorDisplay = indicatorLocation.getWorld().spawn(indicatorLocation, ItemDisplay.class);
        indicatorDisplay.setItemStack(new ItemStack(Material.EMERALD));
        indicatorDisplay.setPersistent(false);
        indicatorDisplay.setInvulnerable(true);
        indicatorDisplay.setBillboard(Display.Billboard.CENTER);
        indicatorDisplay.setBrightness(new Display.Brightness(15, 15));
        indicatorDisplay.setViewRange(64.0f);
        indicatorDisplay.setGlowing(true);
        indicatorDisplay.addScoreboardTag(FLAG_INDICATOR_TAG);
        teamManager.addEntityToTeam(teamName, indicatorDisplay);

        indicatorEntityIdsByTeam.put(teamName, indicatorDisplay.getUniqueId());
        syncVisibility(teamManager.getJoinedPlayers());
        Bukkit.getLogger().info(LOG_PREFIX + "Indicator spawned - team=" + teamName);
    }

    /**
     * Removes a team's indicator if present.
     */
    public void removeFlagIndicatorForTeam(String teamName) {
        UUID indicatorEntityId = indicatorEntityIdsByTeam.remove(teamName);
        if (indicatorEntityId == null) {
            return;
        }

        Entity indicatorEntity = Bukkit.getEntity(indicatorEntityId);
        if (indicatorEntity != null) {
            teamManager.removeEntityFromTeams(indicatorEntity);
            indicatorEntity.remove();
        }
        Bukkit.getLogger().info(LOG_PREFIX + "Indicator removed - team=" + teamName);
    }

    /**
     * Removes all flag indicators.
     */
    public void removeAllFlagIndicators() {
        for (String teamName : new ArrayList<>(indicatorEntityIdsByTeam.keySet())) {
            removeFlagIndicatorForTeam(teamName);
        }
    }

    /**
     * Removes all persisted/stale indicators from startup before re-rendering.
     * Includes legacy entities that were created before indicator tagging.
     */
    public void resetFlagIndicators(List<Location> indicatorLocations) {
        removeAllFlagIndicators();
        indicatorEntityIdsByTeam.clear();

        if (indicatorLocations == null || indicatorLocations.isEmpty()) {
            return;
        }

        Set<World> worlds = new HashSet<>();
        for (Location indicator : indicatorLocations) {
            if (indicator != null && indicator.getWorld() != null) {
                worlds.add(indicator.getWorld());
            }
        }

        int removed = 0;
        for (World world : worlds) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (!isStaleIndicator(display, indicatorLocations)) {
                    continue;
                }
                teamManager.removeEntityFromTeams(display);
                display.remove();
                removed++;
            }
        }

        if (removed > 0) {
            Bukkit.getLogger().info(LOG_PREFIX + "Reset stale indicators on enable - removed=" + removed);
        }
    }

    /**
     * Returns true when a team's indicator entity currently exists.
     */
    public boolean hasFlagIndicatorForTeam(String teamName) {
        UUID indicatorEntityId = indicatorEntityIdsByTeam.get(teamName);
        if (indicatorEntityId == null) {
            return false;
        }

        Entity indicatorEntity = Bukkit.getEntity(indicatorEntityId);
        if (indicatorEntity == null || !indicatorEntity.isValid()) {
            if (indicatorEntity != null) {
                teamManager.removeEntityFromTeams(indicatorEntity);
            }
            indicatorEntityIdsByTeam.remove(teamName);
            return false;
        }
        return true;
    }

    /**
     * Ensures arena players and admins can see indicators.
     */
    public void syncVisibility(List<Player> arenaPlayers) {
        if (plugin == null) {
            return;
        }

        Set<UUID> arenaViewerIds = arenaPlayers == null ? Set.of()
            : arenaPlayers.stream().map(Player::getUniqueId).collect(Collectors.toSet());

        for (UUID indicatorId : indicatorEntityIdsByTeam.values()) {
            Entity indicator = Bukkit.getEntity(indicatorId);
            if (!(indicator instanceof ItemDisplay) || !indicator.isValid()) {
                continue;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean canView = arenaViewerIds.contains(player.getUniqueId()) || player.hasPermission("ctf.admin");
                if (canView) {
                    player.showEntity(plugin, indicator);
                } else {
                    player.hideEntity(plugin, indicator);
                }
            }
        }
    }

    private boolean isStaleIndicator(ItemDisplay display, List<Location> indicatorLocations) {
        if (display.getScoreboardTags().contains(FLAG_INDICATOR_TAG)) {
            return true;
        }

        ItemStack stack = display.getItemStack();
        if (stack == null || stack.getType() != Material.EMERALD) {
            return false;
        }

        Location indicatorLocation = display.getLocation();
        for (Location indicator : indicatorLocations) {
            if (indicator == null || indicator.getWorld() == null) {
                continue;
            }
            if (!indicator.getWorld().equals(indicatorLocation.getWorld())) {
                continue;
            }

            if (indicator.distanceSquared(indicatorLocation) <= LEGACY_MATCH_RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
    }
}


