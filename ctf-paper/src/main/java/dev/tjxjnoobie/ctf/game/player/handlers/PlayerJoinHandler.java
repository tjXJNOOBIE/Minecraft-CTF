package dev.tjxjnoobie.ctf.game.player.handlers;

import dev.tjxjnoobie.ctf.events.handlers.PlayerJoinEventHandler;

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
/**
 * Owns player join UX: display name formatting and welcome messaging.
 */
public final class PlayerJoinHandler implements PlayerJoinEventHandler, MessageAccess, BukkitMessageSender, FlagDependencyAccess, LifecycleDependencyAccess {
    private static final String DEFAULT_WORLD_NAME = "CTFMap"; // Core systems (plugin, game state, loop, debug)

    // == Lifecycle ==

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        World ctfWorld = Bukkit.getWorld(DEFAULT_WORLD_NAME);
        // Guard: short-circuit when ctfWorld != null.
        if (ctfWorld != null) {
            Location spawnLocation = ctfWorld.getSpawnLocation();
            player.teleport(spawnLocation);
        }

        String playerName = player.getName();
        player.displayName(Component.text(playerName, NamedTextColor.GRAY));

        Component joinMessage = getMessageFormatted("broadcast.server_join", playerName);
        event.joinMessage(joinMessage);

        Component welcome = getMessage("player.welcome");
        sendMessage(player, welcome);
        GameState gameState = getGameStateManager().getGameState();
        if (gameState == GameState.IN_PROGRESS || gameState == GameState.OVERTIME) {
            Component activeNotice = getMessage("player.active_game_notice");
            sendMessage(player, activeNotice);
        }
        getFlagLifecycleHandler().syncIndicatorVisibility();
    }
}
