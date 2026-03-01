package dev.tjxjnoobie.ctf.kit;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HomingSpearItemTest extends TestLogSupport {
    // Constants
    private static final String LOG_PREFIX = "[Test] [HomingSpearItemTest] ";
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void detectsHomingSpearItem() {
        Bukkit.getLogger().info(LOG_PREFIX + "Kit loadout handler identifies Homing Spear by meta and model data.");
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(meta.displayName()).thenReturn(Component.text("Homing Spear", NamedTextColor.AQUA));
        Mockito.when(meta.lore()).thenReturn(List.of(Component.text("(F to Throw)", NamedTextColor.GOLD)));
        Mockito.when(meta.hasCustomModelData()).thenReturn(true);
        Mockito.when(meta.getCustomModelData()).thenReturn(91021);

        org.bukkit.inventory.ItemStack spear = Mockito.mock(org.bukkit.inventory.ItemStack.class);
        Mockito.when(spear.getType()).thenReturn(Material.TRIDENT);
        Mockito.when(spear.getItemMeta()).thenReturn(meta);

        org.bukkit.inventory.ItemStack plain = Mockito.mock(org.bukkit.inventory.ItemStack.class);
        ItemMeta plainMeta = Mockito.mock(ItemMeta.class);
        Mockito.when(plain.getType()).thenReturn(Material.TRIDENT);
        Mockito.when(plain.getItemMeta()).thenReturn(plainMeta);
        Mockito.when(plainMeta.displayName()).thenReturn(Component.text("Homing Spear", NamedTextColor.AQUA));
        Mockito.when(plainMeta.lore()).thenReturn(List.of(Component.text("(F to Throw)", NamedTextColor.GOLD)));
        Mockito.when(plainMeta.hasCustomModelData()).thenReturn(false);

        assertTrue(HomingSpearItem.INSTANCE.matches(spear));
        assertFalse(HomingSpearItem.INSTANCE.matches(plain));
        Bukkit.getLogger().info(LOG_PREFIX + "homing spear item matcher detects spear items");
    }

    @Test
    void cooldownNamingDelegatesThroughInventoryInterface() {
        Bukkit.getLogger().info(LOG_PREFIX + "Cooldown naming uses inventory interface and keeps base naming contract.");
        RecordingInventoryUtils inventoryUtils = new RecordingInventoryUtils();
        HomingSpearItem itemService = new HomingSpearItem(inventoryUtils);
        ItemStack item = Mockito.mock(ItemStack.class);

        itemService.applyCooldownName(item, 1500L, remainingMs -> "1.5");
        assertTrue(PLAIN.serialize(inventoryUtils.lastDisplayName).startsWith("Homing Spear (1.5s)"));

        itemService.applyCooldownName(item, 0L, remainingMs -> "0.0");
        assertTrue(PLAIN.serialize(inventoryUtils.lastDisplayName).startsWith("Homing Spear"));
        Bukkit.getLogger().info(LOG_PREFIX + "cooldown naming and restore path are delegated correctly");
    }

    private static final class RecordingInventoryUtils implements dev.tjxjnoobie.ctf.util.bukkit.interfaces.IInventoryUtils {
        private Component lastDisplayName;

        @Override
        public void setItemDisplayNameIfChanged(ItemStack item, Component displayName) {
            this.lastDisplayName = displayName;
        }
    }
}
