package dev.tjxjnoobie.ctf.kit;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.KitUiDependencyAccess;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.tasks.EffectTaskOrchestrator;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

/**
 * Owns kit selector inventory UI construction and click/drag/close handling.
 */
public final class KitSelectorGUI implements Listener, MessageAccess, KitUiDependencyAccess {
    private final KitSelectionHandler kitSelectionHandler;

    // == Lifecycle ==
    /**
     * Constructs a KitSelectorGUI instance.
     *
     * @param kitSelectionHandler Dependency responsible for kit selection handler.
     */
    public KitSelectorGUI(KitSelectionHandler kitSelectionHandler) {
        // Capture kit selection dependencies.
        this.kitSelectionHandler = kitSelectionHandler;
    }

    @EventHandler
    public void onKitMenuClick(InventoryClickEvent event) {
        boolean kitMenu = isKitMenu(event);
        // Guard: short-circuit when !kitMenu.
        if (!kitMenu) {
            return;
        }

        // Prevent taking items from the kit menu.
        event.setCancelled(true);
        Object whoClicked = event.getWhoClicked();
        boolean clickedPlayer = whoClicked instanceof Player;
        // Guard: short-circuit when !clickedPlayer.
        if (!clickedPlayer) {
            return;
        }
        Player player = (Player) whoClicked;

        int clickedSlot = event.getSlot();
        KitType selectedKit = KitSelectorMenu.kitForSlot(clickedSlot);
        // Guard: short-circuit when selectedKit == null.
        if (selectedKit == null) {
            return;
        }
        applyKitSelection(player, selectedKit);
        player.closeInventory();
    }

    @EventHandler
    public void onKitMenuDrag(InventoryDragEvent event) {
        boolean kitMenu = isKitMenu(event);
        // Guard: short-circuit when !kitMenu.
        if (!kitMenu) {
            return;
        }
        // Prevent drag interactions within kit menu.
        event.setCancelled(true);
    }

    @EventHandler
    public void onKitMenuClose(InventoryCloseEvent event) {
        boolean kitMenu = isKitMenu(event);
        // Guard: short-circuit when !kitMenu.
        if (!kitMenu) {
            return;
        }
        Object closingPlayer = event.getPlayer();
        boolean isPlayer = closingPlayer instanceof Player;
        // Guard: short-circuit when !isPlayer.
        if (!isPlayer) {
            return;
        }
        Player player = (Player) closingPlayer;

        boolean inventoryTracked = kitSelectionHandler.isInventoryTracked(player);
        // Guard: short-circuit when !inventoryTracked.
        if (!inventoryTracked) {
            return;
        }
        boolean hasSelection = kitSelectionHandler.hasSelection(player);
        // Guard: short-circuit when hasSelection.
        if (hasSelection) {
            return;
        }

        // Re-open selector if player closed without selecting.
        EffectTaskOrchestrator.startLater(null, () -> {
            boolean online = player.isOnline();
            // Guard: short-circuit when !online.
            if (!online) {
                return;
            }
            boolean stillTracked = kitSelectionHandler.isInventoryTracked(player);
            // Guard: short-circuit when !stillTracked.
            if (!stillTracked) {
                return;
            }
            boolean selectedAfterClose = kitSelectionHandler.hasSelection(player);
            // Guard: short-circuit when selectedAfterClose.
            if (selectedAfterClose) {
                return;
            }
            getKitSelectorGui().openKitSelector(player, true);
        }, 1L);
    }

    // == Utilities ==
    /**
     * Executes openKitSelector.
     *
     * @param player Player involved in this operation.
     * @param allowDuringMatch Control value that changes behavior for this operation.
     */
    public void openKitSelector(Player player, boolean allowDuringMatch) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        boolean matchRunning = kitSelectionHandler.isMatchRunning(); // Block selection during match when not allowed.
        boolean shouldBlockForMatch = !allowDuringMatch && matchRunning;
        if (shouldBlockForMatch) {
            Component message = getMessage(CTFKeys.uiKitLockedActionbarKey());
            player.sendActionBar(message);
            return;
        }

        Inventory inventory = KitSelectorMenu.create(getScoutIconItem(), getRangerIconItem());
        if (inventory != null) {
            player.openInventory(inventory);
        }
    }

    private void applyKitSelection(Player player, KitType kitType) {
        kitSelectionHandler.selectKit(player, kitType);
        String kitLabel = KitSelectorMenu.labelFor(kitType);
        Component message = getMessageFormatted(CTFKeys.uiKitSelectedMessageKey(), kitLabel);
        player.sendMessage(message);
        boolean matchRunning = kitSelectionHandler.isMatchRunning();
        if (matchRunning) {
            KitType selectedKit = kitSelectionHandler.getSelectedKit(player);
            kitSelectionHandler.applyKitLoadout(player, selectedKit);
        }
    }

    // == Predicates ==
    private boolean isKitMenu(InventoryClickEvent event) {
        // Guard: short-circuit when event == null.
        if (event == null) {
            return false;
        }
        InventoryView view = event.getView();
        return isKitMenuView(view);
    }

    private boolean isKitMenu(InventoryDragEvent event) {
        // Guard: short-circuit when event == null.
        if (event == null) {
            return false;
        }
        InventoryView view = event.getView();
        return isKitMenuView(view);
    }

    private boolean isKitMenu(InventoryCloseEvent event) {
        // Guard: short-circuit when event == null.
        if (event == null) {
            return false;
        }
        InventoryView view = event.getView();
        return isKitMenuView(view);
    }

    private boolean isKitMenuView(InventoryView view) {
        // Guard: short-circuit when view == null.
        if (view == null) {
            return false;
        }
        Component title = view.title();
        return KitSelectorMenu.isTitle(title);
    }
}
