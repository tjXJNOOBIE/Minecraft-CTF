package dev.tjxjnoobie.ctf.game.tasks;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.GameLoopTimer;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Maintains lobby waiting visuals and auto-start trigger checks.
 */
public final class LobbyWaitingTask implements Runnable, MessageAccess {
    private final int minPlayers;
    private final TeamManager teamManager;
    private final BossBarManager bossBarManager;
    private final GameStateManager gameStateManager;
    private final GameLoopTimer gameLoopManager;
    private final FlagBaseSetupHandler flagBaseSetupHandler;
    private final Runnable autoCountdownStarter;

    // == Lifecycle ==
    /**
     * Constructs a LobbyWaitingTask instance.
     *
     * @param minPlayers Numeric value used by this operation.
     * @param teamManager Dependency responsible for team manager.
     * @param bossBarManager Service dependency required by this operation.
     * @param gameStateManager Dependency responsible for game state manager.
     * @param gameLoopManager Service dependency required by this operation.
     * @param flagBaseSetupHandler Service dependency required by this operation.
     * @param autoCountdownStarter Callback that starts the automatic pre-match countdown.
     */
    public LobbyWaitingTask(
        int minPlayers,
        TeamManager teamManager,
        BossBarManager bossBarManager,
        GameStateManager gameStateManager,
        GameLoopTimer gameLoopManager,
        FlagBaseSetupHandler flagBaseSetupHandler,
        Runnable autoCountdownStarter
    ) {
        this.minPlayers = minPlayers;
        this.teamManager = teamManager;
        this.bossBarManager = bossBarManager;
        this.gameStateManager = gameStateManager;
        this.gameLoopManager = gameLoopManager;
        this.flagBaseSetupHandler = flagBaseSetupHandler;
        this.autoCountdownStarter = autoCountdownStarter;
    }

    // == Utilities ==
    /**
     * Executes the run operation.
     */
    @Override
    public void run() {
        boolean cleanupInProgress = gameStateManager.isCleanupInProgress();
        // Guard: short-circuit when cleanupInProgress.
        if (cleanupInProgress) {
            return;
        }

        boolean conditionResult1 = gameStateManager.getGameState() != GameState.LOBBY;
        if (conditionResult1) {
            // Clear lobby bars once the match leaves lobby state.
            for (Player player : teamManager.getJoinedPlayers()) {
                bossBarManager.hideWaitingBar(player);
            }
            return;
        }

        int joined = teamManager.getJoinedPlayerCount(); // Update waiting UI based on joined count.
        float progress = Math.min(1.0f, joined / (float) minPlayers);

        Component actionBar = null;
        Component bossBar = null;
        if (joined < minPlayers) {
            String joinedText = Integer.toString(joined);
            String requiredText = Integer.toString(minPlayers);
            actionBar = getMessageFormatted("actionbar.waiting", joinedText, requiredText);
            bossBar = getMessageFormatted("bossbar.waiting", joinedText, requiredText);
        }

        for (Player player : teamManager.getJoinedPlayers()) {
            if (joined < minPlayers) {
                player.sendActionBar(actionBar);
                bossBarManager.showWaitingBar(player, bossBar, progress);
            } else {
                bossBarManager.hideWaitingBar(player);
            }
        }

        // Auto-start countdown when conditions are met.
        if (joined >= minPlayers && !gameLoopManager.isStartCountdownActive()
            && flagBaseSetupHandler != null && flagBaseSetupHandler.areBasesReady()) {
            gameStateManager.setForcedCountdown(false);
            autoCountdownStarter.run();
        }
    }
}

