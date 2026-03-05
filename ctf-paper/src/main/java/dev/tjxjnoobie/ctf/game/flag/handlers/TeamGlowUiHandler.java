package dev.tjxjnoobie.ctf.game.flag.handlers;

import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Handles per-viewer teammate glow visuals.
 */
public final class TeamGlowUiHandler implements FlagDependencyAccess, PlayerDependencyAccess, BukkitEffectsUtil {

    // == Constants ==
    private static final int TEAM_GLOW_REFRESH_TICKS = 40;

    // == Runtime state ==
    private final Method sendPotionEffectChangeMethod;
    private final Method sendPotionEffectChangeRemoveMethod;
    private PotionEffect teamGlowEffect;
    private PotionEffectType glowingEffectType;

    // == Lifecycle ==
    /**
     * Constructs a TeamGlowUiHandler instance.
     */
    public TeamGlowUiHandler() {
        this.sendPotionEffectChangeMethod = resolvePlayerMethod("sendPotionEffectChange", LivingEntity.class, PotionEffect.class);
        this.sendPotionEffectChangeRemoveMethod = resolvePlayerMethod("sendPotionEffectChangeRemove", LivingEntity.class, PotionEffectType.class);
    }

    // == Getters ==
    private PotionEffect getTeamGlowEffect() {
        // Guard: short-circuit when teamGlowEffect != null.
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

    private PotionEffectType getGlowingEffectType() {
        // Guard: short-circuit when glowingEffectType != null.
        if (glowingEffectType != null) {
            return glowingEffectType;
        }
        try {
            glowingEffectType = PotionEffectType.GLOWING;
        } catch (Throwable ignored) {
            return null;
        }
        return glowingEffectType;
    }

    private Method resolvePlayerMethod(String name, Class<?>... parameterTypes) {
        try {
            return Player.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    // == Setters ==
    public void updateTeamGlowVisuals() {
        List<Player> joinedPlayers = getTeamManager().getJoinedPlayers();
        for (Player viewer : joinedPlayers) {
            String viewerTeamKey = getTeamManager().getTeamKey(viewer);
            TeamId viewerTeam = TeamId.fromKey(viewerTeamKey);
            // Guard: short-circuit when viewerTeam == null.
            if (viewerTeam == null) {
                continue;
            }

            UUID viewerId = viewer.getUniqueId();
            for (Player target : joinedPlayers) {
                UUID targetId = target.getUniqueId();
                boolean equalsResult = viewerId.equals(targetId);
                // Guard: short-circuit when equalsResult.
                if (equalsResult) {
                    continue;
                }
                boolean conditionResult1 = getFlagStateRegistry().isFlagCarrier(targetId);
                if (conditionResult1) {
                    clearFakeTeamGlow(viewer, target);
                    continue;
                }

                String targetTeamKey = getTeamManager().getTeamKey(target);
                TeamId targetTeam = TeamId.fromKey(targetTeamKey);
                if (targetTeam != null && targetTeam == viewerTeam) {
                    applyFakeTeamGlow(viewer, target);
                } else {
                    clearFakeTeamGlow(viewer, target);
                }
            }
        }
    }

    // == Utilities ==
    /**
     * Executes clearTeamGlowVisuals.
     */
    public void clearTeamGlowVisuals() {
        List<Player> joinedPlayers = getTeamManager().getJoinedPlayers();
        for (Player viewer : joinedPlayers) {
            UUID viewerId = viewer.getUniqueId();
            for (Player target : joinedPlayers) {
                UUID targetId = target.getUniqueId();
                boolean equalsResult2 = viewerId.equals(targetId);
                // Guard: short-circuit when equalsResult2.
                if (equalsResult2) {
                    continue;
                }
                clearFakeTeamGlow(viewer, target);
            }
        }

        for (Player player : joinedPlayers) {
            setGlowing(player, false);
        }
    }

    private void applyFakeTeamGlow(Player viewer, Player target) {
        // Guard: short-circuit when viewer == null || target == null.
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
        // Guard: short-circuit when viewer == null || target == null.
        if (viewer == null || target == null) {
            return;
        }

        if (sendPotionEffectChangeRemoveMethod != null) {
            try {
                PotionEffectType glowingType = getGlowingEffectType();
                // Guard: short-circuit when glowingType == null.
                if (glowingType == null) {
                    return;
                }
                sendPotionEffectChangeRemoveMethod.invoke(viewer, target, glowingType);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Packet API unavailable: skip per-viewer glow removal.
            }
        }
    }
}

