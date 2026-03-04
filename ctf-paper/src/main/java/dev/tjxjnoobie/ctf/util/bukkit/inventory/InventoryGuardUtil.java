package dev.tjxjnoobie.ctf.util.bukkit.inventory;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Thin inventory guard helpers.
 */
public interface InventoryGuardUtil {
    int MIN_SLOT = 0;
    int MAX_SLOT = 40;

    default boolean isSlotInRange(int slot) {
        // Guard against invalid inventory slots.
        return slot >= MIN_SLOT && slot <= MAX_SLOT;
    }

    default ItemStack cloneItemOrNull(ItemStack item) {
        return item == null ? null : item.clone(); // Avoid NPEs by cloning only when present.
    }

    default void setItemIfDifferent(Player player, int slot, ItemStack item) {
        boolean conditionResult1 = player == null || !isSlotInRange(slot);
        // Guard: short-circuit when player == null || !isSlotInRange(slot).
        if (conditionResult1) {
            return;
        }

        ItemStack current = player.getInventory().getItem(slot);
        boolean equalsResult = Objects.equals(current, item);
        // Guard: short-circuit when equalsResult.
        if (equalsResult) {
            return;
        }
        // Apply only when the target item actually changes.
        player.getInventory().setItem(slot, cloneItemOrNull(item));
    }

    default boolean hasMaterialInSlot(Player player, int slot, Material material) {
        boolean conditionResult2 = player == null || material == null || !isSlotInRange(slot);
        // Guard: short-circuit when player == null || material == null || !isSlotInRange(slot).
        if (conditionResult2) {
            return false;
        }
        ItemStack item = player.getInventory().getItem(slot);
        // Match by material in the requested slot.
        return item != null && item.getType() == material;
    }

    default boolean isHoldingHotbarSlot(Player player, int slot) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return false;
        }
        // Compare the currently held hotbar slot.
        return player.getInventory().getHeldItemSlot() == slot;
    }

    default boolean isNewHeldHotbarSlot(PlayerItemHeldEvent event, int slot) {
        // Guard: short-circuit when event == null.
        if (event == null) {
            return false;
        }
        // Validate the newly selected hotbar slot.
        return event.getNewSlot() == slot;
    }
}
