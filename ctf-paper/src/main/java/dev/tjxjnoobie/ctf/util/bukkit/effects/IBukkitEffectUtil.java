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

    BukkitEffectUtil DEFAULT_BUKKIT_EFFECT_UTIL = new BukkitEffectUtil();

    default BukkitEffectUtil getBukkitEffectUtil() {
        BukkitEffectUtil effectUtil = DependencyLoaderAccess.findInstance(BukkitEffectUtil.class);
        return effectUtil == null ? DEFAULT_BUKKIT_EFFECT_UTIL : effectUtil;
    }

    default void playSound(Player player, Sound sound, float volume, float pitch) {
        getBukkitEffectUtil().playSound(player, sound, volume, pitch);
    }

    default void playSoundToPlayers(Iterable<Player> players, Sound sound, float volume, float pitch) {
        getBukkitEffectUtil().playSoundToPlayers(players, sound, volume, pitch);
    }

    default void spawnParticle(World world,
                               Particle particle,
                               Location location,
                               int count,
                               double offsetX,
                               double offsetY,
                               double offsetZ,
                               double extra) {
        getBukkitEffectUtil().spawnParticle(world, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    default void spawnParticleForPlayers(Iterable<Player> players,
                                         Particle particle,
                                         Location location,
                                         int count,
                                         double offsetX,
                                         double offsetY,
                                         double offsetZ,
                                         double extra) {
        getBukkitEffectUtil().spawnParticleForPlayers(players, particle, location, count, offsetX, offsetY, offsetZ,
                extra);
    }

    default void spawnParticle(Player player,
                               Particle particle,
                               Location location,
                               int count,
                               double offsetX,
                               double offsetY,
                               double offsetZ,
                               double extra) {
        getBukkitEffectUtil().spawnParticle(player, particle, location, count, offsetX, offsetY, offsetZ, extra);
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
        getBukkitEffectUtil().spawnFirework(location, offsetX, offsetY, offsetZ, color, type, flicker, trail, power,
                detonateTicks, silent);
    }

    default void setGlowing(Entity entity, boolean glowing) {
        getBukkitEffectUtil().setGlowing(entity, glowing);
    }

    default void setGlowing(Player player, boolean glowing) {
        setGlowing((Entity) player, glowing);
    }
}
