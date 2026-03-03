package dev.tjxjnoobie.ctf.kit;

import dev.tjxjnoobie.ctf.kit.tags.KitType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Builds and validates kit-related item stacks.
 */
public final class KitFactory {
    public static final int SPEAR_SLOT = 2;

    private static final int SPEAR_CUSTOM_MODEL_DATA = 91021;
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final String SPEAR_NAME_TEXT = "Homing Spear";
    private static final String SCOUT_SWORD_NAME_TEXT = "Scout Tagger";
    private static final Component SPEAR_NAME = Component.text("Homing Spear", NamedTextColor.AQUA);
    private static final Component SPEAR_LORE = Component.text("(F to Throw)", NamedTextColor.GOLD);
    private static final Component SCOUT_SWORD_NAME = Component.text("Scout Tagger", NamedTextColor.GREEN);
    private static final Component SCOUT_SWORD_LORE = Component.text("Right-click: Throw Snowball", NamedTextColor.GRAY);

    public void applyKit(Player player, KitType kitType) {
        if (player == null || kitType == null) {
            return;
        }

        player.getInventory().clear();
        switch (kitType) {
            case RANGER -> applyRangerKit(player);
            case SCOUT -> applyScoutKit(player);
        }
    }

    public ItemStack createHomingSpear() {
        ItemStack spear = new ItemStack(Material.TRIDENT);
        ItemMeta meta = spear.getItemMeta();
        if (meta != null) {
            meta.displayName(SPEAR_NAME);
            meta.lore(List.of(SPEAR_LORE));
            meta.setCustomModelData(SPEAR_CUSTOM_MODEL_DATA);
            spear.setItemMeta(meta);
        }
        return spear;
    }

    public ItemStack createScoutTaggerSword() {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(SCOUT_SWORD_NAME);
            meta.lore(List.of(SCOUT_SWORD_LORE));
            sword.setItemMeta(meta);
        }
        return sword;
    }

    public ItemStack createScoutIcon() {
        ItemStack icon = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Scout", NamedTextColor.GREEN, TextDecoration.BOLD));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    public ItemStack createRangerIcon() {
        ItemStack icon = new ItemStack(Material.BOW);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ranger", NamedTextColor.AQUA, TextDecoration.BOLD));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    public boolean isHomingSpear(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null || meta.lore() == null) {
            return false;
        }

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        Integer customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        String displayName = PLAIN_TEXT.serialize(meta.displayName());
        return displayName.startsWith(SPEAR_NAME_TEXT)
            && SPEAR_LORE.equals(lore.getFirst())
            && Integer.valueOf(SPEAR_CUSTOM_MODEL_DATA).equals(customModelData);
    }

    public boolean isScoutTaggerSword(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SWORD) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null || meta.lore() == null) {
            return false;
        }

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        String displayName = PLAIN_TEXT.serialize(meta.displayName());
        return displayName.startsWith(SCOUT_SWORD_NAME_TEXT)
            && SCOUT_SWORD_LORE.equals(lore.getFirst());
    }

    private void applyScoutKit(Player player) {
        player.getInventory().setItem(0, createScoutTaggerSword());
        player.getInventory().setItem(8, new ItemStack(Material.COOKED_BEEF, 16));
        applyLeatherArmor(player);
    }

    private void applyRangerKit(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 1);

        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        crossbow.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);

        player.getInventory().setItem(0, bow);
        player.getInventory().setItem(1, crossbow);
        player.getInventory().setItem(SPEAR_SLOT, createHomingSpear());
        player.getInventory().setItem(8, new ItemStack(Material.COOKED_BEEF, 16));
        player.getInventory().setItem(7, new ItemStack(Material.ARROW, 32));
        applyLeatherArmor(player);
    }

    private void applyLeatherArmor(Player player) {
        player.getInventory().setArmorContents(new ItemStack[] {
            new ItemStack(Material.LEATHER_BOOTS),
            new ItemStack(Material.LEATHER_LEGGINGS),
            new ItemStack(Material.LEATHER_CHESTPLATE),
            new ItemStack(Material.LEATHER_HELMET)
        });
    }
}
