package dev.tjxjnoobie.ctf.util.bukkit.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

/**
 * Static helpers for common Bukkit message component building.
 */
public final class BukkitMessageUtils {

    // == Lifecycle ==
    private BukkitMessageUtils() {
    }

    // == Utilities ==
    /**
     * Returns the result of title.
     *
     * @param title User-facing display text.
     * @param subtitle User-facing display text.
     * @return Built title instance, or {@code null} when required inputs are missing.
     */
    public static Title title(Component title, Component subtitle) {
        // Guard: short-circuit when title == null || subtitle == null.
        if (title == null || subtitle == null) {
            return null;
        }
        // Build a simple title without custom timing.
        return Title.title(title, subtitle);
    }

    /**
     * Returns the result of title.
     *
     * @param title User-facing display text.
     * @param subtitle User-facing display text.
     * @param times Title timing configuration (fade-in, stay, fade-out).
     * @return Built title instance, or {@code null} when required inputs are missing.
     */
    public static Title title(Component title, Component subtitle, Title.Times times) {
        // Guard: short-circuit when title == null || subtitle == null || times == null.
        if (title == null || subtitle == null || times == null) {
            return null;
        }
        // Build a title with explicit timing.
        return Title.title(title, subtitle, times);
    }
}
