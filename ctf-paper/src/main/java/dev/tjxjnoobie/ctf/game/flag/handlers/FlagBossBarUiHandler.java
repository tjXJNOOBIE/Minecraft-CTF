package dev.tjxjnoobie.ctf.game.flag.handlers;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;

import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles return/carrier boss bar rendering for flag UI.
 */
public final class FlagBossBarUiHandler implements MessageAccess, BukkitMessageSender, FlagDependencyAccess, PlayerDependencyAccess {

    // == Constants ==
    private static final double BOSS_BAR_DISTANCE_CAP = 80.0;

    // == Getters ==
    private String resolveDirection(Location from, Location to) {
        // Guard: short-circuit when from == null || to == null.
        if (from == null || to == null) {
            return "N";
        }

        Vector delta = to.toVector().subtract(from.toVector());
        boolean conditionResult1 = Math.abs(delta.getX()) > Math.abs(delta.getZ());
        // Guard: short-circuit when Math.abs(delta.getX()) > Math.abs(delta.getZ()).
        if (conditionResult1) {
            return delta.getX() >= 0 ? "E" : "W";
        }
        return delta.getZ() >= 0 ? "S" : "N";
    }

    // == Setters ==
    public void updateReturnBossBars() {
        for (TeamId teamId : TeamId.values()) {
            updateReturnBossBarsForTeam(teamId);
        }
    }

    public void updateCarrierBossBars() {
        for (Map.Entry<TeamId, FlagMetaData> entry : getFlagMetaDataByTeamId().entrySet()) {
            TeamId carriedFlagTeam = entry.getKey();
            FlagMetaData flag = entry.getValue();
            // Guard: short-circuit when carriedFlagTeam == null || flag == null || flag.state != FlagState.CARRIED || flag.carrier == null.
            if (carriedFlagTeam == null || flag == null || flag.state != FlagState.CARRIED || flag.carrier == null) {
                continue;
            }

            Player carrier = Bukkit.getPlayer(flag.carrier);
            // Guard: short-circuit when carrier == null.
            if (carrier == null) {
                continue;
            }

            String carrierTeamKey = getTeamManager().getTeamKey(carrier);
            TeamId carrierTeam = TeamId.fromKey(carrierTeamKey);
            // Guard: short-circuit when carrierTeam == null.
            if (carrierTeam == null) {
                continue;
            }

            TeamBaseMetaData carrierBaseData = getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(carrierTeam);
            Location carrierBaseLocation = carrierBaseData == null ? null : carrierBaseData.getFlagSpawnLocation();
            // Guard: short-circuit when carrierBaseLocation == null.
            if (carrierBaseLocation == null) {
                continue;
            }

            Location carrierLocation = carrier.getLocation();
            double distance = carrierLocation.distance(carrierBaseLocation);
            float progress = distanceToProgress(distance);
            String direction = resolveDirection(carrierLocation, carrierBaseLocation);
            String distanceText = Integer.toString((int) Math.round(distance));

            Component carrierMessage = getMessageFormatted("bossbar.flag_carrier", direction, distanceText);
            showBossBar(carrier, BukkitBossBarType.CARRIER, carrierMessage, progress);
            Component actionBar = getMessage(CTFKeys.uiFlagCarrierActionbarKey());
            sendActionBar(carrier, actionBar);
        }
    }

    private void updateReturnBossBarsForTeam(TeamId teamId) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return;
        }

        FlagMetaData flag = getFlagStateRegistry().flagFor(teamId);
        if (flag == null || flag.state != FlagState.DROPPED || flag.activeLocation == null) {
            clearReturnBossBars(teamId);
            return;
        }

        String teamKey = teamId.key();
        Location flagLocation = flag.activeLocation;
        for (Player player : getTeamManager().getTeamPlayers(teamKey)) {
            Location playerLocation = player.getLocation();
            double distance = playerLocation.distance(flagLocation);
            float progress = distanceToProgress(distance);

            String direction = resolveDirection(playerLocation, flagLocation);
            String distanceText = Integer.toString((int) Math.round(distance));
            Component returnMessage = getMessageFormatted("bossbar.flag_return", direction, distanceText);
            showBossBar(player, BukkitBossBarType.RETURN, returnMessage, progress);
        }
    }

    // == Utilities ==
    private void clearReturnBossBars(TeamId teamId) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return;
        }
        for (Player player : getTeamManager().getTeamPlayers(teamId.key())) {
            hideBossBar(player, BukkitBossBarType.RETURN);
        }
    }

    private float distanceToProgress(double distance) {
        double clamped = Math.max(0.0, Math.min(BOSS_BAR_DISTANCE_CAP, distance));
        return (float) (1.0 - (clamped / BOSS_BAR_DISTANCE_CAP));
    }
}

