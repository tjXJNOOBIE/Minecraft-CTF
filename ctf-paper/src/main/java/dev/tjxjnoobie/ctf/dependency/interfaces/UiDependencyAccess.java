package dev.tjxjnoobie.ctf.dependency.interfaces;

/**
 * Backward-compatible aggregate for UI-adjacent dependency domains.
 */
public interface UiDependencyAccess extends FlagUiDependencyAccess,
        KitUiDependencyAccess,
        ScoreboardDependencyAccess {
}
