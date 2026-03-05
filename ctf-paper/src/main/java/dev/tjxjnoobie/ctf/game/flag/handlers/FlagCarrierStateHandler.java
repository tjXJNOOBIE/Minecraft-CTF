package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.CarrierInventoryTracker;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles carrier state checks, inventory lock enforcement, and carrier cleanup.
 */
public final class FlagCarrierStateHandler implements BukkitMessageSender, FlagDependencyAccess, PlayerDependencyAccess {

    // == Lifecycle ==
    /**
     * Constructs a FlagCarrierStateHandler instance.
     */
    public FlagCarrierStateHandler() {
    }

    // == Utilities ==
    /**
     * Executes clearCarrierItems.
     */
    public void clearCarrierItems() {
        for (FlagMetaData flag : getAllFlagMetaData()) {
            // Guard: short-circuit when flag == null || flag.state != FlagState.CARRIED || flag.carrier == null.
            if (flag == null || flag.state != FlagState.CARRIED || flag.carrier == null) {
                continue;
            }
            Player carrier = Bukkit.getPlayer(flag.carrier);
            if (carrier != null) {
                clearCarrierFlagItem(carrier);
            }
        }
    }

    /**
     * Executes clearCarrierFlagItemAndEffects.
     *
     * @param player Player involved in this operation.
     */
    public void clearCarrierFlagItemAndEffects(Player player) {
        clearCarrierFlagItem(player);
        clearCarrierEffects(player);
    }

    /**
     * Executes enforceCarrierFlagHotbarSlot.
     *
     * @param player Player involved in this operation.
     */
    public void enforceCarrierFlagHotbarSlot(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        TeamId carriedTeam = getFlagStateRegistry().findCarriedFlagTeam(player.getUniqueId());
        getCarrierInventoryTracker().enforceCarrierFlagHotbarSlot(player, carriedTeam, this::isFlagMaterial, this::giveCarrierFlagItem);
    }

    private void clearCarrierFlagItem(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        getCarrierInventoryTracker().restoreCarrierFlagItem(player);
        hideBossBar(player, BukkitBossBarType.CARRIER);
    }

    private void clearCarrierEffects(Player player) {
        getCarrierEffects().clearCarrierEffects(player);
    }

    private void giveCarrierFlagItem(Player player, TeamId flagTeam) {
        // Guard: short-circuit when player == null || flagTeam == null.
        if (player == null || flagTeam == null) {
            return;
        }

        Material material = getTeamBaseMetaDataResolver().resolveFlagMaterial(flagTeam);
        Component displayName = getTeamManager().getDisplayComponent(flagTeam)
            .append(Component.text(" Flag", NamedTextColor.GRAY));
        getCarrierInventoryTracker().giveCarrierFlagItem(player, material, displayName);
    }

    // == Predicates ==
    public boolean isFlagCarrier(UUID playerUUID) {
        return getFlagStateRegistry().isFlagCarrier(playerUUID);
    }

    private boolean isFlagMaterial(Material material) {
        return material != null
            && (material == getTeamBaseMetaDataResolver().resolveFlagMaterial(TeamId.RED)
            || material == getTeamBaseMetaDataResolver().resolveFlagMaterial(TeamId.BLUE));
    }
}

