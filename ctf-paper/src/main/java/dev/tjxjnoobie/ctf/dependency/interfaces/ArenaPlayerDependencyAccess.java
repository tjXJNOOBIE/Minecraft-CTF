package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Dependency-access surface for the tracked arena-player roster.
 */
public interface ArenaPlayerDependencyAccess {
    default CTFPlayerMetaData getCTFPlayerMetaData() { return DependencyLoaderAccess.findInstance(CTFPlayerMetaData.class); }

    default List<Player> arenaPlayers() {
        return getCTFPlayerMetaData().getPlayers();
    }

    default void broadcastArenaPlayers(Component message) {
        getCTFPlayerMetaData().broadcast(message);
    }
}
