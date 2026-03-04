package dev.tjxjnoobie.ctf.scoreboard.handlers;

import dev.tjxjnoobie.ctf.dependency.interfaces.CombatDependencyAccess;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearTimerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds scoreboard cooldown lines for combat abilities.
 */
public final class CooldownLineProviderHandler implements CombatDependencyAccess {
    /**
     * Returns the result of buildCooldownLines.
     *
     * @param playerUUID Unique id of the p la ye r.
     * @return Newly created value.
     */

    // == Lifecycle ==

    public List<Component> buildCooldownLines(UUID playerUUID) {
        List<Component> lines = new ArrayList<>(); // Build ability cooldown lines for the scoreboard.
        // Guard: short-circuit when playerUUID == null.
        if (playerUUID == null) {
            return lines;
        }

        HomingSpearAbilityCooldown homingSpearCooldown = getHomingSpearAbilityCooldown(); // Resolve ability handlers once.
        ScoutTaggerAbility scoutTaggerAbility = getScoutTaggerAbility();
        // Guard: short-circuit when homingSpearAbility == null || scoutTaggerAbility == null.
        if (homingSpearCooldown == null || scoutTaggerAbility == null) {
            return lines;
        }

        long spearRemaining = homingSpearCooldown.getPlayerCooldownRemainingMs(playerUUID); // Add spear cooldown line when active.
        if (spearRemaining > 0L) {
            String spearText = HomingSpearTimerUtil.formatTenths(spearRemaining) + "s";
            Component spearValue = Component.text(spearText, NamedTextColor.WHITE);
            Component spearLine = Component.text("Spear CD: ", NamedTextColor.AQUA)
                .append(spearValue);
            lines.add(spearLine);
        }

        long tagRemaining = scoutTaggerAbility.getCooldownRemainingMs(playerUUID); // Add tag cooldown line when active.
        if (tagRemaining > 0L) {
            String tagText = scoutTaggerAbility.formatCooldownTenths(tagRemaining) + "s";
            Component tagValue = Component.text(tagText, NamedTextColor.WHITE);
            Component tagLine = Component.text("Tag CD: ", NamedTextColor.GREEN)
                .append(tagValue);
            lines.add(tagLine);
        }

        return lines;
    }
}
