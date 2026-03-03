package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.team.TeamManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Formats team chat display cleanly with team tag + display name.
 */
public final class CTFPlayerChatEvent implements Listener {
    private final TeamManager teamManager;

    public CTFPlayerChatEvent(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String teamKey = teamManager.getTeamKey(event.getPlayer());
        final Component teamPrefix = TeamManager.RED.equals(teamKey)
            ? Component.text("[Red] ", NamedTextColor.RED)
            : TeamManager.BLUE.equals(teamKey)
                ? Component.text("[Blue] ", NamedTextColor.BLUE)
                : Component.empty();

        event.renderer((source, sourceDisplayName, message, viewer) ->
            teamPrefix
                .append(sourceDisplayName.colorIfAbsent(NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(message.colorIfAbsent(NamedTextColor.WHITE))
        );
    }
}

