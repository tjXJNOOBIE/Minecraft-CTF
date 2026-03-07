package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.items.kit.RangerIconItem;
import dev.tjxjnoobie.ctf.items.kit.ScoutIconItem;
import dev.tjxjnoobie.ctf.items.kit.ScoutSwordItem;
import dev.tjxjnoobie.ctf.items.kit.SpearLockedPlaceholderItem;
import dev.tjxjnoobie.ctf.items.kit.SpearReturningPlaceholderItem;
import dev.tjxjnoobie.ctf.kit.KitSelectorGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Dependency-access surface for kit selection UI and kit-backed item presenters.
 */
public interface KitUiDependencyAccess {
    default KitSelectorGUI getKitSelectorGui() { return DependencyLoaderAccess.findInstance(KitSelectorGUI.class); }
    default ScoutIconItem getScoutIconItem() { return DependencyLoaderAccess.findInstance(ScoutIconItem.class); }
    default RangerIconItem getRangerIconItem() { return DependencyLoaderAccess.findInstance(RangerIconItem.class); }
    default ScoutSwordItem getScoutSwordItem() { return DependencyLoaderAccess.findInstance(ScoutSwordItem.class); }
    default HomingSpearItem getHomingSpearItem() { return DependencyLoaderAccess.findInstance(HomingSpearItem.class); }
    default SpearLockedPlaceholderItem getSpearLockedPlaceholderItem() { return DependencyLoaderAccess.findInstance(SpearLockedPlaceholderItem.class); }
    default SpearReturningPlaceholderItem getSpearReturningPlaceholderItem() { return DependencyLoaderAccess.findInstance(SpearReturningPlaceholderItem.class); }

    default void openKitSelector(Player player, boolean allowDuringMatch) {
        getKitSelectorGui().openKitSelector(player, allowDuringMatch);
    }

    default ItemStack createScoutIconItem() {
        return getScoutIconItem().create();
    }

    default ItemStack createRangerIconItem() {
        return getRangerIconItem().create();
    }

    default ItemStack createScoutSwordItem() {
        return getScoutSwordItem().create();
    }

    default ItemStack createHomingSpearItem() {
        return getHomingSpearItem().create();
    }

    default boolean homingSpearItemMatches(ItemStack item) {
        HomingSpearItem spearItem = getHomingSpearItem();
        return spearItem != null && spearItem.matches(item);
    }

    default ItemStack createSpearLockedPlaceholderItem() {
        return getSpearLockedPlaceholderItem().create();
    }

    default ItemStack createSpearReturningPlaceholderItem() {
        return getSpearReturningPlaceholderItem().create();
    }
}
