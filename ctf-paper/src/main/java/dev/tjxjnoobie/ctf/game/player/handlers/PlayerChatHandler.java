package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerChatEventHandler;

import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
import dev.tjxjnoobie.ctf.team.handlers.TeamChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;

/**
 * Owns chat event behavior for team chat rendering.
 */
public final class PlayerChatHandler implements PlayerChatEventHandler, PlayerDependencyAccess {
    // == Lifecycle ==

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        getTeamChatRenderer().applyTeamChatRenderer(event);
    }
}

