package dev.tjxjnoobie.ctf.kit;

import dev.tjxjnoobie.ctf.items.kit.RangerIconItem;
import dev.tjxjnoobie.ctf.items.kit.ScoutIconItem;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

/**
 * Shared kit selector menu layout and slot semantics.
 */
public final class KitSelectorMenu {
    public static final Component TITLE = Component.text("Select Kit");
    /**
     * Constructs a KitSelectorMenu instance.
     */
    public static final int SIZE = 9;
    public static final int SCOUT_SLOT = 3;
    public static final int RANGER_SLOT = 5;

    // == Lifecycle ==
    private KitSelectorMenu() {
    }

    // == Utilities ==
    /**
     * Returns the result of create.
     *
     * @param scoutIconItem Service dependency required by this operation.
     * @param rangerIconItem Service dependency required by this operation.
     * @return Newly created value.
     */
    public static Inventory create(ScoutIconItem scoutIconItem, RangerIconItem rangerIconItem) {
        // Guard: short-circuit when scoutIconItem == null || rangerIconItem == null.
        if (scoutIconItem == null || rangerIconItem == null) {
            return null;
        }
        Inventory inventory = Bukkit.createInventory(null, SIZE, TITLE);
        inventory.setItem(SCOUT_SLOT, scoutIconItem.create());
        inventory.setItem(RANGER_SLOT, rangerIconItem.create());
        return inventory;
    }

    /**
     * Returns the result of kitForSlot.
     *
     * @param slot Inventory slot or model metadata value.
     * @return Result produced by this method.
     */
    public static KitType kitForSlot(int slot) {
        // Guard: short-circuit when slot == SCOUT_SLOT.
        if (slot == SCOUT_SLOT) {
            return KitType.SCOUT;
        }
        // Guard: short-circuit when slot == RANGER_SLOT.
        if (slot == RANGER_SLOT) {
            return KitType.RANGER;
        }
        return null;
    }

    /**
     * Returns the result of labelFor.
     *
     * @param kitType Domain enum value used to control behavior.
     * @return Result produced by this method.
     */
    public static String labelFor(KitType kitType) {
        // Guard: short-circuit when kitType == KitType.RANGER.
        if (kitType == KitType.RANGER) {
            return "Ranger";
        }
        return "Scout";
    }

    // == Predicates ==
    public static boolean isTitle(Component title) {
        return TITLE.equals(title);
    }
}
