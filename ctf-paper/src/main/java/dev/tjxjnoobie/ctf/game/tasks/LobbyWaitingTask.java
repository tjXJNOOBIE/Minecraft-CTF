package dev.tjxjnoobie.ctf.game.tasks;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.GameLoopManager;
import dev.tjxjnoobie.ctf.game.state.managers.GameStateManager;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.Map;
import org.bukkit.entity.Player;

/**
 * Maintains lobby waiting visuals and auto-start trigger checks.
 */
public final class LobbyWaitingTask implements Runnable, MessageAccess {
    private final int minPlayers;
    private final TeamManager teamManager;
    private final BossBarManager bossBarManager;
    private final GameStateManager gameStateManager;
    private final GameLoopManager gameLoopManager;
    private final FlagManager flagManager;
    private final Runnable autoCountdownStarter;

    public LobbyWaitingTask(
        int minPlayers,
        TeamManager teamManager,
        BossBarManager bossBarManager,
        GameStateManager gameStateManager,
        GameLoopManager gameLoopManager,
        FlagManager flagManager,
        Runnable autoCountdownStarter
    ) {
        this.minPlayers = minPlayers;
        this.teamManager = teamManager;
        this.bossBarManager = bossBarManager;
        this.gameStateManager = gameStateManager;
        this.gameLoopManager = gameLoopManager;
        this.flagManager = flagManager;
        this.autoCountdownStarter = autoCountdownStarter;
    }

    @Override
    public void run() {
        if (gameStateManager.isCleanupInProgress()) {
            return;
        }

        if (gameStateManager.getGameState() != GameState.LOBBY) {
            for (Player player : teamManager.getJoinedPlayers()) {
                bossBarManager.hideWaitingBar(player);
            }
            return;
        }

        int joined = teamManager.getJoinedPlayerCount();
        float progress = Math.min(1.0f, joined / (float) minPlayers);

        for (Player player : teamManager.getJoinedPlayers()) {
            if (joined < minPlayers) {
                player.sendActionBar(msg("actionbar.waiting", Map.of(
                    "joined", Integer.toString(joined),
                    "required", Integer.toString(minPlayers)
                )));
                bossBarManager.showWaitingBar(player, msg("bossbar.waiting", Map.of(
                    "joined", Integer.toString(joined),
                    "required", Integer.toString(minPlayers)
                )), progress);
            } else {
                bossBarManager.hideWaitingBar(player);
            }
        }

        if (joined >= minPlayers && !gameLoopManager.isStartCountdownActive() && flagManager.areBasesReady()) {
            gameStateManager.setForcedCountdown(false);
            autoCountdownStarter.run();
        }
    }
}



