package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.PlayerChatEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

public final class CTFAsyncChatEvent implements Listener {
    private final PlayerChatEventHandler playerChatHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFAsyncChatEvent instance.
     */
    public CTFAsyncChatEvent() {
        Class<PlayerChatEventHandler> chatClass = PlayerChatEventHandler.class; // Resolve handler once after dependency registration.
        String chatMsg = "PlayerChatEventHandler not registered";
        this.playerChatHandler = DependencyLoaderAccess.requireInstance(chatClass, chatMsg);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        // Async event: handler must avoid unsafe Bukkit access.
        this.playerChatHandler.onAsyncChat(event);
    }
}
