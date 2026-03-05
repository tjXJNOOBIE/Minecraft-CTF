package dev.tjxjnoobie.ctf.kit;

import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.items.builder.ItemBuilder;
import dev.tjxjnoobie.ctf.kit.metadata.PlayerInventorySnapshot;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.KitUiDependencyAccess;
/**
 * Tracks kit selection state and inventory snapshots.
 */
public final class KitSelectionHandler implements LifecycleDependencyAccess, KitUiDependencyAccess {

    // == Runtime state ==
    private final Map<UUID, KitPlayerState> stateByPlayer = new HashMap<>();

    // == Getters ==
    public KitType getSelectedKit(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return KitType.SCOUT;
        }
        KitPlayerState state = getState(player); // Default to scout when no selection exists.
        return state == null || state.selectedKit == null ? KitType.SCOUT : state.selectedKit;
    }

    private KitPlayerState getState(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return null;
        }
        return stateByPlayer.get(player.getUniqueId());
    }

    private KitPlayerState getOrCreateState(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return null;
        }
        return stateByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new KitPlayerState());
    }

    // == Utilities ==
    /**
     * Executes recordOriginalInventory.
     *
     * @param player Player involved in this operation.
     */
    public void recordOriginalInventory(Player player) {
        KitPlayerState state = getOrCreateState(player); // Capture the initial inventory snapshot once.
        // Guard: short-circuit when state == null.
        if (state == null) {
            return;
        }
        // Guard: short-circuit when state.snapshot != null.
        if (state.snapshot != null) {
            return;
        }
        state.snapshot = PlayerInventorySnapshot.capture(player);
    }

    /**
     * Executes restoreInventory.
     *
     * @param player Player involved in this operation.
     */
    public void restoreInventory(Player player) {
        UUID playerUUID = player == null ? null : player.getUniqueId(); // Restore and clear the stored snapshot.
        KitPlayerState state = getState(player);
        // Guard: short-circuit when state == null || state.snapshot == null.
        if (state == null || state.snapshot == null) {
            return;
        }
        state.snapshot.restore(player);
        state.snapshot = null;
        pruneStateIfEmpty(playerUUID, state);
    }

    /**
     * Executes restoreAll.
     */
    public void restoreAll() {
        // Restore all stored snapshots for tracked players.
        for (Map.Entry<UUID, KitPlayerState> entry : stateByPlayer.entrySet()) {
            KitPlayerState state = entry.getValue();
            // Guard: short-circuit when state == null || state.snapshot == null.
            if (state == null || state.snapshot == null) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                state.snapshot.restore(player);
            }
            state.snapshot = null;
        }
        pruneEmptyStates();
    }

    /**
     * Executes clearAll.
     */
    public void clearAll() {
        // Drop all stored kit state.
        stateByPlayer.clear();
    }

    /**
     * Executes clearSelection.
     *
     * @param player Player involved in this operation.
     */
    public void clearSelection(Player player) {
        UUID playerUUID = player == null ? null : player.getUniqueId(); // Clear the player's selected kit.
        KitPlayerState state = getState(player);
        // Guard: short-circuit when state == null.
        if (state == null) {
            return;
        }
        state.selectedKit = null;
        pruneStateIfEmpty(playerUUID, state);
    }

    /**
     * Executes selectKit.
     *
     * @param player Player involved in this operation.
     * @param kitType Domain enum value used to control behavior.
     */
    public void selectKit(Player player, KitType kitType) {
        // Guard: short-circuit when player == null || kitType == null.
        if (player == null || kitType == null) {
            return;
        }
        KitPlayerState state = getOrCreateState(player); // Persist the chosen kit.
        // Guard: short-circuit when state == null.
        if (state == null) {
            return;
        }
        state.selectedKit = kitType;
    }

    /**
     * Executes clearSelections.
     *
     * @param players Players involved in this operation.
     */
    public void clearSelections(Collection<Player> players) {
        // Guard: short-circuit when players == null.
        if (players == null) {
            return;
        }
        // Clear selections for each provided player.
        for (Player player : players) {
            clearSelection(player);
        }
    }

    private void pruneEmptyStates() {
        // Remove entries with no snapshot or kit.
        stateByPlayer.entrySet().removeIf(entry -> {
            KitPlayerState state = entry.getValue();
            return state == null || (state.snapshot == null && state.selectedKit == null);
        });
    }

    private void pruneStateIfEmpty(UUID playerUUID, KitPlayerState state) {
        // Guard: short-circuit when playerUUID == null || state == null.
        if (playerUUID == null || state == null) {
            return;
        }
        // Remove state when no longer needed.
        if (state.snapshot == null && state.selectedKit == null) {
            stateByPlayer.remove(playerUUID);
        }
    }

    /**
     * Executes applyKitLoadout.
     *
     * @param player Player involved in this operation.
     * @param kitType Domain enum value used to control behavior.
     */
    public void applyKitLoadout(Player player, KitType kitType) {
        // Guard: short-circuit when player == null || kitType == null.
        if (player == null || kitType == null) {
            return;
        }

        // Clear inventory and apply the selected kit.
        player.getInventory().clear();
        switch (kitType) {
            case RANGER -> applyRangerKit(player);
            case SCOUT -> applyScoutKit(player);
        }
    }

    /**
     * Executes applyKitLoadout.
     *
     * @param players Players involved in this operation.
     * @param kitType Domain enum value used to control behavior.
     */
    public void applyKitLoadout(List<Player> players, KitType kitType) {
        // Guard: short-circuit when players == null || kitType == null.
        if (players == null || kitType == null) {
            return;
        }
        // Apply kit to each player in the list.
        for (Player player : players) {
            applyKitLoadout(player, kitType);
        }
    }

    private void applyScoutKit(Player player) {
        ItemStack sword = getScoutSwordItem().create(); // Equip the scout kit items and armor.
        ItemStack food = ItemBuilder.fromMaterial(Material.COOKED_BEEF, 16).build();
        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(8, food);
        applyScoutArmor(player);
    }

    private void applyRangerKit(Player player) {
        // Equip the ranger kit items and armor.
        ItemStack bow = ItemBuilder.fromMaterial(Material.BOW)
            .enchant(Enchantment.POWER, 1)
            .build();

        ItemStack crossbow = ItemBuilder.fromMaterial(Material.CROSSBOW)
            .unsafeEnchant(Enchantment.KNOCKBACK, 3)
            .build();

        ItemStack spear = getHomingSpearItem().create();
        ItemStack food = ItemBuilder.fromMaterial(Material.COOKED_BEEF, 16).build();
        ItemStack arrows = ItemBuilder.fromMaterial(Material.ARROW, 32).build();
        player.getInventory().setItem(0, bow);
        player.getInventory().setItem(1, crossbow);
        player.getInventory().setItem(KitSlots.SPEAR_SLOT, spear);
        player.getInventory().setItem(8, food);
        player.getInventory().setItem(7, arrows);
        applyLeatherArmor(player);
    }

    private void applyLeatherArmor(Player player) {
        ItemStack boots = ItemBuilder.fromMaterial(Material.LEATHER_BOOTS).build(); // Apply default leather armor set.
        ItemStack leggings = ItemBuilder.fromMaterial(Material.LEATHER_LEGGINGS).build();
        ItemStack chestplate = ItemBuilder.fromMaterial(Material.LEATHER_CHESTPLATE).build();
        ItemStack helmet = ItemBuilder.fromMaterial(Material.LEATHER_HELMET).build();
        player.getInventory().setArmorContents(new ItemStack[] {
            boots,
            leggings,
            chestplate,
            helmet
        });
    }

    private void applyScoutArmor(Player player) {
        ItemStack boots = ItemBuilder.fromMaterial(Material.IRON_BOOTS).build();
        ItemStack leggings = ItemBuilder.fromMaterial(Material.CHAINMAIL_LEGGINGS).build();
        ItemStack chestplate = ItemBuilder.fromMaterial(Material.CHAINMAIL_CHESTPLATE).build();
        ItemStack helmet = ItemBuilder.fromMaterial(Material.IRON_HELMET).build();
        player.getInventory().setArmorContents(new ItemStack[] {
            boots,
            leggings,
            chestplate,
            helmet
        });
    }

    // == Predicates ==
    public boolean hasSelection(Player player) {
        KitPlayerState state = getState(player); // Check if the player chose a kit.
        return state != null && state.selectedKit != null;
    }

    public boolean isInventoryTracked(Player player) {
        KitPlayerState state = getState(player); // Check if an inventory snapshot is stored.
        return state != null && state.snapshot != null;
    }

    public boolean isRanger(Player player) {
        return player != null && getSelectedKit(player) == KitType.RANGER;
    }

    public boolean isScout(Player player) {
        return player != null && getSelectedKit(player) == KitType.SCOUT;
    }

    public boolean isMatchRunning() {
        GameStateManager gameStateManager = getGameStateManager();
        // Match is running when not in cleanup and state is active.
        return gameStateManager != null
            && !gameStateManager.isCleanupInProgress()
            && gameStateManager.isRunning();
    }

    private static final class KitPlayerState {
        private PlayerInventorySnapshot snapshot;
        private KitType selectedKit;
    }
}
