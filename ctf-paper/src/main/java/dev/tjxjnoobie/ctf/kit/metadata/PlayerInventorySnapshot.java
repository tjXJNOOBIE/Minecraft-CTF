package dev.tjxjnoobie.ctf.kit.metadata;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Stores a snapshot of a player's inventory state.
 */
public final class PlayerInventorySnapshot {

    // == Snapshot data ==
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack[] extra;
    private final int foodLevel;
    private final float saturation;
    private final float exp;
    private final int level;

    // == Lifecycle ==
    private PlayerInventorySnapshot(ItemStack[] contents, ItemStack[] armor, ItemStack[] extra, int foodLevel, float saturation, float exp, int level) {
        // Capture all inventory and XP fields.
        this.contents = contents;
        this.armor = armor;
        this.extra = extra;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exp = exp;
        this.level = level;
    }

    // == Utilities ==
    /**
     * Returns the result of capture.
     *
     * @param player Player involved in this operation.
     * @return Result produced by this method.
     */
    public static PlayerInventorySnapshot capture(Player player) {
        if (player == null) {
            // Return an empty snapshot for null players.
            return new PlayerInventorySnapshot(new ItemStack[0], new ItemStack[0], new ItemStack[0], 20, 5.0f, 0.0f, 0);
        }

        ItemStack[] capturedContents = cloneItems(player.getInventory().getContents()); // Clone the player's inventory arrays.
        ItemStack[] capturedArmor = cloneItems(player.getInventory().getArmorContents());
        ItemStack[] capturedExtra = cloneItems(player.getInventory().getExtraContents());
        return new PlayerInventorySnapshot(
            capturedContents,
            capturedArmor,
            capturedExtra,
            player.getFoodLevel(),
            player.getSaturation(),
            player.getExp(),
            player.getLevel()
        );
    }

    /**
     * Executes restore.
     *
     * @param player Player involved in this operation.
     */
    public void restore(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        ItemStack[] restoredContents = cloneItems(contents); // Restore cloned inventory contents and XP values.
        ItemStack[] restoredArmor = cloneItems(armor);
        ItemStack[] restoredExtra = cloneItems(extra);
        player.getInventory().setContents(restoredContents);
        player.getInventory().setArmorContents(restoredArmor);
        player.getInventory().setExtraContents(restoredExtra);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExp(exp);
        player.setLevel(level);
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        // Guard: short-circuit when items == null.
        if (items == null) {
            return new ItemStack[0];
        }

        ItemStack[] clone = new ItemStack[items.length]; // Deep-clone item stacks to avoid shared references.
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            clone[i] = item == null ? null : item.clone();
        }
        return clone;
    }
}

