package dev.tjxjnoobie.ctf.commands.util;

import org.bukkit.Bukkit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CTFCommandUtilsTest extends TestLogSupport {
    // Constants
    private static final String LOG_PREFIX = "[Test] [CTFCommandUtilsTest] ";

    @Test
    void returnsPlayer() {
        Bukkit.getLogger().info(LOG_PREFIX + "Command sender is a player: allow command execution.");
        Player player = Mockito.mock(Player.class);

        Player result = CommandSenderUtil.requirePlayer(player);

        assertSame(player, result);
        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
        Bukkit.getLogger().info(LOG_PREFIX + "command utils returns player sender");
    }

    @Test
    void rejectsConsole() {
        Bukkit.getLogger().info(LOG_PREFIX + "Command sender is console: reject with error message.");
        CommandSender sender = Mockito.mock(CommandSender.class);

        Player result = CommandSenderUtil.requirePlayer(sender);

        assertNull(result);
        verify(sender).sendMessage(org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
        Bukkit.getLogger().info(LOG_PREFIX + "command utils rejects console sender");
    }
}

