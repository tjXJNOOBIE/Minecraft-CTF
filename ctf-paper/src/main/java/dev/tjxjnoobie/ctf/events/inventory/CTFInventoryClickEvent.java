package dev.tjxjnoobie.ctf.events.inventory;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.FlagCarrierLockEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerBuildGuardEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class CTFInventoryClickEvent implements Listener {
    private final FlagCarrierLockEventHandler flagCarrierLockHandler;
    private final PlayerBuildGuardEventHandler playerBuildRestrictionHandler;
    private final IHomingSpearCombatEventHandler homingSpearCombatHandler;

    // == Lifecycle ==
    /**
     * Constructs a CTFInventoryClickEvent instance.
     */
    public CTFInventoryClickEvent() {
        // Resolve handler once after dependency registration.
        this.flagCarrierLockHandler = DependencyLoaderAccess.requireInstance(FlagCarrierLockEventHandler.class, "FlagCarrierLockEventHandler not registered");
        this.playerBuildRestrictionHandler = DependencyLoaderAccess.requireInstance(PlayerBuildGuardEventHandler.class, "PlayerBuildGuardEventHandler not registered");
        this.homingSpearCombatHandler = DependencyLoaderAccess.requireInstance(IHomingSpearCombatEventHandler.class, "IHomingSpearCombatEventHandler not registered");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        flagCarrierLockHandler.onInventoryClick(event);
        boolean cancelledAfterFlagLock = event.isCancelled();
        // Guard: short-circuit when cancelledAfterFlagLock.
        if (cancelledAfterFlagLock) {
            return;
        }

        playerBuildRestrictionHandler.onInventoryClick(event);
        boolean cancelledAfterBuildGuard = event.isCancelled();
        // Guard: short-circuit when cancelledAfterBuildGuard.
        if (cancelledAfterBuildGuard) {
            return;
        }

        homingSpearCombatHandler.onInventoryClick(event);
    }
}

