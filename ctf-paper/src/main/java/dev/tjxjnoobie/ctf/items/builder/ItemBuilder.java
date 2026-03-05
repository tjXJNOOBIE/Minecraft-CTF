package dev.tjxjnoobie.ctf.items.builder;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Fluent builder for ItemStack creation with safe meta handling.
 */
public final class ItemBuilder {

    // == Configuration ==
    private final Material material;
    private int amount = 1;
    private Component displayName;
    private List<Component> lore;
    private Integer customModelData;
    private final List<EnchantmentEntry> enchantments = new ArrayList<>();

    // == Lifecycle ==
    private ItemBuilder(Material material) {
        // Capture base material for the item.
        this.material = material;
    }

    // == Utilities ==
    /**
     * Returns the result of fromMaterial.
     *
     * @param material Bukkit type used by this operation.
     * @return Result produced by this method.
     */
    public static ItemBuilder fromMaterial(Material material) {
        // Start building from a material.
        return new ItemBuilder(material);
    }

    /**
     * Returns the result of fromMaterial.
     *
     * @param material Bukkit type used by this operation.
     * @param amount Numeric value used by this operation.
     * @return Result produced by this method.
     */
    public static ItemBuilder fromMaterial(Material material, int amount) {
        // Start building with a preset amount.
        return new ItemBuilder(material).amount(amount);
    }

    /**
     * Returns the result of amount.
     *
     * @param amount Numeric value used by this operation.
     * @return Result produced by this method.
     */
    public ItemBuilder amount(int amount) {
        if (amount > 0) {
            // Only accept positive amounts.
            this.amount = amount;
        }
        return this;
    }

    /**
     * Returns the result of displayName.
     *
     * @param displayName User-facing display text.
     * @return Result produced by this method.
     */
    public ItemBuilder displayName(Component displayName) {
        // Set the display name (optional).
        this.displayName = displayName;
        return this;
    }

    /**
     * Returns the result of lore.
     *
     * @param lore Component content applied to the output item or message.
     * @return Result produced by this method.
     */
    public ItemBuilder lore(List<Component> lore) {
        // Set lore lines (optional).
        this.lore = lore;
        return this;
    }

    /**
     * Returns the result of customModelData.
     *
     * @param customModelData Inventory slot or model metadata value.
     * @return Result produced by this method.
     */
    public ItemBuilder customModelData(Integer customModelData) {
        // Set custom model data (optional).
        this.customModelData = customModelData;
        return this;
    }

    /**
     * Returns the result of enchant.
     *
     * @param enchantment Bukkit type used by this operation.
     * @param level Numeric value used by this operation.
     * @return Result produced by this method.
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            // Add safe enchantment.
            enchantments.add(new EnchantmentEntry(enchantment, level, false));
        }
        return this;
    }

    /**
     * Returns the result of unsafeEnchant.
     *
     * @param enchantment Bukkit type used by this operation.
     * @param level Numeric value used by this operation.
     * @return Result produced by this method.
     */
    public ItemBuilder unsafeEnchant(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            // Add unsafe enchantment.
            enchantments.add(new EnchantmentEntry(enchantment, level, true));
        }
        return this;
    }

    /**
     * Returns the result of build.
     *
     * @return Newly created value.
     */
    public ItemStack build() {
        // Guard: short-circuit when material == null.
        if (material == null) {
            return null;
        }

        ItemStack item = new ItemStack(material, amount); // Build base item with metadata.
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.displayName(displayName);
            }
            boolean conditionResult1 = lore != null && !lore.isEmpty();
            if (conditionResult1) {
                meta.lore(List.copyOf(lore));
            }
            if (customModelData != null) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }

        for (EnchantmentEntry entry : enchantments) {
            if (entry.unsafe) {
                item.addUnsafeEnchantment(entry.enchantment, entry.level);
            } else {
                item.addEnchantment(entry.enchantment, entry.level);
            }
        }

        return item;
    }

    private record EnchantmentEntry(Enchantment enchantment, int level, boolean unsafe) {
    }
}
