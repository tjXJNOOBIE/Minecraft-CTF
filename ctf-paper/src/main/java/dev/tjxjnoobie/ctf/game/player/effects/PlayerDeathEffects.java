package dev.tjxjnoobie.ctf.game.player.effects;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Owns UX feedback for player death events: broadcast messaging, boss bars, and sounds.
 */
public final class PlayerDeathEffects implements MessageAccess, BukkitMessageSender, BukkitEffectsUtil {

    // == Constants ==
    private static final String LOG_PREFIX = "[CTFPlayerDeathEvent] ";

    // == Utilities ==
    /**
     * Executes playAllDeathEffects.
     *
     * @param victim Player involved in this operation.
     * @param killer Player involved in this operation.
     * @param creditedKiller Control value that changes behavior for this operation.
     * @param spearDeathMessage User-facing display text.
     * @param wasCarrier Control value that changes behavior for this operation.
     */
    public void playAllDeathEffects(
            Player victim,
            Player killer,
            Player creditedKiller,
            Component spearDeathMessage,
            boolean wasCarrier) {
        // Guard: short-circuit when victim == null.
        if (victim == null) {
            return;
        }
        // Broadcast death message, kill effects, and sounds.
        announceDeath(victim, killer, spearDeathMessage);
        playAllKillEffects(creditedKiller, victim, wasCarrier);
        playDeathSound(victim);
    }

    /**
     * Executes playAllKillEffects.
     *
     * @param killer Player involved in this operation.
     * @param victim Player involved in this operation.
     * @param wasCarrier Control value that changes behavior for this operation.
     */
    public void playAllKillEffects(Player killer, Player victim, boolean wasCarrier) {
        // Guard: short-circuit when killer == null || victim == null.
        if (killer == null || victim == null) {
            return;
        }
        // Show the kill bar and carrier stop message when needed.
        showKillBar(killer, victim);
        if (wasCarrier) {
            announceCarrierStop(killer, victim);
        }
    }

    /**
     * Executes announceDeath.
     *
     * @param victim Player involved in this operation.
     * @param killer Player involved in this operation.
     * @param spearDeathMessage User-facing display text.
     */
    public void announceDeath(Player victim, Player killer, Component spearDeathMessage) {
        // Broadcast the resolved death message to the arena.
        broadcastToArena(buildDeathMessage(victim, killer, spearDeathMessage));
    }

    /**
     * Executes showKillBar.
     *
     * @param killer Player involved in this operation.
     * @param victim Player involved in this operation.
     */
    public void showKillBar(Player killer, Player victim) {
        // Show a short-lived kill bar and play a sound.
        showBossBar(killer, BukkitBossBarType.KILL, buildKillBarMessage(victim), 1.0f);
        tryPlayKillSound(killer);
    }

    /**
     * Executes announceCarrierStop.
     *
     * @param killer Player involved in this operation.
     * @param victim Player involved in this operation.
     */
    public void announceCarrierStop(Player killer, Player victim) {
        // Broadcast a carrier-stop message to the arena.
        broadcastToArena(buildCarrierStopMessage(killer, victim));
    }

    /**
     * Executes playDeathSound.
     *
     * @param victim Player involved in this operation.
     */
    public void playDeathSound(Player victim) {
        // Play the victim's death sound safely.
        tryPlayDeathSound(victim);
    }

    private void tryPlayKillSound(Player killer) {
        try {
            // Play a short success sound to the killer.
            playSound(killer, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        } catch (Throwable ex) {
            String errorMessage = ex.getMessage();
            sendDebugMessage(LOG_PREFIX + "Kill sound skipped - reason=" + errorMessage);
        }
    }

    private void tryPlayDeathSound(Player victim) {
        try {
            // Play a heavy impact sound to the victim.
            playSound(victim, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        } catch (Throwable ex) {
            String errorMessage = ex.getMessage();
            sendDebugMessage(LOG_PREFIX + "Death sound skipped - reason=" + errorMessage);
        }
    }

    private Component buildDeathMessage(Player victim, Player killer, Component spearDeathMessage) {
        // Guard: short-circuit when spearDeathMessage != null.
        if (spearDeathMessage != null) {
            return spearDeathMessage;
        }
        Component victimName = victim.displayName();
        if (killer != null) {
            Component killerName = killer.displayName();
            return killerName
                .append(Component.text(" killed ", NamedTextColor.GRAY))
                .append(victimName)
                .append(Component.text(".", NamedTextColor.GRAY));
        }
        return victimName.append(Component.text(" died.", NamedTextColor.GRAY));
    }

    private Component buildKillBarMessage(Player victim) {
        return getMessageFormatted("bossbar.kill", victim.getName());
    }

    private Component buildCarrierStopMessage(Player killer, Player victim) {
        Component killerName = killer.displayName();
        Component victimName = victim.displayName();
        return killerName
            .append(Component.text(" stopped the flag carrier ", NamedTextColor.GRAY))
            .append(victimName)
            .append(Component.text("!", NamedTextColor.GRAY));
    }
}
