package dev.tjxjnoobie.ctf.config.message.interfaces;

import java.util.Map;
import net.kyori.adventure.text.Component;

public interface MessageHandler {
    // Load initial messages from disk.
    void loadMessageConfig();

    // Reload messages from disk.
    void reloadMessageConfig();

    // Get a message by key with no placeholders.
    Component getMessage(String key);

    // Get a message by key with placeholders.
    Component getMessage(String key, Map<String, String> placeholders);

    // Get a formatted message by key.
    Component getMessageFormatted(String key, Object... args);

    // True when messages are loaded.
    boolean isMessageConfigActive();
}

