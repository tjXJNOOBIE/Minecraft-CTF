package dev.tjxjnoobie.ctf.items.kit;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.List;
import java.util.function.LongFunction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;

import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;

public final class ScoutSwordItem {

    // == Display constants ==
    public static final String NAME_TEXT = "Scout Tagger (Right Click)";
    public static final Component NAME = Component.text(NAME_TEXT, NamedTextColor.GREEN);
    public static final Component LORE = Component.text("Right-click: Throw Snowball", NamedTextColor.GRAY);

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final IInventoryUtils inventoryUtils;

    // == Lifecycle ==
    /**
     * Constructs a ScoutSwordItem instance.
     *
     * @param inventoryUtils Dependency responsible for inventory utils.
     */
    public ScoutSwordItem(IInventoryUtils inventoryUtils) {
        this.inventoryUtils = inventoryUtils;
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @return Newly created value.
     */
    public ItemStack create() {
        // Mark the sword as edible so Paper receives right-click-in-air use packets too.
        ItemStack item = ItemBuilder.fromMaterial(Material.WOODEN_SWORD)
            .displayName(NAME)
            .lore(List.of(LORE))
            .build();
        if (item != null) {
            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(72_000.0f)
                .animation(ItemUseAnimation.NONE)
                .sound(Key.key("minecraft", "entity.generic.eat"))
                .hasConsumeParticles(false)
                .build());
        }
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta != null) {
            FoodComponent foodComponent = meta.getFood();
            foodComponent.setNutrition(0);
            foodComponent.setSaturation(0.0f);
            foodComponent.setCanAlwaysEat(true);
            meta.setFood(foodComponent);
            item.setItemMeta(meta);
        }
        return item;
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

        ItemMeta meta = item.getItemMeta(); // Match by display name prefix for safety.
        boolean conditionResult2 = meta == null || meta.displayName() == null;
        // Guard: short-circuit when meta == null || meta.displayName() == null.
        if (conditionResult2) {
            return false;
        }

        String plain = PLAIN.serialize(meta.displayName());
        return plain.startsWith(NAME_TEXT) || plain.startsWith("Scout Tagger");
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
            "Scout Tagger (Right Click) (" + formatted + "s)",
            NamedTextColor.GREEN
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
        // Restore the base item name.
        inventoryUtils.setItemDisplayNameIfChanged(item, NAME);
    }
}
