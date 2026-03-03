package dev.tjxjnoobie.ctf.kit.metadata;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Stores a snapshot of a player's inventory state.
 */
public final class InventorySnapshot {
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack[] extra;
    private final int foodLevel;
    private final float saturation;
    private final float exp;
    private final int level;

    private InventorySnapshot(ItemStack[] contents, ItemStack[] armor, ItemStack[] extra, int foodLevel, float saturation, float exp, int level) {
        this.contents = contents;
        this.armor = armor;
        this.extra = extra;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exp = exp;
        this.level = level;
    }

    /**
     * Captures the current inventory contents of a player.
     */
    public static InventorySnapshot capture(Player player) {
        if (player == null) {
            return new InventorySnapshot(new ItemStack[0], new ItemStack[0], new ItemStack[0], 20, 5.0f, 0.0f, 0);
        }

        return new InventorySnapshot(
            cloneItems(player.getInventory().getContents()),
            cloneItems(player.getInventory().getArmorContents()),
            cloneItems(player.getInventory().getExtraContents()),
            player.getFoodLevel(),
            player.getSaturation(),
            player.getExp(),
            player.getLevel()
        );
    }

    /**
     * Restores the snapshot into the player's inventory.
     */
    public void restore(Player player) {
        if (player == null) {
            return;
        }

        player.getInventory().setContents(cloneItems(contents));
        player.getInventory().setArmorContents(cloneItems(armor));
        player.getInventory().setExtraContents(cloneItems(extra));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExp(exp);
        player.setLevel(level);
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) {
            return new ItemStack[0];
        }

        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            clone[i] = item == null ? null : item.clone();
        }
        return clone;
    }
}

