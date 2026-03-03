package dev.tjxjnoobie.ctf.config.message.interfaces;

import java.util.Map;
import net.kyori.adventure.text.Component;

public interface MessageService {
    void loadMessageConfig();

    void reloadMessageConfig();

    Component getMessage(String key);

    Component getMessage(String key, Map<String, String> placeholders);

    boolean isMessageConfigActive();
}

