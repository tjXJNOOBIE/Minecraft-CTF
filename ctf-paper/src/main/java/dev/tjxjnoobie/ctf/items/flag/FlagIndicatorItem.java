package dev.tjxjnoobie.ctf.items.flag;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;

public final class FlagIndicatorItem {

    // == Lifecycle ==
    /**
     * Constructs a FlagIndicatorItem instance.
     */
    public FlagIndicatorItem() {
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @return Newly created value.
     */
    public ItemStack create() {
        // Build a basic indicator item stack.
        return ItemBuilder.fromMaterial(Material.EMERALD).build();
    }

    /**
     * Returns the result of matches.
     *
     * @param item Item stack to inspect or update.
     * @return {@code true} when the candidate matches; otherwise {@code false}.
     */
    public boolean matches(ItemStack item) {
        // Match on base material for indicator detection.
        return item != null && item.getType() == Material.EMERALD;
    }
}
