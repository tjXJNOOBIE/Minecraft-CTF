package dev.tjxjnoobie.ctf.kit;

import dev.tjxjnoobie.ctf.kit.metadata.InventorySnapshot;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Thin kit coordinator: tracks kit selections/snapshots and delegates UI/item logic.
 */
public final class KitManager {
    public static final int SPEAR_SLOT = KitFactory.SPEAR_SLOT;

    private final Map<UUID, InventorySnapshot> inventorySnapshots = new HashMap<>();
    private final Map<UUID, KitType> selectedKitByPlayer = new HashMap<>();
    private final KitFactory kitFactory;
    private final KitSelectorUI kitSelectorUI;
    private BooleanSupplier matchRunningSupplier = () -> false;

    public KitManager() {
        this.kitFactory = new KitFactory();
        this.kitSelectorUI = new KitSelectorUI(this, kitFactory);
    }

    public Listener getSelectorListener() {
        return kitSelectorUI;
    }

    public void setMatchRunningSupplier(BooleanSupplier supplier) {
        if (supplier != null) {
            matchRunningSupplier = supplier;
        }
    }

    public boolean isMatchRunning() {
        return matchRunningSupplier.getAsBoolean();
    }

    public void recordOriginalInventory(Player player) {
        if (player == null || inventorySnapshots.containsKey(player.getUniqueId())) {
            return;
        }
        inventorySnapshots.put(player.getUniqueId(), InventorySnapshot.capture(player));
    }

    public void openKitSelector(Player player) {
        kitSelectorUI.openKitSelector(player);
    }

    public void openKitSelector(Player player, boolean allowDuringMatch) {
        kitSelectorUI.openKitSelector(player, allowDuringMatch);
    }

    public void applyKit(Player player) {
        if (player == null) {
            return;
        }
        kitFactory.applyKit(player, getSelectedKit(player));
    }

    public void applyKit(Collection<Player> players) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            applyKit(player);
        }
    }

    public void restoreInventory(Player player) {
        if (player == null) {
            return;
        }

        InventorySnapshot snapshot = inventorySnapshots.remove(player.getUniqueId());
        if (snapshot != null) {
            snapshot.restore(player);
        }
    }

    public void restoreAll() {
        for (Map.Entry<UUID, InventorySnapshot> entry : inventorySnapshots.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().restore(player);
            }
        }
        inventorySnapshots.clear();
    }

    public void clearAll() {
        inventorySnapshots.clear();
        selectedKitByPlayer.clear();
    }

    public void clearSelection(Player player) {
        if (player == null) {
            return;
        }
        selectedKitByPlayer.remove(player.getUniqueId());
    }

    public boolean hasSelection(Player player) {
        return player != null && selectedKitByPlayer.containsKey(player.getUniqueId());
    }

    public boolean isInventoryTracked(Player player) {
        return player != null && inventorySnapshots.containsKey(player.getUniqueId());
    }

    public void selectKit(Player player, KitType kitType) {
        if (player == null || kitType == null) {
            return;
        }
        selectedKitByPlayer.put(player.getUniqueId(), kitType);
    }

    public boolean isRanger(Player player) {
        return player != null && getSelectedKit(player) == KitType.RANGER;
    }

    public boolean isScout(Player player) {
        return player != null && getSelectedKit(player) == KitType.SCOUT;
    }

    public ItemStack createHomingSpearItem() {
        return kitFactory.createHomingSpear();
    }

    public boolean isHomingSpear(ItemStack item) {
        return kitFactory.isHomingSpear(item);
    }

    public boolean isScoutTaggerSword(ItemStack item) {
        return kitFactory.isScoutTaggerSword(item);
    }

    private KitType getSelectedKit(Player player) {
        if (player == null) {
            return KitType.SCOUT;
        }
        return selectedKitByPlayer.getOrDefault(player.getUniqueId(), KitType.SCOUT);
    }
}
