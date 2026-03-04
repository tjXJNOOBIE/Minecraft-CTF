package dev.tjxjnoobie.ctf.team.handlers;

import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.team.TeamDomainUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Owns team chat rendering (prefix + name/message coloring).
 */
public final class TeamChatRenderer implements PlayerDependencyAccess {
    /**
     * Executes applyTeamChatRenderer.
     *
     * @param event Event context to inspect and optionally mutate.
     */

    // == Lifecycle ==

    public void applyTeamChatRenderer(AsyncChatEvent event) {
        // Guard: short-circuit when event == null.
        if (event == null) {
            return;
        }

        String teamKey = getTeamManager().getCachedTeamKey(event.getPlayer().getUniqueId()); // Use cached team state to avoid touching Bukkit scoreboard on async thread.
        final Component teamPrefix = TeamDomainUtil.chatPrefix(teamKey);

        event.renderer((source, sourceDisplayName, message, viewer) ->
            teamPrefix
                .append(sourceDisplayName.colorIfAbsent(NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(message.colorIfAbsent(NamedTextColor.WHITE))
        );
    }
}

