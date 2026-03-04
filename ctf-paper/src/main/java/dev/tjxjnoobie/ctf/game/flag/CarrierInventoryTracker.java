package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.items.flag.FlagItem;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Owns carrier hotbar slot item replacement/restore logic.
 */
public final class CarrierInventoryTracker {

    // == Constants ==
    private static final int INVENTORY_START_SLOT = 9;
    private static final int INVENTORY_END_SLOT = 35;

    // == Runtime state ==
    private final Map<UUID, CarrierSlotState> replacedInventoryItemByCarrierId = new HashMap<>();
    private final IInventoryUtils inventoryUtils;

    // == Lifecycle ==
    /**
     * Constructs a CarrierInventoryTracker instance.
     *
     * @param inventoryUtils Dependency responsible for inventory utils.
     */
    public CarrierInventoryTracker(IInventoryUtils inventoryUtils) {
        this.inventoryUtils = inventoryUtils;
    }

    // == Getters ==
    public Set<UUID> getTrackedCarrierIdsSnapshot() {
        return new HashSet<>(replacedInventoryItemByCarrierId.keySet());
    }

    // == Utilities ==
    /**
     * Executes giveCarrierFlagItem.
     *
     * @param player Player involved in this operation.
     * @param flagMaterial Callback or dependency used during processing.
     * @param flagDisplayName User-facing display text.
     */
    public void giveCarrierFlagItem(Player player, Material flagMaterial, Component flagDisplayName) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        // Guard: short-circuit when flagMaterial == null.
        if (flagMaterial == null) {
            return;
        }

        int targetSlot = resolvePreferredInventorySlot(player);
        if (targetSlot < 0) {
            return;
        }

        CarrierSlotState existingState = replacedInventoryItemByCarrierId.get(player.getUniqueId());
        if (existingState == null || existingState.slot() != targetSlot) {
            ItemStack current = player.getInventory().getItem(targetSlot);
            replacedInventoryItemByCarrierId.put(player.getUniqueId(),
                    new CarrierSlotState(targetSlot, inventoryUtils.cloneItemOrNull(current)));
        }

        ItemStack flagItem = new FlagItem(flagMaterial, flagDisplayName).create();
        inventoryUtils.setItemIfDifferent(player, targetSlot, flagItem);
    }

    /**
     * Executes restoreCarrierFlagItem.
     *
     * @param player Player involved in this operation.
     */
    public void restoreCarrierFlagItem(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        CarrierSlotState previousState = replacedInventoryItemByCarrierId.remove(player.getUniqueId());
        if (previousState == null) {
            return;
        }
        inventoryUtils.setItemIfDifferent(player, previousState.slot(), previousState.previousItem());
    }

    /**
     * Executes enforceCarrierFlagHotbarSlot.
     *
     * @param player Player involved in this operation.
     * @param carriedFlagTeam Team identifier associated with the flag operation.
     * @param isFlagMaterial Boolean flag that controls this operation.
     * @param giveCarrierFlagItemAction Callback or dependency used during processing.
     */
    public void enforceCarrierFlagHotbarSlot(Player player,
                                             TeamId carriedFlagTeam,
                                             Predicate<Material> isFlagMaterial,
                                             BiConsumer<Player, TeamId> giveCarrierFlagItemAction) {
        // Guard: short-circuit when player == null || carriedFlagTeam == null.
        if (player == null || carriedFlagTeam == null) {
            return;
        }

        CarrierSlotState trackedState = replacedInventoryItemByCarrierId.get(player.getUniqueId());
        ItemStack flagItem = trackedState == null ? null : player.getInventory().getItem(trackedState.slot());
        boolean conditionResult1 = flagItem == null || isFlagMaterial == null || !isFlagMaterial.test(flagItem.getType());
        if (conditionResult1) {
            if (giveCarrierFlagItemAction != null) {
                giveCarrierFlagItemAction.accept(player, carriedFlagTeam);
            }
        }
    }

    private int resolvePreferredInventorySlot(Player player) {
        CarrierSlotState trackedState = replacedInventoryItemByCarrierId.get(player.getUniqueId());
        if (trackedState != null && trackedState.slot() >= INVENTORY_START_SLOT && trackedState.slot() <= INVENTORY_END_SLOT) {
            return trackedState.slot();
        }

        for (int slot = INVENTORY_START_SLOT; slot <= INVENTORY_END_SLOT; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || current.getType().isAir()) {
                return slot;
            }
        }

        return INVENTORY_START_SLOT;
    }

    private record CarrierSlotState(int slot, ItemStack previousItem) {
    }
}
