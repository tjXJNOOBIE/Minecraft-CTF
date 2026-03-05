package dev.tjxjnoobie.ctf.util.bukkit.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Static helpers for entity glow effects.
 */
public final class GlowEffectUtils {

    // == Lifecycle ==
    private GlowEffectUtils() {
    }

    // == Setters ==
    public static void setGlowing(Entity entity, boolean glowing) {
        // Guard: The entity instance is null, usually because it despawned or was
        // never spawned.
        // Guard: short-circuit when entity == null.
        if (entity == null) {
            return;
        }

        // Inform the client to render the glowing outline so players can track this
        // entity through walls.
        entity.setGlowing(glowing);
    }

    public static void setGlowing(Player player, boolean glowing) {
        // Guard: The player instance is null, usually because they are offline or
        // not found.
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        Entity entityTarget = (Entity) player; // Cast the player to an entity to apply the glowing outline for visibility.
        setGlowing(entityTarget, glowing);
    }
}
