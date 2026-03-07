package dev.tjxjnoobie.ctf.game.combat.scout.util;

import dev.tjxjnoobie.ctf.game.combat.scout.metadata.ScoutTaggerPlayerMetaData;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Shared Scout Tagger state helpers used by gameplay handlers.
 */
public final class ScoutTaggerStateUtil {
    private ScoutTaggerStateUtil() {
    }

    public static boolean isAbilityState(GameState state) {
        return state == GameState.IN_PROGRESS || state == GameState.OVERTIME;
    }

    public static ScoutTaggerPlayerMetaData ensureAmmoEntry(Map<UUID, ScoutTaggerPlayerMetaData> playerStateById,
                                                            UUID playerUUID,
                                                            int maxAmmo) {
        if (playerStateById == null || playerUUID == null) {
            return null;
        }
        return playerStateById.computeIfAbsent(playerUUID, ignored -> new ScoutTaggerPlayerMetaData(maxAmmo));
    }

    public static long getCooldownRemainingMs(Long lastThrowAtMs, long cooldownMs, long nowMs) {
        if (lastThrowAtMs == null || cooldownMs <= 0L) {
            return 0L;
        }
        long elapsedMs = Math.max(0L, nowMs - lastThrowAtMs);
        return Math.max(0L, cooldownMs - elapsedMs);
    }

    public static String formatTenths(long remainingMs) {
        double seconds = Math.max(0.0d, remainingMs / 1000.0d);
        double floored = Math.floor(seconds * 10.0d) / 10.0d;
        return String.format(Locale.US, "%.1f", floored);
    }

    public static boolean hasAssignedTeam(TeamManager teamManager, Player player) {
        if (teamManager == null || player == null) {
            return false;
        }
        return teamManager.getTeamKey(player) != null;
    }

    public static boolean isFriendlyTeamHit(TeamManager teamManager, Player shooter, Player victim) {
        if (teamManager == null || shooter == null || victim == null) {
            return false;
        }

        String shooterTeam = teamManager.getTeamKey(shooter);
        String victimTeam = teamManager.getTeamKey(victim);
        if (shooterTeam == null || victimTeam == null) {
            return false;
        }
        return shooterTeam.equals(victimTeam);
    }
}
