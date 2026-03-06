package dev.tjxjnoobie.ctf.events.handlers;

import io.papermc.paper.event.player.AsyncChatEvent;

public interface IPlayerChatEventHandler {
    void onAsyncChat(AsyncChatEvent event);
}
