package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagPickupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagReturnHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.location.LocationFormatUtils;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles player flag touch and movement-driven capture transitions.
 */
public final class FlagCarrierHandler implements MessageAccess, BukkitMessageSender, FlagDependencyAccess, PlayerDependencyAccess {

    // == Constants ==
    private static final long OWN_FLAG_NOTICE_COOLDOWN_MS = 2_000L;
    private static final String LOG_PREFIX = "[FlagCarrierHandler] ";

    // == Runtime state ==
    private final Map<UUID, Long> ownFlagNoticeCooldownByPlayerUUID = new HashMap<>();

    // == Utilities ==
    /**
     * Executes the processFlagTouch operation.
     *
     * @param player Player involved in this operation.
     * @param blockLocation World location used by this operation.
     * @param isMatchRunning Control flag that changes how this operation is executed.
     * @return {@code true} when the operation succeeds; otherwise {@code false}.
     */
    public boolean processFlagTouch(Player player, Location blockLocation, boolean isMatchRunning) {
        // Guard: short-circuit when !isMatchRunning || player == null || blockLocation == null.
        if (!isMatchRunning || player == null || blockLocation == null) {
            return false;
        }

        TeamId playerTeamId = getTeamManager().getTeamId(player);
        // Guard: short-circuit when playerTeamId == null.
        if (playerTeamId == null) {
            return false;
        }

        TeamId touchedTeamId = getFlagBlockPlacer().resolveFlagTeamAtBlockLocation(blockLocation, getFlagStateRegistry());
        // Guard: short-circuit when touchedTeamId == null.
        if (touchedTeamId == null) {
            return false;
        }

        if (touchedTeamId == playerTeamId) {
            FlagMetaData ownFlag = getFlagStateRegistry().flagFor(playerTeamId);
            // Guard: short-circuit when ownFlag != null && ownFlag.state == FlagState.DROPPED.
            if (ownFlag != null && ownFlag.state == FlagState.DROPPED) {
                return getFlagReturnHandler().returnDroppedOwnFlagToBase(player, playerTeamId);
            }

            notifyOwnFlagCaptureBlocked(player);
            return false;
        }

        return getFlagPickupHandler().processEnemyFlagPickup(player, touchedTeamId);
    }

    /**
     * Executes the processFlagCarrierMovement operation.
     *
     * @param player Player involved in this operation.
     * @param to World location used by this operation.
     * @param isMatchRunning Control flag that changes how this operation is executed.
     */
    public void processFlagCarrierMovement(Player player, Location to, boolean isMatchRunning) {
        // Guard: short-circuit when !isMatchRunning || player == null || to == null.
        if (!isMatchRunning || player == null || to == null) {
            return;
        }

        TeamId playerTeamId = getTeamManager().getTeamId(player);
        // Guard: short-circuit when playerTeamId == null.
        if (playerTeamId == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        TeamId carriedFlagTeamId = getFlagStateRegistry().findCarriedFlagTeam(playerUUID);
        // Guard: short-circuit when carriedFlagTeamId == null.
        if (carriedFlagTeamId == null) {
            return;
        }

        FlagMetaData ownFlag = getFlagStateRegistry().flagFor(playerTeamId);
        // Guard: short-circuit when ownFlag == null || ownFlag.state != FlagState.AT_BASE.
        if (ownFlag == null || ownFlag.state != FlagState.AT_BASE) {
            return;
        }

        TeamBaseMetaData ownBaseData = getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(playerTeamId);
        boolean conditionResult1 = !getCTFCaptureZoneHandler().isInsideCaptureZone(ownBaseData, to);
        // Guard: short-circuit when !isInsideCaptureZone(ownBaseData, to).
        if (conditionResult1) {
            return;
        }

        completeFlagCapture(player, playerTeamId, carriedFlagTeamId);
    }

    private void completeFlagCapture(Player player, TeamId scoringTeamId, TeamId capturedFlagTeamId) {
        // Guard: short-circuit when player == null || scoringTeamId == null || capturedFlagTeamId == null.
        if (player == null || scoringTeamId == null || capturedFlagTeamId == null) {
            return;
        }

        FlagMetaData capturedFlag = getFlagStateRegistry().flagFor(capturedFlagTeamId);
        Location capturedBaseLocation = LocationFormatUtils.cloneLocation(capturedFlag == null ? null : capturedFlag.baseLocation);
        // Guard: short-circuit when capturedFlag == null || capturedBaseLocation == null.
        if (capturedFlag == null || capturedBaseLocation == null) {
            return;
        }

        getFlagStateRegistry().setAtBase(capturedFlagTeamId, capturedBaseLocation);
        Location indicatorLocation = getTeamBaseMetaDataResolver().resolveBaseIndicatorLocation(capturedFlagTeamId, capturedBaseLocation);
        getFlagBlockPlacer().placeBaseFlagBlock(
            capturedBaseLocation,
            capturedFlagTeamId,
            indicatorLocation
        );

        getFlagCarrierStateHandler().clearCarrierFlagItemAndEffects(player);

        String scoringTeamKey = scoringTeamId.key();
        String capturedTeamKey = capturedFlagTeamId.key();
        getFlagScoreHandler().processFlagCapture(player, scoringTeamKey, capturedTeamKey);

        getFlagEventEffects().showFlagCaptureBroadcast(player, scoringTeamId, capturedFlagTeamId);
        TeamBaseMetaData scoringBaseData = getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(scoringTeamId);
        Location scoringBaseLocation = scoringBaseData == null ? null : scoringBaseData.getFlagSpawnLocation();
        getFlagEventEffects().playFlagCaptureEffects(
            scoringTeamId,
            capturedFlagTeamId,
            scoringBaseLocation == null ? capturedBaseLocation : scoringBaseLocation
        );

        getFlagUiTicker().tickFlagUi();
        String playerName = player.getName();
        sendDebugMessage(LOG_PREFIX + "flag capture team=" + scoringTeamId + " player=" + playerName);
    }

    private void notifyOwnFlagCaptureBlocked(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerUUID = player.getUniqueId();
        long nextAllowedAt = ownFlagNoticeCooldownByPlayerUUID.getOrDefault(playerUUID, 0L);
        // Guard: short-circuit when nextAllowedAt > now.
        if (nextAllowedAt > now) {
            return;
        }

        ownFlagNoticeCooldownByPlayerUUID.put(playerUUID, now + OWN_FLAG_NOTICE_COOLDOWN_MS);
        Component message = getMessage("error.capture_own_flag");
        player.sendMessage(message);
    }
}

