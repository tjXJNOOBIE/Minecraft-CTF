package dev.tjxjnoobie.ctf.team;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

/**
 * Shared team-domain mapping helpers (keys, visuals, and materials).
 */
public final class TeamDomainUtil {

    // == Lifecycle ==
    private TeamDomainUtil() {
    }

    // == Utilities ==
    /**
     * Returns the result of normalizeKey.
     *
     * @param input Input value to normalize or validate.
     * @return Resolved value for the requested lookup.
     */
    public static String normalizeKey(String input) {
        // Guard: short-circuit when input == null.
        if (input == null) {
            return null;
        }
        String key = input.trim().toLowerCase(Locale.ROOT);
        boolean conditionResult1 = TeamManager.RED.equals(key) || TeamManager.BLUE.equals(key);
        // Guard: short-circuit when TeamManager.RED.equals(key) || TeamManager.BLUE.equals(key).
        if (conditionResult1) {
            return key;
        }
        return null;
    }

    /**
     * Returns the result of flagMaterial.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @return Result produced by this method.
     */
    public static Material flagMaterial(String teamKey) {
        boolean equalsResult = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult.
        if (equalsResult) {
            return Material.RED_STAINED_GLASS;
        }
        boolean equalsResult2 = TeamManager.BLUE.equals(teamKey);
        // Guard: short-circuit when equalsResult2.
        if (equalsResult2) {
            return Material.BLUE_STAINED_GLASS;
        }
        return null;
    }

    /**
     * Returns the result of captureMaterial.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @return Result produced by this method.
     */
    public static Material captureMaterial(String teamKey) {
        boolean equalsResult3 = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult3.
        if (equalsResult3) {
            return Material.RED_WOOL;
        }
        boolean equalsResult4 = TeamManager.BLUE.equals(teamKey);
        // Guard: short-circuit when equalsResult4.
        if (equalsResult4) {
            return Material.BLUE_WOOL;
        }
        return null;
    }

    /**
     * Returns the result of displayName.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @return Result produced by this method.
     */
    public static String displayName(String teamKey) {
        boolean equalsResult5 = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult5.
        if (equalsResult5) {
            return "Red";
        }
        boolean equalsResult6 = TeamManager.BLUE.equals(teamKey);
        // Guard: short-circuit when equalsResult6.
        if (equalsResult6) {
            return "Blue";
        }
        return teamKey == null ? "Unknown" : teamKey;
    }

    /**
     * Returns the result of displayComponent.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @return Result produced by this method.
     */
    public static Component displayComponent(String teamKey) {
        boolean equalsResult7 = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult7.
        if (equalsResult7) {
            return Component.text("Red", NamedTextColor.RED, TextDecoration.BOLD);
        }
        boolean equalsResult8 = TeamManager.BLUE.equals(teamKey);
        // Guard: short-circuit when equalsResult8.
        if (equalsResult8) {
            return Component.text("Blue", NamedTextColor.BLUE, TextDecoration.BOLD);
        }
        return Component.text(teamKey == null ? "Unknown" : teamKey);
    }

    /**
     * Returns the result of chatPrefix.
     *
     * @param teamKey Team key used for lookup or state updates.
     * @return Result produced by this method.
     */
    public static Component chatPrefix(String teamKey) {
        boolean equalsResult9 = TeamManager.RED.equals(teamKey);
        // Guard: short-circuit when equalsResult9.
        if (equalsResult9) {
            return Component.text("[Red] ", NamedTextColor.RED);
        }
        boolean equalsResult10 = TeamManager.BLUE.equals(teamKey);
        // Guard: short-circuit when equalsResult10.
        if (equalsResult10) {
            return Component.text("[Blue] ", NamedTextColor.BLUE);
        }
        return Component.empty();
    }
}
