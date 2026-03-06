package dev.tjxjnoobie.ctf.items.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;

public final class SpearLockedPlaceholderItem {

    // == Display name ==
    public static final Component NAME = Component.text("Spear Locked", NamedTextColor.RED);

    // == Lifecycle ==
    /**
     * Constructs a SpearLockedPlaceholderItem instance.
     */
    public SpearLockedPlaceholderItem() {
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @return Newly created value.
     */
    public ItemStack create() {
        // Build the locked placeholder item.
        return ItemBuilder.fromMaterial(Material.BARRIER)
            .displayName(NAME)
            .build();
    }

    /**
     * Returns the result of matches.
     *
     * @param item Item stack to inspect or update.
     * @return {@code true} when the candidate matches; otherwise {@code false}.
     */
    public boolean matches(ItemStack item) {
        boolean conditionResult1 = item == null || item.getType() != Material.BARRIER;
        // Guard: short-circuit when item == null || item.getType() != Material.BARRIER.
        if (conditionResult1) {
            return false;
        }
        ItemMeta meta = item.getItemMeta(); // Match by display name to identify placeholder.
        return meta != null && NAME.equals(meta.displayName());
    }
}
