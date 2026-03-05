package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.PlayerQuitEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CTFPlayerQuitEvent implements Listener {
    private final PlayerQuitEventHandler playerQuitHandler;
    private final IHomingSpearCombatEventHandler homingSpearCombatHandler;
    private final IScoutTaggerCombatEventHandler scoutTaggerAbility;

    // == Lifecycle ==
    /**
     * Constructs a CTFPlayerQuitEvent instance.
     */
    public CTFPlayerQuitEvent() {
        Class<PlayerQuitEventHandler> quitClass = PlayerQuitEventHandler.class; // Resolve handler once after dependency registration.
        String quitMsg = "PlayerQuitEventHandler not registered";
        this.playerQuitHandler = DependencyLoaderAccess.requireInstance(quitClass, quitMsg);

        Class<IHomingSpearCombatEventHandler> spearClass = IHomingSpearCombatEventHandler.class;
        String spearMsg = "IHomingSpearCombatEventHandler not registered";
        this.homingSpearCombatHandler = DependencyLoaderAccess.requireInstance(spearClass, spearMsg);

        Class<IScoutTaggerCombatEventHandler> scoutClass = IScoutTaggerCombatEventHandler.class;
        String scoutMsg = "IScoutTaggerCombatEventHandler not registered";
        this.scoutTaggerAbility = DependencyLoaderAccess.requireInstance(scoutClass, scoutMsg);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        // Deterministic chain controlled by the CTF event listener.
        this.playerQuitHandler.onPlayerQuit(event);
        this.homingSpearCombatHandler.onPlayerQuit(event);
        this.scoutTaggerAbility.onPlayerQuit(event);
    }
}
