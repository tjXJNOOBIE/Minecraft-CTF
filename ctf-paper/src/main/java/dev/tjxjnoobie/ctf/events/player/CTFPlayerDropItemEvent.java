package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.FlagCarrierLockEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerBuildGuardEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public final class CTFPlayerDropItemEvent implements Listener {
    private final FlagCarrierLockEventHandler flagCarrierLockHandler;
    private final PlayerBuildGuardEventHandler playerBuildRestrictionHandler;
    private final IHomingSpearCombatEventHandler homingSpearCombatHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerDropItemEvent instance.
     */
    public CTFPlayerDropItemEvent() {
        Class<FlagCarrierLockEventHandler> flagClass = FlagCarrierLockEventHandler.class; // Resolve handler once after dependency registration.
        String flagMsg = "FlagCarrierLockEventHandler not registered";
        this.flagCarrierLockHandler = DependencyLoaderAccess.requireInstance(flagClass, flagMsg);

        Class<PlayerBuildGuardEventHandler> buildClass = PlayerBuildGuardEventHandler.class;
        String buildMsg = "PlayerBuildGuardEventHandler not registered";
        this.playerBuildRestrictionHandler = DependencyLoaderAccess.requireInstance(buildClass, buildMsg);

        Class<IHomingSpearCombatEventHandler> spearClass = IHomingSpearCombatEventHandler.class;
        String spearMsg = "IHomingSpearCombatEventHandler not registered";
        this.homingSpearCombatHandler = DependencyLoaderAccess.requireInstance(spearClass, spearMsg);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.flagCarrierLockHandler.onPlayerDropItem(event);
        boolean cancelledAfterFlagLock = event.isCancelled();
        // Guard: short-circuit when cancelledAfterFlagLock.
        if (cancelledAfterFlagLock) {
            return;
        }

        this.playerBuildRestrictionHandler.onPlayerDropItem(event);
        boolean cancelledAfterBuildGuard = event.isCancelled();
        // Guard: short-circuit when cancelledAfterBuildGuard.
        if (cancelledAfterBuildGuard) {
            return;
        }

        this.homingSpearCombatHandler.onPlayerDropItem(event);
    }
}
