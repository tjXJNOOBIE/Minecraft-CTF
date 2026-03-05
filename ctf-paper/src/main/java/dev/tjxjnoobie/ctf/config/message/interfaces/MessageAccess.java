package dev.tjxjnoobie.ctf.config.message.interfaces;

import dev.tjxjnoobie.ctf.config.message.MessageConfigHandler;
import dev.tjxjnoobie.ctf.dependency.interfaces.MessageConfigDependencyAccess;

import java.util.Map;
import net.kyori.adventure.text.Component;

public interface MessageAccess extends MessageConfigDependencyAccess {

    // == Lifecycle ==

    default MessageHandler getMessageService() {
        MessageConfigHandler handler = getMessageConfigHandler(); // MessageConfigHandler implements MessageHandler and is DI-managed.
        return handler;
    }

    default Component getMessage(String key) {
        MessageHandler service = getMessageService();
        return service == null
            ? missingMessageServiceText(key)
            : service.getMessage(key);
    }

    default Component getMessage(String key, Map<String, String> placeholders) {
        MessageHandler service = getMessageService();
        return service == null
            ? missingMessageServiceText(key)
            : service.getMessage(key, placeholders);
    }

    default Component getMessageFormatted(String key, Object... args) {
        MessageHandler service = getMessageService();
        return service == null
            ? missingMessageServiceText(key)
            : service.getMessageFormatted(key, args);
    }

    private static Component missingMessageServiceText(String key) {
        return Component.text("Missing message service: " + key);
    }
}
