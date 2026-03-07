package dev.tjxjnoobie.ctf.game.combat.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.combat.spear.HomingSpearAbilityEvents;
import dev.tjxjnoobie.ctf.items.kit.SpearLockedPlaceholderItem;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearInventoryUtils;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.items.kit.SpearReturningPlaceholderItem;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.kit.KitSlots;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HomingSpearAbilityEventsTest extends TestLogSupport {
    private Player arenaPlayer;
    private Player offArenaPlayer;
    private PlayerInventory inventory;
    private HomingSpearAbilityEvents events;

    @BeforeEach
    void setUpEvents() {
        HomingSpearAbilityHandler abilityService = Mockito.mock(HomingSpearAbilityHandler.class);
        HomingSpearInventoryUtils homingSpearInventoryUtils = new HomingSpearInventoryUtils(
                HomingSpearItem.INSTANCE,
                new SpearLockedPlaceholderItem(),
                new SpearReturningPlaceholderItem());
        arenaPlayer = Mockito.mock(Player.class);
        offArenaPlayer = Mockito.mock(Player.class);
        inventory = Mockito.mock(PlayerInventory.class);

        when(abilityService.sessionIsPlayerInArena(arenaPlayer)).thenReturn(true);
        when(abilityService.sessionIsPlayerInArena(offArenaPlayer)).thenReturn(false);
        when(arenaPlayer.getInventory()).thenReturn(inventory);
        when(offArenaPlayer.getInventory()).thenReturn(inventory);

        events = new HomingSpearAbilityEvents(abilityService, homingSpearInventoryUtils);
    }

    @Test
    void playerDropCancelsPlaceholderDropsForArenaPlayers() {
        PlayerDropItemEvent event = Mockito.mock(PlayerDropItemEvent.class);
        Item itemEntity = Mockito.mock(Item.class);
        ItemStack placeholder = placeholderItem();

        when(event.getPlayer()).thenReturn(arenaPlayer);
        when(event.getItemDrop()).thenReturn(itemEntity);
        when(itemEntity.getItemStack()).thenReturn(placeholder);

        events.onPlayerDropItem(event);

        verify(event).setCancelled(true);
    }

    @Test
    void inventoryClickCancelsWhenPlaceholderIsMoved() {
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        ItemStack placeholder = placeholderItem();

        when(event.getWhoClicked()).thenReturn(arenaPlayer);
        when(event.getCurrentItem()).thenReturn(placeholder);
        when(event.getCursor()).thenReturn(null);
        when(event.getHotbarButton()).thenReturn(-1);

        events.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    void inventoryDragCancelsWhenPlaceholderEntersDraggedItems() {
        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        ItemStack placeholder = placeholderItem();

        when(event.getWhoClicked()).thenReturn(arenaPlayer);
        when(event.getNewItems()).thenReturn(Map.of(0, placeholder));
        when(event.getRawSlots()).thenReturn(Set.of());

        events.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void inventoryDragCancelsWhenProtectedSlotAlreadyHasPlaceholder() {
        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        ItemStack placeholder = placeholderItem();

        when(event.getWhoClicked()).thenReturn(arenaPlayer);
        when(event.getNewItems()).thenReturn(Map.of());
        when(event.getRawSlots()).thenReturn(Set.of(KitSlots.SPEAR_SLOT));
        when(inventory.getItem(KitSlots.SPEAR_SLOT)).thenReturn(placeholder);

        events.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void placeholderEventsIgnorePlayersOutsideArena() {
        PlayerDropItemEvent event = Mockito.mock(PlayerDropItemEvent.class);
        Item itemEntity = Mockito.mock(Item.class);
        ItemStack placeholder = placeholderItem();

        when(event.getPlayer()).thenReturn(offArenaPlayer);
        when(event.getItemDrop()).thenReturn(itemEntity);
        when(itemEntity.getItemStack()).thenReturn(placeholder);

        events.onPlayerDropItem(event);

        verify(event, never()).setCancelled(true);
    }

    private ItemStack placeholderItem() {
        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);

        when(item.getType()).thenReturn(Material.BARRIER);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.displayName()).thenReturn(SpearLockedPlaceholderItem.NAME);
        return item;
    }
}
