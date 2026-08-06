package dev.tjxjnoobie.ctf.util.bukkit.effects;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Consumer-facing access contract for reusable Bukkit effects.
 */
public interface IBukkitEffectUtil {

    default BukkitEffectUtil getBukkitEffectUtil() {
        return DependencyLoaderAccess.findInstance(BukkitEffectUtil.class);
    }

    default void playSound(Player player, Sound sound, float volume, float pitch) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.playSound(player, sound, volume, pitch);
    }

    default void playSoundToPlayers(Iterable<Player> players, Sound sound, float volume, float pitch) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.playSoundToPlayers(players, sound, volume, pitch);
    }

    default void spawnParticle(World world,
                               Particle particle,
                               Location location,
                               int count,
                               double offsetX,
                               double offsetY,
                               double offsetZ,
                               double extra) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.spawnParticle(world, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    default void spawnParticleForPlayers(Iterable<Player> players,
                                         Particle particle,
                                         Location location,
                                         int count,
                                         double offsetX,
                                         double offsetY,
                                         double offsetZ,
                                         double extra) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.spawnParticleForPlayers(players, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    default void spawnParticle(Player player,
                               Particle particle,
                               Location location,
                               int count,
                               double offsetX,
                               double offsetY,
                               double offsetZ,
                               double extra) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.spawnParticle(player, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    default void spawnFirework(Location location,
                               double offsetX,
                               double offsetY,
                               double offsetZ,
                               Color color,
                               Type type,
                               boolean flicker,
                               boolean trail,
                               int power,
                               int detonateTicks,
                               boolean silent) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.spawnFirework(location, offsetX, offsetY, offsetZ, color, type, flicker, trail, power,
                detonateTicks, silent);
    }

    default void setGlowing(Entity entity, boolean glowing) {
        BukkitEffectUtil effectUtil = getBukkitEffectUtil();
        if (effectUtil == null) {
            return;
        }
        effectUtil.setGlowing(entity, glowing);
    }

    default void setGlowing(Player player, boolean glowing) {
        setGlowing((Entity) player, glowing);
    }
}
