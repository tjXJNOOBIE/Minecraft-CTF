package dev.tjxjnoobie.ctf.game.lifecycle.handlers;

import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtils;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;
import dev.tjxjnoobie.ctf.util.tasks.EffectTaskOrchestrator;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Shared match presentation effects (sounds, particles, title animation).
 */
public final class MatchEffectsHandler implements BukkitEffectsUtil {

    // == Constants ==
    private static final String OVERTIME_TEXT = "OVERTIME";
    private static final int OVERTIME_STEP_COUNT = OVERTIME_TEXT.length();
    private static final int OVERTIME_TOTAL_TICKS = 46;
    private static final int OVERTIME_STEP_TICKS = Math.max(1, OVERTIME_TOTAL_TICKS / OVERTIME_STEP_COUNT);
    private static final Title.Times OVERTIME_STEP_TIMES = Title.Times.times(
        Duration.ofMillis(80),
        Duration.ofMillis((OVERTIME_STEP_TICKS * 50L) + 40L),
        Duration.ofMillis(80)
    );
    private static final Title.Times OVERTIME_FINAL_TIMES = Title.Times.times(
        Duration.ofMillis(150),
        Duration.ofMillis(4500),
        Duration.ofMillis(200)
    );

    // == Utilities ==
    /**
     * Executes playMatchStartEffects.
     *
     * @param teamManager Dependency responsible for team manager.
     */
    public void playMatchStartEffects(TeamManager teamManager) {
        // Play start-of-match effects for all joined players.
        for (org.bukkit.entity.Player player : teamManager.getJoinedPlayers()) {
            playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
            spawnParticle(
                player,
                Particle.FIREWORK,
                player.getLocation().add(0.0, 1.0, 0.0),
                25,
                0.4,
                0.6,
                0.4,
                0.02
            );
        }
    }

    /**
     * Executes playSoundToJoined.
     *
     * @param teamManager Dependency responsible for team manager.
     * @param sound Sound effect to play.
     * @param pitch Sound pitch applied during playback.
     */
    public void playSoundToJoined(TeamManager teamManager, Sound sound, float pitch) {
        // Broadcast a sound to all joined players.
        playSoundToPlayers(teamManager.getJoinedPlayers(), sound, 1.0f, pitch);
    }

    /**
     * Executes playOvertimeReveal.
     *
     * @param gameStateManager Dependency responsible for game state manager.
     * @param arenaPlayers Collection or state object updated by this operation.
     */
    public void playOvertimeReveal(GameStateManager gameStateManager, CTFPlayerMetaData arenaPlayers) {
        // Animate the overtime title reveal and sounds.
        arenaPlayers.playSound(Sound.AMBIENT_CAVE, 1.0f, 0.6f);

        for (int i = 1; i <= OVERTIME_STEP_COUNT; i++) {
            int revealed = i;
            EffectTaskOrchestrator.startLater(null, () -> {
                boolean conditionResult1 = gameStateManager.getGameState() != GameState.OVERTIME;
                // Guard: short-circuit when gameStateManager.getGameState() != GameState.OVERTIME.
                if (conditionResult1) {
                    return;
                }

                String left = OVERTIME_TEXT.substring(0, revealed);
                String right = OVERTIME_TEXT.substring(revealed);
                Component title = Component.text(left, NamedTextColor.RED, TextDecoration.BOLD);
                boolean empty = right.isEmpty();
                if (!empty) {
                    title = title.append(Component.text(right, NamedTextColor.DARK_RED, TextDecoration.BOLD));
                }

                Component subtitle = Component.empty();
                if (revealed == OVERTIME_STEP_COUNT) {
                    subtitle = Component.text("Sudden Death: First to score wins", NamedTextColor.GOLD);
                    arenaPlayers.playSound(Sound.BLOCK_BELL_RESONATE, 1.0f, 1.0f);
                }

                Title reveal = BukkitMessageUtils.title(
                    title,
                    subtitle,
                    revealed == OVERTIME_STEP_COUNT ? OVERTIME_FINAL_TIMES : OVERTIME_STEP_TIMES
                );
                arenaPlayers.broadcastTitle(reveal);
            }, (long) i * OVERTIME_STEP_TICKS);
        }
    }
}
