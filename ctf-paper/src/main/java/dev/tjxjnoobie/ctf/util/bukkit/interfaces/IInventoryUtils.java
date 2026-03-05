package dev.tjxjnoobie.ctf.util.bukkit.interfaces;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryGuardUtil;

/**
 * Concrete inventory helpers built on top of guard utilities.
 */
public interface IInventoryUtils extends InventoryGuardUtil {
    default ItemStack[] cloneItems(ItemStack[] items) {
        // Guard: short-circuit when items == null.
        if (items == null) {
            return new ItemStack[0];
        }

        ItemStack[] clone = new ItemStack[items.length]; // Clone each item while preserving array size.
        for (int i = 0; i < items.length; i++) {
            clone[i] = cloneItemOrNull(items[i]);
        }
        return clone;
    }

    default void setItemDisplayNameIfChanged(ItemStack item, Component displayName) {
        // Guard: short-circuit when item == null || displayName == null.
        if (item == null || displayName == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        // Guard: short-circuit when meta == null.
        if (meta == null) {
            return;
        }
        boolean conditionResult1 = displayName.equals(meta.displayName());
        // Guard: short-circuit when displayName.equals(meta.displayName()).
        if (conditionResult1) {
            return;
        }
        // Update metadata only when the display name changes.
        meta.displayName(displayName);
        item.setItemMeta(meta);
    }

}
