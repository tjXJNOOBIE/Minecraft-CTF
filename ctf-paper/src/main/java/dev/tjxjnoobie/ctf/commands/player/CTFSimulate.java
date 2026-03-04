package dev.tjxjnoobie.ctf.commands.player;
import dev.tjxjnoobie.ctf.util.CTFKeys;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import dev.tjxjnoobie.ctf.commands.util.CommandSenderUtil;
import dev.tjxjnoobie.ctf.commands.util.SimulationBundleUtil;
import dev.tjxjnoobie.ctf.commands.util.SimulationOperatorUtil;
import dev.tjxjnoobie.ctf.commands.util.SimulationScriptUtil;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PluginConfigDependencyAccess;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
/**
 * Handles /ctf simulate behavior.
 */
public final class CTFSimulate implements CommandExecutor, TabCompleter, MessageAccess, BukkitMessageSender, FlagDependencyAccess, LifecycleDependencyAccess, PluginConfigDependencyAccess {
    private static final String LOG_PREFIX = "[CTFSimulate] "; // Logging + permission
    private static final String PERMISSION = CTFKeys.permissionSimulate();
    private static final int DEFAULT_BOTS = 12;
    private static final int DEFAULT_ARENA_SIZE = 75;
    private static final String DEFAULT_SEED = "demo-seed";

    // == Lifecycle ==
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandSenderUtil.requirePlayer(sender); // Require a player sender.
        // Guard: short-circuit when player == null.
        if (player == null) {
            return true;
        }

        String playerName = player.getName(); // Debug telemetry.
        sendDebugMessage(LOG_PREFIX + "/ctf simulate - player=" + playerName);

        boolean hasPermission = player.hasPermission(PERMISSION); // Enforce simulate permission.
        if (!hasPermission) {
            Component message = getMessage(CTFKeys.messageErrorNoPermissionKey());
            sendMessage(player, message);
            return true;
        }

        // Validate usage (optional mode argument).
        if (args.length > 1) {
            Component message = getMessage("error.usage.ctf_simulate");
            sendMessage(player, message);
            return true;
        }

        Path startDirectory;
        try {
            startDirectory = SimulationBundleUtil.ensureExtracted(getMainPlugin());
        } catch (IOException exception) {
            Bukkit.getLogger().warning(LOG_PREFIX + "failed to prepare bundled simulation runtime: " + exception.getMessage());
            sendMessage(player, Component.text("Failed to prepare bundled simulation runtime. Check server logs."));
            return true;
        }

        String modeName = args.length == 0
            ? SimulationScriptUtil.defaultModeName()
            : args[0].trim().toLowerCase(Locale.ROOT);

        List<String> availableModes;
        try {
            availableModes = SimulationScriptUtil.listAvailableModes(startDirectory);
        } catch (IOException exception) {
            Bukkit.getLogger().warning(LOG_PREFIX + "failed to list simulate modes: " + exception.getMessage());
            sendMessage(player, Component.text("Failed to locate simulation modes. Check server logs."));
            return true;
        }

        if (!availableModes.contains(modeName)) {
            sendMessage(player, Component.text("Unknown simulation mode: " + modeName + " | available: " + String.join(", ", availableModes)));
            return true;
        }

        GameState state = getGameStateManager().getGameState(); // Gate to lobby-only.
        if (state != GameState.LOBBY) {
            String stateName = state.name();
            Component message = getMessageFormatted("error.simulate_lobby_only", stateName);
            sendMessage(player, message);
            return true;
        }

        String host = resolveServerHost(); // Print hyper-real simulation instructions for this host.
        int port = Bukkit.getServer().getPort();
        try {
            SimulationOperatorUtil.ensureOperatorAccess(startDirectory, modeName);
        } catch (IOException exception) {
            Bukkit.getLogger().warning(LOG_PREFIX + "failed to grant simulation operator access: " + exception.getMessage());
            sendMessage(player, Component.text("Failed to prepare simulation bot permissions. Check server logs."));
            return true;
        }
        sendMessage(player, Component.text("Starting simulation script: " + modeName));
        Bukkit.getScheduler().runTaskAsynchronously(getMainPlugin(), () -> {
            try {
                boolean openWindowsConsole = SimulationScriptUtil.isWindowsHost();
                Process process = SimulationScriptUtil.executeScript(
                    startDirectory,
                    host,
                    port,
                    DEFAULT_BOTS,
                    DEFAULT_ARENA_SIZE,
                    DEFAULT_SEED,
                    modeName,
                    openWindowsConsole);
                sendDebugMessage(LOG_PREFIX + "started simulate script pid=" + process.pid() + " mode=" + modeName + " host=" + host + " port=" + port);
                if (openWindowsConsole) {
                    Bukkit.getScheduler().runTask(getMainPlugin(), () ->
                        sendMessage(player, Component.text("Simulation logs opened in a Windows console window.")));
                }
            } catch (IOException exception) {
                Bukkit.getLogger().warning(LOG_PREFIX + "failed to start simulate script: " + exception.getMessage());
                Bukkit.getScheduler().runTask(getMainPlugin(), () ->
                    sendMessage(player, Component.text("Failed to start simulation script. Check server logs.")));
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        try {
            String input = args[0].trim().toLowerCase(Locale.ROOT);
            Path startDirectory = SimulationBundleUtil.ensureExtracted(getMainPlugin());
            return SimulationScriptUtil.listAvailableModes(startDirectory).stream()
                .filter(mode -> mode.startsWith(input))
                .toList();
        } catch (IOException exception) {
            Bukkit.getLogger().warning(LOG_PREFIX + "failed to tab-complete simulate modes: " + exception.getMessage());
            return List.of();
        }
    }

    // == Getters ==
    /**
     * Resolves the server host that simulation bots should connect to.
     *
     * @return Bound server IP when configured, otherwise {@code 127.0.0.1}.
     */
    private String resolveServerHost() {
        String serverIp = Bukkit.getServer().getIp();
        return serverIp == null || serverIp.isBlank() ? "127.0.0.1" : serverIp;
    }
}
