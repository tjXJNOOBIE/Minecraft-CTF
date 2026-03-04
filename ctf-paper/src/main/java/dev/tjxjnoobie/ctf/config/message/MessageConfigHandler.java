package dev.tjxjnoobie.ctf.config.message;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageHandler;
import dev.tjxjnoobie.ctf.dependency.interfaces.MessageConfigDependencyAccess;
import java.util.IllegalFormatException;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Resolves messages from MessageConfig with prefix and formatting rules.
 */
public final class MessageConfigHandler implements MessageHandler, MessageConfigDependencyAccess {

    // == Constants ==
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String FALLBACK_PREFIX = "&7[&b&lC&6&lT&c&lF&7] ";

    // == Lifecycle ==
    /**
     * Returns data for loadMessageConfig.
     */
    @Override
    public void loadMessageConfig() {
        MessageConfig config = messageConfig();
        if (config != null) {
            config.loadMessageConfig();
        }
    }

    /**
     * Executes reloadMessageConfig.
     */
    @Override
    public void reloadMessageConfig() {
        MessageConfig config = messageConfig();
        if (config != null) {
            config.reloadMessageConfig();
        }
    }

    // == Getters ==
    @Override
    public Component getMessage(String key) {
        Map<String, String> placeholders = Map.of();
        return getMessage(key, placeholders);
    }

    @Override
    public Component getMessage(String key, Map<String, String> placeholders) {
        String resolved = resolveMessageText(key);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                resolved = resolved.replace("{" + entry.getKey() + "}", value);
            }
        }

        return LEGACY.deserialize(resolved);
    }

    @Override
    public Component getMessageFormatted(String key, Object... args) {
        String resolved = resolveMessageText(key);
        String formatText = resolved.replace("%p", "%s");
        try {
            return LEGACY.deserialize(formatText.formatted(args == null ? new Object[0] : args));
        } catch (IllegalFormatException ex) {
            return LEGACY.deserialize(formatText);
        }
    }

    public String getResolvedText(String key) {
        return resolveMessageText(key);
    }

    private String resolveMessageText(String key) {
        MessageConfig messageConfig = messageConfig();
        FileConfiguration messages = messageConfig == null ? null : messageConfig.getMessagesConfig();
        FileConfiguration defaults = messageConfig == null ? null : messageConfig.getDefaultMessagesConfig();

        String raw = null;
        if (messages != null && key != null) {
            raw = messages.getString(key);
        }
        if (raw == null && defaults != null && key != null) {
            raw = defaults.getString(key);
        }

        if (raw == null) {
            raw = "&cMissing message: " + (key == null ? "null" : key);
        }

        boolean includePrefix = true;
        if (key != null && (key.startsWith("scoreboard.line.")
            || key.startsWith("bossbar.")
            || key.startsWith("actionbar.")
            || key.startsWith("title.")
            || key.startsWith("debug."))) {
            includePrefix = false;
        }
        boolean scoreboardHeaderKey = "scoreboard.header".equals(key)
            || "scoreboard.header_lobby".equals(key)
            || "scoreboard.header_overtime".equals(key);
        if (scoreboardHeaderKey) {
            includePrefix = true;
        }
        return includePrefix ? resolvePrefix(messages, defaults) + raw : raw;
    }

    private String resolvePrefix(FileConfiguration messages, FileConfiguration defaults) {
        if (messages != null) {
            String configured = messages.getString("prefix");
            // Guard: short-circuit when configured != null.
            if (configured != null) {
                return configured;
            }
        }
        if (defaults != null) {
            String fallback = defaults.getString("prefix");
            // Guard: short-circuit when fallback != null.
            if (fallback != null) {
                return fallback;
            }
        }
        return FALLBACK_PREFIX;
    }

    // == Utilities ==
    private MessageConfig messageConfig() {
        return getMessageConfig();
    }

    // == Predicates ==
    @Override
    public boolean isMessageConfigActive() {
        MessageConfig messageConfig = messageConfig();
        return messageConfig != null && messageConfig.isActive();
    }
}
