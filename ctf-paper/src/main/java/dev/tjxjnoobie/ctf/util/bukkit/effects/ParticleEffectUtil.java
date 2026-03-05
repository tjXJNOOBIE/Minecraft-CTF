package dev.tjxjnoobie.ctf.util.bukkit.effects;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import dev.tjxjnoobie.ctf.util.tasks.EffectTaskOrchestrator;

/**
 * Static helpers for common sound, particle, and firework effects.
 */
public final class ParticleEffectUtil {

    // == Lifecycle ==
    private ParticleEffectUtil() {
    }

    // == Utilities ==
    /**
     * Executes playSound.
     *
     * @param player Player involved in this operation.
     * @param sound Sound effect to play.
     * @param volume Particle or sound tuning value used for rendering/effects.
     * @param pitch Sound pitch applied during playback.
     */
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        // Guard: short-circuit when player == null || sound == null.
        if (player == null || sound == null) {
            return;
        }
        // Play sound at the player's current location.
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Executes playSoundToPlayers.
     *
     * @param players Players involved in this operation.
     * @param sound Sound effect to play.
     * @param volume Particle or sound tuning value used for rendering/effects.
     * @param pitch Sound pitch applied during playback.
     */
    public static void playSoundToPlayers(Iterable<Player> players, Sound sound, float volume, float pitch) {
        // Guard: short-circuit when players == null || sound == null.
        if (players == null || sound == null) {
            return;
        }
        // Fan-out playback across all players.
        for (Player player : players) {
            playSound(player, sound, volume, pitch);
        }
    }

    /**
     * Executes spawnParticle.
     *
     * @param world Bukkit type used by this operation.
     * @param particle Bukkit type used by this operation.
     * @param location World location used by this operation.
     * @param count Numeric value used by this operation.
     * @param offX Particle or sound tuning value used for rendering/effects.
     * @param offY Particle or sound tuning value used for rendering/effects.
     * @param offZ Particle or sound tuning value used for rendering/effects.
     * @param extra Particle or sound tuning value used for rendering/effects.
     */
    public static void spawnParticle(World world,
                                     Particle particle,
                                     Location location,
                                     int count,
                                     double offX,
                                     double offY,
                                     double offZ,
                                     double extra) {
        // Guard: short-circuit when world == null || particle == null || location == null.
        if (world == null || particle == null || location == null) {
            return;
        }
        // Spawn particles directly in the world.
        world.spawnParticle(particle, location, count, offX, offY, offZ, extra);
    }

    /**
     * Executes spawnParticleForPlayers.
     *
     * @param players Players involved in this operation.
     * @param particle Bukkit type used by this operation.
     * @param location World location used by this operation.
     * @param count Numeric value used by this operation.
     * @param offX Particle or sound tuning value used for rendering/effects.
     * @param offY Particle or sound tuning value used for rendering/effects.
     * @param offZ Particle or sound tuning value used for rendering/effects.
     * @param extra Particle or sound tuning value used for rendering/effects.
     */
    public static void spawnParticleForPlayers(Iterable<Player> players,
                                               Particle particle,
                                               Location location,
                                               int count,
                                               double offX,
                                               double offY,
                                               double offZ,
                                               double extra) {
        // Guard: short-circuit when players == null || particle == null || location == null.
        if (players == null || particle == null || location == null) {
            return;
        }
        // Spawn particles for each player individually.
        for (Player player : players) {
            if (player != null) {
                player.spawnParticle(particle, location, count, offX, offY, offZ, extra);
            }
        }
    }

    /**
     * Executes spawnParticle.
     *
     * @param player Player involved in this operation.
     * @param particle Bukkit type used by this operation.
     * @param location World location used by this operation.
     * @param count Numeric value used by this operation.
     * @param offX Particle or sound tuning value used for rendering/effects.
     * @param offY Particle or sound tuning value used for rendering/effects.
     * @param offZ Particle or sound tuning value used for rendering/effects.
     * @param extra Particle or sound tuning value used for rendering/effects.
     */
    public static void spawnParticle(Player player,
                                     Particle particle,
                                     Location location,
                                     int count,
                                     double offX,
                                     double offY,
                                     double offZ,
                                     double extra) {
        // Guard: short-circuit when player == null || particle == null || location == null.
        if (player == null || particle == null || location == null) {
            return;
        }
        // Spawn particles for a single player.
        player.spawnParticle(particle, location, count, offX, offY, offZ, extra);
    }

    /**
     * Executes spawnFirework.
     *
     * @param location World location used by this operation.
     * @param offX Particle or sound tuning value used for rendering/effects.
     * @param offY Particle or sound tuning value used for rendering/effects.
     * @param offZ Particle or sound tuning value used for rendering/effects.
     * @param color Firework primary color.
     * @param type Firework burst type.
     * @param flicker {@code true} to enable flicker effect.
     * @param trail {@code true} to enable trail effect.
     * @param power Firework flight power before detonation.
     * @param detonateTicks Ticks to wait before detonation; {@code <= 0} detonates immediately.
     * @param silent {@code true} to suppress firework sound.
     */
    public static void spawnFirework(Location location,
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
        boolean conditionResult1 = location == null || location.getWorld() == null || color == null || type == null;
        // Guard: short-circuit when location == null || location.getWorld() == null || color == null || type == null.
        if (conditionResult1) {
            return;
        }

        Location spawnLocation = location.clone().add(offX, offY, offZ); // Offset the spawn location for the firework.
        Firework firework = location.getWorld().spawn(spawnLocation, Firework.class); // Spawn and configure a Bukkit firework entity.
        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        // Build the visual firework effect.
        FireworkEffect effect = FireworkEffect.builder()
            .with(type)
            .withColor(color)
            .flicker(flicker)
            .trail(trail)
            .build();

        // Apply effect metadata and power.
        fireworkMeta.clearEffects();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(Math.max(0, power));
        firework.setFireworkMeta(fireworkMeta);
        firework.setSilent(silent);

        if (detonateTicks <= 0) {
            // Detonate immediately when no delay is requested.
            firework.detonate();
            return;
        }
        // Schedule a delayed detonation.
        EffectTaskOrchestrator.startLater(null, () -> {
            boolean dead = firework.isDead();
            if (!dead) {
                firework.detonate();
            }
        }, detonateTicks);
    }
}
