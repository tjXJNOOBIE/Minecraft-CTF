package dev.tjxjnoobie.ctf.game.combat.util;

import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Shared event-side helpers for homing-spear inventory protections.
 */
public final class HomingSpearEventUtil {

    private HomingSpearEventUtil() {
    }

    /**
     * Resolves an arena player from an event candidate object.
     *
     * @param candidate Event-side actor candidate.
     * @param arenaPlayerCheck Predicate used to validate arena membership.
     * @return Arena player instance, or {@code null} when the candidate does not qualify.
     */
    public static Player resolveArenaPlayer(Object candidate, Predicate<Player> arenaPlayerCheck) {
        if (!(candidate instanceof Player player) || arenaPlayerCheck == null || !arenaPlayerCheck.test(player)) {
            return null;
        }
        return player;
    }

    /**
     * Returns whether any of the given items is a homing-spear placeholder.
     *
     * @param homingSpearInventoryUtils Inventory helper used for placeholder detection.
     * @param items Items to inspect.
     * @return {@code true} when at least one item is a placeholder.
     */
    public static boolean containsPlaceholderItem(HomingSpearInventoryUtils homingSpearInventoryUtils,
                                                  ItemStack... items) {
        if (homingSpearInventoryUtils == null || items == null) {
            return false;
        }

        for (ItemStack item : items) {
            if (homingSpearInventoryUtils.isPlaceholderItem(item)) {
                return true;
            }
        }
        return false;
    }
}
