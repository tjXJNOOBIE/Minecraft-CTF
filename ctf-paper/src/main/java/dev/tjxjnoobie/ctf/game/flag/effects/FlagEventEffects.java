package dev.tjxjnoobie.ctf.game.flag.effects;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Owns chat/title/sound/particle UX for flag events (pickup/drop/return/capture).
 */
public final class FlagEventEffects implements MessageAccess, BukkitMessageSender, BukkitEffectsUtil, PlayerDependencyAccess {

    // == Constants ==
    private static final Component FLAG_PICKUP_TITLE =
        Component.text("YOU PICKED UP THE FLAG", NamedTextColor.RED, TextDecoration.BOLD);
    private static final Component FLAG_PICKUP_SUBTITLE =
        Component.text("Return to base to score", NamedTextColor.AQUA);
    private static final int FIREWORK_MIN_HEIGHT = 5;
    private static final int FIREWORK_MAX_HEIGHT = 8;
    private static final int FIREWORK_DETONATE_TICKS = 17;
    private static final List<org.bukkit.FireworkEffect.Type> FIREWORK_TYPES = List.of(
        org.bukkit.FireworkEffect.Type.BALL,
        org.bukkit.FireworkEffect.Type.BALL_LARGE,
        org.bukkit.FireworkEffect.Type.BURST,
        org.bukkit.FireworkEffect.Type.CREEPER,
        org.bukkit.FireworkEffect.Type.STAR
    );

    // == Runtime state ==
    private org.bukkit.FireworkEffect.Type lastFireworkType;

    // == Utilities ==
    /**
     * Executes showFlagPickupMessaging.
     *
     * @param player Player involved in this operation.
     * @param flagTeam Team identifier associated with the flag operation.
     */
    public void showFlagPickupMessaging(Player player, TeamId flagTeam) {
        // Guard: short-circuit when player == null || flagTeam == null.
        if (player == null || flagTeam == null) {
            return;
        }

        sendTitle(player, FLAG_PICKUP_TITLE, FLAG_PICKUP_SUBTITLE);
        Component defenderHeader = getMessage("title.flag.taken.header");
        Component defenderSubtitle = getMessageFormatted("title.flag.taken.sub", player.getName());
        for (Player defender : getTeamManager().getTeamPlayers(flagTeam)) {
            sendTitle(defender, defenderHeader, defenderSubtitle);
        }
        Component actionBar = getMessage(CTFKeys.uiFlagCarrierActionbarKey());
        sendActionBar(player, actionBar);
        Component pickupBroadcast = getMessageFormatted("broadcast.flag.pickup", player.getName(), getTeamManager().getDisplayName(flagTeam));
        broadcastToArena(pickupBroadcast);
    }

    /**
     * Executes playFlagPickupEffects.
     *
     * @param player Player involved in this operation.
     * @param flagTeam Team identifier associated with the flag operation.
     */
    public void playFlagPickupEffects(Player player, TeamId flagTeam) {
        // Guard: short-circuit when player == null || flagTeam == null.
        if (player == null || flagTeam == null) {
            return;
        }

        TeamId playerTeam = getTeamManager().getTeamId(player);
        if (playerTeam != null) {
            playTeamEffects(playerTeam, flagTeam,
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.BLOCK_NOTE_BLOCK_BASS,
                Particle.HAPPY_VILLAGER, Particle.SMOKE,
                player.getLocation());
            playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
        }
    }

    /**
     * Executes showFlagDropBroadcast.
     *
     * @param player Player involved in this operation.
     * @param carriedTeam Team identifier associated with the flag operation.
     */
    public void showFlagDropBroadcast(Player player, TeamId carriedTeam) {
        // Guard: short-circuit when player == null || carriedTeam == null.
        if (player == null || carriedTeam == null) {
            return;
        }

        Component dropBroadcast = getMessageFormatted("broadcast.flag.drop", player.getName(), getTeamManager().getDisplayName(carriedTeam));
        broadcastToArena(dropBroadcast);
    }

    /**
     * Executes playFlagDropEffects.
     *
     * @param player Player involved in this operation.
     * @param carriedTeam Team identifier associated with the flag operation.
     * @param dropLocation Location used for flag/base placement or fallback logic.
     */
    public void playFlagDropEffects(Player player, TeamId carriedTeam, Location dropLocation) {
        // Guard: short-circuit when player == null || carriedTeam == null.
        if (player == null || carriedTeam == null) {
            return;
        }

        TeamId playerTeam = getTeamManager().getTeamId(player);
        // Guard: short-circuit when playerTeam == null.
        if (playerTeam == null) {
            return;
        }

        playTeamEffects(carriedTeam, playerTeam,
            Sound.BLOCK_NOTE_BLOCK_PLING, Sound.ENTITY_VILLAGER_NO,
            Particle.HAPPY_VILLAGER, Particle.SMOKE,
            dropLocation);
    }

    /**
     * Executes showFlagReturnMessages.
     *
     * @param player Player involved in this operation.
     * @param teamId Team identifier used for lookup or state updates.
     */
    public void showFlagReturnMessages(Player player, TeamId teamId) {
        // Guard: short-circuit when player == null || teamId == null.
        if (player == null || teamId == null) {
            return;
        }

        Component ownMessage = getMessageFormatted("broadcast.flag.return_own", player.getName(), getTeamManager().getDisplayName(teamId));
        for (Player teammate : getTeamManager().getTeamPlayers(teamId)) {
            sendMessage(teammate, ownMessage);
        }
        Component enemyMessage = getMessageFormatted("broadcast.flag.return_enemy", player.getName(), getTeamManager().getDisplayName(teamId));
        for (Player enemy : getTeamManager().getTeamPlayers(teamId.opposite())) {
            sendMessage(enemy, enemyMessage);
        }
    }

    /**
     * Executes playFlagReturnEffects.
     *
     * @param teamId Team identifier used for lookup or state updates.
     * @param baseLocation Location used for flag/base placement or fallback logic.
     */
    public void playFlagReturnEffects(TeamId teamId, Location baseLocation) {
        // Guard: short-circuit when teamId == null.
        if (teamId == null) {
            return;
        }

        playTeamEffects(teamId, teamId.opposite(),
            Sound.ENTITY_PLAYER_LEVELUP, Sound.BLOCK_NOTE_BLOCK_BASS,
            Particle.HAPPY_VILLAGER, Particle.SMOKE,
            baseLocation);
    }

    /**
     * Executes showFlagCaptureTitle.
     *
     * @param player Player involved in this operation.
     * @param scoringTeamId Team identifier used for lookup or state updates.
     */
    public void showFlagCaptureTitle(Player player, TeamId scoringTeamId) {
        // Guard: short-circuit when player == null || scoringTeamId == null.
        if (player == null || scoringTeamId == null) {
            return;
        }

        Component header = getMessageFormatted("title.score.header", player.getName(), getTeamManager().getDisplayName(scoringTeamId));
        Component subTitle = getMessageFormatted("title.score.sub", getTeamManager().getDisplayName(scoringTeamId));
        broadcastToArenaTitle(header, subTitle);
    }

    /**
     * Executes showFlagCaptureBroadcast.
     *
     * @param player Player involved in this operation.
     * @param scoringTeam Team key used for lookup or state updates.
     * @param capturedTeam Team identifier associated with the flag operation.
     */
    public void showFlagCaptureBroadcast(Player player, TeamId scoringTeam, TeamId capturedTeam) {
        // Guard: short-circuit when player == null || scoringTeam == null || capturedTeam == null.
        if (player == null || scoringTeam == null || capturedTeam == null) {
            return;
        }

        Component captureBroadcast = getMessageFormatted("broadcast.flag.capture",
            player.getName(),
            getTeamManager().getDisplayName(capturedTeam),
            getTeamManager().getDisplayName(scoringTeam));
        broadcastToArena(captureBroadcast);
    }

    /**
     * Executes playFlagCaptureEffects.
     *
     * @param scoringTeam Team key used for lookup or state updates.
     * @param capturedTeam Team identifier associated with the flag operation.
     * @param location World location used by this operation.
     */
    public void playFlagCaptureEffects(TeamId scoringTeam, TeamId capturedTeam, Location location) {
        // Guard: short-circuit when scoringTeam == null || capturedTeam == null.
        if (scoringTeam == null || capturedTeam == null) {
            return;
        }

        playTeamEffects(scoringTeam, capturedTeam,
            Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_VILLAGER_NO,
            Particle.FIREWORK, Particle.SMOKE,
            location);

        boolean conditionResult1 = location != null && location.getWorld() != null;
        if (conditionResult1) {
            spawnParticle(location.getWorld(), Particle.FIREWORK, location.clone().add(0.5, 1.2, 0.5), 25, 0.3, 0.3, 0.3, 0.02);
            spawnCaptureFirework(scoringTeam, location);
        }
    }

    private void spawnCaptureFirework(TeamId teamId, Location location) {
        boolean conditionResult2 = location == null || location.getWorld() == null || teamId == null;
        // Guard: short-circuit when location == null || location.getWorld() == null || teamId == null.
        if (conditionResult2) {
            return;
        }

        Color color = teamId == TeamId.RED ? Color.RED : Color.BLUE;
        boolean flicker = ThreadLocalRandom.current().nextBoolean();
        boolean trail = ThreadLocalRandom.current().nextBoolean();
        spawnFirework(
            location,
            0.5,
            nextFireworkHeightOffset(),
            0.5,
            color,
            nextFireworkType(),
            flicker,
            trail,
            1,
            FIREWORK_DETONATE_TICKS,
            false
        );
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

    private void playTeamEffects(TeamId positiveTeam,
                                 TeamId negativeTeam,
                                 Sound positiveSound,
                                 Sound negativeSound,
                                 Particle positiveParticle,
                                 Particle negativeParticle,
                                 Location location) {
        // Guard: short-circuit when location == null || positiveTeam == null || negativeTeam == null.
        if (location == null || positiveTeam == null || negativeTeam == null) {
            return;
        }

        for (Player positivePlayer : getTeamManager().getTeamPlayers(positiveTeam)) {
            playSound(positivePlayer, positiveSound, 1.0f, 1.1f);
            spawnParticle(positivePlayer, positiveParticle, location, 15, 0.3, 0.3, 0.3, 0.02);
        }

        for (Player negativePlayer : getTeamManager().getTeamPlayers(negativeTeam)) {
            playSound(negativePlayer, negativeSound, 1.0f, 0.8f);
            spawnParticle(negativePlayer, negativeParticle, location, 15, 0.3, 0.3, 0.3, 0.02);
        }
    }
}

