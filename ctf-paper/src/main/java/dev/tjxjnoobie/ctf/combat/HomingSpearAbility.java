package dev.tjxjnoobie.ctf.combat;

import dev.tjxjnoobie.ctf.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.combat.metadata.SpearLockMetaData;
import dev.tjxjnoobie.ctf.combat.metadata.SpearShooterMetaData;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.kit.KitManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Provides Ranger's F-key Homing Spear ability.
 */
public final class HomingSpearAbility implements Listener, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [HomingSpearAbility] ";
    private static final String SPEAR_NAME = "Homing Spear";
    private static final String SPEAR_LOCKED_NAME = "Spear Locked";
    private static final String SPEAR_RETURNING_NAME = "Spear Returning";
    private static final long COOLDOWN_MILLIS = 30_000L;
    private static final int ACTION_BAR_REFRESH_TICKS = 1;
    private static final int TOTAL_LIFETIME_TICKS = 100;
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int UPDATE_INTERVAL_TICKS = 1;
    private static final int DAMAGE_RETURN_DELAY_TICKS = 2;
    private static final int SCAN_RANGE_BLOCKS = 20;
    private static final double HIT_DAMAGE = 3.0;
    private static final double LOCK_STEER_STRENGTH = 0.48;
    private static final double CRUISE_SPEED = 1.6;
    private static final double DROP_SLOW_MULTIPLIER = 0.25;
    private static final int LOCK_ATTRIBUTION_WINDOW_MS = 5_000;
    private static final double INDIRECT_ATTRIBUTION_RANGE_SQUARED = 9.0;

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CtfMatchOrchestrator gameManager;
    private final FlagManager flagManager;
    private final KitManager kitManager;
    private final NamespacedKey spearKey;
    private final NamespacedKey shooterKey;
    private final NamespacedKey returnShooterKey;

    private final Map<UUID, SpearShooterMetaData> shooterStateById = new HashMap<>();
    private final Map<UUID, SpearLockMetaData> lastSpearLockByVictim = new HashMap<>();
    private final Set<UUID> pendingDirectHits = new HashSet<>();
    private final Set<UUID> pendingHomingLaunchers = new HashSet<>();

    public HomingSpearAbility(JavaPlugin plugin, TeamManager teamManager, CtfMatchOrchestrator gameManager, FlagManager flagManager, KitManager kitManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.flagManager = flagManager;
        this.kitManager = kitManager;
        this.spearKey = new NamespacedKey(plugin, "ctf_homing_spear");
        this.shooterKey = new NamespacedKey(plugin, "ctf_homing_spear_shooter");
        this.returnShooterKey = new NamespacedKey(plugin, "ctf_return_spear_shooter");
    }

    /**
     * F-key throw trigger.
     */
    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player shooter = event.getPlayer();
        if (!gameManager.isPlayerInGame(shooter)) {
            return;
        }

        boolean holdingSpear = isHoldingSpear(shooter);
        if (!holdingSpear) {
            return;
        }

        event.setCancelled(true);

        if (!isAbilityState(gameManager.getGameState())) {
            shooter.sendActionBar(Component.text("Spear is disabled in lobby.", NamedTextColor.RED));
            Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " spear blocked (state=" + gameManager.getGameState().name() + ")");
            return;
        }

        if (flagManager.isFlagCarrier(shooter.getUniqueId())) {
            shooter.sendActionBar(msg("actionbar.spear.blocked"));
            Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " spear blocked (carrying_flag)");
            return;
        }

        UUID shooterId = shooter.getUniqueId();
        SpearShooterMetaData shooterState = state(shooterId);
        long now = System.currentTimeMillis();
        long cooldownUntil = shooterState.getCooldownUntilMs();
        if (cooldownUntil > now) {
            shooter.sendActionBar(msg("actionbar.spear.cooldown", Map.of(
                "seconds", formatTenths(cooldownUntil - now)
            )));
            startActionBarTask(shooterId);
            Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " spear blocked (cooldown)");
            return;
        }

        cleanupPlayer(shooterId);
        throwSpear(shooter);
        shooterState.setCooldownUntilMs(now + COOLDOWN_MILLIS);
        startActionBarTask(shooterId);
        Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " F-threw Homing Spear");
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !isTrackedSpear(trident)) {
            return;
        }

        if (event.getHitEntity() instanceof Player) {
            pendingDirectHits.add(trident.getUniqueId());
        }
    }

    @EventHandler
    public void onSpearDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Trident trident) || !isTrackedSpear(trident)) {
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID shooterId = getShooterId(trident);
        if (shooterId == null) {
            return;
        }

        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) {
            return;
        }

        String shooterTeam = teamManager.getTeamKey(shooter);
        String victimTeam = teamManager.getTeamKey(victim);
        if (shooterTeam == null || victimTeam == null || shooterTeam.equals(victimTeam)) {
            event.setCancelled(true);
            return;
        }

        if (!pendingDirectHits.contains(trident.getUniqueId())) {
            // Keep vanilla hit behavior ignored unless this was our tracked impact.
            event.setCancelled(true);
            return;
        }

        event.setDamage(HIT_DAMAGE);
        pendingDirectHits.remove(trident.getUniqueId());
        long hitUntil = System.currentTimeMillis() + 3300L;
        SpearShooterMetaData shooterState = state(shooterId);
        shooterState.setHitUntilMs(hitUntil);
        shooterState.setHitTargetName(victim.getName());
        startActionBarTask(shooterId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupPlayer(shooterId), DAMAGE_RETURN_DELAY_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID shooterId = event.getPlayer().getUniqueId();
        cleanupPlayer(shooterId);
        clearActionBarState(shooterId);
    }

    @EventHandler
    public void onSpearLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }

        if (!(trident.getShooter() instanceof Player player)) {
            return;
        }

        if (pendingHomingLaunchers.contains(player.getUniqueId())) {
            return;
        }

        if (!gameManager.isPlayerInGame(player)) {
            return;
        }

        if (isTrackedSpear(trident)) {
            return;
        }

        ItemStack thrownItem = trident.getItemStack();
        if (!kitManager.isHomingSpear(thrownItem)) {
            return;
        }

        trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        startReturnBehavior(player, trident);
    }

    /**
     * Called by death handling to resolve spear attribution.
     */
    public SpearDeathAttributionMetaData resolveSpearAttribution(Player victim, EntityDamageEvent.DamageCause cause, Entity directDamager) {
        if (victim == null) {
            return null;
        }

        if (directDamager instanceof Trident trident && isTrackedSpear(trident)) {
            UUID shooterId = getShooterId(trident);
            Player shooter = shooterId == null ? null : Bukkit.getPlayer(shooterId);
            if (shooter != null) {
                Component message = Component.text(victim.getName(), NamedTextColor.RED)
                    .append(Component.text(" was impaled by ", NamedTextColor.GRAY))
                    .append(Component.text(shooter.getName(), NamedTextColor.AQUA))
                    .append(Component.text("'s Homing Spear.", NamedTextColor.GRAY));
                return new SpearDeathAttributionMetaData(message, shooter.getUniqueId());
            }
        }

        SpearLockMetaData lockInfo = lastSpearLockByVictim.get(victim.getUniqueId());
        if (lockInfo == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        if (now - lockInfo.lockedAtMs() > LOCK_ATTRIBUTION_WINDOW_MS) {
            return null;
        }

        if (cause != EntityDamageEvent.DamageCause.FALL
            && cause != EntityDamageEvent.DamageCause.VOID
            && cause != EntityDamageEvent.DamageCause.LAVA
            && cause != EntityDamageEvent.DamageCause.DROWNING) {
            return null;
        }

        UUID spearEntityId = lockInfo.spearEntityId();
        if (spearEntityId == null) {
            return null;
        }

        Entity spearEntity = Bukkit.getEntity(spearEntityId);
        if (!(spearEntity instanceof Trident trackedSpear) || trackedSpear.isDead()) {
            return null;
        }

        if (!sameWorld(victim.getLocation(), trackedSpear.getLocation())) {
            return null;
        }

        if (victim.getLocation().distanceSquared(trackedSpear.getLocation()) > INDIRECT_ATTRIBUTION_RANGE_SQUARED) {
            return null;
        }

        Player shooter = Bukkit.getPlayer(lockInfo.shooterId());
        if (shooter == null) {
            return null;
        }

        Bukkit.getLogger().info(LOG_PREFIX + victim.getName() + " indirect death attributed to "
            + shooter.getName() + " (cause=" + cause.name() + ")");

        Component message = Component.text(victim.getName(), NamedTextColor.RED)
            .append(Component.text(" tried escaping ", NamedTextColor.GRAY))
            .append(Component.text(shooter.getName(), NamedTextColor.AQUA))
            .append(Component.text("'s Homing Spear and died anyway.", NamedTextColor.GRAY));
        return new SpearDeathAttributionMetaData(message, shooter.getUniqueId());
    }

    /**
     * Clears one player's active spear state.
     */
    public void cleanupPlayer(UUID shooterId) {
        if (shooterId == null) {
            return;
        }

        cancelReturnTimers(shooterId, true);

        SpearShooterMetaData shooterState = shooterStateById.get(shooterId);
        if (shooterState != null) {
            Integer taskId = shooterState.getSpearTimerTaskId();
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
                shooterState.setSpearTimerTaskId(null);
            }

            UUID spearEntityId = shooterState.getActiveSpearEntityId();
            if (spearEntityId != null) {
                Entity spearEntity = Bukkit.getEntity(spearEntityId);
                if (spearEntity != null) {
                    spearEntity.remove();
                }
                pendingDirectHits.remove(spearEntityId);
                shooterState.setActiveSpearEntityId(null);
            }

            shooterState.setLockedTargetId(null);
            shooterState.setActiveUntilMs(0L);
            shooterState.setSpearTick(0);
        }

        restoreSpearItem(shooterId);
    }

    /**
     * Clears all spear state for a player leaving the arena.
     */
    public void handlePlayerLeave(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        cleanupPlayer(shooterId);
        clearActionBarState(shooterId);
    }

    /**
     * Clears all active spear/cooldown state.
     */
    public void clearAll() {
        for (UUID shooterId : new HashSet<>(shooterStateById.keySet())) {
            cleanupPlayer(shooterId);
        }
        for (UUID shooterId : new HashSet<>(shooterStateById.keySet())) {
            clearActionBarState(shooterId);
        }
        shooterStateById.clear();
        lastSpearLockByVictim.clear();
        pendingDirectHits.clear();
    }

    private void throwSpear(Player shooter) {
        pendingHomingLaunchers.add(shooter.getUniqueId());
        Trident trident;
        try {
            trident = shooter.launchProjectile(Trident.class);
        } finally {
            pendingHomingLaunchers.remove(shooter.getUniqueId());
        }
        trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        trident.setGravity(false);
        trident.setVelocity(shooter.getLocation().getDirection().normalize().multiply(CRUISE_SPEED));
        trident.getPersistentDataContainer().set(spearKey, PersistentDataType.BYTE, (byte) 1);
        trident.getPersistentDataContainer().set(shooterKey, PersistentDataType.STRING, shooter.getUniqueId().toString());

        UUID shooterId = shooter.getUniqueId();
        SpearShooterMetaData shooterState = state(shooterId);
        shooterState.setActiveSpearEntityId(trident.getUniqueId());
        shooterState.setSpearTick(0);
        shooterState.setLockedTargetId(null);
        shooterState.setActiveUntilMs(System.currentTimeMillis() + (TOTAL_LIFETIME_TICKS * 50L));
        setSpearPlaceholder(shooter);
        startActionBarTask(shooterId);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickSpear(shooterId), UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
        shooterState.setSpearTimerTaskId(task.getTaskId());
    }

    private void startReturnBehavior(Player shooter, Trident trident) {
        UUID shooterId = shooter.getUniqueId();
        cancelReturnTimers(shooterId, true);
        trident.getPersistentDataContainer().set(returnShooterKey, PersistentDataType.STRING, shooterId.toString());

        SpearShooterMetaData shooterState = state(shooterId);
        shooterState.setReturnUntilMs(System.currentTimeMillis() + 5000L);
        setReturnPlaceholder(shooter);
        startActionBarTask(shooterId);
        Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " threw spear, returning in 5s");

        BukkitTask returnTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!trident.isDead()) {
                trident.remove();
            }
            restoreSpearItem(shooterId);
            SpearShooterMetaData refreshedState = shooterStateById.get(shooterId);
            if (refreshedState != null) {
                refreshedState.setReturnUntilMs(0L);
            }
            cancelReturnTimers(shooterId, false);
            refreshActionBar(shooterId);
            Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " spear returned");
        }, 100L);
        shooterState.setSpearReturnTimerTaskId(returnTask.getTaskId());
    }

    private void cancelReturnTimers(UUID shooterId, boolean removeProjectile) {
        SpearShooterMetaData shooterState = shooterStateById.get(shooterId);
        if (shooterState != null) {
            Integer returnTaskId = shooterState.getSpearReturnTimerTaskId();
            if (returnTaskId != null) {
                Bukkit.getScheduler().cancelTask(returnTaskId);
                shooterState.setSpearReturnTimerTaskId(null);
            }
            shooterState.setReturnUntilMs(0L);
        }

        if (removeProjectile) {
            removeReturnTridents(shooterId);
        }
    }

    private void setReturnPlaceholder(Player shooter) {
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(SPEAR_RETURNING_NAME, NamedTextColor.RED));
            placeholder.setItemMeta(meta);
        }
        shooter.getInventory().setItem(KitManager.SPEAR_SLOT, placeholder);
    }

    private void tickSpear(UUID shooterId) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null || !shooter.isOnline()) {
            cleanupPlayer(shooterId);
            return;
        }

        SpearShooterMetaData shooterState = shooterStateById.get(shooterId);
        if (shooterState == null) {
            cleanupPlayer(shooterId);
            return;
        }

        UUID spearEntityId = shooterState.getActiveSpearEntityId();
        if (spearEntityId == null) {
            cleanupPlayer(shooterId);
            return;
        }

        Entity entity = Bukkit.getEntity(spearEntityId);
        if (!(entity instanceof Trident trident) || trident.isDead()) {
            cleanupPlayer(shooterId);
            return;
        }

        int tick = shooterState.getSpearTick() + 1;
        shooterState.setSpearTick(tick);

        UUID lockedTargetId = shooterState.getLockedTargetId();
        Player lockedTarget = lockedTargetId == null ? null : Bukkit.getPlayer(lockedTargetId);

        if (lockedTarget != null && lockedTarget.isOnline() && sameWorld(trident.getLocation(), lockedTarget.getLocation())) {
            Vector toTarget = lockedTarget.getLocation().add(0.0, 1.1, 0.0).toVector().subtract(trident.getLocation().toVector());
            if (toTarget.lengthSquared() > 0.0001) {
                trident.setVelocity(toTarget.normalize().multiply(LOCK_STEER_STRENGTH).add(trident.getVelocity().multiply(0.62)));
            }
        } else if (lockedTargetId != null) {
            shooterState.setLockedTargetId(null);
            trident.setGravity(true);
            trident.setVelocity(trident.getVelocity().multiply(DROP_SLOW_MULTIPLIER));
        }

        if (shooterState.getLockedTargetId() == null && tick % SCAN_INTERVAL_TICKS == 0) {
            Player target = findFirstEnemyInRange(shooter, trident.getLocation());
            if (target != null) {
                shooterState.setLockedTargetId(target.getUniqueId());
                lastSpearLockByVictim.put(target.getUniqueId(),
                    new SpearLockMetaData(shooterId, System.currentTimeMillis(), trident.getUniqueId()));
                Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " locked onto " + target.getName());
            }
        }

        if (tick >= TOTAL_LIFETIME_TICKS) {
            trident.setGravity(true);
            trident.setVelocity(trident.getVelocity().multiply(DROP_SLOW_MULTIPLIER));
            Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupPlayer(shooterId), DAMAGE_RETURN_DELAY_TICKS);
            Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " spear returned");
        }
    }

    private void setSpearPlaceholder(Player shooter) {
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(SPEAR_LOCKED_NAME, NamedTextColor.RED));
            placeholder.setItemMeta(meta);
        }
        shooter.getInventory().setItem(KitManager.SPEAR_SLOT, placeholder);
    }

    private void restoreSpearItem(UUID shooterId) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) {
            return;
        }

        ItemStack slotItem = shooter.getInventory().getItem(KitManager.SPEAR_SLOT);
        if (slotItem == null || slotItem.getType() == Material.BARRIER) {
            shooter.getInventory().setItem(KitManager.SPEAR_SLOT, kitManager.createHomingSpearItem());
            return;
        }

        if (kitManager.isHomingSpear(slotItem)) {
            setItemDisplayName(slotItem, Component.text(SPEAR_NAME, NamedTextColor.AQUA));
            shooter.getInventory().setItem(KitManager.SPEAR_SLOT, slotItem);
        }
    }

    private boolean isHoldingSpear(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (kitManager.isHomingSpear(mainHand)) {
            return true;
        }

        ItemStack slotItem = player.getInventory().getItem(KitManager.SPEAR_SLOT);
        return kitManager.isHomingSpear(slotItem);
    }

    private Player findFirstEnemyInRange(Player shooter, Location spearLocation) {
        String shooterTeam = teamManager.getTeamKey(shooter);
        if (shooterTeam == null) {
            return null;
        }

        String enemyTeam = TeamManager.RED.equals(shooterTeam) ? TeamManager.BLUE : TeamManager.RED;
        for (Player enemy : teamManager.getTeamPlayers(enemyTeam)) {
            if (!enemy.isOnline()) {
                continue;
            }
            if (!sameWorld(spearLocation, enemy.getLocation())) {
                continue;
            }
            if (enemy.getLocation().distanceSquared(spearLocation) <= SCAN_RANGE_BLOCKS * SCAN_RANGE_BLOCKS) {
                return enemy;
            }
        }
        return null;
    }

    private boolean sameWorld(Location first, Location second) {
        return first != null && second != null && first.getWorld() != null && first.getWorld().equals(second.getWorld());
    }

    private boolean isTrackedSpear(Trident trident) {
        return trident.getPersistentDataContainer().has(spearKey, PersistentDataType.BYTE);
    }

    private void removeReturnTridents(UUID shooterId) {
        String shooterRaw = shooterId == null ? null : shooterId.toString();
        if (shooterRaw == null) {
            return;
        }

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Trident trident : world.getEntitiesByClass(Trident.class)) {
                String owner = trident.getPersistentDataContainer().get(returnShooterKey, PersistentDataType.STRING);
                if (shooterRaw.equals(owner)) {
                    trident.remove();
                }
            }
        }
    }

    private void startActionBarTask(UUID shooterId) {
        if (shooterId == null) {
            return;
        }
        SpearShooterMetaData shooterState = state(shooterId);
        if (shooterState.getActionBarTaskId() != null) {
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> refreshActionBar(shooterId), 0L, ACTION_BAR_REFRESH_TICKS);
        shooterState.setActionBarTaskId(task.getTaskId());
    }

    private void refreshActionBar(UUID shooterId) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null || !shooter.isOnline()) {
            stopActionBarTask(shooterId);
            return;
        }

        SpearShooterMetaData shooterState = shooterStateById.get(shooterId);
        if (shooterState == null) {
            stopActionBarTask(shooterId);
            return;
        }

        long now = System.currentTimeMillis();
        long hitUntil = shooterState.getHitUntilMs();
        if (hitUntil > 0L && hitUntil <= now) {
            shooterState.setHitUntilMs(0L);
            shooterState.setHitTargetName(null);
            hitUntil = 0L;
        }

        long returnUntil = Math.max(
            shooterState.getActiveUntilMs(),
            shooterState.getReturnUntilMs()
        );
        long returnRemainingMs = returnUntil - now;
        long cooldownUntil = shooterState.getCooldownUntilMs();
        long cooldownRemainingMs = cooldownUntil - now;
        refreshSpearItemVisual(shooter, cooldownRemainingMs, returnRemainingMs);

        if (hitUntil > 0L) {
            String target = shooterState.getHitTargetName() == null ? "Target" : shooterState.getHitTargetName();
            shooter.sendActionBar(msg("actionbar.spear.hit", Map.of(
                "target", target
            )));
            return;
        }

        Component baseMessage = null;
        UUID lockedTargetId = shooterState.getLockedTargetId();
        if (lockedTargetId != null) {
            Player lockedTarget = Bukkit.getPlayer(lockedTargetId);
            if (lockedTarget != null && lockedTarget.isOnline()) {
                baseMessage = msg("actionbar.spear.locked", Map.of(
                    "target", lockedTarget.getName()
                ));
            } else {
                shooterState.setLockedTargetId(null);
            }
        }

        if (baseMessage == null) {
            if (returnRemainingMs > 0) {
                baseMessage = msg("actionbar.spear.returning", Map.of(
                    "seconds", formatTenths(returnRemainingMs)
                ));
            }
        }

        if (cooldownRemainingMs > 0) {
            Component cooldownMessage = msg(baseMessage == null ? "actionbar.spear.cooldown" : "actionbar.spear.cooldown_short", Map.of(
                "seconds", formatTenths(cooldownRemainingMs)
            ));
            if (baseMessage == null) {
                shooter.sendActionBar(cooldownMessage);
                return;
            }
            shooter.sendActionBar(baseMessage.append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(cooldownMessage));
            return;
        }

        if (baseMessage != null) {
            shooter.sendActionBar(baseMessage);
            return;
        }

        stopActionBarTask(shooterId);
    }

    private void stopActionBarTask(UUID shooterId) {
        SpearShooterMetaData shooterState = shooterStateById.get(shooterId);
        if (shooterState == null) {
            return;
        }
        Integer taskId = shooterState.getActionBarTaskId();
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            shooterState.setActionBarTaskId(null);
        }
    }

    private void clearActionBarState(UUID shooterId) {
        stopActionBarTask(shooterId);
        restoreSpearDisplayName(shooterId);
        SpearShooterMetaData shooterState = shooterStateById.get(shooterId);
        if (shooterState == null) {
            return;
        }
        shooterState.setHitUntilMs(0L);
        shooterState.setHitTargetName(null);
        shooterState.setActiveUntilMs(0L);
        shooterState.setReturnUntilMs(0L);
        shooterState.setCooldownUntilMs(0L);
    }

    private void refreshSpearItemVisual(Player shooter, long cooldownRemainingMs, long returnRemainingMs) {
        if (shooter == null) {
            return;
        }

        ItemStack slotItem = shooter.getInventory().getItem(KitManager.SPEAR_SLOT);
        if (slotItem == null) {
            return;
        }

        if (slotItem.getType() == Material.BARRIER) {
            long visualRemainingMs = Math.max(cooldownRemainingMs, returnRemainingMs);
            if (visualRemainingMs <= 0) {
                return;
            }
            String timeText = formatTenths(visualRemainingMs);
            String label = returnRemainingMs > 0 ? SPEAR_RETURNING_NAME : SPEAR_LOCKED_NAME;
            setItemDisplayName(slotItem, Component.text(label + " " + timeText + "s", NamedTextColor.RED));
            shooter.getInventory().setItem(KitManager.SPEAR_SLOT, slotItem);
            return;
        }

        if (!kitManager.isHomingSpear(slotItem)) {
            return;
        }

        if (cooldownRemainingMs > 0) {
            setItemDisplayName(slotItem, Component.text(
                SPEAR_NAME + " (" + formatTenths(cooldownRemainingMs) + "s)",
                NamedTextColor.AQUA
            ));
        } else {
            setItemDisplayName(slotItem, Component.text(SPEAR_NAME, NamedTextColor.AQUA));
        }
        shooter.getInventory().setItem(KitManager.SPEAR_SLOT, slotItem);
    }

    private void restoreSpearDisplayName(UUID shooterId) {
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null || !shooter.isOnline()) {
            return;
        }

        ItemStack slotItem = shooter.getInventory().getItem(KitManager.SPEAR_SLOT);
        if (slotItem == null || slotItem.getType() == Material.BARRIER) {
            return;
        }
        if (!kitManager.isHomingSpear(slotItem)) {
            return;
        }

        setItemDisplayName(slotItem, Component.text(SPEAR_NAME, NamedTextColor.AQUA));
        shooter.getInventory().setItem(KitManager.SPEAR_SLOT, slotItem);
    }

    private void setItemDisplayName(ItemStack item, Component displayName) {
        if (item == null || displayName == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (displayName.equals(meta.displayName())) {
            return;
        }
        meta.displayName(displayName);
        item.setItemMeta(meta);
    }

    private String formatTenths(long remainingMs) {
        double seconds = Math.max(0.0d, remainingMs / 1000.0d);
        double floored = Math.floor(seconds * 10.0d) / 10.0d;
        return String.format(Locale.US, "%.1f", floored);
    }

    private SpearShooterMetaData state(UUID shooterId) {
        return shooterStateById.computeIfAbsent(shooterId, ignored -> new SpearShooterMetaData());
    }

    private UUID getShooterId(Trident trident) {
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

    private boolean isAbilityState(GameState gameState) {
        return gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME;
    }
}




