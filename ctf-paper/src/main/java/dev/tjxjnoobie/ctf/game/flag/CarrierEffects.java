package dev.tjxjnoobie.ctf.game.flag;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import dev.tjxjnoobie.ctf.util.bukkit.effects.BukkitEffectsUtil;

/**
 * Owns carrier slowness and glowing effects.
 */
public final class CarrierEffects implements BukkitEffectsUtil {

    // == Constants ==
    private static final int CARRIER_SLOW_DURATION = 20 * 60 * 10;
    private static final String CARRIER_GLOW_TEAM = "ctf_flag_carrier_glow";

    // == Runtime state ==
    private final Team carrierGlowTeam;

    // == Lifecycle ==
    /**
     * Constructs a CarrierEffects instance.
     */
    public CarrierEffects() {
        this.carrierGlowTeam = resolveCarrierGlowTeam();
    }

    // == Getters ==
    private Team resolveCarrierGlowTeam() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
            // Guard: short-circuit when scoreboard == null.
            if (scoreboard == null) {
                return null;
            }

            Team team = scoreboard.getTeam(CARRIER_GLOW_TEAM);
            if (team == null) {
                team = scoreboard.registerNewTeam(CARRIER_GLOW_TEAM);
            }
            team.color(NamedTextColor.GREEN);
            return team;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // == Utilities ==
    /**
     * Executes applyCarrierEffects.
     *
     * @param player Player involved in this operation.
     */
    public void applyCarrierEffects(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, CARRIER_SLOW_DURATION, 0, false, false, false), true);
        applyCarrierGlow(player);
    }

    /**
     * Executes clearCarrierEffects.
     *
     * @param player Player involved in this operation.
     */
    public void clearCarrierEffects(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        clearCarrierGlow(player);
    }

    /**
     * Executes clearCarrierEffectsForCarrierIds.
     *
     * @param carrierIds Collection or state object updated by this operation.
     */
    public void clearCarrierEffectsForCarrierIds(Collection<UUID> carrierIds) {
        // Guard: short-circuit when carrierIds == null.
        if (carrierIds == null) {
            return;
        }

        for (UUID playerUUID : new HashSet<>(carrierIds)) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                clearCarrierEffects(player);
            }
        }
    }

    /**
     * Executes clearCarrierGlowTeamEntries.
     */
    public void clearCarrierGlowTeamEntries() {
        // Guard: short-circuit when carrierGlowTeam == null.
        if (carrierGlowTeam == null) {
            return;
        }

        for (String entry : new HashSet<>(carrierGlowTeam.getEntries())) {
            carrierGlowTeam.removeEntry(entry);
        }
    }

    private void applyCarrierGlow(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        setGlowing(player, true);
        if (carrierGlowTeam != null) {
            carrierGlowTeam.addEntry(player.getUniqueId().toString());
        }
    }

    private void clearCarrierGlow(Player player) {
        // Guard: short-circuit when player == null.
        if (player == null) {
            return;
        }

        if (carrierGlowTeam != null) {
            carrierGlowTeam.removeEntry(player.getUniqueId().toString());
        }
        setGlowing(player, false);
    }
}
