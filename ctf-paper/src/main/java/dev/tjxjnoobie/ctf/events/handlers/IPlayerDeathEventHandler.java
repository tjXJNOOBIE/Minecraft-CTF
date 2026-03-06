package dev.tjxjnoobie.ctf.events.handlers;

import org.bukkit.event.entity.PlayerDeathEvent;

public interface IPlayerDeathEventHandler {
    void onPlayerDeath(PlayerDeathEvent event);
}
