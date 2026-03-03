package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CTFPlayerContainer;
import dev.tjxjnoobie.ctf.game.player.managers.PlayerManager;
import dev.tjxjnoobie.ctf.game.state.managers.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/**
 * Handles score updates and win/overtime rules on successful flag capture.
 */
public final class FlagCaptureHandler implements MessageAccess {
    private final IntSupplier winScoreSupplier;
    private final ScoreBoardManager scoreBoardManager;
    private final TeamManager teamManager;
    private final CTFPlayerContainer arenaPlayers;
    private final GameStateManager gameStateManager;
    private final PlayerManager playerManager;
    private final Supplier<Duration> remainingTimeSupplier;
    private final BiConsumer<MatchStopReason, String> stopRequester;
    private final Consumer<String> debugPublisher;

    public FlagCaptureHandler(
        IntSupplier winScoreSupplier,
        ScoreBoardManager scoreBoardManager,
        TeamManager teamManager,
        CTFPlayerContainer arenaPlayers,
        GameStateManager gameStateManager,
        PlayerManager playerManager,
        Supplier<Duration> remainingTimeSupplier,
        BiConsumer<MatchStopReason, String> stopRequester,
        Consumer<String> debugPublisher
    ) {
        this.winScoreSupplier = winScoreSupplier;
        this.scoreBoardManager = scoreBoardManager;
        this.teamManager = teamManager;
        this.arenaPlayers = arenaPlayers;
        this.gameStateManager = gameStateManager;
        this.playerManager = playerManager;
        this.remainingTimeSupplier = remainingTimeSupplier;
        this.stopRequester = stopRequester;
        this.debugPublisher = debugPublisher;
    }

    public int handleCapture(Player player, String scoringTeam, String capturedFlagTeam) {
        int newScore = scoreBoardManager.incrementScore(scoringTeam);
        playerManager.recordCapture(player, remainingTimeSupplier.get(), gameStateManager.getGameState());

        Title title = Title.title(
            msg("title.score.header", Map.of(
                "player", player.getName(),
                "team", teamManager.getDisplayName(scoringTeam)
            )),
            msg("title.score.sub", Map.of(
                "team", teamManager.getDisplayName(scoringTeam)
            ))
        );
        arenaPlayers.broadcastTitle(title);
        debugPublisher.accept("capture scorer=" + player.getName() + " team=" + scoringTeam + " score=" + newScore);

        if (gameStateManager.getGameState() == GameState.OVERTIME) {
            stopRequester.accept(MatchStopReason.WIN, scoringTeam);
            return newScore;
        }

        int winScore = winScoreSupplier == null ? 0 : Math.max(1, winScoreSupplier.getAsInt());
        if (newScore >= winScore) {
            stopRequester.accept(MatchStopReason.WIN, scoringTeam);
        }
        return newScore;
    }
}



