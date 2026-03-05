package dev.tjxjnoobie.ctf.game;

import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Provides arena-only access to joined CTF players.
 */
public final class CTFPlayerMetaData {
    private final TeamManager teamManager;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerMetaData instance.
     *
     * @param teamManager Dependency responsible for team manager.
     */
    public CTFPlayerMetaData(TeamManager teamManager) {
        // Capture team manager for arena membership queries.
        this.teamManager = teamManager;
    }

    // == Getters ==
    public List<Player> getPlayers() {
        // Snapshot the currently joined players.
        return teamManager.getJoinedPlayers();
    }

    // == Utilities ==
    /**
     * Executes broadcast.
     *
     * @param message User-facing text value.
     */
    public void broadcast(Component message) {
        // Send chat messages to all arena players.
        for (Player player : getPlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Executes broadcastTitle.
     *
     * @param title User-facing display text.
     */
    public void broadcastTitle(Title title) {
        // Show a title to all arena players.
        for (Player player : getPlayers()) {
            player.showTitle(title);
        }
    }

    /**
     * Executes playSound.
     *
     * @param sound Sound effect to play.
     * @param volume Particle or sound tuning value used for rendering/effects.
     * @param pitch Sound pitch applied during playback.
     */
    public void playSound(Sound sound, float volume, float pitch) {
        // Play sound at each arena player's location.
        for (Player player : getPlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Executes spawnParticle.
     *
     * @param particle Bukkit type used by this operation.
     * @param location World location used by this operation.
     * @param count Numeric value used by this operation.
     * @param offsetX X-axis particle spread offset.
     * @param offsetY Y-axis particle spread offset.
     * @param offsetZ Z-axis particle spread offset.
     * @param extra Particle or sound tuning value used for rendering/effects.
     */
    public void spawnParticle(Particle particle, org.bukkit.Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        // Guard: short-circuit when location == null.
        if (location == null) {
            return;
        }

        // Spawn particles for each arena player at the location.
        for (Player player : getPlayers()) {
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        }
    }

    // == Predicates ==
    public boolean isInArena(Player player) {
        // Team membership implies arena membership.
        return player != null && teamManager.getTeamKey(player) != null;
    }
}

