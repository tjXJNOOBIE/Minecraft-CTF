package dev.tjxjnoobie.ctf.util.bukkit.effects;

import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Default interface for common Bukkit effects utilities.
 */
public interface BukkitEffectsUtil {
    default void playSound(Player player, Sound sound, float volume, float pitch) {
        // Forward single-player sound playback.
        ParticleEffectUtil.playSound(player, sound, volume, pitch);
    }

    default void playSoundToPlayers(Iterable<Player> players, Sound sound, float volume, float pitch) {
        // Forward bulk sound playback to all players.
        ParticleEffectUtil.playSoundToPlayers(players, sound, volume, pitch);
    }

    default void spawnParticle(World world,
                               Particle particle,
                               Location location,
                               int count,
                               double offX,
                               double offY,
                               double offZ,
                               double extra) {
        // Forward world particle spawning.
        ParticleEffectUtil.spawnParticle(world, particle, location, count, offX, offY, offZ, extra);
    }

    default void spawnParticleForPlayers(Iterable<Player> players,
                                         Particle particle,
                                         Location location,
                                         int count,
                                         double offX,
                                         double offY,
                                         double offZ,
                                         double extra) {
        // Forward per-player particle spawning.
        ParticleEffectUtil.spawnParticleForPlayers(players, particle, location, count, offX, offY, offZ, extra);
    }

    default void spawnParticle(Player player,
                               Particle particle,
                               Location location,
                               int count,
                               double offX,
                               double offY,
                               double offZ,
                               double extra) {
        // Forward single-player particle spawning.
        ParticleEffectUtil.spawnParticle(player, particle, location, count, offX, offY, offZ, extra);
    }

    default void spawnFirework(Location location,
                               double offX,
                               double offY,
                               double offZ,
                               Color color,
                               Type type,
                               boolean flicker,
                               boolean trail,
                               int power,
                               int detonateTicks,
                               boolean silent) {
        // Forward firework spawn configuration.
        ParticleEffectUtil.spawnFirework(location, offX, offY, offZ, color, type, flicker, trail, power, detonateTicks, silent);
    }

    default void setGlowing(Entity entity, boolean glowing) {
        // Forward glow toggling for any entity.
        GlowEffectUtils.setGlowing(entity, glowing);
    }

    default void setGlowing(Player player, boolean glowing) {
        // Forward glow toggling for a player.
        GlowEffectUtils.setGlowing(player, glowing);
    }
}
