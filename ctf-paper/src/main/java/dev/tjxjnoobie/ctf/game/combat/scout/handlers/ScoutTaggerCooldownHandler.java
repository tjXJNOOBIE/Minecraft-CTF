package dev.tjxjnoobie.ctf.game.combat.scout.handlers;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.KitUiDependencyAccess;
import dev.tjxjnoobie.ctf.game.combat.scout.util.ScoutTaggerStateUtil;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Owns scout tagger cooldown state and action-bar rendering.
 */
public final class ScoutTaggerCooldownHandler implements MessageAccess, BukkitMessageSender, KitUiDependencyAccess {
    // == Constants ==
    private static final String LOG_PREFIX = "[ScoutTaggerCooldownHandler] ";
    private static final int MAX_AMMO = 6;

    // == Dependencies ==
    private final MatchPlayerSessionHandler matchPlayerSessionHandler;
    private final KitSelectionHandler kitSelectionHandler;
    private final ScoutTaggerRuntimeHandler runtimeHandler;
    private volatile long nextAmmoRegenAtMs;

    // == Lifecycle ==
    /**
     * Constructs a ScoutTaggerCooldownHandler instance.
     *
     * @param matchPlayerSessionHandler Dependency responsible for match player session handler.
     * @param kitSelectionHandler Dependency responsible for kit selection handler.
     * @param runtimeHandler Dependency responsible for scout ammo and cooldown state.
     */
    public ScoutTaggerCooldownHandler(MatchPlayerSessionHandler matchPlayerSessionHandler,
                                      KitSelectionHandler kitSelectionHandler,
                                      ScoutTaggerRuntimeHandler runtimeHandler) {
        this.matchPlayerSessionHandler = matchPlayerSessionHandler;
        this.kitSelectionHandler = kitSelectionHandler;
        this.runtimeHandler = runtimeHandler;
    }

    /**
     * Executes the startCooldown operation.
     *
     * @param playerUUID Unique id of the player.
     * @param cooldownMs Timestamp or duration in milliseconds.
     * @param nowMs Timestamp or duration in milliseconds.
     */
    public void startCooldown(UUID playerUUID, long cooldownMs, long nowMs) {
        // Scout Tagger now uses clip-plus-regen, not a hard throw cooldown.
    }

    /**
     * Executes the stopAll operation.
     *
     * @param restoreName Control flag that changes how this operation is executed.
     */
    public void stopAll(boolean restoreName) {
        // Scout cooldown feedback is action-bar only.
    }

    // == Getters ==
    public long getCooldownRemainingMs(UUID playerUUID, long nowMs) {
        long remainingMs = nextAmmoRegenAtMs - nowMs;
        return Math.max(0L, remainingMs);
    }

    /**
     * Returns the result of formatTenths.
     *
     * @param remainingMs Timestamp or duration in milliseconds.
     * @return Result produced by this method.
     */
    public String formatTenths(long remainingMs) {
        return ScoutTaggerStateUtil.formatTenths(remainingMs);
    }

    // == Utilities ==
    /**
     * Executes clearCooldown.
     *
     * @param playerUUID Unique id of the player.
     * @param restoreName Control flag that changes how this operation is executed.
     */
    public void clearCooldown(UUID playerUUID, boolean restoreName) {
        // Scout Tagger now uses clip-plus-regen, not a hard throw cooldown.
    }

    /**
     * Executes clearAllCooldowns.
     */
    public void clearAllCooldowns() {
        nextAmmoRegenAtMs = 0L;
    }

    /**
     * Executes refreshCooldownVisuals.
     */
    public void refreshCooldownVisuals() {
        long nowMs = System.currentTimeMillis();
        for (UUID playerUUID : runtimeHandler.getTrackedPlayerIds()) {
            refreshCooldownVisual(playerUUID, nowMs);
        }
    }

    public void setNextAmmoRegenAtMs(long nextAmmoRegenAtMs) {
        this.nextAmmoRegenAtMs = Math.max(0L, nextAmmoRegenAtMs);
    }

    /**
     * Sends the contextual scout status message while the sword is held.
     *
     * @param player Player receiving the action-bar update.
     * @param ammo Current ammo count.
     * @param nowMs Current timestamp in milliseconds.
     */
    public void sendHeldStatusActionBar(Player player, int ammo, long nowMs) {
        if (player == null) {
            return;
        }
        int clampedAmmo = Math.max(0, Math.min(MAX_AMMO, ammo));
        long remainingMs = getCooldownRemainingMs(player.getUniqueId(), nowMs);
        String ammoText = Integer.toString(clampedAmmo);
        String remainingSecondsText = formatTenths(remainingMs);
        boolean showReadyStatus = clampedAmmo >= MAX_AMMO;
        Bukkit.getLogger().info(LOG_PREFIX + "status actionbar for " + player.getName()
                + " ammo=" + ammoText + "/6 mode=" + (showReadyStatus ? "ready" : "regen")
                + " regen=" + remainingSecondsText + "s");
        if (showReadyStatus) {
            sendActionBar(player, getMessageFormatted(CTFKeys.uiScoutAmmoActionbarKey(), ammoText));
            return;
        }
        sendActionBar(player, getMessageFormatted(CTFKeys.uiScoutCooldownActionbarKey(), ammoText, remainingSecondsText));
    }

    /**
     * Executes restoreScoutSwordName.
     *
     * @param playerUUID Unique id of the player.
     */
    public void restoreScoutSwordName(UUID playerUUID) {
        // Scout cooldown feedback is action-bar only.
    }

    // == Private helpers ==
    private void refreshCooldownVisual(UUID playerUUID, long nowMs) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean inArena = matchPlayerSessionHandler.isPlayerInArena(player);
        boolean scoutSelected = kitSelectionHandler.isScout(player);
        if (!inArena || !scoutSelected) {
            Bukkit.getLogger().info(LOG_PREFIX + "clearing scout held actionbar state for " + player.getName()
                    + " inArena=" + inArena + " scoutSelected=" + scoutSelected);
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean scoutSwordHeld = getScoutSwordItem() != null && getScoutSwordItem().matches(heldItem);
        if (!scoutSwordHeld) {
            return;
        }

        int ammo = runtimeHandler.ensurePlayerState(playerUUID, MAX_AMMO).getAmmo();
        sendHeldStatusActionBar(player, ammo, nowMs);
    }
}
