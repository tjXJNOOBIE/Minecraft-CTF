package dev.tjxjnoobie.ctf.util.bukkit.message;

import dev.tjxjnoobie.ctf.dependency.interfaces.MessageUtilAccess;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import org.bukkit.entity.Player;

/**
 * Interface access layer for delegating messages and visualizations to Bukkit
 * players natively.
 */
public interface BukkitMessageSender extends MessageUtilAccess {

    /**
     * Dispatches an action bar overlay message to a single specific player.
     *
     * @param player    The target player.
     * @param component The Adventure component to render.
     */
    default void sendActionBar(Player player, Component component) {
        boolean missingInput = player == null || component == null;
        // Guard: Either the target player identity is fully null or the message
        // payload is missing, rendering the action invalid.
        // Guard: short-circuit when missingInput.
        if (missingInput) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: The required service dependency for sending Bukkit strings failed
        // to resolve or does not exist.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Push the rendered message down the pipeline to physically reach the client
        // UI.
        sender.sendActionBar(player, component);
    }

    /**
     * Dispatches standard chat feed text to a specific player.
     *
     * @param player    The target player.
     * @param component The Adventure component to render in chat.
     */
    default void sendMessage(Player player, Component component) {
        boolean missingInput = player == null || component == null;
        // Guard: Either the target player identity is fully null or the chat
        // payload is missing, breaking the API expectations.
        // Guard: short-circuit when missingInput.
        if (missingInput) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: The required service dependency for sending Bukkit strings failed
        // to resolve or does not exist.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Execute the delivery to place the text directly into their localized chat
        // frame.
        sender.sendMessage(player, component);
    }

    /**
     * Renders a center-screen title layout structure to a given player.
     *
     * @param player The target player.
     * @param title  The fully compiled title object representing text and timing.
     */
    default void sendTitle(Player player, Title title) {
        boolean missingInput = player == null || title == null;
        // Guard: The caller failed to provide a valid target or a structurally
        // sound title configuration.
        // Guard: short-circuit when missingInput.
        if (missingInput) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: The required service dependency for sending Bukkit title packets
        // failed to resolve.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Distribute the completed title container onto their client screen logic.
        sender.sendTitle(player, title);
    }

    /**
     * Renders a center-screen title and subtitle with default timings.
     *
     * @param player   The target player.
     * @param title    The primary scaling center text.
     * @param subtitle The smaller supporting text beneath.
     */
    default void sendTitle(Player player, Component title, Component subtitle) {
        Title structuredTitle = BukkitMessageUtils.title(title, subtitle);
        sendTitle(player, structuredTitle);
    }

    /**
     * Renders a center-screen title and subtitle with explicit transition timings.
     *
     * @param player   The target player.
     * @param title    The primary scaling center text.
     * @param subtitle The smaller supporting text beneath.
     * @param times    The specific tick math defining fade phases and stay length.
     */
    default void sendTitle(Player player, Component title, Component subtitle, Title.Times times) {
        Title structuredTitle = BukkitMessageUtils.title(title, subtitle, times);
        sendTitle(player, structuredTitle);
    }

    /**
     * Universally distributes a standard chat message across the entire active
     * arena.
     *
     * @param component The textual payload to distribute globally.
     */
    default void broadcastToArena(Component component) {
        // Guard: The provided textual payload implies nothing to send, so global
        // enumeration logic is skipped.
        // Guard: short-circuit when component == null.
        if (component == null) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: The required contextual sender abstraction is fully absent
        // representing a terminal structural error.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Funnel the centralized component out to all session-tracked connected
        // participants simultaneously.
        sender.broadcastToArena(component);
    }

    /**
     * Universally distributes a center-screen title overlay to all arena occupants.
     *
     * @param title The configured title definition to push natively.
     */
    default void broadcastToArenaTitle(Title title) {
        // Guard: The title payload lacks configuration forcing us to cleanly drop
        // this logical path.
        // Guard: short-circuit when title == null.
        if (title == null) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: The required contextual sender abstraction is fully absent
        // representing a terminal structural error.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Push the title structure into the universal sender stream for global client
        // alignment.
        sender.broadcastToArenaTitle(title);
    }

    /**
     * Universally distributes a combined title and subtitle to the full arena with
     * default timing.
     *
     * @param title    The core center text.
     * @param subtitle The smaller text positioned underneath.
     */
    default void broadcastToArenaTitle(Component title, Component subtitle) {
        Title structuredTitle = BukkitMessageUtils.title(title, subtitle);
        broadcastToArenaTitle(structuredTitle);
    }

    /**
     * Universally distributes a combined title and subtitle to the full arena with
     * strict timing.
     *
     * @param title    The core center text.
     * @param subtitle The smaller text positioned underneath.
     * @param times    The tick structure controlling the overlay duration and
     *                 animation.
     */
    default void broadcastToArenaTitle(Component title, Component subtitle, Title.Times times) {
        Title structuredTitle = BukkitMessageUtils.title(title, subtitle, times);
        broadcastToArenaTitle(structuredTitle);
    }

    /**
     * Forwards an underlying system administration trace string to console logging.
     *
     * @param message The exact string representation context of the operation that
     *                progressed.
     */
    default void sendDebugMessage(String message) {
        // Guard: The diagnostic string is natively null blocking logging capability
        // explicitly.
        // Guard: short-circuit when message == null.
        if (message == null) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: Diagnostics inherently rely critically on the sender
        // infrastructure to reach the server IO.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Print the string sequentially into the background processes stream for later
        // auditing or review.
        sender.debug(message);
    }

    /**
     * Integrates and exposes a BossBar UI progression element into the specific
     * player's layout.
     *
     * @param player      The single isolated recipient player.
     * @param bossBarType An enumeration value indicating which conceptual bar is
     *                    being updated/requested.
     * @param text        The overlay context string displayed on top of the
     *                    progression bar itself.
     * @param progress    A scaled float mapped roughly 0.0 -> 1.0 dictating visual
     *                    fill status.
     */
    default void showBossBar(Player player, BukkitBossBarType bossBarType, Component text, float progress) {
        boolean missingInput = player == null || bossBarType == null || text == null;
        // Guard: Critical identifying inputs or textual context are missing so the
        // visual construction must be aborted immediately.
        // Guard: short-circuit when missingInput.
        if (missingInput) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: The server UI controller fails resolution isolating us from
        // hardware-level bossbar APIs.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Tell the native systems to map and apply this bossbar container exclusively
        // onto this player's client scope.
        sender.showBossBar(player, bossBarType, text, progress);
    }

    /**
     * Reverts and safely deletes a specific integrated BossBar layout from the
     * player's view screen dynamically.
     *
     * @param player      The single isolated recipient player explicitly looking at
     *                    the bar.
     * @param bossBarType An enumeration value indicating which conceptual bar must
     *                    be surgically removed.
     */
    default void hideBossBar(Player player, BukkitBossBarType bossBarType) {
        boolean missingInput = player == null || bossBarType == null;
        // Guard: Erroneous cleanup sequence due to non-existent context identifiers
        // for either the target or the bar type.
        // Guard: short-circuit when missingInput.
        if (missingInput) {
            return;
        }

        BukkitMessageUtil sender = getBukkitMessageUtil();
        // Guard: We lack the native service required to tell the client to garbage
        // collect the active progression element UI.
        // Guard: short-circuit when sender == null.
        if (sender == null) {
            return;
        }

        // Disconnect and cleanly wipe out the given visual element tracked inside their
        // UI matrix mapping.
        sender.hideBossBar(player, bossBarType);
    }
}
