package dev.tjxjnoobie.ctf.config.message;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageHandler;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and resolves message strings with placeholder support.
 */
public final class MessageConfig implements MessageHandler {

    // == Constants ==
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final JavaPlugin plugin;

    // == Runtime state ==
    private FileConfiguration messageConfig;
    private FileConfiguration defaultMessageConfig;
    private File messageFile;
    private boolean messageConfigActive;

    // == Lifecycle ==
    /**
     * Constructs a MessageConfig instance.
     *
     * @param plugin Plugin instance used to access Bukkit runtime services.
     */
    public MessageConfig(JavaPlugin plugin) {
        // Capture plugin reference.
        this.plugin = plugin;
    }

    /**
     * Returns data for loadMessageConfig.
     */
    @Override
    public void loadMessageConfig() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }

        boolean conditionResult1 = !plugin.getDataFolder().exists(); // Ensure data folder exists.
        if (conditionResult1) {
            plugin.getDataFolder().mkdirs();
        }

        messageFile = new File(plugin.getDataFolder(), "messages.yml");
        boolean existsResult = messageFile.exists();
        if (!existsResult) {
            // Copy default messages on first run.
            plugin.saveResource("messages.yml", false);
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        defaultMessageConfig = loadDefaultMessages();
        mergeDefaultsIntoLiveConfig();
        messageConfigActive = true;
    }

    /**
     * Executes reloadMessageConfig.
     */
    @Override
    public void reloadMessageConfig() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return;
        }

        if (messageFile == null) {
            loadMessageConfig();
            return;
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile); // Reload the file and defaults.
        defaultMessageConfig = loadDefaultMessages();
        mergeDefaultsIntoLiveConfig();
        messageConfigActive = true;
    }

    private FileConfiguration loadDefaultMessages() {
        // Guard: short-circuit when plugin == null.
        if (plugin == null) {
            return null;
        }

        // Read messages.yml from the plugin jar.
        try (InputStream stream = plugin.getResource("messages.yml")) {
            // Guard: short-circuit when stream == null.
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void mergeDefaultsIntoLiveConfig() {
        if (messageConfig == null || defaultMessageConfig == null) {
            return;
        }

        messageConfig.setDefaults(defaultMessageConfig);
        messageConfig.options().copyDefaults(true);
        saveLiveConfig();
    }

    private void saveLiveConfig() {
        if (messageFile == null || messageConfig == null) {
            return;
        }

        try {
            messageConfig.save(messageFile);
        } catch (Exception ignored) {
            // Keep message loading resilient even when the file cannot be rewritten.
        }
    }

    // == Getters ==
    @Override
    public Component getMessage(String key) {
        Map<String, String> placeholders = Map.of(); // Simple message lookup with no placeholders.
        return getMessage(key, placeholders);
    }

    @Override
    public Component getMessage(String key, Map<String, String> placeholders) {
        String resolved = resolveMessageText(key); // Resolve base message text.

        if (placeholders != null) {
            // Apply placeholder replacements.
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                resolved = resolved.replace("{" + entry.getKey() + "}", value);
            }
        }

        return LEGACY.deserialize(resolved);
    }

    @Override
    public Component getMessageFormatted(String key, Object... args) {
        String formatText = resolveMessageText(key).replace("%p", "%s"); // Format with String#formatted after adapting placeholders.
        try {
            return LEGACY.deserialize(formatText.formatted(args == null ? new Object[0] : args));
        } catch (IllegalFormatException ex) {
            return LEGACY.deserialize(formatText);
        }
    }

    public FileConfiguration getMessagesConfig() {
        // Return the mutable message configuration.
        return messageConfig;
    }

    public FileConfiguration getDefaultMessagesConfig() {
        // Return the bundled default configuration.
        return defaultMessageConfig;
    }

    private String resolveMessageText(String key) {
        String raw = null; // Resolve from custom config, then defaults, with fallback.
        if (messageConfig != null && key != null) {
            raw = messageConfig.getString(key);
        }
        if (raw == null && defaultMessageConfig != null && key != null) {
            raw = defaultMessageConfig.getString(key);
        }
        if (raw == null) {
            raw = "&cMissing message: " + (key == null ? "null" : key);
        }

        String prefix = "&7[&b&lC&6&lT&c&lF&7] ";
        if (messageConfig != null) {
            prefix = messageConfig.getString("prefix", prefix);
        } else if (defaultMessageConfig != null) {
            prefix = defaultMessageConfig.getString("prefix", prefix);
        }

        boolean includePrefix = true;
        if (key != null && (key.startsWith("scoreboard.")
            || key.startsWith("bossbar.")
            || key.startsWith("actionbar.")
            || key.startsWith("title.")
            || key.startsWith("debug."))) {
            includePrefix = false;
        }
        boolean conditionResult2 = "scoreboard.header_lobby".equals(key);
        if (conditionResult2) {
            includePrefix = true;
        }
        // Prefix only for chat-style messages.
        return includePrefix ? prefix + raw : raw;
    }

    // == Predicates ==
    @Override
    public boolean isMessageConfigActive() {
        // Return true when messages have been loaded.
        return messageConfigActive;
    }

    public boolean isActive() {
        // Backwards-compatible alias.
        return messageConfigActive;
    }
}

