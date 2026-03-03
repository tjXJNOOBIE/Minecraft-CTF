package dev.tjxjnoobie.ctf.config.message;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageService;
import java.io.File;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and resolves message strings with placeholder support.
 */
public final class MessageConfig implements MessageService {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static MessageService instance;

    private final JavaPlugin plugin;
    private FileConfiguration messageConfig;
    private File messageFile;
    private boolean messageConfigActive;

    public MessageConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static MessageService get() {
        return instance;
    }

    @Override
    public void loadMessageConfig() {
        if (plugin == null) {
            return;
        }

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        messageFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        messageConfigActive = true;
    }

    @Override
    public void reloadMessageConfig() {
        if (plugin == null) {
            return;
        }

        if (messageFile == null) {
            loadMessageConfig();
            return;
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        messageConfigActive = true;
    }

    @Override
    public Component getMessage(String key) {
        return getMessage(key, Map.of());
    }

    @Override
    public Component getMessage(String key, Map<String, String> placeholders) {
        String raw = null;
        if (messageConfig != null && key != null) {
            raw = messageConfig.getString(key);
        }

        if (raw == null) {
            raw = "&cMissing message: " + (key == null ? "null" : key);
        }

        String prefix = messageConfig == null ? "&7[&b&lC&6&lT&c&lF&7] " : messageConfig.getString("prefix", "&7[&b&lC&6&lT&c&lF&7] ");
        boolean includePrefix = true;
        if (key != null && (key.startsWith("scoreboard.")
            || key.startsWith("bossbar.")
            || key.startsWith("actionbar.")
            || key.startsWith("title."))) {
            includePrefix = false;
        }
        String resolved = includePrefix ? prefix + raw : raw;

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                resolved = resolved.replace("{" + entry.getKey() + "}", value);
            }
        }

        return LEGACY.deserialize(resolved);
    }

    @Override
    public boolean isMessageConfigActive() {
        return messageConfigActive;
    }
}

