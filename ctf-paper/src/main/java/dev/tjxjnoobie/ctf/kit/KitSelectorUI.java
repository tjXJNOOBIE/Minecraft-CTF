package dev.tjxjnoobie.ctf.kit;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Owns kit selector inventory UI construction and click/drag/close handling.
 */
public final class KitSelectorUI implements Listener, MessageAccess {
    private static final Component KIT_MENU_TITLE = Component.text("Select Kit");
    private static final int KIT_MENU_SIZE = 9;
    private static final int SCOUT_SLOT = 3;
    private static final int RANGER_SLOT = 5;

    private final KitManager kitManager;
    private final KitFactory kitFactory;

    public KitSelectorUI(KitManager kitManager, KitFactory kitFactory) {
        this.kitManager = kitManager;
        this.kitFactory = kitFactory;
    }

    public void openKitSelector(Player player) {
        openKitSelector(player, false);
    }

    public void openKitSelector(Player player, boolean allowDuringMatch) {
        if (player == null) {
            return;
        }

        if (!allowDuringMatch && kitManager.isMatchRunning()) {
            player.sendActionBar(msg("actionbar.kit.locked"));
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, KIT_MENU_SIZE, KIT_MENU_TITLE);
        inventory.setItem(SCOUT_SLOT, kitFactory.createScoutIcon());
        inventory.setItem(RANGER_SLOT, kitFactory.createRangerIcon());
        player.openInventory(inventory);
    }

    @EventHandler
    public void onKitMenuClick(InventoryClickEvent event) {
        if (!isKitMenu(event)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getSlot() == SCOUT_SLOT) {
            kitManager.selectKit(player, KitType.SCOUT);
            player.sendMessage(msg("kit.selected", Map.of("kit", "Scout")));
            if (kitManager.isMatchRunning()) {
                kitManager.applyKit(player);
            }
            player.closeInventory();
            return;
        }

        if (event.getSlot() == RANGER_SLOT) {
            kitManager.selectKit(player, KitType.RANGER);
            player.sendMessage(msg("kit.selected", Map.of("kit", "Ranger")));
            if (kitManager.isMatchRunning()) {
                kitManager.applyKit(player);
            }
            player.closeInventory();
        }
    }

    @EventHandler
    public void onKitMenuDrag(InventoryDragEvent event) {
        if (!isKitMenu(event)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onKitMenuClose(InventoryCloseEvent event) {
        if (!isKitMenu(event)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!kitManager.isInventoryTracked(player)) {
            return;
        }
        if (kitManager.hasSelection(player)) {
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(KitSelectorUI.class);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!kitManager.isInventoryTracked(player)) {
                return;
            }
            if (kitManager.hasSelection(player)) {
                return;
            }
            openKitSelector(player, true);
        }, 1L);
    }

    private boolean isKitMenu(InventoryClickEvent event) {
        return event != null && event.getView() != null && KIT_MENU_TITLE.equals(event.getView().title());
    }

    private boolean isKitMenu(InventoryDragEvent event) {
        return event != null && event.getView() != null && KIT_MENU_TITLE.equals(event.getView().title());
    }

    private boolean isKitMenu(InventoryCloseEvent event) {
        return event != null && event.getView() != null && KIT_MENU_TITLE.equals(event.getView().title());
    }
}
