package dev.tjxjnoobie.ctf.game.combat.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Trident;
import org.bukkit.persistence.PersistentDataType;

/**
 * Shared projectile metadata helpers for Homing Spear entities.
 */
public final class HomingSpearProjectileUtil {
    private HomingSpearProjectileUtil() {
    }

    /**
     * Returns whether the trident is tagged as a tracked homing spear.
     *
     * @param trident Projectile to inspect.
     * @param spearKey Persistent-data key used for tracked spear markers.
     * @return {@code true} when the tracked-spear marker exists.
     */
    public static boolean isTrackedSpear(Trident trident, NamespacedKey spearKey) {
        return trident != null
                && spearKey != null
                && trident.getPersistentDataContainer().has(spearKey, PersistentDataType.BYTE);
    }

    /**
     * Returns the shooter uuid stored on the tracked spear projectile.
     *
     * @param trident Projectile carrying shooter metadata.
     * @param shooterKey Persistent-data key used for the stored shooter id.
     * @return Shooter uuid, or {@code null} when the metadata is missing or invalid.
     */
    public static UUID getShooterId(Trident trident, NamespacedKey shooterKey) {
        if (trident == null || shooterKey == null) {
            return null;
        }

        String raw = trident.getPersistentDataContainer().get(shooterKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Tags the projectile as an active tracked homing spear.
     *
     * @param trident Projectile to tag.
     * @param spearKey Key used for the tracked-spear marker.
     * @param shooterKey Key used for the shooter metadata.
     * @param shooterId Shooter uuid to store on the projectile.
     */
    public static void markTrackedSpear(Trident trident, NamespacedKey spearKey, NamespacedKey shooterKey, UUID shooterId) {
        if (trident == null || spearKey == null || shooterKey == null || shooterId == null) {
            return;
        }
        // Store both the generic tracked marker and the owner id so later combat hooks can resolve attribution.
        trident.getPersistentDataContainer().set(spearKey, PersistentDataType.BYTE, (byte) 1);
        trident.getPersistentDataContainer().set(shooterKey, PersistentDataType.STRING, shooterId.toString());
    }

    /**
     * Tags the projectile as a return-state spear for later cleanup.
     *
     * @param trident Projectile to tag.
     * @param returnShooterKey Key used for the returning-spear owner id.
     * @param shooterId Shooter uuid to store on the projectile.
     */
    public static void markReturningSpear(Trident trident, NamespacedKey returnShooterKey, UUID shooterId) {
        if (trident == null || returnShooterKey == null || shooterId == null) {
            return;
        }
        trident.getPersistentDataContainer().set(returnShooterKey, PersistentDataType.STRING, shooterId.toString());
    }

    /**
     * Removes all return-state tridents that belong to the given shooter.
     *
     * @param shooterId Shooter uuid used to match return-state projectiles.
     * @param returnShooterKey Key used for the returning-spear owner id.
     */
    public static void removeReturningTridents(UUID shooterId, NamespacedKey returnShooterKey) {
        if (shooterId == null || returnShooterKey == null) {
            return;
        }
        String shooterRaw = shooterId.toString();
        for (World world : Bukkit.getWorlds()) {
            // Scan every loaded world because return-state projectiles are only tracked by persistent data.
            for (Trident trident : world.getEntitiesByClass(Trident.class)) {
                String owner = trident.getPersistentDataContainer()
                        .get(returnShooterKey, PersistentDataType.STRING);
                if (shooterRaw.equals(owner)) {
                    trident.remove();
                }
            }
        }
    }
}
