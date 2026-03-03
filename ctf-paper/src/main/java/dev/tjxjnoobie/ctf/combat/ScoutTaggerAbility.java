package dev.tjxjnoobie.ctf.combat;

import dev.tjxjnoobie.ctf.combat.metadata.ScoutTaggerPlayerMetaData;
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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Provides Scout snowball tagging ability with ammo + cooldown.
 */
public final class ScoutTaggerAbility implements Listener, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [ScoutTaggerAbility] ";
    private static final String SCOUT_TAGGER_NAME = "Scout Tagger";
    private static final int MAX_AMMO = 6;
    private static final long COOLDOWN_MS = 2_000L;
    private static final int COOLDOWN_VISUAL_REFRESH_TICKS = 1;
    private static final int TAG_DURATION_TICKS = 80;
    private static final double FORWARD_BOOST = 0.22;
    private static final int REGEN_PERIOD_TICKS = 20;

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CtfMatchOrchestrator gameManager;
    private final FlagManager flagManager;
    private final KitManager kitManager;
    private final NamespacedKey snowballKey;
    private final NamespacedKey shooterKey;

    private final Map<UUID, ScoutTaggerPlayerMetaData> playerStateById = new HashMap<>();
    private final Map<UUID, Integer> glowRemovalTaskByTarget = new HashMap<>();
    private BukkitTask regenTask;

    public ScoutTaggerAbility(JavaPlugin plugin, TeamManager teamManager, CtfMatchOrchestrator gameManager,
                              FlagManager flagManager, KitManager kitManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.flagManager = flagManager;
        this.kitManager = kitManager;
        this.snowballKey = new NamespacedKey(plugin, "ctf_scout_snowball");
        this.shooterKey = new NamespacedKey(plugin, "ctf_scout_snowball_shooter");
    }

    /**
     * Starts ammo regeneration for the active match.
     */
    public void startForMatch() {
        if (regenTask != null) {
            return;
        }

        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::regenAmmo, REGEN_PERIOD_TICKS, REGEN_PERIOD_TICKS);
    }

    /**
     * Clears all state and stops regen.
     */
    public void stopAll() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
        for (Integer taskId : new HashSet<>(glowRemovalTaskByTarget.values())) {
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
        glowRemovalTaskByTarget.clear();
        for (UUID playerId : new HashSet<>(playerStateById.keySet())) {
            stopCooldownVisualTask(playerId, true);
        }
        playerStateById.clear();
    }

    public void handlePlayerLeave(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        stopCooldownVisualTask(playerId, true);
        playerStateById.remove(playerId);
        clearGlow(playerId);
    }

    public void handleRespawn(Player player) {
        if (player == null) {
            return;
        }
        if (!isAbilityState() || !gameManager.isPlayerInGame(player)) {
            return;
        }
        if (kitManager.isScout(player)) {
            UUID playerId = player.getUniqueId();
            ensureAmmoEntry(playerId);
            if (getCooldownRemainingMs(playerId, System.currentTimeMillis()) > 0) {
                startCooldownVisualTask(playerId);
            } else {
                restoreScoutSwordName(playerId);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getAction() == null) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!kitManager.isScoutTaggerSword(item)) {
            return;
        }

        if (!isAbilityState()) {
            return;
        }

        if (flagManager.isFlagCarrier(player.getUniqueId())) {
            denyThrow(player, "flag", msg("actionbar.scout.flag"));
            return;
        }

        if (!kitManager.isScout(player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        ScoutTaggerPlayerMetaData playerState = ensureAmmoEntry(playerId);
        int ammo = playerState.getAmmo();
        if (ammo <= 0) {
            denyThrow(player, "no_ammo", msg("actionbar.scout.empty"));
            return;
        }

        long now = System.currentTimeMillis();
        long lastThrow = playerState.getLastThrowAtMs();
        long elapsed = now - lastThrow;
        if (elapsed < COOLDOWN_MS) {
            long remainingMs = COOLDOWN_MS - elapsed;
            denyThrow(player, "cooldown", msg("actionbar.scout.cooldown", Map.of(
                "seconds", formatTenths(remainingMs)
            )));
            startCooldownVisualTask(playerId);
            return;
        }

        playerState.setAmmo(ammo - 1);
        playerState.setLastThrowAtMs(now);

        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        snowball.getPersistentDataContainer().set(shooterKey, PersistentDataType.STRING, player.getUniqueId().toString());

        Vector boost = player.getLocation().getDirection().normalize().multiply(FORWARD_BOOST);
        player.setVelocity(player.getVelocity().add(boost));

        player.sendActionBar(msg("actionbar.scout.ammo", Map.of(
            "ammo", Integer.toString(ammo - 1)
        )));
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7f, 1.1f);
        player.spawnParticle(Particle.SNOWFLAKE, player.getEyeLocation(), 6, 0.12, 0.12, 0.12, 0.01);
        startCooldownVisualTask(playerId);

        Bukkit.getLogger().info(LOG_PREFIX + player.getName() + " threw snowball (ammo " + (ammo - 1) + "/" + MAX_AMMO + ")");
    }

    @EventHandler
    public void onSnowballDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball snowball)) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!isTaggedSnowball(snowball)) {
            return;
        }

        Player shooter = resolveShooter(snowball);
        if (shooter == null) {
            return;
        }

        String shooterTeam = teamManager.getTeamKey(shooter);
        String targetTeam = teamManager.getTeamKey(target);
        if (shooterTeam == null || targetTeam == null || shooterTeam.equals(targetTeam)) {
            event.setCancelled(true);
            return;
        }

        applyGlow(target);
        Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " tagged " + target.getName() + " (glow reset)");
    }

    private void denyThrow(Player player, String reason, Component message) {
        player.sendActionBar(message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 0.9f);
        Bukkit.getLogger().info(LOG_PREFIX + player.getName() + " throw blocked (" + reason + ")");
    }

    private void applyGlow(Player target) {
        if (target == null) {
            return;
        }

        cancelGlowRemoval(target.getUniqueId());
        target.setGlowing(true);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player refreshed = Bukkit.getPlayer(target.getUniqueId());
            if (refreshed != null) {
                refreshed.setGlowing(false);
            }
            glowRemovalTaskByTarget.remove(target.getUniqueId());
        }, TAG_DURATION_TICKS);
        glowRemovalTaskByTarget.put(target.getUniqueId(), task.getTaskId());
    }

    private void clearGlow(UUID targetId) {
        if (targetId == null) {
            return;
        }
        cancelGlowRemoval(targetId);
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            target.setGlowing(false);
        }
    }

    private void cancelGlowRemoval(UUID targetId) {
        Integer taskId = glowRemovalTaskByTarget.remove(targetId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private Player resolveShooter(Snowball snowball) {
        if (snowball.getShooter() instanceof Player player) {
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

    private boolean isTaggedSnowball(Snowball snowball) {
        return snowball.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE);
    }

    private ScoutTaggerPlayerMetaData ensureAmmoEntry(UUID playerId) {
        return playerStateById.computeIfAbsent(playerId, ignored -> new ScoutTaggerPlayerMetaData(MAX_AMMO, 0L));
    }

    private void regenAmmo() {
        if (!isAbilityState()) {
            return;
        }

        Set<UUID> activeScouts = new HashSet<>();
        for (Player player : teamManager.getJoinedPlayers()) {
            if (!kitManager.isScout(player)) {
                continue;
            }
            UUID playerId = player.getUniqueId();
            activeScouts.add(playerId);
            ScoutTaggerPlayerMetaData playerState = ensureAmmoEntry(playerId);
            int ammo = playerState.getAmmo();
            if (ammo < MAX_AMMO) {
                playerState.setAmmo(ammo + 1);
            }
        }

        for (UUID playerId : new HashSet<>(playerStateById.keySet())) {
            if (!activeScouts.contains(playerId)) {
                stopCooldownVisualTask(playerId, true);
                playerStateById.remove(playerId);
            }
        }
    }

    private void startCooldownVisualTask(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ScoutTaggerPlayerMetaData playerState = playerStateById.get(playerId);
        if (playerState == null || playerState.getCooldownVisualTaskId() != null) {
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
            plugin,
            () -> refreshCooldownVisual(playerId),
            0L,
            COOLDOWN_VISUAL_REFRESH_TICKS
        );
        playerState.setCooldownVisualTaskId(task.getTaskId());
    }

    private void refreshCooldownVisual(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            stopCooldownVisualTask(playerId, false);
            return;
        }
        if (!gameManager.isPlayerInGame(player) || !kitManager.isScout(player)) {
            stopCooldownVisualTask(playerId, true);
            return;
        }

        long remainingMs = getCooldownRemainingMs(playerId, System.currentTimeMillis());
        refreshScoutSwordName(player, remainingMs);

        if (remainingMs > 0) {
            player.sendActionBar(msg("actionbar.scout.cooldown", Map.of(
                "seconds", formatTenths(remainingMs)
            )));
            return;
        }

        stopCooldownVisualTask(playerId, false);
    }

    private long getCooldownRemainingMs(UUID playerId, long nowMs) {
        ScoutTaggerPlayerMetaData playerState = playerStateById.get(playerId);
        if (playerState == null) {
            return 0L;
        }
        long lastThrow = playerState.getLastThrowAtMs();
        if (lastThrow <= 0L) {
            return 0L;
        }
        return Math.max(0L, COOLDOWN_MS - (nowMs - lastThrow));
    }

    private void stopCooldownVisualTask(UUID playerId, boolean restoreName) {
        ScoutTaggerPlayerMetaData playerState = playerStateById.get(playerId);
        if (playerState == null) {
            return;
        }
        Integer taskId = playerState.getCooldownVisualTaskId();
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            playerState.setCooldownVisualTaskId(null);
        }
        if (restoreName) {
            restoreScoutSwordName(playerId);
        }
    }

    private void restoreScoutSwordName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        refreshScoutSwordName(player, 0L);
    }

    private void refreshScoutSwordName(Player player, long cooldownRemainingMs) {
        if (player == null) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (kitManager.isScoutTaggerSword(mainHand)) {
            updateScoutSwordName(mainHand, cooldownRemainingMs);
            player.getInventory().setItemInMainHand(mainHand);
            return;
        }

        ItemStack slotZero = player.getInventory().getItem(0);
        if (!kitManager.isScoutTaggerSword(slotZero)) {
            return;
        }

        updateScoutSwordName(slotZero, cooldownRemainingMs);
        player.getInventory().setItem(0, slotZero);
    }

    private void updateScoutSwordName(ItemStack sword, long cooldownRemainingMs) {
        if (sword == null || sword.getType() != Material.WOODEN_SWORD) {
            return;
        }

        if (cooldownRemainingMs > 0) {
            setItemDisplayName(sword, Component.text(
                SCOUT_TAGGER_NAME + " (" + formatTenths(cooldownRemainingMs) + "s)",
                NamedTextColor.GREEN
            ));
            return;
        }
        setItemDisplayName(sword, Component.text(SCOUT_TAGGER_NAME, NamedTextColor.GREEN));
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

    private boolean isAbilityState() {
        GameState state = gameManager.getGameState();
        return state == GameState.IN_PROGRESS || state == GameState.OVERTIME;
    }
}

