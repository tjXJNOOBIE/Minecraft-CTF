package dev.tjxjnoobie.ctf.combat.metadata;

import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Immutable spear death attribution output for message + credited killer id.
 */
public record SpearDeathAttributionMetaData(Component message, UUID creditedKillerId) {
}
