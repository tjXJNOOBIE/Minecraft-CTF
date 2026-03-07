package dev.tjxjnoobie.ctf.game.combat.scout;

import dev.tjxjnoobie.ctf.events.handlers.ScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Adapts Bukkit combat events into scout-tagger ability operations.
 */
public final class ScoutTaggerAbilityEvents implements ScoutTaggerCombatEventHandler {
    private static final long DUPLICATE_USE_WINDOW_MS = 75L;
    private final ScoutTaggerAbility ability;
    private final Map<UUID, Long> lastInteractAtMsByPlayerId = new HashMap<>();

    // == Lifecycle ==
    /**
     * Constructs a ScoutTaggerAbilityEvents instance.
     *
     * @param ability Dependency responsible for scout tagger gameplay flow.
     */
    public ScoutTaggerAbilityEvents(ScoutTaggerAbility ability) {
        this.ability = Objects.requireNonNull(ability, "ability");
    }

    // == Event adapters ==
    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event == null) {
            return;
        }

        // Scout snowballs never deal direct damage; the ability decides whether the hit
        // should just tag/cancel.
        if (event.getDamager() instanceof Snowball snowball && event.getEntity() instanceof Player victim) {
            ability.processScoutSnowballDamageCancel(snowball, victim, event);
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event == null) {
            return;
        }

        if (event.getEntity() instanceof Snowball snowball && event.getHitEntity() instanceof Player victim) {
            ability.processScoutSnowballHit(snowball, victim);
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || !isThrowAction(event.getAction())) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (shouldIgnoreDuplicateInteract(player)) {
            return;
        }

        // Paper can surface a sword use through either hand callback depending on the
        // client path.
        // Suppress same-click duplicates here so the throw still resolves once.
        // Intercept interact events only when the ability actually consumes the throw
        // action.
        if (ability.tryThrowScoutSnowball(player)) {
            event.setCancelled(true);
            event.setUseItemInHand(Result.DENY);
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event == null) {
            return;
        }
        ability.processPlayerQuitCleanup(event.getPlayer());
    }

    /**
     * Evaluates whether isThrowAction is currently satisfied.
     *
     * @param action Event value to inspect.
     * @return {@code true} when the condition is satisfied; otherwise
     *         {@code false}.
     */
    private static boolean isThrowAction(Action action) {
        return action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean shouldIgnoreDuplicateInteract(Player player) {
        if (player == null) {
            return false;
        }

        UUID playerUUID = player.getUniqueId();
        long nowMs = System.currentTimeMillis();
        Long lastInteractAtMs = lastInteractAtMsByPlayerId.get(playerUUID);
        lastInteractAtMsByPlayerId.put(playerUUID, nowMs);
        return lastInteractAtMs != null && nowMs - lastInteractAtMs < DUPLICATE_USE_WINDOW_MS;
    }
}
