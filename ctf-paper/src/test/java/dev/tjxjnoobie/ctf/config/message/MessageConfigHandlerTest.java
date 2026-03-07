package dev.tjxjnoobie.ctf.config.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MessageConfigHandlerTest extends TestLogSupport {

    private MessageConfig messageConfig;
    private FileConfiguration messages;
    private MessageConfigHandler handler;

    @BeforeEach
    void setUp() {
        messageConfig = Mockito.mock(MessageConfig.class);
        messages = Mockito.mock(FileConfiguration.class);

        when(messageConfig.getMessagesConfig()).thenReturn(messages);
        when(messageConfig.getDefaultMessagesConfig()).thenReturn(messages);
        when(messages.getString("prefix")).thenReturn("&7[&b&lC&6&lT&c&lF&7] ");
        when(messages.getString("scoreboard.header")).thenReturn("&f%s");
        when(messages.getString("scoreboard.header_lobby")).thenReturn("&eLobby");
        when(messages.getString("scoreboard.header_overtime")).thenReturn("&4&lOT &f%s");
        when(messages.getString("scoreboard.line.red")).thenReturn("&cRed: &f%s");

        registerDependency(MessageConfig.class, messageConfig);
        handler = new MessageConfigHandler();
        logStep("constructed real message config handler");
    }

    @Test
    void scoreboardHeadersUseConfiguredPrefix() {
        assertEquals("&7[&b&lC&6&lT&c&lF&7] &f%s", handler.getResolvedText("scoreboard.header"));
        assertEquals("&7[&b&lC&6&lT&c&lF&7] &eLobby", handler.getResolvedText("scoreboard.header_lobby"));
        assertEquals("&7[&b&lC&6&lT&c&lF&7] &4&lOT &f%s", handler.getResolvedText("scoreboard.header_overtime"));
    }

    @Test
    void scoreboardLinesDoNotDuplicatePrefix() {
        assertEquals("&cRed: &f%s", handler.getResolvedText("scoreboard.line.red"));
    }
}
