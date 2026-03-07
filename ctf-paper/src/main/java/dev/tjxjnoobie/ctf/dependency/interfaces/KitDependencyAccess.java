package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import org.bukkit.entity.Player;

/**
 * Dependency-access surface for kit selection and loadout behavior.
 */
public interface KitDependencyAccess {
    default KitSelectionHandler getKitSelectionHandler() { return DependencyLoaderAccess.findInstance(KitSelectionHandler.class); }

    default boolean kitHasSelection(Player player) {
        return getKitSelectionHandler().hasSelection(player);
    }

    default KitType kitGetSelectedKit(Player player) {
        return getKitSelectionHandler().getSelectedKit(player);
    }

    default void kitApplyLoadout(Player player, KitType kitType) {
        getKitSelectionHandler().applyKitLoadout(player, kitType);
    }

    default void kitSelect(Player player, KitType kitType) {
        getKitSelectionHandler().selectKit(player, kitType);
    }

    default void kitRestoreAll() {
        getKitSelectionHandler().restoreAll();
    }

    default void kitClearAll() {
        getKitSelectionHandler().clearAll();
    }

    default boolean kitIsRanger(Player player) {
        return getKitSelectionHandler().isRanger(player);
    }

    default boolean kitIsScout(Player player) {
        return getKitSelectionHandler().isScout(player);
    }
}
