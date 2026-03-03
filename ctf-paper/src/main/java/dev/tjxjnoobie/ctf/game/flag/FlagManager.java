package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CTFPlayerContainer;
import dev.tjxjnoobie.ctf.game.flag.interfaces.FlagCaptureHandler;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.game.flag.util.FlagDropLocationUtil;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Handles flag pickup/drop/return/capture transitions and carrier item state.
 */
public final class FlagManager implements MessageAccess {
    private static final int FLAG_SLOT = 0;
    private static final String FLAG_METADATA_KEY = "ctf-flag-team";
    private static final String LOG_PREFIX = "[CTF] [FlagManager] ";
    private static final int CARRIER_SLOW_DURATION = 20 * 60 * 10;
    private static final double BOSS_BAR_DISTANCE_CAP = 80.0;
    private static final int TEAM_GLOW_REFRESH_TICKS = 40;
    private static final int CAPTURE_ZONE_PARTICLE_PERIOD_TICKS = 10;
    private static final long OWN_FLAG_NOTICE_COOLDOWN_MS = 2_000L;
    private static final int FIREWORK_MIN_HEIGHT = 5;
    private static final int FIREWORK_MAX_HEIGHT = 8;
    private static final int FIREWORK_DETONATE_TICKS = 46;
    private static final List<org.bukkit.FireworkEffect.Type> FIREWORK_TYPES = List.of(
        org.bukkit.FireworkEffect.Type.BALL,
        org.bukkit.FireworkEffect.Type.BALL_LARGE,
        org.bukkit.FireworkEffect.Type.BURST,
        org.bukkit.FireworkEffect.Type.CREEPER,
        org.bukkit.FireworkEffect.Type.STAR
    );

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CTFPlayerContainer arenaPlayers;
    private final BossBarManager bossBarManager;
    private final FlagConfigHandler flagConfigHandler;
    private final FlagIndicator flagIndicator;
    private final CTFCaptureZoneService captureZoneService;
    private final CaptureZoneParticleRenderer captureZoneParticleRenderer;
    private final FlagCaptureHandler captureHandler;
    private final Consumer<String> debugPublisher;
    private final Method sendPotionEffectChangeMethod;
    private final Method sendPotionEffectChangeRemoveMethod;

    private final Map<String, FlagMetaData> flagsByTeam = new HashMap<>();
    private final Map<UUID, ItemStack> replacedSlotByPlayer = new HashMap<>();
    private final Map<UUID, Long> ownFlagNoticeUntilByPlayer = new HashMap<>();
    private PotionEffect teamGlowEffect;
    private int captureZoneParticleTicker;
    private int indicatorVisibilityTicker;
    private BukkitTask bossBarTask;
    private org.bukkit.FireworkEffect.Type lastFireworkType;

    public FlagManager(JavaPlugin plugin, TeamManager teamManager, CTFPlayerContainer arenaPlayers, BossBarManager bossBarManager,
                       FlagCaptureHandler captureHandler, Consumer<String> debugPublisher) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.arenaPlayers = arenaPlayers;
        this.bossBarManager = bossBarManager;
        this.flagConfigHandler = new FlagConfigHandler(plugin);
        this.flagIndicator = new FlagIndicator(plugin, teamManager);
        this.captureZoneService = new CTFCaptureZoneService(teamManager.getSpawnConfigHandler(), teamManager);
        this.captureZoneParticleRenderer = new CaptureZoneParticleRenderer();
        this.captureHandler = captureHandler;
        this.debugPublisher = debugPublisher == null ? ignored -> { } : debugPublisher;
        this.sendPotionEffectChangeMethod = resolvePlayerMethod("sendPotionEffectChange", LivingEntity.class, PotionEffect.class);
        this.sendPotionEffectChangeRemoveMethod = resolvePlayerMethod("sendPotionEffectChangeRemove", LivingEntity.class, PotionEffectType.class);
        registerFlags();
    }

    /**
     * Starts periodic flag UI updates.
     */
    public void onMatchStart() {
        resetFlagsToBase();
        captureZoneParticleTicker = 0;
        startBossBarTask();
        Bukkit.getLogger().info(LOG_PREFIX + "Manager started - match active.");
    }

    /**
     * Clears flag UI and carrier state.
     */
    public void onMatchStop() {
        stopBossBarTask();
        clearAllBossBars();
        flagIndicator.removeAllFlagIndicators();
        clearTeamGlowVisuals();
        clearCarrierEffects();
        Bukkit.getLogger().info(LOG_PREFIX + "Manager stopped - indicators and boss bars cleared.");
    }

    /**
     * Resets flags to base.
     */
    public void resetFlagsToBase() {
        for (Map.Entry<String, FlagMetaData> entry : flagsByTeam.entrySet()) {
            String teamKey = entry.getKey();
            FlagMetaData flag = entry.getValue();
            if (flag == null || flag.baseLocation == null) {
                continue;
            }

            if (flag.state == FlagState.DROPPED && flag.activeLocation != null) {
                setBlockAir(flag.activeLocation, teamKey);
            }

            flag.state = FlagState.AT_BASE;
            flag.activeLocation = flag.baseLocation;
            flag.carrier = null;
            setFlagBlock(flag.baseLocation, teamKey);
        }

        updateBossBars();
    }

    /**
     * Returns true when both team bases are configured.
     */
    public boolean areBasesReady() {
        return hasBase(TeamManager.RED) && hasBase(TeamManager.BLUE);
    }

    /**
     * Returns the configured base location for a team.
     */
    public Location getBaseLocation(String teamKey) {
        FlagMetaData flag = teamKey == null ? null : flagsByTeam.get(teamKey);
        return flag == null || flag.baseLocation == null ? null : flag.baseLocation.clone();
    }

    /**
     * Removes stale indicator entities (including persisted restart leftovers)
     * and re-renders active indicators from current flag state.
     */
    public void resetFlagIndicators() {
        List<Location> indicatorLocations = new ArrayList<>();
        for (Map.Entry<String, FlagMetaData> entry : flagsByTeam.entrySet()) {
            Location indicator = resolveBaseIndicatorLocation(entry.getKey(), entry.getValue());
            if (indicator != null) {
                indicatorLocations.add(indicator);
            }
        }
        flagIndicator.resetFlagIndicators(indicatorLocations);
        ensureFlagIndicators();
        flagIndicator.syncVisibility(teamManager.getJoinedPlayers());
    }

    /**
     * Refreshes indicator visibility for all online players.
     */
    public void syncIndicatorVisibility() {
        flagIndicator.syncVisibility(teamManager.getJoinedPlayers());
    }

    /**
     * Clears all held flag items from carriers.
     */
    public void clearCarrierItems() {
        for (FlagMetaData flag : flagsByTeam.values()) {
            if (flag != null && flag.state == FlagState.CARRIED && flag.carrier != null) {
                Player carrier = Bukkit.getPlayer(flag.carrier);
                if (carrier != null) {
                    clearCarrierFlagItem(carrier);
                }
            }
        }
    }

    /**
     * Sets one team flag base at player's block location.
     */
    public boolean setFlagBase(Player player, String teamKey) {
        if (player == null || teamKey == null) {
            return false;
        }

        FlagMetaData flag = flagsByTeam.get(teamKey);
        if (flag == null) {
            return false;
        }

        Location base = toBlockLocation(player.getLocation());
        if (flag.activeLocation != null && !sameBlock(flag.activeLocation, base)) {
            setBlockAir(flag.activeLocation, teamKey);
        }

        Material material = resolveFlagMaterial(teamKey);
        Location indicator = toIndicatorLocation(base);
        flagConfigHandler.setFlagData(teamKey, new FlagConfigData(base, indicator, material));

        flag.baseLocation = base;
        flag.state = FlagState.AT_BASE;
        flag.activeLocation = base;
        flag.carrier = null;
        setFlagBlock(base, teamKey);
        updateBossBars();

        Bukkit.getLogger().info(LOG_PREFIX + "Base set - team=" + teamKey + " location=" + formatLocation(base));
        publishDebug("setflag team=" + teamKey + " location=" + formatLocation(base));
        return true;
    }

    /**
     * Handles player touching a flag block.
     */
    public boolean handleFlagTouch(Player player, Location blockLocation) {
        if (player == null || blockLocation == null) {
            return false;
        }

        String playerTeam = teamManager.getTeamKey(player);
        if (playerTeam == null) {
            return false;
        }

        String touchedTeam = teamForFlagAt(blockLocation);
        if (touchedTeam == null) {
            return false;
        }

        if (Objects.equals(touchedTeam, playerTeam)) {
            FlagMetaData ownFlag = flagsByTeam.get(playerTeam);
            if (ownFlag != null && ownFlag.state == FlagState.DROPPED) {
                return returnFlag(player, playerTeam);
            }
            notifyOwnFlagCaptureBlocked(player);
            return false;
        }

        return pickupFlag(player, touchedTeam);
    }

    /**
     * Drops a carried flag at player's location.
     */
    public void dropFlagIfCarrier(Player player) {
        if (player == null) {
            return;
        }

        String carriedTeam = flagCarriedBy(player.getUniqueId());
        if (carriedTeam == null) {
            return;
        }

        FlagMetaData flag = flagsByTeam.get(carriedTeam);
        if (flag == null) {
            return;
        }

        Location dropLocation = resolveDroppedFlagLocation(player);
        flag.state = FlagState.DROPPED;
        flag.activeLocation = dropLocation;
        flag.carrier = null;

        setDroppedFlagBlock(dropLocation, carriedTeam);
        clearCarrierFlagItem(player);
        clearCarrierEffects(player);

        broadcast(msg("broadcast.flag.drop", Map.of(
            "player", player.getName(),
            "team", teamManager.getDisplayName(carriedTeam)
        )));
        publishDebug("flag drop team=" + carriedTeam + " player=" + player.getName());

        playDropEffects(player, carriedTeam, dropLocation);
        updateBossBars();
    }

    /**
     * Evaluates capture when a carrier reaches a return zone.
     */
    public void handleMove(Player player, Location to) {
        if (player == null || to == null) {
            return;
        }

        String playerTeam = teamManager.getTeamKey(player);
        if (playerTeam == null) {
            return;
        }

        String carriedTeam = flagCarriedBy(player.getUniqueId());
        if (carriedTeam == null) {
            return;
        }

        FlagMetaData ownFlag = flagsByTeam.get(playerTeam);
        if (ownFlag == null || ownFlag.state != FlagState.AT_BASE) {
            return;
        }

        List<Location> returnPoints = teamManager.getReturnPoints(playerTeam);
        if (!captureZoneService.isInsideCaptureZone(playerTeam, to, returnPoints, ownFlag.baseLocation)) {
            return;
        }

        capture(player, playerTeam, carriedTeam);
    }

    /**
     * Returns true when player is currently flag carrier.
     */
    public boolean isFlagCarrier(UUID playerId) {
        return flagCarriedBy(playerId) != null;
    }

    /**
     * Keeps carrier flag in locked slot.
     */
    public void lockCarrierHotbarSlot(Player player) {
        if (player == null || !isFlagCarrier(player.getUniqueId())) {
            return;
        }

        ItemStack flagItem = player.getInventory().getItem(FLAG_SLOT);
        if (flagItem == null || !isFlagMaterial(flagItem.getType())) {
            String carriedTeam = flagCarriedBy(player.getUniqueId());
            if (carriedTeam != null) {
                giveCarrierFlagItem(player, carriedTeam);
            }
        }
    }

    /**
     * Clears one player's carrier item/effects.
     */
    public void clearCarrierItem(Player player) {
        clearCarrierFlagItem(player);
        clearCarrierEffects(player);
    }

    private void capture(Player player, String scoringTeam, String capturedFlagTeam) {
        FlagMetaData capturedFlag = flagsByTeam.get(capturedFlagTeam);
        if (capturedFlag == null || capturedFlag.baseLocation == null) {
            return;
        }

        capturedFlag.state = FlagState.AT_BASE;
        capturedFlag.activeLocation = capturedFlag.baseLocation;
        capturedFlag.carrier = null;
        setFlagBlock(capturedFlag.baseLocation, capturedFlagTeam);

        clearCarrierFlagItem(player);
        clearCarrierEffects(player);

        captureHandler.onCapture(player, scoringTeam, capturedFlagTeam);
        publishDebug("flag capture team=" + scoringTeam + " player=" + player.getName());

        broadcast(msg("broadcast.flag.capture", Map.of(
            "player", player.getName(),
            "captured", teamManager.getDisplayName(capturedFlagTeam),
            "scoring", teamManager.getDisplayName(scoringTeam)
        )));

        // Fireworks should celebrate at the scoring team's base location.
        Location scoringBase = flagsByTeam.get(scoringTeam) == null ? null : flagsByTeam.get(scoringTeam).baseLocation;
        playCaptureEffects(scoringTeam, capturedFlagTeam, scoringBase == null ? capturedFlag.baseLocation : scoringBase);

        updateBossBars();
    }

    private boolean pickupFlag(Player player, String flagTeam) {
        if (flagCarriedBy(player.getUniqueId()) != null) {
            return false;
        }

        FlagMetaData flag = flagsByTeam.get(flagTeam);
        if (flag == null || flag.state == FlagState.CARRIED) {
            return false;
        }

        if (flag.activeLocation != null) {
            setBlockAir(flag.activeLocation, flagTeam);
        }

        flag.state = FlagState.CARRIED;
        flag.activeLocation = null;
        flag.carrier = player.getUniqueId();

        giveCarrierFlagItem(player, flagTeam);
        applyCarrierEffects(player);
        showPickupTitle(player);
        player.sendActionBar(msg("actionbar.flag_carrier"));

        broadcast(msg("broadcast.flag.pickup", Map.of(
            "player", player.getName(),
            "team", teamManager.getDisplayName(flagTeam)
        )));
        publishDebug("flag pickup team=" + flagTeam + " player=" + player.getName());

        playPickupEffects(player, flagTeam);
        updateBossBars();
        return true;
    }

    private boolean returnFlag(Player player, String teamKey) {
        FlagMetaData flag = flagsByTeam.get(teamKey);
        if (flag == null || flag.state != FlagState.DROPPED || flag.baseLocation == null) {
            return false;
        }

        if (flag.activeLocation != null) {
            setBlockAir(flag.activeLocation, teamKey);
        }

        flag.state = FlagState.AT_BASE;
        flag.activeLocation = flag.baseLocation;
        flag.carrier = null;
        setFlagBlock(flag.baseLocation, teamKey);

        for (Player teammate : teamManager.getTeamPlayers(teamKey)) {
            teammate.sendMessage(msg("broadcast.flag.return_own", Map.of(
                "player", player.getName(),
                "team", teamManager.getDisplayName(teamKey)
            )));
        }
        for (Player enemy : teamManager.getTeamPlayers(otherTeam(teamKey))) {
            enemy.sendMessage(msg("broadcast.flag.return_enemy", Map.of(
                "player", player.getName(),
                "team", teamManager.getDisplayName(teamKey)
            )));
        }

        playReturnEffects(teamKey, flag.baseLocation);
        publishDebug("flag return team=" + teamKey + " player=" + player.getName());
        updateBossBars();
        return true;
    }

    private void registerFlags() {
        flagsByTeam.put(TeamManager.RED, new FlagMetaData());
        flagsByTeam.put(TeamManager.BLUE, new FlagMetaData());

        loadFlagFromConfig(TeamManager.RED);
        loadFlagFromConfig(TeamManager.BLUE);
    }

    private void loadFlagFromConfig(String teamKey) {
        FlagConfigData data = flagConfigHandler.getFlagData(teamKey);
        if (data == null || data.getBaseLocation() == null) {
            return;
        }

        FlagMetaData flag = flagsByTeam.get(teamKey);
        if (flag == null) {
            return;
        }

        Location base = toBlockLocation(data.getBaseLocation());
        flag.baseLocation = base;
        flag.activeLocation = base;
        flag.state = FlagState.AT_BASE;
        flag.carrier = null;
        setFlagBlockWithoutIndicator(base, teamKey);
    }

    private boolean hasBase(String teamKey) {
        FlagMetaData flag = flagsByTeam.get(teamKey);
        return flag != null && flag.baseLocation != null;
    }

    private String teamForFlagAt(Location location) {
        if (location == null || location.getBlock() == null) {
            return null;
        }

        String teamKey = null;
        for (org.bukkit.metadata.MetadataValue value : location.getBlock().getMetadata(FLAG_METADATA_KEY)) {
            if (value.getOwningPlugin() == plugin) {
                teamKey = value.asString();
                break;
            }
        }

        if (teamKey == null) {
            return null;
        }

        FlagMetaData flag = flagsByTeam.get(teamKey);
        if (flag == null || flag.activeLocation == null) {
            return null;
        }

        return sameBlock(flag.activeLocation, location) ? teamKey : null;
    }

    private String flagCarriedBy(UUID playerId) {
        for (Map.Entry<String, FlagMetaData> entry : flagsByTeam.entrySet()) {
            FlagMetaData flag = entry.getValue();
            if (flag != null && flag.state == FlagState.CARRIED && playerId.equals(flag.carrier)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void giveCarrierFlagItem(Player player, String flagTeam) {
        ItemStack current = player.getInventory().getItem(FLAG_SLOT);
        replacedSlotByPlayer.put(player.getUniqueId(), current == null ? null : current.clone());

        ItemStack flagItem = new ItemStack(resolveFlagMaterial(flagTeam));
        ItemMeta meta = flagItem.getItemMeta();
        if (meta != null) {
            meta.displayName(teamManager.getDisplayComponent(flagTeam).append(Component.text(" Flag", NamedTextColor.GRAY)));
            flagItem.setItemMeta(meta);
        }

        player.getInventory().setItem(FLAG_SLOT, flagItem);
    }

    private void clearCarrierFlagItem(Player player) {
        if (player == null) {
            return;
        }

        ItemStack previous = replacedSlotByPlayer.remove(player.getUniqueId());
        player.getInventory().setItem(FLAG_SLOT, previous);
        bossBarManager.hideCarrierBar(player);
    }

    private void applyCarrierEffects(Player player) {
        if (player == null) {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, CARRIER_SLOW_DURATION, 0, false, false, false), true);
    }

    private void clearCarrierEffects(Player player) {
        if (player == null) {
            return;
        }

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setExp(0.0f);
        player.setLevel(0);
    }

    private void clearCarrierEffects() {
        for (UUID playerId : new HashMap<>(replacedSlotByPlayer).keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                clearCarrierEffects(player);
            }
        }
    }

    private void showPickupTitle(Player player) {
        if (player == null) {
            return;
        }

        Title title = Title.title(
            Component.text("YOU PICKED UP THE FLAG", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text("Return to base to score", NamedTextColor.AQUA)
        );
        player.showTitle(title);
    }

    private void startBossBarTask() {
        if (bossBarTask != null) {
            return;
        }

        // Faster updates for smoother meter changes.
        bossBarTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
            Bukkit.getScheduler().runTask(plugin, this::updateBossBars), 0L, 2L);
    }

    private void stopBossBarTask() {
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
    }

    private void updateBossBars() {
        ensureFlagIndicators();
        syncFlagIndicatorVisibility();
        renderCaptureZoneBorders();
        updateReturnBossBarsForTeam(TeamManager.RED);
        updateReturnBossBarsForTeam(TeamManager.BLUE);
        updateCarrierBossBars();
        updateTeamGlow();
    }

    private void renderCaptureZoneBorders() {
        captureZoneParticleTicker += 2;
        if (captureZoneParticleTicker < CAPTURE_ZONE_PARTICLE_PERIOD_TICKS) {
            return;
        }
        captureZoneParticleTicker = 0;

        double captureRadius = captureZoneService.getCaptureRadius();
        renderCaptureZoneBorderForTeam(TeamManager.RED, captureRadius);
        renderCaptureZoneBorderForTeam(TeamManager.BLUE, captureRadius);
    }

    private void renderCaptureZoneBorderForTeam(String teamKey, double captureRadius) {
        List<Player> viewers = teamManager.getTeamPlayers(teamKey);
        if (viewers.isEmpty()) {
            return;
        }

        List<Location> returnPoints = teamManager.getReturnPoints(teamKey);
        FlagMetaData ownFlag = flagsByTeam.get(teamKey);
        Location baseLocation = ownFlag == null ? null : ownFlag.baseLocation;
        captureZoneParticleRenderer.renderTeamZones(teamKey, viewers, returnPoints, baseLocation, captureRadius);
    }

    private void updateReturnBossBarsForTeam(String teamKey) {
        FlagMetaData flag = flagsByTeam.get(teamKey);
        if (flag == null || flag.state != FlagState.DROPPED || flag.activeLocation == null) {
            clearReturnBossBars(teamKey);
            return;
        }

        Location flagLocation = flag.activeLocation;
        for (Player player : teamManager.getTeamPlayers(teamKey)) {
            double distance = player.getLocation().distance(flagLocation);
            float progress = distanceToProgress(distance);
            bossBarManager.showReturnBar(player, msg("bossbar.flag_return", Map.of(
                "distance", Integer.toString((int) Math.round(distance)),
                "direction", resolveDirection(player.getLocation(), flagLocation)
            )), progress);
        }
    }

    private void updateCarrierBossBars() {
        for (Map.Entry<String, FlagMetaData> entry : flagsByTeam.entrySet()) {
            FlagMetaData flag = entry.getValue();
            if (flag == null || flag.state != FlagState.CARRIED || flag.carrier == null) {
                continue;
            }

            Player carrier = Bukkit.getPlayer(flag.carrier);
            if (carrier == null) {
                continue;
            }

            String carrierTeam = teamManager.getTeamKey(carrier);
            if (carrierTeam == null) {
                continue;
            }

            FlagMetaData ownFlag = flagsByTeam.get(carrierTeam);
            if (ownFlag == null || ownFlag.baseLocation == null) {
                continue;
            }

            double distance = carrier.getLocation().distance(ownFlag.baseLocation);
            float progress = distanceToProgress(distance);
            String direction = resolveDirection(carrier.getLocation(), ownFlag.baseLocation);

            bossBarManager.showCarrierBar(carrier, msg("bossbar.flag_carrier", Map.of(
                "distance", Integer.toString((int) Math.round(distance)),
                "direction", direction
            )), progress);

            carrier.sendActionBar(msg("actionbar.flag_carrier"));
            carrier.setLevel((int) Math.round(distance));
            carrier.setExp(progress);
        }
    }

    private void updateTeamGlow() {
        List<Player> joinedPlayers = teamManager.getJoinedPlayers();
        for (Player viewer : joinedPlayers) {
            String viewerTeam = teamManager.getTeamKey(viewer);
            if (viewerTeam == null) {
                continue;
            }

            for (Player target : joinedPlayers) {
                if (viewer.getUniqueId().equals(target.getUniqueId())) {
                    continue;
                }

                String targetTeam = teamManager.getTeamKey(target);
                if (targetTeam != null && targetTeam.equals(viewerTeam)) {
                    applyFakeTeamGlow(viewer, target);
                } else {
                    clearFakeTeamGlow(viewer, target);
                }
            }
        }
    }

    private void ensureFlagIndicators() {
        for (Map.Entry<String, FlagMetaData> entry : flagsByTeam.entrySet()) {
            String teamKey = entry.getKey();
            FlagMetaData flag = entry.getValue();
            if (flag == null) {
                continue;
            }

            if (flag.state == FlagState.CARRIED || flag.activeLocation == null) {
                flagIndicator.removeFlagIndicatorForTeam(teamKey);
                continue;
            }

            Location indicatorLocation = resolveIndicatorSpawnLocation(teamKey, flag);
            if (indicatorLocation == null) {
                flagIndicator.removeFlagIndicatorForTeam(teamKey);
                continue;
            }

            if (!flagIndicator.hasFlagIndicatorForTeam(teamKey)) {
                flagIndicator.spawnFlagIndicatorForTeam(teamKey, indicatorLocation);
            }
        }
    }

    private void syncFlagIndicatorVisibility() {
        indicatorVisibilityTicker += 2;
        if (indicatorVisibilityTicker < 20) {
            return;
        }
        indicatorVisibilityTicker = 0;
        flagIndicator.syncVisibility(teamManager.getJoinedPlayers());
    }

    private void applyFakeTeamGlow(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return;
        }

        PotionEffect glow = getTeamGlowEffect();
        if (sendPotionEffectChangeMethod != null) {
            try {
                if (glow != null) {
                    sendPotionEffectChangeMethod.invoke(viewer, target, glow);
                    return;
                }
            } catch (ReflectiveOperationException ignored) {
                // Packet API unavailable: skip per-viewer glow.
            }
        }
    }

    private void clearFakeTeamGlow(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return;
        }

        if (sendPotionEffectChangeRemoveMethod != null) {
            try {
                sendPotionEffectChangeRemoveMethod.invoke(viewer, target, PotionEffectType.GLOWING);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Packet API unavailable: skip per-viewer glow removal.
            }
        }
    }

    private void clearTeamGlowVisuals() {
        List<Player> joinedPlayers = teamManager.getJoinedPlayers();
        for (Player viewer : joinedPlayers) {
            for (Player target : joinedPlayers) {
                if (viewer.getUniqueId().equals(target.getUniqueId())) {
                    continue;
                }
                clearFakeTeamGlow(viewer, target);
            }
        }

        for (Player player : joinedPlayers) {
            player.setGlowing(false);
        }
    }

    private PotionEffect getTeamGlowEffect() {
        if (teamGlowEffect != null) {
            return teamGlowEffect;
        }
        try {
            teamGlowEffect = new PotionEffect(PotionEffectType.GLOWING, TEAM_GLOW_REFRESH_TICKS, 0, false, false, false);
        } catch (Throwable ignored) {
            return null;
        }
        return teamGlowEffect;
    }

    private float distanceToProgress(double distance) {
        double clamped = Math.max(0.0, Math.min(BOSS_BAR_DISTANCE_CAP, distance));
        return (float) (1.0 - (clamped / BOSS_BAR_DISTANCE_CAP));
    }

    private String resolveDirection(Location from, Location to) {
        if (from == null || to == null) {
            return "N";
        }

        Vector delta = to.toVector().subtract(from.toVector());
        if (Math.abs(delta.getX()) > Math.abs(delta.getZ())) {
            return delta.getX() >= 0 ? "E" : "W";
        }
        return delta.getZ() >= 0 ? "S" : "N";
    }

    private void clearReturnBossBars(String teamKey) {
        for (Player player : teamManager.getTeamPlayers(teamKey)) {
            bossBarManager.hideReturnBar(player);
        }
    }

    private void clearAllBossBars() {
        bossBarManager.clearAll();
    }

    private void playPickupEffects(Player player, String flagTeam) {
        String playerTeam = teamManager.getTeamKey(player);
        if (playerTeam == null) {
            return;
        }

        playTeamEffects(playerTeam, flagTeam,
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.BLOCK_NOTE_BLOCK_BASS,
            Particle.HAPPY_VILLAGER, Particle.SMOKE,
            player.getLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
    }

    private void playDropEffects(Player player, String carriedTeam, Location dropLocation) {
        String playerTeam = teamManager.getTeamKey(player);
        if (playerTeam == null) {
            return;
        }

        playTeamEffects(carriedTeam, playerTeam,
            Sound.BLOCK_NOTE_BLOCK_PLING, Sound.ENTITY_VILLAGER_NO,
            Particle.HAPPY_VILLAGER, Particle.SMOKE,
            dropLocation);
    }

    private void playReturnEffects(String teamKey, Location baseLocation) {
        String otherTeam = otherTeam(teamKey);
        playTeamEffects(teamKey, otherTeam,
            Sound.ENTITY_PLAYER_LEVELUP, Sound.BLOCK_NOTE_BLOCK_BASS,
            Particle.HAPPY_VILLAGER, Particle.SMOKE,
            baseLocation);
    }

    private void playCaptureEffects(String scoringTeam, String capturedTeam, Location location) {
        playTeamEffects(scoringTeam, capturedTeam,
            Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_VILLAGER_NO,
            Particle.FIREWORK, Particle.SMOKE,
            location);

        if (location != null && location.getWorld() != null) {
            location.getWorld().spawnParticle(Particle.FIREWORK, location.clone().add(0.5, 1.2, 0.5), 25, 0.3, 0.3, 0.3, 0.02);
            spawnCaptureFirework(scoringTeam, location);
        }
    }

    private void spawnCaptureFirework(String teamKey, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        Location spawnLocation = location.clone().add(0.5, nextFireworkHeightOffset(), 0.5);
        Firework firework = spawnLocation.getWorld().spawn(spawnLocation, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        Color color = TeamManager.RED.equals(teamKey) ? Color.RED : Color.BLUE;
        boolean flicker = ThreadLocalRandom.current().nextBoolean();
        boolean trail = ThreadLocalRandom.current().nextBoolean();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
            .withColor(color)
            .with(nextFireworkType())
            .flicker(flicker)
            .trail(trail)
            .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.setTicksToDetonate(FIREWORK_DETONATE_TICKS);
    }

    private org.bukkit.FireworkEffect.Type nextFireworkType() {
        org.bukkit.FireworkEffect.Type next = lastFireworkType;
        for (int attempts = 0; attempts < 6 && next == lastFireworkType; attempts++) {
            next = FIREWORK_TYPES.get(ThreadLocalRandom.current().nextInt(FIREWORK_TYPES.size()));
        }
        lastFireworkType = next;
        return next;
    }

    private double nextFireworkHeightOffset() {
        return ThreadLocalRandom.current().nextDouble(FIREWORK_MIN_HEIGHT, FIREWORK_MAX_HEIGHT + 1.0);
    }

    private void playTeamEffects(String positiveTeam, String negativeTeam, Sound positiveSound, Sound negativeSound,
                                 Particle positiveParticle, Particle negativeParticle, Location location) {
        if (location == null) {
            return;
        }

        for (Player player : teamManager.getTeamPlayers(positiveTeam)) {
            player.playSound(player.getLocation(), positiveSound, 1.0f, 1.1f);
            player.spawnParticle(positiveParticle, location, 15, 0.3, 0.3, 0.3, 0.02);
        }

        for (Player player : teamManager.getTeamPlayers(negativeTeam)) {
            player.playSound(player.getLocation(), negativeSound, 1.0f, 0.8f);
            player.spawnParticle(negativeParticle, location, 15, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private String otherTeam(String teamKey) {
        if (TeamManager.RED.equals(teamKey)) {
            return TeamManager.BLUE;
        }
        return TeamManager.RED;
    }

    private void broadcast(Component message) {
        arenaPlayers.broadcast(message);
    }

    private Material resolveFlagMaterial(String teamKey) {
        return flagConfigHandler.getMaterial(teamKey, teamManager.getFlagMaterial(teamKey));
    }

    private Location resolveDroppedFlagLocation(Player player) {
        Location rawDrop = toBlockLocation(player.getLocation());
        String scoringTeam = teamManager.getTeamKey(player);
        if (scoringTeam == null) {
            return rawDrop;
        }

        FlagMetaData ownFlag = flagsByTeam.get(scoringTeam);
        Location ownBaseLocation = ownFlag == null ? null : ownFlag.baseLocation;
        List<Location> returnPoints = teamManager.getReturnPoints(scoringTeam);
        return FlagDropLocationUtil.resolveDropLocation(
            player,
            rawDrop,
            ownBaseLocation,
            returnPoints,
            captureZoneService.getCaptureRadius()
        );
    }

    private boolean isFlagMaterial(Material material) {
        return material != null && (material == resolveFlagMaterial(TeamManager.RED)
            || material == resolveFlagMaterial(TeamManager.BLUE));
    }

    private Location toBlockLocation(Location location) {
        if (location == null) {
            return null;
        }

        World world = location.getWorld();
        return new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private Location toIndicatorLocation(Location baseBlockLocation) {
        if (baseBlockLocation == null) {
            return null;
        }

        return baseBlockLocation.toBlockLocation().add(0.5, 2.25, 0.5);
    }

    private Location resolveBaseIndicatorLocation(String teamKey, FlagMetaData flag) {
        Location baseLocation = flag == null ? null : flag.baseLocation;
        return resolveBaseIndicatorLocation(teamKey, baseLocation);
    }

    private Location resolveBaseIndicatorLocation(String teamKey, Location baseLocation) {
        Location indicator = flagConfigHandler.getIndicator(teamKey).orElse(null);
        if (indicator != null) {
            return indicator;
        }
        return toIndicatorLocation(baseLocation);
    }

    private Location resolveIndicatorSpawnLocation(String teamKey, FlagMetaData flag) {
        if (flag == null) {
            return null;
        }

        if (flag.state == FlagState.AT_BASE) {
            Location baseIndicator = resolveBaseIndicatorLocation(teamKey, flag);
            if (baseIndicator != null) {
                return baseIndicator;
            }
        }

        if (flag.activeLocation != null) {
            return toIndicatorLocation(flag.activeLocation);
        }

        return null;
    }

    private boolean sameBlock(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }

        return first.getWorld().equals(second.getWorld())
            && first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY()
            && first.getBlockZ() == second.getBlockZ();
    }

    private void setFlagBlock(Location location, String teamKey) {
        if (location == null) {
            return;
        }

        Material material = resolveFlagMaterial(teamKey);
        if (material != null) {
            location.getBlock().setType(material);
            location.getBlock().setMetadata(FLAG_METADATA_KEY, new FixedMetadataValue(plugin, teamKey));
            Location indicatorLocation = resolveBaseIndicatorLocation(teamKey, location);
            flagIndicator.spawnFlagIndicatorForTeam(teamKey, indicatorLocation);
        }
    }

    private void setFlagBlockWithoutIndicator(Location location, String teamKey) {
        if (location == null) {
            return;
        }

        Material material = resolveFlagMaterial(teamKey);
        if (material == null) {
            return;
        }

        location.getBlock().setType(material);
        location.getBlock().setMetadata(FLAG_METADATA_KEY, new FixedMetadataValue(plugin, teamKey));
    }

    private void setDroppedFlagBlock(Location location, String teamKey) {
        if (location == null) {
            return;
        }

        Material material = teamManager.getCaptureMaterial(teamKey);
        if (material != null) {
            location.getBlock().setType(material);
            location.getBlock().setMetadata(FLAG_METADATA_KEY, new FixedMetadataValue(plugin, teamKey));
            flagIndicator.spawnFlagIndicatorForTeam(teamKey, toIndicatorLocation(location));
        }
    }

    private void setBlockAir(Location location, String teamKey) {
        if (location == null) {
            return;
        }

        location.getBlock().removeMetadata(FLAG_METADATA_KEY, plugin);
        location.getBlock().setType(Material.AIR);
        if (teamKey != null) {
            flagIndicator.removeFlagIndicatorForTeam(teamKey);
        }
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Method resolvePlayerMethod(String name, Class<?>... parameterTypes) {
        try {
            return Player.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private void notifyOwnFlagCaptureBlocked(Player player) {
        if (player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long nextAllowed = ownFlagNoticeUntilByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (nextAllowed > now) {
            return;
        }

        ownFlagNoticeUntilByPlayer.put(player.getUniqueId(), now + OWN_FLAG_NOTICE_COOLDOWN_MS);
        player.sendMessage(msg("error.capture_own_flag"));
    }

    private void publishDebug(String message) {
        if (message == null) {
            return;
        }
        debugPublisher.accept(message);
    }
}



