package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.combat.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class CTFPlayerRespawnEvent implements Listener {
    private static final String LOG_PREFIX = "[CTF] [CTFPlayerRespawnEvent] ";

    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final KitManager kitManager;
    private final ScoutTaggerAbility scoutTaggerAbility;

    public CTFPlayerRespawnEvent(CtfMatchOrchestrator gameManager, KitManager kitManager, ScoutTaggerAbility scoutTaggerAbility) {
        this.gameManager = gameManager;
        this.kitManager = kitManager;
        this.scoutTaggerAbility = scoutTaggerAbility;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location respawn = gameManager.getRespawnLocation(event.getPlayer());
        if (respawn != null) {
            event.setRespawnLocation(respawn);
        }

        if (gameManager.isRunning() && gameManager.isPlayerInGame(event.getPlayer())) {
            kitManager.applyKit(event.getPlayer());
            event.getPlayer().setNoDamageTicks(40);
            if (scoutTaggerAbility != null) {
                scoutTaggerAbility.handleRespawn(event.getPlayer());
            }
        }

        Bukkit.getLogger().info(LOG_PREFIX + "Respawn handled - player=" + event.getPlayer().getName());
    }
}



