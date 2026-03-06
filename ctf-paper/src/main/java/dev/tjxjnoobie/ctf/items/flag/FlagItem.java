package dev.tjxjnoobie.ctf.items.flag;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;

public final class FlagItem {

    // == Configuration ==
    private final Material material;
    private final Component displayName;

    // == Lifecycle ==
    /**
     * Constructs a FlagItem instance.
     *
     * @param material Bukkit type used by this operation.
     * @param displayName User-facing display text.
     */
    public FlagItem(Material material, Component displayName) {
        // Capture desired material and name.
        this.material = material;
        this.displayName = displayName;
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @return Newly created value.
     */
    public ItemStack create() {
        // Guard: short-circuit when material == null.
        if (material == null) {
            return null;
        }
        // Build the flag item stack.
        return ItemBuilder.fromMaterial(material)
            .displayName(displayName)
            .build();
    }

    /**
     * Returns the result of matches.
     *
     * @param item Item stack to inspect or update.
     * @return {@code true} when the candidate matches; otherwise {@code false}.
     */
    public boolean matches(ItemStack item) {
        boolean conditionResult1 = item == null || material == null || item.getType() != material;
        // Guard: short-circuit when item == null || material == null || item.getType() != material.
        if (conditionResult1) {
            return false;
        }
        // Guard: short-circuit when displayName == null.
        if (displayName == null) {
            return true;
        }
        ItemMeta meta = item.getItemMeta(); // Match by display name when configured.
        return meta != null && displayName.equals(meta.displayName());
    }
}
