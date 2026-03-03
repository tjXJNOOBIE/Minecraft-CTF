package dev.tjxjnoobie.ctf.kit;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tjxjnoobie.ctf.TestLogSupport;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KitManagerTest extends TestLogSupport {
    private static final String LOG_PREFIX = "[Test] [KitManagerTest] ";

    @Test
    void detectsHomingSpearItem() {
        Bukkit.getLogger().info(LOG_PREFIX + "Kit manager identifies Homing Spear by meta and model data.");
        KitManager manager = new KitManager();

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

        assertTrue(manager.isHomingSpear(spear));
        assertFalse(manager.isHomingSpear(plain));
        Bukkit.getLogger().info(LOG_PREFIX + "kit manager detects homing spear items");
    }
}
