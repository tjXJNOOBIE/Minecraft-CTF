package dev.tjxjnoobie.ctf.events.player;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.combat.HomingSpearAbility;
import dev.tjxjnoobie.ctf.combat.metadata.SpearDeathAttributionMetaData;
import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.game.CtfMatchOrchestrator;
import dev.tjxjnoobie.ctf.game.flag.FlagManager;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CTFPlayerDeathEvent implements Listener, MessageAccess {
    private static final String LOG_PREFIX = "[CTF] [CTFPlayerDeathEvent] ";
    
    // Dependencies
    private final CtfMatchOrchestrator gameManager;
    private final FlagManager flagManager;
    private final BossBarManager bossBarManager;
    private final HomingSpearAbility homingSpearAbility;
    private final JavaPlugin plugin;

    public CTFPlayerDeathEvent(CtfMatchOrchestrator gameManager, FlagManager flagManager, BossBarManager bossBarManager,
                               HomingSpearAbility homingSpearAbility, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.flagManager = flagManager;
        this.bossBarManager = bossBarManager;
        this.homingSpearAbility = homingSpearAbility;
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!gameManager.isPlayerInGame(victim)) {
            return;
        }

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        boolean wasCarrier = flagManager.isFlagCarrier(victim.getUniqueId());
        Player killer = victim.getKiller();
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        Entity directDamager = lastDamage instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
        EntityDamageEvent.DamageCause cause = lastDamage == null ? EntityDamageEvent.DamageCause.CUSTOM : lastDamage.getCause();

        flagManager.dropFlagIfCarrier(victim);
        victim.getInventory().clear();

        SpearDeathAttributionMetaData spearAttribution = homingSpearAbility == null ? null
            : homingSpearAbility.resolveSpearAttribution(victim, cause, directDamager);
        Component spearDeathMessage = spearAttribution == null ? null : spearAttribution.message();
        Player creditedKiller = killer;
        if (creditedKiller == null && spearAttribution != null && spearAttribution.creditedKillerId() != null) {
            creditedKiller = Bukkit.getPlayer(spearAttribution.creditedKillerId());
        }

        if (spearDeathMessage != null) {
            gameManager.broadcastToArena(spearDeathMessage);
        } else if (killer != null) {
            gameManager.broadcastToArena(msg("broadcast.kill", Map.of(
                "killer", killer.getName(),
                "victim", victim.getName()
            )));
        } else {
            gameManager.broadcastToArena(msg("broadcast.death", Map.of(
                "player", victim.getName()
            )));
        }

        if (creditedKiller != null) {
            gameManager.recordKill(creditedKiller);
            bossBarManager.showKillBar(creditedKiller, msg("bossbar.kill", Map.of(
                "player", victim.getName()
            )));
            try {
                creditedKiller.playSound(creditedKiller.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            } catch (Throwable ex) {
                Bukkit.getLogger().fine(LOG_PREFIX + "Kill sound skipped - reason=" + ex.getMessage());
            }
        }

        if (wasCarrier && creditedKiller != null) {
            gameManager.broadcastToArena(msg("broadcast.flag_carrier_killed", Map.of(
                "killer", creditedKiller.getName(),
                "victim", victim.getName()
            )));
        }

        try {
            gameManager.recordDeath(victim);
            victim.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        } catch (Throwable ex) {
            Bukkit.getLogger().fine(LOG_PREFIX + "Death sound skipped - reason=" + ex.getMessage());
        }
        Bukkit.getScheduler().runTask(plugin, victim.spigot()::respawn);

        Bukkit.getLogger().info(LOG_PREFIX + "Death handled - victim=" + victim.getName() + " killer="
            + (creditedKiller == null ? "none" : creditedKiller.getName()));
    }
}


