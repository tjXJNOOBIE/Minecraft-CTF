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
public final class CTFPlayerContainer {
    private final TeamManager teamManager;

    public CTFPlayerContainer(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * Returns true when the player is joined to a CTF team.
     */
    public boolean isInArena(Player player) {
        return player != null && teamManager.getTeamKey(player) != null;
    }

    /**
     * Returns a list of players currently in the match.
     */
    public List<Player> getPlayers() {
        return teamManager.getJoinedPlayers();
    }

    /**
     * Sends a chat message to all arena players.
     */
    public void broadcast(Component message) {
        for (Player player : getPlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Sends a title to all arena players.
     */
    public void broadcastTitle(Title title) {
        for (Player player : getPlayers()) {
            player.showTitle(title);
        }
    }

    /**
     * Plays a sound to all arena players.
     */
    public void playSound(Sound sound, float volume, float pitch) {
        for (Player player : getPlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Spawns particles for all arena players.
     */
    public void spawnParticle(Particle particle, org.bukkit.Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (location == null) {
            return;
        }

        for (Player player : getPlayers()) {
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        }
    }
}

