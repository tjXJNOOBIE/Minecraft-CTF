package dev.tjxjnoobie.ctf.game.combat.scout.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Shared metadata helpers for Scout Tagger snowball projectiles.
 */
public final class ScoutTaggerProjectileUtil {
    private ScoutTaggerProjectileUtil() {
    }

    public static void tagSnowball(Snowball snowball, NamespacedKey snowballKey, NamespacedKey shooterKey, UUID shooterId) {
        if (snowball == null || snowballKey == null || shooterKey == null || shooterId == null) {
            return;
        }
        snowball.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        snowball.getPersistentDataContainer().set(shooterKey, PersistentDataType.STRING, shooterId.toString());
    }

    public static boolean isTaggedSnowball(Snowball snowball, NamespacedKey snowballKey) {
        return snowball != null
                && snowballKey != null
                && snowball.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE);
    }

    public static Player resolveShooter(Snowball snowball, NamespacedKey shooterKey) {
        if (snowball == null || shooterKey == null) {
            return null;
        }

        ProjectileSource shooterSource = snowball.getShooter();
        if (shooterSource instanceof Player player) {
            return player;
        }

        String shooterRaw = snowball.getPersistentDataContainer().get(shooterKey, PersistentDataType.STRING);
        if (shooterRaw == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(shooterRaw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
