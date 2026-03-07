package dev.tjxjnoobie.ctf.game.combat.scout.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PluginConfigDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.UiDependencyAccess;
import dev.tjxjnoobie.ctf.game.combat.scout.metadata.ScoutTaggerPlayerMetaData;
import dev.tjxjnoobie.ctf.game.combat.scout.util.ScoutTaggerProjectileUtil;
import dev.tjxjnoobie.ctf.game.combat.scout.util.ScoutTaggerStateUtil;
import dev.tjxjnoobie.ctf.game.combat.util.CombatNamespacedKeyFactory;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import dev.tjxjnoobie.ctf.util.tasks.AbilityTaskOrchestrator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;

/**
 * Owns Scout Tagger throw orchestration, ammo use, and enemy tag application.
 */
public final class ScoutTaggerAbility implements MessageAccess, BukkitMessageSender, BukkitEffectsUtil,
        FlagDependencyAccess, LifecycleDependencyAccess, PlayerDependencyAccess, PluginConfigDependencyAccess, UiDependencyAccess {

    // == Constants ==
    private static final String LOG_PREFIX = "[ScoutTaggerAbility] ";
    private static final String FALLBACK_NAMESPACE = CTFKeys.coreFallbackNamespace();
    private static final int MAX_AMMO = 6;
    private static final double FORWARD_BOOST = 0.22;
    private static final int REGEN_PERIOD_TICKS = 20;
    private static final long AMMO_REGEN_PERIOD_MS = 1_000L;
    private static final String DENIED_SOUND_KEY = "entity.villager.no";
    private static final String THROW_SOUND_KEY = "entity.snowball.throw";

    // == Dependencies ==
    private final ScoutTaggerRuntimeHandler runtimeHandler;
    private final ScoutTaggerCooldownHandler cooldownHandler;
    private final ScoutTaggerGlowHandler glowHandler;

    // == Runtime state ==
    private BukkitTask scoutAmmoRegenTimerTask;
    private BukkitTask scoutHeldVisualTimerTask;
    private final NamespacedKey snowballKey;
    private final NamespacedKey shooterKey;
    private final Set<UUID> scoutRaisedUseLatch = new HashSet<>();

    // == Lifecycle ==
    /**
     * Constructs a ScoutTaggerAbility instance.
     *
     * @param runtimeHandler Runtime owner for ammo and cooldown timestamps.
     * @param cooldownHandler Cooldown visual owner for action-bar and sword updates.
     * @param glowHandler Glow owner for tagged enemy players.
     */
    public ScoutTaggerAbility(ScoutTaggerRuntimeHandler runtimeHandler,
                              ScoutTaggerCooldownHandler cooldownHandler,
                              ScoutTaggerGlowHandler glowHandler) {
        this.runtimeHandler = runtimeHandler;
        this.cooldownHandler = cooldownHandler;
        this.glowHandler = glowHandler;
        this.snowballKey = CombatNamespacedKeyFactory.create(
                CTFKeys.combatScoutSnowballTag(),
                FALLBACK_NAMESPACE,
                this::getMainPlugin);
        this.shooterKey = CombatNamespacedKeyFactory.create(
                CTFKeys.combatScoutSnowballShooterTag(),
                FALLBACK_NAMESPACE,
                this::getMainPlugin);
    }

    /**
     * Starts the match-scoped scout ammo and cooldown UI timers.
     */
    public void startForMatch() {
        if (scoutAmmoRegenTimerTask != null) {
            Bukkit.getLogger().info(LOG_PREFIX + "startForMatch ignored because timers already exist");
            return;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "starting scout match timers regenPeriodTicks=" + REGEN_PERIOD_TICKS);
        cooldownHandler.setNextAmmoRegenAtMs(System.currentTimeMillis() + AMMO_REGEN_PERIOD_MS);
        scoutAmmoRegenTimerTask = AbilityTaskOrchestrator.startTimer(
                scoutAmmoRegenTimerTask,
                this::regenAmmo,
                REGEN_PERIOD_TICKS,
                REGEN_PERIOD_TICKS);
        startScoutHeldVisualTimer();
    }

    /**
     * Clears all scout runtime state and stops active timers.
     */
    public void stopAll() {
        Bukkit.getLogger().info(LOG_PREFIX + "stopping all scout state and timers");
        scoutAmmoRegenTimerTask = AbilityTaskOrchestrator.cancel(scoutAmmoRegenTimerTask);
        scoutHeldVisualTimerTask = AbilityTaskOrchestrator.cancel(scoutHeldVisualTimerTask);

        glowHandler.stopAll();
        cooldownHandler.clearAllCooldowns();
        runtimeHandler.clearAll();
        scoutRaisedUseLatch.clear();
    }

    // == Getters ==
    /**
     * Returns the remaining scout throw cooldown for the player.
     *
     * @param playerUUID Unique id of the player.
     * @return Remaining cooldown in milliseconds.
     */
    public long getCooldownRemainingMs(UUID playerUUID) {
        if (playerUUID == null) {
            return 0L;
        }
        return cooldownHandler.getCooldownRemainingMs(playerUUID, System.currentTimeMillis());
    }

    /**
     * Formats a cooldown value into tenths of a second.
     *
     * @param remainingMs Remaining cooldown in milliseconds.
     * @return Formatted seconds text.
     */
    public String formatCooldownTenths(long remainingMs) {
        return cooldownHandler.formatTenths(remainingMs);
    }

    // == Utilities ==
    /**
     * Clears Scout Tagger runtime state for a player who left the arena flow.
     *
     * @param player Player leaving arena-owned scout state.
     */
    public void removePlayerFromArena(Player player) {
        if (player == null) {
            return;
        }

        Bukkit.getLogger().info(LOG_PREFIX + "removing arena state for " + player.getName());
        UUID playerUUID = player.getUniqueId();
        cooldownHandler.clearCooldown(playerUUID, true);
        runtimeHandler.clearPlayer(playerUUID);
        glowHandler.clearGlow(playerUUID);
        scoutRaisedUseLatch.remove(playerUUID);
    }

    /**
     * Restores scout runtime state for respawning arena players.
     *
     * @param player Respawning player.
     */
    public void processRespawnState(Player player) {
        if (player == null) {
            return;
        }

        MatchPlayerSessionHandler sessionHandler = getMatchPlayerSessionHandler();
        TeamManager teamManager = getTeamManager();
        KitSelectionHandler selectionHandler = getKitSelectionHandler();
        boolean abilityState = ScoutTaggerStateUtil.isAbilityState(getGameStateManager().getGameState());
        boolean inArena = sessionHandler.isPlayerInArena(player);
        boolean hasTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, player);
        boolean scoutSelected = selectionHandler.isScout(player);
        if (!abilityState || !inArena || !hasTeam || !scoutSelected) {
            Bukkit.getLogger().info(LOG_PREFIX + "respawn state skipped for " + player.getName()
                    + " abilityState=" + abilityState
                    + " inArena=" + inArena
                    + " hasTeam=" + hasTeam
                    + " scoutSelected=" + scoutSelected);
            return;
        }

        UUID playerUUID = player.getUniqueId();
        runtimeHandler.ensurePlayerState(playerUUID, MAX_AMMO);
        Bukkit.getLogger().info(LOG_PREFIX + "respawn state restored ready scout state for " + player.getName());
    }

    /**
     * Clears scout runtime state when the player disconnects.
     *
     * @param player Disconnecting player.
     */
    public void processPlayerQuitCleanup(Player player) {
        removePlayerFromArena(player);
    }

    /**
     * Clears scout runtime state when the player leaves the arena.
     *
     * @param player Arena-leaving player.
     */
    public void processPlayerLeaveArena(Player player) {
        removePlayerFromArena(player);
    }

    /**
     * Attempts to throw a Scout Tagger snowball.
     *
     * @param player Player trying to throw the scout snowball.
     * @return {@code true} when the throw succeeds.
     */
    public boolean tryThrowScoutSnowball(Player player) {
        if (player == null) {
            return false;
        }

        MatchPlayerSessionHandler sessionHandler = getMatchPlayerSessionHandler();
        TeamManager teamManager = getTeamManager();
        KitSelectionHandler selectionHandler = getKitSelectionHandler();
        FlagCarrierStateHandler carrierStateHandler = getFlagCarrierStateHandler();
        boolean inArena = sessionHandler.isPlayerInArena(player);
        boolean abilityState = ScoutTaggerStateUtil.isAbilityState(getGameStateManager().getGameState());
        boolean hasTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, player);
        boolean scoutSelected = selectionHandler.isScout(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean scoutSword = getScoutSwordItem().matches(heldItem);
        if (!inArena || !abilityState || !hasTeam || !scoutSelected || !scoutSword) {
            Bukkit.getLogger().info(LOG_PREFIX + "throw ignored for " + player.getName()
                    + " inArena=" + inArena
                    + " abilityState=" + abilityState
                    + " hasTeam=" + hasTeam
                    + " scoutSelected=" + scoutSelected
                    + " scoutSword=" + scoutSword
                    + " heldItem=" + describeHeldItem(heldItem)
                    + " gameState=" + getGameStateManager().getGameState());
            return false;
        }

        UUID playerUUID = player.getUniqueId();
        long nowMs = System.currentTimeMillis();
        Bukkit.getLogger().info(LOG_PREFIX + "throw requested by " + player.getName()
                + " ammoTracked=" + runtimeHandler.getTrackedPlayerIds().contains(playerUUID));
        player.clearActiveItem();
        if (carrierStateHandler.isFlagCarrier(playerUUID)) {
            Component deniedMessage = getMessage(CTFKeys.uiScoutFlagActionbarKey());
            sendActionBar(player, deniedMessage);
            player.playSound(player.getLocation(), DENIED_SOUND_KEY, 0.6f, 0.9f);
            Bukkit.getLogger().info(LOG_PREFIX + player.getName() + " throw blocked (carrying_flag)");
            return false;
        }

        ScoutTaggerPlayerMetaData playerState = runtimeHandler.ensurePlayerState(playerUUID, MAX_AMMO);
        int ammo = playerState == null ? 0 : playerState.getAmmo();
        Bukkit.getLogger().info(LOG_PREFIX + "throw state for " + player.getName()
                + " ammo=" + ammo + "/" + MAX_AMMO);
        if (ammo <= 0) {
            cooldownHandler.sendHeldStatusActionBar(player, 0, nowMs);
            player.playSound(player.getLocation(), DENIED_SOUND_KEY, 0.6f, 0.9f);
            Bukkit.getLogger().info(LOG_PREFIX + player.getName() + " throw blocked (no_ammo)");
            return false;
        }

        playerState.setAmmo(ammo - 1);

        // The projectile tag stores ownership so later hit hooks can resolve enemy-only glow logic.
        Snowball snowball = player.launchProjectile(Snowball.class);
        ScoutTaggerProjectileUtil.tagSnowball(snowball, snowballKey, shooterKey, playerUUID);
        Bukkit.getLogger().info(LOG_PREFIX + "launched snowball entity for " + player.getName()
                + " projectileId=" + snowball.getUniqueId());

        Vector boost = player.getLocation().getDirection().normalize().multiply(FORWARD_BOOST);
        player.setVelocity(player.getVelocity().add(boost));

        int remainingAmmo = playerState.getAmmo();
        cooldownHandler.sendHeldStatusActionBar(player, remainingAmmo, nowMs);
        player.playSound(player.getLocation(), THROW_SOUND_KEY, 0.7f, 1.1f);

        Location eyeLocation = player.getEyeLocation();
        spawnParticle(player, Particle.SNOWFLAKE, eyeLocation, 6, 0.12, 0.12, 0.12, 0.01);
        Bukkit.getLogger().info(LOG_PREFIX + player.getName() + " threw snowball (ammo " + remainingAmmo + "/" + MAX_AMMO + ")");
        return true;
    }

    /**
     * Applies Scout Tagger glow state when a tracked snowball hits a player.
     *
     * @param snowball Scout snowball projectile.
     * @param victim Player struck by the projectile.
     */
    public void processScoutSnowballHit(Snowball snowball, Player victim) {
        if (snowball == null || victim == null) {
            return;
        }

        Player shooter = ScoutTaggerProjectileUtil.resolveShooter(snowball, shooterKey);
        if (shooter == null) {
            Bukkit.getLogger().info(LOG_PREFIX + "snowball hit ignored because shooter metadata was missing");
            return;
        }

        processScoutSnowballHit(shooter, victim, snowball);
    }

    /**
     * Applies Scout Tagger glow state when a tracked projectile resolves onto an enemy.
     *
     * @param shooter Player who threw the projectile.
     * @param victim Player struck by the projectile.
     * @param projectile Projectile used for the tag resolution.
     */
    public void processScoutSnowballHit(Player shooter, Player victim, Projectile projectile) {
        if (shooter == null || victim == null || !(projectile instanceof Snowball snowball)) {
            return;
        }

        boolean taggedSnowball = ScoutTaggerProjectileUtil.isTaggedSnowball(snowball, snowballKey);
        if (!taggedSnowball) {
            Bukkit.getLogger().info(LOG_PREFIX + "snowball hit ignored because projectile was not tagged");
            return;
        }

        TeamManager teamManager = getTeamManager();
        boolean sameTeam = ScoutTaggerStateUtil.isFriendlyTeamHit(teamManager, shooter, victim);
        boolean hasShooterTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, shooter);
        boolean hasVictimTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, victim);
        if (!hasShooterTeam || !hasVictimTeam || sameTeam) {
            Bukkit.getLogger().info(LOG_PREFIX + "snowball hit ignored shooter=" + shooter.getName()
                    + " victim=" + victim.getName()
                    + " hasShooterTeam=" + hasShooterTeam
                    + " hasVictimTeam=" + hasVictimTeam
                    + " sameTeam=" + sameTeam);
            return;
        }

        glowHandler.applyGlow(victim);
        Bukkit.getLogger().info(LOG_PREFIX + shooter.getName() + " tagged " + victim.getName() + " (glow reset)");
    }

    /**
     * Cancels direct damage for scout snowballs and ignores friendly hits.
     *
     * @param snowball Scout snowball projectile.
     * @param victim Player hit by the projectile.
     * @param event Damage event to cancel when needed.
     */
    public void processScoutSnowballDamageCancel(Snowball snowball, Player victim, EntityDamageByEntityEvent event) {
        if (snowball == null || victim == null || event == null) {
            return;
        }

        boolean taggedSnowball = ScoutTaggerProjectileUtil.isTaggedSnowball(snowball, snowballKey);
        if (!taggedSnowball) {
            return;
        }

        Player shooter = ScoutTaggerProjectileUtil.resolveShooter(snowball, shooterKey);
        if (shooter == null) {
            Bukkit.getLogger().info(LOG_PREFIX + "damage cancel hit missing shooter metadata");
            return;
        }

        TeamManager teamManager = getTeamManager();
        boolean sameTeam = ScoutTaggerStateUtil.isFriendlyTeamHit(teamManager, shooter, victim);
        boolean hasShooterTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, shooter);
        boolean hasVictimTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, victim);
        if (!hasShooterTeam || !hasVictimTeam || sameTeam) {
            Bukkit.getLogger().info(LOG_PREFIX + "damage cancelled without enemy tag shooter=" + shooter.getName()
                    + " victim=" + victim.getName()
                    + " hasShooterTeam=" + hasShooterTeam
                    + " hasVictimTeam=" + hasVictimTeam
                    + " sameTeam=" + sameTeam);
            event.setCancelled(true);
            return;
        }

        // Scout snowballs tag only; they never deal direct damage to enemy players.
        event.setCancelled(true);
    }

    /**
     * Regenerates scout ammo once per second for active scouts.
     */
    private void startScoutHeldVisualTimer() {
        if (scoutHeldVisualTimerTask != null) {
            return;
        }

        scoutHeldVisualTimerTask = AbilityTaskOrchestrator.startTimer(
                scoutHeldVisualTimerTask,
                this::refreshScoutHeldState,
                1L,
                2L);
    }

    private void refreshScoutHeldState() {
        cooldownHandler.refreshCooldownVisuals();

        MatchPlayerSessionHandler sessionHandler = getMatchPlayerSessionHandler();
        TeamManager teamManager = getTeamManager();
        KitSelectionHandler selectionHandler = getKitSelectionHandler();
        Set<Player> joinedPlayers = new HashSet<>(teamManager.getJoinedPlayers());
        for (Player joinedPlayer : joinedPlayers) {
            UUID playerUUID = joinedPlayer.getUniqueId();
            ItemStack heldItem = joinedPlayer.getInventory().getItemInMainHand();
            boolean activeScoutUse = sessionHandler.isPlayerInArena(joinedPlayer)
                    && selectionHandler.isScout(joinedPlayer)
                    && getScoutSwordItem().matches(heldItem)
                    && joinedPlayer.hasActiveItem()
                    && joinedPlayer.getActiveItemHand() == EquipmentSlot.HAND
                    && getScoutSwordItem().matches(joinedPlayer.getActiveItem());
            if (!activeScoutUse) {
                scoutRaisedUseLatch.remove(playerUUID);
                continue;
            }
            if (!scoutRaisedUseLatch.add(playerUUID)) {
                continue;
            }
            tryThrowScoutSnowball(joinedPlayer);
        }
    }

    private void regenAmmo() {
        boolean abilityState = ScoutTaggerStateUtil.isAbilityState(getGameStateManager().getGameState());
        if (!abilityState) {
            Bukkit.getLogger().info(LOG_PREFIX + "regen skipped because game state is " + getGameStateManager().getGameState());
            return;
        }

        cooldownHandler.setNextAmmoRegenAtMs(System.currentTimeMillis() + AMMO_REGEN_PERIOD_MS);

        Set<UUID> activeScoutIds = new HashSet<>();
        TeamManager teamManager = getTeamManager();
        KitSelectionHandler selectionHandler = getKitSelectionHandler();
        Set<Player> joinedPlayers = new HashSet<>(teamManager.getJoinedPlayers());
        for (Player joinedPlayer : joinedPlayers) {
            boolean scoutSelected = selectionHandler.isScout(joinedPlayer);
            boolean hasTeam = ScoutTaggerStateUtil.hasAssignedTeam(teamManager, joinedPlayer);
            if (!scoutSelected || !hasTeam) {
                Bukkit.getLogger().info(LOG_PREFIX + "regen skipped for " + joinedPlayer.getName()
                        + " scoutSelected=" + scoutSelected + " hasTeam=" + hasTeam);
                continue;
            }

            UUID playerUUID = joinedPlayer.getUniqueId();
            activeScoutIds.add(playerUUID);

            ScoutTaggerPlayerMetaData playerState = runtimeHandler.ensurePlayerState(playerUUID, MAX_AMMO);
            int ammo = playerState == null ? 0 : playerState.getAmmo();
            if (ammo < MAX_AMMO) {
                playerState.setAmmo(ammo + 1);
                Bukkit.getLogger().info(LOG_PREFIX + "regen +1 ammo for " + joinedPlayer.getName()
                        + " now=" + playerState.getAmmo() + "/" + MAX_AMMO);
            }
        }

        // Drop stale state for players who are no longer in the active scout pool.
        for (UUID trackedPlayerId : runtimeHandler.getTrackedPlayerIds()) {
            if (activeScoutIds.contains(trackedPlayerId)) {
                continue;
            }

            Bukkit.getLogger().info(LOG_PREFIX + "dropping stale scout state for " + trackedPlayerId
                    + " activeScoutCount=" + activeScoutIds.size());
            runtimeHandler.clearPlayer(trackedPlayerId);
            glowHandler.clearGlow(trackedPlayerId);
        }
    }

    private String describeHeldItem(ItemStack heldItem) {
        if (heldItem == null) {
            return "null";
        }
        if (heldItem.getType() == null) {
            return "unknown";
        }
        return heldItem.getType().name();
    }
}
