package dev.tjxjnoobie.ctf.game.combat.util;

import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.items.kit.SpearLockedPlaceholderItem;
import dev.tjxjnoobie.ctf.items.kit.SpearReturningPlaceholderItem;
import dev.tjxjnoobie.ctf.kit.KitSlots;
import java.util.UUID;
import java.util.function.LongFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Shared homing-spear inventory and placeholder operations.
 */
public final class HomingSpearInventoryUtils {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private final HomingSpearItem homingSpearItem;
    private final SpearLockedPlaceholderItem spearLockedPlaceholderItem;
    private final SpearReturningPlaceholderItem spearReturningPlaceholderItem;

    /**
     * Constructs a HomingSpearInventoryUtils instance.
     *
     * @param homingSpearItem Item factory for the base spear item.
     * @param spearLockedPlaceholderItem Placeholder shown while the spear is active.
     * @param spearReturningPlaceholderItem Placeholder shown while the spear is returning.
     */
    public HomingSpearInventoryUtils(HomingSpearItem homingSpearItem,
                                     SpearLockedPlaceholderItem spearLockedPlaceholderItem,
                                     SpearReturningPlaceholderItem spearReturningPlaceholderItem) {
        this.homingSpearItem = homingSpearItem != null ? homingSpearItem : HomingSpearItem.INSTANCE;
        this.spearLockedPlaceholderItem = spearLockedPlaceholderItem != null
                ? spearLockedPlaceholderItem
                : new SpearLockedPlaceholderItem();
        this.spearReturningPlaceholderItem = spearReturningPlaceholderItem != null
                ? spearReturningPlaceholderItem
                : new SpearReturningPlaceholderItem();
    }

    /**
     * Returns whether the item is one of the spear placeholder variants.
     *
     * @param item Inventory item to inspect.
     * @return {@code true} when the item is a spear placeholder.
     */
    public boolean isPlaceholderItem(ItemStack item) {
        if (item == null || item.getType() != Material.BARRIER) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) {
            return true;
        }

        Component displayName = meta.displayName();
        if (SpearLockedPlaceholderItem.NAME.equals(displayName)) {
            return true;
        }
        if (SpearReturningPlaceholderItem.NAME.equals(displayName)) {
            return true;
        }

        String plain = PLAIN_TEXT.serialize(displayName);
        return plain.startsWith(HomingSpearItem.NAME_TEXT);
    }

    /**
     * Returns whether the player is currently holding or slotting the homing spear.
     *
     * @param player Player to inspect.
     * @return {@code true} when the spear is in the player's active combat flow.
     */
    public boolean isHoldingSpear(Player player) {
        if (player == null) {
            return false;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (homingSpearItem.matches(mainHand)) {
            return true;
        }
        ItemStack slotItem = player.getInventory().getItem(KitSlots.SPEAR_SLOT);
        return homingSpearItem.matches(slotItem);
    }

    /**
     * Replaces the spear slot with the locked placeholder.
     *
     * @param shooter Player whose spear slot should show the active placeholder.
     */
    public void setSpearPlaceholder(Player shooter) {
        if (shooter == null) {
            return;
        }
        ItemStack placeholder = spearLockedPlaceholderItem.create();
        if (placeholder == null) {
            return;
        }
        shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, placeholder);
    }

    /**
     * Replaces the spear slot with the returning placeholder.
     *
     * @param shooter Player whose spear slot should show the returning placeholder.
     */
    public void setReturnPlaceholder(Player shooter) {
        if (shooter == null) {
            return;
        }
        ItemStack placeholder = spearReturningPlaceholderItem.create();
        if (placeholder == null) {
            return;
        }
        shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, placeholder);
    }

    /**
     * Restores the player's spear item into the dedicated kit slot.
     *
     * @param shooterId Player id whose spear slot should be restored.
     */
    public void restoreSpearItem(UUID shooterId) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) {
            return;
        }

        ItemStack slotItem = shooter.getInventory().getItem(KitSlots.SPEAR_SLOT);
        if (slotItem == null || slotItem.getType() == Material.BARRIER) {
            shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, homingSpearItem.create());
            return;
        }

        if (homingSpearItem.matches(slotItem)) {
            homingSpearItem.restoreName(slotItem);
            shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, slotItem);
        }
    }

    /**
     * Refreshes the spear item name or placeholder cooldown text for the player.
     *
     * @param shooter Player whose spear slot should be updated.
     * @param cooldownRemainingMs Remaining cooldown time.
     * @param returnRemainingMs Remaining return placeholder time.
     * @param formatter Formatter used to render one-decimal seconds.
     */
    public void refreshSpearItemVisual(Player shooter, long cooldownRemainingMs, long returnRemainingMs,
                                       LongFunction<String> formatter) {
        if (shooter == null) {
            return;
        }
        ItemStack slotItem = shooter.getInventory().getItem(KitSlots.SPEAR_SLOT);
        if (slotItem == null) {
            return;
        }

        // Barrier placeholders also surface the cooldown text while the spear is away from the player.
        if (slotItem.getType() == Material.BARRIER) {
            long visualRemainingMs = Math.max(cooldownRemainingMs, returnRemainingMs);
            if (visualRemainingMs <= 0L) {
                return;
            }
            homingSpearItem.applyCooldownName(slotItem, visualRemainingMs, formatter);
            shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, slotItem);
            return;
        }

        if (!homingSpearItem.matches(slotItem)) {
            return;
        }

        if (cooldownRemainingMs > 0L) {
            homingSpearItem.applyCooldownName(slotItem, cooldownRemainingMs, formatter);
        } else {
            homingSpearItem.restoreName(slotItem);
        }
        shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, slotItem);
    }

    /**
     * Restores the default display name on the player's spear item.
     *
     * @param shooterId Player id whose spear display name should be reset.
     */
    public void restoreSpearDisplayName(UUID shooterId) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null || !shooter.isOnline()) {
            return;
        }

        ItemStack slotItem = shooter.getInventory().getItem(KitSlots.SPEAR_SLOT);
        if (slotItem == null || slotItem.getType() == Material.BARRIER) {
            return;
        }

        if (!homingSpearItem.matches(slotItem)) {
            return;
        }

        homingSpearItem.restoreName(slotItem);
        shooter.getInventory().setItem(KitSlots.SPEAR_SLOT, slotItem);
    }
}
