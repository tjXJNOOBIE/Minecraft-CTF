package dev.tjxjnoobie.ctf.game.lifecycle.util;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Shared helper methods used during match cleanup and lobby reset.
 */
public final class MatchCleanupUtil {

    private MatchCleanupUtil() {
    }

    public static Component resolveStopMessage(MessageAccess messageAccess,
                                               Function<String, String> teamDisplayNameResolver,
                                               MatchStopReason reason,
                                               String winningTeamKey) {
        MatchStopReason resolved = reason == null ? MatchStopReason.GENERIC : reason;
        return switch (resolved) {
            case ADMIN -> messageAccess.getMessage("broadcast.stop.admin");
            case TIMEOUT -> messageAccess.getMessage("broadcast.stop.timeout");
            case WIN -> messageAccess.getMessageFormatted(
                    "broadcast.win",
                    teamDisplayNameResolver == null ? winningTeamKey : teamDisplayNameResolver.apply(winningTeamKey));
            case GENERIC -> messageAccess.getMessage("broadcast.stop.generic");
        };
    }

    public static List<Player> snapshotPlayers(List<Player> players) {
        return players == null ? new ArrayList<>() : new ArrayList<>(players);
    }

    public static void forEachPlayer(List<Player> players, Consumer<Player> action) {
        if (players == null || action == null) {
            return;
        }

        for (Player player : players) {
            if (player != null) {
                action.accept(player);
            }
        }
    }

    public static void clearInventories(List<Player> players) {
        forEachPlayer(players, player -> player.getInventory().clear());
    }

    public static void sendMessageToPlayers(List<Player> players, Consumer<Player> messageAction) {
        forEachPlayer(players, messageAction);
    }

    public static void enableWinnerCelebrationFlight(String winningTeamKey,
                                                     List<Player> joinedPlayers,
                                                     Function<Player, String> teamKeyResolver) {
        if (winningTeamKey == null || joinedPlayers == null || teamKeyResolver == null) {
            return;
        }

        forEachPlayer(joinedPlayers, player -> {
            if (!winningTeamKey.equals(teamKeyResolver.apply(player))) {
                return;
            }
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFallDistance(0.0f);
        });
    }

    public static void restorePostCleanupPlayerState(List<Player> joinedPlayers) {
        forEachPlayer(joinedPlayers, player -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            if (player.isFlying()) {
                player.setFlying(false);
            }
            player.setFallDistance(0.0f);
        });
    }
}
