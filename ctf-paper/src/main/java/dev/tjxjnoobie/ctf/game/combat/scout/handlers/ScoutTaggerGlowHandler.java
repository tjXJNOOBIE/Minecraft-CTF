package dev.tjxjnoobie.ctf.game.combat.scout.handlers;

import dev.tjxjnoobie.ctf.util.tasks.AbilityTaskOrchestrator;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Tracks temporary glow application for tagged players.
 */
public final class ScoutTaggerGlowHandler implements BukkitEffectsUtil {

    // == Constants ==
    private static final int TAG_DURATION_TICKS = 80;

    // == Runtime state ==
    private final Map<UUID, Integer> glowRemovalTaskByTarget = new HashMap<>();

    // == Lifecycle ==
    /**
     * Constructs a ScoutTaggerGlowHandler instance.
     */
    public ScoutTaggerGlowHandler() {
    }

    /**
     * Executes the stopAll operation.
     */
    public void stopAll() {
        // Cancel every pending removal task before wiping the registry.
        for (Integer taskId : new HashSet<>(glowRemovalTaskByTarget.values())) {
            AbilityTaskOrchestrator.cancelTaskId(taskId);
        }
        glowRemovalTaskByTarget.clear();
    }

    // == Utilities ==
    /**
     * Executes applyGlow.
     *
     * @param target Player involved in this operation.
     */
    public void applyGlow(Player target) {
        // Guard: short-circuit when target == null.
        if (target == null) {
            return;
        }

        cancelGlowRemoval(target.getUniqueId());
        setGlowing(target, true);

        // Reapply glow and replace the expiry task so repeated tags extend the effect cleanly.
        Integer taskId = AbilityTaskOrchestrator.startLaterTaskId(glowRemovalTaskByTarget.get(target.getUniqueId()), () -> {
            Player refreshed = Bukkit.getPlayer(target.getUniqueId());
            if (refreshed != null) {
                setGlowing(refreshed, false);
            }
            glowRemovalTaskByTarget.remove(target.getUniqueId());
        }, TAG_DURATION_TICKS);
        glowRemovalTaskByTarget.put(target.getUniqueId(), taskId);
    }

    /**
     * Executes clearGlow.
     *
     * @param targetId Identifier for the t ar ge t.
     */
    public void clearGlow(UUID targetId) {
        // Guard: short-circuit when targetId == null.
        if (targetId == null) {
            return;
        }
        cancelGlowRemoval(targetId);
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            setGlowing(target, false);
        }
    }

    /**
     * Executes cancelGlowRemoval.
     *
     * @param targetId Identifier for the target.
     */
    private void cancelGlowRemoval(UUID targetId) {
        Integer taskId = glowRemovalTaskByTarget.remove(targetId);
        AbilityTaskOrchestrator.cancelTaskId(taskId);
    }
}
