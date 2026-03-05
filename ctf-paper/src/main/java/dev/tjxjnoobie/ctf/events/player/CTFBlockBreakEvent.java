package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.FlagBreakEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerBuildGuardEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class CTFBlockBreakEvent implements Listener {
    private final FlagBreakEventHandler flagBreakHandler;
    private final PlayerBuildGuardEventHandler playerBuildRestrictionHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFBlockBreakEvent instance.
     */
    public CTFBlockBreakEvent() {
        Class<FlagBreakEventHandler> flagClass = FlagBreakEventHandler.class; // Resolve handler once after dependency registration.
        String flagMsg = "FlagBreakEventHandler not registered";
        this.flagBreakHandler = DependencyLoaderAccess.requireInstance(flagClass, flagMsg);

        Class<PlayerBuildGuardEventHandler> buildClass = PlayerBuildGuardEventHandler.class;
        String buildMsg = "PlayerBuildGuardEventHandler not registered";
        this.playerBuildRestrictionHandler = DependencyLoaderAccess.requireInstance(buildClass, buildMsg);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.flagBreakHandler.onBlockBreak(event);
        boolean cancelledAfterFlagBreak = event.isCancelled();
        // Guard: short-circuit when cancelledAfterFlagBreak.
        if (cancelledAfterFlagBreak) {
            return;
        }

        this.playerBuildRestrictionHandler.onBlockBreak(event);
    }
}
