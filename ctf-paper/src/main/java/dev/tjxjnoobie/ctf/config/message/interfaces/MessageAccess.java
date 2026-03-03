package dev.tjxjnoobie.ctf.config.message.interfaces;

import dev.tjxjnoobie.ctf.config.message.MessageConfig;
import java.util.Map;
import net.kyori.adventure.text.Component;

public interface MessageAccess {
    default MessageService messages() {
        return MessageConfig.get();
    }

    default Component msg(String key) {
        MessageService service = messages();
        return service == null ? Component.text("Missing message service: " + key) : service.getMessage(key);
    }

    default Component msg(String key, Map<String, String> placeholders) {
        MessageService service = messages();
        return service == null ? Component.text("Missing message service: " + key) : service.getMessage(key, placeholders);
    }
}

