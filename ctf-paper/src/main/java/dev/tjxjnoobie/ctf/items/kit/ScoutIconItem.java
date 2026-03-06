package dev.tjxjnoobie.ctf.items.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;

public final class ScoutIconItem {

    // == Display name ==
    public static final Component NAME = Component.text("Scout", NamedTextColor.GREEN, TextDecoration.BOLD);

    // == Lifecycle ==
    /**
     * Constructs a ScoutIconItem instance.
     */
    public ScoutIconItem() {
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @return Newly created value.
     */
    public ItemStack create() {
        // Build the scout kit icon.
        return ItemBuilder.fromMaterial(Material.WOODEN_SWORD)
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
        boolean conditionResult1 = item == null || item.getType() != Material.WOODEN_SWORD;
        // Guard: short-circuit when item == null || item.getType() != Material.WOODEN_SWORD.
        if (conditionResult1) {
            return false;
        }
        ItemMeta meta = item.getItemMeta(); // Match by display name to avoid false positives.
        return meta != null && NAME.equals(meta.displayName());
    }
}
