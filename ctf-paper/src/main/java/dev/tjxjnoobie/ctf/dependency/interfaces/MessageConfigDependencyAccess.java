package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.config.message.MessageConfig;
import dev.tjxjnoobie.ctf.config.message.MessageConfigHandler;
import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

/**
 * Dependency-access surface for message-config loading and lookup helpers.
 */
public interface MessageConfigDependencyAccess {
    default MessageConfig getMessageConfig() { return DependencyLoaderAccess.findInstance(MessageConfig.class); }
    default MessageConfigHandler getMessageConfigHandler() { return DependencyLoaderAccess.findInstance(MessageConfigHandler.class); }
}
