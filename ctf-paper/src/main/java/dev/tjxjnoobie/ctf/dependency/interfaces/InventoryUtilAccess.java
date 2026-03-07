package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * DI access layer for shared inventory utility logic.
 */
public interface InventoryUtilAccess {
    default IInventoryUtils getInventoryUtils() { return DependencyLoaderAccess.findInstance(IInventoryUtils.class); }

    default boolean inventoryIsHoldingHotbarSlot(Player player, int slot) {
        return getInventoryUtils().isHoldingHotbarSlot(player, slot);
    }

    default boolean inventoryIsNewHeldHotbarSlot(PlayerItemHeldEvent event, int slot) {
        return getInventoryUtils().isNewHeldHotbarSlot(event, slot);
    }
}
