package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles enemy flag pickup transitions.
 */
public final class FlagPickupHandler implements BukkitMessageSender, FlagDependencyAccess, PlayerDependencyAccess {

    // == Getters ==
    private org.bukkit.Material resolveFlagMaterial(TeamId teamId) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return null;
        }
        String teamKey = teamId.key();
        org.bukkit.Material fallbackMaterial = getTeamManager().getFlagMaterial(teamId);
        return getFlagMaterial(teamKey, fallbackMaterial);
    }

    // == Utilities ==
    /**
     * Executes the processEnemyFlagPickup operation.
     *
     * @param player Player involved in this operation.
     * @param flagTeam Team identifier associated with the flag operation.
     * @return {@code true} when the operation succeeds; otherwise {@code false}.
     */
    public boolean processEnemyFlagPickup(Player player, TeamId flagTeam) {
        // Validation & early exits
        // Guard: short-circuit when player == null || flagTeam == null.
        if (player == null || flagTeam == null) {
            return false;
        }

        boolean conditionResult1 = getFlagStateRegistry().findCarriedFlagTeam(player.getUniqueId()) != null;
        // Guard: short-circuit when findCarriedFlagTeam(player.getUniqueId()) != null.
        if (conditionResult1) {
            return false;
        }

        FlagMetaData flag = getFlagStateRegistry().flagFor(flagTeam);
        // Guard: short-circuit when flag == null || flag.state == FlagState.CARRIED.
        if (flag == null || flag.state == FlagState.CARRIED) {
            return false;
        }

        // World/application side effects
        if (flag.activeLocation != null) {
            getFlagBlockPlacer().clearFlagBlock(flag.activeLocation, flagTeam);
        }

        // State transition
        getFlagStateRegistry().setCarried(flagTeam, player.getUniqueId());

        // World/application side effects
        Component flagDisplayName = getTeamManager().getDisplayComponent(flagTeam)
            .append(Component.text(" Flag", NamedTextColor.GRAY));
        getCarrierInventoryTracker().giveCarrierFlagItem(player, resolveFlagMaterial(flagTeam), flagDisplayName);
        getCarrierEffects().applyCarrierEffects(player);

        // UX feedback
        getFlagEventEffects().showFlagPickupMessaging(player, flagTeam);

        String playerName = player.getName(); // Debug/telemetry
        sendDebugMessage("flag pickup team=" + flagTeam + " player=" + playerName);

        // UX feedback
        getFlagEventEffects().playFlagPickupEffects(player, flagTeam);
        getFlagUiTicker().tickFlagUi();
        return true;
    }
}

