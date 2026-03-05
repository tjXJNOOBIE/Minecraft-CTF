package dev.tjxjnoobie.ctf.game.flag;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;

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
public final class FlagIndicator implements BukkitEffectsUtil {

    // == Constants ==
    private static final String FLAG_INDICATOR_TAG = "ctf_flag_indicator";
    private static final double LEGACY_MATCH_RADIUS_SQUARED = 9.0;
    private static final String LOG_PREFIX = "[CTF] [FlagIndicator] ";
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final FlagIndicatorItem flagIndicatorItem;

    // == Runtime state ==
    private final Map<String, UUID> indicatorEntityIdsByTeam = new HashMap<>();

    // == Lifecycle ==
    /**
     * Constructs a FlagIndicator instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     * @param teamManager Dependency responsible for team manager.
     * @param flagIndicatorItem Callback or dependency used during processing.
     */
    public FlagIndicator(JavaPlugin plugin, TeamManager teamManager, FlagIndicatorItem flagIndicatorItem) {
        // Capture plugin and team manager references.
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.flagIndicatorItem = flagIndicatorItem;
    }

    // == Utilities ==
    /**
     * Executes spawnFlagIndicatorForTeam.
     *
     * @param teamName User-facing display text.
     * @param indicatorLocation Location used for flag/base placement or fallback logic.
     */
    public void spawnFlagIndicatorForTeam(String teamName, Location indicatorLocation) {
        removeFlagIndicatorForTeam(teamName);
        boolean conditionResult1 = indicatorLocation == null || indicatorLocation.getWorld() == null;
        // Guard: short-circuit when indicatorLocation == null || indicatorLocation.getWorld() == null.
        if (conditionResult1) {
            return;
        }

        ItemDisplay indicatorDisplay = indicatorLocation.getWorld().spawn(indicatorLocation, ItemDisplay.class); // Spawn and configure the item display indicator.
        indicatorDisplay.setItemStack(flagIndicatorItem.create());
        indicatorDisplay.setPersistent(false);
        indicatorDisplay.setInvulnerable(true);
        indicatorDisplay.setBillboard(Display.Billboard.CENTER);
        indicatorDisplay.setBrightness(new Display.Brightness(15, 15));
        indicatorDisplay.setViewRange(64.0f);
        setGlowing(indicatorDisplay, true);
        indicatorDisplay.addScoreboardTag(FLAG_INDICATOR_TAG);
        teamManager.addEntityToTeam(teamName, indicatorDisplay);

        indicatorEntityIdsByTeam.put(teamName, indicatorDisplay.getUniqueId());
        syncVisibility(teamManager.getJoinedPlayers());
        Bukkit.getLogger().info(LOG_PREFIX + "Indicator spawned - team=" + teamName);
    }

    /**
     * Executes removeFlagIndicatorForTeam.
     *
     * @param teamName User-facing display text.
     */
    public void removeFlagIndicatorForTeam(String teamName) {
        UUID indicatorEntityId = indicatorEntityIdsByTeam.remove(teamName);
        // Guard: short-circuit when indicatorEntityId == null.
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
     * Executes removeAllFlagIndicators.
     */
    public void removeAllFlagIndicators() {
        for (String teamName : new ArrayList<>(indicatorEntityIdsByTeam.keySet())) {
            removeFlagIndicatorForTeam(teamName);
        }
    }

    /**
     * Executes resetFlagIndicators.
     *
     * @param indicatorLocations Collection or state object updated by this operation.
     */
    public void resetFlagIndicators(List<Location> indicatorLocations) {
        removeAllFlagIndicators();
        indicatorEntityIdsByTeam.clear();

        boolean conditionResult2 = indicatorLocations == null || indicatorLocations.isEmpty();
        // Guard: short-circuit when indicatorLocations == null || indicatorLocations.isEmpty().
        if (conditionResult2) {
            return;
        }

        Set<World> worlds = new HashSet<>();
        for (Location indicator : indicatorLocations) {
            boolean conditionResult3 = indicator != null && indicator.getWorld() != null;
            if (conditionResult3) {
                worlds.add(indicator.getWorld());
            }
        }

        int removed = 0;
        for (World world : worlds) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                boolean staleIndicator = isStaleIndicator(display, indicatorLocations);
                // Guard: short-circuit when !staleIndicator.
                if (!staleIndicator) {
                    continue;
                }
                // Remove legacy or stale indicators.
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
     * Executes syncVisibility.
     *
     * @param arenaPlayers Collection or state object updated by this operation.
     */
    public void syncVisibility(List<Player> arenaPlayers) {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }

        // Build the allowed viewer set for arena players.
        Set<UUID> arenaViewerIds = arenaPlayers == null ? Set.of()
                : arenaPlayers.stream().map(Player::getUniqueId).collect(Collectors.toSet());

        for (UUID indicatorId : indicatorEntityIdsByTeam.values()) {
            Entity indicator = Bukkit.getEntity(indicatorId);
            boolean conditionResult5 = !(indicator instanceof ItemDisplay) || !indicator.isValid();
            // Guard: short-circuit when !(indicator instanceof ItemDisplay) || !indicator.isValid().
            if (conditionResult5) {
                continue;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean canView = arenaViewerIds.contains(player.getUniqueId()) || player.hasPermission(CTFKeys.permissionAdmin());
                if (canView) {
                    player.showEntity(plugin, indicator);
                } else {
                    player.hideEntity(plugin, indicator);
                }
            }
        }
    }

    // == Predicates ==
    public boolean hasFlagIndicatorForTeam(String teamName) {
        UUID indicatorEntityId = indicatorEntityIdsByTeam.get(teamName);
        // Guard: short-circuit when indicatorEntityId == null.
        if (indicatorEntityId == null) {
            return false;
        }

        Entity indicatorEntity = Bukkit.getEntity(indicatorEntityId);
        boolean conditionResult4 = indicatorEntity == null || !indicatorEntity.isValid();
        if (conditionResult4) {
            if (indicatorEntity != null) {
                teamManager.removeEntityFromTeams(indicatorEntity);
            }
            indicatorEntityIdsByTeam.remove(teamName);
            return false;
        }
        return true;
    }

    private boolean isStaleIndicator(ItemDisplay display, List<Location> indicatorLocations) {
        boolean conditionResult6 = display.getScoreboardTags().contains(FLAG_INDICATOR_TAG);
        // Guard: short-circuit when display.getScoreboardTags().contains(FLAG_INDICATOR_TAG).
        if (conditionResult6) {
            return true;
        }

        ItemStack stack = display.getItemStack();
        boolean conditionResult7 = stack == null || stack.getType() != Material.EMERALD;
        // Guard: short-circuit when stack == null || stack.getType() != Material.EMERALD.
        if (conditionResult7) {
            return false;
        }

        Location indicatorLocation = display.getLocation();
        for (Location indicator : indicatorLocations) {
            boolean conditionResult8 = indicator == null || indicator.getWorld() == null;
            // Guard: short-circuit when indicator == null || indicator.getWorld() == null.
            if (conditionResult8) {
                continue;
            }
            boolean conditionResult9 = !indicator.getWorld().equals(indicatorLocation.getWorld());
            // Guard: short-circuit when !indicator.getWorld().equals(indicatorLocation.getWorld()).
            if (conditionResult9) {
                continue;
            }

            boolean conditionResult10 = indicator.distanceSquared(indicatorLocation) <= LEGACY_MATCH_RADIUS_SQUARED;
            // Guard: short-circuit when indicator.distanceSquared(indicatorLocation) <= LEGACY_MATCH_RADIUS_SQUARED.
            if (conditionResult10) {
                return true;
            }
        }
        return false;
    }
}

