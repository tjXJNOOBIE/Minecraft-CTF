package dev.tjxjnoobie.ctf.combat.metadata;

import java.util.UUID;

/**
 * Immutable spear lock attribution metadata for indirect death credit.
 */
public record SpearLockMetaData(UUID shooterId, long lockedAtMs, UUID spearEntityId) {
}
