package dev.tjxjnoobie.ctf.util.bukkit.effects;

import dev.tjxjnoobie.ctf.util.tasks.EffectTaskOrchestrator;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * Reusable Bukkit API adapter for sounds, particles, fireworks, and entity effects.
 */
public final class BukkitEffectUtil {

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void playSoundToPlayers(Iterable<Player> players, Sound sound, float volume, float pitch) {
        if (players == null || sound == null) {
            return;
        }
        for (Player player : players) {
            playSound(player, sound, volume, pitch);
        }
    }

    public void spawnParticle(World world,
                              Particle particle,
                              Location location,
                              int count,
                              double offsetX,
                              double offsetY,
                              double offsetZ,
                              double extra) {
        if (world == null || particle == null || location == null) {
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    public void spawnParticleForPlayers(Iterable<Player> players,
                                        Particle particle,
                                        Location location,
                                        int count,
                                        double offsetX,
                                        double offsetY,
                                        double offsetZ,
                                        double extra) {
        if (players == null || particle == null || location == null) {
            return;
        }
        for (Player player : players) {
            spawnParticle(player, particle, location, count, offsetX, offsetY, offsetZ, extra);
        }
    }

    public void spawnParticle(Player player,
                              Particle particle,
                              Location location,
                              int count,
                              double offsetX,
                              double offsetY,
                              double offsetZ,
                              double extra) {
        if (player == null || particle == null || location == null) {
            return;
        }
        player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    public void spawnFirework(Location location,
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
        if (location == null || location.getWorld() == null || color == null || type == null) {
            return;
        }

        Location spawnLocation = location.clone().add(offsetX, offsetY, offsetZ);
        Firework firework = location.getWorld().spawn(spawnLocation, Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .with(type)
                .withColor(color)
                .flicker(flicker)
                .trail(trail)
                .build();

        fireworkMeta.clearEffects();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(Math.max(0, power));
        firework.setFireworkMeta(fireworkMeta);
        firework.setSilent(silent);

        if (detonateTicks <= 0) {
            firework.detonate();
            return;
        }

        EffectTaskOrchestrator.startLater(null, () -> {
            if (!firework.isDead()) {
                firework.detonate();
            }
        }, detonateTicks);
    }

    public void setGlowing(Entity entity, boolean glowing) {
        if (entity == null) {
            return;
        }
        entity.setGlowing(glowing);
    }
}
