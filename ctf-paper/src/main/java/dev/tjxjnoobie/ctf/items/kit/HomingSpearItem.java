package dev.tjxjnoobie.ctf.items.kit;

import java.util.function.LongFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import dev.tjxjnoobie.ctf.util.bukkit.inventory.InventoryUtils;

/**
 * Factory and matcher for the custom homing-spear kit item.
 */
public final class HomingSpearItem {

    // == Display constants ==
    public static final String NAME_TEXT = "Homing Spear";
    public static final String READY_NAME_TEXT = NAME_TEXT + " (F to Throw)";
    public static final Component NAME = Component.text(READY_NAME_TEXT, NamedTextColor.AQUA);
    public static final int CUSTOM_MODEL_DATA = 91021;
    public static final HomingSpearItem INSTANCE = new HomingSpearItem(new InventoryUtils());

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final IInventoryUtils inventoryUtils;

    // == Lifecycle ==
    /**
     * Constructs a HomingSpearItem instance.
     *
     * @param inventoryUtils Dependency responsible for inventory utils.
     */
    public HomingSpearItem(IInventoryUtils inventoryUtils) {
        this.inventoryUtils = inventoryUtils;
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @return Newly created value.
     */
    public ItemStack create() {
        // Build the spear item stack.
        return ItemBuilder.fromMaterial(Material.TRIDENT)
            .displayName(NAME)
            .customModelData(CUSTOM_MODEL_DATA)
            .build();
    }

    /**
     * Returns the result of matches.
     *
     * @param item Item stack to inspect or update.
     * @return {@code true} when the candidate matches; otherwise {@code false}.
     */
    public boolean matches(ItemStack item) {
        boolean conditionResult1 = item == null || item.getType() != Material.TRIDENT;
        // Guard: short-circuit when item == null || item.getType() != Material.TRIDENT.
        if (conditionResult1) {
            return false;
        }

        ItemMeta meta = item.getItemMeta(); // Validate model data and display name.
        boolean conditionResult2 = meta == null || !meta.hasCustomModelData();
        // Guard: short-circuit when meta == null || !meta.hasCustomModelData().
        if (conditionResult2) {
            return false;
        }
        boolean conditionResult3 = meta.getCustomModelData() != CUSTOM_MODEL_DATA;
        // Guard: short-circuit when meta.getCustomModelData() != CUSTOM_MODEL_DATA.
        if (conditionResult3) {
            return false;
        }

        boolean conditionResult4 = meta.displayName() == null;
        // Guard: short-circuit when meta.displayName() == null.
        if (conditionResult4) {
            return false;
        }
        String plain = PLAIN.serialize(meta.displayName());
        return plain.startsWith(NAME_TEXT);
    }

    /**
     * Executes applyCooldownName.
     *
     * @param item Item stack to inspect or update.
     * @param remainingMs Timestamp or duration in milliseconds.
     * @param formatter Formatter used to render user-facing time values.
     */
    public void applyCooldownName(ItemStack item, long remainingMs, LongFunction<String> formatter) {
        // Guard: short-circuit when item == null.
        if (item == null) {
            return;
        }
        if (remainingMs <= 0L) {
            restoreName(item);
            return;
        }
        // Guard: short-circuit when formatter == null.
        if (formatter == null) {
            return;
        }
        String formatted = formatter.apply(remainingMs); // Format and apply the cooldown suffix.
        // Guard: short-circuit when formatted == null.
        if (formatted == null) {
            return;
        }
        inventoryUtils.setItemDisplayNameIfChanged(item, Component.text(
            NAME_TEXT + " (" + formatted + "s)",
            NamedTextColor.AQUA
        ));
    }

    /**
     * Executes restoreName.
     *
     * @param item Item stack to inspect or update.
     */
    public void restoreName(ItemStack item) {
        // Guard: short-circuit when item == null.
        if (item == null) {
            return;
        }
        // Restore the base name without cooldown suffix.
        inventoryUtils.setItemDisplayNameIfChanged(item, NAME);
    }
}
